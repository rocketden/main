import styled from 'styled-components';

export const Text = styled.p`
  font-size: ${({ theme }) => theme.fontSize.default};
`;

export const ErrorText = styled(Text)`
   color: ${({ theme }) => theme.colors.red};
`;

export const LargeText = styled.h3`
  font-size: ${({ theme }) => theme.fontSize.xLarge};
`;

export const LandingHeaderText = styled.h1`
  font-size: ${({ theme }) => theme.fontSize.xxLarge};
`;

export const UserNicknameText = styled(LargeText)`
  margin: 0;
`;

export const ProblemHeaderText = styled.h3`
  font-size: ${({ theme }) => theme.fontSize.default};
`;

export const SmallActionText = styled.p`
  font-size: ${({ theme }) => theme.fontSize.mediumSmall};
  display: inline;
  margin: 4px 12px;

  &:hover {
    cursor: pointer;
  }
`;
