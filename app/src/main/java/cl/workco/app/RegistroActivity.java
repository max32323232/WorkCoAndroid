package cl.workco.app;

import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class RegistroActivity extends BaseActivity {
    private EditText email, password, confirm;
    private Button submit, switchScreen;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        prepararPantalla(R.layout.activity_auth);
        ((TextView) findViewById(R.id.heading)).setText(R.string.register);
        findViewById(R.id.confirmBox).setVisibility(View.VISIBLE);
        email = findViewById(R.id.email); password = findViewById(R.id.password);
        confirm = findViewById(R.id.confirm);
        submit = findViewById(R.id.submit); submit.setText(R.string.register);
        switchScreen = findViewById(R.id.switchScreen); switchScreen.setText(R.string.to_login);
        submit.setOnClickListener(v -> registrar());
        switchScreen.setOnClickListener(v -> finish());
    }

    @Override protected void onStart() {
        super.onStart();
        ocupado(false);
        if (auth.getCurrentUser() != null) navegar(MainActivity.class);
    }

    private void registrar() {
        String correo = email.getText().toString().trim();
        String clave = password.getText().toString();
        if (!Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
            email.setError("Ingresa un correo válido"); email.requestFocus(); return;
        }
        if (clave.trim().isEmpty() || clave.length() < 6) {
            password.setError("Usa al menos 6 caracteres"); password.requestFocus(); return;
        }
        if (!clave.equals(confirm.getText().toString())) {
            confirm.setError("Las contraseñas no coinciden"); confirm.requestFocus(); return;
        }
        if (!ErroresFirebase.hayInternet(this)) { avisar("Sin conexión a internet."); return; }
        ocupado(true);
        auth.createUserWithEmailAndPassword(correo, clave).addOnCompleteListener(this, task -> {
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
