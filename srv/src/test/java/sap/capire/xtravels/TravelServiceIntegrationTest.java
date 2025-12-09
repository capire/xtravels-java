package sap.capire.xtravels;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
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

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isA;
import static org.hamcrest.Matchers.matchesRegex;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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

    private static final String ODATA_BASE_URL = "/odata/v4/travel";
    private static final String TRAVELS_ENDPOINT = ODATA_BASE_URL + "/Travels";

    private static int testCounter = 0; // for test-data generation

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .build();
    }

    @Test
    void contextLoads() {
        assertNotNull(webApplicationContext);
    }

    @Test
    @WithMockUser("admin")
    void shouldGetMetadataSuccessfully() throws Exception {

        mockMvc.perform(get(ODATA_BASE_URL + "/$metadata"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith("application/xml"));
    }

    // ========== Travels Entity Tests ==========

    @Test
    @WithMockUser("admin")
    void shouldGetAllTravels() throws Exception {

        mockMvc.perform(get(TRAVELS_ENDPOINT))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.@context", containsString("Travels")))
                .andExpect(jsonPath("$.value", isA(java.util.List.class)));
    }

    @Test
    @WithMockUser("admin")
    void shouldCreateTravel() throws Exception {
        Map<String, Object> travelData = createTravelData("shouldCreateTravel");

        mockMvc.perform(post(TRAVELS_ENDPOINT)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(travelData)))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.Description", is(travelData.get("Description"))))
                .andExpect(jsonPath("$.BeginDate", is(travelData.get("BeginDate"))))
                .andExpect(jsonPath("$.EndDate", is(travelData.get("EndDate"))))
                .andExpect(jsonPath("$.BookingFee", is(travelData.get("BookingFee"))))
                .andExpect(jsonPath("$.Currency_code", is(travelData.get("Currency_code"))))
                .andExpect(jsonPath("$.ID", notNullValue()));
    }

    @Test
    @WithMockUser("admin")
    void shouldCreateAndRetrieveTravelSuccessfully() throws Exception {

        Map<String, Object> travelData = createTravelData("shouldCreateAndRetrieveTravelSuccessfully");
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

        mockMvc.perform(get(ODATA_BASE_URL + "/Travels(ID="+travelId+",IsActiveEntity=true)"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith("application/json"))
            .andExpect(jsonPath("$.BookingFee", is(travelData.get("BookingFee"))))
            .andExpect(jsonPath("$.Currency_code", is(travelData.get("Currency_code"))));
    }

    @Test
    @WithMockUser("admin")
    void shouldSupportODataFilterQuery() throws Exception {

        mockMvc.perform(get(ODATA_BASE_URL + "/Travels?$filter=Currency_code eq 'EUR'&$top=1&$skip=0"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.value[0].Currency_code", is("EUR")));
    }

    @Test
    @WithMockUser("admin")
    void shouldSupportSelectQuery() throws Exception {
        mockMvc.perform(get(TRAVELS_ENDPOINT + "?$select=ID,Description,Currency_code"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json;charset=UTF-8"))
                .andExpect(jsonPath("$.value", isA(java.util.List.class)));
    }

    @Test
    @WithMockUser("admin")
    void shouldSupportOrderByQuery() throws Exception {

        mockMvc.perform(get(TRAVELS_ENDPOINT + "?$orderby=Description desc&$top=3&$skip=0"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json;charset=UTF-8"))
                .andExpect(jsonPath("$.value[0].Description", is("Zazzing the Cuba")))
                .andExpect(jsonPath("$.value[1].Description", is("Watching Walter")))
                .andExpect(jsonPath("$.value[2].Description", is("Visiting Walter")));
    }

    @Test
    @WithMockUser("admin")
    void shouldSupportTopAndSkipQuery() throws Exception {
        mockMvc.perform(get(TRAVELS_ENDPOINT + "?$top=5&$skip=0"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json;charset=UTF-8"))
                .andExpect(jsonPath("$.value", isA(java.util.List.class)));
    }

    @Test
    @WithMockUser("admin")
    void shouldSupportCountQuery() throws Exception {
        mockMvc.perform(get(TRAVELS_ENDPOINT + "/$count"))
                .andExpect(status().isOk())
                .andExpect(content().string(matchesRegex("\\d+")));
    }

    @Test
    @WithMockUser("admin")
    void shouldGetReadOnlyEntitiesSuccessfully() throws Exception {

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

        mockMvc.perform(get(ODATA_BASE_URL + "/Travels(ID=9999999,IsActiveEntity=false)"))
            .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser("admin")
    void shouldReturn400ForInvalidDiscountPercentage() throws Exception {
        // First create a travel
        Map<String, Object> travelData = createTravelData("shouldReturn400ForInvalidDiscountPercentage");
        String response = mockMvc.perform(post(TRAVELS_ENDPOINT)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(travelData)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Map<String, Object> createdTravel = objectMapper.readValue(response, Map.class);
        Integer travelId = (Integer) createdTravel.get("ID");

        // Try to execute deductDiscount action with invalid percentage (>100)
        Map<String, Object> actionParams = new HashMap<>();
        actionParams.put("percent", 150); // Invalid percentage

        mockMvc.perform(post(TRAVELS_ENDPOINT + "(ID=" + travelId + ",IsActiveEntity=true)/TravelService.deductDiscount")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(actionParams)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser("admin")
    void shouldDeleteTravel() throws Exception {
        // First create a travel
        Map<String, Object> travelData = createTravelData("shouldDeleteTravel");
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
        mockMvc.perform(delete(TRAVELS_ENDPOINT + "(ID=" + travelId + ",IsActiveEntity=true)"))
                .andExpect(status().isNoContent());

        // Verify it's deleted
        mockMvc.perform(get(TRAVELS_ENDPOINT + "(ID=" + travelId + ",IsActiveEntity=true)"))
                .andExpect(status().isNotFound());
    }


    @Test
    @WithMockUser("admin")
    void shouldExecuteAcceptTravelAction() throws Exception {
        // First create a travel
        Map<String, Object> travelData = createTravelData("shouldExecuteAcceptTravelAction");
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
        mockMvc.perform(post(TRAVELS_ENDPOINT + "(ID=" + travelId + ",IsActiveEntity=true)/TravelService.acceptTravel")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().is2xxSuccessful());

        // Check if travel status is accepted
        mockMvc.perform(get(ODATA_BASE_URL + "/Travels(ID="+travelId+",IsActiveEntity=true)"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.Status_code").value("A"));
    }

    @Test
    @WithMockUser("admin")
    void shouldExecuteRejectTravelAction() throws Exception {
        // First create a travel
        Map<String, Object> travelData = createTravelData("shouldExecuteRejectTravelAction");
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
        mockMvc.perform(post(TRAVELS_ENDPOINT + "(ID=" + travelId + ",IsActiveEntity=true)/TravelService.rejectTravel")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().is2xxSuccessful());

        // Check if travel status is rejected
        mockMvc.perform(get(ODATA_BASE_URL + "/Travels(ID="+travelId+",IsActiveEntity=true)"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.Status_code").value("X"));
    }

    @Test
    @WithMockUser("admin")
    void shouldExecuteDeductDiscountAction() throws Exception {
        // First create a travel
        Map<String, Object> travelData = createTravelData("shouldExecuteDeductDiscountAction");

        String response = mockMvc.perform(post(TRAVELS_ENDPOINT)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(travelData)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Map<String, Object> createdTravel = objectMapper.readValue(response, Map.class);
        Integer travelId = (Integer) createdTravel.get("ID");
        Integer deduction = 10;

        // Execute deductDiscount action with 10% discount
        Map<String, Object> actionParams = new HashMap<>();
        actionParams.put("percent", deduction);

        mockMvc.perform(post(TRAVELS_ENDPOINT + "(ID=" + travelId + ",IsActiveEntity=true)/TravelService.deductDiscount")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(actionParams)))
                .andExpect(status().isOk());

        mockMvc.perform(get(ODATA_BASE_URL + "/Travels(ID="+travelId+",IsActiveEntity=true)"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.BookingFee").value(90));
    }

    private Map<String, Object> createTravelData(String testName) {
        synchronized (TravelServiceIntegrationTest.class) {
            testCounter++;
        }

        Map<String, Object> travelData = new HashMap<>();
        travelData.put("IsActiveEntity", true);
        travelData.put("Description", testName + " - Test Travel " + testCounter + " to Paris");
        travelData.put("BeginDate", LocalDate.now().plusDays(30 + testCounter).toString());
        travelData.put("EndDate", LocalDate.now().plusDays(37 + testCounter).toString());
        travelData.put("BookingFee", 100);
        travelData.put("Currency_code", "EUR");
        travelData.put("Agency_ID", "070001");
        travelData.put("Customer_ID", "000001");
        return travelData;
    }
}
