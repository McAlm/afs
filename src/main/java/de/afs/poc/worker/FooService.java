package de.afs.poc.worker;

import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class FooService {

    public void executeFoo(){
        log.info("executing some logic on foo....");
    }
}
