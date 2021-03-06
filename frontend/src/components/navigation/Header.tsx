import React, { useState } from 'react';
import { useHistory } from 'react-router-dom';
import styled from 'styled-components';
import { NavbarLink } from '../core/Link';
import app from '../../api/Firebase';
import { TextButton } from '../core/Button';
import { useAppDispatch, useAppSelector } from '../../util/Hook';
import { setAccount, setToken } from '../../redux/Account';
import Dropdown from '../core/Dropdown';

const Content = styled.div`
  position: relative;
  height: 50px;
  padding: 20px;
  text-align: center;
  z-index: 2;
`;

const MinimalContent = styled.div`
  height: 20px;
  padding: 20px 20px 0 20px;
  text-align: center;
  z-index: 2;
`;

const LeftHeader = styled(NavbarLink)`
  float: left;
  margin-left: 50px;
  
  @media(max-width: 600px) {
    margin-left: 0;
  }
`;

const RightHeader = styled(NavbarLink)`
  margin: 0 15px;
  
  @media(max-width: 600px) {
    margin: 0 8px;
  }
`;

const NavButton = styled(TextButton)`
  font-size: ${({ theme }) => theme.fontSize.mediumLarge};
  margin: 0 15px;
  
  @media(max-width: 600px) {
    margin: 0 8px;
  }
`;

const RightContainer = styled.div`
  float: right;
  margin-right: 50px;
  z-index: 1;
  
  @media(max-width: 600px) {
    margin-right: 0;
  }
`;

const DropdownContainer = styled.div`
  position: relative;
  display: inline;
  padding: 10px 0;
`;

const InlineHeaderTag = styled.span`
  position: relative;
  top: -0.1rem;
  margin-left: 0.4rem;
  padding: 0 0.5rem;
  font-size: ${({ theme }) => theme.fontSize.medium};
  background: ${({ theme }) => theme.colors.gradients.purple};
  border-radius: 1rem;
  color: ${({ theme }) => theme.colors.white};
`;

const LogoIcon = styled.img`
  vertical-align: bottom;
  width: 27px;
  margin-right: 5px;
`;

// Note: Can also create a center header with simply display: inline-block

function LoggedInContent() {
  const dispatch = useAppDispatch();
  const history = useHistory();
  const [mouseOver, setMouseOver] = useState(false);

  const logOut = () => {
    dispatch(setAccount(null));
    dispatch(setToken(null));
    app.auth().signOut();
    history.push('/');
  };

  const loggedInAccountItems = [
    { title: 'Dashboard', link: '/', active: window.location.pathname === '/' },
    { title: 'Profile', link: '/profile', active: window.location.pathname === '/profile' },
    { title: 'Logout', action: logOut, active: false },
  ];

  return (
    <RightContainer>
      <DropdownContainer
        onMouseEnter={() => setMouseOver(true)}
        onMouseLeave={() => setMouseOver(false)}
      >
        <NavButton>
          My Account
        </NavButton>
        { mouseOver ? <Dropdown items={loggedInAccountItems} /> : null}
      </DropdownContainer>
      <RightHeader to="/contact-us">
        Contact Us
      </RightHeader>
    </RightContainer>
  );
}

function LoggedOutContent() {
  return (
    <RightContainer>
      <RightHeader to="/register">
        Register
      </RightHeader>
      <RightHeader to="/login">
        Login
      </RightHeader>
      <RightHeader to="/contact-us">
        Contact Us
      </RightHeader>
    </RightContainer>
  );
}

function HeaderContent() {
  const { firebaseUser } = useAppSelector((state) => state.account);

  return (
    <nav>
      <LeftHeader to="/">
        <LogoIcon src="/logo512.png" alt="Logo Icon" />
        CodeJoust
        <InlineHeaderTag>Beta</InlineHeaderTag>
      </LeftHeader>
      {firebaseUser ? <LoggedInContent /> : <LoggedOutContent />}
    </nav>
  );
}

export function Header() {
  return (
    <Content>
      <HeaderContent />
    </Content>
  );
}

export function MinimalHeader() {
  return (
    <MinimalContent>
      <HeaderContent />
    </MinimalContent>
  );
}
