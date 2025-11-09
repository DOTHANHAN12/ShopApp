// ============================================================================
// BarcodeInsertPanel.js - FIXED VERSION
// File: src/components/BarcodeInsertPanel.js
// ============================================================================

import React, { useState } from 'react';
import * as barcodeUtils from '../utils/barcodeUtils';  // ← FIXED: import *

const BarcodeInsertPanel = () => {
    const [loading, setLoading] = useState(false);
    const [result, setResult] = useState(null);
    const [message, setMessage] = useState('');

    const styles = {
        container: {
            backgroundColor: '#1A1A1A',
            border: '2px solid #C40000',
            borderRadius: '8px',
            padding: '20px',
            marginBottom: '20px',
            color: '#E0E0E0'
        },
        title: {
            fontSize: '18px',
            fontWeight: 'bold',
            color: '#C40000',
            marginBottom: '15px',
            display: 'flex',
            alignItems: 'center',
            gap: '10px'
        },
        buttonGroup: {
            display: 'flex',
            gap: '10px',
            flexWrap: 'wrap',
            marginBottom: '15px'
        },
        button: {
            padding: '10px 15px',
            border: 'none',
            borderRadius: '4px',
            cursor: 'pointer',
            fontWeight: 'bold',
            transition: 'all 0.2s',
            fontSize: '14px'
        },
        buttonPrimary: {
            backgroundColor: '#007bff',
            color: 'white'
        },
        buttonSuccess: {
            backgroundColor: '#28a745',
            color: 'white'
        },
        buttonDanger: {
            backgroundColor: '#dc3545',
            color: 'white'
        },
        buttonDisabled: {
            opacity: 0.5,
            cursor: 'not-allowed'
        },
        resultBox: {
            backgroundColor: '#292929',
            border: '1px solid #444',
            borderRadius: '4px',
            padding: '15px',
            marginTop: '15px',
            maxHeight: '300px',
            overflowY: 'auto',
            fontFamily: 'monospace',
            fontSize: '12px'
        },
        resultItem: {
            padding: '5px 0',
            borderBottom: '1px solid #444'
        },
        success: {
            color: '#00FF00'
        },
        error: {
            color: '#FF4D4D'
        },
        warning: {
            color: '#FFA500'
        },
        info: {
            color: '#00BFFF'
        }
    };

    // ========== HANDLE INSERT BARCODES ==========
    const handleInsertBarcodes = async () => {
        const confirm = window.confirm(
            '⚠️ WARNING! Này sẽ tạo barcode cho TẤT CẢ sản phẩm chưa có barcode.\n\n' +
            'Các sản phẩm đã có barcode sẽ KHÔNG bị thay đổi.\n\n' +
            'Bạn chắc chắn không?'
        );

        if (!confirm) return;

        setLoading(true);
        setMessage('Đang xử lý... Vui lòng chờ');
        setResult(null);

        try {
            console.log('🔄 Calling insertBarcodesForAllProducts...');
            
            // ← FIXED: Gọi function đúng cách
            const res = await barcodeUtils.insertBarcodesForAllProducts();
            
            console.log('✅ Result:', res);
            setResult(res);

            if (res.success) {
                setMessage(`✅ Hoàn tất! Cập nhật: ${res.updated} | Bỏ qua: ${res.skipped} | Lỗi: ${res.failed}`);
            } else {
                setMessage('❌ Có lỗi xảy ra!');
            }
        } catch (err) {
            console.error('❌ Error:', err);
            setMessage(`❌ Lỗi: ${err.message}`);
        } finally {
            setLoading(false);
        }
    };

    // ========== HANDLE DELETE ALL BARCODES ==========
    const handleDeleteAllBarcodes = async () => {
        const confirm = window.confirm(
            '🔴 DANGER! Này sẽ XÓA TẤT CẢ barcode từ database!\n\n' +
            'Action này không thể undo!\n\n' +
            'Bạn CHẮC CHẮN không?'
        );

        if (!confirm) {
            const confirm2 = window.confirm('Bạn chắc chắn lần 2?');
            if (!confirm2) return;
        }

        setLoading(true);
        setMessage('Đang xóa... Vui lòng chờ');
        setResult(null);

        try {
            // ← FIXED: Gọi function đúng cách
            const res = await barcodeUtils.deleteAllBarcodes();
            
            setResult(res);

            if (res.success) {
                setMessage(`✅ Đã xóa barcode từ ${res.deleted} sản phẩm`);
            } else {
                setMessage('❌ Có lỗi xảy ra!');
            }
        } catch (err) {
            console.error('❌ Error:', err);
            setMessage(`❌ Lỗi: ${err.message}`);
        } finally {
            setLoading(false);
        }
    };

    // ========== HANDLE EXPORT BARCODES ==========
    const handleExportBarcodes = async () => {
        setLoading(true);
        setMessage('Đang xuất dữ liệu...');

        try {
            // ← FIXED: Gọi function đúng cách
            const data = await barcodeUtils.exportProductsWithBarcode();

            if (data.length === 0) {
                setMessage('⚠️ Không có dữ liệu để xuất');
                setLoading(false);
                return;
            }

            // Tạo CSV
            const csv = [
                ['ID Sản Phẩm', 'Tên Sản Phẩm', 'Barcode'].join(','),
                ...data.map(item => 
                    `"${item.id}","${(item.name || '').replace(/"/g, '""')}","${item.barcode}"`
                )
            ].join('\n');

            // Download CSV
            const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
            const link = document.createElement('a');
            const url = URL.createObjectURL(blob);
            link.setAttribute('href', url);
            link.setAttribute('download', `barcodes_${new Date().getTime()}.csv`);
            link.style.visibility = 'hidden';
            document.body.appendChild(link);
            link.click();
            document.body.removeChild(link);

            setMessage(`✅ Đã export ${data.length} sản phẩm. File: barcodes_*.csv`);
            setResult({ success: true, exported: data.length });

        } catch (err) {
            console.error('❌ Error:', err);
            setMessage(`❌ Lỗi: ${err.message}`);
        } finally {
            setLoading(false);
        }
    };

    // ========== RENDER RESULT ==========
    const renderResult = () => {
        if (!result) return null;

        return (
            <div style={styles.resultBox}>
                <div style={{ ...styles.resultItem, ...styles.info }}>
                    📊 RESULT SUMMARY
                </div>
                {result.total !== undefined && (
                    <div style={{ ...styles.resultItem, ...styles.info }}>
                        Total: {result.total}
                    </div>
                )}
                {result.updated !== undefined && (
                    <div style={{ ...styles.resultItem, ...styles.success }}>
                        ✅ Updated: {result.updated}
                    </div>
                )}
                {result.skipped !== undefined && (
                    <div style={{ ...styles.resultItem, ...styles.warning }}>
                        ⏭️  Skipped: {result.skipped}
                    </div>
                )}
                {result.failed !== undefined && (
                    <div style={{ ...styles.resultItem, ...styles.error }}>
                        ❌ Failed: {result.failed}
                    </div>
                )}
                {result.deleted !== undefined && (
                    <div style={{ ...styles.resultItem, ...styles.success }}>
                        🗑️  Deleted: {result.deleted}
                    </div>
                )}
                {result.exported !== undefined && (
                    <div style={{ ...styles.resultItem, ...styles.success }}>
                        📥 Exported: {result.exported}
                    </div>
                )}
                {result.errors && result.errors.length > 0 && (
                    <>
                        <div style={{ ...styles.resultItem, ...styles.error }}>
                            Errors: {result.errors.length}
                        </div>
                        {result.errors.map((err, idx) => (
                            <div key={idx} style={{ ...styles.resultItem, ...styles.error, fontSize: '11px' }}>
                                {err.productId || 'Global'}: {err.error}
                            </div>
                        ))}
                    </>
                )}
            </div>
        );
    };

    return (
        <div style={styles.container}>
            <div style={styles.title}>
                🏷️  BARCODE INSERT PANEL
            </div>

            <p style={{ color: '#888', marginBottom: '15px', fontSize: '13px' }}>
                Quản lý barcode cho tất cả sản phẩm. Cảnh báo: Một số action không thể undo!
            </p>

            <div style={styles.buttonGroup}>
                <button
                    style={{
                        ...styles.button,
                        ...styles.buttonPrimary,
                        ...(loading ? styles.buttonDisabled : {})
                    }}
                    onClick={handleInsertBarcodes}
                    disabled={loading}
                    title="Tạo barcode cho tất cả sản phẩm chưa có barcode"
                >
                    {loading ? '⏳ Processing...' : '➕ INSERT BARCODES'}
                </button>

                <button
                    style={{
                        ...styles.button,
                        ...styles.buttonSuccess,
                        ...(loading ? styles.buttonDisabled : {})
                    }}
                    onClick={handleExportBarcodes}
                    disabled={loading}
                    title="Xuất danh sách barcode sang CSV"
                >
                    {loading ? '⏳ Processing...' : '📥 EXPORT CSV'}
                </button>

                <button
                    style={{
                        ...styles.button,
                        ...styles.buttonDanger,
                        ...(loading ? styles.buttonDisabled : {})
                    }}
                    onClick={handleDeleteAllBarcodes}
                    disabled={loading}
                    title="XÓA TẤT CẢ barcode - Action này không thể undo!"
                >
                    {loading ? '⏳ Processing...' : '🗑️  DELETE ALL'}
                </button>
            </div>

            {message && (
                <div
                    style={{
                        ...styles.resultBox,
                        marginTop: '10px',
                        maxHeight: 'auto',
                        overflowY: 'visible'
                    }}
                >
                    <div
                        style={{
                            color: message.includes('❌') ? '#FF4D4D' : 
                                   message.includes('⚠️') ? '#FFA500' : '#00FF00',
                            fontSize: '13px',
                            wordBreak: 'break-word'
                        }}
                    >
                        {message}
                    </div>
                </div>
            )}

            {renderResult()}

            <div style={{ marginTop: '15px', color: '#888', fontSize: '12px' }}>
                <strong>💡 Notes:</strong>
                <ul style={{ marginTop: '5px' }}>
                    <li>INSERT: Tạo barcode EAN-13 tự động cho sản phẩm chưa có</li>
                    <li>EXPORT: Xuất danh sách barcode dạng CSV</li>
                    <li>DELETE: Xóa tất cả barcode (cần confirm 2 lần)</li>
                </ul>
            </div>
        </div>
    );
};

export default BarcodeInsertPanel;