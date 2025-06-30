package com.example.lab6;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;


public class TheVistaInicial extends AppCompatActivity {

    private TextView tvUserName;
    private FirebaseAuth auth;
    private BottomNavigationView bottomNavigationView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_vistainicial);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        tvUserName = findViewById(R.id.tv_user_name);
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        auth = FirebaseAuth.getInstance();


        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        String displayName = user.getDisplayName();
        if (displayName == null || displayName.isEmpty()) {
            displayName = user.getEmail();
        }
        tvUserName.setText(displayName != null ? displayName : "Usuario");

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_ingresos) {
                startActivity(new Intent(TheVistaInicial.this, IngresosActivity.class));
                return true;
            } else if (id == R.id.nav_egresos) {
                startActivity(new Intent(TheVistaInicial.this, EgresosActivity.class));
                return true;
            } else if (id == R.id.nav_resumen) {
                startActivity(new Intent(TheVistaInicial.this, ResumenActivity.class));
                return true;
            } else if (id == R.id.nav_logout) {
                auth.signOut();
                startActivity(new Intent(TheVistaInicial.this, LoginActivity.class));
                finish();
                return true;
            }
            return false;
        });

    }
}