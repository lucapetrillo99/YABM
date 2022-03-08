package com.ilpet.yabm.activities;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.ilpet.yabm.R;
import com.ilpet.yabm.adapters.CategoriesAdapter;
import com.ilpet.yabm.classes.Category;
import com.ilpet.yabm.utils.DatabaseHandler;
import com.ilpet.yabm.utils.SettingsManager;
import com.ilpet.yabm.utils.StoragePermissionDialog;

import java.util.ArrayList;
import java.util.Collections;

public class CategoriesActivity extends AppCompatActivity implements View.OnLongClickListener {
    public static final int PERMISSION_REQUEST_STORAGE = 1000;
    public boolean areAllSelected = false;
    public boolean isContextualMenuEnable = false;
    private RecyclerView recyclerView;
    private CategoriesAdapter categoriesAdapter;
    private Toolbar toolbar;
    private TextView toolbarTitle;
    private ImageView sortOptions;
    private ArrayList<Category> categories;
    private ArrayList<Category> selectedCategories;
    private int selectedCategory = 0;
    private SettingsManager settingsManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_categories);

        toolbar = findViewById(R.id.toolbar);
        toolbarTitle = findViewById(R.id.toolbar_title);
        toolbarTitle.setText(R.string.categories_title);
        setSupportActionBar(toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_back_button);

        toolbar.setNavigationOnClickListener(v -> {
            finish();
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        });
        sortOptions = findViewById(R.id.category_options_sort);
        recyclerView = findViewById(R.id.recycler_view);
        FloatingActionButton insertCategory = findViewById(R.id.add_button);

        DatabaseHandler db = DatabaseHandler.getInstance(getApplicationContext());
        settingsManager = new SettingsManager(getApplicationContext(), "category");
        categories = db.getCategories(settingsManager.getCategoryOrderBy(), settingsManager.getCategoryOrderType());
        selectedCategories = new ArrayList<>();
        setAdapter();
        setSortOptions();
        insertCategory.setOnClickListener(view -> categoriesAdapter.newCategoryDialog(0, false, view));
    }

    @SuppressLint({"NonConstantResourceId", "NotifyDataSetChanged"})
    private void setSortOptions() {
        sortOptions.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(this, sortOptions);
            popup.getMenuInflater().inflate(R.menu.sort_options_menu, popup.getMenu());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                popup.setForceShowIcon(true);
            }

            if (settingsManager.getCategoryOrderBy().equals(String.valueOf(SettingsManager.SortOrder.date)) &&
                    settingsManager.getCategoryOrderType().equals(String.valueOf(SettingsManager.SortOrder.ASC))) {
                popup.getMenu().getItem(0).setChecked(true);
            } else if (settingsManager.getCategoryOrderBy().equals(String.valueOf(SettingsManager.SortOrder.date)) &&
                    settingsManager.getCategoryOrderType().equals(String.valueOf(SettingsManager.SortOrder.DESC))) {
                popup.getMenu().getItem(1).setChecked(true);
            } else if (settingsManager.getCategoryOrderBy().equals(String.valueOf(SettingsManager.SortOrder.title)) &&
                    settingsManager.getCategoryOrderType().equals(String.valueOf(SettingsManager.SortOrder.ASC))) {
                popup.getMenu().getItem(2).setChecked(true);
            } else if (settingsManager.getCategoryOrderBy().equals(String.valueOf(SettingsManager.SortOrder.title)) &&
                    settingsManager.getCategoryOrderType().equals(String.valueOf(SettingsManager.SortOrder.DESC))) {
                popup.getMenu().getItem(3).setChecked(true);
            }

            popup.setOnMenuItemClickListener(item -> {
                int id = item.getItemId();
                switch (id) {
                    case R.id.date_ascending:
                        Collections.sort(categories, Category.DateAscendingOrder);
                        item.setChecked(!item.isChecked());
                        settingsManager.setCategoryOrderBy(SettingsManager.SortOrder.date);
                        settingsManager.setCategoryOrderType(SettingsManager.SortOrder.ASC);
                        categoriesAdapter.notifyDataSetChanged();
                        break;
                    case R.id.date_descending:
                        Collections.sort(categories, Category.DateDescendingOrder);
                        item.setChecked(!item.isChecked());
                        settingsManager.setCategoryOrderBy(SettingsManager.SortOrder.date);
                        settingsManager.setCategoryOrderType(SettingsManager.SortOrder.DESC);
                        categoriesAdapter.notifyDataSetChanged();
                        break;
                    case R.id.title_ascending:
                        Collections.sort(categories, Category.TitleAscendingOrder);
                        item.setChecked(!item.isChecked());
                        settingsManager.setCategoryOrderBy(SettingsManager.SortOrder.title);
                        settingsManager.setCategoryOrderType(SettingsManager.SortOrder.ASC);
                        categoriesAdapter.notifyDataSetChanged();
                        break;
                    case R.id.title_descending:
                        Collections.sort(categories, Category.TitleDescendingOrder);
                        item.setChecked(!item.isChecked());
                        settingsManager.setCategoryOrderBy(SettingsManager.SortOrder.title);
                        settingsManager.setCategoryOrderType(SettingsManager.SortOrder.DESC);
                        categoriesAdapter.notifyDataSetChanged();
                        break;
                }
                return true;
            });
            popup.show();
        });
    }

    private void setAdapter() {
        categoriesAdapter = new CategoriesAdapter(categories, this);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getApplicationContext());
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(categoriesAdapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        MenuItem settings = menu.findItem(R.id.settings);
        settings.setVisible(false);
        return super.onCreateOptionsMenu(menu);
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.settings:
                Intent activityIntent = new Intent(CategoriesActivity.this, SettingsActivity.class);
                startActivity(activityIntent);
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
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
                        categoriesAdapter.getFilter().filter(newText);
                        return false;
                    }
                });
                break;
            case R.id.delete:
                if (selectedCategory > 0) {
                    confirmDeletionDialog();
                } else {
                    Toast.makeText(getApplicationContext(),
                            "Seleziona una categoria", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.select_all:
                if (!areAllSelected) {
                    areAllSelected = true;
                    selectedCategories.addAll(categories);
                    selectedCategory = categories.size() - 1;
                } else {
                    areAllSelected = false;
                    selectedCategories.removeAll(categories);
                    selectedCategory = 0;
                }
                updateCounter();
                categoriesAdapter.notifyDataSetChanged();
        }
        return true;
    }

    public void makeSelection(View v, int position) {
        if (((CheckBox) v).isChecked()) {
            selectedCategories.add(categories.get(position));
            selectedCategory++;
        } else {
            selectedCategories.remove(categories.get(position));
            selectedCategory--;
        }
        updateCounter();
    }

    public void updateCounter() {
        toolbarTitle.setText(String.valueOf(selectedCategory));
    }

    private void removeContextualActionMode() {
        isContextualMenuEnable = false;
        areAllSelected = false;
        toolbarTitle.setText(R.string.categories_title);
        toolbar.getMenu().clear();
        toolbar.setNavigationIcon(R.drawable.ic_back_button);
        toolbar.setNavigationOnClickListener(v -> {
            finish();
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        });
        toolbar.inflateMenu(R.menu.menu);
        selectedCategory = 0;
        selectedCategories.clear();
        categoriesAdapter.notifyDataSetChanged();

    }

    @Override
    public boolean onLongClick(View v) {
        if (categories.size() > 1) {
            isContextualMenuEnable = true;
            toolbar.getMenu().clear();
            toolbar.inflateMenu(R.menu.contextual_menu);
            MenuItem archive = toolbar.getMenu().findItem(R.id.archive);
            MenuItem unarchive = toolbar.getMenu().findItem(R.id.unarchive);
            archive.setVisible(false);
            unarchive.setVisible(false);
            toolbar.setNavigationIcon(R.drawable.ic_back_button);
            toolbar.setNavigationOnClickListener(v1 -> removeContextualActionMode());
            categoriesAdapter.notifyDataSetChanged();
        }

        return false;
    }

    private void confirmDeletionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        String message;
        String categoryQuestion;
        String deletedQuestion;
        String categoryMessage;

        message = "Sei sicuro di voler eliminare ";
        if (selectedCategory > 1) {
            categoryQuestion = " categorie?";
            deletedQuestion = " eliminate!";
            categoryMessage = "Categorie";
        } else {
            categoryQuestion = " categoria?";
            deletedQuestion = " eliminata!";
            categoryMessage = "Categoria";
        }

        String finalBookmarkMessage = categoryMessage;
        String finalDeletedQuestion = deletedQuestion;
        builder.setMessage(message + selectedCategory + categoryQuestion)
                .setCancelable(false)
                .setNegativeButton("No", (dialogInterface, i) -> dialogInterface.cancel())
                .setPositiveButton("Sì", (dialogInterface, i) -> {
                    categoriesAdapter.updateCategories(selectedCategories);
                    Toast.makeText(getApplicationContext(), finalBookmarkMessage + finalDeletedQuestion,
                            Toast.LENGTH_LONG).show();
                    removeContextualActionMode();
                });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_STORAGE) {
            if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                categoriesAdapter.closeCategoryDialog();
                StoragePermissionDialog storagePermissionDialog = new StoragePermissionDialog(this);
                storagePermissionDialog.showWarningMessage();
            }
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }
}