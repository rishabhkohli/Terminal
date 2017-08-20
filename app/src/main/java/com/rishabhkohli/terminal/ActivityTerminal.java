package com.rishabhkohli.terminal;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class ActivityTerminal extends AppCompatActivity implements SocketDelegate {

    private String ip;
    private int port;
    private SocketHandler socketHandler;
    private EditText sendMessageEditText;
    private Button sendButton;
    private ArrayList<Message> messageList;
    private LogArrayAdapter logArrayAdapter;
    private Boolean CR, LF, clearInput;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_terminal);

        sendMessageEditText = (EditText) findViewById(R.id.send_edit_text);
        sendButton = (Button) findViewById(R.id.send_button);

        socketHandler = new SocketHandler();

        ip = getIntent().getStringExtra("ip");
        port = getIntent().getIntExtra("port", 0);

        connect(ip, port);

        int[] extraButtonIDs = new int[]{R.id.extra_button_1, R.id.extra_button_2, R.id.extra_button_3, R.id.extra_button_4, R.id.extra_button_5, R.id.extra_button_6, R.id.extra_button_7, R.id.extra_button_8, R.id.extra_button_9, R.id.extra_button_10};
        final Button[] extraButtons = new Button[extraButtonIDs.length];

        sharedPreferences = getSharedPreferences("Terminal_settings", MODE_PRIVATE);
        CR = sharedPreferences.getBoolean("CR", true);
        LF = sharedPreferences.getBoolean("LF", true);
        clearInput = sharedPreferences.getBoolean("clearInput", true);

        for (int i = 0; i < extraButtonIDs.length; i++) {
            final int buttonNumber = i + 1;
            extraButtons[i] = (Button) findViewById(extraButtonIDs[i]);
            extraButtons[i].setText(sharedPreferences.getString("Button_" + buttonNumber + "_title", "Btn " + buttonNumber));
            extraButtons[i].setTag(sharedPreferences.getString("Button_" + buttonNumber + "_message", ""));

            extraButtons[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String message = (String) v.getTag();
                    sendMessageEditText.setText(message);
                    sendMessageEditText.setSelection(sendMessageEditText.getText().length());
                }
            });
            extraButtons[i].setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(final View v) {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(ActivityTerminal.this);
                    final View view = getLayoutInflater().inflate(R.layout.extra_button_dialog_layout, (ConstraintLayout)findViewById(R.id.terminal_container), false);
                    final EditText buttonTitleEditText = (EditText) (view.findViewById(R.id.extra_button_title_editText));
                    final EditText buttonMessageEditText = (EditText) (view.findViewById(R.id.extra_button_message_editText));
                    buttonTitleEditText.setText(extraButtons[buttonNumber - 1].getText());
                    buttonMessageEditText.setText((String) extraButtons[buttonNumber - 1].getTag());
                    builder.setView(view)
                            .setMessage("Button " + buttonNumber)
                            .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    ((Button) v).setText(buttonTitleEditText.getText().toString());
                                    v.setTag(buttonMessageEditText.getText().toString());

                                    SharedPreferences.Editor editor = sharedPreferences.edit();
                                    editor.putString("Button_" + buttonNumber + "_title", buttonTitleEditText.getText().toString());
                                    editor.putString("Button_" + buttonNumber + "_message", buttonMessageEditText.getText().toString());
                                    editor.apply();
                                }
                            })
                            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.cancel();
                                }
                            })
                            .show();
                    return true;
                }
            });
        }

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String message = sendMessageEditText.getText().toString();
                if (CR && LF) {
                    message += "\r\n";
                } else if (CR) {
                    message += "\r";
                } else if (LF) {
                    message += "\n";
                }
                socketHandler.printOut(message);
                messageList.add(new Message(message.trim(), MessageType.OUTGOING));
                logArrayAdapter.notifyDataSetChanged();

                if (clearInput) sendMessageEditText.setText("");
            }
        });

        ListView logListView = (ListView) findViewById(R.id.listView);
        logListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                copyToClipboard(((TextView) view).getText().toString());
                return true;
            }
        });
        messageList = new ArrayList<>();
        logArrayAdapter = new LogArrayAdapter(this, messageList);
        logListView.setAdapter(logArrayAdapter);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.clear();
        getMenuInflater().inflate(R.menu.terminal_menu, menu);

        Drawable connectionDrawable = menu.findItem(R.id.connection_status).getIcon();
        if (!socketHandler.isClosed() && socketHandler.isConnected()) //closed checks current status but returns false in the start. This is solved by isConnected which returns true only after having been connected once.
            connectionDrawable.setColorFilter(Color.GREEN, PorterDuff.Mode.SRC_IN);
        else
            connectionDrawable.setColorFilter(Color.RED, PorterDuff.Mode.SRC_IN);

        menu.findItem(R.id.carriage_return).setChecked(CR);
        menu.findItem(R.id.line_feed).setChecked(LF);
        menu.findItem(R.id.clear_input).setChecked(clearInput);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.connection_status:
                if (socketHandler.isClosed()) {
                    connect(ip, port);
                } else {
                    onDisconnect(); //should make the toast that is defined in onDisconnect()
                }
            case R.id.carriage_return:
                CR = !CR;
                sharedPreferences.edit().putBoolean("CR", CR).apply();
                break;
            case R.id.line_feed:
                LF = !LF;
                sharedPreferences.edit().putBoolean("LF", LF).apply();
                break;
            case R.id.clear_input:
                clearInput = !clearInput;
                sharedPreferences.edit().putBoolean("clearInput", clearInput).apply();
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        disconnect();
        super.onBackPressed();
    }

    private void connect(final String ip, final int port) {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                if (socketHandler.setSocket(ip, port, ActivityTerminal.this)) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            invalidateOptionsMenu();
                            sendButton.setEnabled(true);
                            sendMessageEditText.setEnabled(true);
                            Toast.makeText(getApplicationContext(), "Connected", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        });
    }

    private void disconnect() {
        socketHandler.closeSocket();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                invalidateOptionsMenu();
                sendButton.setEnabled(false);
                sendMessageEditText.setEnabled(false);
            }
        });
    }

    private void copyToClipboard(String text) {
        try {
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            android.content.ClipData clip = android.content.ClipData.newPlainText("WordKeeper", text);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(getApplicationContext(), "Text copied to clipboard.", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), "Error copying text to clipboard.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onMessageReceived(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                messageList.add(new Message(message.trim(), MessageType.INCOMING));
                logArrayAdapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    public void onDisconnect() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), "Disconnected", Toast.LENGTH_SHORT).show();
            }
        });
        disconnect();
    }
}
