package com.rishabhkohli.terminal;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ActivityLaunch extends AppCompatActivity {

    ArrayList<String> connectionDetailsArrayList;
    ArrayAdapter<String> stringArrayAdapter;
    SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launch);

        sharedPreferences = getSharedPreferences("Devices", MODE_PRIVATE);
        connectionDetailsArrayList = new ArrayList<>();

        for (int i = 0; i < sharedPreferences.getInt("count", 0); i++) {
            connectionDetailsArrayList.add(sharedPreferences.getString("details_"+i, ""));
        }

        ListView devicesListView = (ListView)findViewById(R.id.devices_listView);
        stringArrayAdapter = new ArrayAdapter<String>(this, R.layout.devices_list_item, R.id.item_text, connectionDetailsArrayList) {
            @NonNull
            @Override
            public View getView(final int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View viewToReturn = super.getView(position, convertView, parent);
                viewToReturn.findViewById(R.id.del_button).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        deleteEntry(position);
                    }
                });
                return viewToReturn;
            }
        };
        devicesListView.setAdapter(stringArrayAdapter);
        devicesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String connectionDetail = ((TextView)(view.findViewById(R.id.item_text))).getText().toString();
                String ip = connectionDetail.substring(0, connectionDetail.indexOf(":"));
                int port = Integer.parseInt(connectionDetail.substring(connectionDetail.indexOf(":") + 1, connectionDetail.length()));
                connect(ip, port);
            }
        });

        findViewById(R.id.connect_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String ip = ((EditText) findViewById(R.id.ip_edit_text)).getText().toString();
                int port = Integer.parseInt(((EditText) findViewById(R.id.port_edit_text)).getText().toString());

                connectionDetailsArrayList.add(ip + ":" + Integer.toString(port));
                saveList();

                connect(ip, port);
            }
        });
    }

    private void connect(String ip, int port) {
        Intent intent = new Intent(ActivityLaunch.this, ActivityTerminal.class);
        intent.putExtra("port", port);
        intent.putExtra("ip", ip);
        startActivity(intent);
    }

    private void saveList() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        for (int i = 0; i < connectionDetailsArrayList.size(); i++) {
            editor.putString("details_"+i, connectionDetailsArrayList.get(i));
        }
        editor.putInt("count", connectionDetailsArrayList.size());
        editor.apply();
    }

    private void deleteEntry(int position) {
        connectionDetailsArrayList.remove(position);
        stringArrayAdapter.notifyDataSetChanged();
        saveList();
    }
}