package com.example.nacosserver.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RefreshScope
@RequestMapping("/config")
public class ProviderController {

    @Value("${value:无返回值}")
    private String value;

    @GetMapping("/getValue")
    public String getValue(){
        String s = "获取Value值：" + value;
        System.out.println(s);
        return s;
    }
}
