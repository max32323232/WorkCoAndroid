package cl.workco.app;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class ProyectoAdapter extends RecyclerView.Adapter<ProyectoAdapter.Fila> {
    public interface Acciones {
        void editar(Proyecto proyecto);
        void eliminar(Proyecto proyecto);
    }
    private final List<Proyecto> datos = new ArrayList<>();
    private final Acciones acciones;

    public ProyectoAdapter(Acciones acciones) { this.acciones = acciones; }

    public void mostrar(List<Proyecto> proyectos) {
        datos.clear(); datos.addAll(proyectos); notifyDataSetChanged();
    }

    @NonNull @Override public Fila onCreateViewHolder(@NonNull ViewGroup parent, int tipo) {
        return new Fila(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_proyecto, parent, false));
    }

    @Override public void onBindViewHolder(@NonNull Fila fila, int pos) {
        Proyecto p = datos.get(pos);
        fila.titulo.setText(p.getTitulo());
        fila.descripcion.setText(p.getDescripcion());
        fila.detalle.setText(p.getCategoria() + " · " + p.getEstado());
        fila.itemView.findViewById(R.id.edit).setOnClickListener(v -> acciones.editar(p));
        fila.itemView.findViewById(R.id.delete).setOnClickListener(v -> acciones.eliminar(p));
    }

    @Override public int getItemCount() { return datos.size(); }

    static class Fila extends RecyclerView.ViewHolder {
        final TextView titulo, descripcion, detalle;
        Fila(View view) {
            super(view);
            titulo = view.findViewById(R.id.title);
            descripcion = view.findViewById(R.id.description);
            detalle = view.findViewById(R.id.details);
        }
    }
}
