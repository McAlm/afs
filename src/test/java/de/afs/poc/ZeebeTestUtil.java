package de.afs.poc;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.junit.jupiter.api.Assertions;

import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.command.DeployResourceCommandStep1;
import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.response.DeploymentEvent;
import io.camunda.zeebe.process.test.api.ZeebeTestEngine;
import io.camunda.zeebe.protocol.impl.record.value.processinstance.ProcessInstanceRecord;
import io.camunda.zeebe.protocol.record.Record;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ZeebeTestUtil {

    private static final Duration MAX_WAIT_FOR_IDLE_TIME = Duration.ofSeconds(5);
    private static final Duration MAX_WAIT_FOR_BUSY_TIME = Duration.ofSeconds(5);
    // injected by ZeebeProcessTest annotation
    protected ZeebeTestEngine engine;
    // injected by ZeebeProcessTest annotation
    protected ZeebeClient client;

    public ZeebeTestUtil() {

    }

    public ZeebeTestUtil(ZeebeTestEngine engine, ZeebeClient client) {
        this.engine = engine;
        this.client = client;
    }

    public void setEngine(ZeebeTestEngine engine) {
        this.engine = engine;
    }

    public void setClient(ZeebeClient client) {
        this.client = client;
    }

    public DeploymentEvent deployResources(final String... resources) {
        final DeployResourceCommandStep1 commandStep1 = client.newDeployResourceCommand();

        DeployResourceCommandStep1.DeployResourceCommandStep2 commandStep2 = null;
        for (final String process : resources) {
            if (commandStep2 == null) {
                commandStep2 = commandStep1.addResourceFromClasspath(process);
            } else {
                commandStep2 = commandStep2.addResourceFromClasspath(process);
            }
        }

        return commandStep2.send().join();
    }

    /*
     * These two methods deal with the asynchronous nature of the engine. It is
     * recommended
     * to wait for an idle state before you assert on the state of the engine.
     * Otherwise, you
     * may run into race conditions and flaky tests, depending on whether the engine
     * is still busy processing your last commands.
     *
     * Also note that many of the helper functions used in this test (e.g. {@code
     * sendMessage(..)}
     * have a call to this method at the end. This is to ensure that each command
     * sent to the engine
     * is fully processed before moving on. Without that you can run into issues,
     * where e.g. you want
     * to complete a task, but the task has not been activated yet.
     *
     * Note that the duration is not like a {@code Thread.sleep()}. The tests will
     * continue as soon as
     * an idle state is reached. Only if no idle state is reached during the {@code
     * duration}
     * passed in as argument, then a timeout exception will be thrown.
     */
    public void waitForIdleState(final Duration duration)
            throws InterruptedException, TimeoutException {
        engine.waitForIdleState(duration);
    }

    public void waitForBusyState(final Duration duration)
            throws InterruptedException, TimeoutException {
        engine.waitForBusyState(duration);
    }

    public void increaseTime(final Duration duration)
            throws InterruptedException, TimeoutException {
        // this method increases the time in a deterministic manner

        /*
         * Process all existing commands to make sure that timer subscriptions related
         * to the process
         * so far have been created
         */
        waitForIdleState(MAX_WAIT_FOR_IDLE_TIME);

        /*
         * Increase time in the engine. This will not take immediate effect, though.
         * There is a
         * real-time delay of a couple of ms until the updated time is picked up by the
         * scheduler
         */
        engine.increaseTime(duration);

        try {
            /*
             * This code assumes that the increase of time will trigger timer events.
             * Therefore, we wait
             * until the engine is busy. This means that it started triggering events.
             *
             * And after that, we wait for it to become idle again. That means it is waiting
             * for new commands
             */
            waitForBusyState(MAX_WAIT_FOR_BUSY_TIME);
            waitForIdleState(MAX_WAIT_FOR_IDLE_TIME);
        } catch (final TimeoutException e) {
            // Do nothing. We've waited up to 1 second for processing to start, if it didn't
            // start in this
            // time the engine probably has not got anything left to process.
        }
    }

    public void completeServiceTask(final String jobType)
            throws InterruptedException, TimeoutException {
        completeServiceTasks(jobType, 1);
    }

    public void completeServiceTasks(final String jobType, final int count)
            throws InterruptedException, TimeoutException {

        waitForIdleState(MAX_WAIT_FOR_IDLE_TIME);

        final var activateJobsResponse = client.newActivateJobsCommand().jobType(jobType).maxJobsToActivate(count)
                .send().join();

        final int activatedJobCount = activateJobsResponse.getJobs().size();
        if (activatedJobCount < count) {
            Assertions.fail(
                    "Unable to activate %d jobs, because only %d were activated."
                            .formatted(count, activatedJobCount));
        }

        for (int i = 0; i < count; i++) {
            final var job = activateJobsResponse.getJobs().get(i);

            client.newCompleteCommand(job.getKey()).send().join();
        }

        waitForIdleState(MAX_WAIT_FOR_IDLE_TIME);
    }

    public void completeUserTask(final String elementId)
            throws InterruptedException, TimeoutException {
        completeUserTask(elementId, null);
    }

    public void completeUserTask(final String elementId, Map<String, Object> variables)
            throws InterruptedException, TimeoutException {

        waitForIdleState(MAX_WAIT_FOR_IDLE_TIME);

        // user tasks can be controlled similarly to service tasks
        // all user tasks share a common job type
        final var activateJobsResponse = client
                .newActivateJobsCommand()
                .jobType("io.camunda.zeebe:userTask")
                .maxJobsToActivate(100)
                .send()
                .join();

        boolean userTaskWasCompleted = false;

        for (final ActivatedJob userTask : activateJobsResponse.getJobs()) {
            if (userTask.getElementId().equals(elementId)) {
                if (variables == null) {
                    // complete the user task we care about
                    client.newCompleteCommand(userTask).send().join();
                } else {
                    client.newCompleteCommand(userTask).variables(variables).send().join();
                }
                userTaskWasCompleted = true;
            } else {
                // fail all other user tasks that were activated
                // failing a task with a retry value >0 means the task can be reactivated in the
                // future
                client.newFailCommand(userTask).retries(Math.max(userTask.getRetries(), 1)).send().join();
            }
        }

        waitForIdleState(MAX_WAIT_FOR_IDLE_TIME);

        if (!userTaskWasCompleted) {
            Assertions.fail("Tried to complete task `%s`, but it was not found".formatted(elementId));
        }
    }

    private boolean taskCompleted = false;

    public void waitForServiceTask(String taskId, Duration maxWait, boolean assertTimeout) throws InterruptedException, TimeoutException {
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

        Runnable run = () -> {
            log.info("running the runnable run...");
            List<Boolean> completed = StreamSupport
                    .stream(engine.getRecordStreamSource().getRecords().spliterator(), false)
                    .map(r -> {
                        return isEventCompleted(taskId, r);
                    }).collect(Collectors.toList());
            taskCompleted = completed.contains(true);
        };

        // init Delay = 5, repeat the task every 1 second
        ScheduledFuture<?> scheduledFuture = executor.scheduleAtFixedRate(run, 10, 100, TimeUnit.MILLISECONDS);

        long start = System.currentTimeMillis();
        long end = start + maxWait.toMillis();
        while (System.currentTimeMillis() < end) {
            Thread.sleep(100);
            log.info("task is completed " + taskCompleted);
            if (taskCompleted) {
                scheduledFuture.cancel(true);
                executor.shutdown();
                break;
            }
        }

        if (!taskCompleted && assertTimeout) {
            Assertions.fail("Task " + taskId + " was expected to complete but it was not");
        } else {
            taskCompleted = false;
        }

    }

    private boolean isEventCompleted(String taskId, Record<?> r) {
        if (r.getIntent().toString().equals("ELEMENT_COMPLETED")) {
            ProcessInstanceRecord record = (ProcessInstanceRecord) r.getValue();
            if (taskId.equals(record.getElementId())) {
                return true;
            }
        }
        return false;
    }

}
