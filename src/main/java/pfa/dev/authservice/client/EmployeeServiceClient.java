package pfa.dev.authservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import pfa.dev.authservice.dto.EmployeeLookupResponse;

@FeignClient(name = "employee-service", path = "/api/employees")
public interface EmployeeServiceClient {

    @GetMapping("/by-user-id/{userId}")
    EmployeeLookupResponse getEmployeeByUserId(
            @PathVariable("userId") String userId,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader
    );
}
