import React from 'react';
import styled from 'styled-components';

const ToggleButtonLabel = styled.label`
  position: relative;
  display: inline-block;
  width: 55px;
  height: 20px;
  user-select: none;
  opacity: 0.7;
`;

type CheckboxProps = {
  checked: boolean,
  editable: boolean,
};

const ToggleButtonInput = styled.input.attrs((props: CheckboxProps) => ({
  type: 'checkbox',
  checked: props.checked,
}))<CheckboxProps>``;

const ToggleButtonSpan = styled.span<CheckboxProps>`
  background-color: ${({ theme, checked }) => (checked ? theme.colors.sliderBlue : theme.colors.sliderGray)};
  position: absolute;
  cursor: ${({ editable }) => (editable ? 'pointer' : 'default')};;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  -webkit-transition: .4s;
  transition: .4s;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.24);
  border-radius: 34px;

  &:before {
    position: absolute;
    content: '';
    height: 30px;
    width: 30px;
    left: 0px;
    bottom: -5px;
    background: ${({ theme }) => theme.colors.gradients.blue};
    -webkit-transition: .4s;
    transition: .4s;
    box-shadow: 0 1px 4px rgba(0, 0, 0, 0.24);
    border-radius: 50%;
    -webkit-transform: ${(props) => (props.checked ? 'translateX(26px)' : 'translateX(0px)')};
    -ms-transform: ${(props) => (props.checked ? 'translateX(26px)' : 'translateX(0px)')};
    transform: ${(props) => (props.checked ? 'translateX(26px)' : 'translateX(0px)')};
  }
`;

type ToggleButtonParams = {
  onChangeFunction: () => void,
  checked: boolean,
  editable: boolean,
};

const ToggleButton = (params: ToggleButtonParams) => (
  <ToggleButtonLabel>
    <ToggleButtonInput
      onChange={() => (params.editable ? params.onChangeFunction() : '')}
      checked={params.checked}
      editable={params.editable}
    />
    <ToggleButtonSpan
      checked={params.checked}
      editable={params.editable}
    />
  </ToggleButtonLabel>
);

export default ToggleButton;
