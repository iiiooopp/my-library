package com.example.nacosconsumer.error;

import com.example.nacosconsumer.server.ProviderServer;
import org.springframework.stereotype.Component;

@Component
public class FeignError implements ProviderServer {

    @Override
    public String getValue() {
        return "服务器维护中....";
    }
}
