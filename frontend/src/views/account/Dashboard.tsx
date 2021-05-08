import React from 'react';
import { useAppSelector } from '../../util/Hook';
import { LandingHeaderTitle } from '../../components/core/Text';

function DashboardPage() {
  const { firebaseUser } = useAppSelector((state) => state.account);

  return (
    <div>
      <LandingHeaderTitle>
        Dashboard
      </LandingHeaderTitle>

      <div>
        <p>
          Email:
          {' '}
          {firebaseUser?.email}
        </p>
      </div>
    </div>
  );
}

export default DashboardPage;
