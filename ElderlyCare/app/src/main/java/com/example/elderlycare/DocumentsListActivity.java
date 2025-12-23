package com.example.elderlycare;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Spinner;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.text.SimpleDateFormat;
import java.util.*;

public class DocumentsListActivity extends AppCompatActivity {

    RecyclerView recycler;
    DocumentsAdapter adapter;
    List<Object> items = new ArrayList<>();
    String uid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_documents_list);

        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        recycler = findViewById(R.id.recyclerDocs);
        recycler.setLayoutManager(new LinearLayoutManager(this));

        adapter = new DocumentsAdapter(this, items);
        recycler.setAdapter(adapter);

        findViewById(R.id.btnAdd).setOnClickListener(v ->
                startActivity(new Intent(this, AddDocumentActivity.class))
        );
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDocuments();
    }

    void loadDocuments() {
        FirebaseDatabase db = FirebaseDatabase.getInstance(
                "https://elderlycare-7fc2a-default-rtdb.asia-southeast1.firebasedatabase.app/"
        );
        db.getReference("patient_documents").child(uid)

                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot s) {

                        List<DocumentModel> docs = new ArrayList<>();
                        for (DataSnapshot x : s.getChildren()) {
                            DocumentModel m = x.getValue(DocumentModel.class);
                            if (m != null) docs.add(m);
                        }

                        // Sort newest first
                        Collections.sort(docs, (a, b) -> Long.compare(b.timestamp, a.timestamp));

                        items.clear();

                        // Group by date
                        Map<String, List<DocumentModel>> grouped = new TreeMap<>(Collections.reverseOrder());
                        for (DocumentModel m : docs) {
                            grouped.computeIfAbsent(m.date, k -> new ArrayList<>()).add(m);
                        }

                        for (String k : grouped.keySet()) {
                            items.add(formatDateHeader(k));
                            items.addAll(grouped.get(k));
                        }

                        adapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {}
                });
    }

    private String formatDateHeader(String raw) {
        try {
            SimpleDateFormat in = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat out = new SimpleDateFormat("MMM dd, yyyy • EEEE", Locale.getDefault());
            return out.format(in.parse(raw));
        } catch (Exception e) {
            return raw;
        }
    }
}
