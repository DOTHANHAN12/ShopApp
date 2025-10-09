package com.example.shopapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class MainActivity2 extends AppCompatActivity {

    private FirebaseFirestore db;
    private Button btnWrite, btnRead;
    private TextView tvOutput;
    private static final String TAG = "FirestoreTest";

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        db = FirebaseFirestore.getInstance();
        btnWrite = findViewById(R.id.btnWrite);
        btnRead = findViewById(R.id.btnRead);
        tvOutput = findViewById(R.id.tvOutput); // Thêm TextView trong layout để hiển thị dữ liệu

        // Nút ghi dữ liệu
        btnWrite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                writeSampleData();
            }
        });

        // Nút đọc dữ liệu
        btnRead.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                readSampleData();
            }
        });
    }

    /** Ghi dữ liệu mẫu vào Firestore */
    private void writeSampleData() {
        CollectionReference cities = db.collection("cities");

        Map<String, Object> data1 = new HashMap<>();
        data1.put("name", "San Francisco");
        data1.put("state", "CA");
        data1.put("country", "USA");
        data1.put("capital", false);
        data1.put("population", 860000);
        data1.put("regions", Arrays.asList("west_coast", "norcal"));
        cities.document("SF").set(data1);

        Map<String, Object> data2 = new HashMap<>();
        data2.put("name", "Tokyo");
        data2.put("country", "Japan");
        data2.put("capital", true);
        data2.put("population", 9000000);
        data2.put("regions", Arrays.asList("kanto", "honshu"));
        cities.document("TOK").set(data2);

        Toast.makeText(this, "✅ Ghi dữ liệu mẫu thành công!", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "✅ Ghi dữ liệu mẫu thành công!");
    }

    /** Đọc dữ liệu và hiển thị ra màn hình + log */
    private void readSampleData() {
        db.collection("cities")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            StringBuilder dataBuilder = new StringBuilder();
                            dataBuilder.append("📂 Dữ liệu trong collection 'cities':\n\n");

                            for (QueryDocumentSnapshot document : task.getResult()) {
                                Log.d(TAG, document.getId() + " => " + document.getData());
                                dataBuilder.append("🗎 ")
                                        .append(document.getId())
                                        .append(" => ")
                                        .append(document.getData().toString())
                                        .append("\n\n");
                            }

                            tvOutput.setText(dataBuilder.toString());
                        } else {
                            Log.w(TAG, "❌ Lỗi đọc dữ liệu 'cities': ", task.getException());
                            tvOutput.setText("❌ Lỗi đọc dữ liệu: " + task.getException().getMessage());
                        }
                    }
                });
    }
}
