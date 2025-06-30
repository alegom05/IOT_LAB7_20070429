package com.example.lab6;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;


import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lab6.beans.Ingresos;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

//Ingresos Activity con redirecciones

public class IngresosActivity extends AppCompatActivity {

    private Uri uriImagenSeleccionada;
    private ServicioAlmacenamiento servicioAlmacenamiento;
    private RecyclerView recyclerView;
    private IngresosAdapter adapter;
    private List<Ingresos> listaIngresos;
    private LinearLayout emptyStateContainer;
    private TextView tvTotalIngresos;
    private FirebaseFirestore db;
    private String userId;

    private final int IMAGE_CODE = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_ingresos);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        recyclerView = findViewById(R.id.recycler_view_ingresos);
        emptyStateContainer = findViewById(R.id.empty_state_container);
        tvTotalIngresos = findViewById(R.id.tv_total_ingresos);
        FloatingActionButton btnAgregar = findViewById(R.id.btn_agregar_ingreso);
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        listaIngresos = new ArrayList<>();
        adapter = new IngresosAdapter(this, listaIngresos, this::mostrarDialogoEditarIngreso, this::eliminarIngreso);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
        db = FirebaseFirestore.getInstance();
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        servicioAlmacenamiento = new ServicioAlmacenamiento(this);
        metodoIngresos();

        btnAgregar.setOnClickListener(v -> mostrarDialogoIngreso(null));

        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_ingresos) {
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
                FirebaseAuth.getInstance().signOut();
                startActivity(new Intent(this, LoginActivity.class));
                finish();
                return true;
            }

            return false;
        });

        bottomNav.setSelectedItemId(R.id.nav_ingresos);

    }

    private void metodoIngresos() {
        db.collection("usuarios").document(userId).collection("ingresos")
                .orderBy("fecha", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) return;

                    listaIngresos.clear();
                    double total = 0;

                    for (QueryDocumentSnapshot doc : snapshots) {
                        Ingresos ingreso = doc.toObject(Ingresos.class);
                        ingreso.setId(doc.getId());
                        listaIngresos.add(ingreso);
                        total += ingreso.getMonto();
                    }

                    adapter.notifyDataSetChanged();
                    emptyStateContainer.setVisibility(listaIngresos.isEmpty() ? View.VISIBLE : View.GONE);
                    tvTotalIngresos.setText(String.format("Total de Ingresos: S/. %.2f", total));
                });
    }

    private void mostrarDialogoIngreso(@NonNull Ingresos ingresoExistente) {
        View view = LayoutInflater.from(this).inflate(R.layout.agregar_ingreso, null);
        TextInputEditText etTitulo = view.findViewById(R.id.et_titulo);
        TextInputEditText etMonto = view.findViewById(R.id.et_monto);
        TextInputEditText etDescripcion = view.findViewById(R.id.et_descripcion);
        TextInputEditText etFecha = view.findViewById(R.id.et_fecha);
        TextView tvTituloDialog = view.findViewById(R.id.tv_titulo_dialog);
        Button btnGuardar = view.findViewById(R.id.btn_guardar);
        Button btnCancelar = view.findViewById(R.id.btn_cancelar);

        MaterialButton btnSeleccionarImagen = view.findViewById(R.id.btn_seleccionar_imagen);
        MaterialButton btnEliminarImagen = view.findViewById(R.id.btn_eliminar_imagen);
        LinearLayout llPreview = view.findViewById(R.id.ll_imagen_preview);
        ImageView ivPreview = view.findViewById(R.id.iv_imagen_preview);
        TextView tvNombreImagen = view.findViewById(R.id.tv_nombre_imagen);

        final Calendar calendario = Calendar.getInstance();
        etFecha.setOnClickListener(v -> {
            new DatePickerDialog(this, (view1, year, month, dayOfMonth) -> {
                String fechaFormateada = String.format(Locale.getDefault(), "%02d/%02d/%d", dayOfMonth, month + 1, year);
                etFecha.setText(fechaFormateada);
            }, calendario.get(Calendar.YEAR), calendario.get(Calendar.MONTH), calendario.get(Calendar.DAY_OF_MONTH)).show();
        });

        btnSeleccionarImagen.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            startActivityForResult(intent, IMAGE_CODE);
        });

        btnEliminarImagen.setOnClickListener(v -> {
            uriImagenSeleccionada = null;
            llPreview.setVisibility(View.GONE);
        });

        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(view)
                .setCancelable(false)
                .create();

        if (ingresoExistente != null) {
            tvTituloDialog.setText("Editar Ingreso");
            etTitulo.setText(ingresoExistente.getTitulo());
            etTitulo.setEnabled(false); // solo monto y descripción
            etMonto.setText(String.valueOf(ingresoExistente.getMonto()));
            etDescripcion.setText(ingresoExistente.getDescripcion());
            SimpleDateFormat formato = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            etFecha.setText(formato.format(ingresoExistente.getFecha()));
        }

        btnGuardar.setOnClickListener(v -> {
            final String titulo = etTitulo.getText().toString().trim();
            final String montoStr = etMonto.getText().toString().trim();
            final String descripcion = etDescripcion.getText().toString().trim();
            final String fechaStr = etFecha.getText().toString().trim();
            final Date fecha;
            try {
                fecha = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(fechaStr);
            } catch (Exception e) {
                Toast.makeText(this, "Fecha inválida", Toast.LENGTH_SHORT).show();
                return;
            }

            if (titulo.isEmpty() || montoStr.isEmpty() || fecha == null || uriImagenSeleccionada == null) {
                Toast.makeText(this, "Por favor completa los campos obligatorios", Toast.LENGTH_SHORT).show();
                return;
            }

            final double monto = Double.parseDouble(montoStr);
            final String nombreImagen = UUID.randomUUID().toString() + ".jpg";


            servicioAlmacenamiento.guardarArchivo(uriImagenSeleccionada, urlImagen -> {
                Ingresos ingreso = new Ingresos(titulo, monto, descripcion, fecha);
                ingreso.setImagenUrl(urlImagen);

                if (ingresoExistente != null) {
                    db.collection("usuarios").document(userId)
                            .collection("ingresos").document(ingresoExistente.getId())
                            .update("monto", monto, "descripcion", descripcion)
                            .addOnSuccessListener(aVoid -> dialog.dismiss());
                } else {
                    db.collection("usuarios").document(userId)
                            .collection("ingresos").add(ingreso)
                            .addOnSuccessListener(docRef -> dialog.dismiss());
                }

            }, error -> {
                Toast.makeText(this, "Error al subir imagen: " + error, Toast.LENGTH_SHORT).show();
            });

        });

        btnCancelar.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }


    private void eliminarIngreso(Ingresos ingreso) {
        db.collection("usuarios").document(userId)
                .collection("ingresos").document(ingreso.getId())
                .delete()
                .addOnSuccessListener(unused -> Toast.makeText(this, "Ingreso eliminado", Toast.LENGTH_SHORT).show());
    }

    private void mostrarDialogoEditarIngreso(Ingresos ingreso) {
        mostrarDialogoIngreso(ingreso);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == IMAGE_CODE && resultCode == RESULT_OK && data != null) {
            uriImagenSeleccionada = data.getData();
            Log.d("IngresoActivity", "Imagen seleccionada: " + uriImagenSeleccionada);
            if (uriImagenSeleccionada != null) {
                View viewDialog = getLayoutInflater().inflate(R.layout.agregar_ingreso, null);
                TextView tvNombreImagen = viewDialog.findViewById(R.id.tv_nombre_imagen);
                ImageView ivPreview = viewDialog.findViewById(R.id.iv_imagen_preview);
                LinearLayout llPreview = viewDialog.findViewById(R.id.ll_imagen_preview);

                tvNombreImagen.setText(uriImagenSeleccionada.getLastPathSegment());
                ivPreview.setImageURI(uriImagenSeleccionada);
                llPreview.setVisibility(View.VISIBLE);
            }
        }
    }

}