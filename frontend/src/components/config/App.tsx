import React, { useEffect, useState } from 'react';
import ReactGA from 'react-ga';
import { Switch, useLocation } from 'react-router-dom';
import MainLayout from '../layout/Main';
import LandingPage from '../../views/Landing';
import NotFound from '../../views/NotFound';
import { CustomRoute, CustomRedirect, PrivateRoute } from './Route';
import GamePage from '../../views/Game';
import GameLayout from '../layout/Game';
import JoinGamePage from '../../views/Join';
import CreateGamePage from '../../views/Create';
import GameResultsPage from '../../views/Results';
import LobbyPage from '../../views/Lobby';
import AllProblemsPage from '../../views/AllProblemsPage';
import ProblemPage from '../../views/ProblemPage';
import CreateProblemPage from '../../views/CreateProblemPage';
import CircleBackgroundLayout from '../layout/CircleBackground';
import DashboardPage from '../../views/account/Dashboard';
import LoginPage from '../../views/account/Login';
import RegisterPage from '../../views/account/Register';
import ContactUsPage from '../../views/ContactUs';
import MinimalLayout from '../layout/MinimalLayout';
import { useAppDispatch } from '../../util/Hook';
import app from '../../api/Firebase';
import { setAccount, UserType, setToken } from '../../redux/Account';
import { CenteredContainer } from '../core/Container';
import Loading from '../core/Loading';

// Set up Google Analytics
ReactGA.initialize('UA-192641172-2');

function App() {
  const location = useLocation();
  const dispatch = useAppDispatch();
  const [loading, setLoading] = useState(true);

  // Track page view on every change in location and clear errors when switching pages
  useEffect(() => {
    ReactGA.pageview(location.pathname);
  }, [location]);

  // Set authentication status when Firebase auth status changes
  useEffect(() => {
    app.auth().onAuthStateChanged((account) => {
      dispatch(setAccount(account?.toJSON() as UserType || null));
      setLoading(false);

      // Save Token in Redux state if authenticated
      if (account) {
        account.getIdToken(true)
          .then((token) => dispatch(setToken(token)));
      }
    });
  }, [dispatch, setLoading]);

  // While the initial Firebase auth is still loading, show blank loading screen
  if (loading) {
    return (
      <CenteredContainer>
        <Loading />
      </CenteredContainer>
    );
  }

  return (
    <Switch>
      <CustomRoute path="/" component={LandingPage} layout={CircleBackgroundLayout} exact />
      <CustomRoute path="/game" component={GamePage} layout={GameLayout} exact />
      <CustomRoute path="/game/join" component={JoinGamePage} layout={CircleBackgroundLayout} exact />
      <CustomRoute path="/game/create" component={CreateGamePage} layout={CircleBackgroundLayout} exact />
      <CustomRoute path="/game/lobby" component={LobbyPage} layout={MinimalLayout} exact />
      <CustomRoute path="/game/results" component={GameResultsPage} layout={MinimalLayout} exact />
      <PrivateRoute path="/problems/all" component={AllProblemsPage} layout={MinimalLayout} exact />
      <PrivateRoute path="/problem/create" component={CreateProblemPage} layout={MinimalLayout} exact />
      <PrivateRoute path="/problem/:id" component={ProblemPage} layout={MinimalLayout} exact />
      <PrivateRoute path="/dashboard" component={DashboardPage} layout={MinimalLayout} exact />
      <CustomRoute path="/login" component={LoginPage} layout={MainLayout} exact />
      <CustomRoute path="/register" component={RegisterPage} layout={MainLayout} exact />
      <CustomRoute path="/contact-us" component={ContactUsPage} layout={MainLayout} exact />
      <CustomRedirect from="/play" to="/game/join" />
      <CustomRoute path="*" component={NotFound} layout={MainLayout} />
    </Switch>
  );
}

export default App;
