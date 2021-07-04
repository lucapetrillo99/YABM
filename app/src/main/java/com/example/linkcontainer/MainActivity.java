package com.example.linkcontainer;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Canvas;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import androidx.appcompat.widget.SearchView;

import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.util.ArrayList;

import it.xabaras.android.recyclerview.swipedecorator.RecyclerViewSwipeDecorator;

public class MainActivity extends AppCompatActivity implements View.OnLongClickListener {
    private static final int DELETE_OPTION = 1;
    private static final int ARCHIVE_OPTION = 2;
    private static final int UNARCHIVE_OPTION = 3;
    private static final String CATEGORY = "category";
    private static final String ALL_BOOKMARKS = "Tutti i segnalibri";
    private Intent activityIntent;
    private DatabaseHandler db;
    private ArrayList<Bookmark> bookmarks;
    private RecyclerView recyclerView;
    private RecyclerAdapter recyclerAdapter;
    private Toolbar toolbar;
    private TextView toolbarTitle;
    private TextView noBookmarks;
    private FloatingActionButton fab;
    private ArrayList<String> categories;
    private ArrayList<Bookmark> archivedUrl;
    private ArrayList<Bookmark> removedFromArchive;
    private ArrayList<Bookmark> selectedBookmarks;
    private int counter = 0;
    private String previousCategory;
    public boolean isContextualMenuEnable = false;
    public boolean areAllSelected = false;
    public boolean isArchiveModeEnabled = false;
    private static final int SYSTEM_DEFAULT = 0;
    private static final int LIGHT_MODE = 1;
    private static final int NIGHT_MODE = 2;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setAppTheme();
        db = DatabaseHandler.getInstance(getApplicationContext());
        SettingsManager settingsManager = new SettingsManager(getApplicationContext(), CATEGORY);
        String result = settingsManager.getCategory();
        setContentView(R.layout.activity_main);
        toolbar = findViewById(R.id.toolbar);
        toolbarTitle = findViewById(R.id.toolbar_title);
        setSupportActionBar(toolbar);
        noBookmarks = findViewById(R.id.no_bookmarks);
        recyclerView = findViewById(R.id.recycler_view);

        if (settingsManager.isFirstAccess()) {
            File folder = new File("/storage/emulated/0/Android" + File.separator +
                    getString(R.string.app_name));
            boolean success = true;
            if (!folder.exists()) {
                success = folder.mkdirs();
            }
            if (!success) {
                Toast.makeText(getApplicationContext(), "Qualcosa è andato storto!",
                        Toast.LENGTH_LONG).show();
            }
        }

        if (result.equals(ALL_BOOKMARKS)) {
            bookmarks = new ArrayList<>(db.getAllBookmarks());
        } else {
            bookmarks = new ArrayList<>(db.getBookmarksByCategory(result));
        }
        toolbarTitle.setText(result);
        archiveUrl();
        unarchiveBookmark();

        archivedUrl = new ArrayList<>();
        selectedBookmarks = new ArrayList<>();
        removedFromArchive = new ArrayList<>();
        categories = db.getAllCategories();
        setAdapter();

        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        if (Intent.ACTION_SEND.equals(action) && type != null) {
            if ("text/plain".equals(type)) {
                handleSendText(intent);
            }
        }

        fab = findViewById(R.id.add_button);
        fab.setOnClickListener(view -> {
            activityIntent = new Intent(MainActivity.this, InsertLink.class);
            startActivity(activityIntent);
            if (isContextualMenuEnable) {
                removeContextualActionMode();
            }
        });

        setBookmarksLabel();
    }

    private void setAdapter() {
        setBookmarksLabel();
        initSwipe();
        recyclerAdapter = new RecyclerAdapter(bookmarks, MainActivity.this);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getApplicationContext());
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(recyclerAdapter);
    }

    void handleSendText (Intent intent){
        String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
        if (sharedText != null) {
            Intent linkIntent = new Intent(MainActivity.this, InsertLink.class);
            linkIntent.putExtra("url", sharedText);
            startActivity(linkIntent);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        addFilterCategories();
        return super.onCreateOptionsMenu(menu);
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.settings:
                activityIntent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(activityIntent);
                if (isContextualMenuEnable) {
                    removeContextualActionMode();
                }
                break;
            case R.id.search:
                SearchView searchView = (SearchView) item.getActionView();
                searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                    @Override
                    public boolean onQueryTextSubmit(String query) {
                        return false;
                    }

                    @Override
                    public boolean onQueryTextChange(String newText) {
                        recyclerAdapter.getFilter().filter(newText);
                        return false;
                    }
                });
                break;
            case R.id.delete:
                if (counter > 0) {
                    contextualModeDialog(DELETE_OPTION);
                }
                break;
            case R.id.archive:
                if (counter > 0) {
                    contextualModeDialog(ARCHIVE_OPTION);
                }
                break;
            case R.id.unarchive:
                if (counter > 0) {
                    contextualModeDialog(UNARCHIVE_OPTION);
                }
                break;
            case R.id.select_all:
                if (!areAllSelected) {
                    areAllSelected = true;
                    selectedBookmarks.addAll(bookmarks);
                    counter = bookmarks.size();
                } else {
                    areAllSelected = false;
                    selectedBookmarks.removeAll(bookmarks);
                    counter = 0;
                }
                updateCounter();
                recyclerAdapter.notifyDataSetChanged();
                break;
        }

        for (String category: categories) {
            if(item.getTitle() == category) {
                if(item.getTitle().equals("Archiviati")) {
                    isArchiveModeEnabled = true;
                    fab.setVisibility(View.INVISIBLE);
                } else {
                    isArchiveModeEnabled = false;
                    fab.setVisibility(View.VISIBLE);
                }
                toolbarTitle.setText(item.getTitle());
                archiveUrl();
                unarchiveBookmark();
                bookmarks.clear();
                bookmarks = db.getBookmarksByCategory((String)item.getTitle());
                setAdapter();
            } else if (item.getTitle().equals(ALL_BOOKMARKS)){
                toolbarTitle.setText(item.getTitle());
                fab.setVisibility(View.VISIBLE);
                archiveUrl();
                unarchiveBookmark();
                bookmarks.clear();
                bookmarks = db.getAllBookmarks();
                setAdapter();
            }
        }
        return true;
    }

    public void initSwipe() {

        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {

                int position = viewHolder.getAdapterPosition();

                switch (direction) {
                    case ItemTouchHelper.LEFT:
                        if(toolbarTitle.getText().toString().equals("Archiviati")) {
                            Bookmark disarchivedBookmark = bookmarks.get(position);
                            removedFromArchive.add(bookmarks.get(position));
                            bookmarks.remove(position);
                            recyclerAdapter.notifyItemRemoved(position);

                            Snackbar.make(recyclerView, disarchivedBookmark.getLink() + " rimosso dall'archivio.", Snackbar.LENGTH_LONG)
                                    .setAction("Annulla", v -> {
                                        removedFromArchive.remove(removedFromArchive.lastIndexOf(disarchivedBookmark));
                                        bookmarks.add(position, disarchivedBookmark);
                                        recyclerAdapter.notifyItemInserted(position);
                                    }).show();
                        } else {
                            Bookmark archivedBookmark = bookmarks.get(position);
                            archivedUrl.add(bookmarks.get(position));
                            bookmarks.remove(position);
                            recyclerAdapter.notifyItemRemoved(position);

                            Snackbar.make(recyclerView, archivedBookmark.getLink() + " archiviato.", Snackbar.LENGTH_LONG)
                                    .setAction("Annulla", v -> {
                                        archivedUrl.remove(archivedUrl.lastIndexOf(archivedBookmark));
                                        bookmarks.add(position, archivedBookmark);
                                        recyclerAdapter.notifyItemInserted(position);
                                    }).show();
                        }
                        break;
                    case ItemTouchHelper.RIGHT:
                        confirmDialog(bookmarks.get(position).getId(), position);
                        break;
                }
            }

            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
                if(toolbarTitle.getText().toString().equals("Archiviati")) {
                    new RecyclerViewSwipeDecorator.Builder(MainActivity.this, c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
                            .addSwipeRightBackgroundColor(ContextCompat.getColor(MainActivity.this, R.color.design_default_color_primary_dark))
                            .addSwipeRightActionIcon(R.drawable.ic_actions)
                            .addSwipeLeftBackgroundColor(ContextCompat.getColor(MainActivity.this, R.color.design_default_color_primary_dark))
                            .addSwipeLeftActionIcon(R.drawable.ic_unarchive)
                            .create()
                            .decorate();

                } else {
                    new RecyclerViewSwipeDecorator.Builder(MainActivity.this, c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
                            .addSwipeRightBackgroundColor(ContextCompat.getColor(MainActivity.this, R.color.design_default_color_primary_dark))
                            .addSwipeRightActionIcon(R.drawable.ic_actions)
                            .addSwipeLeftBackgroundColor(ContextCompat.getColor(MainActivity.this, R.color.design_default_color_primary_dark))
                            .addSwipeLeftActionIcon(R.drawable.ic_archive)
                            .create()
                            .decorate();

                }
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);

            }
        };
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleCallback);
        itemTouchHelper.attachToRecyclerView(recyclerView);
    }


    private void confirmDialog(String id, int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setMessage("Sei sicuro di voler eliminare il link?")
                .setCancelable(false)
                .setNegativeButton("No", (dialogInterface, i) -> {
                    dialogInterface.cancel();
                    recyclerAdapter.notifyDataSetChanged();
                })
                .setPositiveButton("Sì", (dialogInterface, i) -> {
                    if (db.deleteBookmark(id)) {
                        bookmarks.remove(position);
                        recyclerAdapter.notifyItemRemoved(position);
                        Toast.makeText(getApplicationContext(), "Segnalibro eliminato", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(getApplicationContext(), "Impossibile eliminare il segnalibro", Toast.LENGTH_LONG).show();
                    }
                });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    public void archiveUrl() {
        if (archivedUrl != null) {
            if (archivedUrl.size() > 0){
                for (int i = 0; i < archivedUrl.size(); i++)
                    db.addToArchive(archivedUrl.get(i).getId(), archivedUrl.get(i).getCategory());
            }
        }
    }

    public void unarchiveBookmark() {
        if (removedFromArchive != null) {
            if (removedFromArchive.size() > 0){
                for (int i = 0; i < removedFromArchive.size(); i++)
                    db.removeFromArchive(removedFromArchive.get(i).getId());
            }
        }
    }

    @SuppressLint("SetTextI18n")
    public void onResume() {
        super.onResume();
        archiveUrl();
        setBookmarksLabel();
        categories.clear();
        categories = db.getAllCategories();
        invalidateOptionsMenu();
    }

    @Override
    public boolean onLongClick(View v) {
        previousCategory = toolbarTitle.getText().toString();
        isContextualMenuEnable = true;
        toolbar.getMenu().clear();
        toolbar.inflateMenu(R.menu.contextual_menu);
        MenuItem archive = toolbar.getMenu().findItem(R.id.archive);
        MenuItem unarchive = toolbar.getMenu().findItem(R.id.unarchive);
        if (previousCategory.equals("Archiviati")) {
            archive.setVisible(false);
            unarchive.setVisible(true);
        } else {
            archive.setVisible(true);
            unarchive.setVisible(false);
        }
        toolbar.setNavigationIcon(R.drawable.ic_back_button);

        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                removeContextualActionMode();
            }
        });
        recyclerAdapter.notifyDataSetChanged();

        return true;
    }

    public void makeSelection(View view, int position) {
        if (((CheckBox)view).isChecked()) {
            selectedBookmarks.add(bookmarks.get(position));
            counter ++;
        } else {
            selectedBookmarks.remove(bookmarks.get(position));
            counter --;
        }
        updateCounter();
    }

    public void updateCounter() {
        toolbarTitle.setText(String.valueOf(counter));
    }

    public void removeContextualActionMode() {
        isContextualMenuEnable = false;
        areAllSelected = false;
        toolbarTitle.setText(previousCategory);
        toolbar.getMenu().clear();
        toolbar.setNavigationIcon(null);
        toolbar.inflateMenu(R.menu.menu);
        addFilterCategories();
        counter = 0;
        selectedBookmarks.clear();
        recyclerAdapter.notifyDataSetChanged();

    }

    private void addFilterCategories() {
        SubMenu subMenu = toolbar.getMenu().findItem(R.id.filter).getSubMenu();
        subMenu.clear();
        subMenu.add(0, 0, Menu.NONE, ALL_BOOKMARKS);
        for (int i = 0; i < categories.size(); i ++) {
            subMenu.add(0, i + 1, Menu.NONE, categories.get(i));
        }
    }

    @Override
    public void onBackPressed() {
        if (isContextualMenuEnable) {
            removeContextualActionMode();
        } else {
            finish();
        }
    }

    private void contextualModeDialog(int operation) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        String message = null;
        String bookmarkQuestion = null;
        String deletedQuestion = null;
        String bookmarkMessage = null;

        switch (operation) {
            case DELETE_OPTION:
                message = "Sei sicuro di voler eliminare ";
                if (counter > 1) {
                    bookmarkQuestion = " segnalibri?";
                    deletedQuestion = " eliminati!";
                    bookmarkMessage = "Segnalibri";
                } else {
                    bookmarkQuestion = " segnalibro?";
                    deletedQuestion = " eliminato!";
                    bookmarkMessage = "Segnalibro";
                }
                break;
            case ARCHIVE_OPTION:
                message = "Sei sicuro di voler archiviare ";
                if (counter > 1) {
                    bookmarkQuestion = " segnalibri?";
                    deletedQuestion = " archiviati!";
                    bookmarkMessage = "Segnalibri";
                } else {
                    bookmarkQuestion = " segnalibro?";
                    deletedQuestion = " archiviato!";
                    bookmarkMessage = "Segnalibro";
                }
                break;
            case UNARCHIVE_OPTION:
                message = "Sei sicuro di voler ripristinare ";
                if (counter > 1) {
                    bookmarkQuestion = " segnalibri?";
                    deletedQuestion = " ripristinati!";
                    bookmarkMessage = "Segnalibri";
                } else {
                    bookmarkQuestion = " segnalibro?";
                    deletedQuestion = " ripristinato!";
                    bookmarkMessage = "Segnalibro";
                }
                break;
        }
        String finalBookmarkMessage = bookmarkMessage;
        String finalDeletedQuestion = deletedQuestion;
        builder.setMessage(message + counter + bookmarkQuestion)
                .setCancelable(false)
                .setNegativeButton("No", (dialogInterface, i) -> {
                    dialogInterface.cancel();
                })
                .setPositiveButton("Sì", (dialogInterface, i) -> {
                    switch (operation) {
                        case DELETE_OPTION:
                            recyclerAdapter.updateBookmarks(selectedBookmarks, DELETE_OPTION);
                            break;
                        case ARCHIVE_OPTION:
                            recyclerAdapter.updateBookmarks(selectedBookmarks, ARCHIVE_OPTION);
                            break;
                        case UNARCHIVE_OPTION:
                            recyclerAdapter.updateBookmarks(selectedBookmarks, UNARCHIVE_OPTION);
                            break;
                    }
                    Toast.makeText(getApplicationContext(), finalBookmarkMessage + finalDeletedQuestion,
                            Toast.LENGTH_LONG).show();
                    removeContextualActionMode();

                });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    private void setBookmarksLabel(){
        if (bookmarks.size() == 0) {
            noBookmarks.setVisibility(View.VISIBLE);
        } else {
            noBookmarks.setVisibility(View.INVISIBLE);
        }
    }

    public void setAppTheme() {
        SettingsManager settingsManager = new SettingsManager(this, "theme");
        int theme = settingsManager.getTheme();
        switch (theme) {
            case NIGHT_MODE:
                settingsManager.setTheme(NIGHT_MODE);
                break;
            case LIGHT_MODE:
                settingsManager.setTheme(LIGHT_MODE);
                break;
            default:
                settingsManager.setTheme(SYSTEM_DEFAULT);
        }
    }
}