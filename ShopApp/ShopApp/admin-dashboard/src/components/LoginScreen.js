// src/components/LoginScreen.js
import React, { useState } from 'react';
import { auth } from '../firebaseConfig'; 
import { signInWithEmailAndPassword } from 'firebase/auth';

const LoginScreen = ({ setIsLoggedIn }) => {
    const [email, setEmail] = useState('test@admin.com'); 
    const [password, setPassword] = useState('123456'); 
    const [error, setError] = useState('');
    const [loading, setLoading] = useState(false);

    const handleLogin = async (e) => {
        e.preventDefault();
        setError('');
        setLoading(true);
        try {
            await signInWithEmailAndPassword(auth, email, password);
            setIsLoggedIn(true); 
            alert('Đăng nhập thành công! Bây giờ bạn có token để Upload.');
        } catch (err) {
            console.error("Login Error:", err);
            setError('Đăng nhập thất bại. Kiểm tra email/password hoặc kết nối mạng.');
        } finally {
            setLoading(false);
        }
    };

    // --- DARK/MINIMALIST STYLES ---
    const loginStyles = {
        container: { 
            maxWidth: '400px', 
            margin: '100px auto', 
            padding: '40px', 
            backgroundColor: '#292929', 
            borderRadius: '10px', 
            boxShadow: '0 8px 16px rgba(0,0,0,0.5)',
            color: '#E0E0E0' 
        },
        h2: {
            textAlign: 'center',
            marginBottom: '30px',
            color: '#C40000', 
            fontSize: '24px',
            fontWeight: 'bold'
        },
        input: { 
            padding: '12px', 
            margin: '10px 0', 
            width: '100%', 
            border: '1px solid #444', 
            backgroundColor: '#333', 
            color: '#E0E0E0', 
            borderRadius: '6px',
            boxSizing: 'border-box'
        },
        button: { 
            padding: '12px', 
            backgroundColor: '#C40000', 
            color: 'white', 
            border: 'none', 
            width: '100%', 
            cursor: 'pointer', 
            borderRadius: '6px',
            marginTop: '20px',
            fontSize: '16px',
            fontWeight: 'bold',
            transition: 'background-color 0.3s'
        },
        error: { color: '#FF4D4D', marginTop: '15px', textAlign: 'center' },
        note: { marginTop: '25px', fontSize: '12px', color: '#888', textAlign: 'center' }
    };


    return (
        <div style={loginStyles.container}>
            <h2 style={loginStyles.h2}>Admin Dashboard Login</h2>
            <form onSubmit={handleLogin}>
                <input 
                    type="email" 
                    placeholder="Email" 
                    value={email} 
                    onChange={(e) => setEmail(e.target.value)} 
                    style={loginStyles.input}
                    disabled={loading}
                />
                <input 
                    type="password" 
                    placeholder="Password" 
                    value={password} 
                    onChange={(e) => setPassword(e.target.value)} 
                    style={loginStyles.input}
                    disabled={loading}
                />
                <button 
                    type="submit" 
                    style={loginStyles.button}
                    disabled={loading}
                    onMouseEnter={(e) => e.target.style.backgroundColor = '#990000'}
                    onMouseLeave={(e) => e.target.style.backgroundColor = '#C40000'}
                >
                    {loading ? 'Đang xác thực...' : 'Đăng nhập'}
                </button>
            </form>
            {error && <p style={loginStyles.error}>{error}</p>}
            <p style={loginStyles.note}>
                *Yêu cầu tài khoản đã tạo sẵn trong Firebase Authentication.
            </p>
        </div>
    );
};

export default LoginScreen;