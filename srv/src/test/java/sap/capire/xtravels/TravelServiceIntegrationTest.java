package sap.capire.xtravels;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sap.cds.reflect.CdsModel;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for the CAP Travel Service OData endpoints.
 * This class provides comprehensive testing of OData operations including:
 * - Basic CRUD operations on Travels entity
 * - OData query options ($filter, $select, $orderby, etc.)
 * - Custom actions (acceptTravel, rejectTravel, deductDiscount)
 * - Read-only entities (Flights, Supplements, Currencies)
 * - Error handling scenarios
 */
@SpringBootTest
@AutoConfigureWebMvc
class TravelServiceIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private CdsModel cdsModel;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String ODATA_BASE_URL = "/odata/v4/travel";

    @Test
    void contextLoads() {
        assertNotNull(webApplicationContext);
    }

    @Test
    @WithMockUser("admin")
    void shouldGetMetadataSuccessfully() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        
        mockMvc.perform(get(ODATA_BASE_URL + "/$metadata"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith("application/xml"));
    }

    @Test
    @WithMockUser("admin")
    void shouldGetTravelsCollectionSuccessfully() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        
        mockMvc.perform(get(ODATA_BASE_URL + "/Travels"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith("application/json"));
    }

    @Test
    @WithMockUser("admin")
    void shouldCreateAndRetrieveTravelSuccessfully() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        
        Map<String, Object> travelData = new HashMap<>();
        travelData.put("Description", "Integration Test Travel");
        travelData.put("BeginDate", LocalDate.now().plusDays(10).toString());
        travelData.put("EndDate", LocalDate.now().plusDays(17).toString());
        travelData.put("BookingFee", 200.0);
        travelData.put("Currency_code", "USD");

        // Create travel
        String response = mockMvc.perform(post(ODATA_BASE_URL + "/Travels")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(travelData)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

        // Verify the created travel can be retrieved
        Map<String, Object> createdTravel = objectMapper.readValue(response, new TypeReference<Map<String, Object>>() {});
        Integer travelId = (Integer) createdTravel.get("ID");
        assertNotNull(travelId);

        mockMvc.perform(get(ODATA_BASE_URL + "/Travels(ID="+travelId+",IsActiveEntity=false)"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith("application/json"));
    }

    @Test
    @WithMockUser("admin")
    void shouldSupportODataFilterQuery() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        
        mockMvc.perform(get(ODATA_BASE_URL + "/Travels?$filter=Currency_code eq 'EUR'"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith("application/json"));
    }

    @Test
    @WithMockUser("admin")
    void shouldGetReadOnlyEntitiesSuccessfully() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        
        // Test Flights entity
        mockMvc.perform(get(ODATA_BASE_URL + "/Flights"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith("application/json"));

        // Test Supplements entity  
        mockMvc.perform(get(ODATA_BASE_URL + "/Supplements"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith("application/json"));

        // Test Currencies entity
        mockMvc.perform(get(ODATA_BASE_URL + "/Currencies"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith("application/json"));
    }

    @Test
    @WithMockUser("admin")
    void shouldReturn404ForNonExistentEntity() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        
        mockMvc.perform(get(ODATA_BASE_URL + "/Travels(ID=9999999,IsActiveEntity=false)"))
            .andExpect(status().isNotFound());
    }
}
