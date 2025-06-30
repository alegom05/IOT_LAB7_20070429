package com.example.lab6;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.facebook.login.LoginManager;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserInfo;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;



public class ResumenActivity extends AppCompatActivity {

    private TextView tvIngTotal, tvTotalEgresos, tvBalance, tvMesActual;
    private FirebaseFirestore db;
    private String userId;
    private Calendar calendario;
    private SimpleDateFormat formatoFirestore, formatoMesVisual;
    WebView webViewPie;
    WebView webViewBar;
    private boolean pieCargado = false;
    private boolean barraCargado = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_resumen);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        tvIngTotal = findViewById(R.id.tv_total_ingresos_resumen);
        tvTotalEgresos = findViewById(R.id.tv_total_egresos_resumen);
        tvBalance = findViewById(R.id.tv_balance_total);
        tvMesActual = findViewById(R.id.tv_mes_actual);
        ImageButton btnMesAnterior = findViewById(R.id.btn_mes_anterior);
        ImageButton btnMesSiguiente = findViewById(R.id.btn_mes_siguiente);
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        webViewPie = findViewById(R.id.webview_pie_chart);
        webViewPie.getSettings().setJavaScriptEnabled(true);
        webViewPie.setBackgroundColor(Color.TRANSPARENT);
        webViewPie.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                pieCargado = true;
                if (barraCargado) {
                    actualizarResumen();
                }
            }
        });
        webViewPie.loadUrl("file:///android_asset/chart_pie.html");
        webViewBar = findViewById(R.id.webview_bar_chart);
        webViewBar.getSettings().setJavaScriptEnabled(true);
        webViewBar.setBackgroundColor(Color.TRANSPARENT);

        webViewBar.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                barraCargado = true;
                if (pieCargado) {
                    actualizarResumen();
                }
            }
        });

        webViewBar.loadUrl("file:///android_asset/chart_bar.html");
        db = FirebaseFirestore.getInstance();
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if(auth.getCurrentUser()==null){
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        Log.d("ResumenActivity", "✅ Usuario autenticado: " + userId);

        // Formatos para mes y fecha
        calendario = Calendar.getInstance();
        formatoFirestore = new SimpleDateFormat("MM/yyyy", Locale.getDefault());
        formatoMesVisual = new SimpleDateFormat("MMMM yyyy", new Locale("es", "ES"));

        tvMesActual.setOnClickListener(v -> mostrarDialogoMes());

        btnMesAnterior.setOnClickListener(v -> {
            calendario.add(Calendar.MONTH, -1);
            actualizarResumen();
        });

        btnMesSiguiente.setOnClickListener(v -> {
            calendario.add(Calendar.MONTH, 1);
            actualizarResumen();
        });

        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_ingresos) {
                startActivity(new Intent(this, IngresosActivity.class));
                return true;
            } else if (itemId == R.id.nav_egresos) {
                startActivity(new Intent(this, EgresosActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.nav_resumen) {
                startActivity(new Intent(this, ResumenActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.nav_logout) {
                FirebaseUser usuarioActual = FirebaseAuth.getInstance().getCurrentUser();

                if (usuarioActual != null) {
                    for (UserInfo userInfo : usuarioActual.getProviderData()) {
                        String providerId = userInfo.getProviderId();

                        switch (providerId) {
                            case "google.com":
                                GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                        .requestEmail()
                                        .build();
                                GoogleSignInClient googleClient = GoogleSignIn.getClient(this, gso);
                                googleClient.signOut().addOnCompleteListener(task -> {
                                    googleClient.revokeAccess();
                                    FirebaseAuth.getInstance().signOut();
                                    redirigirAlLogin();
                                });
                                return true;

                            case "facebook.com":
                                LoginManager.getInstance().logOut();  // de Facebook SDK
                                FirebaseAuth.getInstance().signOut();
                                redirigirAlLogin();
                                return true;

                            default:
                                // Para email/password u otros
                                FirebaseAuth.getInstance().signOut();
                                redirigirAlLogin();
                                return true;
                        }
                    }
                }
            }
            return false;
        });

        actualizarResumen();
    }
    private void redirigirAlLogin() {
        Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }


    private void actualizarResumen() {
        String mesFiltro = new SimpleDateFormat("MM/yyyy", Locale.getDefault()).format(calendario.getTime());
        //tvMesActual.setText(formatoMesVisual.format(calendario.getTime()));
        String mesVisual = formatoMesVisual.format(calendario.getTime());
        tvMesActual.setText(mesVisual);

        double[] totalIngresos = {0.0};
        double[] totalEgresos = {0.0};

        Calendar inicioMes = (Calendar) calendario.clone();
        inicioMes.set(Calendar.DAY_OF_MONTH, 1);
        inicioMes.set(Calendar.HOUR_OF_DAY, 0);
        inicioMes.set(Calendar.MINUTE, 0);
        inicioMes.set(Calendar.SECOND, 0);

        Calendar finMes = (Calendar) inicioMes.clone();
        finMes.set(Calendar.DAY_OF_MONTH, finMes.getActualMaximum(Calendar.DAY_OF_MONTH));
        finMes.set(Calendar.HOUR_OF_DAY, 23);
        finMes.set(Calendar.MINUTE, 59);
        finMes.set(Calendar.SECOND, 59);

        Timestamp fechaInicio = new Timestamp(inicioMes.getTime());
        Timestamp fechaFin = new Timestamp(finMes.getTime());

        db.collection("usuarios").document(userId)
                .collection("ingresos")
                .whereGreaterThanOrEqualTo("fecha", fechaInicio)
                .whereLessThanOrEqualTo("fecha",fechaFin)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Double monto = doc.getDouble("monto");
                        if (monto != null) totalIngresos[0] += monto;
                    }
                    tvIngTotal.setText(String.format(Locale.getDefault(), "S/. %.2f", totalIngresos[0]));

                    db.collection("usuarios").document(userId)
                            .collection("egresos")

                            .whereGreaterThanOrEqualTo("fecha", fechaInicio)
                            .whereLessThanOrEqualTo("fecha",fechaFin)
                            .get()
                            .addOnSuccessListener(querySnapshots -> {
                                for (QueryDocumentSnapshot doc : querySnapshots) {
                                    Double monto = doc.getDouble("monto");
                                    if (monto != null) totalEgresos[0] += monto;
                                }
                                tvTotalEgresos.setText(String.format(Locale.getDefault(), "S/. %.2f", totalEgresos[0]));
                                double balance = totalIngresos[0] - totalEgresos[0];
                                tvBalance.setText(String.format(Locale.getDefault(), "%s S/. %.2f", (balance >= 0 ? "+" : "-"), Math.abs(balance)));
                                TextView placeholderCircular = findViewById(R.id.tv_placeholder_circular);
                                WebView pieChartWebView = findViewById(R.id.webview_pie_chart);

                                if (totalIngresos[0] == 0 && totalEgresos[0] == 0) {
                                    placeholderCircular.setVisibility(View.VISIBLE);
                                    pieChartWebView.setVisibility(View.GONE);
                                } else {
                                    placeholderCircular.setVisibility(View.GONE);
                                    pieChartWebView.setVisibility(View.VISIBLE);
                                }
                                try {
                                    JSONObject jsonData = new JSONObject();
                                    JSONArray labels = new JSONArray();
                                    JSONArray values = new JSONArray();
                                    labels.put("Ingresos");
                                    labels.put("Egresos");
                                    values.put(totalIngresos[0]);
                                    values.put(totalEgresos[0]);
                                    jsonData.put("labels", labels);
                                    jsonData.put("values", values);
                                    webViewPie.evaluateJavascript("updateChart(" + jsonData.toString() + ")", null);
                                    webViewBar.evaluateJavascript("updateBarChart(" + jsonData.toString() + ")", null);

                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            });
                });
    }

    private void mostrarDialogoMes() {
        int anio = calendario.get(Calendar.YEAR);
        int mes = calendario.get(Calendar.MONTH);

        DatePickerDialog dialog = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            calendario.set(Calendar.YEAR, year);
            calendario.set(Calendar.MONTH, month);
            actualizarResumen();
        }, anio, mes, 1); // El día no importa

        try {
            ((ViewGroup) ((ViewGroup) dialog.getDatePicker()).getChildAt(0)).getChildAt(2).setVisibility(View.GONE);
        } catch (Exception e) {
            e.printStackTrace();
        }

        dialog.show();
    }


}