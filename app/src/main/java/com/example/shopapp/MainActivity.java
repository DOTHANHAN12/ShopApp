package com.example.shopapp;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task; // ✅ thêm dòng này
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();

        // Thử tạo tài khoản (test)
        String email = "huy@gmail.com";
        String password = "Alin2004@";

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Log.d("Main", "createUserWithEmail:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                Toast.makeText(
                                        getApplicationContext(),
                                        "Tạo tài khoản thành công: " + user.getEmail(),
                                        Toast.LENGTH_LONG
                                ).show();
                            }
                        } else {
                            Log.w("Main", "createUserWithEmail:failure", task.getException());
                            Toast.makeText(
                                    MainActivity.this,
                                    "Tạo tài khoản thất bại: " + task.getException().getMessage(),
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                    }
                });
    }
}
