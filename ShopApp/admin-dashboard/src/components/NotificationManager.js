// src/components/NotificationManager.js

import React, { useState, useEffect } from 'react';
import { collection, addDoc, getDocs } from 'firebase/firestore';
import { db } from '../firebaseConfig'; 

// ----------------------------------------------------------------------
// CẤU HÌNH VÀ STYLES
// ----------------------------------------------------------------------
const styles = {
    container: { padding: '20px', backgroundColor: '#FFFFFF', minHeight: '80vh', maxWidth: '1000px', margin: '0 auto' },
    title: { color: '#000000', borderBottom: '3px solid #C40000', paddingBottom: '10px', marginBottom: '20px', fontWeight: 300, fontSize: '28px' },
    formSection: { padding: '20px', border: '1px solid #ccc', borderRadius: '8px', marginBottom: '30px' },
    formGroup: { marginBottom: '15px' },
    label: { display: 'block', fontWeight: 'bold', marginBottom: '5px' },
    input: { padding: '10px', border: '1px solid #ccc', borderRadius: '4px', width: '100%' },
    textArea: { padding: '10px', border: '1px solid #ccc', borderRadius: '4px', width: '100%', minHeight: '100px' },
    sendButton: { backgroundColor: '#C40000', color: 'white', padding: '10px 20px', border: 'none', borderRadius: '4px', cursor: 'pointer', fontWeight: 'bold', marginTop: '15px' },
    table: { width: '100%', borderCollapse: 'collapse', marginTop: '20px', fontSize: '14px' },
    th: { backgroundColor: '#000000', color: '#FFFFFF', padding: '12px 15px', textAlign: 'left', textTransform: 'uppercase', fontWeight: 500 },
    td: { padding: '10px 15px', borderBottom: '1px solid #EEEEEE' },
    statusTag: (status) => {
        let color = '#666';
        if (status === 'SENT') color = '#28a745';
        if (status === 'PENDING') color = '#007bff';
        if (status === 'FAILED') color = '#dc3545';
        return { backgroundColor: color, color: 'white', padding: '4px 8px', borderRadius: '12px', fontSize: '12px', fontWeight: 'bold', display: 'inline-block' };
    }
};

const NotificationManager = () => {
    const [title, setTitle] = useState('');
    const [body, setBody] = useState('');
    const [scheduleTime, setScheduleTime] = useState(''); // Định dạng ISO Date String cho input
    const [loading, setLoading] = useState(false);
    const [history, setHistory] = useState([]);

    // ----------------------------------------------------------------------
    // HÀM FETCH LỊCH SỬ
    // ----------------------------------------------------------------------
    const fetchHistory = async () => {
        try {
            const notifCollectionRef = collection(db, "scheduledNotifications");
            const snapshot = await getDocs(notifCollectionRef);

            const historyList = snapshot.docs.map(doc => ({
                id: doc.id,
                ...doc.data()
            }));

            // Sắp xếp theo ngày gần nhất
            historyList.sort((a, b) => (b.createdAt || 0) - (a.createdAt || 0));
            setHistory(historyList);
        } catch (err) {
            console.error("Lỗi tải lịch sử thông báo:", err);
        }
    };

    useEffect(() => {
        fetchHistory();
    }, []);

    // ----------------------------------------------------------------------
    // HÀM XỬ LÝ GỬI / ĐẶT LỊCH
    // ----------------------------------------------------------------------
    const handleSubmit = async (e) => {
        e.preventDefault();
        if (!title || !body) {
            alert('Vui lòng nhập Tiêu đề và Nội dung.');
            return;
        }

        setLoading(true);
        const isScheduled = !!scheduleTime;
        
        let scheduledTimestamp = null;
        if (isScheduled) {
            scheduledTimestamp = new Date(scheduleTime).getTime();
            if (scheduledTimestamp < Date.now()) {
                alert("Thời gian đặt lịch không hợp lệ (Phải là tương lai).");
                setLoading(false);
                return;
            }
        }

        const notificationData = {
    title: title,
    body: body,
    scheduleTime: scheduledTimestamp || Date.now(), 
    status: 'PENDING', // <--- FIXED: Luôn là PENDING để Backend xử lý
    createdAt: Date.now(),
};

        try {
            // Lưu vào Firestore. Backend Cloud Function sẽ đọc collection này và gửi đi.
            const collectionRef = collection(db, "scheduledNotifications");
            await addDoc(collectionRef, notificationData);

            alert(isScheduled 
                ? `Đã đặt lịch thông báo thành công vào: ${new Date(scheduledTimestamp).toLocaleString()}` 
                : 'Yêu cầu gửi thông báo NGAY LẬP TỨC đã được ghi nhận. Backend sẽ xử lý.');
            
            // Reset form và tải lại lịch sử
            setTitle('');
            setBody('');
            setScheduleTime('');
            fetchHistory(); 

        } catch (error) {
            console.error("Lỗi khi gửi/đặt lịch thông báo:", error);
            alert("LỖI: Không thể lưu thông báo. Kiểm tra kết nối Firestore.");
        } finally {
            setLoading(false);
        }
    };

    // ----------------------------------------------------------------------
    // RENDER
    // ----------------------------------------------------------------------

    return (
        <div style={styles.container}>
            <h1 style={styles.title}>Quản Lý & Xuất bản Thông báo (FCM)</h1>
            
            {/* FORM TẠO/ĐẶT LỊCH THÔNG BÁO */}
            <div style={styles.formSection}>
                <h2>Tạo Thông báo mới (Notification Payload)</h2>
                <p style={{color: '#666', marginBottom: '15px'}}>Ứng dụng di động sẽ tự động hiển thị tiêu đề và nội dung này.</p>
                
                <form onSubmit={handleSubmit}>
                    <div style={styles.formGroup}>
                        <label style={styles.label}>Tiêu đề (Title):</label>
                        <input type="text" value={title} onChange={(e) => setTitle(e.target.value)} style={styles.input} placeholder="Tiêu đề thông báo (Tối đa 50 ký tự)" required />
                    </div>
                    
                    <div style={styles.formGroup}>
                        <label style={styles.label}>Nội dung (Body):</label>
                        <textarea value={body} onChange={(e) => setBody(e.target.value)} style={styles.textArea} placeholder="Nội dung chi tiết của thông báo" required />
                    </div>

                    <hr style={{margin: '20px 0'}}/>

                    <div style={styles.formGroup}>
                        <label style={styles.label}>Đặt lịch gửi (Schedule Time - Tùy chọn):</label>
                        <input 
                            type="datetime-local" 
                            value={scheduleTime} 
                            onChange={(e) => setScheduleTime(e.target.value)} 
                            style={styles.input} 
                            min={new Date().toISOString().slice(0, 16)} // Giới hạn ngày giờ trong tương lai
                        />
                        <small style={{color: '#C40000', marginTop: '5px'}}>Bỏ trống để gửi NGAY LẬP TỨC.</small>
                    </div>

                    <button type="submit" style={styles.sendButton} disabled={loading}>
                        {loading ? 'Đang xử lý...' : (scheduleTime ? 'Đặt lịch Xuất bản' : 'Gửi NGAY')}
                    </button>
                </form>
            </div>

            {/* BẢNG LỊCH SỬ VÀ LỊCH ĐÃ ĐẶT */}
            <div style={{ marginTop: '40px' }}>
                <h2>Lịch sử & Thông báo đã Đặt lịch</h2>
                <table style={styles.table}>
                    <thead>
                        <tr>
                            <th style={styles.th}>Thời điểm Gửi</th>
                            <th style={styles.th}>Tiêu đề</th>
                            <th style={styles.th}>Trạng thái</th>
                            <th style={styles.th}>Ngày tạo</th>
                        </tr>
                    </thead>
                    <tbody>
                        {history.map(item => (
                            <tr key={item.id}>
                                <td style={styles.td}>
                                    {new Date(item.scheduleTime).toLocaleString()}
                                </td>
                                <td style={styles.td}>
                                    {item.title}
                                </td>
                                <td style={styles.td}>
                                    <span style={styles.statusTag(item.status)}>
                                        {item.status}
                                    </span>
                                </td>
                                <td style={styles.td}>
                                    <small>{new Date(item.createdAt).toLocaleDateString()}</small>
                                </td>
                            </tr>
                        ))}
                        {history.length === 0 && (
                            <tr>
                                <td colSpan="4" style={{ textAlign: 'center', padding: '20px', color: '#666' }}>
                                    Chưa có thông báo nào được lưu.
                                </td>
                            </tr>
                        )}
                    </tbody>
                </table>
            </div>
        </div>
    );
};

export default NotificationManager;