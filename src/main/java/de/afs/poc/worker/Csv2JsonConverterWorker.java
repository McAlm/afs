package de.afs.poc.worker;

import java.io.File;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;

import de.afs.poc.dto.Csv2JsonDto;
import de.afs.poc.dto.KommunalesProdukt;
import io.camunda.zeebe.spring.client.annotation.JobWorker;
import io.camunda.zeebe.spring.client.annotation.VariablesAsType;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class Csv2JsonConverterWorker {

    @Value("${afs.fips2fips2.fileTransferFolder}")
    private String fileTransferFolder;

    @JobWorker(type = "csv2Json", fetchVariables = "csvInputFileName", autoComplete = true)
    public Csv2JsonDto convert(@VariablesAsType Csv2JsonDto dto) throws Exception{
        try {

            // verarbeite zu json
            CsvSchema produkteSchema = CsvSchema.emptySchema().withHeader();
            CsvMapper csvMapper = new CsvMapper();
            MappingIterator<KommunalesProdukt> produkte = csvMapper.readerFor(KommunalesProdukt.class)
                    .with(produkteSchema)
                    .readValues(new File(fileTransferFolder + dto.getCsvInputFileName()));

            List<KommunalesProdukt> readAll = produkte.readAll();

            new ObjectMapper()
                    .configure(SerializationFeature.INDENT_OUTPUT, true)
                    .writeValue(new File(fileTransferFolder + "kommunaleProdukteFromCsv.json"), readAll);
            dto.setJsonOutputFileName("kommunaleProdukteFromCsv.json");
            log.info("Csv2JsonConnector returning " + dto.toString()); 
            return dto;
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }

    }
}
