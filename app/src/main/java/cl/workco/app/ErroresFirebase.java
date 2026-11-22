package cl.workco.app;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.firestore.FirebaseFirestoreException;

public final class ErroresFirebase {
    private ErroresFirebase() { }

    public static boolean hayInternet(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        NetworkCapabilities nc = cm.getNetworkCapabilities(cm.getActiveNetwork());
        return nc != null && nc.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
    }

    public static String mensaje(Exception e) {
        if (e instanceof FirebaseTooManyRequestsException) return "Demasiados intentos. Espera unos minutos.";
        if (e instanceof FirebaseNetworkException) return "Sin conexión. Revisa tu internet e intenta otra vez.";
        if (e instanceof SecurityException) return "Tu sesión no es válida. Vuelve a iniciar sesión.";
        if (e instanceof FirebaseAuthException) {
            switch (((FirebaseAuthException) e).getErrorCode()) {
                case "ERROR_EMAIL_ALREADY_IN_USE": return "Este correo ya está registrado.";
                case "ERROR_INVALID_EMAIL": return "El formato del correo no es válido.";
                case "ERROR_WEAK_PASSWORD": return "La contraseña no cumple la política del proyecto (mínimo 6 caracteres).";
                case "ERROR_WRONG_PASSWORD":
                case "ERROR_USER_NOT_FOUND":
                case "ERROR_INVALID_CREDENTIAL":
                case "ERROR_INVALID_LOGIN_CREDENTIALS": return "Correo o contraseña incorrectos.";
                case "ERROR_USER_DISABLED": return "Esta cuenta está deshabilitada.";
                case "ERROR_TOO_MANY_REQUESTS": return "Demasiados intentos. Espera unos minutos.";
                case "ERROR_OPERATION_NOT_ALLOWED": return "Habilita Correo/Contraseña en Firebase Authentication.";
                default: return "No se pudo autenticar. Revisa la configuración y vuelve a intentar.";
            }
        }
        if (e instanceof FirebaseFirestoreException) {
            switch (((FirebaseFirestoreException) e).getCode()) {
                case PERMISSION_DENIED: return "No tienes permiso. Revisa la sesión y las reglas publicadas.";
                case UNAUTHENTICATED: return "Inicia sesión nuevamente.";
                case UNAVAILABLE:
                case DEADLINE_EXCEEDED: return "Sin conexión con Firestore. Revisa internet e intenta otra vez.";
                case NOT_FOUND: return "El proyecto ya no existe.";
                default: return "No se pudo completar la operación en Firestore.";
            }
        }
        return "No se pudo completar la operación. Intenta nuevamente.";
    }
}
