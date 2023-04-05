package de.afs.poc.worker;

import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import io.camunda.zeebe.spring.client.annotation.JobWorker;
import io.camunda.zeebe.spring.client.annotation.Variable;

@Component
public class ErrorLogger {
    @JobWorker(type = "errorLog", fetchVariables = {"employeeName", "zustaendigkeit"})
    public void logError(@Variable String employeeName, @Variable String zustaendigkeit) {

        

        LoggerFactory.getLogger(getClass()).error(employeeName + " " + zustaendigkeit);
    }
}
