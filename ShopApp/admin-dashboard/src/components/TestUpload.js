// src/components/TestUpload.js
import React, { useState, useCallback } from 'react';
import { uploadFile, deleteFile } from '../firebaseConfig'; // Import hàm tiện ích

// Sử dụng Inline SVG (từ file gốc của bạn)
const Icon = ({ children, className = '' }) => <span className={`inline-flex items-center justify-center ${className}`}>{children}</span>;
const Cloud = (props) => <Icon {...props}><svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M17.5 19H17a4.5 4.5 0 0 0 0-9h-.5c-.6-1.5-2-2.5-3.5-2.5-2.2 0-4 1.8-4 4h-.2a2.5 2.5 0 0 0 0 5h2.2"></path><path d="M12 10v9"></path><path d="m16 16-4-4-4 4"></path></svg></Icon>;
const AlertTriangle = (props) => <Icon {...props}><svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="m21.73 18-9.08-15.75A1.99 1.99 0 0 0 10.92 2.25l-9.08 15.75A1.99 1.99 0 0 0 2.65 20h18.7c.71 0 1.34-.49 1.5-1.25z"/><path d="M12 9v4"/><path d="M12 17h.01"/></svg></Icon>;
const Loader2 = (props) => <Icon {...props}><svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M21 12a9 9 0 1 1-6.219-8.56"/></svg></Icon>;
const Upload = (props) => <Icon {...props}><svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="17 8 12 3 7 8"/><line x1="12" x2="12" y1="3" y2="15"/></svg></Icon>;
const LinkIcon = (props) => <Icon {...props}><svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71"/><path d="M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71"/></svg></Icon>;
const X = (props) => <Icon {...props}><svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M18 6 6 18"/><path d="m6 6 12 12"/></svg></Icon>;
const DeleteIcon = (props) => <Icon {...props}><svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M3 6h18"/><path d="M19 6v14c0 1-1 2-2 2H7c-1 0-2-1-2-2V6"/><path d="M8 6V4c0-1 1-2 2-2h4c1 0 2 1 2 2v2"/></svg></Icon>;
// End of Icon definitions

const generateTempId = () => `test_upload_${Date.now()}`;

const TestUpload = () => {
    const [status, setStatus] = useState('Sẵn sàng tải lên.');
    const [loading, setLoading] = useState(false);
    const [log, setLog] = useState('');
    const [uploadedUrl, setUploadedUrl] = useState('');
    const [testProductId] = useState(generateTempId()); 

    const appendLog = useCallback((message, isError = false) => {
        setLog(prev => `${prev}\n[${new Date().toLocaleTimeString('vi-VN')}] ${isError ? '❌ LỖI: ' : '✅ INFO: '}${message}`);
    }, []);

    const handleFileUpload = async (e) => {
        const file = e.target.files[0];
        if (!file) return;

        // Xóa URL cũ trước khi upload mới (để tránh rác)
        if (uploadedUrl) {
            await deleteFile(uploadedUrl);
            setUploadedUrl('');
        }

        const path = `test_images/${testProductId}/${file.name}`; // Đường dẫn đơn giản hơn
        
        setLoading(true);
        setStatus('Đang tải lên...');
        appendLog(`Bắt đầu upload file: ${file.name} tới đường dẫn: ${path}`);
        
        try {
            // Gọi hàm tiện ích đã được kiểm tra CORS OK
            const url = await uploadFile(file, path); 
            
            setStatus('Tải lên THÀNH CÔNG! ✅');
            setUploadedUrl(url);
            appendLog(`THÀNH CÔNG. URL: ${url}`);

        } catch (error) {
            setStatus('Tải lên THẤT BẠI! ❌');
            let errorMessage = "Lỗi không xác định.";

            if (error.code) {
                errorMessage = `FIREBASE ERROR CODE: ${error.code} - ${error.message}`;
            } else if (error.message) {
                errorMessage = error.message; 
            }

            appendLog(`THẤT BẠI: ${errorMessage}`, true);
            console.error("LỖI UPLOAD CHI TIẾT:", error);
            alert(`Lỗi: ${errorMessage}`); // Alert cho người dùng dễ thấy
        } finally {
            setLoading(false);
            e.target.value = null; // Reset input file
        }
    };
    
    const handleDelete = async () => {
        if (!uploadedUrl) return;

        setLoading(true);
        setStatus('Đang xóa file...');
        try {
            const success = await deleteFile(uploadedUrl);
            if (success) {
                setStatus('Xóa file THÀNH CÔNG!🗑️');
                setUploadedUrl('');
            } else {
                setStatus('Xóa file THẤT BẠI. Kiểm tra log.');
            }
        } catch (error) {
            setStatus('Xóa file THẤT BẠI. Kiểm tra log.');
            appendLog(`Lỗi xóa: ${error.message}`, true);
        } finally {
            setLoading(false);
        }
    };

    // Tailwind-like styles
    const uploadBoxClasses = `
        border-2 border-dashed rounded-xl p-8 transition duration-300 ease-in-out 
        text-center cursor-pointer mb-6 text-gray-700 
        ${loading ? 'bg-gray-100 border-gray-400 cursor-not-allowed' : 'bg-red-50 border-red-700 hover:bg-red-100'}
        flex flex-col items-center justify-center space-y-3
    `;
    const buttonBaseClasses = 'px-5 py-2 rounded-lg font-semibold transition duration-200 shadow-md';

    return (
        <div className="max-w-xl mx-auto bg-white shadow-lg rounded-2xl p-8 border border-gray-100">
            <h2 className="text-3xl font-extrabold text-gray-900 mb-6 flex items-center">
                <Cloud className="w-7 h-7 mr-2 text-red-600" /> Test Upload (Không CORS)
            </h2>
            <p className="text-sm text-gray-500 mb-6">
                Test upload bằng phương thức **`uploadBytes`** an toàn.
            </p>

            {/* Upload Area */}
            <label htmlFor="testFileUpload" className={uploadBoxClasses}>
                <input 
                    type="file" 
                    accept="image/*" 
                    onChange={handleFileUpload} 
                    style={{ display: 'none' }}
                    id="testFileUpload"
                    disabled={loading}
                />
                {loading ? (
                    <>
                        <Loader2 className="w-8 h-8 text-gray-600 animate-spin" />
                        <span className="text-lg font-medium text-gray-600">ĐANG TẢI... Vui lòng đợi</span>
                    </>
                ) : (
                    <>
                        <Upload className="w-8 h-8 text-red-600" />
                        <span className="text-lg font-medium text-red-700">Click hoặc Kéo thả file ảnh để TEST</span>
                        <span className="text-sm text-gray-500">(Chỉ chấp nhận file ảnh)</span>
                    </>
                )}
            </label>

            {/* Status and Uploaded URL */}
            <p className={`text-center text-xl font-bold mb-4 ${status.includes('THÀNH CÔNG') ? 'text-green-600' : status.includes('THẤT BẠI') ? 'text-red-600' : 'text-gray-700'}`}>
                {status}
            </p>

            {uploadedUrl && (
                <div className="mb-6 p-4 border border-green-200 bg-green-50 rounded-lg flex flex-col sm:flex-row items-center justify-between">
                    <div className="flex-1 min-w-0">
                        <p className="font-semibold text-green-700 mb-1 flex items-center">
                            <LinkIcon className="w-4 h-4 mr-1" /> Download URL
                        </p>
                        <a 
                            href={uploadedUrl} 
                            target="_blank" 
                            rel="noopener noreferrer" 
                            className="text-sm text-green-800 underline break-all hover:text-green-600 transition"
                        >
                            {uploadedUrl.substring(0, 50)}...
                        </a>
                    </div>
                    <button 
                        onClick={handleDelete} 
                        disabled={loading}
                        className={`${buttonBaseClasses} mt-3 sm:mt-0 sm:ml-4 bg-red-500 hover:bg-red-700 text-white flex items-center justify-center`}
                    >
                        <DeleteIcon className="w-4 h-4 mr-1" /> Xóa File
                    </button>
                </div>
            )}
            
            {/* Log Console */}
            <h3 className="text-xl font-semibold mt-8 mb-3 text-gray-800">Log (Kết quả)</h3>
            <pre className="bg-gray-900 text-white text-xs p-4 rounded-lg overflow-x-auto h-48 whitespace-pre-wrap">
                {log || 'Chưa có hành động nào...'}
            </pre>
            
            <p className="text-sm text-red-500 mt-4 flex items-start">
                <AlertTriangle className="w-4 h-4 mr-1 mt-0.5 flex-shrink-0" /> 
                Nếu thất bại (kể cả không có CORS), lỗi phổ biến nhất là **Firebase Storage Rules** chưa cho phép người dùng hiện tại (chưa đăng nhập hoặc quy tắc quá chặt) được ghi (write).
            </p>
        </div>
    );
};

export default TestUpload;