package com.rocketden.main.dto.room;

import java.util.Set;

import com.rocketden.main.model.User;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class JoinRoomResponse {

    public static final String SUCCESS = "Sucessfully joined room.";
    public static final String ERROR_NOT_FOUND = "A room could not be found with the given id.";
    public static final String ERROR_USER_ALREADY_PRESENT = "A user with the features provided has already joined the room.";

    private String message;
    private String roomId;
    private Set<User> users;
}
