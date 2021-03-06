import React from 'react';
import { Link } from 'react-router-dom';
import styled from 'styled-components';
import { GreenSmallButton, PrimaryButton } from './Button';

// Wrap a button inside of a Link to get the styling of a button
const createButtonLink = (Button:any, props:any) => {
  const {
    to,
    children,
    ...rest
  } = props;
  const button = (
    <Button {...rest}>
      {children}
    </Button>
  );

  return <Link to={to}>{button}</Link>;
};

export const PrimaryButtonLink = (props:any) => createButtonLink(PrimaryButton, props);
export const GreenSmallButtonLink = (props:any) => createButtonLink(GreenSmallButton, props);

export const NavbarLink = styled(Link)`
  color: ${({ theme }) => theme.colors.text};
  font-size: ${({ theme }) => theme.fontSize.mediumLarge};
  text-decoration: none;
`;

export const TextLink = styled(Link)`
  font-size: ${({ theme }) => theme.fontSize.default};
  color: ${({ theme }) => theme.colors.gray};
  text-decoration: none;
`;

export const InheritedTextLink = styled(Link)`
  font-size: inherit;
  color: inherit;
  text-decoration: underline;
`;

export const SecondaryHeaderTextLink = styled(TextLink)`
  font-size: ${({ theme }) => theme.fontSize.mediumLarge};
  color: ${({ theme }) => theme.colors.blueLink};
`;

export const InlineExternalLink = styled.a`
  color: ${({ theme }) => theme.colors.blueLink};
`;

export const DivLink = styled(Link)`
  display: block;
  color: inherit;
  text-decoration: inherit;
`;
