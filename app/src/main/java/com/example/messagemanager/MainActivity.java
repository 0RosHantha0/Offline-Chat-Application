package com.example.messagemanager;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int CAMERA_REQUEST = 2;
    private static final int PERMISSION_REQUEST_CODE = 100;
    private List<Integer> selectedMessageIds = new ArrayList<>();

    private DatabaseHelper databaseHelper;
    private ListView messageListView;
    private EditText searchBar;
    private ImageView addImage, deleteMessage;
    private EditText messageText;
    private Button sendMessageButton;
    private MessageAdapter adapter;
    private Cursor cursor;
    private byte[] imageByteArray;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.CAMERA,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            }, PERMISSION_REQUEST_CODE);
        }

        databaseHelper = new DatabaseHelper(this);

        searchBar = findViewById(R.id.search_bar);
        messageListView = findViewById(R.id.message_list);
        addImage = findViewById(R.id.add_image);
        messageText = findViewById(R.id.message_text);
        deleteMessage = findViewById(R.id.delete_message);
        sendMessageButton = findViewById(R.id.send_message);

        cursor = databaseHelper.getAllMessages();
        adapter = new MessageAdapter(this, cursor, 0);
        messageListView.setAdapter(adapter);

        addImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showImagePickerDialog();
            }
        });

        deleteMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                messageText.setText("");
                imageByteArray = null;
                addImage.setImageResource(android.R.drawable.ic_menu_camera); // Reset image view
            }
        });

        sendMessageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String message = messageText.getText().toString();
                if (!message.isEmpty() || imageByteArray != null) {
                    databaseHelper.addMessage(message, imageByteArray);
                    loadMessages();
                    messageText.setText("");
                    imageByteArray = null;
                    addImage.setImageResource(android.R.drawable.ic_menu_camera); // Reset image view
                    Toast.makeText(MainActivity.this, "Message sent", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "Please enter a message or select an image", Toast.LENGTH_SHORT).show();
                }
            }
        });

        messageListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                cursor.moveToPosition(position);
                String selectedMessage = cursor.getString(cursor.getColumnIndexOrThrow("text"));
                showEditPopup(selectedMessage);
            }
        });

        searchBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchMessages(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });

        //

        messageListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        messageListView.setMultiChoiceModeListener(new AbsListView.MultiChoiceModeListener() {
            @Override
            public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
                Cursor cursor = (Cursor) adapter.getItem(position);
                @SuppressLint("Range") int messageId = cursor.getInt(cursor.getColumnIndex("_id"));
                if (checked) {
                    selectedMessageIds.add(messageId);
                } else {
                    selectedMessageIds.remove((Integer) messageId);
                }
            }

            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                MenuInflater inflater = getMenuInflater();
                inflater.inflate(R.menu.menu_delete, menu); // Create a menu for deletion
                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                return false;
            }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                if (item.getItemId() == R.id.action_delete) {
                    deleteSelectedMessages();
                    mode.finish(); // Finish the action mode
                    return true;
                }
                return false;
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {
                selectedMessageIds.clear(); // Clear the selected messages
            }
        });

        //

    }

    //

    private void deleteSelectedMessages() {
        for (int messageId : selectedMessageIds) {
            databaseHelper.deleteMessage(messageId);
        }
        loadMessages(); // Reload the messages after deletion
    }

    //

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permissions granted
            } else {
                Toast.makeText(this, "Permissions denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void loadMessages() {
        cursor = databaseHelper.getAllMessages();
        adapter.changeCursor(cursor);
    }

    private void searchMessages(String query) {
        cursor = databaseHelper.searchMessages(query);
        adapter.changeCursor(cursor);
    }

    private void showImagePickerDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Choose Image")
                .setItems(new CharSequence[]{"Gallery", "Camera"}, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0:
                                pickImageFromGallery();
                                break;
                            case 1:
                                captureImageFromCamera();
                                break;
                        }
                    }
                }).show();
    }

    private void pickImageFromGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    private void captureImageFromCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, CAMERA_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == PICK_IMAGE_REQUEST && data != null && data.getData() != null) {
                Uri selectedImageUri = data.getData();
                try {
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImageUri);
                    imageByteArray = getBytesFromBitmap(bitmap);
                    addImage.setImageBitmap(bitmap);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else if (requestCode == CAMERA_REQUEST && data != null && data.getExtras() != null) {
                Bitmap bitmap = (Bitmap) data.getExtras().get("data");
                imageByteArray = getBytesFromBitmap(bitmap);
                addImage.setImageBitmap(bitmap);
            }
        }
    }

    private byte[] getBytesFromBitmap(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        return stream.toByteArray();
    }

    private void showEditPopup(final String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        @SuppressLint("InflateParams") View view = LayoutInflater.from(this).inflate(R.layout.popup_view_message, null);
        builder.setView(view);
        final AlertDialog dialog = builder.create();

        final EditText messageText = view.findViewById(R.id.view_message_text);
        final ImageView imageView = view.findViewById(R.id.view_image);
        Button editButton = view.findViewById(R.id.edit_message);
        Button shareButton = view.findViewById(R.id.share_message);
        Button deleteButton = view.findViewById(R.id.delete_message);

        messageText.setText(message);

        Cursor cursor = databaseHelper.getMessageId(message);
        if (cursor.moveToFirst()) {
            @SuppressLint("Range") byte[] image = cursor.getBlob(cursor.getColumnIndex("image"));
            if (image != null) {
                Bitmap bitmap = BitmapFactory.decodeByteArray(image, 0, image.length);
                imageView.setImageBitmap(bitmap);
            } else {
                imageView.setImageResource(android.R.drawable.ic_menu_gallery); // default image
            }
        }

        editButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                messageText.setEnabled(true);
                messageText.requestFocus();
                editButton.setText("Save");
                editButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        int messageId = getMessageId(message);
                        if (messageId != -1) {
                            databaseHelper.updateMessage(messageId, messageText.getText().toString(), imageByteArray);
                            loadMessages();
                            dialog.dismiss();
                            Toast.makeText(MainActivity.this, "Message Edited", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(MainActivity.this, "Error updating message", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });

        shareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shareMessage(message);
            }
        });

        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int messageId = getMessageId(message);
                if (messageId != -1) {
                    databaseHelper.deleteMessage(messageId);
                    loadMessages();
                    dialog.dismiss();
                    Toast.makeText(MainActivity.this, "Message Deleted", Toast.LENGTH_SHORT).show(); // Toast message here
                } else {
                    Toast.makeText(MainActivity.this, "Error deleting message", Toast.LENGTH_SHORT).show();
                }
            }
        });

        dialog.show();
    }

    private int getMessageId(String message) {
        Cursor cursor = databaseHelper.getMessageId(message);
        if (cursor.moveToFirst()) {
            @SuppressLint("Range") int id = cursor.getInt(cursor.getColumnIndex("_id"));
            return id;
        }
        return -1;
    }


    //

    private void shareMessage(String message) {
        Cursor cursor = databaseHelper.getMessageId(message);
        if (cursor.moveToFirst()) {
            @SuppressLint("Range") byte[] image = cursor.getBlob(cursor.getColumnIndex("image"));
            Bitmap bitmap = null;
            if (image != null) {
                bitmap = BitmapFactory.decodeByteArray(image, 0, image.length);
            }

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("image/*");
            shareIntent.putExtra(Intent.EXTRA_TEXT, message);

            if (bitmap != null) {
                // Save the bitmap to a temporary file
                String path = MediaStore.Images.Media.insertImage(getContentResolver(), bitmap, "Image", null);
                Uri imageUri = Uri.parse(path);
                shareIntent.putExtra(Intent.EXTRA_STREAM, imageUri);
            }

            startActivity(Intent.createChooser(shareIntent, "Share message via"));
        } else {
            // Handle the case where the message is not found
            Toast.makeText(this, "Message not found", Toast.LENGTH_SHORT).show();
        }
    }

}
