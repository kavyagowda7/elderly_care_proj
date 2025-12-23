package com.example.elderlycare;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;

import java.text.SimpleDateFormat;
import java.util.*;

public class DocumentsListActivity extends AppCompatActivity {

    RecyclerView recycler;
    DocumentsAdapter adapter;

    // Used by adapter (String headers + DocumentModel)
    List<Object> items = new ArrayList<>();

    // Used for filtering
    List<DocumentModel> allDocs = new ArrayList<>();

    Spinner spinnerFilter;
    TextView tvUserName;

    String uid;
    boolean isReadOnly = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_documents_list);

        // 🔐 AUTH CHECK
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            finish();
            return;
        }
        // Check if opened for family member
        Intent i = getIntent();
        if (i != null && i.hasExtra("elderlyId")) {
            uid = i.getStringExtra("elderlyId");
            isReadOnly = true;
        } else {
            uid = user.getUid();
        }


        // UI
        tvUserName = findViewById(R.id.tvUserName);
        spinnerFilter = findViewById(R.id.spinnerFilter);

        recycler = findViewById(R.id.recyclerDocs);
        recycler.setLayoutManager(new LinearLayoutManager(this));

        adapter = new DocumentsAdapter(this, items, isReadOnly);

        recycler.setAdapter(adapter);

        View addBtn = findViewById(R.id.btnAdd);

        if (isReadOnly) {
            addBtn.setVisibility(View.GONE);
        } else {
            addBtn.setOnClickListener(v ->
                    startActivity(new Intent(this, AddDocumentActivity.class))
            );
        }


        loadUserName();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDocuments();
    }

    // ---------------- USER NAME ----------------
    void loadUserName() {
        FirebaseUtil.db()
                .getReference("users")
                .child(uid)
                .child("name")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            tvUserName.setText(snapshot.getValue(String.class));
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {}
                });
    }

    // ---------------- LOAD DOCUMENTS ----------------
    void loadDocuments() {

        FirebaseUtil.db()
                .getReference("patient_documents")
                .child(uid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot s) {

                        allDocs.clear();

                        for (DataSnapshot x : s.getChildren()) {
                            DocumentModel m = x.getValue(DocumentModel.class);
                            if (m != null) {
                                if (m.date == null || m.date.trim().isEmpty()) {
                                    m.date = "1970-01-01"; // safety
                                }
                                allDocs.add(m);
                            }
                        }

                        setupHealthFilter();
                        buildList(allDocs);
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {}
                });
    }

    // ---------------- HEALTH ISSUE FILTER ----------------
    void setupHealthFilter() {
        List<String> issues = new ArrayList<>();
        issues.add("Show all");

        for (DocumentModel d : allDocs) {
            if (d.healthTags != null) {
                for (String tag : d.healthTags) {
                    if (!issues.contains(tag)) {
                        issues.add(tag);
                    }
                }
            }
        }

        spinnerFilter.setAdapter(new android.widget.ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                issues
        ));

        spinnerFilter.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent,
                                       android.view.View view, int position, long id) {
                String selected = parent.getItemAtPosition(position).toString();
                filterByHealthIssue(selected);
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
    }

    void filterByHealthIssue(String issue) {
        List<DocumentModel> filtered = new ArrayList<>();

        for (DocumentModel d : allDocs) {
            if (issue.equals("Show all") ||
                    (d.healthTags != null && d.healthTags.contains(issue))) {
                filtered.add(d);
            }
        }

        buildList(filtered);
    }

    // ---------------- BUILD LIST WITH DATE HEADERS ----------------
    void buildList(List<DocumentModel> docs) {

        // Sort newest first
        Collections.sort(docs, (a, b) -> Long.compare(b.timestamp, a.timestamp));

        items.clear();

        Map<String, List<DocumentModel>> grouped =
                new TreeMap<>(Collections.reverseOrder());

        for (DocumentModel d : docs) {
            grouped.computeIfAbsent(d.date, k -> new ArrayList<>()).add(d);
        }

        for (String date : grouped.keySet()) {
            items.add(formatDateHeader(date));
            items.addAll(grouped.get(date));
        }

        adapter.notifyDataSetChanged();
    }

    private String formatDateHeader(String raw) {
        try {
            SimpleDateFormat in = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat out = new SimpleDateFormat("yyyy-MM-dd • EEEE", Locale.getDefault());
            return out.format(in.parse(raw));
        } catch (Exception e) {
            return raw;
        }
    }
}
