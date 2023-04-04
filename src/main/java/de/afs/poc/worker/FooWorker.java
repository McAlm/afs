package de.afs.poc.worker;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.camunda.zeebe.gateway.protocol.GatewayOuterClass.ActivatedJob;
import io.camunda.zeebe.spring.client.annotation.JobWorker;

@Component
public class FooWorker {

    @Autowired
    private FooService fooService;

    @JobWorker(type = "foo")
    public void handleJobFoo(final ActivatedJob job){
        fooService.executeFoo();
    }

}
