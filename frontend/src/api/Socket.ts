/* eslint-disable consistent-return */
import SockJS from 'sockjs-client';
import Stomp, { Client, Message, Subscription } from 'stompjs';
import { errorHandler } from './Error';

let stompClient: Client;

// Dynamic route endpoints that depend on the room id
const basePath = '/api/v1/socket';
export const routes = (roomId?: string, userId?: string) => ({
  connect: `${basePath}/join-room-endpoint`,
  subscribe_lobby: `${basePath}/${roomId}/subscribe-lobby`,
  subscribe_game: `${basePath}/${roomId}/subscribe-game`,
  subscribe_notification: `${basePath}/${roomId}/subscribe-notification`,
  subscribe_player: `${basePath}/${roomId}/subscribe-player/${userId}`,
});

/**
 * The requirements for validity are as follows:
 * 1. Non-empty
 * 2. Less than or equal to sixteen characters
 * 3. Contains no spaces
 */
export const isValidNickname = (nickname: string) => nickname.length > 0
  && !nickname.includes(' ') && nickname.length <= 16;

/**
 * Connect the user via socket.
 * @returns void Promise, reject if socket is already connected
 * or fails to connect.
*/
export const connect = (userId: string):
  Promise<void> => new Promise<void>((resolve, reject) => {
    if (!stompClient || !stompClient.connected) {
      // Connect to given endpoint, subscribe to future messages, and send user message.
      const socket: WebSocket = new SockJS(routes().connect);
      stompClient = Stomp.over(socket);
      stompClient.debug = () => {};

      // Headers to be retrieved on the backend to update the user information.
      const connectHeaders: any = {
        userId,
      };
      stompClient.connect(connectHeaders, () => {
        resolve();
      }, () => {
        reject(errorHandler('The socket failed to connect.'));
      });
    } else {
      resolve();
    }
  });

/**
 * Subscribe the user to a URL via socket.
 * @returns void Promise, reject if socket is not connected.
 */
export const subscribe = (subscribeUrl: string,
  subscribeCallback: (room: Message) => void):
  Promise<Subscription> => new Promise<Subscription>((resolve, reject) => {
    if (stompClient && stompClient.connected) {
      resolve(stompClient.subscribe(subscribeUrl, subscribeCallback));
    } else {
      reject(errorHandler('The socket is not connected.'));
    }
  });

/**
 * Subscribe the user via socket.
 * @returns void Promise, reject if socket is not subscribed to that URL.
 */
export const send = (sendUrl: string, headers?: {}, body?: string):
  Promise<void> => new Promise<void>((resolve, reject) => {
    if (stompClient && stompClient.connected) {
      stompClient.send(sendUrl, headers, body);
      resolve();
    } else {
      reject(errorHandler('The socket is not connected.'));
    }
  });

/**
 * Disconnect the user by sending a message via socket (do nothing if already disconnected).
 * @returns void
*/
export const disconnect = ():
  Promise<void> => new Promise<void>((resolve) => {
    if (stompClient && stompClient.connected) {
      // Stomp client is already set from last connection.
      stompClient.disconnect(() => {
        resolve();
      });
    } else {
      resolve();
    }
  });
