package com.example.shopapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.Locale;

public class MainActivity2 extends AppCompatActivity {

    private static final String TAG = "FirestoreTest";
    private static final String PRODUCTS_COLLECTION = "products";
    private static final String USERS_COLLECTION = "users";
    private static final String ORDERS_COLLECTION = "orders";

    private FirebaseFirestore db;
    private Button btnWrite, btnRead;
    private TextView tvOutput;

    private String[] productNames = {"Airism T-Shirt", "Blocktech Parka", "Dry-Ex Polo Shirt", "Heattech Innerwear",
            "Wide Fit Jeans", "Linen Blend Dress", "Cashmere Sweater", "Flannel Shirt",
            "Jogger Pants", "Kando Jacket"};
    private Random random = new Random();

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        db = FirebaseFirestore.getInstance();
        btnWrite = findViewById(R.id.btnWrite);
        btnRead = findViewById(R.id.btnRead);
        tvOutput = findViewById(R.id.tvOutput);

        btnWrite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startWriteProcess();
            }
        });

        btnRead.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                readSampleData();
            }
        });
    }

    // ---------------------------------------------------------------------
    // HÀM CHÍNH GHI TẤT CẢ DỮ LIỆU MẪU
    // ---------------------------------------------------------------------

    private void startWriteProcess() {
        writeSampleProductData();
        writeSampleUserData();
    }

    // ---------------------------------------------------------------------
    // PHẦN 1: GHI DỮ LIỆU SẢN PHẨM (PRODUCTS)
    // ---------------------------------------------------------------------
    private void writeSampleProductData() {
        WriteBatch batch = db.batch();
        List<Product> products;

        try {
            products = createProductData();
        } catch (Exception e) {
            Log.e(TAG, "LỖI FATAL: Không thể tạo danh sách sản phẩm mẫu.", e);
            Toast.makeText(this, "❌ Lỗi tạo dữ liệu sản phẩm (Xem Logcat)", Toast.LENGTH_LONG).show();
            return;
        }


        tvOutput.setText("Đang ghi 50 sản phẩm...");

        for (Product product : products) {
            String docId = product.getCategory().toLowerCase(Locale.ROOT) + "_"
                    + product.getType().toLowerCase(Locale.ROOT).replace(" ", "_") + "_"
                    + UUID.randomUUID().toString().substring(0, 8);

            product.setProductId(docId);

            batch.set(db.collection(PRODUCTS_COLLECTION).document(docId), product);
        }

        batch.commit().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Log.d(TAG, "✅ Ghi 50 sản phẩm thành công!");
                Toast.makeText(this, "✅ Ghi 50 sản phẩm thành công!", Toast.LENGTH_LONG).show();
            } else {
                Log.e(TAG, "❌ LỖI FIREBASE BATCH WRITE (PRODUCTS):", task.getException());
                Toast.makeText(this, "❌ Lỗi ghi dữ liệu!", Toast.LENGTH_LONG).show();
            }
        });
    }

    private List<Product> createProductData() {
        List<Product> list = new ArrayList<>();
        String[] categories = {"WOMEN", "MEN", "KIDS", "BABY"};
        String[] types = {"OUTERWEAR", "SWEATERS & KNITWEAR", "BOTTOMS", "T-SHIRTS, SWEAT & FLEECE", "INNERWEAR & UNDERWEAR", "ACCESSORIES", "DRESSES"};
        double[] prices = {399000.0, 499000.0, 599000.0, 784000.0, 980000.0};
        String[] sizes = {"XS", "S", "M", "L", "XL", "XXL"};
        String[] colors = {"Black", "Navy", "White", "Gray"};

        long now = System.currentTimeMillis();
        long oneDay = 24 * 60 * 60 * 1000;

        for (int i = 0; i < 50; i++) {
            String category = categories[random.nextInt(categories.length)];
            String type = types[random.nextInt(types.length)];
            double basePrice = prices[random.nextInt(prices.length)];

            boolean isFeatured = random.nextBoolean();

            String name = productNames[random.nextInt(productNames.length)] + " - " + type;
            String desc = "Chất liệu cao cấp, mang lại cảm giác thoải mái và bền bỉ.";
            String status = "Active";

            // Offer
            boolean isOfferActive = random.nextBoolean();
            OfferDetails offer = null;
            double currentPriceForVariant = basePrice;

            if (isOfferActive) {
                Long discountPercent = (long) (random.nextInt(4) + 1) * 10;
                Long endDate = now + (long) (random.nextInt(30) + 7) * oneDay;

                offer = new OfferDetails(discountPercent, now - oneDay, endDate);

                currentPriceForVariant = basePrice * (1.0 - offer.getDiscountPercent() / 100.0);
            }

            // Images
            List<String> productColors = Arrays.asList(colors).subList(0, random.nextInt(4) + 1);
            String mainImage = "https://storage.firebase.com/v0/b/shopapp-demo.appspot.com/o/main_" + i + ".jpg?alt=media";

            Map<String, List<String>> colorImages = new HashMap<>();
            for (String color : productColors) {
                List<String> detailUrls = new ArrayList<>();
                String colorSlug = color.toLowerCase(Locale.ROOT);
                for(int k=1; k<=5; k++) {
                    detailUrls.add("https://storage.firebase.com/v0/b/shopapp-demo.appspot.com/o/detail_" + colorSlug + "_" + k + "_" + i + ".jpg?alt=media");
                }
                colorImages.put(color, detailUrls);
            }

            // Variants (Size x Color)
            List<ProductVariant> variants = new ArrayList<>();
            for(String color : productColors) {
                List<String> sizesForColor = Arrays.asList(sizes).subList(random.nextInt(3), random.nextInt(3) + 3);

                for(String size : sizesForColor) {
                    long varQuantity = (long) (random.nextInt(50) + 10);
                    String varId = "SKU-" + color.substring(0, 2).toUpperCase(Locale.ROOT) + "-" + size + "-" + i;

                    variants.add(new ProductVariant(varId, size, color, varQuantity, currentPriceForVariant));
                }
            }

            // Rating
            double rating = Math.round((random.nextDouble() * 2 + 3.0) * 10.0) / 10.0;
            long totalReviews = (long) (random.nextInt(200) + 50);

            // Khởi tạo và gán giá trị
            Product p = new Product();
            p.setProductId(null);
            p.setName(name);
            p.setDesc(desc);
            p.setBasePrice(basePrice);
            p.setMainImage(mainImage);
            p.setCategory(category);
            p.setType(type);
            p.setStatus(status);
            p.setIsOfferStatus(isOfferActive);
            p.setOffer(offer);
            p.setVariants(variants);
            p.setAverageRating(rating);
            p.setTotalReviews(totalReviews);
            p.setFeatured(isFeatured);
            p.setColorImages(colorImages);
            p.setCreatedAt(now);
            p.setUpdatedAt(now);

            list.add(p);
        }
        return list;
    }

    // ---------------------------------------------------------------------
    // PHẦN 2: GHI DỮ LIỆU NGƯỜI DÙNG, GIỎ HÀNG, ĐƠN HÀNG
    // ---------------------------------------------------------------------

    private void writeSampleUserData() {
        WriteBatch batch = db.batch();
        long now = System.currentTimeMillis();
        String demoUserId = "DEMO_USER_UID";
        String dummyProductId = "men_t-shirts_demo01";
        long oneDay = 24 * 60 * 60 * 1000;

        // --- 1. Tạo User Mẫu (Đã sửa lỗi constructor và thêm gender) ---
        Map<String, String> addressMap = new HashMap<>();
        addressMap.put("street", "123 Pham Van Dong St.");
        addressMap.put("city", "Hanoi");
        addressMap.put("zipCode", "10000");

        // CẦN SỬ DỤNG CONSTRUCTOR MỚI CÓ GENDER
        User demoUser = new User("Demo User", "demo@shop.com", "0987654321", now, "Male", addressMap);
        batch.set(db.collection(USERS_COLLECTION).document(demoUserId), demoUser);

        // --- 2. Tạo Giỏ hàng Mẫu (Subcollection) ---
        CartItem item1 = new CartItem(dummyProductId, "SKU-BL-M-22", 2, 499000.0, now);
        CartItem item2 = new CartItem("women_dress_demo02", "SKU-RE-S-10", 1, 784000.0, now + 1000);

        batch.set(db.collection(USERS_COLLECTION).document(demoUserId).collection("cart").document("item_01"), item1);
        batch.set(db.collection(USERS_COLLECTION).document(demoUserId).collection("cart").document("item_02"), item2);

        // --- 3. Tạo Yêu thích Mẫu (Subcollection) ---
        FavoriteItem fav1 = new FavoriteItem(now);
        batch.set(db.collection(USERS_COLLECTION).document(demoUserId).collection("favorites").document(dummyProductId), fav1);

        // --- 4. Tạo Đơn hàng Mẫu (ĐÃ SỬA LỖI: Chuyển sang Setters) ---
        Map<String, Object> orderItem1 = new HashMap<>();
        orderItem1.put("productId", dummyProductId);
        orderItem1.put("name", "Demo T-Shirt");
        orderItem1.put("price", 499000.0);
        orderItem1.put("quantity", 2);

        Order order = new Order();
        order.setOrderId("ORD-" + UUID.randomUUID().toString().substring(0, 6));
        order.setUserId(demoUserId);
        order.setTotalAmount(1283000.0);
        order.setStatus("Shipped");
        order.setCreatedAt(now - oneDay);
        order.setShippingAddress(addressMap);
        order.setItems(Arrays.asList(orderItem1)); // Sử dụng setter cho List<Map>

        batch.set(db.collection(ORDERS_COLLECTION).document(order.getOrderId()), order);


        batch.commit().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(this, "✅ Ghi dữ liệu User/Cart/Order thành công!", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "❌ Lỗi ghi dữ liệu User/Cart/Order!", Toast.LENGTH_LONG).show();
                Log.e(TAG, "❌ LỖI FIREBASE BATCH WRITE (USERS/ORDERS):", task.getException());
            }
        });
    }

    private void writeAllSampleData() {
        writeSampleProductData();
        writeSampleUserData();
    }

    // ---------------------------------------------------------------------
    // HÀM ĐỌC DỮ LIỆU (Giữ nguyên logic đọc cũ)
    // ---------------------------------------------------------------------

    /** Hàm đọc dữ liệu từ collection 'products' */
    private void readSampleData() {
        tvOutput.setText("Đang đọc dữ liệu từ collection '" + PRODUCTS_COLLECTION + "'...");

        db.collection(PRODUCTS_COLLECTION)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            StringBuilder dataBuilder = new StringBuilder();
                            dataBuilder.append("📂 Dữ liệu trong collection '").append(PRODUCTS_COLLECTION).append("':\n\n");
                            int count = 0;

                            for (QueryDocumentSnapshot document : task.getResult()) {
                                count++;
                                Product p = document.toObject(Product.class);

                                // TÍNH GIÁ HIỂN THỊ (Display Price)
                                double displayPrice = p.getBasePrice();
                                String discountInfo = "Không KM";

                                if (p.getIsOfferStatus() && p.getOffer() != null) {
                                    Long percent = p.getOffer().getDiscountPercent();
                                    displayPrice = p.getBasePrice() * (1.0 - percent / 100.0);
                                    discountInfo = String.format("%d%% OFF (Còn %,.0f đ)", percent, displayPrice);
                                }

                                dataBuilder.append("--- Sản phẩm #").append(count).append(" ---\n")
                                        .append(" 🔑 Doc ID (Product ID): ").append(document.getId()).append("\n")
                                        .append(" 📝 Tên: ").append(p.getName()).append("\n")
                                        .append(" 🏷️ Giá Gốc: ").append(String.format("%,.0f đ", p.getBasePrice())).append("\n")
                                        .append(" 💥 KM (isOffer): ").append(p.getIsOfferStatus() ? "CÓ" : "KHÔNG").append("\n")
                                        .append(" 💰 Giá KM/Hiện tại: ").append(String.format("%,.0f đ", displayPrice)).append(" (").append(discountInfo).append(")\n")
                                        .append(" 🎨 Màu/Size Biến thể (").append(p.getVariants().size()).append("):\n");

                                // Hiển thị chi tiết từng biến thể
                                for (ProductVariant pv : p.getVariants()) {
                                    dataBuilder.append("     - [")
                                            .append(pv.color).append("/").append(pv.size)
                                            .append("] tồn: ").append(pv.quantity)
                                            .append(" | Giá Variant: ").append(String.format("%,.0f đ", pv.price))
                                            .append("\n");
                                }
                                dataBuilder.append("\n");
                            }

                            if (count == 0) {
                                tvOutput.setText("✅ Collection '" + PRODUCTS_COLLECTION + "' không có tài liệu nào.");
                            } else {
                                tvOutput.setText(dataBuilder.toString());
                                Toast.makeText(MainActivity2.this, "Đã đọc thành công " + count + " sản phẩm.", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Log.w(TAG, "❌ Lỗi đọc dữ liệu '" + PRODUCTS_COLLECTION + "': ", task.getException());
                            tvOutput.setText("❌ Lỗi đọc dữ liệu: " + task.getException().getMessage());
                        }
                    }
                });
    }
}