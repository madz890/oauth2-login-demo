import React, {useEffect, useState} from 'react';

export default function Profile() {
  const [user, setUser] = useState(null);
  const [csrf, setCsrf] = useState(null);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    fetch('/api/me').then(r => r.json()).then(setUser).catch(()=>{});
    fetch('/api/csrf').then(r => r.json()).then(setCsrf).catch(()=>{});
  }, []);

  if (user === null) return <div style={{padding:20}}>Loading...</div>;
  if (!user.authenticated) return <div style={{padding:20}}>Not logged in</div>;

  const submit = async (e) => {
    e.preventDefault();
    setSaving(true);
    const body = { displayName: user.displayName, bio: user.bio };
    const headers = {'Content-Type': 'application/json'};
    if (csrf && csrf.headerName) headers[csrf.headerName] = csrf.token;

    try {
      const res = await fetch('/profile', {
        method: 'POST',
        headers,
        body: new URLSearchParams({ displayName: user.displayName, bio: user.bio })
      });
      if (res.ok) {
        alert('Profile updated');
      } else {
        alert('Update failed');
      }
    } catch (err) {
      alert('Network error');
    }
    setSaving(false);
  };

  return (
    <div style={{padding:20}}>
      <h1>User Profile</h1>
      <form onSubmit={submit}>
        <p>Email: <span>{user.email}</span></p>
        <p>Display Name: <input type="text" value={user.displayName || ''} onChange={e => setUser({...user, displayName: e.target.value})} /></p>
        <p>Bio: <textarea value={user.bio || ''} onChange={e => setUser({...user, bio: e.target.value})}></textarea></p>
        <button type="submit" disabled={saving}>{saving ? 'Saving...' : 'Update'}</button>
      </form>
      <p><a href="/">Back to Home</a></p>
    </div>
  );
}
