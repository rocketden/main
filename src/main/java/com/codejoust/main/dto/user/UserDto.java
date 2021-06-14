package com.codejoust.main.dto.user;

import com.codejoust.main.dto.account.AccountUidDto;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class UserDto {

    @EqualsAndHashCode.Include
    private String userId;

    @EqualsAndHashCode.Include
    private String nickname;

    private Boolean spectator = false;

    // The session ID of the user connection, auto-generated by sockets.
    private String sessionId;

    private AccountUidDto accountUidDto;
}
