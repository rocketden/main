import React, { useEffect, useState } from 'react';
import styled from 'styled-components';
import { useLocation, useHistory } from 'react-router-dom';
import { Message } from 'stompjs';
import { LargeText, Text } from '../components/core/Text';
import { getGame, Game, Player, playAgain } from '../api/Game';
import { checkLocationState } from '../util/Utility';
import { errorHandler } from '../api/Error';
import PlayerResultsCard from '../components/card/PlayerResultsCard';
import { PrimaryButton } from '../components/core/Button';
import { Room } from '../api/Room';
import ErrorMessage from '../components/core/Error';
import Loading from '../components/core/Loading';
import { connect, disconnect, routes, subscribe } from '../api/Socket';
import { User } from '../api/User';

const Content = styled.div`
  padding: 0 20%;
`;

type LocationState = {
  roomId: string,
  currentUser: User,
};

function GameResultsPage() {
  const history = useHistory();
  const location = useLocation<LocationState>();

  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const [players, setPlayers] = useState<Player[]>([]);
  const [currentUser, setCurrentUser] = useState<User | null>(null);
  const [host, setHost] = useState<User | null>(null);
  const [roomId, setRoomId] = useState('');

  useEffect(() => {
    if (checkLocationState(location, 'roomId', 'currentUser')) {
      setRoomId(location.state.roomId);
      setCurrentUser(location.state.currentUser);

      // Function that's called when playAgain is triggered
      const playAgainAction = (game: Game) => {
        disconnect()
          .then(() => {
            history.replace(`/game/lobby?room=${game.room.roomId}`, {
              user: location.state.currentUser,
              roomId: game.room.roomId,
            });
          })
          .catch((err) => setError(err.message));
      };

      // Get latest game information
      getGame(location.state.roomId).then((res) => {
        setPlayers(res.players);
        setHost(res.room.host);

        // Check if host elected to play again
        if (res.playAgain) {
          playAgainAction(res);
        }

        const subscribeCallback = (result: Message) => {
          const updatedGame: Game = JSON.parse(result.body);

          // Disconnect users from socket and then redirect them to the lobby page
          if (updatedGame.playAgain) {
            playAgainAction(updatedGame);
          }
        };

        connect(res.room.roomId, location.state.currentUser.userId!).then(() => {
          subscribe(routes(res.room.roomId).subscribe_game, subscribeCallback)
            .catch((err) => setError(err.message));
        }).catch((err) => setError(err.message));
      }).catch((err) => setError(err.message));
    } else {
      history.replace('/game/join', {
        error: errorHandler('Please join and play a game before viewing the results page.'),
      });
    }
  }, [location, history]);

  const playAgainAction = () => {
    setError('');
    setLoading(true);

    playAgain(roomId, { initiator: currentUser! })
      .catch((err) => {
        setLoading(false);
        setError(err.message);
      });
  };

  return (
    <Content>
      { error ? <ErrorMessage message={error} /> : null }
      { loading ? <Loading /> : null }
      <LargeText>Winners</LargeText>
      {players?.map((player, index) => (
        <PlayerResultsCard
          player={player}
          place={index + 1}
          isCurrentPlayer={currentUser?.userId === player.user.userId}
          color={player.color}
        />
      ))}

      {currentUser && currentUser?.userId === host?.userId
        ? <PrimaryButton onClick={playAgainAction}>Play Again</PrimaryButton>
        : <Text>Waiting for the host to choose whether to play again...</Text>}
    </Content>
  );
}

export default GameResultsPage;
