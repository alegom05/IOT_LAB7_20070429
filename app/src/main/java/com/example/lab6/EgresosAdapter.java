package com.example.lab6;

import android.app.DownloadManager;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lab6.beans.Egresos;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class EgresosAdapter extends RecyclerView.Adapter<EgresosAdapter.EgresoViewHolder>{

    public interface OnEgresoActionListener {
        void onEditar(Egresos egreso);
        void onEliminar(Egresos egreso);
    }

    public interface OnEditarEgresoListener {
        void onEditar(Egresos egreso);
    }

    public interface OnEliminarEgresoListener {
        void onEliminar(Egresos egreso);
    }

    private Context context;
    private List<Egresos> listaEgresos;
//    private OnEgresoActionListener listener;
//
//    public EgresoAdapter(List<Egreso> listaEgresos, OnEgresoActionListener listener) {
//        this.listaEgresos = listaEgresos;
//        this.listener = listener;
//    }
    private OnEditarEgresoListener editarListener;
    private OnEliminarEgresoListener eliminarListener;

    public EgresosAdapter(Context context, List<Egresos> lista, OnEditarEgresoListener editar, OnEliminarEgresoListener eliminar) {
        this.context = context;
        this.listaEgresos = lista;
        this.editarListener = editar;
        this.eliminarListener = eliminar;
    }

    @NonNull
    @Override
    public EgresoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View vista = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_egreso, parent, false);
        return new EgresoViewHolder(vista);
    }

    @Override
    public void onBindViewHolder(@NonNull EgresoViewHolder holder, int position) {
        Egresos egreso = listaEgresos.get(position);
        SimpleDateFormat formatoFecha = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());


        holder.tvTitulo.setText(egreso.getTitulo());
        holder.tvDescripcion.setText(egreso.getDescripcion().isEmpty() ? "(Sin descripción)" : egreso.getDescripcion());
        holder.tvFecha.setText(formatoFecha.format(egreso.getFecha()));
        holder.tvMonto.setText(String.format("S/. %.2f", egreso.getMonto()));

//        holder.btnEditar.setOnClickListener(v -> {
//            if (listener != null) listener.onEditar(egreso);
//        });
//
//        holder.btnEliminar.setOnClickListener(v -> {
//            if (listener != null) listener.onEliminar(egreso);
//        });
        holder.btnEditar.setOnClickListener(v -> editarListener.onEditar(egreso));
        holder.btnEliminar.setOnClickListener(v -> eliminarListener.onEliminar(egreso));

        holder.btnDescargar.setOnClickListener(v -> {
            String urlImagen = egreso.getUrlImagen();
            if (urlImagen == null || urlImagen.isEmpty()) {
                Toast.makeText(context, "No hay comprobante para descargar", Toast.LENGTH_SHORT).show();
                return;
            }

            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(urlImagen));
            request.setTitle("Comprobante - " + egreso.getTitulo());
            request.setDescription("Descargando comprobante del egreso");
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "comprobante_" + egreso.getTitulo() + ".jpg");

            DownloadManager manager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
            if (manager != null) {
                manager.enqueue(request);
                Toast.makeText(context, "Descarga iniciada...", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, "Error al iniciar la descarga", Toast.LENGTH_SHORT).show();
            }
        });


    }

    @Override
    public int getItemCount() {
        return listaEgresos.size();
    }

    static class EgresoViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitulo, tvDescripcion, tvFecha, tvMonto;
        ImageButton btnEditar, btnEliminar, btnDescargar;
        ImageView ivIcono;

        public EgresoViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitulo = itemView.findViewById(R.id.tv_titulo_egreso); // si usas el mismo ID
            tvDescripcion = itemView.findViewById(R.id.tv_descripcion_egreso);
            tvFecha = itemView.findViewById(R.id.tv_fecha_egreso);
            tvMonto = itemView.findViewById(R.id.tv_monto_egreso);
            btnEditar = itemView.findViewById(R.id.btn_editar_egreso);
            btnEliminar = itemView.findViewById(R.id.btn_eliminar_egreso);
            ivIcono = itemView.findViewById(R.id.iv_icono_egreso);
            btnDescargar = itemView.findViewById(R.id.btn_descargar_egreso);

        }
    }

}
