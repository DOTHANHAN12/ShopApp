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
import java.util.List;
import java.util.Random;
import java.util.UUID;

public class MainActivity2 extends AppCompatActivity {

    private FirebaseFirestore db;
    private Button btnWrite, btnRead;
    private TextView tvOutput;
    private static final String TAG = "FirestoreTest";
    private static final String PRODUCTS_COLLECTION = "products";

    // Danh sách tên sản phẩm mẫu
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
                write50SampleProducts();
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
    // HÀM GHI DỮ LIỆU SẢN PHẨM MỚI
    // ---------------------------------------------------------------------

    /** TẠO VÀ GHI 50 SẢN PHẨM KHÁC NUA VÀO COLLECTION 'products' */
    private void write50SampleProducts() {
        WriteBatch batch = db.batch();
        List<Product> products = create50Products();

        tvOutput.setText("Đang ghi 50 sản phẩm vào Firestore...");

        for (Product product : products) {
            // TẠM THỜI VẪN DÙNG CẤU TRÚC ID CŨ ĐỂ DỄ QUẢN LÝ KHI TEST
            String docId = product.category.toLowerCase() + "_"
                    + product.type.toLowerCase().replace(" ", "_") + "_"
                    + product.productId.substring(0, 8);

            // Tham chiếu và thêm vào batch
            batch.set(db.collection(PRODUCTS_COLLECTION).document(docId), product);
        }

        // Thực hiện batch write
        batch.commit().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(this, "✅ Ghi 50 sản phẩm thành công!", Toast.LENGTH_LONG).show();
                tvOutput.setText("✅ Ghi 50 sản phẩm thành công vào collection '" + PRODUCTS_COLLECTION + "'.");
            } else {
                Toast.makeText(this, "❌ Lỗi ghi dữ liệu!", Toast.LENGTH_LONG).show();
                tvOutput.setText("❌ Lỗi ghi dữ liệu: " + task.getException().getMessage());
                Log.e(TAG, "Lỗi ghi batch:", task.getException());
            }
        });
    }

    /** Hàm tạo danh sách 50 sản phẩm mẫu ngẫu nhiên */
    private List<Product> create50Products() {
        List<Product> list = new ArrayList<>();
        String[] categories = {"WOMEN", "MEN", "KIDS", "BABY"};
        String[] types = {"OUTERWEAR", "SWEATERS & KNITWEAR", "BOTTOMS", "T-SHIRTS, SWEAT & FLEECE", "INNERWEAR & UNDERWEAR", "ACCESSORIES", "DRESSES"};
        double[] prices = {399000.0, 499000.0, 599000.0, 784000.0, 980000.0};
        String[] sizes = {"S", "M", "L", "XL"};
        String[] colors = {"Black", "Navy", "White", "Gray", "Red", "Green", "Beige"};

        for (int i = 0; i < 50; i++) {
            String category = categories[random.nextInt(categories.length)];
            String type = types[random.nextInt(types.length)];
            double currentPrice = prices[random.nextInt(prices.length)];
            double originalPrice = currentPrice + 200000;
            boolean isOffer = random.nextBoolean();
            String name = productNames[random.nextInt(productNames.length)] + " - " + type;
            String desc = "Chất liệu cao cấp, mang lại cảm giác thoải mái và bền bỉ.";
            String status = "Active";

            // --- TẠO BIẾN THỂ (VARIANTS) ---
            List<ProductVariant> variants = new ArrayList<>();
            int numVariants = random.nextInt(3) + 2; // 2 đến 4 biến thể
            for(int j = 0; j < numVariants; j++) {
                String varSize = sizes[random.nextInt(sizes.length)];
                String varColor = colors[random.nextInt(colors.length)];
                long varQuantity = (long) (random.nextInt(50) + 10); // Tồn kho 10-60 cho mỗi biến thể
                String varId = "SKU-" + varSize + "-" + varColor.substring(0, 2).toUpperCase() + "-" + i + "-" + j;

                double variantPrice = currentPrice + (random.nextBoolean() ? 0 : random.nextInt(5) * 10000);

                variants.add(new ProductVariant(varId, varSize, varColor, varQuantity, variantPrice));
            }

            // --- TẠO RATING và ISFEATURED ---
            double rating = Math.round((random.nextDouble() * 2 + 3.0) * 10.0) / 10.0;
            long totalReviews = (long) (random.nextInt(200) + 50);
            boolean isFeatured = random.nextBoolean();

            // --- TẠO ĐỐI TƯỢNG ẢNH MỚI (ProductImageDetails) ---
            String typeSlug = type.toLowerCase().replace(" ", "-");
            String mainImageUrl = "https://storage.firebase.com/v0/b/shopapp-demo.appspot.com/o/products%2Fmain%2F" + typeSlug + "_" + i + ".jpg?alt=media";
            String subCategoryUrl = "https://storage.firebase.com/v0/b/shopapp-demo.appspot.com/o/icons%2F" + typeSlug + ".png?alt=media";

            List<String> secondaryUrls = new ArrayList<>();
            secondaryUrls.add("https://storage.firebase.com/v0/b/shopapp-demo.appspot.com/o/products%2Fdetail%2Fview1_" + i + ".jpg?alt=media");
            secondaryUrls.add("https://storage.firebase.com/v0/b/shopapp-demo.appspot.com/o/products%2Fdetail%2Fview2_" + i + ".jpg?alt=media");

            ProductImageDetails imageDetails = new ProductImageDetails(mainImageUrl, secondaryUrls, subCategoryUrl);


            // GỌI CONSTRUCTOR MỚI CỦA PRODUCT
            Product p = new Product(
                    UUID.randomUUID().toString(), // Product ID nội bộ
                    name,
                    desc,
                    currentPrice,
                    originalPrice,
                    imageDetails, // <-- TRUYỀN ĐỐI TƯỢNG IMAGE MỚI
                    category,
                    type,
                    status,
                    isOffer,
                    isOffer ? "From 15.10 - 22.10" : null,
                    "Sản phẩm mới, số lượng có hạn.",
                    variants,    // Danh sách biến thể
                    rating,      // Đánh giá trung bình
                    totalReviews,// Tổng đánh giá
                    isFeatured   // Nổi bật
            );
            list.add(p);
        }
        return list;
    }


    // ---------------------------------------------------------------------
    // HÀM ĐỌC DỮ LIỆU
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

                                dataBuilder.append("--- Sản phẩm #").append(count).append(" ---\n")
                                        .append(" 🔑 Doc ID: ").append(document.getId()).append("\n")
                                        .append(" 📝 Tên: ").append(p.name).append("\n")
                                        .append(" 🏷️ Giá: ").append(String.format("%,.0f đ", p.currentPrice)).append("\n")
                                        .append(" ⭐️ Đánh giá: ").append(p.averageRating).append(" (").append(p.totalReviews).append(" lượt)\n")
                                        .append(" 💥 Nổi bật: ").append(p.isFeatured ? "CÓ" : "KHÔNG").append("\n")
                                        // HIỂN THỊ CÁC THÔNG TIN ẢNH MỚI
                                        .append(" 📸 Ảnh Chính: ").append(p.images != null ? p.images.mainImage : "N/A").append("\n")
                                        .append(" 🖼 Ảnh Sub-Category: ").append(p.images != null ? p.images.subCategoryImage : "N/A").append("\n")
                                        .append(" 🛍 Biến thể (").append(p.variants.size()).append("):\n");

                                // Hiển thị chi tiết từng biến thể
                                for (ProductVariant pv : p.variants) {
                                    dataBuilder.append("     - [")
                                            .append(pv.size).append("/").append(pv.color)
                                            .append("] tồn: ").append(pv.quantity)
                                            .append(" | Giá: ").append(String.format("%,.0f đ", pv.price))
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