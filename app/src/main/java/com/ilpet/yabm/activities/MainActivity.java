package com.ilpet.yabm.activities;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Canvas;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.splashscreen.SplashScreen;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.ilpet.yabm.R;
import com.ilpet.yabm.adapters.BookmarksAdapter;
import com.ilpet.yabm.adapters.CategoriesMenuAdapter;
import com.ilpet.yabm.classes.Bookmark;
import com.ilpet.yabm.classes.Category;
import com.ilpet.yabm.utils.dialogs.CategoriesSelectionDialog;
import com.ilpet.yabm.utils.DatabaseHandler;
import com.ilpet.yabm.utils.dialogs.PasswordDialog;
import com.ilpet.yabm.utils.SettingsManager;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import it.xabaras.android.recyclerview.swipedecorator.RecyclerViewSwipeDecorator;

public class MainActivity extends AppCompatActivity implements View.OnLongClickListener {
    private static final int DELETE_OPTION = 1;
    private static final int ARCHIVE_OPTION = 2;
    private static final int UNARCHIVE_OPTION = 3;
    private static final String CATEGORY = "category";
    public boolean isContextualMenuEnable = false;
    public boolean areAllSelected = false;
    public boolean isArchiveModeEnabled = false;
    private DatabaseHandler db;
    private ArrayList<Bookmark> bookmarks = new ArrayList<>();
    private RecyclerView recyclerView;
    private BookmarksAdapter bookmarksAdapter;
    private RecyclerView categoriesRecyclerview;
    private Toolbar toolbar, contextualToolbar;
    private TextView toolbarTitle;
    private DrawerLayout drawerLayout;
    private TextView noBookmarks;
    private ImageView sortOptions;
    private FloatingActionButton fab;
    private ArrayList<Category> categories;
    private ArrayList<Bookmark> archivedUrl;
    private ArrayList<Bookmark> removedFromArchive;
    private ArrayList<Bookmark> selectedBookmarks;
    private int counter = 0;
    private String previousCategory;
    private boolean refreshCategories = false;
    private SettingsManager settingsManager;
    private static final int SYSTEM_DEFAULT = 0;
    private static final int LIGHT_MODE = 1;
    private static final int NIGHT_MODE = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen splashScreen = SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);
        setAppTheme();
        db = DatabaseHandler.getInstance(getApplicationContext());
        settingsManager = new SettingsManager(getApplicationContext(), CATEGORY);
        String categoryToLoad = settingsManager.getCategory();
        setContentView(R.layout.activity_main);
        toolbar = findViewById(R.id.toolbar);
        contextualToolbar = findViewById(R.id.contextual_toolbar);
        toolbarTitle = findViewById(R.id.toolbar_title);
        drawerLayout = findViewById(R.id.drawer_layout);
        categoriesRecyclerview = findViewById(R.id.categories_recyclerview);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_menu);
        }
        sortOptions = findViewById(R.id.bookmark_options_sort);
        noBookmarks = findViewById(R.id.no_bookmarks);
        recyclerView = findViewById(R.id.recycler_view);

        if (settingsManager.isFirstAccess()) {
            SimpleDateFormat dateFormat = new SimpleDateFormat(
                    "yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            Date date = new Date();
            Category defaultCategory = new Category(null, getString(R.string.default_bookmarks),
                    ContextCompat.getDrawable(getApplicationContext(),
                            R.drawable.ic_default_icon), dateFormat.format(date),
                    Category.CategoryProtection.UNLOCK);
            Category archiveCategory = new Category(null, getString(R.string.archived_bookmarks), ContextCompat
                    .getDrawable(getApplicationContext(), R.drawable.ic_archive),
                    dateFormat.format(date), Category.CategoryProtection.UNLOCK);
            ArrayList<Category> appCategories = new ArrayList<>();
            appCategories.add(defaultCategory);
            appCategories.add(archiveCategory);
            for (Category category : appCategories) {
                db.addCategory(category);
            }
            File folder = new File(getFilesDir() + File.separator +
                    getString(R.string.app_name));
            boolean success = true;
            if (!folder.exists()) {
                success = folder.mkdirs();
            }
            if (!success) {
                Toast.makeText(getApplicationContext(), getString(R.string.something_wrong),
                        Toast.LENGTH_LONG).show();
            }
            settingsManager.setFirstAccess(false);
        }

        if (categoryToLoad.equals(getString(R.string.all_bookmarks_title))) {
            bookmarks = new ArrayList<>(db.getBookmarks(settingsManager.getBookmarkOrderBy(),
                    settingsManager.getBookmarkOrderType()));
            setAdapter();
        } else {
            if (db.getCategoryProtectionByTitle(categoryToLoad)) {
                PasswordDialog passwordDialog = new PasswordDialog(this,
                        result -> {
                            if (result) {
                                bookmarks = new ArrayList<>(db.getBookmarksByCategory(categoryToLoad,
                                        settingsManager.getBookmarkOrderBy(), settingsManager.
                                                getBookmarkOrderType()));
                                setAdapter();
                            }
                        });
                passwordDialog.show(getSupportFragmentManager(),
                        "Password dialog");
            } else {
                bookmarks = new ArrayList<>(db.getBookmarksByCategory(categoryToLoad,
                        settingsManager.getBookmarkOrderBy(), settingsManager.getBookmarkOrderType()));
                setAdapter();
            }
        }
        toolbarTitle.setText(categoryToLoad);
        previousCategory = categoryToLoad;
        archiveBookmark();
        unarchiveBookmark();

        archivedUrl = new ArrayList<>();
        selectedBookmarks = new ArrayList<>();
        removedFromArchive = new ArrayList<>();
        categories = db.getAllCategories(settingsManager.getCategoryOrderBy(), settingsManager.getCategoryOrderType());

        fab = findViewById(R.id.add_button);
        fab.setOnClickListener(view -> {
            previousCategory = toolbarTitle.getText().toString();
            Intent intent = new Intent(MainActivity.this, InsertBookmarkActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            if (isContextualMenuEnable) {
                removeContextualActionMode();
            }
        });
        setSortOptions();
    }

    @SuppressLint({"NonConstantResourceId", "NotifyDataSetChanged"})
    private void setSortOptions() {
        sortOptions.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(this, sortOptions);
            popup.getMenuInflater().inflate(R.menu.sort_options_menu, popup.getMenu());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                popup.setForceShowIcon(true);
            }

            if (settingsManager.getBookmarkOrderBy().equals(String.valueOf(SettingsManager.SortOrder.date)) &&
                    settingsManager.getBookmarkOrderType().equals(String.valueOf(SettingsManager.SortOrder.ASC))) {
                popup.getMenu().getItem(0).setChecked(true);
            } else if (settingsManager.getBookmarkOrderBy().equals(String.valueOf(SettingsManager.SortOrder.date)) &&
                    settingsManager.getBookmarkOrderType().equals(String.valueOf(SettingsManager.SortOrder.DESC))) {
                popup.getMenu().getItem(1).setChecked(true);
            } else if (settingsManager.getBookmarkOrderBy().equals(String.valueOf(SettingsManager.SortOrder.title)) &&
                    settingsManager.getBookmarkOrderType().equals(String.valueOf(SettingsManager.SortOrder.ASC))) {
                popup.getMenu().getItem(2).setChecked(true);
            } else if (settingsManager.getBookmarkOrderBy().equals(String.valueOf(SettingsManager.SortOrder.title)) &&
                    settingsManager.getBookmarkOrderType().equals(String.valueOf(SettingsManager.SortOrder.DESC))) {
                popup.getMenu().getItem(3).setChecked(true);
            }
            popup.setOnMenuItemClickListener(item -> {
                int id = item.getItemId();
                switch (id) {
                    case R.id.date_ascending:
                        bookmarks.sort(Bookmark.DateAscendingOrder);
                        item.setChecked(!item.isChecked());
                        settingsManager.setBookmarkOrderBy(SettingsManager.SortOrder.date);
                        settingsManager.setBookmarkOrderType(SettingsManager.SortOrder.ASC);
                        bookmarksAdapter.notifyDataSetChanged();
                        break;
                    case R.id.date_descending:
                        bookmarks.sort(Bookmark.DateDescendingOrder);
                        item.setChecked(!item.isChecked());
                        settingsManager.setBookmarkOrderBy(SettingsManager.SortOrder.date);
                        settingsManager.setBookmarkOrderType(SettingsManager.SortOrder.DESC);
                        bookmarksAdapter.notifyDataSetChanged();
                        break;
                    case R.id.title_ascending:
                        bookmarks.sort(Bookmark.TitleAscendingOrder);
                        item.setChecked(!item.isChecked());
                        settingsManager.setBookmarkOrderBy(SettingsManager.SortOrder.title);
                        settingsManager.setBookmarkOrderType(SettingsManager.SortOrder.ASC);
                        bookmarksAdapter.notifyDataSetChanged();
                        break;
                    case R.id.title_descending:
                        bookmarks.sort(Bookmark.TitleDescendingOrder);
                        item.setChecked(!item.isChecked());
                        settingsManager.setBookmarkOrderBy(SettingsManager.SortOrder.title);
                        settingsManager.setBookmarkOrderType(SettingsManager.SortOrder.DESC);
                        bookmarksAdapter.notifyDataSetChanged();
                        break;
                }
                return true;
            });
            popup.show();
        });
    }

    private void setAdapter() {
        setBookmarksLabel();
        initSwipe();
        bookmarksAdapter = new BookmarksAdapter(bookmarks, MainActivity.this);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getApplicationContext());
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(bookmarksAdapter);
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
                Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                if (isContextualMenuEnable) {
                    removeContextualActionMode();
                }
                if (bookmarks.size() > 0)
                    bookmarks.clear();
                break;
            case android.R.id.home:
                drawerLayout.openDrawer(GravityCompat.START);
                break;
            case R.id.search:
                SearchView searchView = (SearchView) item.getActionView();
                searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                    @Override
                    public boolean onQueryTextSubmit(String query) {
                        bookmarksAdapter.getFilter().filter(query);
                        return false;
                    }

                    @Override
                    public boolean onQueryTextChange(String newText) {
                        return false;
                    }
                });
                searchView.setOnQueryTextFocusChangeListener((view, queryTextFocused) -> {
                    if (!queryTextFocused) {
                        if (previousCategory.equals(getString(R.string.all_bookmarks_title))) {
                            bookmarks.clear();
                            bookmarks = db.getBookmarks(settingsManager.getBookmarkOrderBy(), settingsManager.getBookmarkOrderType());
                            setAdapter();
                        } else {
                            bookmarks.clear();
                            bookmarks = db.getBookmarksByCategory(previousCategory,
                                    settingsManager.getBookmarkOrderBy(), settingsManager.getBookmarkOrderType());
                            setAdapter();
                        }
                    }
                });
                break;
        }
        return true;
    }

    public void initSwipe() {
        ItemTouchHelper.SimpleCallback simpleCallback =
                new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {

                    @Override
                    public boolean onMove(@NonNull RecyclerView recyclerView,
                                          @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                        return false;
                    }

                    @Override
                    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {

                        int position = viewHolder.getAbsoluteAdapterPosition();

                        switch (direction) {
                            case ItemTouchHelper.LEFT:
                                if (toolbarTitle.getText().toString().equals(getString(R.string.archived_bookmarks))) {
                                    Bookmark unarchivedBookmark = bookmarks.get(position);
                                    removedFromArchive.add(bookmarks.get(position));
                                    bookmarksAdapter.removeBookmark(position);
                                    bookmarks.remove(position);
                                    bookmarksAdapter.notifyItemRemoved(position);

                                    Snackbar.make(recyclerView, unarchivedBookmark.getLink() + " "
                                                    + getString(R.string.removed_from_archive), Snackbar.LENGTH_LONG)
                                            .setAction(getString(R.string.cancel), v -> {
                                                removedFromArchive.remove(removedFromArchive.lastIndexOf(unarchivedBookmark));
                                                bookmarks.add(position, unarchivedBookmark);
                                                bookmarksAdapter.notifyItemInserted(position);
                                            }).show();
                                    setBookmarksLabel();
                                } else {
                                    Bookmark archivedBookmark = bookmarks.get(position);
                                    archivedUrl.add(bookmarks.get(position));
                                    bookmarks.remove(position);
                                    bookmarksAdapter.removeBookmark(position);
                                    bookmarksAdapter.notifyItemRemoved(position);

                                    Snackbar.make(recyclerView, archivedBookmark.getLink() + " "
                                                    + getString(R.string.bookmark_archived), Snackbar.LENGTH_LONG)
                                            .setAction(getString(R.string.cancel), v -> {
                                                archivedUrl.remove(archivedUrl.lastIndexOf(archivedBookmark));
                                                bookmarks.add(position, archivedBookmark);
                                                bookmarksAdapter.notifyItemInserted(position);
                                            }).show();
                                    setBookmarksLabel();
                                }
                                break;
                            case ItemTouchHelper.RIGHT:
                                confirmDialog(bookmarks.get(position).getId(), position);
                                setBookmarksLabel();
                                break;
                        }
                    }

                    @Override
                    public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView,
                                            @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
                        if (toolbarTitle.getText().toString().equals(getString(R.string.archived_bookmarks))) {
                            new RecyclerViewSwipeDecorator.Builder(MainActivity.this, c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
                                    .addSwipeRightBackgroundColor(ContextCompat.getColor(MainActivity.this, R.color.red))
                                    .addSwipeRightActionIcon(R.drawable.ic_delete)
                                    .addSwipeLeftBackgroundColor(ContextCompat.getColor(MainActivity.this, R.color.orange_dead))
                                    .addSwipeLeftActionIcon(R.drawable.ic_unarchive_30)
                                    .create()
                                    .decorate();

                        } else {
                            new RecyclerViewSwipeDecorator.Builder(MainActivity.this, c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
                                    .addSwipeRightBackgroundColor(ContextCompat.getColor(MainActivity.this, R.color.red))
                                    .addSwipeRightActionIcon(R.drawable.ic_delete)
                                    .addSwipeLeftBackgroundColor(ContextCompat.getColor(MainActivity.this, R.color.orange_dead))
                                    .addSwipeLeftActionIcon(R.drawable.ic_archive_30)
                                    .create()
                                    .decorate();

                        }
                        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);

                    }
                };
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleCallback);
        itemTouchHelper.attachToRecyclerView(recyclerView);
    }

    public void confirmDialog(String id, int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setMessage(getString(R.string.bookmark_elimination_question))
                .setCancelable(false)
                .setNegativeButton(getString(R.string.cancel), (dialogInterface, i) -> {
                    dialogInterface.cancel();
                    bookmarksAdapter.notifyDataSetChanged();
                })
                .setPositiveButton("Sì", (dialogInterface, i) -> {
                    if (db.deleteBookmark(id)) {
                        bookmarks.remove(position);
                        bookmarksAdapter.removeBookmark(position);
                        bookmarksAdapter.notifyItemRemoved(position);
                        Toast.makeText(getApplicationContext(), getString(R.string.bookmark_deleted),
                                Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(getApplicationContext(), getString(R.string.unable_delete_bookmark),
                                Toast.LENGTH_LONG).show();
                    }
                });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    public void archiveBookmark() {
        if (archivedUrl != null) {
            if (archivedUrl.size() > 0) {
                for (int i = 0; i < archivedUrl.size(); i++)
                    db.addToArchive(archivedUrl.get(i).getId(), archivedUrl.get(i).getCategory());
            }
        }
    }

    public void unarchiveBookmark() {
        if (removedFromArchive != null) {
            if (removedFromArchive.size() > 0) {
                for (int i = 0; i < removedFromArchive.size(); i++)
                    db.removeFromArchive(removedFromArchive.get(i).getId());
            }
        }
    }

    public void onResume() {
        super.onResume();
        archiveBookmark();
        addFilterCategories();
        if (previousCategory != null) {
            if (previousCategory.equals(getString(R.string.all_bookmarks_title))) {
                bookmarks = db.getBookmarks(settingsManager.getBookmarkOrderBy(), settingsManager.getBookmarkOrderType());
                setAdapter();
            } else if (db.getCategoryProtectionByTitle(previousCategory)) {
                PasswordDialog passwordDialog = new PasswordDialog(this,
                        result -> {
                            if (result) {
                                bookmarks = new ArrayList<>(db.getBookmarksByCategory(previousCategory,
                                        settingsManager.getBookmarkOrderBy(), settingsManager.
                                                getBookmarkOrderType()));
                                setAdapter();
                            }
                        });
                passwordDialog.show(getSupportFragmentManager(),
                        "Password dialog");
            } else {
                bookmarks = new ArrayList<>(db.getBookmarksByCategory(previousCategory,
                        settingsManager.getBookmarkOrderBy(), settingsManager.
                                getBookmarkOrderType()));
                setAdapter();
            }
        } else {
            SettingsManager settingsManager = new SettingsManager(getApplicationContext(), CATEGORY);
            String categoryToLoad = settingsManager.getCategory();
            if (categoryToLoad != null) {
                if (categoryToLoad.equals(getString(R.string.all_bookmarks_title))) {
                    bookmarks = db.getBookmarks(settingsManager.getBookmarkOrderBy(), settingsManager.getBookmarkOrderType());
                    setAdapter();
                } else if (db.getCategoryProtectionByTitle(categoryToLoad)) {
                    PasswordDialog passwordDialog = new PasswordDialog(this,
                            result -> {
                                if (result) {
                                    bookmarks = new ArrayList<>(db.getBookmarksByCategory(previousCategory,
                                            settingsManager.getBookmarkOrderBy(), settingsManager.
                                                    getBookmarkOrderType()));
                                    setAdapter();
                                }
                            });
                    passwordDialog.show(getSupportFragmentManager(),
                            "Password dialog");
                } else {
                    bookmarks = new ArrayList<>(db.getBookmarksByCategory(categoryToLoad,
                            settingsManager.getBookmarkOrderBy(), settingsManager.
                                    getBookmarkOrderType()));
                    setAdapter();
                }
            } else {
                toolbarTitle.setText(R.string.all_bookmarks_title);
                bookmarks = db.getBookmarks(settingsManager.getBookmarkOrderBy(), settingsManager.getBookmarkOrderType());
                setAdapter();
            }
        }
        categories.clear();
        categories = db.getAllCategories(settingsManager.getCategoryOrderBy(), settingsManager.getCategoryOrderType());
        invalidateOptionsMenu();
    }

    @Override
    public boolean onLongClick(View v) {
        refreshCategories = true;
        previousCategory = toolbarTitle.getText().toString();
        isContextualMenuEnable = true;
        fab.setVisibility(View.GONE);
        contextualToolbar.setVisibility(View.VISIBLE);
        ImageButton move = contextualToolbar.findViewById(R.id.move);
        ImageButton delete = contextualToolbar.findViewById(R.id.delete);
        ImageButton archive = contextualToolbar.findViewById(R.id.archive);
        ImageButton unarchive = contextualToolbar.findViewById(R.id.unarchive);
        ImageButton selectAll = contextualToolbar.findViewById(R.id.select_all);

        if (previousCategory.equals(getString(R.string.archived_bookmarks))) {
            unarchive.setVisibility(View.VISIBLE);
            archive.setVisibility(View.GONE);
        } else {
            archive.setVisibility(View.VISIBLE);
            unarchive.setVisibility(View.GONE);
        }

        if (previousCategory.equals(getString(R.string.all_bookmarks_title))) {
            move.setVisibility(View.GONE);
        }

        move.setOnClickListener(v12 -> {
            if (counter > 0) {
                CategoriesSelectionDialog categoriesSelection =
                        new CategoriesSelectionDialog(MainActivity.this, previousCategory, selectedBookmarks,
                                result -> {
                                    if (result) {
                                        bookmarks.removeAll(selectedBookmarks);
                                        bookmarksAdapter.notifyDataSetChanged();
                                        removeContextualActionMode();
                                        setBookmarksLabel();
                                    }
                                });
                categoriesSelection.show(getSupportFragmentManager(), "Categories dialog");
            }
        });

        delete.setOnClickListener(v12 -> {
            if (counter > 0) {
                contextualModeDialog(DELETE_OPTION);
            }
        });

        archive.setOnClickListener(v12 -> {
            if (counter > 0) {
                contextualModeDialog(ARCHIVE_OPTION);
            }
        });

        unarchive.setOnClickListener(v12 -> {
            if (counter > 0) {
                contextualModeDialog(UNARCHIVE_OPTION);
            }
        });

        selectAll.setOnClickListener(v12 -> {
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
            bookmarksAdapter.notifyDataSetChanged();
        });

        toolbar.setNavigationIcon(R.drawable.ic_back_button);
        toolbar.setNavigationOnClickListener(v1 -> removeContextualActionMode());
        bookmarksAdapter.notifyDataSetChanged();
        return true;
    }

    public void makeSelection(View view, int position) {
        if (((CheckBox) view).isChecked()) {
            selectedBookmarks.add(bookmarks.get(position));
            counter++;
        } else {
            selectedBookmarks.remove(bookmarks.get(position));
            counter--;
        }
        updateCounter();
    }

    public void updateCounter() {
        if (counter == 0) {
            toolbarTitle.setText(previousCategory);
        } else {
            toolbarTitle.setText(String.valueOf(counter));
        }
    }

    public void removeContextualActionMode() {
        isContextualMenuEnable = false;
        areAllSelected = false;
        toolbarTitle.setText(previousCategory);
        toolbar.getMenu().clear();
        toolbar.setNavigationIcon(null);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_menu);
        }
        contextualToolbar.setVisibility(View.GONE);
        fab.setVisibility(View.VISIBLE);
        counter = 0;
        selectedBookmarks.clear();
        bookmarksAdapter.notifyDataSetChanged();
    }

    private void addFilterCategories() {
        if (!refreshCategories) {
            RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
            categoriesRecyclerview.setLayoutManager(layoutManager);
            categoriesRecyclerview.setHasFixedSize(true);
            CategoriesMenuAdapter categoriesMenuAdapter = new CategoriesMenuAdapter(categories,
                    this, toolbarTitle.getText().toString());
            categoriesRecyclerview.setAdapter(categoriesMenuAdapter);
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
                message = getString(R.string.delete_question);
                if (counter > 1) {
                    bookmarkQuestion = getString(R.string.bookmarks_question);
                    deletedQuestion = getString(R.string.bookmarks_deleted_message);
                    bookmarkMessage = getString(R.string.bookmarks_message);
                } else {
                    bookmarkQuestion = getString(R.string.bookmark_question);
                    deletedQuestion = getString(R.string.bookmark_deleted_message);
                    bookmarkMessage = getString(R.string.bookmark_message);
                }
                break;
            case ARCHIVE_OPTION:
                message = getString(R.string.bookmark_archive_question);
                if (counter > 1) {
                    bookmarkQuestion = getString(R.string.bookmarks_question);
                    deletedQuestion = getString(R.string.bookmarks_archived_message);
                    bookmarkMessage = getString(R.string.bookmarks_message);
                } else {
                    bookmarkQuestion = getString(R.string.bookmark_question);
                    deletedQuestion = getString(R.string.bookmark_archived_message);
                    bookmarkMessage = getString(R.string.bookmark_message);
                }
                break;
            case UNARCHIVE_OPTION:
                message = getString(R.string.bookmark_restore_question);
                if (counter > 1) {
                    bookmarkQuestion = getString(R.string.bookmarks_question);
                    deletedQuestion = getString(R.string.bookmarks_restored_message);
                    bookmarkMessage = getString(R.string.bookmarks_message);
                } else {
                    bookmarkQuestion = getString(R.string.bookmark_question);
                    deletedQuestion = getString(R.string.bookmark_restored_message);
                    bookmarkMessage = getString(R.string.bookmark_message);
                }
                break;
        }
        String finalBookmarkMessage = bookmarkMessage;
        String finalDeletedQuestion = deletedQuestion;
        builder.setMessage(message + " " + counter + " " + bookmarkQuestion)
                .setCancelable(false)
                .setNegativeButton(getString(R.string.cancel), (dialogInterface, i) -> dialogInterface.cancel())
                .setPositiveButton(getString(R.string.ok), (dialogInterface, i) -> {
                    switch (operation) {
                        case DELETE_OPTION:
                            bookmarksAdapter.updateBookmarks(selectedBookmarks, DELETE_OPTION);
                            setBookmarksLabel();
                            break;
                        case ARCHIVE_OPTION:
                            bookmarksAdapter.updateBookmarks(selectedBookmarks, ARCHIVE_OPTION);
                            setBookmarksLabel();
                            break;
                        case UNARCHIVE_OPTION:
                            bookmarksAdapter.updateBookmarks(selectedBookmarks, UNARCHIVE_OPTION);
                            setBookmarksLabel();
                            break;
                    }

                    Toast.makeText(getApplicationContext(), finalBookmarkMessage + " " + finalDeletedQuestion,
                            Toast.LENGTH_LONG).show();
                    removeContextualActionMode();

                });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    private void setBookmarksLabel() {
        if (bookmarks.size() == 0) {
            noBookmarks.setVisibility(View.VISIBLE);
        } else {
            noBookmarks.setVisibility(View.INVISIBLE);
        }
    }

    public void filterByCategory(String categoryName) {
        if (categoryName.equals(getString(R.string.all_bookmarks_title))) {
            toolbarTitle.setText(categoryName);
            previousCategory = categoryName;
            fab.setVisibility(View.VISIBLE);
            archiveBookmark();
            unarchiveBookmark();
            bookmarks.clear();
            bookmarks = db.getBookmarks(settingsManager.getBookmarkOrderBy(), settingsManager.getBookmarkOrderType());
        } else {
            if (categoryName.equals(getString(R.string.archived_bookmarks))) {
                isArchiveModeEnabled = true;
                fab.setVisibility(View.INVISIBLE);
            } else {
                isArchiveModeEnabled = false;
                fab.setVisibility(View.VISIBLE);
            }
            toolbarTitle.setText(categoryName);
            previousCategory = categoryName;
            archiveBookmark();
            unarchiveBookmark();
            if (bookmarks.size() > 0)
                bookmarks.clear();
            bookmarks = db.getBookmarksByCategory(categoryName, settingsManager.getBookmarkOrderBy(),
                    settingsManager.getBookmarkOrderType());
        }
        setAdapter();
        drawerLayout.closeDrawers();
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