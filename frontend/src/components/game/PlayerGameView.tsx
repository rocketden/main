import React, { useCallback, useEffect, useState } from 'react';
import styled from 'styled-components';
import SplitterLayout from 'react-splitter-layout';
import MarkdownEditor from 'rich-markdown-editor';
import { useBeforeunload } from 'react-beforeunload';
import copy from 'copy-to-clipboard';
import Editor from './Editor';
import { DefaultCodeType, getDefaultCodeMap, Problem } from '../../api/Problem';
import { CenteredContainer, Panel, SplitterContainer } from '../core/Container';
import ErrorMessage from '../core/Error';
import 'react-splitter-layout/lib/index.css';
import { ProblemHeaderText, BottomFooterText } from '../core/Text';
import Console from './Console';
import Loading from '../core/Loading';
import { User } from '../../api/User';
import { displayNameFromDifficulty } from '../../api/Difficulty';
import {
  Game,
  Player, runSolution, Submission, SubmissionType, submitSolution,
} from '../../api/Game';
import LeaderboardCard from '../card/LeaderboardCard';
import { DifficultyDisplayButton } from '../core/Button';
import Language from '../../api/Language';
import {
  CopyIndicator,
  BottomCopyIndicatorContainer,
  SmallInlineCopyIcon,
  SmallInlineCopyText,
} from '../special/CopyIndicator';

const StyledMarkdownEditor = styled(MarkdownEditor)`
  margin-top: 15px;
  padding: 0;
  
  p {
    font-family: ${({ theme }) => theme.font};
  }

  // The specific list of attributes to have dark text color.
  .ProseMirror > p, blockquote, h1, h2, h3, ul, ol, table {
    color: ${({ theme }) => theme.colors.text};
  }
`;

const OverflowPanel = styled(Panel)`
  overflow-y: auto;
  height: 100%;
  padding: 0 25px;
`;

const NoPaddingPanel = styled(Panel)`
  padding: 0;
`;

const LeaderboardContent = styled.div`
  text-align: center;
  margin: 0 auto;
  width: 75%;
  overflow-x: scroll;
  white-space: nowrap;
    
  // Show shadows if there is scrollable content  
  background-image: 
    /* Shadow covers */ 
    ${({ theme: { colors: { background } } }) => `linear-gradient(to right, ${background}, ${background})`},
    ${({ theme: { colors: { background } } }) => `linear-gradient(to left, ${background}, ${background})`},
  
    /* Shadows */ 
    ${({ theme: { colors: { background } } }) => `linear-gradient(to right, rgba(0,0,0.8,.12), ${background})`},
    ${({ theme: { colors: { background } } }) => `linear-gradient(to left, rgba(0,0,0,.12), ${background})`};

  background-position: left center, right center, 0.15% center, 99.85% center;
  background-repeat: no-repeat;
  background-color: ${({ theme }) => theme.colors.background};
  background-size: 20px 100%, 20px 100%, 10px 100%, 10px 100%;
  background-attachment: local, local, scroll, scroll;
`;

type PlayerGameViewProps = {
  gameError: string,
  roomId: string,
  players: Player[],
  problems: Problem[],
  currentUser: User | null,
  game: Game | null,
};

function PlayerGameView(props: PlayerGameViewProps) {
  const {
    gameError, roomId, players, problems, currentUser, game,
  } = props;

  const [copiedEmail, setCopiedEmail] = useState(false);
  const [submission, setSubmission] = useState<Submission | null>(null);

  const [loading, setLoading] = useState<boolean>(false);
  const [error, setError] = useState<string>(gameError);

  const [currentLanguage, setCurrentLanguage] = useState<Language>(Language.Java);
  const [currentCode, setCurrentCode] = useState('');
  const [defaultCodeList, setDefaultCodeList] = useState<DefaultCodeType[]>([]);

  /**
   * Display beforeUnload message to inform the user that they may lose
   * their code / data if they leave the page.
   * Some browsers will display this message, others will display a fixed
   * message; see https://github.com/jacobbuck/react-beforeunload.
   */
  useBeforeunload(() => 'Leaving this page may cause you to lose your current code and data.');

  // const { currentUser, game } = useAppSelector((state) => state);

  const setDefaultCodeFromProblems = useCallback((problemsParam: Problem[],
    code: string, language: Language) => {
    const promises: Promise<DefaultCodeType>[] = [];
    problemsParam.forEach((problem) => {
      if (problem && problem.problemId) {
        promises.push(getDefaultCodeMap(problem.problemId));
      }
    });

    // Get the result of promises and set the default code list.
    Promise.all(promises).then((result) => {
      const codeMap = result[0];

      // If previous code and language specified, save those as defaults
      if (code) {
        codeMap[language] = code;
      }

      // Set this user's current code and language
      setCurrentCode(codeMap[language]);
      setCurrentLanguage(language);

      setDefaultCodeList(result);
    }).catch((err) => {
      setError(err.message);
    });
  }, [setDefaultCodeList, setCurrentCode, setCurrentLanguage]);

  // Map the game in Redux to the state variables used in this file
  useEffect(() => {
    if (game) {
      /**
       * If default code list is empty and current user (non-spectator) is
       * loaded, fetch the code from the backend
       */
      if (!defaultCodeList.length && currentUser && !currentUser.spectator) {
        let matchFound = false;

        // If this user refreshed and has already submitted code, load and save their latest code
        game.players.forEach((player) => {
          if (player.user.userId === currentUser?.userId && player.code) {
            setDefaultCodeFromProblems(game.problems, player.code, player.language as Language);
            matchFound = true;
          }
        });

        // If no previous code, proceed as normal with the default Java language
        if (!matchFound) {
          setDefaultCodeFromProblems(game.problems, '', Language.Java);
        }
      }
    }
  }, [game, currentUser, defaultCodeList, setDefaultCodeFromProblems]);

  // Creates Event when splitter bar is dragged
  const onSecondaryPanelSizeChange = () => {
    const event = new Event('secondaryPanelSizeChange');
    window.dispatchEvent(event);
  };

  // Callback when user runs code against custom test case
  const runCode = (input: string) => {
    setLoading(true);
    setError('');
    const request = {
      initiator: currentUser!,
      input,
      code: currentCode,
      language: currentLanguage,
    };

    runSolution(roomId, request)
      .then((res) => {
        setLoading(false);

        // Set the 'test' submission type to correctly display result.
        // eslint-disable-next-line no-param-reassign
        res.submissionType = SubmissionType.Test;
        setSubmission(res);
      })
      .catch((err) => {
        setLoading(false);
        setError(err.message);
      });
  };

  // Callback when user runs code against custom test case
  const submitCode = () => {
    setLoading(true);
    setError('');
    const request = {
      initiator: currentUser!,
      code: currentCode,
      language: currentLanguage,
    };

    submitSolution(roomId, request)
      .then((res) => {
        setLoading(false);

        // Set the 'submit' submission type to correctly display result.
        // eslint-disable-next-line no-param-reassign
        res.submissionType = SubmissionType.Submit;
        setSubmission(res);
      })
      .catch((err) => {
        setLoading(false);
        setError(err.message);
      });
  };

  const displayPlayerLeaderboard = useCallback(() => players.map((player, index) => (
    <LeaderboardCard
      player={player}
      isCurrentPlayer={player.user.userId === currentUser?.userId}
      place={index + 1}
      color={player.color}
    />
  )), [players, currentUser]);

  return (
    <>
      <LeaderboardContent>
        {displayPlayerLeaderboard()}
      </LeaderboardContent>

      {loading ? <CenteredContainer><Loading /></CenteredContainer> : null}
      {error ? <CenteredContainer><ErrorMessage message={error} /></CenteredContainer> : null}
      <SplitterContainer>
        <SplitterLayout
          onSecondaryPaneSizeChange={onSecondaryPanelSizeChange}
          percentage
          primaryMinSize={20}
          secondaryMinSize={35}
          customClassName="game-splitter-container"
        >
          {/* Problem title/description panel */}
          <OverflowPanel className="display-box-shadow">
            <ProblemHeaderText>{problems[0]?.name}</ProblemHeaderText>
            {problems[0] ? (
              <DifficultyDisplayButton
                difficulty={problems[0].difficulty!}
                enabled={false}
                active
              >
                {displayNameFromDifficulty(problems[0].difficulty!)}
              </DifficultyDisplayButton>
            ) : null}
            <StyledMarkdownEditor
              defaultValue={problems[0]?.description}
              onChange={() => ''}
              readOnly
            />
            <BottomFooterText>
              {'Notice an issue? Contact us at '}
              <SmallInlineCopyText
                onClick={() => {
                  copy('support@codejoust.co');
                  setCopiedEmail(true);
                }}
              >
                support@codejoust.co
                <SmallInlineCopyIcon>content_copy</SmallInlineCopyIcon>
              </SmallInlineCopyText>
            </BottomFooterText>
          </OverflowPanel>

          {/* Code editor and console panels */}
          <SplitterLayout
            vertical
            percentage
            primaryMinSize={20}
            secondaryMinSize={0}
          >
            <NoPaddingPanel>
              <Editor
                onCodeChange={setCurrentCode}
                onLanguageChange={setCurrentLanguage}
                codeMap={defaultCodeList[0]}
                defaultLanguage={currentLanguage}
                defaultCode={null}
              />
            </NoPaddingPanel>

            <Panel>
              <Console
                testCases={problems[0]?.testCases}
                submission={submission}
                onRun={runCode}
                onSubmit={submitCode}
              />
            </Panel>
          </SplitterLayout>
        </SplitterLayout>
      </SplitterContainer>
      <BottomCopyIndicatorContainer copied={copiedEmail}>
        <CopyIndicator onClick={() => setCopiedEmail(false)}>
          Email copied!&nbsp;&nbsp;✕
        </CopyIndicator>
      </BottomCopyIndicatorContainer>
    </>
  );
}

export default PlayerGameView;