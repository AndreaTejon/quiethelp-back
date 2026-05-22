package com.trinitarias.quiethelp.components;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
public class SupabaseClient {

    @Value("${app.supabase.url}")
    private String supabaseUrl;

    @Value("${app.supabase.api-key}")
    private String supabaseKey;

    private final RestTemplate restTemplate = new RestTemplate();

    /* Valida si un token existe */
    public boolean validarToken(String token) {
        try {
            String url = supabaseUrl + "/rest/v1/alumnos_tokens?token=eq."
                    + java.net.URLEncoder.encode(token, java.nio.charset.StandardCharsets.UTF_8)
                    + "&select=*";

            System.out.println("Validando token: " + token);
            System.out.println("URL Supabase: " + url);

            HttpHeaders headers = new HttpHeaders();
            headers.set("apikey", supabaseKey);
            headers.set("Authorization", "Bearer " + supabaseKey);

            HttpEntity<?> entity = new HttpEntity<>(headers);

            ResponseEntity<Map<String, Object>[]> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<Map<String, Object>[]>() {}
            );

            Map<String, Object>[] resultados = response.getBody();

            System.out.println("Tokens encontrados: " + (resultados == null ? 0 : resultados.length));

            return resultados != null && resultados.length > 0;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}