// functions/index.js

const admin = require('firebase-admin');
const { onSchedule } = require('firebase-functions/v2/scheduler'); 
const { setGlobalOptions } = require('firebase-functions/v2'); 

// Khởi tạo Admin SDK
admin.initializeApp();
const db = admin.firestore();
const messaging = admin.messaging(); // CÁCH NÀY ỔN ĐỊNH HƠN

// Đặt cấu hình chung
setGlobalOptions({ 
    region: 'asia-east1', 
    maxInstances: 10
});

// 1. CHỨC NĂNG CHÍNH: Gửi thông báo đến tất cả người dùng
async function sendFCMNotification(notification) {
    const message = {
        notification: {
            title: notification.title,
            body: notification.body,
        },
        topic: 'all_users'
    };
    
    try {
        const response = await messaging.send(message);
        console.log('Successfully sent message:', response);
        return true;
    } catch (error) {
        console.error('Error sending message:', error);
        return false;
    }
}


// 2. CLOUD FUNCTION ĐỊNH KỲ (CRON JOB)
exports.checkScheduledNotifications = onSchedule({
    schedule: 'every 5 minutes',
    timeZone: 'Asia/Ho_Chi_Minh' 
    
}, async (event) => {
    
    const now = Date.now();
    const notificationRef = db.collection('scheduledNotifications');

    // 1. Truy vấn các thông báo cần gửi
    const snapshot = await notificationRef
        .where('status', '==', 'PENDING')
        .where('scheduleTime', '<=', now)
        .get();

    if (snapshot.empty) {
        console.log('Không có thông báo nào đang chờ gửi.');
        return null;
    }

    console.log(`Tìm thấy ${snapshot.size} thông báo cần gửi.`);

    const updates = [];
    
    // 2. Gửi và Cập nhật trạng thái
    for (const doc of snapshot.docs) {
        const notification = doc.data();
        const notificationDocRef = notificationRef.doc(doc.id);
        
        const sendSuccess = await sendFCMNotification(notification); 

        if (sendSuccess) {
            updates.push(notificationDocRef.update({ 
                status: 'SENT', 
                sentTime: admin.firestore.FieldValue.serverTimestamp() 
            }));
        } else {
             updates.push(notificationDocRef.update({ 
                status: 'FAILED'
            }));
        }
    }

    await Promise.all(updates);
    return null;
});