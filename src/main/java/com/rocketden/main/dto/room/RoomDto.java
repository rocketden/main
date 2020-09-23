package com.rocketden.main.dto.room;

import com.rocketden.main.dto.user.UserDto;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
public class RoomDto {

    private String roomId;
    private UserDto host;
    private Set<UserDto> users;
}