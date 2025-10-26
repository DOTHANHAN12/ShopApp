// src/App.js
import React, { useState, useEffect } from 'react';
import { onAuthStateChanged } from 'firebase/auth';
import { auth, logoutUser } from './firebaseConfig'; // Import Auth và Logout
import TestUpload from './components/TestUpload'; // Component Test Upload đã sửa
import LoginScreen from './components/LoginScreen'; // Component Login đã sửa
import ProductList from './components/ProductList';

function App() {
    const [currentPage, setCurrentPage] = useState('products');
    const [isLoggedIn, setIsLoggedIn] = useState(false); // State theo dõi đăng nhập

    // Theo dõi trạng thái Auth Firebase
    useEffect(() => {
        const unsubscribe = onAuthStateChanged(auth, (user) => {
            setIsLoggedIn(!!user); // Cập nhật state đăng nhập
        });
        return () => unsubscribe();
    }, []);

    const handleLogout = async () => {
        try {
            await logoutUser();
            setCurrentPage('products'); // Reset trang về mặc định
        } catch (error) {
            console.error("Lỗi đăng xuất:", error);
            alert("Đăng xuất thất bại. Vui lòng thử lại.");
        }
    };

    const styles = {
        // ... (Styles không thay đổi)
        navbar: {
            backgroundColor: '#000000',
            color: '#FFFFFF',
            padding: '15px 30px',
            fontWeight: 'bold',
            fontSize: '20px',
            letterSpacing: '2px',
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
        },
        logo: {
            color: '#C40000',
            fontSize: '28px',
            fontWeight: 900,
            textTransform: 'uppercase'
        },
        navItem: {
            color: '#FFFFFF',
            textDecoration: 'none',
            padding: '0 15px',
            cursor: 'pointer',
            transition: 'color 0.3s',
        },
        navItemSelected: {
            color: '#C40000',
            fontWeight: 'bold',
            borderBottom: '2px solid #C40000',
            paddingBottom: '5px',
        },
        mainContent: {
            backgroundColor: '#F5F5F5',
            minHeight: 'calc(100vh - 60px)',
            padding: '40px 20px',
        },
        container: {
            maxWidth: '1200px',
            margin: '0 auto',
            backgroundColor: '#FFFFFF',
            padding: '30px',
            borderRadius: '8px',
            boxShadow: '0 2px 8px rgba(0,0,0,0.1)',
        },
        title: {
            fontSize: '32px',
            fontWeight: 'bold',
            marginBottom: '20px',
            color: '#000',
            textTransform: 'uppercase',
            letterSpacing: '1px',
        },
        card: {
            backgroundColor: '#FFFFFF',
            border: '1px solid #E0E0E0',
            borderRadius: '8px',
            padding: '20px',
            marginBottom: '15px',
            transition: 'box-shadow 0.3s',
        },
        logoutButton: {
            backgroundColor: '#FF4D4D',
            color: 'white',
            border: 'none',
            padding: '8px 15px',
            borderRadius: '4px',
            cursor: 'pointer',
            fontWeight: 'bold',
            marginLeft: '20px',
            transition: 'background-color 0.3s',
        }
    };

    const renderContent = () => {
        // TRƯỜNG HỢP 1: CHƯA ĐĂNG NHẬP
        if (!isLoggedIn) {
            return <LoginScreen />;
        }
        
        // TRƯỜNG HỢP 2: ĐÃ ĐĂNG NHẬP (Hiển thị nội dung chính)
        // Hiện tại, trang 'products' là TestUpload
        if (currentPage === 'products') {
            return <ProductList/>; 
        }
        
        if (currentPage === 'orders') {
            return (
                <div style={styles.container}>
                    <h1 style={styles.title}>Quản lý đơn hàng</h1>
                    <div style={styles.card}><p style={{ color: '#666' }}>Chức năng đang được phát triển...</p></div>
                </div>
            );
        }
        
        if (currentPage === 'vouchers') {
            return (
                <div style={styles.container}>
                    <h1 style={styles.title}>Quản lý voucher</h1>
                    <div style={styles.card}><p style={{ color: '#666' }}>Chức năng đang được phát triển...</p></div>
                </div>
            );
        }
        
        return (
            <div style={styles.container}>
                <h1 style={styles.title}>Trang không tồn tại</h1>
            </div>
        );
    };

    return (
        <div style={{ fontFamily: 'Arial, sans-serif', margin: 0, padding: 0 }}>
            {/* Ẩn Navbar nếu chưa đăng nhập */}
            {isLoggedIn && (
                <nav style={styles.navbar}>
                    <div style={styles.logo}>ADMIN DASH</div>
                    <div style={{ display: 'flex', alignItems: 'center' }}>
                        <span 
                            style={{ 
                                ...styles.navItem, 
                                ...(currentPage === 'products' ? styles.navItemSelected : {}) 
                            }}
                            onClick={() => setCurrentPage('products')}
                            onMouseEnter={(e) => { if (currentPage !== 'products') { e.target.style.color = '#C40000'; } }}
                            onMouseLeave={(e) => { if (currentPage !== 'products') { e.target.style.color = '#FFFFFF'; } }}
                        >
                            SẢN PHẨM (TEST UPLOAD)
                        </span>
                        <span 
                            style={{ 
                                ...styles.navItem, 
                                ...(currentPage === 'orders' ? styles.navItemSelected : {}) 
                            }}
                            onClick={() => setCurrentPage('orders')}
                            onMouseEnter={(e) => { if (currentPage !== 'orders') { e.target.style.color = '#C40000'; } }}
                            onMouseLeave={(e) => { if (currentPage !== 'orders') { e.target.style.color = '#FFFFFF'; } }}
                        >
                            ĐƠN HÀNG
                        </span>
                        <span 
                            style={{ 
                                ...styles.navItem, 
                                ...(currentPage === 'vouchers' ? styles.navItemSelected : {}) 
                            }}
                            onClick={() => setCurrentPage('vouchers')}
                            onMouseEnter={(e) => { if (currentPage !== 'vouchers') { e.target.style.color = '#C40000'; } }}
                            onMouseLeave={(e) => { if (currentPage !== 'vouchers') { e.target.style.color = '#FFFFFF'; } }}
                        >
                            VOUCHER
                        </span>
                        <button style={styles.logoutButton} onClick={handleLogout}
                            onMouseEnter={(e) => { e.target.style.backgroundColor = '#FF0000'; }}
                            onMouseLeave={(e) => { e.target.style.backgroundColor = '#FF4D4D'; }}
                        >
                            Đăng Xuất
                        </button>
                    </div>
                </nav>
            )}
            <main style={styles.mainContent}>
                {renderContent()}
            </main>
        </div>
    );
}

export default App;