// src/utils/format.js

export const formatCurrency = (amount) => {
    if (amount === undefined || amount === null) {
        return 'N/A';
    }
    return new Intl.NumberFormat('vi-VN', {
        style: 'currency',
        currency: 'VND',
        minimumFractionDigits: 0,
    }).format(amount);
};