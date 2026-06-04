package com.example.campusrun;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class ProfileActivity extends AppCompatActivity {

    private static final int REQUEST_PICK_AVATAR = 1001;

    private ImageView imgAvatar;
    private EditText etNickname;
    private RadioGroup rgAvatar;
    private DBManager dbManager;
    private String username;
    private int avatarIndex;
    private String avatarUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        SharedPreferences sp = getSharedPreferences("user", MODE_PRIVATE);
        username = sp.getString("username", "");
        dbManager = new DBManager(this);

        imgAvatar = findViewById(R.id.imgProfileAvatar);
        etNickname = findViewById(R.id.etNickname);
        rgAvatar = findViewById(R.id.rgAvatar);
        Button btnChooseLocal = findViewById(R.id.btnChooseLocalAvatar);
        Button btnUseDefault = findViewById(R.id.btnUseDefaultAvatar);
        Button btnSave = findViewById(R.id.btnSaveProfile);

        etNickname.setText(dbManager.getNickname(username));
        avatarIndex = dbManager.getAvatarIndex(username);
        avatarUri = dbManager.getAvatarUri(username);
        rgAvatar.check(radioIdForIndex(avatarIndex));
        refreshAvatar();

        rgAvatar.setOnCheckedChangeListener((group, checkedId) -> {
            avatarIndex = indexForRadioId(checkedId);
            avatarUri = "";
            refreshAvatar();
        });

        btnChooseLocal.setOnClickListener(v -> chooseLocalAvatar());
        btnUseDefault.setOnClickListener(v -> {
            avatarUri = "";
            refreshAvatar();
        });
        btnSave.setOnClickListener(v -> saveProfile());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_PICK_AVATAR && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri == null) return;

            final int flags = data.getFlags() &
                    (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            try {
                getContentResolver().takePersistableUriPermission(uri, flags);
            } catch (SecurityException ignored) {
            }

            avatarUri = uri.toString();
            refreshAvatar();
        }
    }

    private void chooseLocalAvatar() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(intent, REQUEST_PICK_AVATAR);
    }

    private void saveProfile() {
        String nickname = etNickname.getText().toString().trim();
        if (nickname.isEmpty()) {
            Toast.makeText(this, "\u8bf7\u8f93\u5165\u6635\u79f0", Toast.LENGTH_SHORT).show();
            return;
        }

        if (dbManager.updateProfile(username, nickname, avatarIndex, avatarUri)) {
            Toast.makeText(this, "\u8d44\u6599\u5df2\u4fdd\u5b58", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(this, "\u4fdd\u5b58\u5931\u8d25", Toast.LENGTH_SHORT).show();
        }
    }

    private void refreshAvatar() {
        if (avatarUri != null && !avatarUri.isEmpty()) {
            imgAvatar.setImageURI(Uri.parse(avatarUri));
        } else {
            imgAvatar.setImageResource(AvatarHelper.getAvatarResId(avatarIndex));
        }
    }

    private int radioIdForIndex(int index) {
        if (index == 1) return R.id.rbAvatar1;
        if (index == 2) return R.id.rbAvatar2;
        if (index == 3) return R.id.rbAvatar3;
        return R.id.rbAvatar0;
    }

    private int indexForRadioId(int checkedId) {
        if (checkedId == R.id.rbAvatar1) return 1;
        if (checkedId == R.id.rbAvatar2) return 2;
        if (checkedId == R.id.rbAvatar3) return 3;
        return 0;
    }
}
