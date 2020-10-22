package com.rocketden.main.model;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer id;

    private String roomId;

    private LocalDateTime createdDateTime = LocalDateTime.now();

    // host_id column in room table holds the primary key of the user host
    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "host_id")
    private User host;

    /**
     * Generated from all the matching room variables in the User class.
     * If the room is deleted or users removed from this list, those users will also be deleted.
     * Setter is set to private to ensure proper use of addUser and removeUser methods.
     */
    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, orphanRemoval = true)
    @Setter(AccessLevel.PRIVATE)
    private List<User> users = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    private ProblemDifficulty difficulty = ProblemDifficulty.RANDOM;

    public void addUser(User user) {
        users.add(user);
        user.setRoom(this);
    }

    // Removes user if the nicknames match (based on equals/hashCode implementation)
    // Note: if we switch to permanent users and disable orphanRemoval, this will
    // need to call getEquivalentUser(user).setRoom(null) first
    public boolean removeUser(User user) {
        return users.remove(user);
    }

    /**
     * Given a user object, return the equivalent user object in this room's list
     * of users, or null if it doesn't exist. The reason this is necessary is
     * because, while a user object constructed from a client's UserDto might
     * "equal" a user in the room at the application level, they are not the same at
     * the database level (different primary keys). This method therefore returns the
     * correct user object that's equal at both the application and database levels.
     *
     * @param userToFind the desired user to find
     * @return the equivalent user in the database, or null if none exists
     */
    public User getEquivalentUser(User userToFind) {
        int index = users.indexOf(userToFind);
        if (index >= 0) {
            return users.get(index);
        }
        return null;
    }

    /**
     * Determine whether a user with the given nickname already exists
     * in the room. Capitalization matters.
     * 
     * @param nickname The provided nickname we are searching for.
     * @return true if a user with the provided nickname exists in the room,
     * false if no user with that nickname is found.
     */
    public boolean containsUserWithNickname(String nickname) {
        for (User roomUser : this.users) {
            if (roomUser.getNickname().equals(nickname)) {
                return true;
            }
        }
        return false;
    }
}
