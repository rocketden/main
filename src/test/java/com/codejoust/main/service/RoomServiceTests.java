package com.codejoust.main.service;

import com.codejoust.main.model.problem.Problem;
import com.codejoust.main.util.TestFields;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.codejoust.main.dao.RoomRepository;
import com.codejoust.main.dto.problem.SelectableProblemDto;
import com.codejoust.main.dto.room.CreateRoomRequest;
import com.codejoust.main.dto.room.DeleteRoomRequest;
import com.codejoust.main.dto.room.JoinRoomRequest;
import com.codejoust.main.dto.room.RemoveUserRequest;
import com.codejoust.main.dto.room.RoomDto;
import com.codejoust.main.dto.room.SetSpectatorRequest;
import com.codejoust.main.dto.room.UpdateHostRequest;
import com.codejoust.main.dto.room.UpdateSettingsRequest;
import com.codejoust.main.dto.user.UserDto;
import com.codejoust.main.dto.user.UserMapper;
import com.codejoust.main.exception.ProblemError;
import com.codejoust.main.exception.RoomError;
import com.codejoust.main.exception.TimerError;
import com.codejoust.main.exception.UserError;
import com.codejoust.main.exception.api.ApiException;
import com.codejoust.main.model.Room;
import com.codejoust.main.model.User;
import com.codejoust.main.model.problem.ProblemDifficulty;
import com.codejoust.main.util.Utility;

@ExtendWith(MockitoExtension.class)
public class RoomServiceTests {

    @Mock
    private RoomRepository repository;

    @Mock
    private ProblemService problemService;

    @Mock
    private SocketService socketService;

    @Mock
    private Utility utility;

    @Spy
    @InjectMocks
    private RoomService roomService;

    @Test
    public void createRoomSuccess() {
        UserDto user = new UserDto();
        user.setNickname(TestFields.NICKNAME);
        CreateRoomRequest request = new CreateRoomRequest();
        request.setHost(user);
        
        // Mock generateUniqueId to return a custom room id
        Mockito.doReturn(TestFields.ROOM_ID).when(utility).generateUniqueId(eq(RoomService.ROOM_ID_LENGTH), eq(Utility.ROOM_ID_KEY));

        // Mock generateUniqueId to return a custom user id
        Mockito.doReturn(TestFields.USER_ID).when(utility).generateUniqueId(eq(UserService.USER_ID_LENGTH), eq(Utility.USER_ID_KEY));

        // Verify create room request succeeds and returns correct response
        RoomDto response = roomService.createRoom(request, null);

        verify(repository).save(Mockito.any(Room.class));
        assertEquals(TestFields.ROOM_ID, response.getRoomId());
        assertEquals(user.getNickname(), response.getHost().getNickname());
        assertEquals(TestFields.USER_ID, response.getHost().getUserId());
        assertEquals(ProblemDifficulty.RANDOM, response.getDifficulty());
        assertEquals(0, response.getProblems().size());
    }

    @Test
    public void joinRoomSuccess() {
        // Verify join room request succeeds and returns correct response
        User user = new User();
        user.setNickname(TestFields.NICKNAME);
        JoinRoomRequest request = new JoinRoomRequest();
        request.setUser(UserMapper.toDto(user));

        // Mock generateUniqueId to return a custom user id
        Mockito.doReturn(TestFields.USER_ID).when(utility).generateUniqueId(eq(UserService.USER_ID_LENGTH), eq(Utility.USER_ID_KEY));

        Room room = new Room();
        room.setRoomId(TestFields.ROOM_ID);

        // Create host
        User host = new User();
        host.setNickname(TestFields.NICKNAME_2);
        room.addUser(host);
        room.setHost(host);

        // Mock repository to return room when called
        Mockito.doReturn(room).when(repository).findRoomByRoomId(eq(TestFields.ROOM_ID));
        RoomDto response = roomService.joinRoom(TestFields.ROOM_ID, request, null);

        verify(socketService).sendSocketUpdate(eq(response));
        assertEquals(TestFields.ROOM_ID, response.getRoomId());
        assertEquals(2, response.getUsers().size());
        assertEquals(host.getNickname(), response.getUsers().get(0).getNickname());
        assertEquals(user.getNickname(), response.getUsers().get(1).getNickname());
        assertEquals(TestFields.USER_ID, response.getUsers().get(1).getUserId());
        assertEquals(ProblemDifficulty.RANDOM, response.getDifficulty());
    }

    @Test
    public void joinRoomNonexistentFailure() {
        // Verify join room request fails when room does not exist
        User user = new User();
        user.setNickname(TestFields.NICKNAME);
        JoinRoomRequest request = new JoinRoomRequest();
        request.setUser(UserMapper.toDto(user));

        // Mock repository to return room when called
        Mockito.doReturn(null).when(repository).findRoomByRoomId(eq(TestFields.ROOM_ID));

        // Assert that service.joinRoom(request) throws the correct exception
        ApiException exception = assertThrows(ApiException.class, () -> roomService.joinRoom(TestFields.ROOM_ID, request, null));

        verify(repository).findRoomByRoomId(TestFields.ROOM_ID);
        assertEquals(RoomError.NOT_FOUND, exception.getError());
    }

    @Test
    public void joinRoomDuplicateUserFailure() {
        /**
         * Verify join room request fails when user with same features 
         * is already present
         * Define two identical, and make the first one the host and 
         * second one the joiner
         */
        User firstUser = new User();
        firstUser.setNickname(TestFields.NICKNAME);
        UserDto newUser = new UserDto();
        newUser.setNickname(TestFields.NICKNAME);

        JoinRoomRequest request = new JoinRoomRequest();
        request.setUser(newUser);

        Room room = new Room();
        room.setRoomId(TestFields.ROOM_ID);
        room.setHost(firstUser);
        room.addUser(firstUser);

        // Mock repository to return room when called
        Mockito.doReturn(room).when(repository).findRoomByRoomId(eq(TestFields.ROOM_ID));
        ApiException exception = assertThrows(ApiException.class, () -> roomService.joinRoom(TestFields.ROOM_ID, request, null));

        verify(repository).findRoomByRoomId(TestFields.ROOM_ID);
        assertEquals(RoomError.DUPLICATE_USERNAME, exception.getError());
    }

    @Test
    public void setRoomSizeFailure() {
        /**
         * Verify set room size request fails when the size to be set is less
         * than the the number of users already in the room
         * Define four users, add to the room, and attempt to set room size to 3
         */
        User firstUser = new User();
        firstUser.setNickname(TestFields.NICKNAME);
        User secondUser = new User();
        secondUser.setNickname(TestFields.NICKNAME_2);
        User thirdUser = new User();
        thirdUser.setNickname(TestFields.NICKNAME_3);
        User fourthUser = new User();
        fourthUser.setNickname(TestFields.NICKNAME_4);

        Room room = new Room();
        room.setRoomId(TestFields.ROOM_ID);
        room.setSize(4);
        room.setHost(firstUser);
        room.addUser(firstUser);
        
        room.addUser(secondUser);
        room.addUser(thirdUser);
        room.addUser(fourthUser);

        // Mock repository to return room when called
        Mockito.doReturn(room).when(repository).findRoomByRoomId(eq(TestFields.ROOM_ID));

        UpdateSettingsRequest request = new UpdateSettingsRequest();
        request.setInitiator(UserMapper.toDto(firstUser));
        request.setDifficulty(ProblemDifficulty.EASY);
        request.setDuration(TestFields.DURATION);
        request.setSize(3);

        ApiException exception = assertThrows(ApiException.class, () -> roomService.updateRoomSettings(TestFields.ROOM_ID, request));
        assertEquals(RoomError.BAD_ROOM_SIZE, exception.getError());
        verify(repository).findRoomByRoomId(TestFields.ROOM_ID);
    }

    @Test
    public void joinFullRoomFailure() {
        /**
         * Verify join room request fails when the room is already full
         * Define five users, and add to the room
         */
        User firstUser = new User();
        firstUser.setNickname(TestFields.NICKNAME);
        User secondUser = new User();
        secondUser.setNickname(TestFields.NICKNAME_2);
        User thirdUser = new User();
        thirdUser.setNickname(TestFields.NICKNAME_3);
        User fourthUser = new User();
        fourthUser.setNickname(TestFields.NICKNAME_4);
        UserDto fifthUser = new UserDto();
        fifthUser.setNickname(TestFields.NICKNAME_5);

        JoinRoomRequest request = new JoinRoomRequest();
        request.setUser(fifthUser);

        Room room = new Room();
        room.setRoomId(TestFields.ROOM_ID);
        room.setSize(4);
        room.setHost(firstUser);
        room.addUser(firstUser);
        
        room.addUser(secondUser);
        room.addUser(thirdUser);
        room.addUser(fourthUser);

        // Mock repository to return room when called
        Mockito.doReturn(room).when(repository).findRoomByRoomId(eq(TestFields.ROOM_ID));
        ApiException exception = assertThrows(ApiException.class, () -> roomService.joinRoom(TestFields.ROOM_ID, request, null));

        verify(repository).findRoomByRoomId(TestFields.ROOM_ID);
        assertEquals(RoomError.ALREADY_FULL, exception.getError());
    }


    @Test
    public void manyUsersJoiningAnInfinitelySizedRoomSuccess() {
        /**
         * Verify join room request works when the room is infinitely sized
         * Define a hundred users, add to the room, then request to add another user
         */
        User firstUser = new User();
        firstUser.setNickname(TestFields.NICKNAME);
        UserDto secondUser = new UserDto();
        secondUser.setNickname(TestFields.NICKNAME_2);
        JoinRoomRequest request = new JoinRoomRequest();
        request.setUser(secondUser);

        Room room = new Room();
        room.setRoomId(TestFields.ROOM_ID);
        room.setSize((int) (RoomService.MAX_SIZE + 1));
        room.setHost(firstUser);
        room.addUser(firstUser);

        for (int i = 0; i < 100; i++) {
            User temp = new User();
            temp.setNickname("Rocket" + i);
            room.addUser(temp);
        }

        // Mock repository to return room when called
        Mockito.doReturn(room).when(repository).findRoomByRoomId(eq(TestFields.ROOM_ID));
        assertDoesNotThrow(() -> roomService.joinRoom(TestFields.ROOM_ID, request, null));
        verify(repository).findRoomByRoomId(TestFields.ROOM_ID);

        assertEquals(102, room.getUsers().size());
    }

    @Test
    public void setInvalidNumProblemsFailure() {
        /**
         * Verify update settings request fails when numProblems is
         * set to outside of the allowable range
         */
        User host = new User();
        host.setNickname(TestFields.NICKNAME);

        Room room = new Room();
        room.setRoomId(TestFields.ROOM_ID);
        room.setHost(host);
        room.addUser(host);
        
        UpdateSettingsRequest request = new UpdateSettingsRequest();
        request.setInitiator(UserMapper.toDto(host));
        request.setNumProblems(15);

        // Mock repository to return room when called
        Mockito.doReturn(room).when(repository).findRoomByRoomId(eq(TestFields.ROOM_ID));
        ApiException exception = assertThrows(ApiException.class, () -> roomService.updateRoomSettings(TestFields.ROOM_ID, request));

        verify(repository).findRoomByRoomId(TestFields.ROOM_ID);
        assertEquals(ProblemError.INVALID_NUMBER_REQUEST, exception.getError());
    }
    
    @Test
    public void getRoomSuccess() {
        Room room = new Room();
        room.setRoomId(TestFields.ROOM_ID);

        User host = new User();
        host.setNickname(TestFields.NICKNAME);

        room.setHost(host);
        room.addUser(host);

        Mockito.doReturn(room).when(repository).findRoomByRoomId(eq(TestFields.ROOM_ID));
        RoomDto response = roomService.getRoom(TestFields.ROOM_ID);

        assertEquals(TestFields.ROOM_ID, response.getRoomId());
        assertEquals(room.getHost(), UserMapper.toEntity(response.getHost()));

        List<User> actual = response.getUsers().stream()
                .map(UserMapper::toEntity).collect(Collectors.toList());
        assertEquals(room.getUsers(), actual);
    }

    @Test
    public void getRoomFailure() {
        ApiException exception = assertThrows(ApiException.class, () -> roomService.getRoom(TestFields.ROOM_ID));

        assertEquals(RoomError.NOT_FOUND, exception.getError());
    }

    @Test
    public void changeRoomHostSuccess() {
        Room room = new Room();
        room.setRoomId(TestFields.ROOM_ID);

        User host = new User();
        host.setNickname(TestFields.NICKNAME);
        host.setSessionId(TestFields.SESSION_ID);

        User user =  new User();
        user.setNickname(TestFields.NICKNAME_2);
        user.setSessionId(TestFields.SESSION_ID_2);

        room.setHost(host);
        room.addUser(host);
        room.addUser(user);

        Mockito.doReturn(room).when(repository).findRoomByRoomId(eq(TestFields.ROOM_ID));

        UpdateHostRequest request = new UpdateHostRequest();
        request.setInitiator(UserMapper.toDto(host));
        request.setNewHost(UserMapper.toDto(user));

        RoomDto response = roomService.updateRoomHost(room.getRoomId(), request, false);

        verify(socketService).sendSocketUpdate(eq(response));
        assertEquals(user, UserMapper.toEntity(response.getHost()));
    }

    @Test
    public void changeRoomHostFailure() {
        Room room = new Room();
        room.setRoomId(TestFields.ROOM_ID);

        User host = new User();
        host.setNickname(TestFields.NICKNAME);
        host.setSessionId(TestFields.SESSION_ID);

        User user =  new User();
        user.setNickname(TestFields.NICKNAME_2);
        user.setSessionId(TestFields.SESSION_ID_2);

        room.setHost(host);
        room.addUser(host);
        room.addUser(user);

        Mockito.doReturn(room).when(repository).findRoomByRoomId(eq(TestFields.ROOM_ID));

        // Invalid permissions
        UpdateHostRequest invalidPermRequest = new UpdateHostRequest();
        invalidPermRequest.setInitiator(UserMapper.toDto(user));
        invalidPermRequest.setNewHost(UserMapper.toDto(host));

        ApiException exception = assertThrows(ApiException.class, () ->
                roomService.updateRoomHost(TestFields.ROOM_ID, invalidPermRequest, false));
        assertEquals(RoomError.INVALID_PERMISSIONS, exception.getError());

        // Nonexistent room
        UpdateHostRequest noRoomRequest = new UpdateHostRequest();
        noRoomRequest.setInitiator(UserMapper.toDto(host));
        noRoomRequest.setNewHost(UserMapper.toDto(user));

        exception = assertThrows(ApiException.class, () ->
                roomService.updateRoomHost("999999", noRoomRequest, false));
        assertEquals(RoomError.NOT_FOUND, exception.getError());

        // Nonexistent new host
        UpdateHostRequest noUserRequest = new UpdateHostRequest();
        noUserRequest.setInitiator(UserMapper.toDto(host));

        UserDto nonExistentUser = new UserDto();
        nonExistentUser.setNickname(TestFields.NICKNAME_3);
        noUserRequest.setNewHost(nonExistentUser);

        exception = assertThrows(ApiException.class, () ->
                roomService.updateRoomHost(TestFields.ROOM_ID, noUserRequest, false));
        assertEquals(UserError.NOT_FOUND, exception.getError());

        // New host inactive
        UpdateHostRequest inactiveUserRequest = new UpdateHostRequest();
        user.setSessionId(null);
        inactiveUserRequest.setInitiator(UserMapper.toDto(host));
        inactiveUserRequest.setNewHost(UserMapper.toDto(user));

        exception = assertThrows(ApiException.class, () ->
                roomService.updateRoomHost(TestFields.ROOM_ID, inactiveUserRequest, false));
        assertEquals(RoomError.INACTIVE_USER, exception.getError());
    }

    @Test
    public void updateRoomSettingsSuccess() {
        Room room = new Room();
        room.setRoomId(TestFields.ROOM_ID);

        User host = new User();
        host.setNickname(TestFields.NICKNAME);

        room.setHost(host);
        room.addUser(host);

        Mockito.doReturn(room).when(repository).findRoomByRoomId(eq(TestFields.ROOM_ID));

        UpdateSettingsRequest request = new UpdateSettingsRequest();
        request.setInitiator(UserMapper.toDto(host));
        request.setDifficulty(ProblemDifficulty.EASY);
        request.setDuration(TestFields.DURATION);
        request.setSize(5);
        request.setNumProblems(3);

        RoomDto response = roomService.updateRoomSettings(room.getRoomId(), request);

        verify(socketService).sendSocketUpdate(eq(response));
        assertEquals(request.getDifficulty(), response.getDifficulty());
        assertEquals(request.getDuration(), response.getDuration());
        assertEquals(request.getSize(), response.getSize());
        assertEquals(request.getNumProblems(), response.getNumProblems());
    }

    @Test
    public void updateRoomSettingsInvalidPermissions() {
        Room room = new Room();
        room.setRoomId(TestFields.ROOM_ID);

        User host = new User();
        host.setNickname(TestFields.NICKNAME);
        User user =  new User();
        user.setNickname(TestFields.NICKNAME_2);

        room.setHost(host);
        room.addUser(host);
        room.addUser(user);

        Mockito.doReturn(room).when(repository).findRoomByRoomId(eq(TestFields.ROOM_ID));

        // Invalid permissions
        UpdateSettingsRequest invalidPermRequest = new UpdateSettingsRequest();
        invalidPermRequest.setInitiator(UserMapper.toDto(user));
        invalidPermRequest.setDifficulty(ProblemDifficulty.MEDIUM);

        ApiException exception = assertThrows(ApiException.class, () ->
                roomService.updateRoomSettings(TestFields.ROOM_ID, invalidPermRequest));
        assertEquals(RoomError.INVALID_PERMISSIONS, exception.getError());
    }

    @Test
    public void updateRoomSettingsNoRoomFound() {
        UserDto userDto = new UserDto();
        userDto.setNickname(TestFields.NICKNAME);

        // Non-existent room
        UpdateSettingsRequest noRoomRequest = new UpdateSettingsRequest();
        noRoomRequest.setInitiator(userDto);
        noRoomRequest.setDifficulty(ProblemDifficulty.HARD);

        ApiException exception = assertThrows(ApiException.class, () ->
                roomService.updateRoomSettings("999999", noRoomRequest));
        assertEquals(RoomError.NOT_FOUND, exception.getError());
    }

    @Test
    public void updateRoomSettingsInvalidDuration() {
        Room room = new Room();
        room.setRoomId(TestFields.ROOM_ID);

        User host = new User();
        host.setNickname(TestFields.NICKNAME);

        room.setHost(host);
        room.addUser(host);

        Mockito.doReturn(room).when(repository).findRoomByRoomId(eq(TestFields.ROOM_ID));

        UpdateSettingsRequest request = new UpdateSettingsRequest();
        request.setInitiator(UserMapper.toDto(host));
        request.setDifficulty(ProblemDifficulty.EASY);
        request.setDuration(-1L);

        ApiException exception = assertThrows(ApiException.class, () -> roomService.updateRoomSettings(TestFields.ROOM_ID, request));
        assertEquals(TimerError.INVALID_DURATION, exception.getError());
    }

    @Test
    public void updateRoomSettingsDurationTooLong() {
        Room room = new Room();
        room.setRoomId(TestFields.ROOM_ID);

        User host = new User();
        host.setNickname(TestFields.NICKNAME);

        room.setHost(host);
        room.addUser(host);

        Mockito.doReturn(room).when(repository).findRoomByRoomId(eq(TestFields.ROOM_ID));

        UpdateSettingsRequest request = new UpdateSettingsRequest();
        request.setInitiator(UserMapper.toDto(host));

        request.setDifficulty(ProblemDifficulty.EASY);
        request.setDuration(RoomService.MAX_DURATION + 1);

        ApiException exception = assertThrows(ApiException.class, () -> roomService.updateRoomSettings(TestFields.ROOM_ID, request));
        assertEquals(TimerError.INVALID_DURATION, exception.getError());
    }

    @Test
    public void updateRoomSettingsBadNumProblems() {
        Room room = new Room();
        room.setRoomId(TestFields.ROOM_ID);

        User host = new User();
        host.setNickname(TestFields.NICKNAME);

        room.setHost(host);
        room.addUser(host);

        Mockito.doReturn(room).when(repository).findRoomByRoomId(eq(TestFields.ROOM_ID));

        UpdateSettingsRequest request = new UpdateSettingsRequest();
        request.setInitiator(UserMapper.toDto(host));
        request.setNumProblems(-1);

        ApiException exception = assertThrows(ApiException.class, () ->
                roomService.updateRoomSettings(TestFields.ROOM_ID, request));
        assertEquals(ProblemError.INVALID_NUMBER_REQUEST, exception.getError());
    }

    @Test
    public void updateRoomSettingsExceedsMaxProblems() {
        Room room = new Room();
        room.setRoomId(TestFields.ROOM_ID);

        User host = new User();
        host.setNickname(TestFields.NICKNAME);

        room.setHost(host);
        room.addUser(host);

        Mockito.doReturn(room).when(repository).findRoomByRoomId(eq(TestFields.ROOM_ID));

        UpdateSettingsRequest request = new UpdateSettingsRequest();
        request.setInitiator(UserMapper.toDto(host));
        request.setNumProblems(RoomService.MAX_NUM_PROBLEMS + 1);

        ApiException exception = assertThrows(ApiException.class, () ->
                roomService.updateRoomSettings(TestFields.ROOM_ID, request));
        assertEquals(ProblemError.INVALID_NUMBER_REQUEST, exception.getError());
    }

    @Test
    public void updateRoomSettingsChoosingProblemsModifiesNumProblems() {
        /**
         * 1. Update the room with two problems
         * 2. Verify the numProblems field is now set to 2
         * 3. Get rid of all the problems
         * 4. Verify the numProblems field is now set to 1
         */

        Room room = new Room();
        room.setRoomId(TestFields.ROOM_ID);
        User host = new User();
        host.setNickname(TestFields.NICKNAME);
        room.setHost(host);
        room.addUser(host);

        Mockito.doReturn(room).when(repository).findRoomByRoomId(eq(TestFields.ROOM_ID));
        Mockito.doReturn(new Problem()).when(problemService).getProblemEntity(Mockito.any());

        UpdateSettingsRequest request = new UpdateSettingsRequest();
        request.setInitiator(UserMapper.toDto(host));
        request.setNumProblems(1);

        SelectableProblemDto problemDto = new SelectableProblemDto();
        problemDto.setProblemId(TestFields.PROBLEM_ID);
        SelectableProblemDto problemDto2 = new SelectableProblemDto();
        problemDto.setProblemId(TestFields.PROBLEM_ID_2);
        request.setProblems(Arrays.asList(problemDto, problemDto2));

        RoomDto response = roomService.updateRoomSettings(TestFields.ROOM_ID, request);
        assertEquals(2, response.getNumProblems());

        request.setProblems(Collections.emptyList());
        response = roomService.updateRoomSettings(TestFields.ROOM_ID, request);
        assertEquals(1, response.getNumProblems());
    }

    @Test
    public void removeUserSuccessHostInitiator() {
        Room room = new Room();
        room.setRoomId(TestFields.ROOM_ID);

        User host = new User();
        host.setNickname(TestFields.NICKNAME);
        host.setUserId(TestFields.USER_ID);

        room.setHost(host);
        room.addUser(host);

        User user = new User();
        user.setNickname(TestFields.NICKNAME_2);
        user.setUserId(TestFields.USER_ID_2);
        room.addUser(user);

        Mockito.doReturn(room).when(repository).findRoomByRoomId(eq(TestFields.ROOM_ID));

        RemoveUserRequest request = new RemoveUserRequest();
        request.setInitiator(UserMapper.toDto(host));
        request.setUserToDelete(UserMapper.toDto(user));
        RoomDto response = roomService.removeUser(TestFields.ROOM_ID, request);

        verify(socketService).sendSocketUpdate(eq(response));
        assertEquals(1, response.getUsers().size());
        assertFalse(response.getUsers().contains(UserMapper.toDto(user)));
    }

    @Test
    public void removeUserSuccessSelfInitiator() {
        Room room = new Room();
        room.setRoomId(TestFields.ROOM_ID);

        User host = new User();
        host.setNickname(TestFields.NICKNAME);
        host.setUserId(TestFields.USER_ID);

        room.setHost(host);
        room.addUser(host);

        User user = new User();
        user.setNickname(TestFields.NICKNAME_2);
        user.setUserId(TestFields.USER_ID_2);
        room.addUser(user);

        Mockito.doReturn(room).when(repository).findRoomByRoomId(eq(TestFields.ROOM_ID));

        RemoveUserRequest request = new RemoveUserRequest();
        request.setInitiator(UserMapper.toDto(user));
        request.setUserToDelete(UserMapper.toDto(user));
        RoomDto response = roomService.removeUser(TestFields.ROOM_ID, request);

        verify(socketService).sendSocketUpdate(eq(response));
        assertEquals(1, response.getUsers().size());
        assertFalse(response.getUsers().contains(UserMapper.toDto(user)));
    }

    @Test
    public void removeHost() {
        Room room = new Room();
        room.setRoomId(TestFields.ROOM_ID);

        User host = new User();
        host.setNickname(TestFields.NICKNAME);
        host.setUserId(TestFields.USER_ID);
        host.setSessionId(TestFields.SESSION_ID);

        room.setHost(host);
        room.addUser(host);

        User user = new User();
        user.setNickname(TestFields.NICKNAME_2);
        user.setUserId(TestFields.USER_ID_2);
        user.setSessionId(TestFields.SESSION_ID_2);
        room.addUser(user);

        Mockito.doReturn(room).when(repository).findRoomByRoomId(eq(TestFields.ROOM_ID));

        RemoveUserRequest request = new RemoveUserRequest();
        request.setInitiator(UserMapper.toDto(host));
        request.setUserToDelete(UserMapper.toDto(host));
        RoomDto response = roomService.removeUser(TestFields.ROOM_ID, request);

        verify(socketService).sendSocketUpdate(eq(response));
        assertEquals(1, response.getUsers().size());
        assertEquals(UserMapper.toDto(user), response.getHost());
        assertFalse(response.getUsers().contains(UserMapper.toDto(host)));
    }

    @Test
    public void removeNonExistentUser() {
        Room room = new Room();
        room.setRoomId(TestFields.ROOM_ID);

        User host = new User();
        host.setNickname(TestFields.NICKNAME);
        host.setUserId(TestFields.USER_ID);

        room.setHost(host);
        room.addUser(host);

        Mockito.doReturn(room).when(repository).findRoomByRoomId(eq(TestFields.ROOM_ID));

        User user = new User();
        user.setUserId(TestFields.USER_ID_2);

        RemoveUserRequest request = new RemoveUserRequest();
        request.setInitiator(UserMapper.toDto(host));
        request.setUserToDelete(UserMapper.toDto(user));

        ApiException exception = assertThrows(ApiException.class, () ->
                roomService.removeUser(TestFields.ROOM_ID, request));
        assertEquals(UserError.NOT_FOUND, exception.getError());
    }

    @Test
    public void removeUserBadHost() {
        Room room = new Room();
        room.setRoomId(TestFields.ROOM_ID);

        User host = new User();
        host.setNickname(TestFields.NICKNAME);
        host.setUserId(TestFields.USER_ID);

        room.setHost(host);
        room.addUser(host);

        User user = new User();
        user.setNickname(TestFields.NICKNAME_2);
        user.setUserId(TestFields.USER_ID_2);
        room.addUser(user);

        User user2 = new User();
        user2.setNickname(TestFields.NICKNAME_2);
        user2.setUserId(TestFields.USER_ID_3);
        room.addUser(user2);

        Mockito.doReturn(room).when(repository).findRoomByRoomId(eq(TestFields.ROOM_ID));

        RemoveUserRequest request = new RemoveUserRequest();
        request.setInitiator(UserMapper.toDto(user));
        request.setUserToDelete(UserMapper.toDto(user2));

        ApiException exception = assertThrows(ApiException.class, () ->
                roomService.removeUser(TestFields.ROOM_ID, request));
        assertEquals(RoomError.INVALID_PERMISSIONS, exception.getError());
    }

    @Test
    public void removeUserRoomNotFound() {
        User host = new User();
        host.setUserId(TestFields.USER_ID);

        User user = new User();
        user.setUserId(TestFields.USER_ID_2);

        Mockito.doReturn(null).when(repository).findRoomByRoomId(eq(TestFields.ROOM_ID));

        RemoveUserRequest request = new RemoveUserRequest();
        request.setInitiator(UserMapper.toDto(host));
        request.setUserToDelete(UserMapper.toDto(user));

        ApiException exception = assertThrows(ApiException.class, () ->
                roomService.removeUser(TestFields.ROOM_ID, request));
        assertEquals(RoomError.NOT_FOUND, exception.getError());
    }

    @Test
    public void conditionallyUpdateRoomHostSuccess() {
        User user1 = new User();
        user1.setUserId(TestFields.USER_ID);
        user1.setSessionId(TestFields.SESSION_ID);

        User user2 = new User();
        user2.setUserId(TestFields.USER_ID_2);

        User user3 = new User();
        user3.setSessionId(TestFields.USER_ID_3);
        user3.setSessionId(TestFields.SESSION_ID_2);

        Room room = new Room();
        room.setRoomId(TestFields.ROOM_ID);
        room.setHost(user1);
        room.addUser(user1);
        room.addUser(user2);
        room.addUser(user3);

        // Passing in a non-host user has no effect on the room
        roomService.conditionallyUpdateRoomHost(room, user2, false);
        assertEquals(user1, room.getHost());

        Mockito.doReturn(room).when(repository).findRoomByRoomId(eq(room.getRoomId()));

        // Passing in the host will assign the first active user to be the new host
        roomService.conditionallyUpdateRoomHost(room, user1, false);
        assertEquals(user3, room.getHost());

        verify(repository).save(room);
    }

    @Test
    public void deleteRoomSuccess() {
        User host = new User();
        host.setUserId(TestFields.USER_ID);
        host.setSessionId(TestFields.SESSION_ID);

        User user = new User();
        user.setUserId(TestFields.USER_ID_2);
        user.setSessionId(TestFields.SESSION_ID_2);

        Room room = new Room();
        room.setRoomId(TestFields.ROOM_ID);
        room.setHost(host);
        room.addUser(host);
        room.addUser(user);

        Mockito.doReturn(room).when(repository).findRoomByRoomId(eq(TestFields.ROOM_ID));

        DeleteRoomRequest deleteRoomRequest = new DeleteRoomRequest();
        deleteRoomRequest.setHost(UserMapper.toDto(host));
        roomService.deleteRoom(TestFields.ROOM_ID, deleteRoomRequest);
        verify(repository).delete(eq(room));
    }

    @Test
    public void deleteRoomBadHost() {
        User host = new User();
        host.setUserId(TestFields.USER_ID);
        host.setSessionId(TestFields.SESSION_ID);

        User user = new User();
        user.setUserId(TestFields.USER_ID_2);
        user.setSessionId(TestFields.SESSION_ID_2);

        Room room = new Room();
        room.setRoomId(TestFields.ROOM_ID);
        room.setHost(host);
        room.addUser(host);
        room.addUser(user);

        Mockito.doReturn(room).when(repository).findRoomByRoomId(eq(TestFields.ROOM_ID));

        DeleteRoomRequest deleteRoomRequest = new DeleteRoomRequest();
        deleteRoomRequest.setHost(UserMapper.toDto(user));

        ApiException exception = assertThrows(ApiException.class, () ->
                roomService.deleteRoom(TestFields.ROOM_ID, deleteRoomRequest));
        assertEquals(RoomError.INVALID_PERMISSIONS, exception.getError());
        assertNotNull(roomService.getRoom(TestFields.ROOM_ID));
    }

    @Test
    public void setSpectatorSuccess() {
        /**
         * Tests ProblemService.setSpectator(roomId, spectator, SetSpectatorRequest)
         * SetSpectatorRequest has initiator and receiver user; initiator must be
         * either same person as receiver or is the host. Create new room, add two users,
         * set one as host, and test setSpectator between host and non-host.
         */
        User firstUser = new User();
        firstUser.setNickname(TestFields.NICKNAME);
        firstUser.setUserId(TestFields.USER_ID);
        User secondUser = new User();
        secondUser.setNickname(TestFields.NICKNAME_2);
        secondUser.setUserId(TestFields.USER_ID_2);

        Room room = new Room();
        room.setRoomId(TestFields.ROOM_ID);
        room.setHost(firstUser);
        room.addUser(firstUser);
        room.addUser(secondUser);

        // Mock repository to return room when called
        Mockito.doReturn(room).when(repository).findRoomByRoomId(eq(TestFields.ROOM_ID));

        SetSpectatorRequest request1 = new SetSpectatorRequest();
        request1.setInitiator(UserMapper.toDto(firstUser));
        request1.setReceiver(UserMapper.toDto(secondUser));
        request1.setSpectator(true);
        RoomDto response1 = roomService.setSpectator(TestFields.ROOM_ID, request1);
        assertTrue(response1.getUsers().get(1).getSpectator());

        SetSpectatorRequest request2 = new SetSpectatorRequest();
        request2.setInitiator(UserMapper.toDto(secondUser));
        request2.setReceiver(UserMapper.toDto(secondUser));
        request2.setSpectator(true);
        RoomDto response2 = roomService.setSpectator(TestFields.ROOM_ID, request2);
        assertTrue(response2.getUsers().get(1).getSpectator());
        verify(repository, times(2)).save(room);
    }

    @Test
    public void setSpectatorFailureInvalidPermissions() {
        /**
         * secondUser (non-host) will try to set the firstUser (host) as a spectator
         * expected result: RoomError.INVALID_PERMISSIONS
         */
        User firstUser = new User();
        firstUser.setNickname(TestFields.NICKNAME);
        firstUser.setUserId(TestFields.USER_ID);
        User secondUser = new User();
        secondUser.setNickname(TestFields.NICKNAME_2);
        secondUser.setUserId(TestFields.USER_ID_2);

        Room room = new Room();
        room.setRoomId(TestFields.ROOM_ID);
        room.setHost(firstUser);
        room.addUser(firstUser);
        room.addUser(secondUser);

        // Mock repository to return room when called
        Mockito.doReturn(room).when(repository).findRoomByRoomId(eq(TestFields.ROOM_ID));

        SetSpectatorRequest request = new SetSpectatorRequest();
        request.setInitiator(UserMapper.toDto(secondUser));
        request.setReceiver(UserMapper.toDto(firstUser));
        request.setSpectator(true);

        ApiException exception = assertThrows(ApiException.class, () ->
                roomService.setSpectator(TestFields.ROOM_ID, request));
        assertEquals(RoomError.INVALID_PERMISSIONS, exception.getError());
    }

    @Test
    public void setSpectatorFailureUserNotFound() {
        /**
         * secondUser will not be added to the room at all, but will still be a user.
         * expected result: RoomError.USER_NOT_FOUND
         */
        User firstUser = new User();
        firstUser.setNickname(TestFields.NICKNAME);
        firstUser.setUserId(TestFields.USER_ID);
        User secondUser = new User();
        secondUser.setNickname(TestFields.NICKNAME_2);
        secondUser.setUserId(TestFields.USER_ID_2);

        Room room = new Room();
        room.setRoomId(TestFields.ROOM_ID);
        room.setHost(firstUser);
        room.addUser(firstUser);

        // Mock repository to return room when called
        Mockito.doReturn(room).when(repository).findRoomByRoomId(eq(TestFields.ROOM_ID));

        SetSpectatorRequest request = new SetSpectatorRequest();
        request.setInitiator(UserMapper.toDto(firstUser));
        request.setReceiver(UserMapper.toDto(secondUser));
        request.setSpectator(true);

        ApiException exception = assertThrows(ApiException.class, () ->
                roomService.setSpectator(TestFields.ROOM_ID, request));
        assertEquals(RoomError.USER_NOT_FOUND, exception.getError());
    }
}
