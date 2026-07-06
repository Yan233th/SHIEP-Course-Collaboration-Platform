package com.yan233.courseplatform.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserRequest {
    @NotBlank
    @Size(max = 50)
    private String username;
    @Size(min = 6, max = 100)
    private String password;
    @NotBlank
    @Size(max = 50)
    private String realName;
    @NotBlank
    @Pattern(regexp = "ADMIN|TEACHER|TA|STUDENT")
    private String roleCode;
    @Email
    @Size(max = 100)
    private String email;
    @Size(max = 30)
    private String phone;
    @Pattern(regexp = "M|F|U")
    private String gender = "U";
    private Integer status = 1;
}

