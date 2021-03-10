import styled from 'styled-components';

const Input = styled.input`
  box-sizing: border-box;
  border-radius: 0.25rem;
  border: 3px solid ${({ theme }) => theme.colors.blue};
  display: block;
  margin: 1rem auto;
  outline: none;
  font-weight: 700;
`;

export const LargeCenterInputText = styled(Input).attrs(() => ({
  type: 'text',
}))`
  width: 20rem;
  text-align: center;
  font-size: ${({ theme }) => theme.fontSize.xMediumLarge};
  padding: 1rem;
  color: ${({ theme }) => theme.colors.text};

  &:focus {
    border: 3px solid ${({ theme }) => theme.colors.darkBlue};
  }
`;

export const LargeInputButton = styled(Input).attrs(() => ({
  type: 'button',
}))`
  width: 20rem;
  font-size: ${({ theme }) => theme.fontSize.xMediumLarge};
  padding: 1rem;
  color: white;
  background-color: ${({ theme }) => theme.colors.blue};
  font-weight: 700;
  cursor: pointer;

  &:disabled {
    background-color: ${({ theme }) => theme.colors.lightBlue};
    border: 3px solid ${({ theme }) => theme.colors.lightBlue};
    cursor: default;
  }
`;

export const ConsoleTextArea = styled.textarea`
  font-family: Monaco, monospace;
  font-size: ${({ theme }) => theme.fontSize.default};
  color: ${({ theme }) => theme.colors.text};
  margin: 2px;
  min-width: 50px;
  max-width: 90%;
  width: 70%;
  min-height: 24px;
  max-height: 150px;

  padding: 5px;
  border: 2px solid ${({ theme }) => theme.colors.blue};
  border-radius: 0.3rem;
`;

export const NumberInput = styled(Input).attrs(() => ({
  type: 'number',
}))`
  display: inline-block;
  width: 7rem;
  text-align: center;
  font-size: ${({ theme }) => theme.fontSize.mediumLarge};
  padding: 1rem;
  color: ${({ theme }) => theme.colors.text};

  &:focus {
    border: 3px solid ${({ theme }) => theme.colors.darkBlue};
  }
`;

export const TextInput = styled(Input).attrs(() => ({
  type: 'text',
}))`
  display: block;
  width: 15rem;
  text-align: center;
  font-size: ${({ theme }) => theme.fontSize.mediumLarge};
  padding: 1rem;
  color: ${({ theme }) => theme.colors.text};

  &:focus {
    border: 3px solid ${({ theme }) => theme.colors.darkBlue};
  }
`;

export const CheckboxInput = styled(Input).attrs(() => ({
  type: 'checkbox',
}))`
  display: inline-block;
  font-size: ${({ theme }) => theme.fontSize.mediumLarge};
  color: ${({ theme }) => theme.colors.text};
  margin: 5px;
`;
