package com.example.diplomaProject.domain.api.registration;

import com.example.diplomaProject.domain.dto.RoleDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthenticationReq {

    private String login;

    private String password;

}
