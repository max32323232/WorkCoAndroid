package cl.workco.app;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

public class ProyectoFormActivity extends BaseActivity {
    private final ProyectoRepositorio repo = new ProyectoRepositorio();
    private EditText titulo, descripcion, categoria;
    private Spinner estado;
    private String id;
    private boolean editar, cargado, guardando;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        prepararPantalla(R.layout.activity_proyecto_form);
        titulo = findViewById(R.id.title); descripcion = findViewById(R.id.description);
        categoria = findViewById(R.id.category); estado = findViewById(R.id.status);
        String recibido = getIntent().getStringExtra("proyectoId");
        editar = recibido != null;
        id = editar ? recibido : state != null ? state.getString("id") : repo.nuevoId();
        cargado = !editar || state != null && state.getBoolean("cargado");
        ((TextView) findViewById(R.id.heading)).setText(editar ? R.string.edit_project : R.string.new_project);
        findViewById(R.id.save).setOnClickListener(v -> guardar());
        findViewById(R.id.cancel).setOnClickListener(v -> finish());
    }

    @Override protected void onStart() {
        super.onStart();
        guardando = false;
        if (!exigirSesion()) return;
        if (!cargado) cargar(); else ocupado(false);
    }

    private void cargar() {
        ocupado(true);
        ((TextView) findViewById(R.id.message)).setText("Cargando proyecto desde el servidor…");
        repo.obtener(id).addOnCompleteListener(this, task -> {
            if (!task.isSuccessful()) { avisar(ErroresFirebase.mensaje(task.getException())); finish(); return; }
            Proyecto p = task.getResult().toObject(Proyecto.class);
            if (p == null || auth.getCurrentUser() == null
                    || !auth.getCurrentUser().getUid().equals(p.getUserId())) {
                avisar("Proyecto inexistente o sin permiso."); finish(); return;
            }
            titulo.setText(p.getTitulo()); descripcion.setText(p.getDescripcion()); categoria.setText(p.getCategoria());
            String[] estados = getResources().getStringArray(R.array.states);
            for (int i = 0; i < estados.length; i++) if (estados[i].equals(p.getEstado())) estado.setSelection(i);
            cargado = true; ocupado(false);
            ((TextView) findViewById(R.id.message)).setText("");
        });
    }

    private void guardar() {
        if (guardando || !cargado || !exigirSesion()) return;
        String t = titulo.getText().toString().trim();
        String d = descripcion.getText().toString().trim();
        String c = categoria.getText().toString().trim();
        if (!validar(titulo, t, 80) || !validar(descripcion, d, 500) || !validar(categoria, c, 40)) return;
        if (!ErroresFirebase.hayInternet(this)) { avisar("Sin conexión. Conecta a internet para guardar."); return; }
        Proyecto p = new Proyecto(id, auth.getCurrentUser().getUid(), t, d, c, estado.getSelectedItem().toString());
        guardando = true; ocupado(true);
        ((TextView) findViewById(R.id.message)).setText("Enviando. Si se corta internet, espera a reconectar. El éxito se confirma al recibir respuesta del servidor.");
        repo.guardar(p, editar).addOnCompleteListener(this, task -> {
            guardando = false; ocupado(false);
            if (task.isSuccessful()) { avisar("Proyecto guardado en el servidor."); finish(); }
            else ((TextView) findViewById(R.id.message)).setText(ErroresFirebase.mensaje(task.getException()));
        });
    }

    private boolean validar(EditText campo, String texto, int max) {
        if (texto.isEmpty() || texto.length() > max) {
            campo.setError("Obligatorio: entre 1 y " + max + " caracteres"); campo.requestFocus(); return false;
        }
        return true;
    }

    private void ocupado(boolean valor) {
        findViewById(R.id.progress).setVisibility(valor ? View.VISIBLE : View.GONE);
        findViewById(R.id.save).setEnabled(!valor && cargado);
        findViewById(R.id.cancel).setEnabled(!guardando);
        titulo.setEnabled(!valor); descripcion.setEnabled(!valor);
        categoria.setEnabled(!valor); estado.setEnabled(!valor);
    }

    @Override protected void onSaveInstanceState(Bundle out) {
        out.putString("id", id); out.putBoolean("cargado", cargado);
        super.onSaveInstanceState(out);
    }
}
