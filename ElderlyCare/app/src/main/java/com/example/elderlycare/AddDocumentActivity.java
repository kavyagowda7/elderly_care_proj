package com.example.elderlycare;

import android.Manifest;
import android.app.DatePickerDialog;
import android.content.ClipData;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import org.json.JSONObject;

import java.io.File;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.*;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.BufferedSink;
import okio.Okio;

public class AddDocumentActivity extends AppCompatActivity {

    static final int RF = 1001, RI = 1002, RP = 2001;
    static final String CN = "du8sonnyf";
    static final String UP = "unsigned_preset";

    TextView tvDate, tvSelectedFile;
    Spinner spinnerDocType;
    EditText etTitle, etNotes;
    Button btnAttachFile, btnAttachPhoto;
    ImageView btnSave, btnBack, btnAddTag;
    LinearLayout tagContainer;

    List<Uri> pickedUris = new ArrayList<>();
    List<String> pickedNames = new ArrayList<>();
    List<String> pickedSizes = new ArrayList<>();
    List<String> healthTags = new ArrayList<>();

    String selectedDate = "";
    int lastPicker = -1;
    String docId = "";


    OkHttpClient client = new OkHttpClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_document);

        tvDate = findViewById(R.id.tvSelectedDate);
        tvSelectedFile = findViewById(R.id.tvSelectedFile);
        spinnerDocType = findViewById(R.id.spinnerDocType);
        etTitle = findViewById(R.id.etTitle);
        etNotes = findViewById(R.id.etNotes);
        btnAttachFile = findViewById(R.id.btnAttachFile);
        btnAttachPhoto = findViewById(R.id.btnAttachPhoto);
        btnSave = findViewById(R.id.btnSaveDocument);
        btnBack = findViewById(R.id.btnBack);
        btnAddTag = findViewById(R.id.btnAddHealthTag);
        tagContainer = findViewById(R.id.healthTagContainer);

        spinnerDocType.setAdapter(new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                new String[]{"None", "Lab Report", "Prescription", "Imaging", "Other"}
        ));

        selectedDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        tvDate.setText(new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date()));

        findViewById(R.id.datePickerLayout).setOnClickListener(v -> pickDate());

        btnAttachFile.setOnClickListener(v -> { lastPicker = RF; requestPermissionAndOpen(RF); });
        btnAttachPhoto.setOnClickListener(v -> { lastPicker = RI; requestPermissionAndOpen(RI); });

        btnBack.setOnClickListener(v -> finish());
        btnSave.setOnClickListener(v -> onSaveClicked());

        btnAddTag.setOnClickListener(v -> openTagSelector());
    }

    // ----------------------------------------------------------
    // DATE PICKER
    // ----------------------------------------------------------
    void pickDate() {
        Calendar c = Calendar.getInstance();
        new DatePickerDialog(this, (view, y, m, d) -> {
            Calendar cc = Calendar.getInstance();
            cc.set(y, m, d);
            selectedDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cc.getTime());
            tvDate.setText(new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(cc.getTime()));
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    // ----------------------------------------------------------
    // PERMISSION REQUEST
    // ----------------------------------------------------------
    void requestPermissionAndOpen(int pickerType) {
        List<String> toAsk = new ArrayList<>();

        if (Build.VERSION.SDK_INT >= 33)
            toAsk.add(Manifest.permission.READ_MEDIA_IMAGES);
        else
            toAsk.add(Manifest.permission.READ_EXTERNAL_STORAGE);

        if (pickerType == RI)
            toAsk.add(Manifest.permission.CAMERA);

        List<String> needed = new ArrayList<>();
        for (String p : toAsk)
            if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED)
                needed.add(p);

        if (!needed.isEmpty()) {
            ActivityCompat.requestPermissions(this, needed.toArray(new String[0]), RP);
        } else {
            openPicker(pickerType);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] p, @NonNull int[] g) {
        super.onRequestPermissionsResult(requestCode, p, g);

        boolean granted = true;
        for (int x : g)
            if (x != PackageManager.PERMISSION_GRANTED)
                granted = false;

        if (!granted) {
            Toast.makeText(this, "Permissions required", Toast.LENGTH_SHORT).show();
            return;
        }

        if (lastPicker != -1) openPicker(lastPicker);
    }

    // ----------------------------------------------------------
    // OPEN PICKER
    // ----------------------------------------------------------
    void openPicker(int type) {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);

        if (type == RF) {
            i.setType("*/*");
        } else {
            i.setType("image/*");
        }

        i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true); // MULTIPLE SELECT
        startActivityForResult(i, type);
    }

    // ----------------------------------------------------------
    // PICK RESULT
    // ----------------------------------------------------------
    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (res != RESULT_OK || data == null) return;

        pickedUris.clear();
        pickedNames.clear();
        pickedSizes.clear();

        if (data.getClipData() != null) {
            ClipData cd = data.getClipData();
            for (int i = 0; i < cd.getItemCount(); i++) {
                addUri(cd.getItemAt(i).getUri());
            }
        } else {
            addUri(data.getData());
        }

        tvSelectedFile.setText(pickedUris.size() + " file(s) selected");
    }

    void addUri(Uri uri) {
        if (uri == null) return;

        pickedUris.add(uri);

        try {
            getContentResolver().takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (Exception ignored) {}

        pickedNames.add(getFileName(uri));
        pickedSizes.add(getFileSize(uri));
    }

    // ----------------------------------------------------------
    // FILE INFO HELPERS
    // ----------------------------------------------------------
    String getFileName(Uri uri) {
        try (Cursor c = getContentResolver().query(uri,
                new String[]{MediaStore.MediaColumns.DISPLAY_NAME}, null, null, null)) {
            if (c != null && c.moveToFirst()) return c.getString(0);
        } catch (Exception ignored) {}
        return uri.getLastPathSegment();
    }

    String getFileSize(Uri uri) {
        try {
            InputStream is = getContentResolver().openInputStream(uri);
            int s = is.available();
            is.close();
            if (s < 1024) return s + " B";
            if (s < 1024 * 1024) return (s / 1024) + " KB";
            return (s / (1024 * 1024)) + " MB";
        } catch (Exception e) {
            return "";
        }
    }

    // ----------------------------------------------------------
    // HEALTH TAG SELECTOR
    // ----------------------------------------------------------
    void openTagSelector() {
        final String[] preset = {"Heart disease", "Diabetes", "Hypertension", "Asthma", "Thyroid", "Other..."};

        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle("Select Health Issue");

        b.setItems(preset, (dialog, which) -> {
            if (preset[which].equals("Other...")) {
                EditText et = new EditText(this);
                et.setInputType(InputType.TYPE_CLASS_TEXT);

                new AlertDialog.Builder(this)
                        .setTitle("Enter custom issue")
                        .setView(et)
                        .setPositiveButton("Add", (d2, w2) -> {
                            String v = et.getText().toString().trim();
                            if (!v.isEmpty()) {
                                healthTags.add(v);
                                addTagChip(v);
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            } else {
                healthTags.add(preset[which]);
                addTagChip(preset[which]);
            }
        });

        b.show();
    }

    void addTagChip(String text) {
        TextView chip = new TextView(this);
        chip.setText(text);
        chip.setPadding(22, 10, 22, 10);
        chip.setBackgroundResource(R.drawable.tag_bg);
        chip.setTextColor(getColor(android.R.color.black));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(10, 10, 10, 10);
        chip.setLayoutParams(lp);

        tagContainer.addView(chip);
    }

    // ----------------------------------------------------------
    // SAVE DOCUMENT
    // ----------------------------------------------------------
    // ------------------- SAVE DOCUMENT (replace the existing onSaveClicked) -------------------
    void onSaveClicked() {
        if (TextUtils.isEmpty(etTitle.getText().toString().trim())) {
            Toast.makeText(this, "Enter title", Toast.LENGTH_SHORT).show();
            return;
        }

        if (pickedUris.isEmpty()) {
            Toast.makeText(this, "Attach at least one file", Toast.LENGTH_SHORT).show();
            return;
        }

        // Generate docId early so Cloudinary folder matches this document
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseDatabase db = FirebaseDatabase.getInstance(
                "https://elderlycare-7fc2a-default-rtdb.asia-southeast1.firebasedatabase.app/"
        );
        DatabaseReference ref = db.getReference("patient_documents").child(uid);

        // If docId already present (unlikely) keep it, otherwise create
        if (docId == null || docId.isEmpty()) {
            docId = ref.push().getKey();  // IMPORTANT: this docId will be used both for Cloudinary folder and the DB key
        }

        if (docId == null || docId.isEmpty()) {
            // fallback, should not happen, but protect against null
            Toast.makeText(this, "Could not create document id. Try again.", Toast.LENGTH_LONG).show();
            return;
        }

        // debug so you can see folder being used
        Toast.makeText(this, "Saving under folder: patient_documents/" + docId, Toast.LENGTH_SHORT).show();

        uploadAllFiles();
    }


    // ------------------- SAVE TO FIREBASE (replace the existing saveToFirebase) -------------------
    void saveToFirebase(List<String> urls, List<String> names, List<String> sizes) {

        if (urls == null || urls.isEmpty()) {
            Toast.makeText(this, "Upload failed — no files saved.", Toast.LENGTH_LONG).show();
            return;
        }

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("patient_documents")
                .child(uid);

        // VERY IMPORTANT: use the SAME docId used for Cloudinary
        if (docId == null || docId.isEmpty()) {
            Toast.makeText(this, "docId missing!", Toast.LENGTH_LONG).show();
            return;
        }

        DocumentModel doc = new DocumentModel();
        doc.id = docId;
        doc.date = selectedDate;
        doc.type = spinnerDocType.getSelectedItem().toString();
        doc.title = etTitle.getText().toString();
        doc.notes = etNotes.getText().toString();
        doc.healthTags = healthTags;
        doc.fileUrls = urls;
        doc.fileNames = names;
        doc.fileSizes = sizes;
        doc.timestamp = System.currentTimeMillis();

        ref.child(docId).setValue(doc)   // <-- IMPORTANT: use docId here
                .addOnSuccessListener(v -> {

                    Toast.makeText(this, "Uploaded Successfully!", Toast.LENGTH_SHORT).show();

                    Intent i = new Intent(AddDocumentActivity.this, DocumentsListActivity.class);
                    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(i);
                    finish();

                })
                .addOnFailureListener(e -> {
                    Log.e("FIREBASE", "ERROR: " + e.getMessage());
                    Toast.makeText(this, "Save failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }




    void uploadAllFiles() {
        Toast.makeText(this, "Uploading...", Toast.LENGTH_SHORT).show();

        new Thread(() -> {
            try {
                List<String> urls = new ArrayList<>();
                List<String> names = new ArrayList<>();
                List<String> sizes = new ArrayList<>();

                for (int i = 0; i < pickedUris.size(); i++) {
                    Uri u = pickedUris.get(i);

                    String originalName = pickedNames.get(i);
                    String originalSize = pickedSizes.get(i);

                    InputStream is = getContentResolver().openInputStream(u);

                    // FIX: prefix must be >= 3 chars
                    File tmp = File.createTempFile("upld", ".tmp", getCacheDir());

                    BufferedSink sink = Okio.buffer(Okio.sink(tmp));
                    sink.writeAll(Okio.source(is));
                    sink.close();
                    is.close();

                    MediaType mt = MediaType.parse(getContentResolver().getType(u));
                    if (mt == null) mt = MediaType.parse("application/octet-stream");

                    RequestBody fb = RequestBody.create(tmp, mt);

                    MultipartBody mb = new MultipartBody.Builder()
                            .setType(MultipartBody.FORM)
                            .addFormDataPart("file", originalName, fb)
                            .addFormDataPart("upload_preset", UP)
                            .addFormDataPart("folder", "patient_documents/" + docId)   // <-- ALL FILES GO HERE
                            .build();


                    Request rq = new Request.Builder()
                            .url("https://api.cloudinary.com/v1_1/" + CN + "/auto/upload")
                            .post(mb)
                            .build();

                    Response re = client.newCall(rq).execute();

                    if (!re.isSuccessful()) {
                        runOnUiThread(() ->
                                Toast.makeText(this, "Upload failed: " + originalName, Toast.LENGTH_LONG).show());
                        return;
                    }

                    String json = re.body().string();
                    String url = new JSONObject(json).optString("secure_url", null);

                    if (url != null) {
                        urls.add(url);
                        names.add(originalName);
                        sizes.add(originalSize);
                    }
                }

                runOnUiThread(() -> saveToFirebase(urls, names, sizes));

            } catch (Exception e) {
                runOnUiThread(() ->
                        Toast.makeText(this, "Upload error: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }


}
