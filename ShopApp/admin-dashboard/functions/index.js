// functions/index.js
const admin = require('firebase-admin');
const { onSchedule } = require('firebase-functions/v2/scheduler');
const { onRequest } = require('firebase-functions/v2/https');
const { setGlobalOptions } = require('firebase-functions/v2');
const { logger } = require('firebase-functions');

admin.initializeApp();
const db = admin.firestore();
const messaging = admin.messaging();

setGlobalOptions({
    region: 'asia-east1',
    maxInstances: 10,
    timeoutSeconds: 540,
    memory: '512MiB',
});

const NOTIFICATION_COLLECTION = 'scheduledNotifications';
const USER_NOTIFICATIONS_COLLECTION = 'notifications';

// ============================================================================
// 🔧 BUILD FCM MESSAGE - FIXED VERSION
// ============================================================================
function buildFCMMessage(notification) {
    const message = {
        topic: 'all_users',
        notification: {
            title: notification.title || 'Thông báo mới',
            body: notification.body || '',
        },
        // ⚠️ QUAN TRỌNG: Phải convert tất cả thành STRING
        data: {
            title: String(notification.title || ''),
            body: String(notification.body || ''),
            type: String(notification.type || 'SYSTEM'),
            actionType: String(notification.actionType || 'NONE'),
            actionData: String(notification.actionData || ''),
            icon: String(notification.icon || 'bell'),
            priority: String(notification.priority || 1),
            imageUrl: String(notification.imageUrl || ''),
        },
        android: {
            priority: (notification.priority || 1) >= 2 ? 'high' : 'normal',
            notification: {
                sound: 'default',
                channelId: 'fcm_default_channel',
                icon: 'ic_notification',
                color: '#667eea',
                priority: (notification.priority || 1) >= 2 ? 'high' : 'default',
            },
        },
        apns: {
            payload: {
                aps: {
                    sound: 'default',
                    badge: 1,
                },
            },
        },
    };

    // Add image to notification payload if available
    if (notification.imageUrl) {
        message.notification.imageUrl = notification.imageUrl;
    }

    return message;
}

// ============================================================================
// 📤 SEND FCM NOTIFICATION
// ============================================================================
async function sendFCMNotification(notification) {
    try {
        const message = buildFCMMessage(notification);
        
        logger.info('Sending FCM with data:', {
            title: notification.title,
            type: notification.type,
            actionType: notification.actionType,
            actionData: notification.actionData,
            imageUrl: notification.imageUrl,
        });

        const response = await messaging.send(message);
        
        logger.info('FCM sent successfully:', response);

        return {
            success: true,
            messageId: response,
            error: null,
        };
    } catch (error) {
        logger.error('FCM error:', error);
        return {
            success: false,
            messageId: null,
            error: error.message,
        };
    }
}

// ============================================================================
// 💾 SAVE TO USER NOTIFICATIONS
// ============================================================================
async function saveNotificationToUsers(notification) {
    try {
        const usersSnapshot = await db.collection('users').limit(1000).get();
        
        const batch = db.batch();
        let count = 0;

        for (const userDoc of usersSnapshot.docs) {
            const notificationRef = db.collection(USER_NOTIFICATIONS_COLLECTION).doc();
            
            // ⚠️ ĐẢM BẢO TẤT CẢ FIELDS KHÔNG NULL
            batch.set(notificationRef, {
                notificationId: notificationRef.id,
                userId: userDoc.id,
                title: notification.title || '',
                body: notification.body || '',
                imageUrl: notification.imageUrl || '',
                type: notification.type || 'SYSTEM',
                actionType: notification.actionType || 'NONE',
                actionData: notification.actionData || '',
                icon: notification.icon || 'bell',
                priority: notification.priority || 1,
                isRead: false,
                timestamp: admin.firestore.FieldValue.serverTimestamp(),
                senderName: 'Hệ thống',
                extraData: {},
            });

            count++;
            
            if (count === 500) {
                await batch.commit();
                count = 0;
            }
        }

        if (count > 0) {
            await batch.commit();
        }

        logger.info(`Saved to ${usersSnapshot.size} users`);
    } catch (error) {
        logger.error('Error saving to users:', error);
    }
}

// ============================================================================
// 🔄 UPDATE STATUS
// ============================================================================
async function updateNotificationStatus(docId, status, additionalData = {}) {
    await db.collection(NOTIFICATION_COLLECTION).doc(docId).update({
        status,
        lastUpdated: admin.firestore.FieldValue.serverTimestamp(),
        ...additionalData,
    });
}

// ============================================================================
// ⏰ SCHEDULED FUNCTION - Every 5 minutes
// ============================================================================
exports.checkScheduledNotifications = onSchedule({
    schedule: 'every 5 minutes',
    timeZone: 'Asia/Ho_Chi_Minh',
    retryCount: 3,
}, async (event) => {
    logger.info('🚀 Checking scheduled notifications');

    try {
        const now = Date.now();
        const snapshot = await db.collection(NOTIFICATION_COLLECTION)
            .where('status', '==', 'PENDING')
            .where('scheduleTime', '<=', now)
            .limit(100)
            .get();

        if (snapshot.empty) {
            logger.info('✅ No pending notifications');
            return null;
        }

        logger.info(`📬 Found ${snapshot.size} notifications`);

        const results = {
            total: snapshot.size,
            sent: 0,
            failed: 0,
        };

        for (const doc of snapshot.docs) {
            const notification = { id: doc.id, ...doc.data() };
            
            logger.info('Processing notification:', {
                id: doc.id,
                title: notification.title,
                actionType: notification.actionType,
                actionData: notification.actionData,
            });

            const sendResult = await sendFCMNotification(notification);

            if (sendResult.success) {
                await updateNotificationStatus(doc.id, 'SENT', {
                    sentTime: admin.firestore.FieldValue.serverTimestamp(),
                    messageId: sendResult.messageId,
                });

                await saveNotificationToUsers(notification);
                results.sent++;
            } else {
                await updateNotificationStatus(doc.id, 'FAILED', {
                    failedTime: admin.firestore.FieldValue.serverTimestamp(),
                    errorMessage: sendResult.error,
                });
                results.failed++;
            }
        }

        logger.info('✅ Completed:', results);
        return results;
    } catch (error) {
        logger.error('❌ Error:', error);
        throw error;
    }
});

// ============================================================================
// 🔥 HTTP ENDPOINT - Send immediately
// ============================================================================
exports.sendNotificationNow = onRequest({
    cors: true,
    timeoutSeconds: 60,
}, async (req, res) => {
    if (req.method !== 'POST') {
        return res.status(405).json({ error: 'Method not allowed' });
    }

    try {
        const notification = req.body;

        if (!notification.title || !notification.body) {
            return res.status(400).json({
                error: 'Missing title or body',
            });
        }

        const notificationData = {
            title: notification.title,
            body: notification.body,
            type: notification.type || 'SYSTEM',
            actionType: notification.actionType || 'NONE',
            actionData: notification.actionData || '',
            icon: notification.icon || 'bell',
            priority: notification.priority || 1,
            imageUrl: notification.imageUrl || '',
            scheduleTime: Date.now(),
            status: 'PENDING',
            createdAt: Date.now(),
        };

        const docRef = await db.collection(NOTIFICATION_COLLECTION).add(notificationData);
        
        const sendResult = await sendFCMNotification({
            id: docRef.id,
            ...notificationData,
        });

        if (sendResult.success) {
            await updateNotificationStatus(docRef.id, 'SENT', {
                sentTime: admin.firestore.FieldValue.serverTimestamp(),
                messageId: sendResult.messageId,
            });

            await saveNotificationToUsers(notificationData);

            return res.status(200).json({
                success: true,
                notificationId: docRef.id,
                messageId: sendResult.messageId,
            });
        } else {
            await updateNotificationStatus(docRef.id, 'FAILED', {
                failedTime: admin.firestore.FieldValue.serverTimestamp(),
                errorMessage: sendResult.error,
            });

            return res.status(500).json({
                success: false,
                error: sendResult.error,
            });
        }
    } catch (error) {
        logger.error('Error:', error);
        return res.status(500).json({
            success: false,
            error: error.message,
        });
    }
});

// ============================================================================
// 📊 GET STATISTICS
// ============================================================================
exports.getNotificationStats = onRequest({
    cors: true,
}, async (req, res) => {
    try {
        const [sentSnapshot, pendingSnapshot, failedSnapshot] = await Promise.all([
            db.collection(NOTIFICATION_COLLECTION).where('status', '==', 'SENT').count().get(),
            db.collection(NOTIFICATION_COLLECTION).where('status', '==', 'PENDING').count().get(),
            db.collection(NOTIFICATION_COLLECTION).where('status', '==', 'FAILED').count().get(),
        ]);

        const stats = {
            total: sentSnapshot.data().count + pendingSnapshot.data().count + failedSnapshot.data().count,
            sent: sentSnapshot.data().count,
            pending: pendingSnapshot.data().count,
            failed: failedSnapshot.data().count,
            timestamp: new Date().toISOString(),
        };

        return res.status(200).json(stats);
    } catch (error) {
        logger.error('Error:', error);
        return res.status(500).json({ error: error.message });
    }
});

// ============================================================================
// 🗑️ CLEANUP OLD NOTIFICATIONS
// ============================================================================
exports.cleanupOldNotifications = onSchedule({
    schedule: 'every 24 hours',
    timeZone: 'Asia/Ho_Chi_Minh',
}, async (event) => {
    logger.info('🧹 Cleanup old notifications');

    try {
        const thirtyDaysAgo = Date.now() - (30 * 24 * 60 * 60 * 1000);
        const snapshot = await db.collection(NOTIFICATION_COLLECTION)
            .where('createdAt', '<', thirtyDaysAgo)
            .where('status', 'in', ['SENT', 'FAILED'])
            .limit(500)
            .get();

        if (snapshot.empty) {
            logger.info('✅ Nothing to clean');
            return null;
        }

        const batch = db.batch();
        snapshot.docs.forEach((doc) => {
            batch.delete(doc.ref);
        });

        await batch.commit();
        logger.info(`✅ Deleted ${snapshot.size} old notifications`);
        return { deleted: snapshot.size };
    } catch (error) {
        logger.error('Error:', error);
        throw error;
    }
});