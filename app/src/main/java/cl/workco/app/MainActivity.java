package cl.workco.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.MetadataChanges;
import java.util.ArrayList;
import java.util.Collections;

public class MainActivity extends BaseActivity implements ProyectoAdapter.Acciones {
    private final ProyectoRepositorio repo = new ProyectoRepositorio();
    private ProyectoAdapter adapter;
    private ListenerRegistration listener;
    private String uid;
    private boolean eliminando;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        prepararPantalla(R.layout.activity_main);
        adapter = new ProyectoAdapter(this);
        RecyclerView list = findViewById(R.id.list);
        list.setLayoutManager(new LinearLayoutManager(this)); list.setAdapter(adapter);
        findViewById(R.id.add).setOnClickListener(v -> {
            if (exigirSesion()) startActivity(new Intent(this, ProyectoFormActivity.class));
        });
        findViewById(R.id.retry).setOnClickListener(v -> escuchar());
        findViewById(R.id.logout).setOnClickListener(v -> {
            detenerEscucha(); adapter.mostrar(new ArrayList<>());
            auth.signOut(); navegar(LoginActivity.class);
        });
    }

    @Override protected void onStart() {
        super.onStart();
        eliminando = false;
        if (!exigirSesion()) return;
        uid = auth.getCurrentUser().getUid();
        ((TextView) findViewById(R.id.account)).setText(auth.getCurrentUser().getEmail());
        escuchar();
    }

    private void escuchar() {
        if (!exigirSesion()) return;
        detenerEscucha();
        findViewById(R.id.progress).setVisibility(View.VISIBLE);
        findViewById(R.id.retry).setVisibility(View.GONE);
        listener = repo.consultaPropios(uid).addSnapshotListener(MetadataChanges.INCLUDE, (snapshot, error) -> {
            findViewById(R.id.progress).setVisibility(View.GONE);
            TextView sync = findViewById(R.id.syncState);
            if (error != null) {
                adapter.mostrar(new ArrayList<>());
                findViewById(R.id.empty).setVisibility(View.GONE);
                sync.setText(ErroresFirebase.mensaje(error));
                findViewById(R.id.retry).setVisibility(View.VISIBLE); return;
            }
            if (snapshot == null) return;
            ArrayList<Proyecto> proyectos = new ArrayList<>();
            for (DocumentSnapshot doc : snapshot.getDocuments()) {
                Proyecto p = doc.toObject(Proyecto.class);
                if (p != null && uid.equals(p.getUserId())) {
                    p.setId(doc.getId()); proyectos.add(p);
                }
            }
            Collections.sort(proyectos, (a, b) -> a.getTitulo().compareToIgnoreCase(b.getTitulo()));
            adapter.mostrar(proyectos);
            findViewById(R.id.empty).setVisibility(proyectos.isEmpty() ? View.VISIBLE : View.GONE);
            if (snapshot.getMetadata().hasPendingWrites()) sync.setText("Cambios pendientes de confirmación del servidor.");
            else if (snapshot.getMetadata().isFromCache()) sync.setText("Vista local. Conecta a internet para confirmar los datos.");
            else sync.setText("Sincronizado · " + proyectos.size() + " proyectos");
        });
    }

    @Override public void editar(Proyecto p) {
        if (exigirSesion()) startActivity(new Intent(this, ProyectoFormActivity.class).putExtra("proyectoId", p.getId()));
    }

    @Override public void eliminar(Proyecto p) {
        if (eliminando) return;
        new MaterialAlertDialogBuilder(this).setTitle("Eliminar proyecto")
                .setMessage("¿Eliminar «" + p.getTitulo() + "»? Esta acción no se puede deshacer.")
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    if (eliminando || !exigirSesion()) return;
                    if (!ErroresFirebase.hayInternet(this)) { avisar("Sin conexión. Intenta cuando tengas internet."); return; }
                    eliminando = true;
                    findViewById(R.id.progress).setVisibility(View.VISIBLE);
                    ((TextView) findViewById(R.id.syncState)).setText("Eliminando. Si se corta internet, espera a reconectar.");
                    repo.eliminar(p.getId()).addOnCompleteListener(this, task -> {
                        eliminando = false;
                        findViewById(R.id.progress).setVisibility(View.GONE);
                        avisar(task.isSuccessful() ? "Proyecto eliminado en el servidor." : ErroresFirebase.mensaje(task.getException()));
                    });
                }).show();
    }

    private void detenerEscucha() {
        if (listener != null) { listener.remove(); listener = null; }
    }

    @Override protected void onStop() { detenerEscucha(); super.onStop(); }
}
