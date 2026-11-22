package cl.workco.app;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.Source;
import java.util.HashMap;
import java.util.Map;

public class ProyectoRepositorio {
    public static final String COLLECTION_NAME = "proyectos";
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public String nuevoId() {
        return db.collection(COLLECTION_NAME).document().getId();
    }

    public Query consultaPropios(String uid) {
        return db.collection(COLLECTION_NAME).whereEqualTo("userId", uid);
    }

    public Task<DocumentSnapshot> obtener(String id) {
        return db.collection(COLLECTION_NAME).document(id).get(Source.SERVER);
    }

    public Task<Void> guardar(Proyecto p, boolean editar) {
        if (FirebaseAuth.getInstance().getCurrentUser() == null
                || !FirebaseAuth.getInstance().getCurrentUser().getUid().equals(p.getUserId())) {
            return Tasks.forException(new SecurityException("Sesión inválida"));
        }
        if (!editar) {
            return db.collection(COLLECTION_NAME).document(p.getId()).set(p);
        }
        Map<String, Object> cambios = new HashMap<>();
        cambios.put("titulo", p.getTitulo());
        cambios.put("descripcion", p.getDescripcion());
        cambios.put("categoria", p.getCategoria());
        cambios.put("estado", p.getEstado());
        return db.collection(COLLECTION_NAME).document(p.getId()).update(cambios);
    }

    public Task<Void> eliminar(String id) {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            return Tasks.forException(new SecurityException("Sesión inválida"));
        }
        return db.collection(COLLECTION_NAME).document(id).delete();
    }
}
