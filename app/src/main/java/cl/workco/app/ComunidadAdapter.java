package cl.workco.app;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ComunidadAdapter extends RecyclerView.Adapter<ComunidadAdapter.Fila> {
    private final List<Proyecto> datos = new ArrayList<>();
    private Map<String, String> autores;

    public void mostrar(List<Proyecto> proyectos, Map<String, String> autores) {
        this.datos.clear();
        this.datos.addAll(proyectos);
        this.autores = autores;
        notifyDataSetChanged();
    }

    @NonNull @Override public Fila onCreateViewHolder(@NonNull ViewGroup parent, int tipo) {
        return new Fila(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_proyecto_comunidad, parent, false));
    }

    @Override public void onBindViewHolder(@NonNull Fila fila, int pos) {
        Proyecto p = datos.get(pos);
        fila.titulo.setText(p.getTitulo());
        fila.descripcion.setText(p.getDescripcion());
        fila.detalle.setText(p.getCategoria() + " · " + p.getEstado());

        String nombre = autores != null ? autores.get(p.getUserId()) : null;
        if (nombre == null || nombre.trim().isEmpty()) {
            String uid = p.getUserId();
            nombre = uid != null && uid.length() > 8 ? uid.substring(0, 8) + "…" : (uid != null ? uid : "Usuario");
        }
        fila.autor.setText("Autor: " + nombre);
    }

    @Override public int getItemCount() { return datos.size(); }

    static class Fila extends RecyclerView.ViewHolder {
        final TextView titulo, descripcion, detalle, autor;
        Fila(View view) {
            super(view);
            titulo = view.findViewById(R.id.title);
            descripcion = view.findViewById(R.id.description);
            detalle = view.findViewById(R.id.details);
            autor = view.findViewById(R.id.author);
        }
    }
}