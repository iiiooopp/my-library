package com.example.nacosconsumer.controller;

import com.example.nacosconsumer.server.ProviderServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/config")
public class ConsumerController {

    /**
     * 动态代理对象，内部远程调用服务生产者
     */
    @Autowired
    private ProviderServer providerServer;


    @GetMapping("/getValue")
    public String getValue() {
        System.out.println("远程调用getValue");
        //远程调用
        String value = providerServer.getValue();
        return value;
    }

}
