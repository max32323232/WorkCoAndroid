package cl.workco.app;

import android.os.Bundle;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class ComunidadActivity extends BaseActivity {
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        prepararPantalla(R.layout.activity_comunidad);
        ComunidadAdapter adapter = new ComunidadAdapter();
        RecyclerView list = findViewById(R.id.listComunidad);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);
        findViewById(R.id.btnVolver).setOnClickListener(v -> finish());
    }
}