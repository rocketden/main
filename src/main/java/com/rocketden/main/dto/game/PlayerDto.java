package com.rocketden.main.dto.game;

import java.util.List;

import com.rocketden.main.dto.user.UserDto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PlayerDto extends UserDto {
    private PlayerCodeDto playerCode;
    private List<SubmissionDto> submissions;
    private Boolean solved;
}
