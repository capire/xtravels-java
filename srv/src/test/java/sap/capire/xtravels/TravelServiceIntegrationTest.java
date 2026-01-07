package sap.capire.xtravels;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cds.gen.travelservice.Travels;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sap.cds.CdsData;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

/** Integration tests for the CAP Travel Service OData endpoints */
@SpringBootTest
@AutoConfigureMockMvc
class TravelServiceIntegrationTest {

  private static final String ODATA_BASE_URL = "/odata/v4/travel";
  private static final String TRAVELS_ENDPOINT = ODATA_BASE_URL + "/Travels";

  @Autowired private MockMvc mockMvc;

  @Test
  @WithMockUser("admin")
  void shouldGetMetadataSuccessfully() throws Exception {
    mockMvc
        .perform(get(ODATA_BASE_URL + "/$metadata"))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith("application/xml"));
  }

  // ========== Travels Entity Tests ==========

  @Test
  @WithMockUser("admin")
  void shouldGetAllTravels() throws Exception {
    mockMvc
        .perform(get(TRAVELS_ENDPOINT))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith("application/json"))
        .andExpect(jsonPath("$.@context", containsString("Travels")))
        .andExpect(jsonPath("$.value").isArray());
  }

  @Test
  @WithMockUser("admin")
  void shouldCreateTravel() throws Exception {
    Travels travel = createTravelData("shouldCreateTravel");

    mockMvc
        .perform(post(TRAVELS_ENDPOINT).contentType("application/json").content(travel.toJson()))
        .andExpect(status().isCreated())
        .andExpect(content().contentTypeCompatibleWith("application/json"))
        .andExpect(jsonPath("$.Description").value(travel.getDescription()))
        .andExpect(jsonPath("$.BeginDate").value(travel.getBeginDate().toString()))
        .andExpect(jsonPath("$.EndDate").value(travel.getEndDate().toString()))
        .andExpect(jsonPath("$.BookingFee").value(travel.getBookingFee().intValue()))
        .andExpect(jsonPath("$.Currency_code").value(travel.getCurrencyCode()))
        .andExpect(jsonPath("$.ID", notNullValue()));
  }

  @Test
  @WithMockUser("admin")
  void shouldCreateAndRetrieveTravelSuccessfully() throws Exception {
    Travels travel = createTravelData("shouldCreateAndRetrieveTravelSuccessfully");
    travel.setBookingFee(BigDecimal.valueOf(200.0));
    travel.setCurrencyCode("USD");

    // Create travel
    String response =
        mockMvc
            .perform(
                post(TRAVELS_ENDPOINT).contentType("application/json").content(travel.toJson()))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

    // Verify the created travel can be retrieved
    ObjectMapper mapper = new ObjectMapper();
    Map<String, Object> createdTravel =
        mapper.readValue(response, new TypeReference<Map<String, Object>>() {});
    Integer travelId = (Integer) createdTravel.get("ID");
    assertNotNull(travelId);

    mockMvc
        .perform(get(TRAVELS_ENDPOINT + "(ID=" + travelId + ",IsActiveEntity=true)"))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith("application/json"))
        .andExpect(jsonPath("$.BookingFee").value(200.0))
        .andExpect(jsonPath("$.Currency_code").value("USD"));
  }

  @Test
  @WithMockUser("admin")
  void shouldGetReadOnlyEntitiesSuccessfully() throws Exception {
    // Test Flights entity
    mockMvc
        .perform(get(ODATA_BASE_URL + "/Flights"))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith("application/json"));

    // Test Supplements entity
    mockMvc
        .perform(get(ODATA_BASE_URL + "/Supplements"))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith("application/json"));

    // Test Currencies entity
    mockMvc
        .perform(get(ODATA_BASE_URL + "/Currencies"))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith("application/json"));
  }

  @Test
  @WithMockUser("admin")
  void shouldReturn400ForInvalidDiscountPercentage() throws Exception {
    // First create a travel
    Travels travelData = createTravelData("shouldReturn400ForInvalidDiscountPercentage");
    String response =
        mockMvc
            .perform(
                post(TRAVELS_ENDPOINT).contentType("application/json").content(travelData.toJson()))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

    ObjectMapper mapper = new ObjectMapper();
    Map<String, Object> createdTravel =
        mapper.readValue(response, new TypeReference<Map<String, Object>>() {});
    Integer travelId = (Integer) createdTravel.get("ID");

    // Try to execute deductDiscount action with invalid percentage (>100)
    CdsData actionParams = CdsData.create();
    actionParams.put("percent", 150); // Invalid percentage

    mockMvc
        .perform(
            post(TRAVELS_ENDPOINT
                    + "(ID="
                    + travelId
                    + ",IsActiveEntity=true)/TravelService.deductDiscount")
                .contentType("application/json")
                .content(actionParams.toJson()))
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithMockUser("admin")
  void shouldExecuteAcceptTravelAction() throws Exception {
    // First create a travel
    Travels travelData = createTravelData("shouldExecuteAcceptTravelAction");
    String response =
        mockMvc
            .perform(
                post(TRAVELS_ENDPOINT).contentType("application/json").content(travelData.toJson()))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

    ObjectMapper mapper = new ObjectMapper();
    Map<String, Object> createdTravel =
        mapper.readValue(response, new TypeReference<Map<String, Object>>() {});
    Integer travelId = (Integer) createdTravel.get("ID");

    // Execute acceptTravel action
    mockMvc
        .perform(
            post(TRAVELS_ENDPOINT
                    + "(ID="
                    + travelId
                    + ",IsActiveEntity=true)/TravelService.acceptTravel")
                .contentType("application/json")
                .content("{}"))
        .andExpect(status().is2xxSuccessful());

    // Check if travel status is accepted
    mockMvc
        .perform(get(TRAVELS_ENDPOINT + "(ID=" + travelId + ",IsActiveEntity=true)"))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith("application/json"))
        .andExpect(jsonPath("$.Status_code").value("A"));
  }

  @Test
  @WithMockUser("admin")
  void shouldExecuteRejectTravelAction() throws Exception {
    // First create a travel
    Travels travelData = createTravelData("shouldExecuteRejectTravelAction");
    String response =
        mockMvc
            .perform(
                post(TRAVELS_ENDPOINT).contentType("application/json").content(travelData.toJson()))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

    ObjectMapper mapper = new ObjectMapper();
    Map<String, Object> createdTravel =
        mapper.readValue(response, new TypeReference<Map<String, Object>>() {});
    Integer travelId = (Integer) createdTravel.get("ID");

    // Execute rejectTravel action
    mockMvc
        .perform(
            post(TRAVELS_ENDPOINT
                    + "(ID="
                    + travelId
                    + ",IsActiveEntity=true)/TravelService.rejectTravel")
                .contentType("application/json")
                .content("{}"))
        .andExpect(status().is2xxSuccessful());

    // Check if travel status is rejected
    mockMvc
        .perform(get(TRAVELS_ENDPOINT + "(ID=" + travelId + ",IsActiveEntity=true)"))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith("application/json"))
        .andExpect(jsonPath("$.Status_code").value("X"));
  }

  @Test
  @WithMockUser("admin")
  void shouldExecuteDeductDiscountAction() throws Exception {
    // First create a travel
    Travels travelData = createTravelData("shouldExecuteDeductDiscountAction");

    String response =
        mockMvc
            .perform(
                post(TRAVELS_ENDPOINT).contentType("application/json").content(travelData.toJson()))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

    ObjectMapper mapper = new ObjectMapper();
    Map<String, Object> createdTravel =
        mapper.readValue(response, new TypeReference<Map<String, Object>>() {});
    Integer travelId = (Integer) createdTravel.get("ID");

    // Execute deductDiscount action with 10% discount
    CdsData actionParams = CdsData.create();
    actionParams.put("percent", 10);

    mockMvc
        .perform(
            post(TRAVELS_ENDPOINT
                    + "(ID="
                    + travelId
                    + ",IsActiveEntity=true)/TravelService.deductDiscount")
                .contentType("application/json")
                .content(actionParams.toJson()))
        .andExpect(status().isOk());

    mockMvc
        .perform(get(TRAVELS_ENDPOINT + "(ID=" + travelId + ",IsActiveEntity=true)"))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith("application/json"))
        .andExpect(jsonPath("$.BookingFee").value(90));
  }

  private Travels createTravelData(String testName) {
    Travels travel = Travels.create();
    travel.setIsActiveEntity(true);
    travel.setDescription(testName + " - Test Travel");
    travel.setBeginDate(LocalDate.now().plusDays(30));
    travel.setEndDate(LocalDate.now().plusDays(37));
    travel.setBookingFee(BigDecimal.valueOf(100));
    travel.setCurrencyCode("EUR");
    travel.setAgencyId("070001");
    travel.setCustomerId("000001");
    return travel;
  }
}
