package com.anurag.cse;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PersonalfinancetrackerApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void contextLoads() {
	}

	@Test
	void servesFinanceDashboardFromRoot() throws Exception {
		mockMvc.perform(get("/"))
				.andExpect(status().isOk())
				.andExpect(forwardedUrl("index.html"));
	}

	@Test
	void servesActualFinanceDashboardDocument() throws Exception {
		mockMvc.perform(get("/index.html"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Wealth & Expense Intelligence")))
				.andExpect(content().string(containsString("Your money, in focus.")))
				.andExpect(content().string(containsString("id=\"appContainer\"")))
				.andExpect(content().string(containsString("`http://${window.location.hostname}:8081/api`")))
				.andExpect(content().string(containsString(": '/api'")))
				.andExpect(content().string(containsString("id=\"paymentRequestForm\"")));
	}

	@Test
	void servesDashboardStylesAndScripts() throws Exception {
		mockMvc.perform(get("/css/styles.css"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString(".app-container")));
		mockMvc.perform(get("/js/auth.js"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("window.FINANCE_API_BASE_URL")));
		mockMvc.perform(get("/assets/ai-bot-logo.svg"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("<svg")));
	}

	@Test
	void identifiesTheFinanceDashboardHealthEndpoint() throws Exception {
		mockMvc.perform(get("/health"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("\"application\":\"Wealth & Expense Intelligence\"")));
	}

}
