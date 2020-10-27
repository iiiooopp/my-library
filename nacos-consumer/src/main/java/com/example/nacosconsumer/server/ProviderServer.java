package com.example.nacosconsumer.server;

import com.example.nacosconsumer.error.FeignError;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Primary;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@FeignClient(value = "nacos-server", fallback = FeignError.class)
@Primary
public interface ProviderServer {

    @GetMapping("/config/getValue")
    public String getValue();

}
