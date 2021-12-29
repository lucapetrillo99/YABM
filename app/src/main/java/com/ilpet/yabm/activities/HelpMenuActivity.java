package com.ilpet.yabm.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.Intent;
import android.os.Bundle;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.ilpet.yabm.R;

public class HelpMenuActivity extends AppCompatActivity {
    private RelativeLayout addBookmark;
    private RelativeLayout modifyBookmark;
    private RelativeLayout deleteBookmark;
    private RelativeLayout archiveBookmark;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu_help);

        Toolbar toolbar = findViewById(R.id.toolbar);
        TextView toolbarTitle = findViewById(R.id.toolbar_title);
        toolbarTitle.setText(R.string.help);
        setSupportActionBar(toolbar);

        addBookmark = findViewById(R.id.add_bookmark_help);
        modifyBookmark = findViewById(R.id.modify_bookmark_help);
        deleteBookmark = findViewById(R.id.delete_bookmark_help);
        archiveBookmark = findViewById(R.id.archive_bookmark_help);

        toolbar.setNavigationIcon(R.drawable.ic_back_button);
        toolbar.setNavigationOnClickListener(v -> {
            finish();
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        });

        addBookmarkClickListener();
        modifyBookmarkClickListener();
        deleteBookmarkClickListener();
        archiveBookmarkClickListener();
    }

    private void addBookmarkClickListener() {
        addBookmark.setOnClickListener(v -> {
            Intent intent = new Intent(HelpMenuActivity.this, HelpActivity.class);
            intent.putExtra("help", 1);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });

    }

    private void modifyBookmarkClickListener() {
        modifyBookmark.setOnClickListener(v -> {
            Intent intent = new Intent(HelpMenuActivity.this, HelpActivity.class);
            intent.putExtra("help", 2);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });
    }

    private void deleteBookmarkClickListener() {
        deleteBookmark.setOnClickListener(v -> {
            Intent intent = new Intent(HelpMenuActivity.this, HelpActivity.class);
            intent.putExtra("help", 3);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });
    }

    private void archiveBookmarkClickListener() {
        archiveBookmark.setOnClickListener(v -> {
            Intent intent = new Intent(HelpMenuActivity.this, HelpActivity.class);
            intent.putExtra("help", 4);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });

    }
}