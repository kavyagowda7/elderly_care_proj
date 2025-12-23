package com.example.elderlycare;

import android.content.*;
import android.net.*;
import android.view.*;
import android.widget.*;

import androidx.recyclerview.widget.*;
import androidx.annotation.*;

import com.bumptech.glide.Glide;

import java.util.*;
import androidx.appcompat.app.AlertDialog;


public class DocumentsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0, TYPE_ITEM = 1;

    Context ctx;
    List<Object> items;

    public DocumentsAdapter(Context c, List<Object> i) {
        ctx = c;
        items = i;
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position) instanceof String ? TYPE_HEADER : TYPE_ITEM;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int type) {
        LayoutInflater li = LayoutInflater.from(parent.getContext());
        if (type == TYPE_HEADER)
            return new H(li.inflate(R.layout.item_date_header, parent, false));
        return new I(li.inflate(R.layout.item_document, parent, false));
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder h, int pos) {

        if (getItemViewType(pos) == TYPE_HEADER) {
            ((H) h).t.setText((String) items.get(pos));
            return;
        }

        DocumentModel d = (DocumentModel) items.get(pos);
        I v = (I) h;

        v.ti.setText(d.title);
        v.ca.setText(d.type);
        v.fn.setText((d.fileNames != null && d.fileNames.size() > 0) ? d.fileNames.get(0) : (d.fileUrls != null && d.fileUrls.size() > 0 ? d.fileUrls.get(0) : ""));
        v.ta.setText(d.healthTags != null && d.healthTags.size() > 0 ? d.healthTags.get(0) : "");

        // show first attachment thumbnail if image, otherwise generic icon
        if (d.fileUrls != null && d.fileUrls.size() > 0) {
            String first = d.fileUrls.get(0);
            Glide.with(ctx).load(first).centerCrop().into(v.th);
            if (d.fileUrls.size() > 1) {
                v.moreBadge.setVisibility(View.VISIBLE);
                v.moreBadge.setText("+" + (d.fileUrls.size() - 1));
            } else {
                v.moreBadge.setVisibility(View.GONE);
            }
        } else {
            v.th.setImageResource(R.drawable.ic_attachment);
            v.moreBadge.setVisibility(View.GONE);
        }

        // show file open dialog
        v.itemView.setOnClickListener(view -> {
            if (d.fileUrls == null || d.fileUrls.isEmpty()) {
                Toast.makeText(ctx, "No attachments", Toast.LENGTH_SHORT).show();
                return;
            }
            // show list dialog
            String[] names = new String[d.fileUrls.size()];
            for (int i = 0; i < names.length; i++) {
                names[i] = (d.fileNames != null && i < d.fileNames.size()) ? d.fileNames.get(i) : "Attachment " + (i + 1);
            }
            AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
            builder.setTitle(d.title);
            builder.setItems(names, (dialog, which) -> {
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(d.fileUrls.get(which)));
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    ctx.startActivity(intent);
                } catch (Exception ex) {
                    Toast.makeText(ctx, "Cannot open", Toast.LENGTH_SHORT).show();
                }
            });
            builder.show();
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class H extends RecyclerView.ViewHolder {
        TextView t;

        H(View v) {
            super(v);
            t = v.findViewById(R.id.tvDateHeader);
        }
    }

    static class I extends RecyclerView.ViewHolder {

        TextView ti, ca, fn, ta, moreBadge;
        ImageView th;

        I(View v) {
            super(v);

            ti = v.findViewById(R.id.tvTitle);
            ca = v.findViewById(R.id.tvCategory);
            fn = v.findViewById(R.id.tvFileName);
            ta = v.findViewById(R.id.tvTag);
            th = v.findViewById(R.id.iv_attachment_thumb);

            moreBadge = v.findViewById(R.id.tvMoreBadge); // new small badge view in item layout
        }
    }
}
