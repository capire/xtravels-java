package sap.capire.xtravels;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@SpringBootTest
@AutoConfigureWebMvc
class TravelServiceODataTest {
    /*

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;





    // ========== Service Document and Metadata Tests ==========



    @Test
    void shouldGetTravelById() throws Exception {
        // First create a travel to ensure we have data
        Map<String, Object> travelData = createTravelData();
        
        String response = mockMvc.perform(post(TRAVELS_ENDPOINT)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(travelData)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
        
        Map<String, Object> createdTravel = objectMapper.readValue(response, Map.class);
        Integer travelId = (Integer) createdTravel.get("ID");

        // Now get the travel by ID
        mockMvc.perform(get(TRAVELS_ENDPOINT + "(" + travelId + ")"))
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/json;charset=UTF-8"))
            .andExpect(jsonPath("$.ID", is(travelId)))
            .andExpect(jsonPath("$.Description", is(travelData.get("Description"))));
    }

    @Test
    void shouldCreateTravel() throws Exception {
        Map<String, Object> travelData = createTravelData();

        mockMvc.perform(post(TRAVELS_ENDPOINT)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(travelData)))
            .andExpect(status().isCreated())
            .andExpect(content().contentType("application/json;charset=UTF-8"))
            .andExpect(jsonPath("$.Description", is(travelData.get("Description"))))
            .andExpect(jsonPath("$.BeginDate", is(travelData.get("BeginDate"))))
            .andExpect(jsonPath("$.EndDate", is(travelData.get("EndDate"))))
            .andExpect(jsonPath("$.BookingFee", is(travelData.get("BookingFee"))))
            .andExpect(jsonPath("$.Currency_code", is(travelData.get("Currency_code"))))
            .andExpect(jsonPath("$.ID", notNullValue()));
    }

    @Test
    void shouldUpdateTravel() throws Exception {
        // First create a travel
        Map<String, Object> travelData = createTravelData();
        String response = mockMvc.perform(post(TRAVELS_ENDPOINT)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(travelData)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
        
        Map<String, Object> createdTravel = objectMapper.readValue(response, Map.class);
        Integer travelId = (Integer) createdTravel.get("ID");

        // Update the travel
        Map<String, Object> updateData = new HashMap<>();
        updateData.put("Description", "Updated Travel Description");
        updateData.put("BookingFee", 150.0);

        mockMvc.perform(patch(TRAVELS_ENDPOINT + "(" + travelId + ")")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(updateData)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.Description", is("Updated Travel Description")))
            .andExpect(jsonPath("$.BookingFee", is(150.0)));
    }

    @Test
    void shouldDeleteTravel() throws Exception {
        // First create a travel
        Map<String, Object> travelData = createTravelData();
        String response = mockMvc.perform(post(TRAVELS_ENDPOINT)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(travelData)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
        
        Map<String, Object> createdTravel = objectMapper.readValue(response, Map.class);
        Integer travelId = (Integer) createdTravel.get("ID");

        // Delete the travel
        mockMvc.perform(delete(TRAVELS_ENDPOINT + "(" + travelId + ")"))
            .andExpect(status().isNoContent());

        // Verify it's deleted
        mockMvc.perform(get(TRAVELS_ENDPOINT + "(" + travelId + ")"))
            .andExpect(status().isNotFound());
    }

    // ========== OData Query Options Tests ==========

    @Test
    void shouldSupportFilterQuery() throws Exception {
        mockMvc.perform(get(TRAVELS_ENDPOINT + "?$filter=Currency_code eq 'EUR'"))
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/json;charset=UTF-8"))
            .andExpect(jsonPath("$.value", isA(java.util.List.class)));
    }

    @Test
    void shouldSupportSelectQuery() throws Exception {
        mockMvc.perform(get(TRAVELS_ENDPOINT + "?$select=ID,Description,Currency_code"))
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/json;charset=UTF-8"))
            .andExpect(jsonPath("$.value", isA(java.util.List.class)));
    }

    @Test
    void shouldSupportOrderByQuery() throws Exception {
        mockMvc.perform(get(TRAVELS_ENDPOINT + "?$orderby=Description desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/json;charset=UTF-8"))
            .andExpect(jsonPath("$.value", isA(java.util.List.class)));
    }

    @Test
    void shouldSupportTopAndSkipQuery() throws Exception {
        mockMvc.perform(get(TRAVELS_ENDPOINT + "?$top=5&$skip=0"))
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/json;charset=UTF-8"))
            .andExpect(jsonPath("$.value", isA(java.util.List.class)));
    }

    @Test
    void shouldSupportCountQuery() throws Exception {
        mockMvc.perform(get(TRAVELS_ENDPOINT + "/$count"))
            .andExpect(status().isOk())
            .andExpect(content().contentType("text/plain;charset=UTF-8"))
            .andExpect(content().string(matchesRegex("\\d+")));
    }

    // ========== Travel Actions Tests ==========

    @Test
    void shouldExecuteAcceptTravelAction() throws Exception {
        // First create a travel
        Map<String, Object> travelData = createTravelData();
        String response = mockMvc.perform(post(TRAVELS_ENDPOINT)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(travelData)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
        
        Map<String, Object> createdTravel = objectMapper.readValue(response, Map.class);
        Integer travelId = (Integer) createdTravel.get("ID");

        // Execute acceptTravel action
        mockMvc.perform(post(TRAVELS_ENDPOINT + "(" + travelId + ")/TravelService.acceptTravel")
                .contentType("application/json")
                .content("{}"))
            .andExpect(status().isOk());
    }

    @Test
    void shouldExecuteRejectTravelAction() throws Exception {
        // First create a travel
        Map<String, Object> travelData = createTravelData();
        String response = mockMvc.perform(post(TRAVELS_ENDPOINT)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(travelData)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
        
        Map<String, Object> createdTravel = objectMapper.readValue(response, Map.class);
        Integer travelId = (Integer) createdTravel.get("ID");

        // Execute rejectTravel action
        mockMvc.perform(post(TRAVELS_ENDPOINT + "(" + travelId + ")/TravelService.rejectTravel")
                .contentType("application/json")
                .content("{}"))
            .andExpect(status().isOk());
    }

    @Test
    void shouldExecuteDeductDiscountAction() throws Exception {
        // First create a travel
        Map<String, Object> travelData = createTravelData();
        String response = mockMvc.perform(post(TRAVELS_ENDPOINT)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(travelData)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
        
        Map<String, Object> createdTravel = objectMapper.readValue(response, Map.class);
        Integer travelId = (Integer) createdTravel.get("ID");

        // Execute deductDiscount action with 10% discount
        Map<String, Object> actionParams = new HashMap<>();
        actionParams.put("percent", 10);

        mockMvc.perform(post(TRAVELS_ENDPOINT + "(" + travelId + ")/TravelService.deductDiscount")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(actionParams)))
            .andExpect(status().isOk());
    }

    // ========== Read-only Entities Tests ==========

    @Test
    void shouldGetAllFlights() throws Exception {
        mockMvc.perform(get(FLIGHTS_ENDPOINT))
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/json;charset=UTF-8"))
            .andExpect(jsonPath("$.@odata.context", containsString("Flights")))
            .andExpect(jsonPath("$.value", isA(java.util.List.class)));
    }

    @Test
    void shouldGetAllSupplements() throws Exception {
        mockMvc.perform(get(SUPPLEMENTS_ENDPOINT))
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/json;charset=UTF-8"))
            .andExpect(jsonPath("$.@odata.context", containsString("Supplements")))
            .andExpect(jsonPath("$.value", isA(java.util.List.class)));
    }

    @Test
    void shouldGetAllCurrencies() throws Exception {
        mockMvc.perform(get(CURRENCIES_ENDPOINT))
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/json;charset=UTF-8"))
            .andExpect(jsonPath("$.@odata.context", containsString("Currencies")))
            .andExpect(jsonPath("$.value", isA(java.util.List.class)));
    }

    @Test
    void shouldNotAllowPostToReadOnlyFlights() throws Exception {
        Map<String, Object> flightData = new HashMap<>();
        flightData.put("PlaneType", "Boeing 747");

        mockMvc.perform(post(FLIGHTS_ENDPOINT)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(flightData)))
            .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void shouldNotAllowPutToReadOnlySupplements() throws Exception {
        Map<String, Object> supplementData = new HashMap<>();
        supplementData.put("Description", "Premium Meal");

        mockMvc.perform(put(SUPPLEMENTS_ENDPOINT + "(1)")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(supplementData)))
            .andExpect(status().isMethodNotAllowed());
    }

    // ========== Error Handling Tests ==========

    @Test
    void shouldReturn400ForInvalidTravelData() throws Exception {
        Map<String, Object> invalidData = new HashMap<>();
        invalidData.put("BeginDate", "invalid-date");

        mockMvc.perform(post(TRAVELS_ENDPOINT)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(invalidData)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn404ForNonExistentTravel() throws Exception {
        mockMvc.perform(get(TRAVELS_ENDPOINT + "(99999)"))
            .andExpect(status().isNotFound());
    }



    // ========== Helper Methods ==========
     */
}
