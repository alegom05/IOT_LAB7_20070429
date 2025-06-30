package com.example.lab6;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.appevents.AppEventsLogger;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText etEmail, etPassword;
    private LoginButton fbLoginBtn;
    private FirebaseAuth auth;
    private CallbackManager callbackManager;
    private GoogleSignInClient googleSignInClient;
    private static final int RC_GOOGLE_SIGN_IN = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        FacebookSdk.sdkInitialize(getApplicationContext());
        AppEventsLogger.activateApp(getApplication());

        auth = FirebaseAuth.getInstance();
        callbackManager = CallbackManager.Factory.create();

        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        TextView tvIrRegistro = findViewById(R.id.tv_ir_registro);

        MaterialButton btnCorreo = findViewById(R.id.btn_login);
        MaterialButton btnGoogle = findViewById(R.id.btn_login_google);
        MaterialButton btnFacebook = findViewById(R.id.btn_login_facebook);

        btnCorreo.setOnClickListener(v -> loginConCorreo());
        if (btnCorreo == null) {
            Log.e("LoginActivity", "btn_login no encontrado en el layout!");
            return;
        }


        btnFacebook.setOnClickListener(v -> {
            LoginManager.getInstance().logInWithReadPermissions(this, Arrays.asList("email", "public_profile"));
            LoginManager.getInstance().registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
                @Override
                public void onSuccess(LoginResult loginResult) {
                    AuthCredential credential = FacebookAuthProvider.getCredential(loginResult.getAccessToken().getToken());
                    loginConFirebase(credential);
                }

                @Override public void onCancel() {}
                @Override public void onError(FacebookException error) {
                    Toast.makeText(LoginActivity.this, "Error Facebook: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });
        // Google Login
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id)) // Debes tener este en strings.xml
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);

        btnGoogle.setOnClickListener(v -> {
            Intent signInIntent = googleSignInClient.getSignInIntent();
            startActivityForResult(signInIntent, RC_GOOGLE_SIGN_IN);
        });

        // Si ya está logueado
        if (auth.getCurrentUser() != null) {
            startActivity(new Intent(this, ResumenActivity.class));
            finish();
        }

        tvIrRegistro.setOnClickListener(v -> startActivity(new Intent(this, RegistroActivity.class)));

    }



    private void goToHome() {
        startActivity(new Intent(this, ResumenActivity.class));
        finish();
    }

    private void loginConCorreo() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Completa los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    startActivity(new Intent(this, ResumenActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void loginConFirebase(AuthCredential credential) {
        auth.signInWithCredential(credential)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser user=auth.getCurrentUser();
                    if (user != null){
                        FirebaseFirestore db = FirebaseFirestore.getInstance();
                        String uid = user.getUid();
                        String correo = user.getEmail();
                        String nombre = user.getDisplayName();
                        db.collection("usuarios").document(uid).get().addOnSuccessListener(documentSnapshot -> {
                            if(!documentSnapshot.exists()){
                                Map<String,Object> datosUsuario= new HashMap<>();
                                datosUsuario.put("correo",correo);
                                datosUsuario.put("nombre", nombre);
                                datosUsuario.put("fechaRegistro", new Timestamp(new Date()));

                                db.collection("usuarios").document(uid).set(datosUsuario)
                                        .addOnSuccessListener(aVoid -> Log.d("LoginActivity", "✅ Usuario creado en Firestore"))
                                        .addOnFailureListener(e -> Log.e("LoginActivity", "X Error al crear usuario Firestore", e));
                            }else{
                                Log.d("LoginActivity", "VV Usuario ya existe en Firestore");
                            }
                            goToHome();
                        });
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_GOOGLE_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
                String token = account.getIdToken();
                if (token == null) {
                    Log.e("LoginActivity", "El idToken es null, no se puede autenticar con Firebase");
                } else {
                    Log.d("LoginActivity", "IdToken recibido: " + token);
                }
                loginConFirebase(credential);
            } catch (ApiException e) {
                Toast.makeText(this, "Google error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

}