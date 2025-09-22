package com.example.sqltask;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@SpringBootApplication
public class SqlTaskApplication implements CommandLineRunner {

    public static void main(String[] args) {
        SpringApplication.run(SqlTaskApplication.class, args);
    }

    @Override
    public void run(String... args) {
        RestTemplate restTemplate = new RestTemplate();
        try {
            // 1) Generate webhook
            String generateUrl = "https://bfhldevapigw.healthrx.co.in/hiring/generateWebhook/JAVA";

            Map<String, String> req = new HashMap<>();
            req.put("name", "Ved Vishwakarma");
            req.put("regNo", "0002CD221065"); // change if needed (odd → Q1, even → Q2)
            req.put("email", "veduvishwa04@gmail.com");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, String>> entity = new HttpEntity<>(req, headers);

            ResponseEntity<Map> resp = restTemplate.postForEntity(generateUrl, entity, Map.class);
            if (resp == null || resp.getBody() == null) {
                System.out.println("generateWebhook returned null");
                System.exit(0);
            }
            Map body = resp.getBody();
            String webhookUrl = body.get("webhook") != null ? body.get("webhook").toString() : null;
            String accessToken = body.get("accessToken") != null ? body.get("accessToken").toString() : null;

            System.out.println("webhookUrl: " + webhookUrl);
            System.out.println("accessToken present: " + (accessToken != null));

            if (webhookUrl == null || accessToken == null) {
                System.out.println("Missing webhook or accessToken; aborting.");
                System.exit(0);
            }

            // 2) Final SQL query (Question 1 — adjust if you need Q2)
            String finalQuery = "SELECT d.DEPARTMENT_NAME, COUNT(DISTINCT e.EMP_ID) AS TOTAL_EMPLOYEES, " +
                    "ROUND(AVG(p.AMOUNT),2) AS AVG_SALARY " +
                    "FROM DEPARTMENT d " +
                    "JOIN EMPLOYEE e ON d.DEPARTMENT_ID = e.DEPARTMENT " +
                    "JOIN PAYMENTS p ON e.EMP_ID = p.EMP_ID " +
                    "GROUP BY d.DEPARTMENT_NAME;";

            // 3) Submit final query to webhook with JWT in Authorization header
            HttpHeaders submitHeaders = new HttpHeaders();
            submitHeaders.setContentType(MediaType.APPLICATION_JSON);
            submitHeaders.setBearerAuth(accessToken);

            Map<String, String> submitBody = new HashMap<>();
            submitBody.put("finalQuery", finalQuery);

            HttpEntity<Map<String, String>> submitEntity = new HttpEntity<>(submitBody, submitHeaders);

            ResponseEntity<String> submitResp = restTemplate.postForEntity(webhookUrl, submitEntity, String.class);
            System.out.println("Submission status: " + (submitResp != null ? submitResp.getStatusCode() : "null"));
            System.out.println("Submission response body: " + (submitResp != null ? submitResp.getBody() : "null"));

        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            // make sure the app exits (important for CI runs)
            System.exit(0);
        }
    }
}
