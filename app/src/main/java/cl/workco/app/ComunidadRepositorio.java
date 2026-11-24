package cl.workco.app;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

/**
 * Repositorio de solo lectura para la vista de comunidad.
 * No modifica nada de ProyectoRepositorio ni PerfilRepositorio.
 */
public class ComunidadRepositorio {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    /** Todos los proyectos (el filtro "no propios" se hace en la vista). */
    public Query consultaTodos() {
        return db.collection("proyectos");
    }

    /** Lee el documento del autor para intentar mostrar su nombre. */
    public Task<DocumentSnapshot> autor(String uid) {
        return db.collection("usuarios").document(uid).get();
    }
}