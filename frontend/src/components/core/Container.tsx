import styled from 'styled-components';

export const FlexContainer = styled.div`
  display: flex;
  flex: auto;
  flex-direction: column;
  margin: 0 1rem;
`;

export const FlexBareContainer = styled.div`
  display: flex;
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
  padding: 0 1rem;
  box-sizing: border-box;
  border: 2px solid ${({ theme }) => theme.colors.border};
  border-radius: 10px;
  background-color: ${({ theme }) => theme.colors.white};
  overflow: hidden;
`;

export const SplitterContainer = styled.div`
  flex: auto;
  position: relative;
  margin: 5px 20px 25px 20px;
  min-height: 200px;
  
  .splitter-layout {
    overflow: visible;
  }
  
  /* References the layout pane class generated by the Monaco Editor. */
  .layout-pane {
    height: 100%;
    overflow: hidden;
    box-shadow: 0 -1px 8px rgba(0, 0, 0, 0.08);
    border-radius: 8px !important;
    
    div {
      border: none;
      border-radius: 0;
    }
  }
  
  /* Edge case to hide box shadow for right panel containing editor and console */
  .game-splitter-container {
    & > .layout-pane:nth-of-type(3) {
      box-shadow: none !important;
      overflow: visible;
    }
  }
  
  /* References the layout splitter class generated by the Splitter Layout. */
  .layout-splitter {
    background-color: transparent !important;
    min-width: 20px;
    min-height: 20px;
  }
`;

export const MainContainer = styled.div`
  margin: 0 auto;
  padding: 3rem 0;
  width: 80%;
`;

export const ProblemContainer = styled.div`
  margin: 0 auto;
  padding-bottom: 3rem;
  width: 80%;
`;

export const CenteredContainer = styled.div`
  text-align: center;
  justify-content: center;
`;

export const LandingPageContainer = styled.div`
  position: relative;
  margin: 0 auto;
  width: 66.666%;

  @media(max-width: 640px) {
    width: 100%;
  }
`;
