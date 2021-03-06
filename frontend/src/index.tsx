import React from 'react';
import ReactDOM from 'react-dom';
import { Provider } from 'react-redux';
import { BrowserRouter } from 'react-router-dom';
import { createGlobalStyle } from 'styled-components';
import 'typeface-roboto';
import App from './components/config/App';
import * as serviceWorker from './serviceWorker';
import Theme, { ThemeType } from './components/config/Theme';
import store from './redux/Store';

const GlobalStyle = createGlobalStyle<{ theme: ThemeType }>`
  html {
    height: 100vh;
    font-family: ${({ theme }) => theme.font};
    font-weight: 400;
    font-size: ${({ theme }) => theme.fontSize.globalDefault};
    color: ${({ theme }) => theme.colors.text};
    
    @media (max-width: 800px) {
      font-size: ${({ theme }) => theme.fontSize.globalSmall};
    }
  }
  
  body {
    margin: 0;
    height: 100%;
  }
  
  #root {
    height: 100%
  }
`;

ReactDOM.render(
  <React.StrictMode>
    <Theme>
      <GlobalStyle />
      <BrowserRouter>
        <Provider store={store}>
          <App />
        </Provider>
      </BrowserRouter>
    </Theme>
  </React.StrictMode>,
  document.getElementById('root'),
);

// If you want your app to work offline and load faster, you can change
// unregister() to register() below. Note this comes with some pitfalls.
// Learn more about service workers: https://bit.ly/CRA-PWA
serviceWorker.unregister();
