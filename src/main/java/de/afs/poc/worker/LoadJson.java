package de.afs.poc.worker;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.functors.TruePredicate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.afs.poc.dto.KommunalesProdukt;
import io.camunda.zeebe.spring.client.annotation.JobWorker;
import io.camunda.zeebe.spring.client.annotation.Variable;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class LoadJson {
    
    @Value("${afs.fips2fips2.fileTransferFolder}")
    private String fileTransferFolder;

    @JobWorker(type="loadJson", fetchVariables = "ausgabeDatei", autoComplete = true)
    public Map<String, Object> loadJson(@Variable String ausgabeDatei) throws Exception{

        log.info("lade JSON aus Datei " + fileTransferFolder + ausgabeDatei);
        try {
            Path path = Paths.get(fileTransferFolder + ausgabeDatei);
            String json = new String(Files.readAllBytes(path));
            // ObjectMapper mapper = new ObjectMapper();
            // ArrayList<KommunalesProdukt> clazz= new ArrayList<KommunalesProdukt>();
            // ArrayList<KommunalesProdukt> readValue = mapper.readValue(json, clazz.getClass());
            log.info("returning json: " + json);
            Map<String, Object> vars = new HashMap<>();
            vars.put("json", json);
            return vars;
        } catch (IOException e) {
            e.printStackTrace();
            throw e;
        }
        
    }
}
