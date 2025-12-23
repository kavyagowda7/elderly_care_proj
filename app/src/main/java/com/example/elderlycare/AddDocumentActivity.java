package com.example.elderlycare;

import android.Manifest;
import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.InputType;
import android.text.TextUtils;
import android.view.View;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;

import org.json.JSONObject;

import java.io.File;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.*;

import okhttp3.*;
import okio.BufferedSink;
import okio.Okio;

public class AddDocumentActivity extends AppCompatActivity {

    static final int RF = 1001, RI = 1002, RP = 2001;
    static final String CN = "du8sonnyf";
    static final String UP = "unsigned_preset";

    ProgressDialog progressDialog;

    TextView tvDate, tvSelectedFile;
    Spinner spinnerDocType;
    EditText etTitle, etNotes;
    Button btnAttachFile, btnAttachPhoto;
    ImageView btnSave, btnBack, btnAddTag;
    LinearLayout tagContainer;

    List<Uri> pickedUris = new ArrayList<>();
    List<String> pickedNames = new ArrayList<>();
    List<String> pickedSizes = new ArrayList<>();
    List<String> pickedPublicIds = new ArrayList<>();
    List<String> healthTags = new ArrayList<>();

    Uri cameraImageUri;
    String selectedDate = "";
    String docId = "";
    String uid;

    OkHttpClient client = new OkHttpClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_document);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            finish();
            return;
        }
        uid = user.getUid();

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

        View dateLayout = findViewById(R.id.datePickerLayout);
        dateLayout.setClickable(true);
        dateLayout.setFocusable(true);
        dateLayout.setOnClickListener(v -> pickDate());

        tvDate.setClickable(false);
        tvDate.setFocusable(false);



        spinnerDocType.setAdapter(new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                new String[]{"None", "Lab Report", "Prescription", "Imaging", "Other"}
        ));

        selectedDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        tvDate.setText(new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date()));

        btnAttachFile.setOnClickListener(v -> openFilePicker());
        btnAttachPhoto.setOnClickListener(v -> openCamera());
        btnSave.setOnClickListener(v -> onSaveClicked());
        btnBack.setOnClickListener(v -> finish());
        btnAddTag.setOnClickListener(v -> openTagSelector());
    }

    // ---------------- DATE PICKER ----------------
    void pickDate() {
        Calendar c = Calendar.getInstance();

        new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {

                    Calendar selected = Calendar.getInstance();
                    selected.set(year, month, dayOfMonth);

                    selectedDate = new SimpleDateFormat(
                            "yyyy-MM-dd",
                            Locale.getDefault()
                    ).format(selected.getTime());

                    tvDate.setText(
                            new SimpleDateFormat(
                                    "dd/MM/yyyy",
                                    Locale.getDefault()
                            ).format(selected.getTime())
                    );
                },
                c.get(Calendar.YEAR),
                c.get(Calendar.MONTH),
                c.get(Calendar.DAY_OF_MONTH)
        ).show();
    }


    // ---------------- TAGS ----------------

    void openTagSelector() {
        String[] preset = {"Heart disease", "Diabetes", "Hypertension", "Asthma", "Thyroid", "Other..."};

        new AlertDialog.Builder(this)
                .setTitle("Select Health Issue")
                .setItems(preset, (d, i) -> {
                    if (preset[i].equals("Other...")) {
                        EditText et = new EditText(this);
                        et.setInputType(InputType.TYPE_CLASS_TEXT);
                        new AlertDialog.Builder(this)
                                .setView(et)
                                .setPositiveButton("Add", (dd, w) -> {
                                    healthTags.add(et.getText().toString());
                                    addTagChip(et.getText().toString());
                                })
                                .show();
                    } else {
                        healthTags.add(preset[i]);
                        addTagChip(preset[i]);
                    }
                }).show();
    }

    void addTagChip(String text) {
        TextView chip = new TextView(this);
        chip.setText(text);
        chip.setBackgroundResource(R.drawable.tag_bg);
        chip.setPadding(20, 10, 20, 10);
        tagContainer.addView(chip);
    }

    // ---------------- PICKERS ----------------

    void openFilePicker() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.setType("*/*");
        i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(i, RF);
    }

    void openCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, RP);
            return;
        }

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) == null) {
            Toast.makeText(this, "No camera app", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            File photo = File.createTempFile("camera_", ".jpg", getExternalFilesDir(null));
            cameraImageUri = FileProvider.getUriForFile(
                    this, getPackageName() + ".provider", photo);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri);
            startActivityForResult(intent, RI);
        } catch (Exception e) {
            Toast.makeText(this, "Camera error", Toast.LENGTH_SHORT).show();
        }
    }

    // ---------------- RESULT ----------------

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (res != RESULT_OK) return;

        if (req == RI && cameraImageUri != null) {
            addUri(cameraImageUri);
        } else if (data != null) {
            pickedUris.clear();
            pickedNames.clear();
            pickedSizes.clear();
            pickedPublicIds.clear();

            if (data.getClipData() != null) {
                ClipData cd = data.getClipData();
                for (int i = 0; i < cd.getItemCount(); i++)
                    addUri(cd.getItemAt(i).getUri());
            } else if (data.getData() != null) {
                addUri(data.getData());
            }
        }

        tvSelectedFile.setText(pickedUris.size() + " file(s) selected");
    }

    void addUri(Uri uri) {
        pickedUris.add(uri);
        pickedNames.add(getFileName(uri));
        pickedSizes.add(getFileSize(uri));
    }

    String getFileName(Uri uri) {
        try (Cursor c = getContentResolver().query(uri,
                new String[]{MediaStore.MediaColumns.DISPLAY_NAME}, null, null, null)) {
            if (c != null && c.moveToFirst()) return c.getString(0);
        } catch (Exception ignored) {}
        return "file";
    }

    String getFileSize(Uri uri) {
        try (InputStream is = getContentResolver().openInputStream(uri)) {
            int s = is.available();
            return s < 1024 ? s + " B" : (s / 1024) + " KB";
        } catch (Exception e) {
            return "";
        }
    }

    // ---------------- SAVE ----------------

    void onSaveClicked() {
        if (TextUtils.isEmpty(etTitle.getText())) return;
        if (pickedUris.isEmpty()) return;

        progressDialog = ProgressDialog.show(this, "", "Uploading...", true);

        DatabaseReference ref = FirebaseUtil.db()
                .getReference("patient_documents").child(uid);

        if (docId.isEmpty()) docId = ref.push().getKey();

        uploadAllFiles();
    }

    void uploadAllFiles() {
        new Thread(() -> {
            try {
                List<String> urls = new ArrayList<>();

                for (int i = 0; i < pickedUris.size(); i++) {
                    File tmp = File.createTempFile("upl", ".tmp", getCacheDir());
                    BufferedSink sink = Okio.buffer(Okio.sink(tmp));
                    sink.writeAll(Okio.source(getContentResolver().openInputStream(pickedUris.get(i))));
                    sink.close();

                    MultipartBody body = new MultipartBody.Builder()
                            .setType(MultipartBody.FORM)
                            .addFormDataPart("file", pickedNames.get(i),
                                    RequestBody.create(tmp, MediaType.parse("application/octet-stream")))
                            .addFormDataPart("upload_preset", UP)
                            .addFormDataPart("folder", "patient_documents/" + docId)
                            .build();

                    Response r = client.newCall(new Request.Builder()
                            .url("https://api.cloudinary.com/v1_1/" + CN + "/auto/upload")
                            .post(body).build()).execute();

                    JSONObject obj = new JSONObject(r.body().string());
                    urls.add(obj.optString("secure_url"));
                    pickedPublicIds.add(obj.optString("public_id"));
                }

                runOnUiThread(() -> saveToFirebase(urls));

            } catch (Exception e) {
                runOnUiThread(() -> progressDialog.dismiss());
            }
        }).start();
    }

    void saveToFirebase(List<String> urls) {
        DocumentModel doc = new DocumentModel();
        doc.id = docId;
        doc.date = selectedDate;
        doc.title = etTitle.getText().toString();
        doc.type = spinnerDocType.getSelectedItem().toString();
        doc.fileUrls = urls;
        doc.filePublicIds = pickedPublicIds;
        doc.healthTags = healthTags;
        doc.timestamp = System.currentTimeMillis();

        FirebaseUtil.db().getReference("patient_documents")
                .child(uid).child(docId).setValue(doc)
                .addOnSuccessListener(v -> {
                    progressDialog.dismiss();
                    finish();
                });
    }
}
