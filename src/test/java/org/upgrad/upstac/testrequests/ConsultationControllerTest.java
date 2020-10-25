package org.upgrad.upstac.testrequests;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.web.server.ResponseStatusException;
import org.upgrad.upstac.config.security.UserLoggedInService;
import org.upgrad.upstac.exception.AppException;
import org.upgrad.upstac.testrequests.consultation.Consultation;
import org.upgrad.upstac.testrequests.consultation.ConsultationController;
import org.upgrad.upstac.testrequests.consultation.CreateConsultationRequest;
import org.upgrad.upstac.testrequests.consultation.DoctorSuggestion;
import org.upgrad.upstac.testrequests.lab.CreateLabResult;
import org.upgrad.upstac.testrequests.lab.LabResult;
import org.upgrad.upstac.testrequests.lab.TestStatus;
import org.upgrad.upstac.users.User;
import org.upgrad.upstac.users.models.Gender;

import javax.validation.ConstraintViolationException;
import java.time.LocalDate;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;


@SpringBootTest
@Slf4j
@ExtendWith(MockitoExtension.class)
class ConsultationControllerTest {

    @Autowired
    TestRequestQueryService testRequestQueryService;

    @InjectMocks
    ConsultationController consultationController;

    @Mock
    UserLoggedInService userLoggedInService;

    @Mock
    TestRequestUpdateService testRequestUpdateService;

    @Test
    @WithUserDetails(value = "doctor")
    public void calling_assignForConsultation_with_valid_test_request_id_should_update_the_request_status(){
        //Arrange
        TestRequest testRequest = getTestRequestByStatus(RequestStatus.LAB_TEST_COMPLETED);
        User user = createUser();
        Mockito.when(userLoggedInService.getLoggedInUser()).thenReturn(user);
        Mockito.when(testRequestUpdateService.assignForConsultation(testRequest.getRequestId(), user)).thenReturn(testRequest);

        //Act
        TestRequest testRequestOutput = consultationController.assignForConsultation(testRequest.getRequestId());

        //Assert
        assertEquals(testRequestOutput.getRequestId(), testRequest.getRequestId());
        assertEquals(testRequestOutput.getStatus(), RequestStatus.DIAGNOSIS_IN_PROCESS);
        assertNotNull(testRequestOutput.getConsultation());
    }

    @Test
    @WithUserDetails(value = "doctor")
    public void calling_assignForConsultation_with_valid_test_request_id_should_throw_exception(){
        //Arrange
        Long InvalidRequestId= -34L;
        User user = createUser();
        Mockito.when(userLoggedInService.getLoggedInUser()).thenReturn(user);
        Mockito.when(testRequestUpdateService.assignForConsultation(InvalidRequestId, user)).thenThrow(new AppException("Invalid ID"));

        //Act
        ResponseStatusException responseStatusException = assertThrows(ResponseStatusException.class, ()->{
            consultationController.assignForConsultation(InvalidRequestId);
        });

        //Assert
        assertThat(responseStatusException.getMessage(), containsString("Invalid ID"));
    }

    @Test
    @WithUserDetails(value = "doctor")
    public void calling_updateConsultation_with_valid_test_request_id_should_update_the_request_status_and_update_consultation_details(){
        // Arrange
        TestRequest testRequest = getTestRequestByStatus(RequestStatus.DIAGNOSIS_IN_PROCESS);
        CreateConsultationRequest createConsultationRequest = getCreateConsultationRequest(testRequest);
        User user = createUser();
        Mockito.when(userLoggedInService.getLoggedInUser()).thenReturn(user);
        Mockito.when(testRequestUpdateService.updateConsultation(testRequest.getRequestId(), createConsultationRequest, user)).thenReturn(testRequest);

        // Act
        TestRequest testRequestOutput = consultationController.updateConsultation(testRequest.getRequestId(), createConsultationRequest);

        // Assert
        assertEquals(testRequestOutput.getRequestId(), testRequest.getRequestId());
        assertEquals(testRequestOutput.getStatus(), RequestStatus.COMPLETED);
        assertEquals(testRequestOutput.getConsultation().getSuggestion(), testRequest.getConsultation().getSuggestion());
    }

    @Test
    @WithUserDetails(value = "doctor")
    public void calling_updateConsultation_with_invalid_test_request_id_should_throw_exception(){
        //Arrange
        TestRequest testRequest = getTestRequestByStatus(RequestStatus.DIAGNOSIS_IN_PROCESS);
        Long InvalidRequestId= -34L;
        CreateConsultationRequest createConsultationRequest = getCreateConsultationRequest(testRequest);
        User user = createUser();
        Mockito.when(userLoggedInService.getLoggedInUser()).thenReturn(user);
        Mockito.when(testRequestUpdateService.updateConsultation(InvalidRequestId, createConsultationRequest, user)).thenThrow(new AppException("Invalid ID"));

        //Act
        ResponseStatusException responseStatusException = assertThrows(ResponseStatusException.class, ()->{
            consultationController.updateConsultation(InvalidRequestId, createConsultationRequest);
        });

        //Assert
        assertThat(responseStatusException.getMessage(), containsString("Invalid ID"));
    }

    @Test
    @WithUserDetails(value = "doctor")
    public void calling_updateConsultation_with_invalid_empty_status_should_throw_exception(){
        //Arrange
        TestRequest testRequest = getTestRequestByStatus(RequestStatus.DIAGNOSIS_IN_PROCESS);
        CreateConsultationRequest createConsultationRequest = getCreateConsultationRequest(testRequest);
        createConsultationRequest.setSuggestion(null);
        User user = createUser();
        Mockito.when(userLoggedInService.getLoggedInUser()).thenReturn(user);
        Mockito.when(testRequestUpdateService.updateConsultation(testRequest.getRequestId(), createConsultationRequest, user)).thenThrow(ConstraintViolationException.class);

        //Act
        ResponseStatusException responseStatusException = assertThrows(ResponseStatusException.class, ()->{
            consultationController.updateConsultation(testRequest.getRequestId(), createConsultationRequest);
        });

        //Assert
        assertThat(responseStatusException.getMessage(), containsString("ConstraintViolationException"));
    }

    public CreateConsultationRequest getCreateConsultationRequest(TestRequest testRequest) {
        CreateConsultationRequest createConsultationRequest = new CreateConsultationRequest();
        CreateLabResult createLabResult = CreateLabResult();
        if(createLabResult.getResult().equals(TestStatus.POSITIVE)) {
            createConsultationRequest.setSuggestion(DoctorSuggestion.HOME_QUARANTINE);
            createConsultationRequest.setComments("accordingly");
        }else if(createLabResult.getResult().equals(TestStatus.NEGATIVE)) {
            createConsultationRequest.setSuggestion(DoctorSuggestion.NO_ISSUES);
            createConsultationRequest.setComments("Ok");
        }
        return createConsultationRequest;
    }

    public TestRequest getTestRequestByStatus(RequestStatus status) {
        TestRequest testRequest = new TestRequest();
        CreateTestRequest createTestRequest = createTestRequest();
        testRequest.setRequestId(1L);
        testRequest.setName(createTestRequest.getName());
        testRequest.setCreated(LocalDate.now());
        testRequest.setAge(createTestRequest.getAge());
        testRequest.setEmail(createTestRequest.getEmail());
        testRequest.setPhoneNumber(createTestRequest.getPhoneNumber());
        testRequest.setPinCode(createTestRequest.getPinCode());
        testRequest.setAddress(createTestRequest.getAddress());
        testRequest.setGender(createTestRequest.getGender());
        testRequest.setCreatedBy(createUser());
        if(status.equals(RequestStatus.LAB_TEST_COMPLETED)){
            testRequest.setStatus(RequestStatus.DIAGNOSIS_IN_PROCESS);
        }else if(status.equals(RequestStatus.DIAGNOSIS_IN_PROCESS)) {
            testRequest.setStatus(RequestStatus.COMPLETED);
        }
        CreateLabResult createLabResult = CreateLabResult();
        LabResult labResult = getLabResult(testRequest, createLabResult, createUser());
        testRequest.setLabResult(labResult);
        CreateConsultationRequest createConsultationRequest = getCreateConsultationRequest(testRequest);
        Consultation consultation = getConsultation(testRequest, createConsultationRequest, createUser());
        testRequest.setConsultation(consultation);
        return testRequest;
    }

    public LabResult getLabResult(TestRequest testRequest, CreateLabResult createLabResult, User user){
        LabResult labResult = new LabResult();
        labResult.setTester(user);
        labResult.setResultId(testRequest.getRequestId());
        labResult.setTemperature(createLabResult.getTemperature());
        labResult.setOxygenLevel(createLabResult.getOxygenLevel());
        labResult.setHeartBeat(createLabResult.getHeartBeat());
        labResult.setBloodPressure(createLabResult.getBloodPressure());
        labResult.setComments(createLabResult.getComments());
        labResult.setResult(createLabResult.getResult());
        labResult.setRequest(testRequest);
        return labResult;
    }
    public Consultation getConsultation(TestRequest testRequest, CreateConsultationRequest createConsultationRequest, User user){
        Consultation consultation = new Consultation();
        consultation.setDoctor(user);
        consultation.setComments(createConsultationRequest.getComments());
        consultation.setSuggestion(createConsultationRequest.getSuggestion());
        consultation.setRequest(testRequest);
        return consultation;
    }
    public CreateTestRequest createTestRequest() {
        CreateTestRequest createTestRequest = new CreateTestRequest();
        createTestRequest.setAddress("some Addres");
        createTestRequest.setAge(98);
        createTestRequest.setEmail("someone" + "123456789" + "@somedomain.com");
        createTestRequest.setGender(Gender.MALE);
        createTestRequest.setName("someuser");
        createTestRequest.setPhoneNumber("123456789");
        createTestRequest.setPinCode(716768);
        return createTestRequest;
    }
    public CreateLabResult CreateLabResult() {
        CreateLabResult createLabResult = new CreateLabResult();
        createLabResult.setBloodPressure("120");
        createLabResult.setComments("120");
        createLabResult.setHeartBeat("120");
        createLabResult.setOxygenLevel("120");
        createLabResult.setTemperature("98");
        createLabResult.setResult(TestStatus.NEGATIVE);
        return createLabResult;
    }
    private User createUser() {
        User user = new User();
        user.setId(1L);
        user.setUserName("someuser");
        return user;
    }
}