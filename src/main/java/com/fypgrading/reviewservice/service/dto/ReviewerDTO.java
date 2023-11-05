package com.fypgrading.reviewservice.service.dto;

import com.fypgrading.reviewservice.service.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewerDTO {

    private Integer id;

    private String firstName;

    private String lastName;

    private String email;

    private Role role;
}
