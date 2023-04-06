package de.afs.poc.worker;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.camunda.zeebe.spring.client.annotation.JobWorker;
import io.camunda.zeebe.spring.client.annotation.Variable;

@Component
public class FooWorker {

    @Autowired
    private FooService fooService;

    @JobWorker(type = "foo", fetchVariables = "foo")
    public Map<String, Object> handleJobFoo(@Variable String foo){
        String result = fooService.executeFoo(foo);
        return Map.of("fooResult", result);
    }

}
