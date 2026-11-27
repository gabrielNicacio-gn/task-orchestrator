package com.api.task_orchestrator.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;


@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:order_creation_tests;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@AutoConfigureMockMvc
class OrderCreationIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clearDatabase() {
        jdbcTemplate.update("DELETE FROM task");
        jdbcTemplate.update("DELETE FROM customer_order");
        jdbcTemplate.update("DELETE FROM operation");
    }

    @Test
    void createsEachOrderWithAnAvailablePickingTask() throws Exception {
        UUID operationId = createOperation(3);

        mockMvc.perform(post("/api/operation/{operationId}/orders", operationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderBatch("ORD-001", "ORD-002")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].orderNumber").value("ORD-001"))
                .andExpect(jsonPath("$[0].tasks.length()").value(1))
                .andExpect(jsonPath("$[0].tasks[0].phase").value("PICKING"))
                .andExpect(jsonPath("$[0].tasks[0].status").value("AVAILABLE"))
                .andExpect(jsonPath("$[1].orderNumber").value("ORD-002"))
                .andExpect(jsonPath("$[1].tasks[0].phase").value("PICKING"));

        assertEquals(2, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM customer_order", Integer.class));
        assertEquals(2, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM task", Integer.class));
    }

    @Test
    void rejectsBatchThatWouldExceedOperationCapacityWithoutPartialCreation() throws Exception {
        UUID operationId = createOperation(1);
        postOrderBatch(operationId, orderBatch("ORD-101"))
                .andExpect(status().isCreated());

        postOrderBatch(operationId, orderBatch("ORD-102"))
                .andExpect(status().isConflict());

        assertEquals(1, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM customer_order", Integer.class));
        assertEquals(1, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM task", Integer.class));
    }

    @Test
    void repeatedBatchDoesNotCreateDuplicateOrdersOrTasks() throws Exception {
        UUID operationId = createOperation(2);
        String requestBody = orderBatch("ORD-201");

        MvcResult firstResult = postOrderBatch(operationId, requestBody)
                .andExpect(status().isCreated())
                .andReturn();
        MvcResult repeatedResult = postOrderBatch(operationId, requestBody)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].tasks[0].status").value("AVAILABLE"))
                .andReturn();

        String firstOrderId = jsonStringField(firstResult.getResponse().getContentAsString(), "id");
        String repeatedOrderId = jsonStringField(repeatedResult.getResponse().getContentAsString(), "id");
        assertEquals(firstOrderId, repeatedOrderId);
        assertEquals(1, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM customer_order", Integer.class));
        assertEquals(1, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM task", Integer.class));
    }

    @Test
    void sameExternalOrderNumberCanBeUsedInDifferentOperations() throws Exception {
        UUID firstOperationId = createOperation(1);
        UUID secondOperationId = createOperation(1);

        MvcResult first = postOrderBatch(firstOperationId, orderBatch("ORD-301"))
                .andExpect(status().isCreated())
                .andReturn();
        MvcResult second = postOrderBatch(secondOperationId, orderBatch("ORD-301"))
                .andExpect(status().isCreated())
                .andReturn();

        String firstOrderId = jsonStringField(first.getResponse().getContentAsString(), "id");
        String secondOrderId = jsonStringField(second.getResponse().getContentAsString(), "id");
        assertNotEquals(firstOrderId, secondOrderId);
        assertEquals(2, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM customer_order", Integer.class));
    }

    private UUID createOperation(int capacity) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/operation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "description": "Order test",
                                  "orderCapacity": %d,
                                  "deadline": "2035-06-01T12:00:00Z"
                                }
                                """.formatted(capacity)))
                .andExpect(status().isCreated())
                .andReturn();

        return UUID.fromString(jsonStringField(result.getResponse().getContentAsString(), "idOperation"));
    }

    private org.springframework.test.web.servlet.ResultActions postOrderBatch(UUID operationId, String body)
            throws Exception {
        return mockMvc.perform(post("/api/operation/{operationId}/orders", operationId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private String orderBatch(String... orderNumbers) {
        String orders = java.util.Arrays.stream(orderNumbers)
                .map(orderNumber -> "{\"orderNumber\":\"" + orderNumber
                        + "\",\"orderDate\":\"2035-06-01T12:00:00Z\"}")
                .collect(java.util.stream.Collectors.joining(","));
        return "{\"orders\":[" + orders + "]}";
    }

    private String jsonStringField(String json, String fieldName) {
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("\\\"" + java.util.regex.Pattern.quote(fieldName) + "\\\":\\\"([^\\\"]+)\\\"")
                .matcher(json);
        if (!matcher.find()) {
            throw new AssertionError("JSON response does not contain string field " + fieldName + ": " + json);
        }
        return matcher.group(1);
    }
}
