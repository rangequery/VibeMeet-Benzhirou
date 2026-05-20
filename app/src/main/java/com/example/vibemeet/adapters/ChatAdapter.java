package com.example.vibemeet.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.example.vibemeet.R;
import com.example.vibemeet.models.ChatMessage;

import java.util.List;

public class ChatAdapter extends BaseAdapter {
    private static final int TYPE_USER = 0;
    private static final int TYPE_ASSISTANT = 1;

    private final List<ChatMessage> messages;
    private final LayoutInflater inflater;

    public ChatAdapter(Context context, List<ChatMessage> messages) {
        this.messages = messages;
        this.inflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return messages.size();
    }

    @Override
    public Object getItem(int position) {
        return messages.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public int getItemViewType(int position) {
        return messages.get(position).isUserMessage() ? TYPE_USER : TYPE_ASSISTANT;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ChatMessage message = messages.get(position);
        int type = getItemViewType(position);

        if (convertView == null) {
            int layoutRes = type == TYPE_USER ? R.layout.item_chat_user : R.layout.item_chat_bot;
            convertView = inflater.inflate(layoutRes, parent, false);
        }

        TextView messageText = convertView.findViewById(R.id.messageText);
        messageText.setText(message.getContent());

        return convertView;
    }
}
