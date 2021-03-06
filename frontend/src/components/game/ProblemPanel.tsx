import React from 'react';
import styled from 'styled-components';
import MarkdownEditor from 'rich-markdown-editor';
import { BottomFooterText, ProblemHeaderText, SmallText } from '../core/Text';
import { getDifficultyDisplayButton, ProblemNavButton } from '../core/Button';
import { Copyable } from '../special/CopyIndicator';
import { CenteredContainer, Panel } from '../core/Container';
import { Problem } from '../../api/Problem';
import { NextIcon, PrevIcon } from '../core/Icon';

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

const HeaderContainer = styled.div`
  display: flex;
  flex: auto;
  justify-content: space-between;
`;

const TitleContainer = styled.div`
  display: -webkit-box;
  -webkit-line-clamp: 1;
  overflow: hidden;
  -webkit-box-orient: vertical;
  word-break: break-all;
`;

const ProblemNavContainer = styled.div`
  width: 100px;
  min-width: 100px;
  align-items: baseline;
  padding: 15px 0;
`;

const ProblemCountText = styled(SmallText)`
  color: ${({ theme }) => theme.colors.gray};
`;

type ProblemPanelProps = {
  problems: Problem[],
  index: number,
  onNext: (() => void) | null,
  onPrev: (() => void) | null,
};

function ProblemPanel(props: ProblemPanelProps) {
  const {
    problems, index, onNext, onPrev,
  } = props;

  return (
    <OverflowPanel>
      <HeaderContainer>
        <div>
          <TitleContainer>
            <ProblemHeaderText>{problems[index]?.name || 'Loading...'}</ProblemHeaderText>
          </TitleContainer>
          {problems[index] ? getDifficultyDisplayButton(problems[index].difficulty) : null}
        </div>
        <ProblemNavContainer>
          <CenteredContainer>
            <div>
              <ProblemNavButton onClick={onPrev || undefined} disabled={!onPrev}>
                <PrevIcon />
              </ProblemNavButton>
              <ProblemNavButton onClick={onNext || undefined} disabled={!onNext}>
                <NextIcon />
              </ProblemNavButton>
            </div>
            <ProblemCountText>
              {`Problem ${index + 1} of ${problems.length}`}
            </ProblemCountText>
          </CenteredContainer>
        </ProblemNavContainer>
      </HeaderContainer>

      <StyledMarkdownEditor
        defaultValue={problems[index]?.description || ''}
        value={problems[index]?.description || ''}
        onChange={() => ''}
        readOnly
      />
      <BottomFooterText>
        Notice an issue? Contact us at
        {' '}
        <Copyable text="support@codejoust.co" top={false} />
      </BottomFooterText>
    </OverflowPanel>
  );
}

export default ProblemPanel;
