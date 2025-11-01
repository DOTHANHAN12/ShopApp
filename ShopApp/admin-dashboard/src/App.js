// src/App.js
import React, { useState, useEffect } from 'react';
import { onAuthStateChanged } from 'firebase/auth';
import { auth, logoutUser } from './firebaseConfig'; 
import ProductList from './components/ProductList'; 
import OrderList from './components/OrderList'; 
import LoginScreen from './components/LoginScreen'; 
import UserList from './components/UserList'; 
import NotificationManager from './components/NotificationManager'; 
import AdminReviewControl from './components/AdminReviewControlFirebase';
import AdminVoucherManagement from './components/AdminVoucherManagement';

// --- DARK/MINIMALIST STYLES ---
const styles = {
    layout: { display: 'flex', minHeight: '100vh', backgroundColor: '#1A1A1A', color: '#E0E0E0' },
    
    sidebar: {
        width: '250px',
        backgroundColor: '#000000',
        padding: '20px 0',
        boxShadow: '4px 0 10px rgba(0,0,0,0.5)',
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        position: 'fixed',
        height: '100%',
        zIndex: 100
    },
    
    logo: {
        color: '#C40000',
        fontSize: '32px',
        fontWeight: 900,
        textTransform: 'uppercase',
        letterSpacing: '3px',
        marginBottom: '40px',
        marginTop: '10px'
    },
    
    nav: {
        width: '100%',
        flexGrow: 1
    },
    
    navItem: (isActive) => ({
        display: 'flex',
        alignItems: 'center',
        padding: '15px 30px',
        cursor: 'pointer',
        color: isActive ? '#FFFFFF' : '#A0A0A0',
        backgroundColor: isActive ? '#1A1A1A' : 'transparent',
        borderLeft: isActive ? '5px solid #C40000' : '5px solid transparent',
        fontWeight: isActive ? 'bold' : 'normal',
        transition: 'all 0.2s ease-in-out',
        marginBottom: '5px'
    }),
    
    mainContent: {
        flexGrow: 1,
        marginLeft: '250px', // Đảm bảo nội dung không bị che bởi sidebar
        padding: '40px 30px',
        width: 'calc(100% - 250px)',
        boxSizing: 'border-box'
    },

    // Card/Container style cho nội dung chính
    contentContainer: {
        backgroundColor: '#292929', // Màu tối hơn cho container
        padding: '30px',
        borderRadius: '10px',
        boxShadow: '0 4px 12px rgba(0,0,0,0.4)',
        minHeight: '85vh'
    },
    
    logoutContainer: {
        marginTop: 'auto', // Đẩy xuống cuối sidebar
        padding: '20px',
        width: '100%',
        borderTop: '1px solid #333'
    },
    
    logoutButton: {
        width: '100%',
        backgroundColor: '#C40000',
        color: 'white',
        border: 'none',
        padding: '10px',
        borderRadius: '5px',
        cursor: 'pointer',
        fontWeight: 'bold',
        transition: 'background-color 0.3s',
        fontSize: '16px'
    }
};

const navItems = [
    { key: 'products', name: '📦 SẢN PHẨM', component: ProductList },
    { key: 'orders', name: '📋 ĐƠN HÀNG', component: OrderList },
    { key: 'users', name: '👥 NGƯỜI DÙNG', component: UserList },
    { key: 'notifications', name: '🔔 THÔNG BÁO', component: NotificationManager },
    { key: 'reviews', name: '🔔 REVIEWS', component: AdminReviewControl },
    { key: 'voucher', name: '🔔 VOUCHER', component: AdminVoucherManagement },
];


function App() {
    const [currentPage, setCurrentPage] = useState('products');
    const [isLoggedIn, setIsLoggedIn] = useState(false); 

    useEffect(() => {
        const unsubscribe = onAuthStateChanged(auth, (user) => {
            setIsLoggedIn(!!user); 
        });
        return () => unsubscribe();
    }, []);

    const handleLogout = async () => {
        try {
            await logoutUser();
            setCurrentPage('products'); 
        } catch (error) {
            console.error("Lỗi đăng xuất:", error);
            alert("Đăng xuất thất bại. Vui lòng thử lại.");
        }
    };

    const renderContent = () => {
        if (!isLoggedIn) {
            // Căn giữa LoginScreen cho giao diện tối
            return (
                <div style={{...styles.layout, flexDirection: 'column', justifyContent: 'center', alignItems: 'center'}}>
                    <LoginScreen setIsLoggedIn={setIsLoggedIn} />
                </div>
            );
        }
        
        const CurrentComponent = navItems.find(item => item.key === currentPage)?.component;
        
        return (
            <div style={styles.layout}>
                
                {/* --- SIDEBAR --- */}
                <div style={styles.sidebar}>
                    <div style={styles.logo}>ADMIN HUB</div>
                    <nav style={styles.nav}>
                        {navItems.map(item => (
                            <div 
                                key={item.key}
                                style={styles.navItem(currentPage === item.key)}
                                onClick={() => setCurrentPage(item.key)}
                            >
                                {item.name}
                            </div>
                        ))}
                    </nav>
                    <div style={styles.logoutContainer}>
                         <button style={styles.logoutButton} onClick={handleLogout}>
                            Đăng Xuất
                        </button>
                    </div>
                </div>

                {/* --- MAIN CONTENT --- */}
                <main style={styles.mainContent}>
                    <div style={styles.contentContainer}>
                        {CurrentComponent ? <CurrentComponent /> : <div>Trang không tồn tại</div>}
                    </div>
                </main>
            </div>
        );
    };

    return (
        <div style={{ fontFamily: 'Segoe UI, sans-serif', margin: 0, padding: 0 }}>
            {renderContent()}
        </div>
    );
}

export default App;