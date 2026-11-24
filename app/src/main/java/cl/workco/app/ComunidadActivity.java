package cl.workco.app;

import android.os.Bundle;
import android.view.View;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.firestore.DocumentSnapshot;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class ComunidadActivity extends BaseActivity {
    private final ComunidadRepositorio repo = new ComunidadRepositorio();
    private ComunidadAdapter adapter;
    private String uid;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        prepararPantalla(R.layout.activity_comunidad);
        adapter = new ComunidadAdapter();
        RecyclerView list = findViewById(R.id.listComunidad);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);
        findViewById(R.id.btnVolver).setOnClickListener(v -> finish());
    }

    @Override protected void onStart() {
        super.onStart();
        if (!exigirSesion()) return;
        uid = auth.getCurrentUser().getUid();
        cargar();
    }

    private void cargar() {
        findViewById(R.id.progress).setVisibility(View.VISIBLE);
        findViewById(R.id.tvVacio).setVisibility(View.GONE);

        repo.consultaTodos().get().addOnCompleteListener(this, task -> {
            if (!task.isSuccessful()) {
                findViewById(R.id.progress).setVisibility(View.GONE);
                avisar(ErroresFirebase.mensaje(task.getException()));
                return;
            }
            ArrayList<Proyecto> proyectos = new ArrayList<>();
            Set<String> uidsAutores = new HashSet<>();
            for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                Proyecto p = doc.toObject(Proyecto.class);
                if (p == null) continue;
                p.setId(doc.getId());
                if (uid != null && uid.equals(p.getUserId())) continue; // excluir propios
                proyectos.add(p);
                if (p.getUserId() != null) uidsAutores.add(p.getUserId());
            }
            Collections.sort(proyectos, (a, b) ->
                    Objects.toString(a.getTitulo(), "").compareToIgnoreCase(Objects.toString(b.getTitulo(), "")));
            cargarAutores(proyectos, uidsAutores);
        });
    }

    private void cargarAutores(List<Proyecto> proyectos, Set<String> uids) {
        Map<String, String> autores = new HashMap<>();
        if (uids.isEmpty()) { mostrar(proyectos, autores); return; }

        int[] pendientes = { uids.size() };
        for (String u : uids) {
            repo.autor(u).addOnCompleteListener(this, task -> {
                if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                    String nombre = task.getResult().getString("nombre");
                    if (nombre != null && !nombre.trim().isEmpty()) autores.put(u, nombre.trim());
                }
                if (--pendientes[0] == 0) mostrar(proyectos, autores);
            });
        }
    }

    private void mostrar(List<Proyecto> proyectos, Map<String, String> autores) {
        findViewById(R.id.progress).setVisibility(View.GONE);
        adapter.mostrar(proyectos, autores);
        findViewById(R.id.tvVacio).setVisibility(proyectos.isEmpty() ? View.VISIBLE : View.GONE);
    }
}