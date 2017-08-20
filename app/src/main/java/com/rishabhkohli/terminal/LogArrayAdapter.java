package com.rishabhkohli.terminal;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

class LogArrayAdapter extends ArrayAdapter<Message> {
    LogArrayAdapter(Context context, ArrayList<Message> messages) {
        super(context, 0, messages);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Get the data item for this position
        Message message = getItem(position);

        // Check if an existing view is being reused, otherwise inflate the view
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.simple_list_item, parent, false);
        }

        // Lookup view for data population
        TextView textView = (TextView)convertView;

        // Populate the data into the template view using the data object
        textView.setText(message.getMessage());

        switch (message.getMessageType()) {
            case INCOMING:
                textView.setTextColor(Color.MAGENTA);
                break;
            case OUTGOING:
                textView.setTextColor(Color.BLUE);
                break;
        }

        // Return the completed view to render on screen
        return convertView;
    }
}
