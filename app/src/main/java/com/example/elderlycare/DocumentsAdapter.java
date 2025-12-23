package com.example.elderlycare;

import android.app.Activity;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.*;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

public class DocumentsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM = 1;

    Context ctx;
    List<Object> items;
    boolean isReadOnly;


    public DocumentsAdapter(Context ctx, List<Object> items, boolean isReadOnly) {
        this.ctx = ctx;
        this.items = items;
        this.isReadOnly = isReadOnly;
    }


    @Override
    public int getItemViewType(int position) {
        return items.get(position) instanceof String ? TYPE_HEADER : TYPE_ITEM;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater li = LayoutInflater.from(parent.getContext());

        if (viewType == TYPE_HEADER) {
            return new HeaderVH(li.inflate(R.layout.item_date_header, parent, false));
        }
        return new ItemVH(li.inflate(R.layout.item_document, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder h, int position) {

        if (getItemViewType(position) == TYPE_HEADER) {
            ((HeaderVH) h).tvDate.setText((String) items.get(position));
            return;
        }

        DocumentModel d = (DocumentModel) items.get(position);
        ItemVH v = (ItemVH) h;

        v.tvTitle.setText(d.title);
        v.tvCategory.setText(d.type);
        v.tvTag.setText(
                d.healthTags != null && !d.healthTags.isEmpty()
                        ? d.healthTags.get(0)
                        : ""
        );

        if (d.fileUrls != null && !d.fileUrls.isEmpty()) {
            Glide.with(ctx)
                    .load(d.fileUrls.get(0))
                    .centerCrop()
                    .into(v.ivThumb);

            v.tvFileName.setText(
                    d.fileNames != null && !d.fileNames.isEmpty()
                            ? d.fileNames.get(0)
                            : "Attachment"
            );

            if (d.fileUrls.size() > 1) {
                v.tvMore.setVisibility(View.VISIBLE);
                v.tvMore.setText("+" + (d.fileUrls.size() - 1));
            } else {
                v.tvMore.setVisibility(View.GONE);
            }
        }

        // 👁️ VIEW DOCUMENT
        v.ivOpen.setOnClickListener(view -> {

            if (d.fileUrls == null || d.fileUrls.isEmpty()) {
                Toast.makeText(ctx, "No attachments", Toast.LENGTH_SHORT).show();
                return;
            }

            String[] names = new String[d.fileUrls.size()];
            for (int i = 0; i < names.length; i++) {
                names[i] = (d.fileNames != null && i < d.fileNames.size())
                        ? d.fileNames.get(i)
                        : "Attachment " + (i + 1);
            }

            new androidx.appcompat.app.AlertDialog.Builder(ctx)
                    .setTitle("View documents")
                    .setItems(names, (dialog, which) -> {
                        Intent intent = new Intent(Intent.ACTION_VIEW,
                                Uri.parse(d.fileUrls.get(which)));
                        ctx.startActivity(intent);
                    })
                    .show();
        });


        // ⋮ MENU
        if (isReadOnly) {
            v.ivMenu.setVisibility(View.GONE);
        } else {
            v.ivMenu.setVisibility(View.VISIBLE);
            v.ivMenu.setOnClickListener(vw -> {
                PopupMenu menu = new PopupMenu(ctx, vw);
                menu.inflate(R.menu.menu_document);

                menu.setOnMenuItemClickListener(item -> {

                    if (item.getItemId() == R.id.action_delete) {
                        showDeleteConfirm(d);
                        return true;
                    }

                    if (item.getItemId() == R.id.action_share) {
                        shareLink(d);
                        return true;
                    }
                    return false;
                });
                menu.show();
            });
        }

    }
    private void shareLink(DocumentModel d) {

        if (d.fileUrls == null || d.fileUrls.isEmpty()) {
            Toast.makeText(ctx, "No document to share", Toast.LENGTH_SHORT).show();
            return;
        }

        StringBuilder text = new StringBuilder();
        text.append("Medical Document: ").append(d.title).append("\n\n");

        for (String url : d.fileUrls) {
            text.append(url).append("\n");
        }

        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("text/plain");
        share.putExtra(Intent.EXTRA_TEXT, text.toString());
        ctx.startActivity(Intent.createChooser(share, "Share document"));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    // ---------------- DELETE CONFIRMATION ----------------

    private void showDeleteConfirm(DocumentModel d) {

        new AlertDialog.Builder(ctx)
                .setTitle("Delete document")
                .setMessage("Are you sure you want to delete this document?")
                .setPositiveButton("Delete", (dialog, which) -> {

                    FirebaseUtil.db()
                            .getReference("patient_documents")
                            .child(FirebaseAuth.getInstance().getUid())
                            .child(d.id)
                            .removeValue()
                            .addOnSuccessListener(v -> {

                                // 🔥 REMOVE FROM UI IMMEDIATELY
                                int pos = items.indexOf(d);
                                if (pos != -1) {

                                    // Remove the document
                                    items.remove(pos);
                                    notifyItemRemoved(pos);

                                    // 🔥 CHECK HEADER ABOVE
                                    if (pos - 1 >= 0 && items.get(pos - 1) instanceof String) {

                                        // If list ends OR next item is another header
                                        boolean shouldRemoveHeader =
                                                pos == items.size() || items.get(pos) instanceof String;

                                        if (shouldRemoveHeader) {
                                            items.remove(pos - 1);
                                            notifyItemRemoved(pos - 1);
                                        }
                                    }
                                }


                                Toast.makeText(ctx, "Document deleted", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(ctx, "Delete failed", Toast.LENGTH_SHORT).show()
                            );
                })
                .setNegativeButton("Cancel", null)
                .show();
    }


    // ---------------- SHARE AS PDF / FILE ----------------

    private void shareAsFile(DocumentModel d) {

        if (d.fileUrls == null || d.fileUrls.isEmpty()) {
            Toast.makeText(ctx, "No file to share", Toast.LENGTH_SHORT).show();
            return;
        }

        String fileUrl = d.fileUrls.get(0);

        new Thread(() -> {
            try {
                URL url = new URL(fileUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.connect();

                InputStream input = conn.getInputStream();

                String ext = fileUrl.contains(".")
                        ? fileUrl.substring(fileUrl.lastIndexOf("."))
                        : ".file";

                // ✅ USE EXTERNAL CACHE (WhatsApp compatible)
                File outFile = new File(
                        ctx.getExternalCacheDir(),
                        "shared_document" + ext
                );

                FileOutputStream output = new FileOutputStream(outFile);

                byte[] buffer = new byte[4096];
                int n;
                while ((n = input.read(buffer)) > 0) {
                    output.write(buffer, 0, n);
                }

                output.close();
                input.close();

                Uri uri = FileProvider.getUriForFile(
                        ctx,
                        ctx.getPackageName() + ".provider",
                        outFile
                );

                Intent share = new Intent(Intent.ACTION_SEND);
                share.setType("*/*"); // 🔥 IMPORTANT
                share.putExtra(Intent.EXTRA_STREAM, uri);
                share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                // 🔥 CRITICAL FOR WHATSAPP
                share.setClipData(ClipData.newRawUri("file", uri));

                ((Activity) ctx).runOnUiThread(() ->
                        ctx.startActivity(Intent.createChooser(share, "Share document"))
                );

            } catch (Exception e) {
                e.printStackTrace();
                ((Activity) ctx).runOnUiThread(() ->
                        Toast.makeText(ctx, "Unable to share file", Toast.LENGTH_SHORT).show()
                );
            }
        }).start();
    }




    // ---------------- VIEW HOLDERS ----------------

    static class HeaderVH extends RecyclerView.ViewHolder {
        TextView tvDate;
        HeaderVH(View v) {
            super(v);
            tvDate = v.findViewById(R.id.tvDateHeader);
        }
    }

    static class ItemVH extends RecyclerView.ViewHolder {

        TextView tvTitle, tvCategory, tvFileName, tvTag, tvMore;
        ImageView ivThumb, ivOpen, ivMenu;

        ItemVH(View v) {
            super(v);

            tvTitle = v.findViewById(R.id.tvTitle);
            tvCategory = v.findViewById(R.id.tvCategory);
            tvFileName = v.findViewById(R.id.tvFileName);
            tvTag = v.findViewById(R.id.tvTag);
            tvMore = v.findViewById(R.id.tvMoreBadge);

            ivThumb = v.findViewById(R.id.iv_attachment_thumb);
            ivOpen = v.findViewById(R.id.ivOpen);
            ivMenu = v.findViewById(R.id.ivMenu);
        }
    }
}
