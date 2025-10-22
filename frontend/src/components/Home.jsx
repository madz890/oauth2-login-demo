import React from 'react';

export default function Home() {
  return (
    <div style={{padding:20}}>
      <h1>Welcome</h1>
      <p>
        <a href={`${process.env.REACT_APP_API_URL}/oauth2/authorization/google`}>
  <button>Login with Google</button>
</a>
      </p>
      <p>
        <a href={`${process.env.REACT_APP_API_URL}/oauth2/authorization/github`}>
  <button>Login with GitHub</button>
</a>
      </p>
    </div>
  );
}
