package cl.workco.app;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class LoginActivity extends BaseActivity {
    private EditText email, password;
    private Button submit, switchScreen;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        prepararPantalla(R.layout.activity_auth);
        email = findViewById(R.id.email);
        password = findViewById(R.id.password);
        submit = findViewById(R.id.submit);
        switchScreen = findViewById(R.id.switchScreen);
        submit.setOnClickListener(v -> ingresar());
        switchScreen.setOnClickListener(v -> startActivity(new Intent(this, RegistroActivity.class)));
    }

    @Override protected void onStart() {
        super.onStart();
        ocupado(false);
        if (auth.getCurrentUser() != null) navegar(MainActivity.class);
    }

    private void ingresar() {
        String correo = email.getText().toString().trim();
        String clave = password.getText().toString();
        if (!Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
            email.setError("Ingresa un correo válido"); email.requestFocus(); return;
        }
        if (clave.length() < 6) {
            password.setError("Ingresa al menos 6 caracteres"); password.requestFocus(); return;
        }
        if (!ErroresFirebase.hayInternet(this)) { avisar("Sin conexión a internet."); return; }
        ocupado(true);
        auth.signInWithEmailAndPassword(correo, clave).addOnCompleteListener(this, task -> {
            ocupado(false);
            if (task.isSuccessful()) navegar(MainActivity.class);
            else avisar(ErroresFirebase.mensaje(task.getException()));
        });
    }

    private void ocupado(boolean valor) {
        findViewById(R.id.progress).setVisibility(valor ? View.VISIBLE : View.GONE);
        submit.setEnabled(!valor); switchScreen.setEnabled(!valor);
    }
}
