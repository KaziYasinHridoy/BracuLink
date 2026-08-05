package com.braculink.client;

import com.braculink.dto.ConnectJsonSectionDto;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Arrays;
import java.util.List;

@Component
public class ConnectJsonClient {

    private static final String CONNECT_JSON_URL = "https://usis-cdn.eniamza.com/connect.json";

    private final RestClient restClient;

    public ConnectJsonClient(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
    }

    public List<ConnectJsonSectionDto> fetchSections() {
        ConnectJsonSectionDto[] sections = restClient.get()
                .uri(CONNECT_JSON_URL)
                .retrieve()
                .body(ConnectJsonSectionDto[].class);
        return sections != null ? Arrays.asList(sections) : List.of();
    }
}
