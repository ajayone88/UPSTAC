package org.upgrad.upstac.testrequests;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.web.server.ResponseStatusException;
import org.upgrad.upstac.config.security.UserLoggedInService;
import org.upgrad.upstac.exception.AppException;
import org.upgrad.upstac.testrequests.lab.CreateLabResult;
import org.upgrad.upstac.testrequests.lab.LabRequestController;
import org.upgrad.upstac.testrequests.lab.LabResult;
import org.upgrad.upstac.testrequests.lab.TestStatus;
import org.upgrad.upstac.users.User;
import org.upgrad.upstac.users.models.Gender;

import javax.validation.ConstraintViolationException;
import java.time.LocalDate;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

import static org.hibernate.validator.internal.util.Contracts.assertNotNull;
import static org.junit.jupiter.api.Assertions.*;


@SpringBootTest
@Slf4j
@ExtendWith(MockitoExtension.class)
class LabRequestControllerTest {

    @InjectMocks
    LabRequestController labRequestController;

    @Mock
    TestRequestUpdateService testRequestUpdateService;

    @Mock
    UserLoggedInService userLoggedInService;

    @Test
    @WithUserDetails(value = "tester")
    public void calling_assignForLabTest_with_valid_test_request_id_should_update_the_request_status(){
        //Arrange
        TestRequest testRequest = getTestRequestByStatus(RequestStatus.INITIATED);
        User user = createUser();
        Mockito.when(userLoggedInService.getLoggedInUser()).thenReturn(user);
        Mockito.when(testRequestUpdateService.assignForLabTest(testRequest.getRequestId(), user)).thenReturn(testRequest);

        //Act
        TestRequest testRequestOutput = labRequestController.assignForLabTest(testRequest.getRequestId());

        //Assert
        assertEquals(testRequestOutput.getRequestId(), testRequest.getRequestId());
        assertEquals(testRequestOutput.getStatus(), RequestStatus.INITIATED);
        assertNotNull(testRequestOutput.getLabResult());
    }

    @Test
    @WithUserDetails(value = "tester")
    public void calling_assignForLabTest_with_valid_test_request_id_should_throw_exception(){
        //Arrange
        Long InvalidRequestId = -34L;
        User user = createUser();
        Mockito.when(userLoggedInService.getLoggedInUser()).thenReturn(user);
        Mockito.when(testRequestUpdateService.assignForLabTest(InvalidRequestId, user)).thenThrow(new AppException("Invalid RequestId"));

        //Act
        ResponseStatusException responseStatusException = assertThrows(ResponseStatusException.class, ()->{
            labRequestController.assignForLabTest(InvalidRequestId);
        });

        //Assert
        assertThat(responseStatusException.getMessage(), containsString("Invalid RequestId"));
    }

    @Test
    @WithUserDetails(value = "tester")
    public void calling_updateLabTest_with_valid_test_request_id_should_update_the_request_status_and_update_test_request_details(){
        //Arrange
        TestRequest testRequest = getTestRequestByStatus(RequestStatus.LAB_TEST_IN_PROGRESS);
        CreateLabResult createLabResult  = getCreateLabResult(testRequest);
        User user = createUser();
        Mockito.when(userLoggedInService.getLoggedInUser()).thenReturn(user);
        Mockito.when(testRequestUpdateService.updateLabTest(testRequest.getRequestId(), createLabResult, user)).thenReturn(testRequest);

        //Act
        TestRequest testRequestOutput = labRequestController.updateLabTest(testRequest.getRequestId(), createLabResult);

        //Assert
        assertEquals(testRequestOutput.getRequestId(), (testRequest.getRequestId()));
        assertEquals(testRequestOutput.getStatus(), RequestStatus.LAB_TEST_COMPLETED);
        assertEquals(testRequestOutput.getLabResult(), testRequest.getLabResult());
    }


    @Test
    @WithUserDetails(value = "tester")
    public void calling_updateLabTest_with_invalid_test_request_id_should_throw_exception(){
        //Arrange
        TestRequest testRequest = getTestRequestByStatus(RequestStatus.LAB_TEST_IN_PROGRESS);
        Long InvalidTestRequestId = -34L;
        CreateLabResult createLabResult = getCreateLabResult(testRequest);
        User user = createUser();
        Mockito.when(userLoggedInService.getLoggedInUser()).thenReturn(user);
        Mockito.when(testRequestUpdateService.updateLabTest(InvalidTestRequestId, createLabResult, user)).thenThrow(new AppException("Invalid TestRequestId"));

        //Act
        ResponseStatusException responseStatusException = assertThrows(ResponseStatusException.class, ()->{
                labRequestController.updateLabTest(InvalidTestRequestId, createLabResult);
        });

        //Assert
        assertThat(responseStatusException.getMessage(), containsString("Invalid TestRequestId"));
    }

    @Test
    @WithUserDetails(value = "tester")
    public void calling_updateLabTest_with_invalid_empty_status_should_throw_exception(){
        //Arrange
        TestRequest testRequest = getTestRequestByStatus(RequestStatus.LAB_TEST_IN_PROGRESS);
        CreateLabResult createLabResult = getCreateLabResult(testRequest);
        createLabResult.setResult(null);
        User user = createUser();
        Mockito.when(userLoggedInService.getLoggedInUser()).thenReturn(user);
        Mockito.when(testRequestUpdateService.updateLabTest(testRequest.getRequestId(), createLabResult, user)).thenThrow(ConstraintViolationException.class);

        //Act
        ResponseStatusException responseStatusException = assertThrows(ResponseStatusException.class, ()->{
            labRequestController.updateLabTest(testRequest.getRequestId(), createLabResult);
        });

        //Assert
        assertThat(responseStatusException.getMessage(), containsString("ConstraintViolationException"));


    }

    // Helper methods to mock the Objects.
    public CreateLabResult getCreateLabResult(TestRequest testRequest) {
        return CreateLabResult();
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
        if(status.equals(RequestStatus.INITIATED)){
            testRequest.setStatus(RequestStatus.INITIATED);
        }else if(status.equals(RequestStatus.LAB_TEST_IN_PROGRESS)) {
            testRequest.setStatus(RequestStatus.LAB_TEST_COMPLETED);
        }
        CreateLabResult createLabResult = CreateLabResult();
        LabResult labResult = getLabResult(testRequest, createLabResult, createUser());
        testRequest.setLabResult(labResult);
        return testRequest;
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
    private User createUser() {
        User user = new User();
        user.setId(1L);
        user.setUserName("someuser");
        return user;
    }
}