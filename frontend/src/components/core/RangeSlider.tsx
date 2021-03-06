import styled from 'styled-components';

export const SliderContainer = styled.div`
  width: 100%;
  margin: 0;
`;

type SliderRange = {
  value: number,
};

export const Slider = styled.input.attrs((props: SliderRange) => ({
  type: 'range',
  className: 'slider',
  value: props.value,
}))<SliderRange>`
  -webkit-appearance: none;
  max-width: 370px;
  width: 100%;
  height: 15px;
  border-radius: 5px;
  background: ${({ theme }) => theme.colors.sliderBlue};
  outline: none;
  opacity: 0.7;
  -webkit-transition: .2s;
  transition: opacity .2s;

  &:hover {
    opacity: 1;
  }

  &::-webkit-slider-thumb {
    -webkit-appearance: none;
    appearance: none;
    width: 25px;
    height: 25px;
    border-radius: 10px;
    background: ${({ theme }) => theme.colors.gradients.blue};
    cursor: pointer;
  }

  &::-moz-range-thumb {
    width: 25px;
    height: 25px;
    border-radius: 10px;
    background: ${({ theme }) => theme.colors.gradients.blue};
    cursor: pointer;
  }

  &:disabled {
    opacity: 0.5;

    &::-webkit-slider-thumb {
      cursor: default;
    }

    &::-moz-range-thumb {
      cursor: default;
    }

    &:hover {
      opacity: 0.5;
    }
  }
`;
