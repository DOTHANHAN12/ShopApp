package com.example.shopapp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Dữ liệu địa giới hành chính Việt Nam
 * Bao gồm: 63 Tỉnh/Thành phố
 */
public class VietnamLocationData {

    // Danh sách 63 tỉnh/thành phố Việt Nam
    private static final String[] PROVINCES = {
            "Hà Nội",
            "TP Hồ Chí Minh",
            "Đà Nẵng",
            "Hải Phòng",
            "Cần Thơ",
            "An Giang",
            "Bà Rịa - Vũng Tàu",
            "Bắc Giang",
            "Bắc Kạn",
            "Bạc Liêu",
            "Bắc Ninh",
            "Bến Tre",
            "Bình Định",
            "Bình Dương",
            "Bình Phước",
            "Bình Thuận",
            "Cà Mau",
            "Cao Bằng",
            "Đắk Lắk",
            "Đắk Nông",
            "Điện Biên",
            "Đồng Nai",
            "Đồng Tháp",
            "Gia Lai",
            "Hà Giang",
            "Hà Nam",
            "Hà Tĩnh",
            "Hải Dương",
            "Hậu Giang",
            "Hòa Bình",
            "Hưng Yên",
            "Khánh Hòa",
            "Kiên Giang",
            "Kon Tum",
            "Lai Châu",
            "Lâm Đồng",
            "Lạng Sơn",
            "Lào Cai",
            "Long An",
            "Nam Định",
            "Nghệ An",
            "Ninh Bình",
            "Ninh Thuận",
            "Phú Thọ",
            "Phú Yên",
            "Quảng Bình",
            "Quảng Nam",
            "Quảng Ngãi",
            "Quảng Ninh",
            "Quảng Trị",
            "Sóc Trăng",
            "Sơn La",
            "Tây Ninh",
            "Thái Bình",
            "Thái Nguyên",
            "Thanh Hóa",
            "Thừa Thiên Huế",
            "Tiền Giang",
            "Trà Vinh",
            "Tuyên Quang",
            "Vĩnh Long",
            "Vĩnh Phúc",
            "Yên Bái"
    };

    // Danh sách quận/huyện theo tỉnh/thành phố (Mẫu cho các thành phố lớn)
    private static final Map<String, String[]> DISTRICTS = new HashMap<String, String[]>() {{
        // Hà Nội
        put("Hà Nội", new String[]{
                "Ba Đình", "Hoàn Kiếm", "Tây Hồ", "Long Biên", "Cầu Giấy",
                "Đống Đa", "Hai Bà Trưng", "Hoàng Mai", "Thanh Xuân", "Sóc Sơn",
                "Đông Anh", "Gia Lâm", "Nam Từ Liêm", "Thanh Trì", "Bắc Từ Liêm",
                "Mê Linh", "Hà Đông", "Sơn Tây", "Ba Vì", "Phúc Thọ",
                "Đan Phượng", "Hoài Đức", "Quốc Oai", "Thạch Thất", "Chương Mỹ",
                "Thanh Oai", "Thường Tín", "Phú Xuyên", "Ứng Hòa", "Mỹ Đức"
        });

        // TP Hồ Chí Minh
        put("TP Hồ Chí Minh", new String[]{
                "Quận 1", "Quận 2", "Quận 3", "Quận 4", "Quận 5",
                "Quận 6", "Quận 7", "Quận 8", "Quận 9", "Quận 10",
                "Quận 11", "Quận 12", "Quận Thủ Đức", "Quận Gò Vấp", "Quận Bình Thạnh",
                "Quận Tân Bình", "Quận Tân Phú", "Quận Phú Nhuận", "Quận Bình Tân",
                "Huyện Củ Chi", "Huyện Hóc Môn", "Huyện Bình Chánh", "Huyện Nhà Bè", "Huyện Cần Giờ"
        });

        // Đà Nẵng
        put("Đà Nẵng", new String[]{
                "Hải Châu", "Thanh Khê", "Sơn Trà", "Ngũ Hành Sơn", "Liên Chiểu",
                "Cẩm Lệ", "Hòa Vang", "Hoàng Sa"
        });

        // Hải Phòng
        put("Hải Phòng", new String[]{
                "Hồng Bàng", "Ngô Quyền", "Lê Chân", "Hải An", "Kiến An",
                "Đồ Sơn", "Dương Kinh", "Thuỷ Nguyên", "An Dương", "An Lão",
                "Kiến Thuỵ", "Tiên Lãng", "Vĩnh Bảo", "Cát Hải", "Bạch Long Vĩ"
        });

        // Cần Thơ
        put("Cần Thơ", new String[]{
                "Ninh Kiều", "Ô Môn", "Bình Thuỷ", "Cái Răng", "Thốt Nốt",
                "Vĩnh Thạnh", "Cờ Đỏ", "Phong Điền", "Thới Lai"
        });

        // Bắc Ninh
        put("Bắc Ninh", new String[]{
                "Thành phố Bắc Ninh", "Từ Sơn", "Thuận Thành", "Gia Bình", "Lương Tài",
                "Quế Võ", "Tiên Du", "Yên Phong"
        });

        // Bình Dương
        put("Bình Dương", new String[]{
                "Thủ Dầu Một", "Thuận An", "Dĩ An", "Tân Uyên", "Bến Cát",
                "Bàu Bàng", "Dầu Tiếng", "Phú Giáo", "Bắc Tân Uyên"
        });

        // Đồng Nai
        put("Đồng Nai", new String[]{
                "Biên Hòa", "Long Khánh", "Nhơn Trạch", "Vĩnh Cửu", "Trảng Bom",
                "Thống Nhất", "Cẩm Mỹ", "Long Thành", "Xuân Lộc", "Tân Phú", "Định Quán"
        });

        // Khánh Hòa
        put("Khánh Hòa", new String[]{
                "Nha Trang", "Cam Ranh", "Cam Lâm", "Vạn Ninh", "Ninh Hòa",
                "Khánh Vĩnh", "Diên Khánh", "Khánh Sơn", "Trường Sa"
        });

        // Thêm các tỉnh khác với dữ liệu đơn giản hơn
        put("An Giang", new String[]{"Long Xuyên", "Châu Đốc", "An Phú", "Tân Châu", "Phú Tân", "Châu Phú", "Tịnh Biên", "Tri Tôn", "Châu Thành", "Chợ Mới", "Thoại Sơn"});
        put("Bà Rịa - Vũng Tàu", new String[]{"Vũng Tàu", "Bà Rịa", "Châu Đức", "Xuyên Mộc", "Long Điền", "Đất Đỏ", "Côn Đảo", "Tân Thành"});
        put("Bắc Giang", new String[]{"Bắc Giang", "Hiệp Hòa", "Lạng Giang", "Lục Nam", "Lục Ngạn", "Sơn Động", "Tân Yên", "Việt Yên", "Yên Dũng", "Yên Thế"});
    }};

    /**
     * Lấy danh sách tỉnh/thành phố
     */
    public static List<String> getProvinces() {
        return new ArrayList<>(Arrays.asList(PROVINCES));
    }

    /**
     * Lấy danh sách quận/huyện theo tỉnh
     */
    public static List<String> getDistricts(String province) {
        if (DISTRICTS.containsKey(province)) {
            return new ArrayList<>(Arrays.asList(DISTRICTS.get(province)));
        }
        // Trả về danh sách rỗng nếu chưa có dữ liệu
        return new ArrayList<>();
    }

    /**
     * Kiểm tra tỉnh có dữ liệu quận/huyện chưa
     */
    public static boolean hasDistrictData(String province) {
        return DISTRICTS.containsKey(province);
    }

    /**
     * Format địa chỉ đầy đủ
     */
    public static String formatFullAddress(String province, String district, String ward, String street) {
        StringBuilder address = new StringBuilder();

        if (street != null && !street.trim().isEmpty()) {
            address.append(street.trim());
        }

        if (ward != null && !ward.trim().isEmpty()) {
            if (address.length() > 0) address.append(", ");
            address.append(ward.trim());
        }

        if (district != null && !district.trim().isEmpty()) {
            if (address.length() > 0) address.append(", ");
            address.append(district.trim());
        }

        if (province != null && !province.trim().isEmpty()) {
            if (address.length() > 0) address.append(", ");
            address.append(province.trim());
        }

        return address.toString();
    }
}