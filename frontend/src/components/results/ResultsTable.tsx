import React from 'react';
import styled from 'styled-components';
import { Player } from '../../api/Game';
import PlayerResultsItem from './PlayerResultsItem';
import { User } from '../../api/User';

const Content = styled.table`
  
`;

type ResultsTableProps = {
  players: Player[],
  currentUser: User | null,
};

function ResultsTable(props: ResultsTableProps) {
  const {
    players, currentUser,
  } = props;

  return (
    <Content>
      <tr>
        <th>Player</th>
        <th>Score</th>
        <th>Time</th>
        <th>Submissions</th>
        <th>Code</th>
      </tr>
      {players?.map((player, index) => (
        <PlayerResultsItem
          player={player}
          place={index + 1}
          isCurrentPlayer={currentUser?.userId === player.user.userId}
          color={player.color}
        />
      ))}
    </Content>
  );
}

export default ResultsTable;
