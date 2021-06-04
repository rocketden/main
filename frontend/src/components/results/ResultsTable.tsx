/* eslint-disable jsx-a11y/control-has-associated-label */
import React from 'react';
import styled from 'styled-components';
import { Player } from '../../api/Game';
import PlayerResultsItem from './PlayerResultsItem';
import { User } from '../../api/User';

const Content = styled.table`
  text-align: center;
  width: 65%;
  min-width: 600px;
  margin: 0 auto;
  
  border-collapse: separate; 
  border-spacing: 0 10px;
  
  tr td:first-child {
    border-top-left-radius: 5px;
    border-bottom-left-radius: 5px;
  }

  tr td:last-child {
    border-top-right-radius: 5px;
    border-bottom-right-radius: 5px;
  }
`;

const PrimaryTableHeader = styled.th`
  text-align: left;
`;

const SmallColumn = styled.th`
  width: 100px;
`;

type ResultsTableProps = {
  players: Player[],
  currentUser: User | null,
  gameStartTime: string,
  viewPlayerCode: (index: number) => void,
};

function ResultsTable(props: ResultsTableProps) {
  const {
    players, currentUser, gameStartTime, viewPlayerCode,
  } = props;

  return (
    <Content>
      <tr>
        <th />
        <PrimaryTableHeader>Player</PrimaryTableHeader>
        <th>Score</th>
        <th>Time</th>
        <SmallColumn>Submissions</SmallColumn>
        <th>Code</th>
      </tr>
      {players?.map((player, index) => (
        <PlayerResultsItem
          player={player}
          place={index + 1}
          isCurrentPlayer={currentUser?.userId === player.user.userId}
          gameStartTime={gameStartTime}
          color={player.color}
          onViewCode={() => viewPlayerCode(index)}
        />
      ))}
    </Content>
  );
}

export default ResultsTable;
