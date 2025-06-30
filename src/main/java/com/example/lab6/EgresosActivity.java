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
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lab6.beans.Egresos;
import com.google.android.material.bottomnavigation.BottomNavigationView;
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
import java.util.Locale;
import java.util.List;
import java.util.UUID;

//Egresos Activity con redirecciones

public class EgresosActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private EgresosAdapter adapter;
    private List<Egresos> listaEgresos;
    private LinearLayout emptyStateContainer;
    private TextView tvTotalEgresos;
    private FirebaseFirestore db;
    private String userId;
    private Uri uriImagenSeleccionada;
    private ServicioAlmacenamiento servicioAlmacenamiento;
    private static final int REQUEST_SELECCIONAR_IMAGEN = 1001;
    private View vistaDialogoEgreso;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_egresos);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        recyclerView = findViewById(R.id.recycler_view_egresos);
        emptyStateContainer = findViewById(R.id.empty_state_container);
        tvTotalEgresos = findViewById(R.id.tv_total_egresos);
        FloatingActionButton btnAgregar = findViewById(R.id.btn_agregar_egreso);
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);

        listaEgresos = new ArrayList<>();
        adapter = new EgresosAdapter(this,listaEgresos, this::mostrarDialogoEditarEgreso, this::eliminarEgreso);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        servicioAlmacenamiento = new ServicioAlmacenamiento(this);

        cargarEgresos();

        btnAgregar.setOnClickListener(v -> mostrarDialogoEgreso(null));

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
                FirebaseAuth.getInstance().signOut();
                startActivity(new Intent(this, LoginActivity.class));
                finish();
                return true;
            }

            return false;
        });



    }

    private void cargarEgresos() {
        db.collection("usuarios").document(userId).collection("egresos")
                .orderBy("fecha", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) return;

                    listaEgresos.clear();
                    double total = 0;

                    for (QueryDocumentSnapshot doc : snapshots) {
                        Egresos egreso = doc.toObject(Egresos.class);
                        egreso.setId(doc.getId());
                        listaEgresos.add(egreso);
                        total += egreso.getMonto();
                    }

                    adapter.notifyDataSetChanged();
                    emptyStateContainer.setVisibility(listaEgresos.isEmpty() ? View.VISIBLE : View.GONE);
                    tvTotalEgresos.setText(String.format("Total de Egresos: S/. %.2f", total));
                });
    }

    private void mostrarDialogoEgreso(@NonNull Egresos egresoExistente) {
        vistaDialogoEgreso  = LayoutInflater.from(this).inflate(R.layout.agregar_egreso, null);
        View view = vistaDialogoEgreso;
        TextInputEditText etTitulo = view.findViewById(R.id.et_titulo);
        TextInputEditText etMonto = view.findViewById(R.id.et_monto);
        TextInputEditText etDescripcion = view.findViewById(R.id.et_descripcion);
        TextInputEditText etFecha = view.findViewById(R.id.et_fecha);
        TextView tvTituloDialog = view.findViewById(R.id.tv_titulo_dialog);
        Button btnGuardar = view.findViewById(R.id.btn_guardar);
        Button btnCancelar = view.findViewById(R.id.btn_cancelar);

        Button btnSeleccionarImagen = view.findViewById(R.id.btn_seleccionar_imagen);
        btnSeleccionarImagen.setOnClickListener(v -> seleccionarImagen());
        ImageView ivPreview = view.findViewById(R.id.iv_imagen_preview);
        TextView tvNombreImagen = view.findViewById(R.id.tv_nombre_imagen);
        LinearLayout llPreview = view.findViewById(R.id.ll_imagen_preview);
        Button btnEliminarImagen = view.findViewById(R.id.btn_eliminar_imagen);
        ivPreview.setImageResource(0);
        llPreview.setVisibility(View.GONE);

        Button btnTomarFoto = view.findViewById(R.id.btn_tomar_foto);
        btnTomarFoto.setOnClickListener(v -> Toast.makeText(this, "Función cámara aún no implementada", Toast.LENGTH_SHORT).show());


        final Calendar calendario = Calendar.getInstance();
        etFecha.setOnClickListener(v -> {
            new DatePickerDialog(this, (view1, year, month, dayOfMonth) -> {
                String fechaFormateada = String.format(Locale.getDefault(), "%02d/%02d/%d", dayOfMonth, month + 1, year);
                etFecha.setText(fechaFormateada);
            }, calendario.get(Calendar.YEAR), calendario.get(Calendar.MONTH), calendario.get(Calendar.DAY_OF_MONTH)).show();
        });

        ivPreview.setOnClickListener(v -> seleccionarImagen());
        btnEliminarImagen.setOnClickListener(v -> {
            uriImagenSeleccionada = null;
            llPreview.setVisibility(View.GONE);
        });

        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(view)
                .setCancelable(false)
                .create();

        if (egresoExistente != null) {
            tvTituloDialog.setText("Editar Egreso");
            etTitulo.setText(egresoExistente.getTitulo());
            etTitulo.setEnabled(false);
            etMonto.setText(String.valueOf(egresoExistente.getMonto()));
            etDescripcion.setText(egresoExistente.getDescripcion());
            SimpleDateFormat formato = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            etFecha.setText(formato.format(egresoExistente.getFecha()));
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

            if (titulo.isEmpty() || montoStr.isEmpty() || fecha==null || uriImagenSeleccionada == null) {
                Toast.makeText(this, "Por favor completa los campos obligatorios", Toast.LENGTH_SHORT).show();
                return;
            }

            final double monto = Double.parseDouble(montoStr);
            final String nombreImagen = UUID.randomUUID().toString() + ".jpg";


            if (uriImagenSeleccionada != null) {
                Log.d("Cloudinary", "Iniciando subida...");
                servicioAlmacenamiento.guardarArchivo(uriImagenSeleccionada,  url -> {
                    Egresos egreso = new Egresos(titulo, monto, descripcion, fecha);
                    egreso.setUrlImagen(url);

                    guardarEgresoFirestore(egreso, egresoExistente, dialog);

                }, error -> {
                    Toast.makeText(this, "Error al subir imagen: " + error, Toast.LENGTH_SHORT).show();
                });
            } else {
                Egresos egreso = new Egresos(titulo, monto, descripcion, fecha);
                egreso.setUrlImagen(""); // o puedes no incluirlo en Firestore si prefieres
                guardarEgresoFirestore(egreso, egresoExistente, dialog);
            }
        });

        btnCancelar.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void guardarEgresoFirestore(Egresos egreso, Egresos egresoExistente, AlertDialog dialog) {

        if (egresoExistente != null) {
            db.collection("usuarios").document(userId)
                    .collection("egresos").document(egresoExistente.getId())
                    .update("monto", egreso.getMonto(), "descripcion", egreso.getDescripcion(), "urlImagen", egreso.getUrlImagen())
                    .addOnSuccessListener(aVoid -> dialog.dismiss());
        } else {
            db.collection("usuarios").document(userId)
                    .collection("egresos").add(egreso)
                    .addOnSuccessListener(docRef -> dialog.dismiss());
        }
    }

    private void mostrarDialogoEditarEgreso(Egresos egreso) {
        mostrarDialogoEgreso(egreso);
    }

    private void eliminarEgreso(Egresos egreso) {
        db.collection("usuarios").document(userId)
                .collection("egresos").document(egreso.getId())
                .delete()
                .addOnSuccessListener(unused -> Toast.makeText(this, "Egreso eliminado", Toast.LENGTH_SHORT).show());
    }

    private void seleccionarImagen() {
        Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_SELECCIONAR_IMAGEN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_SELECCIONAR_IMAGEN && resultCode == RESULT_OK && data != null) {
            uriImagenSeleccionada = data.getData();
            if (uriImagenSeleccionada != null) {
                // Mostrar vista previa
                View dialogView = getLayoutInflater().inflate(R.layout.agregar_egreso, null);
                ImageView ivPreview = dialogView.findViewById(R.id.iv_imagen_preview);
                TextView tvNombre = dialogView.findViewById(R.id.tv_nombre_imagen);
                LinearLayout llPreview = dialogView.findViewById(R.id.ll_imagen_preview);

                ivPreview.setImageURI(uriImagenSeleccionada);
                tvNombre.setText(uriImagenSeleccionada.getLastPathSegment());
                llPreview.setVisibility(View.VISIBLE);
            }
        }
    }



}