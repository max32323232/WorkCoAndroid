package cl.workco.app;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.SetOptions;
import java.util.Collections;

/** Acceso a Firestore para el perfil: documento usuarios/{uid}. */
public class PerfilRepositorio {
    public static final String COLLECTION_NAME = "usuarios";
    public static final int MAX_DESCRIPCION = 300;
    public static final int MAX_HABILIDADES = 20;

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    private DocumentReference doc(String uid) {
        return db.collection(COLLECTION_NAME).document(uid);
    }

    // Misma verificación de sesión que usa ProyectoRepositorio
    private boolean sesionValida(String uid) {
        FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
        return u != null && u.getUid().equals(uid);
    }

    /** Escucha el documento en tiempo real. Quien llama debe hacer remove() del registro. */
    public ListenerRegistration escuchar(String uid, EventListener<DocumentSnapshot> listener) {
        return doc(uid).addSnapshotListener(listener);
    }

    /** merge: crea el documento si no existe y no pisa los demás campos. */
    public Task<Void> guardarDescripcion(String uid, String descripcion) {
        if (!sesionValida(uid)) return Tasks.forException(new SecurityException("Sesión inválida"));
        return doc(uid).set(Collections.singletonMap("descripcion", descripcion), SetOptions.merge());
    }

    /** arrayUnion es atómico: no hay que leer la lista antes de modificarla. */
    public Task<Void> agregarHabilidad(String uid, String habilidad) {
        if (!sesionValida(uid)) return Tasks.forException(new SecurityException("Sesión inválida"));
        return doc(uid).set(Collections.singletonMap("habilidades", FieldValue.arrayUnion(habilidad)),
                SetOptions.merge());
    }

    public Task<Void> quitarHabilidad(String uid, String habilidad) {
        if (!sesionValida(uid)) return Tasks.forException(new SecurityException("Sesión inválida"));
        return doc(uid).set(Collections.singletonMap("habilidades", FieldValue.arrayRemove(habilidad)),
                SetOptions.merge());
    }
}
