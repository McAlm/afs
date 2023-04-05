package de.afs.poc.worker;

import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import de.afs.poc.dto.ErrorLog;
import io.camunda.zeebe.spring.client.annotation.JobWorker;
import io.camunda.zeebe.spring.client.annotation.VariablesAsType;

@Component
public class ErrorLogger {
    @JobWorker(type = "errorLog", fetchVariables = {"responsibility", "employee"})
    public void logError(@VariablesAsType ErrorLog errorLog) {
        LoggerFactory.getLogger(getClass()).error(errorLog.toString());
    }
}
