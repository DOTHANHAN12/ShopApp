/**
 * Import function triggers from their respective submodules:
 *
 * const {onCall} = require("firebase-functions/v2/https");
 * const {onDocumentWritten} = require("firebase-functions/v2/firestore");
 *
 * See a full list of supported triggers at https://firebase.google.com/docs/functions
 */

const {setGlobalOptions} = require("firebase-functions");
const {onRequest} = require("firebase-functions/https");
const logger = require("firebase-functions/logger");

// For cost control, you can set the maximum number of containers that can be
// running at the same time. This helps mitigate the impact of unexpected
// traffic spikes by instead downgrading performance. This limit is a
// per-function limit. You can override the limit for each function using the
// `maxInstances` option in the function's options, e.g.
// `onRequest({ maxInstances: 5 }, (req, res) => { ... })`.
// NOTE: setGlobalOptions does not apply to functions using the v1 API. V1
// functions should each use functions.runWith({ maxInstances: 10 }) instead.
// In the v1 API, each function can only serve one request per container, so
// this will be the maximum concurrent request count.
setGlobalOptions({ maxInstances: 10 });

// Create and deploy your first functions
// https://firebase.google.com/docs/functions/get-started
const functions = require('firebase-functions');
const admin = require('firebase-admin');
const axios = require('axios');
const crypto = require('crypto');
const moment = require('moment');

// --- KHỞI TẠO VÀ CẤU HÌNH ---
admin.initializeApp();

// Lấy các giá trị khóa bí mật từ cấu hình Firebase (Đã thiết lập ở bước trước)
const partnerCode = functions.config().momo.partner_code;
const accessKey = functions.config().momo.access_key;
const secretKey = functions.config().momo.secret_key;

// URL Momo Sandbox/Production
const momoApiUrl = "https://payment.momo.vn/v2/gateway/api/create";

// URL mà Momo App sẽ gửi người dùng trở lại App của bạn
// URL này phải được đăng ký trong cấu hình Momo của bạn
const redirectUrl = "https://your-app-domain.com/payment/momo-callback"; // THAY THẾ BẰNG DOMAIN THẬT CỦA BẠN (hoặc API Endpoint)
const ipnUrl = redirectUrl;
// -----------------------------

exports.createMomoTransaction = functions.https.onCall(async (data, context) => {
    // 1. Kiểm tra xác thực người dùng
    if (!context.auth) {
        throw new functions.https.HttpsError('unauthenticated', 'Yêu cầu đăng nhập.');
    }

    // 2. Nhận dữ liệu từ App Android
    const { amount, orderId, orderInfo } = data; // amount là VND, orderId là mã giao dịch duy nhất

    // 3. Tạo các tham số duy nhất
    const requestId = partnerCode + new Date().getTime();

    // 4. TẠO CHUỖI RAW HASH (Đúng thứ tự Momo yêu cầu)
    const rawSignature =
        "accessKey=" + accessKey +
        "&amount=" + amount +
        "&extraData=" + "" +
        "&ipnUrl=" + ipnUrl +
        "&orderId=" + orderId +
        "&orderInfo=" + orderInfo +
        "&partnerCode=" + partnerCode +
        "&redirectUrl=" + redirectUrl +
        "&requestId=" + requestId +
        "&requestType=captureWallet" // Loại giao dịch
    ;

    // 5. TẠO CHỮ KÝ SHA256 (Hash)
    const signature = crypto.createHmac('sha256', secretKey)
                            .update(rawSignature)
                            .digest('hex');

    // 6. TẠO BODY GỬI ĐẾN MOMO
    const requestBody = {
        partnerCode: partnerCode,
        accessKey: accessKey,
        requestId: requestId,
        amount: amount,
        orderId: orderId,
        orderInfo: orderInfo,
        redirectUrl: redirectUrl,
        ipnUrl: ipnUrl,
        lang: 'vi',
        extraData: '',
        requestType: 'captureWallet',
        signature: signature
    };

    try {
        // 7. GỌI API MOMO
        const response = await axios.post(momoApiUrl, requestBody);

        if (response.data && response.data.payUrl) {
            // Trả lại payUrl (Deep Link) cho App Android
            return {
                success: true,
                payUrl: response.data.payUrl,
                message: 'Thành công'
            };
        } else {
            console.error("Momo API trả về lỗi:", response.data);
            return { success: false, message: 'Lỗi từ cổng Momo: ' + (response.data.message || 'Không rõ') };
        }
    } catch (error) {
        console.error("Lỗi HTTP khi gọi Momo:", error.message);
        return { success: false, message: 'Lỗi kết nối Backend: ' + error.message };
    }
});
// exports.helloWorld = onRequest((request, response) => {
//   logger.info("Hello logs!", {structuredData: true});
//   response.send("Hello from Firebase!");
// });
