import React, { useEffect, useState } from 'react';
import { useHistory } from 'react-router-dom';
import { useAppDispatch, useAppSelector } from '../../util/Hook';
import { LandingHeaderTitle, MainHeaderText } from '../../components/core/Text';
import { getAccount } from '../../api/Account';
import { setAccount } from '../../redux/Account';
import ErrorMessage from '../../components/core/Error';
import Loading from '../../components/core/Loading';
import ProblemCard from '../../components/card/ProblemCard';

function DashboardPage() {
  const dispatch = useAppDispatch();
  const history = useHistory();

  const { firebaseUser, token, account } = useAppSelector((state) => state.account);

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    if (!account && firebaseUser && token) {
      setLoading(true);
      getAccount(firebaseUser.uid, token)
        .then((res) => dispatch(setAccount(res)))
        .catch((err) => setError(err.message))
        .finally(() => setLoading(false));
    }
  }, [firebaseUser, account, token]);

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

      <MainHeaderText>My Problems</MainHeaderText>
      { error ? <ErrorMessage message={error} /> : null }
      { loading ? <Loading /> : null }

      {account?.problems.map((problem, index) => (
        <ProblemCard
          key={index}
          problem={problem}
          onClick={() => history.push(`/problem/${problem.problemId}`, { locked: false })}
        />
      ))}
    </div>
  );
}

export default DashboardPage;