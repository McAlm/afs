package de.afs.poc.dto;

import lombok.Data;

@Data
public class Csv2JsonDto {

    private String csvInputFileName;
    private String jsonOutputFileName;
}
