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
  
`;

export const ProblemHeaderText = styled.h3`
  font-size: ${({ theme }) => theme.fontSize.default};
`;

export const SmallActionText = styled.p`
  font-size: ${({ theme }) => theme.fontSize.small};
  text-decoration: underline;
  margin: 3px 5px;

  &:hover {
    cursor: pointer;
  }
`;
