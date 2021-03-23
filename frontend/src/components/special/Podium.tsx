import React, { useEffect, useState } from 'react';
import styled from 'styled-components';
import { Player, Submission } from '../../api/Game';
import { Text, MediumText } from '../core/Text';
import Language, { displayNameFromLanguage } from '../../api/Language';

type PodiumProps = {
  place: number,
  player: Player | undefined,
  gameStartTime: string,
};

type MedalProps = {
  color: string,
};

type HeightProps = {
  height: number,
};

const Content = styled.div`
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
`;

const PodiumContainer = styled.div<HeightProps>`
  position: relative;
  display: inline-block;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.12);
  width: 180px;
  height: ${({ height }) => height}px;
  padding: 10px;
  margin: 10px;
  background: ${({ theme }) => theme.colors.white};
  border-radius: 8px;
`;

const WinnerText = styled(MediumText)`
  font-weight: bold;
  margin: 0;
`;

const ScoreText = styled(MediumText)`
  font-weight: normal;
  margin: 0;
`;

const SmallerText = styled(Text)`
  margin: 5px 0;
`;

const BottomContent = styled.div`
  position: absolute;
  bottom: 5px;
  left: 0;
  right: 0;
  margin: 0 auto;
`;

const Medal = styled.div<MedalProps>`
  background: ${({ theme, color }) => theme.colors.gradients[color]};
  border-radius: 50%;
  margin: 15px auto;
  
  height: 50px;
  width: 50px;  
  line-height: 50px;
`;

function Podium(props: PodiumProps) {
  const { place, player, gameStartTime } = props;

  const [bestSubmission, setBestSubmission] = useState<Submission | null>(null);

  useEffect(() => {
    if (player) {
      // Find best submission
      player.submissions.forEach((submission) => {
        if (!bestSubmission || submission.numCorrect > bestSubmission.numCorrect) {
          setBestSubmission(submission);
        }
      });
    }
  }, [player, setBestSubmission]);

  const getMedalColor = () => {
    switch (place) {
      case 1:
        return 'yellow';
      case 2:
        return 'silver';
      default:
        return 'bronze';
    }
  };

  const getPodiumHeight = () => {
    switch (place) {
      case 1:
        return 280;
      case 2:
        return 240;
      default:
        return 200;
    }
  };

  const getScoreText = () => {
    if (!bestSubmission) {
      return <ScoreText />;
    }

    const percent = Math.round((bestSubmission.numCorrect / bestSubmission.numTestCases) * 100);
    return (
      <ScoreText>
        Scored
        <b>{` ${percent}%`}</b>
      </ScoreText>
    );
  };

  const getTimeText = () => {
    if (!bestSubmission) {
      return <SmallerText />;
    }

    // Calculate time from start of game till best submission
    const startTime = new Date(gameStartTime).getTime();
    const diffMilliseconds = new Date(bestSubmission.startTime).getTime() - startTime;
    const diffMinutes = Math.floor(diffMilliseconds / (60 * 1000));

    return (
      <SmallerText>
        in
        <b>{` ${diffMinutes} minutes`}</b>
      </SmallerText>
    );
  };

  const getLanguageText = () => {
    if (!bestSubmission) {
      return <SmallerText />;
    }

    return (
      <SmallerText>
        Language:
        <b>{` ${displayNameFromLanguage(bestSubmission.language as Language)}`}</b>
      </SmallerText>
    );
  };

  return (
    <Content>
      <div>
        <WinnerText>{player?.user.nickname || 'Invite'}</WinnerText>
        <PodiumContainer height={getPodiumHeight()}>
          <Medal color={getMedalColor()} />
          {getScoreText()}
          {getTimeText()}

          <BottomContent>
            {getLanguageText()}
          </BottomContent>
        </PodiumContainer>
      </div>
    </Content>
  );
}

export default Podium;
