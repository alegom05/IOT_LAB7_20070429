package com.example.lab6;

import android.app.DownloadManager;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

//import com.cloudinary.Uploader;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;

//import com.cloudinary.utils.ObjectUtils;
//import com.cloudinary.android.config.Configuration;


import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class ServicioAlmacenamiento {
    //private final Cloudinary cloudinary;
    private final Context context;
    //private final Uploader uploader;
    private static boolean isInitialized = false;


    public ServicioAlmacenamiento(Context context) {
        this.context = context;
        inicializarCloudinary(context);
    }

    private void inicializarCloudinary(Context context) {
        if (!isInitialized) {
            Map<String, String> config = new HashMap<>();
            config.put("cloud_name", "dssoxggz3");
            config.put("api_key", "996445227885663");
            config.put("api_secret", "2WqO6YaSuZyO0WXc8cxj4JkArxU");
            MediaManager.init(context, config);
            isInitialized = true;
        }
    }

    // Conexión al servicio
    public boolean conectar() {
        Log.d("ServioAlmacenamiento", "Se encuentra conectado al storage");
        return MediaManager.get() != null;
    }

    // Guardar archivo
    public void guardarArchivo(Uri uri,  CallbackExito callbackExito, CallbackError callbackError) {
        MediaManager.get().upload(uri)
                .callback(new UploadCallback() {
                    @Override
                    public void onStart(String requestId) {
                        Log.d("Cloudinary", "Inicio de subida");
                    }

                    @Override
                    public void onProgress(String requestId, long bytes, long totalBytes) {
                        // puedes mostrar progreso si quieres
                        Log.d("Cloudinary", "Progreso: " + bytes + "/" + totalBytes);
                    }

                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        String url = resultData.get("secure_url").toString();
                        Log.d("Cloudinary", "Subida exitosa: " + url);
                        callbackExito.onSuccess(url);
                    }

                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        Log.e("Cloudinary", "Error al subir: " + error.getDescription());
                        callbackError.onError("Error al subir: " + error.getDescription());
                    }

                    @Override
                    public void onReschedule(String requestId, ErrorInfo error) {
                        Log.w("Cloudinary", "Reagendado: " + error.getDescription());
                        callbackError.onError("Reagendado: " + error.getDescription());
                    }
                })
                .dispatch();

    }

    public void descargarArchivo(String publicId, String nombreArchivo) {
        String url = obtenerArchivo(publicId);
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        request.setTitle("Descargando imagen");
        request.setDescription("Imagen de comprobante");
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, nombreArchivo + ".jpg");
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

        DownloadManager manager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        manager.enqueue(request);
    }

    public String obtenerArchivo(String publicId) {
        return MediaManager.get().url().secure(true).generate(publicId);
    }

    private File crearArchivoTemporalDesdeUri(Uri uri) {
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            File archivoTemp = File.createTempFile("upload", ".jpg", context.getCacheDir());
            java.nio.file.Files.copy(
                    inputStream,
                    archivoTemp.toPath(),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING
            );
            inputStream.close();
            return archivoTemp;
        } catch (Exception e) {
            Log.e("ServicioAlmacenamiento", "Error creando archivo temporal", e);
            return null;
        }
    }
    public interface CallbackExito {
        void onSuccess(String url);
    }

    public interface CallbackError {
        void onError(String mensaje);
    }

}
