package de.afs.poc.worker;

import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class FooService {

    public String executeFoo(String foo){
        log.info("executing some logic on foo....");
        return foo + "bar";
    }
}
