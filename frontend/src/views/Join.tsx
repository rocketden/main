import React, { useState, useEffect, ReactElement } from 'react';
import { Message } from 'stompjs';
import ErrorMessage from '../components/core/Error';
import { LargeText, Text, UserNicknameText } from '../components/core/Text';
import { LargeCenterInputText, LargeInputButton } from '../components/core/Input';
import {
  isValidNickname, connect, subscribe, deleteUser, User,
  SOCKET_ENDPOINT, SUBSCRIBE_URL, addUser,
} from '../api/Socket';

function JoinGamePage() {
  // Declare nickname state variable.
  const [nickname, setNickname] = useState('');

  // Hold error text.
  const [error, setError] = useState('');

  /**
   * This is updated whenever the nickname changes.
   */
  const [validNickname, setValidNickname] = useState(false);
  useEffect(() => {
    setValidNickname(isValidNickname(nickname));
  }, [nickname]);

  // Variable to hold whether the user is focused on the text input field.
  const [focusInput, setFocusInput] = useState(false);

  // Variable to hold the users on the page.
  const [users, setUsers] = useState<User[]>([]);

  /**
   * Stores the current page state, where:
   * 0 = Enter room ID state (currently unused)
   * 1 = Enter nickname state
   * 2 = Waiting room state
   */
  const [pageState, setPageState] = useState(1);

  /**
   * Subscribe callback that will be triggered on every message.
   * Update the users list.
   */
  const subscribeCallback = (result: Message) => {
    const userObjects:User[] = JSON.parse(result.body);
    setUsers(userObjects);
  };

  /**
   * Add the user to the waiting room through the following steps.
   * 1. Connect the user to the socket.
   * 2. Subscribe the user to future messages.
   * 3. Send the user nickname to the room.
   * 4. Update the room layout to the "waiting room" page.
   * This method also handles any relevant errors.
   */
  const addUserToWaitingRoom = (socketEndpoint: string,
    subscribeUrl: string, nicknameParam: string) => {
    connect(socketEndpoint).then(() => {
      subscribe(subscribeUrl, subscribeCallback).then(() => {
        try {
          addUser(nicknameParam);
          setPageState(2);
        } catch (err) {
          setError(err.message);
        }
      }).catch((err) => {
        setError(err.message);
      });
    }).catch((err) => {
      setError(err.message);
    });
  };

  // Create variable to hold the "Join Page" content.
  let joinPageContent: ReactElement | undefined;

  switch (pageState) {
    case 1:
      // Render the "Enter nickname" state.
      joinPageContent = (
        <div>
          <LargeText>Enter a nickname to join the game!</LargeText>
          <LargeCenterInputText
            placeholder="Your nickname"
            onChange={(event: React.ChangeEvent<HTMLInputElement>) => {
              setNickname(event.target.value);
            }}
            onFocus={() => {
              setFocusInput(true);
            }}
            onBlur={() => {
              setFocusInput(false);
            }}
            onKeyPress={(event) => {
              if (event.key === 'Enter' && validNickname) {
                addUserToWaitingRoom(SOCKET_ENDPOINT, SUBSCRIBE_URL, nickname);
              }
            }}
          />
          <LargeInputButton
            onClick={() => {
              addUserToWaitingRoom(SOCKET_ENDPOINT, SUBSCRIBE_URL, nickname);
            }}
            value="Enter"
            // Input is disabled if no nickname exists, has a space, or is too long.
            disabled={!validNickname}
          />
          { focusInput && !validNickname ? (
            <Text>
              The nickname must be non-empty, have no spaces, and be less than 16 characters.
            </Text>
          ) : null}
          { error ? <ErrorMessage message={error} /> : null }
        </div>
      );
      break;
    case 2:
      // Render the Waiting room state.
      joinPageContent = (
        <div>
          <LargeText>
            You have entered the waiting room! Your nickname is &quot;
            {nickname}
            &quot;.
          </LargeText>
          <div>
            {
              users.map((user) => (
                <UserNicknameText onClick={(event) => {
                  deleteUser((event.target as HTMLElement).innerText);
                }}
                >
                  {user.nickname}
                </UserNicknameText>
              ))
            }
          </div>
        </div>
      );
      break;
    default:
  }

  return joinPageContent;
}

export default JoinGamePage;
