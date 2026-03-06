package com.agentscustomerstickets;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

// Security-focused integration test: starts the Spring application context and uses MockMvc to
// exercise the HTTP layer + Spring Security filter chain (expecting 401/403 where appropriate).
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityIntegrationTest {

        @Autowired
        MockMvc mockMvc;

        @Test
        void meRequiresAuthentication() throws Exception {
                mockMvc.perform(get("/api/me"))
                                .andExpect(status().isUnauthorized());
        }

        @Test
        void nonAdminCannotCreateAgent() throws Exception {
                String tokenBody = mockMvc.perform(post("/api/auth/token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"username\":\"admin\",\"password\":\"admin123\"}"))
                                .andExpect(status().isOk())
                                .andReturn()
                                .getResponse()
                                .getContentAsString();

                String token = tokenBody.replaceAll(".*\\\"access_token\\\"\\s*:\\s*\\\"([^\\\"]+)\\\".*", "$1");

                mockMvc.perform(post("/api/agents")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                                "{\"username\":\"agent1\",\"password\":\"password123\",\"fullName\":\"Agent One\",\"email\":\"agent1@example.com\"}"))
                                .andExpect(status().isCreated());

                String agentTokenBody = mockMvc.perform(post("/api/auth/token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"username\":\"agent1\",\"password\":\"password123\"}"))
                                .andExpect(status().isOk())
                                .andReturn()
                                .getResponse()
                                .getContentAsString();

                String agentToken = agentTokenBody.replaceAll(".*\\\"access_token\\\"\\s*:\\s*\\\"([^\\\"]+)\\\".*",
                                "$1");

                mockMvc.perform(post("/api/agents")
                                .header("Authorization", "Bearer " + agentToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                                "{\"username\":\"agent2\",\"password\":\"password123\",\"fullName\":\"Agent Two\",\"email\":\"agent2@example.com\"}"))
                                .andExpect(status().isForbidden());
        }

        @Test
        void customerCanAccessOnlyOwnTickets() throws Exception {
                // Authenticate as admin
                String adminTokenBody = mockMvc.perform(post("/api/auth/token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"username\":\"admin\",\"password\":\"admin123\"}"))
                                .andExpect(status().isOk())
                                .andReturn()
                                .getResponse()
                                .getContentAsString();

                String adminToken = adminTokenBody.replaceAll(".*\\\"access_token\\\"\\s*:\\s*\\\"([^\\\"]+)\\\".*",
                                "$1");

                // Create an agent
                var agentCreateResponse = mockMvc.perform(post("/api/agents")
                                .header("Authorization", "Bearer " + adminToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                                "{\"username\":\"agent3\",\"password\":\"password123\",\"fullName\":\"Agent Three\",\"email\":\"agent3@example.com\"}"))
                                .andExpect(status().isCreated())
                                .andReturn()
                                .getResponse()
                                .getContentAsString();

                // Extract agent ID from response
                String agentId = agentCreateResponse.replaceAll(".*\\\"id\\\"\\s*:\\s*(\\d+).*", "$1");

                // Authenticate as agent
                String agentTokenBody = mockMvc.perform(post("/api/auth/token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"username\":\"agent3\",\"password\":\"password123\"}"))
                                .andExpect(status().isOk())
                                .andReturn()
                                .getResponse()
                                .getContentAsString();

                String agentToken = agentTokenBody.replaceAll(".*\\\"access_token\\\"\\s*:\\s*\\\"([^\\\"]+)\\\".*",
                                "$1");

                // Create a customer under this agent
                mockMvc.perform(post("/api/customers")
                                .header("Authorization", "Bearer " + agentToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                                "{\"username\":\"customer1\",\"password\":\"password123\",\"fullName\":\"Customer One\",\"email\":\"customer1@example.com\"}"))
                                .andExpect(status().isCreated());

                // Authenticate as customer
                String customerTokenBody = mockMvc.perform(post("/api/auth/token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"username\":\"customer1\",\"password\":\"password123\"}"))
                                .andExpect(status().isOk())
                                .andReturn()
                                .getResponse()
                                .getContentAsString();

                String customerToken = customerTokenBody.replaceAll(
                                ".*\\\"access_token\\\"\\s*:\\s*\\\"([^\\\"]+)\\\".*",
                                "$1");

                // Customer can access /api/tickets and should see an empty list (no tickets
                // created)
                mockMvc.perform(get("/api/tickets")
                                .header("Authorization", "Bearer " + customerToken))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$").isArray())
                                .andExpect(jsonPath("$.length()").value(0));

                // Customer should NOT be able to filter by agentId to access other's tickets
                mockMvc.perform(get("/api/tickets?agentId=" + agentId)
                                .header("Authorization", "Bearer " + customerToken))
                                .andExpect(status().isForbidden());
        }
}