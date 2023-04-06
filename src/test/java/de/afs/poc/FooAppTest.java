package de.afs.poc;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import de.afs.poc.worker.FooService;
import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.response.ProcessInstanceEvent;
import io.camunda.zeebe.process.test.api.ZeebeTestEngine;
import io.camunda.zeebe.process.test.assertions.BpmnAssert;
import io.camunda.zeebe.spring.test.ZeebeSpringTest;
import lombok.extern.slf4j.Slf4j;

@SpringBootTest
@ZeebeSpringTest
@Slf4j
public class FooAppTest extends ZeebeTestUtil {

    @Autowired
    public FooAppTest(ZeebeTestEngine engine, ZeebeClient client) {
        super(engine, client);

    }

    @MockBean
    private FooService fooService;

    @Test
    public void testFoo() throws InterruptedException, TimeoutException {

        Mockito.when(fooService.executeFoo("foo")).thenReturn("fooBAR");
        // start a process instance
        ProcessInstanceEvent processInstance = client.newCreateInstanceCommand() //
                .bpmnProcessId("foo").latestVersion() //
                .variables(Map.of("foo", "foo"))
                .send().join();
        BpmnAssert.assertThat(processInstance).isStarted();

        // completeServiceTask("foo");

        waitForServiceTask("fooTask", Duration.ofSeconds(5));

        

        completeUserTask("barTask");

        BpmnAssert.assertThat(processInstance).hasPassedElement("EndEvent").isCompleted();

        // And verify it caused the right side effects b calling the business methods
        Mockito.verify(fooService).executeFoo("foo");
        Mockito.verifyNoMoreInteractions(fooService);

    }
}
