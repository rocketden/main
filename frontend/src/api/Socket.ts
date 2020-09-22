/* eslint-disable consistent-return */
import SockJS from 'sockjs-client';
import Stomp, { Client, Message } from 'stompjs';
import { errorHandler } from './Error';

let stompClient: Client;

export type User = {
  nickname: string;
}

// Variable to hold the current connected state.
let connected: boolean = false;

// Dynamic route endpoints that depend on the room id
const basePath = '/api/v1/socket';
let socketRoomId: string;
export const routes = (roomId: string) => {
  socketRoomId = roomId;
  return {
    connect: `${basePath}/${roomId}/join-room-endpoint`,
    subscribe: `${basePath}/${roomId}/subscribe-user`,
    addUser: `${basePath}/${roomId}/add-user`,
    deleteUser: `${basePath}/${roomId}/delete-user`,
  };
};

/**
 * The requirements for validity are as follows:
 * 1. Non-empty
 * 2. Less than or equal to sixteen characters
 * 3. Contains no spaces
 */
export const isValidNickname = (nickname: string) => nickname.length > 0
  && !nickname.includes(' ') && nickname.length <= 16;

/**
 * Add the user by sending a message via socket.
 * @returns void, or error if socket is not connected or nickname is invalid.
*/
export const addUser = (nickname:string): void => {
  if (connected) {
    stompClient.send(routes(socketRoomId).addUser, {}, nickname);
  } else if (!connected) {
    throw errorHandler('The socket is not connected.');
  } else {
    throw errorHandler('The provided nickname is invalid.');
  }
};

/**
 * Delete the user by sending a message via socket.
 * @returns void, or error if socket is not connected or nickname is invalid.
*/
export const deleteUser = (nickname:string): void => {
  if (connected) {
    stompClient.send(routes(socketRoomId).deleteUser, {}, nickname);
  } else if (!connected) {
    throw errorHandler('The socket is not connected.');
  } else {
    throw errorHandler('The provided nickname is invalid.');
  }
};

/**
 * Connect the user via socket.
 * @returns void Promise, reject if socket is already connected
 * or fails to connect.
*/
export const connect = (roomId:string):
  Promise<void> => new Promise<void>((resolve, reject) => {
    if (!connected) {
      // Connect to given endpoint, subscribe to future messages, and send user message.
      socketRoomId = roomId;
      const socket: WebSocket = new SockJS(routes(socketRoomId).connect);
      stompClient = Stomp.over(socket);
      stompClient.connect({}, () => {
        // Reassign connected variable.
        connected = true;
        resolve();
      }, () => {
        reject(errorHandler('The socket failed to connect.'));
      });
    } else {
      reject(errorHandler('The socket is already connected.'));
    }
  });

/**
 * Subscribe the user via socket.
 * @returns void Promise, reject if socket is not connected.
 */
export const subscribe = (subscribeUrl: string,
  subscribeCallback: (users: Message) => void):
  Promise<void> => new Promise<void>((resolve, reject) => {
    if (connected) {
      stompClient.subscribe(subscribeUrl, subscribeCallback);
      resolve();
    } else {
      reject(errorHandler('The socket is not connected.'));
    }
  });

/**
 * Disconnect the user by sending a message via socket.
 * @returns void, or error if socket is not connected.
*/
export const disconnect = (): void => {
  if (connected) {
    const socket: WebSocket = new SockJS(routes(socketRoomId).connect);
    stompClient = Stomp.over(socket);
    stompClient.disconnect(() => {
      // Reassign connected variable.
      connected = false;
    });
  } else {
    throw errorHandler('The socket is not connected.');
  }
};
