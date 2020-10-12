import axios from 'axios';
import { axiosErrorHandler } from './Error';
import { User } from './User';
import Difficulty from './Difficulty';

export type Room = {
  roomId: string;
  host: User;
  users: [User],
  difficulty: Difficulty | null,
};

export type CreateRoomParams = {
  host: User;
};

export type JoinRoomParams = {
  roomId: string,
  user: User,
};

export type UpdateSettingsParams = {
  initiator: User,
  difficulty: Difficulty | null,
}

export type ChangeHostParams = {
  initiator: User,
  newHost: User,
}

const basePath = '/api/v1/rooms';
const routes = {
  createRoom: `${basePath}/`,
  joinRoom: `${basePath}/`,
  getRoom: `${basePath}/`,
  updateRoomSettings: (roomId: string) => `${basePath}/${roomId}/settings`,
  changeRoomHost: (roomId: string) => `${basePath}/${roomId}/host`,
};

export const createRoom = (roomParams: CreateRoomParams):
  Promise<Room> => axios.post<Room>(routes.createRoom, roomParams)
  .then((res) => res.data)
  .catch((err) => {
    throw axiosErrorHandler(err);
  });

export const joinRoom = (roomParams: JoinRoomParams):
  Promise<Room> => axios.put<Room>(routes.joinRoom, roomParams)
  .then((res) => res.data)
  .catch((err) => {
    throw axiosErrorHandler(err);
  });

export const getRoom = (roomId: string):
  Promise<Room> => axios.get<Room>(`${routes.getRoom}?roomId=${roomId}`)
  .then((res) => res.data)
  .catch((err) => {
    throw axiosErrorHandler(err);
  });

export const updateRoomSettings = (roomId: string, roomParams: UpdateSettingsParams):
  Promise<Room> => axios.put<Room>(routes.updateRoomSettings(roomId), roomParams)
  .then((res) => res.data)
  .catch((err) => {
    throw axiosErrorHandler(err);
  });

export const changeRoomHost = (roomId: string, roomParams: ChangeHostParams):
  Promise<Room> => axios.put<Room>(routes.changeRoomHost(roomId), roomParams)
  .then((res) => res.data)
  .catch((err) => {
    throw axiosErrorHandler(err);
  });
