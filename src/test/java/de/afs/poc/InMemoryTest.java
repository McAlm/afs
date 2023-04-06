package de.afs.poc;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.response.DeploymentEvent;
import io.camunda.zeebe.client.api.response.ProcessInstanceEvent;
import io.camunda.zeebe.process.test.api.ZeebeTestEngine;
import io.camunda.zeebe.process.test.assertions.BpmnAssert;
import io.camunda.zeebe.process.test.extension.ZeebeProcessTest;

@ZeebeProcessTest
public class InMemoryTest extends ZeebeTestUtil {

    // injected by ZeebeProcessTest annotation
    private ZeebeTestEngine engine;
    // injected by ZeebeProcessTest annotation
    private ZeebeClient client;

    @BeforeEach
    void deployProcesses() {
        super.setClient(client);
        super.setEngine(engine);
        
        // The embedded engine is completely reset before each test run.

        // Therefore, we need to deploy the process each time
        final DeploymentEvent deploymentEvent = deployResources("foo.bpmn");

        BpmnAssert.assertThat(deploymentEvent)
                .containsProcessesByResourceName("foo.bpmn");
    }

    @Test
    public void testFoo() throws InterruptedException, TimeoutException {
        // start a process instance
        ProcessInstanceEvent processInstance = client.newCreateInstanceCommand() //
                .bpmnProcessId("foo").latestVersion() //
                .send().join();
        BpmnAssert.assertThat(processInstance).isStarted();

        completeServiceTask("foo");

        completeUserTask("barTask");

        BpmnAssert.assertThat(processInstance).hasPassedElement("EndEvent").isCompleted();
    }

    @Test
    public void testCancelBar() throws InterruptedException, TimeoutException {
        // start a process instance
        ProcessInstanceEvent processInstance = client.newCreateInstanceCommand() //
                .bpmnProcessId("foo").latestVersion() //
                .send().join();

        completeServiceTask("foo");

        increaseTime(Duration.ofDays(1));

        BpmnAssert.assertThat(processInstance).hasPassedElement("CanceledEndEvent").isCompleted();
    }

}
