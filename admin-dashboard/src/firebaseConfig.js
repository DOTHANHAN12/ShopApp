// src/firebaseConfig.js
import { initializeApp } from "firebase/app";
import { getFirestore } from "firebase/firestore"; 
import { getStorage, ref, uploadBytes, getDownloadURL, deleteObject } from "firebase/storage";
import { getAuth, signInWithEmailAndPassword, signOut, onAuthStateChanged } from "firebase/auth"; // Thêm signOut và onAuthStateChanged

// Cấu hình Firebase của bạn - ĐÃ HOÀN TOÀN ĐÚNG
const firebaseConfig = {
  apiKey: "AIzaSyCbtwv6-UrBcqyfIkeHqMOOTJhQcEGXwgA",
  authDomain: "android-c164b.firebaseapp.com",
  databaseURL: "https://android-c164b-default-rtdb.firebaseio.com",
  projectId: "android-c164b",
  storageBucket: "android-c164b.firebasestorage.app",
  messagingSenderId: "69140808985",
  appId: "1:69140808985:web:8f1b420c774f81cc023188",
  measurementId: "G-9V7CZ4EJQR"
};


const app = initializeApp(firebaseConfig);
export const db = getFirestore(app); 
export const storage = getStorage(app); 
export const auth = getAuth(app); // EXPORT AUTH

// *** HÀM TIỆN ÍCH KIỂM TRA URL ***
const isFirebaseStorageUrl = (url) => {
    return typeof url === 'string' && 
           (url.includes('firebasestorage.googleapis.com') || url.includes('.appspot.com/o/'));
};


// ----------------------------------------------------------------------
// HÀM TIỆN ÍCH TẢI LÊN (Đã được xác nhận là không bị CORS)
// ----------------------------------------------------------------------
export const uploadFile = async (file, path) => {
    const storageRef = ref(storage, `${path}/${file.name}`);
    // *** uploadBytes: SỬ DỤNG PHƯƠNG THỨC POST/PUT TRỰC TIẾP, KHÔNG LỖI CORS ***
    const snapshot = await uploadBytes(storageRef, file); 
    const downloadURL = await getDownloadURL(snapshot.ref);
    return downloadURL;
};

// ----------------------------------------------------------------------
// HÀM TIỆN ÍCH XÓA FILE
// ----------------------------------------------------------------------
export const deleteFile = async (fileUrl) => {
    if (!fileUrl) return true;
    
    if (!isFirebaseStorageUrl(fileUrl)) {
         console.warn(`[DELETE WARNING] Bỏ qua xóa vì URL không phải Firebase: ${fileUrl}`);
         return true; 
    }

    try {
        // ref(storage, fileUrl) sẽ phân tích URL để lấy ra Storage path
        const fileRef = ref(storage, fileUrl); 
        await deleteObject(fileRef);
        console.log("File deleted successfully:", fileUrl);
        return true;
    } catch (error) {
        // Lỗi thường gặp: storage/object-not-found hoặc storage/unauthorized
        console.error("Error deleting file from Storage:", error.code, error.message);
        
        if (error.code === 'storage/object-not-found' || error.code === 'storage/invalid-url') {
             console.warn(`[DELETE WARNING] Error code ${error.code}. Treating as successful removal from DB context.`);
             return true;
        }
        return false; 
    }
};

// ----------------------------------------------------------------------
// HÀM TIỆN ÍCH AUTH
// ----------------------------------------------------------------------
export const logoutUser = () => {
    return signOut(auth);
};