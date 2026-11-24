package cl.workco.app;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

// Extiende BaseActivity (sin modificarla): obtenemos prepararPantalla(), exigirSesion(), avisar()
public class PerfilActivity extends BaseActivity implements ProyectoAdapter.Acciones {
    private static final long RETARDO_GUARDADO_MS = 800;   // debounce del autoguardado

    // Reutilizamos el repositorio de proyectos tal cual
    private final ProyectoRepositorio proyectoRepo = new ProyectoRepositorio();
    private final PerfilRepositorio perfilRepo = new PerfilRepositorio();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable tareaGuardado = this::guardarDescripcionAhora;

    private TextView tvNombre, tvEstadoGuardado, tvSinHabilidades, tvSinProyectos;
    private EditText etDescripcion, etHabilidad;
    private ChipGroup chipsHabilidades;
    private ProyectoAdapter adapter;
    private ListenerRegistration escuchaPerfil, escuchaProyectos;

    private String uid;
    private String ultimaDescripcionGuardada = "";
    private boolean cargandoDesdeServidor;   // evita que setText() programático dispare un guardado
    private boolean eliminando;
    private List<String> habilidades = new ArrayList<>();

    // ---------- Ciclo de vida ----------

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        // PASO 1: inflar layout y ajustar insets (barra de estado, teclado)
        prepararPantalla(R.layout.activity_perfil);

        // PASO 2: referencias a las vistas
        tvNombre = findViewById(R.id.tvNombre);
        tvEstadoGuardado = findViewById(R.id.tvEstadoGuardado);
        tvSinHabilidades = findViewById(R.id.tvSinHabilidades);
        tvSinProyectos = findViewById(R.id.tvSinProyectos);
        etDescripcion = findViewById(R.id.etDescripcion);
        etHabilidad = findViewById(R.id.etHabilidad);
        chipsHabilidades = findViewById(R.id.chipsHabilidades);

        // PASO 3: RecyclerView con el ProyectoAdapter existente
        adapter = new ProyectoAdapter(this);
        RecyclerView lista = findViewById(R.id.listaProyectos);
        lista.setLayoutManager(new LinearLayoutManager(this));
        lista.setAdapter(adapter);

        // PASO 4: autoguardado de la descripción
        // (a) con retardo: cada tecla reinicia el temporizador; guarda solo al dejar de escribir
        etDescripcion.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(Editable s) {
                if (cargandoDesdeServidor) return;
                handler.removeCallbacks(tareaGuardado);
                handler.postDelayed(tareaGuardado, RETARDO_GUARDADO_MS);
            }
        });
        // (b) al perder el foco: guarda de inmediato sin esperar el retardo
        etDescripcion.setOnFocusChangeListener((v, tieneFoco) -> {
            if (!tieneFoco) guardarDescripcionAhora();
        });

        // PASO 5: gestión de habilidades (botón o tecla "Listo" del teclado)
        findViewById(R.id.btnAgregarHabilidad).setOnClickListener(v -> agregarHabilidad());
        etHabilidad.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) { agregarHabilidad(); return true; }
            return false;
        });

        // PASO 6: volver a la pantalla principal
        findViewById(R.id.btnVolver).setOnClickListener(v -> finish());
    }

    @Override protected void onStart() {
        super.onStart();
        eliminando = false;
        if (!exigirSesion()) return;
        uid = auth.getCurrentUser().getUid();
        tvNombre.setText(nombreParaMostrar(null));   // valor inicial mientras llega Firestore
        escuchar();
    }

    @Override protected void onStop() {
        // Si el usuario sale con un cambio pendiente de enviar, lo enviamos ahora
        guardarDescripcionAhora();
        detenerEscuchas();
        super.onStop();
    }

    @Override protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    // ---------- Carga en tiempo real ----------

    private void escuchar() {
        detenerEscuchas();

        // Perfil: usuarios/{uid}. Cada cambio (local o remoto) vuelve a pintar la pantalla.
        escuchaPerfil = perfilRepo.escuchar(uid, (snap, error) -> {
            if (error != null) { tvEstadoGuardado.setText(ErroresFirebase.mensaje(error)); return; }
            if (snap == null) return;

            tvNombre.setText(nombreParaMostrar(snap.getString("nombre")));

            // No pisamos lo que el usuario está escribiendo en este momento
            String remota = snap.getString("descripcion");
            if (remota == null) remota = "";
            if (!etDescripcion.hasFocus() && !remota.equals(etDescripcion.getText().toString())) {
                cargandoDesdeServidor = true;
                etDescripcion.setText(remota);
                cargandoDesdeServidor = false;
                ultimaDescripcionGuardada = remota;
            }

            // snap.get() devuelve Object: filtramos para evitar un cast sin verificar
            List<String> nuevas = new ArrayList<>();
            Object crudo = snap.get("habilidades");
            if (crudo instanceof List) {
                for (Object o : (List<?>) crudo) if (o instanceof String) nuevas.add((String) o);
            }
            mostrarHabilidades(nuevas);
        });

        // Proyectos: misma consulta que MainActivity (whereEqualTo userId), sin tocar el repositorio
        escuchaProyectos = proyectoRepo.consultaPropios(uid).addSnapshotListener((snapshot, error) -> {
            if (error != null) { adapter.mostrar(new ArrayList<>()); avisar(ErroresFirebase.mensaje(error)); return; }
            if (snapshot == null) return;
            ArrayList<Proyecto> proyectos = new ArrayList<>();
            for (DocumentSnapshot doc : snapshot.getDocuments()) {
                Proyecto p = doc.toObject(Proyecto.class);
                if (p != null && uid.equals(p.getUserId())) {
                    p.setId(doc.getId());   // el id es @Exclude: hay que asignarlo a mano
                    proyectos.add(p);
                }
            }
            Collections.sort(proyectos, (a, b) ->
                    Objects.toString(a.getTitulo(), "").compareToIgnoreCase(Objects.toString(b.getTitulo(), "")));
            adapter.mostrar(proyectos);
            tvSinProyectos.setVisibility(proyectos.isEmpty() ? View.VISIBLE : View.GONE);
        });
    }

    private void detenerEscuchas() {
        if (escuchaPerfil != null) { escuchaPerfil.remove(); escuchaPerfil = null; }
        if (escuchaProyectos != null) { escuchaProyectos.remove(); escuchaProyectos = null; }
    }

    // ---------- Guardado automático ----------

    private void guardarDescripcionAhora() {
        handler.removeCallbacks(tareaGuardado);
        // No usamos exigirSesion() aquí: podríamos estar en onStop y no queremos navegar
        if (uid == null || auth.getCurrentUser() == null) return;

        String texto = etDescripcion.getText().toString().trim();
        if (texto.equals(ultimaDescripcionGuardada)) return;   // sin cambios reales: ahorra escrituras

        String anterior = ultimaDescripcionGuardada;
        ultimaDescripcionGuardada = texto;
        tvEstadoGuardado.setText(R.string.perfil_guardando);
        perfilRepo.guardarDescripcion(uid, texto).addOnCompleteListener(this, task -> {
            if (task.isSuccessful()) {
                tvEstadoGuardado.setText(R.string.perfil_guardado);
            } else {
                ultimaDescripcionGuardada = anterior;   // permite reintentar en el próximo cambio
                tvEstadoGuardado.setText(ErroresFirebase.mensaje(task.getException()));
            }
        });
    }

    // ---------- Habilidades ----------

    private void agregarHabilidad() {
        String h = etHabilidad.getText().toString().trim();
        if (h.isEmpty()) return;
        for (String existente : habilidades) {
            if (existente.equalsIgnoreCase(h)) {
                etHabilidad.setError(getString(R.string.perfil_habilidad_repetida)); return;
            }
        }
        if (habilidades.size() >= PerfilRepositorio.MAX_HABILIDADES) {
            avisar(getString(R.string.perfil_limite_habilidades, PerfilRepositorio.MAX_HABILIDADES)); return;
        }
        etHabilidad.setText("");
        // No tocamos la UI aquí: el listener del perfil recibe el cambio local al instante
        perfilRepo.agregarHabilidad(uid, h)
                .addOnFailureListener(this, e -> avisar(ErroresFirebase.mensaje(e)));
    }

    private void quitarHabilidad(String h) {
        perfilRepo.quitarHabilidad(uid, h)
                .addOnFailureListener(this, e -> avisar(ErroresFirebase.mensaje(e)));
    }

    private void mostrarHabilidades(List<String> nuevas) {
        habilidades = nuevas;
        chipsHabilidades.removeAllViews();
        for (String h : nuevas) {
            Chip chip = new Chip(this);
            chip.setText(h);
            chip.setCloseIconVisible(true);
            chip.setCloseIconContentDescription(getString(R.string.perfil_quitar_habilidad, h));
            chip.setOnCloseIconClickListener(v -> quitarHabilidad(h));
            chipsHabilidades.addView(chip);
        }
        tvSinHabilidades.setVisibility(nuevas.isEmpty() ? View.VISIBLE : View.GONE);
    }

    // ---------- Nombre ----------

    // Prioridad: nombre guardado en Firestore > displayName de Auth > parte local del correo
    private String nombreParaMostrar(String guardado) {
        if (guardado != null && !guardado.trim().isEmpty()) return guardado.trim();
        FirebaseUser u = auth.getCurrentUser();
        if (u != null && u.getDisplayName() != null && !u.getDisplayName().trim().isEmpty())
            return u.getDisplayName().trim();
        String correo = u != null ? u.getEmail() : null;
        if (correo != null && correo.contains("@")) return correo.substring(0, correo.indexOf('@'));
        return "Usuario";
    }

    // ---------- ProyectoAdapter.Acciones (reutilización del adapter existente) ----------

    @Override public void editar(Proyecto p) {
        if (exigirSesion())
            startActivity(new Intent(this, ProyectoFormActivity.class).putExtra("proyectoId", p.getId()));
    }

    @Override public void eliminar(Proyecto p) {
        if (eliminando) return;
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.perfil_eliminar_titulo)
                .setMessage(getString(R.string.perfil_eliminar_mensaje, p.getTitulo()))
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    if (eliminando || !exigirSesion()) return;
                    if (!ErroresFirebase.hayInternet(this)) { avisar(getString(R.string.perfil_sin_conexion)); return; }
                    eliminando = true;
                    proyectoRepo.eliminar(p.getId()).addOnCompleteListener(this, task -> {
                        eliminando = false;
                        avisar(task.isSuccessful() ? getString(R.string.perfil_proyecto_eliminado)
                                : ErroresFirebase.mensaje(task.getException()));
                    });
                }).show();
    }
}
