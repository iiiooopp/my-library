package com.example.customer.service;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestMapping;

@FeignClient("single-provider")
public interface IHelloService {

    @RequestMapping(value = "/hello")
    String hello();

    @RequestMapping(value = "nice")
    String nice();
}
