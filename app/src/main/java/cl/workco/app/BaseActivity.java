package cl.workco.app;

import android.content.Intent;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.firebase.auth.FirebaseAuth;

public abstract class BaseActivity extends AppCompatActivity {
    protected final FirebaseAuth auth = FirebaseAuth.getInstance();

    protected void prepararPantalla(int layout) {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(layout);
        View root = findViewById(R.id.root);
        int l = root.getPaddingLeft(), t = root.getPaddingTop();
        int r = root.getPaddingRight(), b = root.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
            v.setPadding(l + bars.left, t + bars.top, r + bars.right,
                    b + Math.max(bars.bottom, ime.bottom));
            return insets;
        });
        ViewCompat.requestApplyInsets(root);
    }

    protected boolean exigirSesion() {
        if (auth.getCurrentUser() != null) return true;
        navegar(LoginActivity.class);
        return false;
    }

    protected void navegar(Class<?> pantalla) {
        startActivity(new Intent(this, pantalla).addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
    }

    protected void avisar(String texto) {
        Toast.makeText(this, texto, Toast.LENGTH_LONG).show();
    }
}
