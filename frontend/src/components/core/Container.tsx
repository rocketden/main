import styled from 'styled-components';

export const FlexContainer = styled.div`
  display: flex;
  flex: auto;
  flex-direction: column;
  margin: 1rem;
`;

export const FlexHorizontalContainer = styled.div`
  display: flex;
  flex: auto;
  margin: 1rem;
`;

export const FlexInfoBar = styled.div`
  padding: 0.5rem;
  text-align: center;
  display: flex;
`;

export const FlexLeft = styled.div`
  flex: 1;
  display: flex;
  justify-content: flex-start;
  align-items: center;
`;

export const FlexCenter = styled.div`
  flex: 1;
  display: flex;
  justify-content: center;
  align-items: center;
`;

export const FlexRight = styled.div`
  flex: 1;
  display: flex;
  justify-content: flex-end;
  align-items: center;
`;

export const Panel = styled.div`
  height: 100%;
  padding: 1rem;
  box-sizing: border-box;
  border: 2px solid ${({ theme }) => theme.colors.border};
  border-radius: 10px;
  background-color: white;
`;

export const SplitterContainer = styled.div`
  flex: auto;
  position: relative;
  
  .layout-pane {
    height: 100%;
  }
  
  .layout-splitter {
    background-color: transparent;
    
    &:hover {
      background-color: transparent;
    }
  }
`;

export const MainContainer = styled.div`
  margin: 0 auto;
  padding: 10vw 0;
  width: 80%;
`;

export const CenteredContainer = styled.div`
  text-align: center;
  justify-content: center;
`;
