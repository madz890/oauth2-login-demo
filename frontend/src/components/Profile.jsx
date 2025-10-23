import React, { useEffect, useState } from 'react';
import axios from 'axios';

const API_URL = process.env.REACT_APP_API_URL || 'http://localhost:8080';

// This tells axios to send cookies (like the login session) with every request
axios.defaults.withCredentials = true;

// --- 1. ADD THIS HELPER FUNCTION ---
// This function manually reads a cookie from the browser
function getCookie(name) {
    const value = `; ${document.cookie}`;
    const parts = value.split(`; ${name}=`);
    if (parts.length === 2) {
        return parts.pop().split(';').shift();
    }
}
// --- END OF HELPER FUNCTION ---


export default function Profile() {
    const [user, setUser] = useState(null);
    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState(false);
    
    // We no longer need the csrfTokenLoaded state, 
    // as we will just read the cookie directly when we submit.

    useEffect(() => {
        const fetchData = async () => {
            try {
                // We still need to hit /api/csrf to *get* the cookie
                await axios.get(`${API_URL}/api/csrf`);

                // Then fetch user data
                const response = await axios.get(`${API_URL}/api/me`);
                setUser(response.data);
            } catch (error) {
                console.error("Could not fetch data", error);
                setUser({ authenticated: false });
            } finally {
                setLoading(false);
            }
        };

        fetchData();
    }, []);

    if (loading) return <div style={{ padding: 20 }}>Loading...</div>;
    if (!user || !user.authenticated) return <div style={{ padding: 20 }}>Not logged in. <a href="/">Go Home</a></div>;

    // --- 2. MODIFY THE handleSubmit FUNCTION ---
    const handleSubmit = async (e) => {
        e.preventDefault();

        // Manually get the token value from the cookie
        const csrfToken = getCookie('XSRF-TOKEN');

        if (!csrfToken) {
            alert("Could not find CSRF token. Please refresh the page and try again.");
            return;
        }

        setSaving(true);
        try {
            const payload = {
                displayName: user.displayName,
                bio: user.bio
            };

            // --- Manually set the header! ---
            const config = {
                headers: {
                    'X-XSRF-TOKEN': csrfToken
                }
            };

            // Send the request with the payload AND the config
            const response = await axios.post(`${API_URL}/api/profile`, payload, config);

            if (response.status === 200) {
                alert('Profile updated successfully!');
            } else {
                alert(`Update failed with status: ${response.status}`);
            }
        } catch (error) {
            console.error("Update failed:", error);
            if (error.response && error.response.status === 403) {
                alert('Update failed: CSRF token validation failed. Please refresh the page and try again.');
            } else {
                alert('Failed to update profile. Check console for details.');
            }
        } finally {
            setSaving(false);
        }
    };

    // --- 3. MODIFY handleLogout AS WELL ---
    const handleLogout = async () => {
        const csrfToken = getCookie('XSRF-TOKEN');

        if (!csrfToken) {
            alert("Could not find CSRF token. Please refresh the page and try again.");
            return;
        }

        try {
            // --- Manually set the header! ---
            const config = {
                headers: {
                    'X-XSRF-TOKEN': csrfToken
                }
            };
            
            await axios.post(`${API_URL}/logout`, {}, config); // Send empty body and config
            setUser(null);
            window.location.href = '/';
        } catch (error) {
            console.error("Logout failed:", error);
            alert('Logout failed. Please try again.');
        }
    };

    // --- 4. UPDATE THE RETURN (remove disabled checks) ---
    return (
        <div style={{ padding: 20 }}>
            <h1>User Profile</h1>
            <form onSubmit={handleSubmit}>
                <p>Email: <span>{user.email}</span></p>
                <p>
                    Display Name:
                    <input
                        type="text"
                        value={user.displayName || ''}
                        onChange={e => setUser({ ...user, displayName: e.target.value })}
                    />
                </p>
                <p>
                    Bio:
                    <textarea
                        value={user.bio || ''}
                        onChange={e => setUser({ ...user, bio: e.target.value })}
                    ></textarea>
                </p>
                {/* We can remove the 'disabled' logic related to the token */}
                <button type="submit" disabled={saving}> 
                    {saving ? 'Saving...' : 'Update'}
                </button>
            </form>
            <hr style={{ margin: '20px 0' }} />
            <button onClick={handleLogout}>Logout</button>
            <p style={{ marginTop: '20px' }}><a href="/">Back to Home</a></p>
        </div>
    );
}