// src/components/LoginScreen.js

import React, { useState } from 'react';
import { auth } from '../firebaseConfig'; 
import { signInWithEmailAndPassword } from 'firebase/auth';

const LoginScreen = ({ setIsLoggedIn }) => {
    // Thay thế bằng tài khoản test mà bạn đã tạo trong Firebase Authentication Console
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
            setIsLoggedIn(true); // Đăng nhập thành công, chuyển sang trang quản lý
            alert('Đăng nhập thành công! Bây giờ bạn có token để Upload.');
        } catch (err) {
            console.error("Login Error:", err);
            setError('Đăng nhập thất bại. Kiểm tra email/password hoặc kết nối mạng.');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div style={{ maxWidth: '400px', margin: '100px auto', padding: '20px', border: '1px solid #ccc', borderRadius: '8px', boxShadow: '0 4px 8px rgba(0,0,0,0.1)' }}>
            <h2>Admin Login (TEST)</h2>
            <form onSubmit={handleLogin}>
                <input 
                    type="email" 
                    placeholder="Email" 
                    value={email} 
                    onChange={(e) => setEmail(e.target.value)} 
                    style={{ padding: '10px', margin: '10px 0', width: '100%', border: '1px solid #ddd' }}
                    disabled={loading}
                />
                <input 
                    type="password" 
                    placeholder="Password" 
                    value={password} 
                    onChange={(e) => setPassword(e.target.value)} 
                    style={{ padding: '10px', margin: '10px 0', width: '100%', border: '1px solid #ddd' }}
                    disabled={loading}
                />
                <button 
                    type="submit" 
                    style={{ padding: '10px', backgroundColor: '#C40000', color: 'white', border: 'none', width: '100%', cursor: 'pointer', borderRadius: '4px' }}
                    disabled={loading}
                >
                    {loading ? 'Đang xác thực...' : 'Đăng nhập'}
                </button>
            </form>
            {error && <p style={{ color: 'red', marginTop: '10px' }}>{error}</p>}
            <p style={{marginTop: '20px', fontSize: '12px', color: '#666'}}>
                *Yêu cầu tài khoản đã tạo sẵn trong Firebase Authentication.
            </p>
        </div>
    );
};

export default LoginScreen;