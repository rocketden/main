import React, { useState } from 'react';
import styled from 'styled-components';
import { ProblemTag, SelectableProblem } from '../../api/Problem';
import { InlineDifficultyDisplayButton } from '../core/Button';
import { displayNameFromDifficulty } from '../../api/Difficulty';
import { TextInput } from '../core/Input';

type SelectedProblemsDisplayProps = {
  problems: SelectableProblem[],
  onRemove: ((index: number) => void) | null,
}

type SelectedTagsDisplayProps = {
  tags: ProblemTag[],
  onRemove: ((index: number) => void) | null,
}

const Content = styled.div`
  margin: 5px 0;
`;

const ProblemDisplay = styled.div`
  display: inline-block;
  padding: 5px;
  margin: 5px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.24);
  background-color: ${({ theme }) => theme.colors.white};
  border-radius: 5px;
`;

const ProblemName = styled.p`
  font-weight: bold;
  display: inline;
  margin: 0 10px;
`;

const RemoveText = styled.p`
  display: inline;
  padding: 5px;
  margin: 0 5px;
  color: ${({ theme }) => theme.colors.gray};
  line-height: 100%;

  &:hover {
    cursor: pointer;
  }
`;

const TextSearch = styled(TextInput)`
  display: block;
  width: 40%;
  margin: 5px 0;
`;

export function SelectedProblemsDisplay(props: SelectedProblemsDisplayProps) {
  const { problems, onRemove } = props;

  return (
    <Content>
      {problems.map((problem, index) => (
        <ProblemDisplay key={problem.problemId}>
          <ProblemName>
            {problem.name}
          </ProblemName>
          <InlineDifficultyDisplayButton
            difficulty={problem.difficulty}
            enabled={false}
            active
          >
            {displayNameFromDifficulty(problem.difficulty)}
          </InlineDifficultyDisplayButton>
          {onRemove ? <RemoveText onClick={() => onRemove(index)}>✕</RemoveText> : null}
        </ProblemDisplay>
      ))}

      { !problems.length ? <p>Selected problems will show here.</p> : null }
    </Content>
  );
}

export function SelectedTagsDisplay(props: SelectedTagsDisplayProps) {
  const { tags, onRemove } = props;

  return (
    <Content>
      {tags.map((tag, index) => (
        <ProblemDisplay key={tag.tagId}>
          <ProblemName>
            {tag.name}
          </ProblemName>
          {onRemove ? <RemoveText onClick={() => onRemove(index)}>✕</RemoveText> : null}
        </ProblemDisplay>
      ))}

      { !tags.length ? <p>Selected tags will show here.</p> : null }
    </Content>
  );
}

type TagProps = {
  tags: ProblemTag[],
};

export function FilterAllTagsDisplay(props: TagProps) {
  const { tags } = props;

  const [searchText, setSearchText] = useState('');

  const setSearchStatus = (e: React.ChangeEvent<HTMLInputElement>) => {
    setSearchText(e.target.value);
  };

  return (
    <Content>
      <TextSearch
        onChange={setSearchStatus}
        placeholder={tags.length ? 'Filter tags' : 'Loading...'}
      />

      {tags.map((tag) => {
        if (searchText && !tag.name.toLowerCase().includes(searchText.toLowerCase())) {
          return null;
        }

        return (
          <ProblemDisplay key={tag.tagId}>
            <ProblemName>
              {tag.name}
            </ProblemName>
          </ProblemDisplay>
        );
      })}
    </Content>
  );
}