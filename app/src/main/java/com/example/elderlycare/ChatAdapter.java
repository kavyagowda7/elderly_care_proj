package com.example.elderlycare;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_SENDER = 1;
    private static final int TYPE_RECEIVER = 2;

    List<ChatMessage> list;
    String currentUid;

    public ChatAdapter(List<ChatMessage> list, String currentUid) {
        this.list = list;
        this.currentUid = currentUid;
    }

    @Override
    public int getItemViewType(int position) {
        return list.get(position).senderId.equals(currentUid)
                ? TYPE_SENDER
                : TYPE_RECEIVER;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent, int viewType) {

        if (viewType == TYPE_SENDER) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_sender, parent, false);
            return new SenderVH(v);
        } else {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_receiver, parent, false);
            return new ReceiverVH(v);
        }
    }

    @Override
    public void onBindViewHolder(
            @NonNull RecyclerView.ViewHolder holder, int position) {

        ChatMessage msg = list.get(position);

        if (holder instanceof SenderVH) {
            ((SenderVH) holder).msgText.setText(msg.message);
        } else {
            ((ReceiverVH) holder).msgText.setText(msg.message);
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class SenderVH extends RecyclerView.ViewHolder {
        TextView msgText;
        SenderVH(View v) {
            super(v);
            msgText = v.findViewById(R.id.msgText);
        }
    }

    static class ReceiverVH extends RecyclerView.ViewHolder {
        TextView msgText;
        ReceiverVH(View v) {
            super(v);
            msgText = v.findViewById(R.id.msgText);
        }
    }
}
