package com.codejoust.main.controller.v1;

import com.codejoust.main.dto.room.CreateRoomRequest;
import com.codejoust.main.dto.room.DeleteRoomRequest;
import com.codejoust.main.dto.room.JoinRoomRequest;
import com.codejoust.main.dto.room.RemoveUserRequest;
import com.codejoust.main.dto.room.RoomDto;
import com.codejoust.main.dto.room.SetSpectatorRequest;
import com.codejoust.main.dto.room.UpdateHostRequest;
import com.codejoust.main.dto.room.UpdateSettingsRequest;
import com.codejoust.main.service.RoomService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RoomController extends BaseRestController {

    private final RoomService service;

    @Autowired
    public RoomController(RoomService service) {
        this.service = service;
    }

    @GetMapping("/rooms/{roomId}")
    public ResponseEntity<RoomDto> getRoom(@PathVariable String roomId) {
        return new ResponseEntity<>(service.getRoom(roomId), HttpStatus.OK);
    }

    @PutMapping("/rooms/{roomId}/users")
    public ResponseEntity<RoomDto> joinRoom(@PathVariable String roomId,@RequestBody JoinRoomRequest request, @RequestHeader(name="Authorization", required = false) String token) {
        return new ResponseEntity<>(service.joinRoom(roomId, request, token), HttpStatus.OK);
    }

    @PostMapping("/rooms")
    public ResponseEntity<RoomDto> createRoom(@RequestBody CreateRoomRequest request, @RequestHeader(name="Authorization", required = false) String token) {
        return new ResponseEntity<>(service.createRoom(request, token), HttpStatus.CREATED);
    }

    @DeleteMapping("/rooms/{roomId}")
    public ResponseEntity<RoomDto> deleteRoom(@PathVariable String roomId, @RequestBody DeleteRoomRequest request) {
        return new ResponseEntity<>(service.deleteRoom(roomId, request), HttpStatus.OK);
    }

    @DeleteMapping("/rooms/{roomId}/users")
    public ResponseEntity<RoomDto> removeUser(@PathVariable String roomId,
                                              @RequestBody RemoveUserRequest request) {
        return new ResponseEntity<>(service.removeUser(roomId, request), HttpStatus.OK);
    }

    @PutMapping("/rooms/{roomId}/host")
    public ResponseEntity<RoomDto> updateRoomHost(@PathVariable String roomId,
                                                  @RequestBody UpdateHostRequest request) {
        return new ResponseEntity<>(service.updateRoomHost(roomId, request, false), HttpStatus.OK);
    }

    @PutMapping("/rooms/{roomId}/settings")
    public ResponseEntity<RoomDto> updateRoomSettings(@PathVariable String roomId,
                                                      @RequestBody UpdateSettingsRequest request) {
        return new ResponseEntity<>(service.updateRoomSettings(roomId, request), HttpStatus.OK);
    }

    @PostMapping("/rooms/{roomId}/spectator")
    public ResponseEntity<RoomDto> setSpectator(@PathVariable String roomId, @RequestBody SetSpectatorRequest request) {
        return new ResponseEntity<>(service.setSpectator(roomId, request), HttpStatus.OK);
    }
}
