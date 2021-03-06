import styled from 'styled-components';
import React from 'react';
import { Difficulty, difficultyToColor, displayNameFromDifficulty } from '../../api/Difficulty';
import { ThemeType } from '../config/Theme';

type Dimensions = {
  width?: string,
  height?: string,
};

export const DefaultButton = styled.button`
  border: none;
  border-radius: 0.25rem;
  margin: 1.2rem;
  font-size: ${({ theme }) => theme.fontSize.default};
  font-family: ${({ theme }) => theme.font};
  box-shadow: 0 1px 8px rgba(0, 0, 0, 0.24);
  
  &:hover {
    outline: none;
    cursor: pointer;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.24);
  }
  
  &:focus {
    outline: none;
  }
`;

export const PrimaryButton = styled(DefaultButton)<Dimensions>`
  font-size: ${({ theme }) => theme.fontSize.mediumLarge};
  background: ${({ theme }) => theme.colors.gradients.blue};
  color: ${({ theme }) => theme.colors.white};
  width: ${({ width }) => width || '12rem'};
  height: ${({ height }) => height || '3rem'};
  min-width: 150px;
  min-height: 40px;

  &:disabled {
    opacity: 0.5;
    background: ${({ theme }) => theme.colors.gradients.gray};

    &:hover {
      cursor: default;
      box-shadow: 0 1px 8px rgba(0, 0, 0, 0.24);
    }
  }
`;

export const SecondaryRedButton = styled(PrimaryButton)`
  background: ${({ theme }) => theme.colors.white};
  color: ${({ theme }) => theme.colors.red2};
  margin: 1.2rem 0;
`;

export const DangerButton = styled(PrimaryButton)`
  display: inline-block;
  background: ${({ theme }) => theme.colors.white};
  color: ${({ theme }) => theme.colors.red2};
  font-size: ${({ theme }) => theme.fontSize.default};
  width: 120px;
  min-width: 120px;
  height: 35px;
  min-height: 35px;
  margin: 2px 10px;
`;

export const TextButton = styled.button<ThemeType>`
  background: none;
  border: none;
  color: ${({ theme }) => theme.colors.text};
  font-size: ${({ theme }) => theme.fontSize.default};
  font-family: ${({ theme }) => theme.font};
  cursor: pointer;
  
  &:focus {
    outline: none;
  }
`;

export const InheritedTextButton = styled(TextButton)`
  font-size: inherit;
  border-bottom: 1px solid ${({ theme }) => theme.colors.text};
  padding: 0;
`;

export const RedTextButton = styled(TextButton)`
  color: ${({ theme }) => theme.colors.red2};
`;

export const GrayTextButton = styled(TextButton)`
  color: ${({ theme }) => theme.colors.gray};
`;

type DifficultyProps = {
  difficulty: Difficulty,
  active: boolean,
  enabled: boolean,
}

export const SmallDifficultyButton = styled(DefaultButton)<DifficultyProps>`
  font-size: ${({ theme }) => theme.fontSize.default};
  background: ${({ active, difficulty, theme }) => (active ? difficultyToColor[difficulty].background : theme.colors.white)};
  color: ${({ active, difficulty, theme }) => (active ? theme.colors.white : difficultyToColor[difficulty].color)};
  width: 5rem;
  height: 2rem;
  border-radius: 20px;
  margin: 0.5rem 1rem 0 0;

  &:disabled {
    opacity: 0.5;

    &:hover {
      cursor: default;
      opacity: 0.5;
      box-shadow: 0 1px 8px rgba(0, 0, 0, 0.24);
    }
  }
  
  &:hover {
    ${({
    difficulty,
    theme,
    enabled,
  }) => enabled && `
      color: ${theme.colors.white};
      background: ${difficultyToColor[difficulty].background};
    `};
    
    cursor: ${({ enabled }) => (enabled ? 'pointer' : 'default')};
  }
`;

export const DifficultyDisplayButton = styled(SmallDifficultyButton)`
  font-size: ${({ theme }) => theme.fontSize.medium};
  width: unset;
  padding: 2px 15px;
  height: 1.75rem;
  margin: 0;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.24);
  
  &:hover {
    box-shadow: 0 1px 6px rgba(0, 0, 0, 0.24);
  }
`;

export const getDifficultyDisplayButton = (difficulty: Difficulty, enabled = false) => (
  <DifficultyDisplayButton
    difficulty={difficulty}
    enabled={enabled}
    active
  >
    {displayNameFromDifficulty(difficulty)}
  </DifficultyDisplayButton>
);

export const InlineDifficultyDisplayButton = styled(DifficultyDisplayButton)`
  padding: 1px 10px;
  height: 1.6rem;
`;

export const SmallDifficultyButtonNoMargin = styled(SmallDifficultyButton)`
  margin: 0;
`;

export const DifficultyButton = styled(DefaultButton)<DifficultyProps>`
  font-size: ${({ theme }) => theme.fontSize.mediumLarge};
  background: ${({ active, difficulty, theme }) => (active ? difficultyToColor[difficulty].background : theme.colors.white)};
  color: ${({ active, difficulty, theme }) => (active ? theme.colors.white : difficultyToColor[difficulty].color)};
  width: 6rem;
  height: 2rem;
  min-width: 90px;
  min-height: 40px;
  border-radius: 20px;

  &:disabled {
    background-color: ${({ theme }) => theme.colors.gray};

    &:hover {
      cursor: default;
      box-shadow: 0 1px 8px rgba(0, 0, 0, 0.24);
    }
  }
  
  &:hover {
    ${({
    difficulty,
    theme,
    enabled,
  }) => enabled && `
      color: ${theme.colors.white};
      background: ${difficultyToColor[difficulty].background};
    `};
    
    cursor: ${({ enabled }) => (enabled ? 'pointer' : 'default')};
  }
`;

export const SmallButton = styled(DefaultButton)`
  color: ${({ theme }) => theme.colors.white};
  background: ${({ theme }) => theme.colors.gradients.blue};
  font-size: ${({ theme }) => theme.fontSize.medium};
  padding: 0;
  margin: 0.4rem;
  width: 6rem;
  height: 2.25rem;
  line-height: 2rem;
`;

export const GreenSmallButton = styled(SmallButton)`
  background: ${({ theme }) => theme.colors.gradients.green};
`;

export const GreenSmallButtonBlock = styled(SmallButton)`
  display: block;
  background: ${({ theme }) => theme.colors.gradients.green};
`;

export const InvertedSmallButton = styled(SmallButton)`
  color: ${({ theme }) => theme.colors.text};
  background: ${({ theme }) => theme.colors.white};
`;

type ProblemNavButtonProps = {
  disabled: boolean,
};

export const ProblemNavButton = styled(DefaultButton)<ProblemNavButtonProps>`
  font-size: ${({ theme }) => theme.fontSize.default};
  color: ${({ theme, disabled }) => (disabled ? theme.colors.lightgray : theme.colors.gray)};
  background-color: ${({ theme }) => theme.colors.white};
  border-radius: 5px;
  width: 35px;
  height: 35px;
  margin: 5px;
  
  box-shadow: 0 1px 6px rgba(0, 0, 0, 0.16);
  
  &:hover {
    box-shadow: ${({ disabled }) => (disabled ? '0 1px 6px rgba(0, 0, 0, 0.16)' : '0 1px 6px rgba(0, 0, 0, 0.20)')};
    cursor: ${({ disabled }) => (disabled ? 'default' : 'pointer')}; 
  }
  
  i {
    line-height: 35px;
  }
`;
