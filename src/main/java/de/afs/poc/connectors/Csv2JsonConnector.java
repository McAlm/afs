package de.afs.poc.connectors;

import java.io.File;
import java.util.List;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;

import de.afs.poc.dto.Csv2JsonDto;
import de.afs.poc.dto.KommunalesProdukt;
import io.camunda.connector.api.annotation.OutboundConnector;
import io.camunda.connector.api.outbound.OutboundConnectorContext;
import io.camunda.connector.api.outbound.OutboundConnectorFunction;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@OutboundConnector(name = "Csv2Json", inputVariables = { "csvInputFileName" }, type = "de.afs.poc.csv2Json:1")
public class Csv2JsonConnector implements OutboundConnectorFunction {

    @Override

    public Object execute(OutboundConnectorContext context) throws Exception {

        try {
            Csv2JsonDto dto = context.getVariablesAsType(Csv2JsonDto.class);

            // verarbeite zu json
            CsvSchema produkteSchema = CsvSchema.emptySchema().withHeader();
            CsvMapper csvMapper = new CsvMapper();
            MappingIterator<KommunalesProdukt> produkte = csvMapper.readerFor(KommunalesProdukt.class)
                    .with(produkteSchema)
                    .readValues(new File("src/main/resources/" + dto.getCsvInputFileName()));

            List<KommunalesProdukt> readAll = produkte.readAll();

            new ObjectMapper()
                    .configure(SerializationFeature.INDENT_OUTPUT, true)
                    .writeValue(new File("src/main/resources/kommunaleProdukteFromCsv.json"), readAll);
            dto.setJsonOutputFileName("kommunaleProdukteFromCsv.json");
            log.info("Csv2JsonConnector returning " + dto.toString()); 
            return dto;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}
