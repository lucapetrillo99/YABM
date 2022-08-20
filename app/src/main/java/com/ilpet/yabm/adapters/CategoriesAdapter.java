package com.ilpet.yabm.adapters;

import static android.view.View.INVISIBLE;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.ilpet.yabm.R;
import com.ilpet.yabm.activities.CategoriesActivity;
import com.ilpet.yabm.classes.Category;
import com.ilpet.yabm.utils.DatabaseHandler;
import com.ilpet.yabm.utils.PasswordDialog;
import com.ilpet.yabm.utils.PasswordManagerDialog;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Locale;

public class CategoriesAdapter extends RecyclerView.Adapter<CategoriesAdapter.categoriesViewHolder>
        implements Filterable {
    private final ArrayList<Category> categories;
    private final CategoriesActivity categoriesActivity;
    private final ArrayList<Category> allCategories;
    private DatabaseHandler db;
    private AlertDialog dialog;

    public CategoriesAdapter(ArrayList<Category> categories, CategoriesActivity categoriesActivity) {
        this.categories = categories;
        this.categoriesActivity = categoriesActivity;
        this.allCategories = new ArrayList<>(categories);
    }

    static class categoriesViewHolder extends RecyclerView.ViewHolder {
        TextView title;
        ImageButton modify, delete;
        ImageView protection;
        CheckBox checkbox;
        View view;

        public categoriesViewHolder(@NonNull View itemView, CategoriesActivity categoriesActivity) {
            super(itemView);
            title = itemView.findViewById(R.id.title);
            modify = itemView.findViewById(R.id.modify);
            delete = itemView.findViewById(R.id.delete);
            protection = itemView.findViewById(R.id.category_protection);
            checkbox = itemView.findViewById(R.id.checkbox);
            view = itemView;
            view.setOnLongClickListener(categoriesActivity);
        }
    }

    @NonNull
    @Override
    public categoriesViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.category_list, parent, false);
        return new categoriesViewHolder(view, categoriesActivity);
    }

    @Override
    public void onBindViewHolder(@NonNull categoriesViewHolder holder, int position) {
        db = DatabaseHandler.getInstance(categoriesActivity);
        holder.title.setText(categories.get(holder.getAbsoluteAdapterPosition()).getCategoryTitle());

        if (!categories.get(holder.getAbsoluteAdapterPosition()).getCategoryTitle().
                equals(categoriesActivity.getString(R.string.default_bookmarks))) {
            if (categories.get(holder.getAbsoluteAdapterPosition()).getPasswordProtection().
                    equals(Category.CategoryProtection.LOCK)) {
                holder.protection.setVisibility(View.VISIBLE);
            } else {
                holder.protection.setVisibility(View.GONE);
            }
        }

        if (categoriesActivity.isContextualMenuEnable) {
            if (categories.get(holder.getAbsoluteAdapterPosition()).getCategoryTitle().
                    equals(categoriesActivity.getString(R.string.default_bookmarks))) {
                holder.checkbox.setVisibility(View.INVISIBLE);
            } else {
                holder.checkbox.setVisibility(View.VISIBLE);
            }
            holder.modify.setVisibility(View.INVISIBLE);
            holder.delete.setVisibility(INVISIBLE);
        } else {
            holder.checkbox.setVisibility(View.INVISIBLE);
            if (categories.get(holder.getAbsoluteAdapterPosition()).getCategoryTitle().
                    equals(categoriesActivity.getString(R.string.default_bookmarks))) {
                holder.modify.setVisibility(INVISIBLE);
                holder.delete.setVisibility(INVISIBLE);
            } else {
                holder.modify.setVisibility(View.VISIBLE);
                holder.delete.setVisibility(View.VISIBLE);
            }
        }
        holder.modify.setOnClickListener(v -> newCategoryDialog(holder.getAbsoluteAdapterPosition(),
                true, v));
        holder.delete.setOnClickListener(v -> confirmDialog(holder.getAbsoluteAdapterPosition(), v));
        holder.checkbox.setOnClickListener(v -> categoriesActivity.makeSelection(v,
                holder.getAbsoluteAdapterPosition()));

        if (categoriesActivity.areAllSelected) {
            for (int i = 0; i < categories.size(); i++) {
                holder.checkbox.setChecked(true);
            }
        } else {
            for (int i = 0; i < categories.size(); i++) {
                holder.checkbox.setChecked(false);
            }
        }
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }

    public void newCategoryDialog(int position, boolean isModify, View v) {
        LayoutInflater layoutInflater = LayoutInflater.from(v.getRootView().getContext());
        View dialogView = layoutInflater.inflate(R.layout.new_category_dialog, null);
        dialog = new AlertDialog.Builder(categoriesActivity)
                .setView(dialogView)
                .setPositiveButton(android.R.string.ok, null)
                .setNegativeButton(categoriesActivity.getString(R.string.cancel), null)
                .create();
        EditText input = dialogView.findViewById(R.id.user_input);
        TextView title = dialogView.findViewById(R.id.title);
        CheckBox protection = dialogView.findViewById(R.id.protection);
        if (isModify) {
            title.setText(R.string.modify_category_title);
            input.setText(categories.get(position).getCategoryTitle());
            protection.setChecked(categories.get(position).getPasswordProtection() ==
                    Category.CategoryProtection.LOCK);
        } else {
            title.setText(R.string.new_category_title);
            input.setHint(R.string.insert_category_title);
        }

        boolean isChecked = protection.isChecked();
        protection.setOnClickListener(view -> {
            if (isModify) {
                if (isChecked) {
                    PasswordDialog passwordDialog = new PasswordDialog(categoriesActivity,
                            result -> {
                                if (result) {
                                    protection.setChecked(false);
                                } else {
                                    protection.setChecked(true);
                                    Toast.makeText(categoriesActivity,
                                            categoriesActivity.getString(R.string.wrong_password),
                                            Toast.LENGTH_LONG).show();
                                }
                            });
                    passwordDialog.show(categoriesActivity.getSupportFragmentManager(),
                            "Password dialog");
                }
            } else {
                String password = db.getPassword();
                if (password == null) {
                    protection.setChecked(false);
                    PasswordManagerDialog passwordManagerDialog = new
                            PasswordManagerDialog(categoriesActivity,
                            protection::setChecked);
                    passwordManagerDialog.show(categoriesActivity.getSupportFragmentManager(),
                            "Password Manager dialog");
                }
            }
        });
        dialog.setOnShowListener(dialogInterface -> {
            Button button = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            button.setOnClickListener(view -> {
                SimpleDateFormat dateFormat = new SimpleDateFormat(
                        "yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                Date date = new Date();
                if (isModify) {
                    Category category = new Category();
                    category.setCategoryId(categories.get(position).getCategoryId());
                    category.setCategoryTitle(input.getText().toString());
                    category.setDate(dateFormat.format(date));
                    if (protection.isChecked()) {
                        category.setCategoryProtection(Category.CategoryProtection.LOCK);
                    } else {
                        category.setCategoryProtection(Category.CategoryProtection.UNLOCK);
                    }
                    boolean result = db.updateCategory(category);
                    if (result) {
                        Toast.makeText(v.getRootView().getContext(),
                                "Categoria modificate correttamente!", Toast.LENGTH_LONG).show();
                        dialog.dismiss();
                        categories.set(position, category);
                        notifyItemChanged(position);
                    } else {
                        Toast.makeText(v.getRootView().getContext(),
                                "Categoria già esistente!", Toast.LENGTH_LONG).show();
                    }
                    dialog.dismiss();
                } else {
                    if (!input.getText().toString().isEmpty()) {
                        Category category = new Category();
                        category.setCategoryTitle(input.getText().toString());
                        category.setDate(dateFormat.format(date));
                        if (protection.isChecked()) {
                            category.setCategoryProtection(Category.CategoryProtection.LOCK);
                        } else {
                            category.setCategoryProtection(Category.CategoryProtection.UNLOCK);
                        }
                        String categoryId = db.addCategory(category);
                        if (categoryId != null) {
                            dialog.dismiss();
                            category.setCategoryId(categoryId);
                            categories.add(category);
                            notifyItemInserted(getItemCount());
                        } else {
                            Toast.makeText(v.getRootView().getContext(),
                                    "Categoria già esistente!", Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Toast.makeText(v.getRootView().getContext(),
                                "Inserisci il nome di una categoria!", Toast.LENGTH_LONG).show();
                    }
                }
            });
        });
        dialog.show();
    }

    public void closeCategoryDialog() {
        dialog.dismiss();
    }


    private void confirmDialog(int position, View v) {
        AlertDialog.Builder builder = new AlertDialog.Builder(v.getRootView().getContext());

        builder.setMessage("Sei sicuro di voler eliminare la categoia?\nTutti i segnalibri " +
                        "verranno eliminati")
                .setCancelable(false)
                .setNegativeButton("No", (dialogInterface, i) -> dialogInterface.cancel())
                .setPositiveButton("Sì", (dialogInterface, i) -> {
                    Category category = new Category();
                    category.setCategoryId(categories.get(position).getCategoryId());
                    category.setCategoryTitle(categories.get(position).getCategoryTitle());
                    boolean result = db.deleteCategory(category);
                    if (result) {
                        categories.remove(position);
                        notifyItemRemoved(position);
                    } else {
                        Toast.makeText(v.getRootView().getContext(), "Impossibile eliminare la categoria", Toast.LENGTH_LONG).show();
                    }
                });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    public void deleteCategories(ArrayList<Category> selectedCategories) {
        categories.removeAll(selectedCategories);
        db.deleteCategories(selectedCategories);
        notifyDataSetChanged();
    }

    @Override
    public Filter getFilter() {
        return filter;
    }

    private final Filter filter = new Filter() {
        @Override
        protected Filter.FilterResults performFiltering(CharSequence constraint) {
            ArrayList<Category> filteredCategories = new ArrayList<>();
            if (constraint.toString().isEmpty()) {
                filteredCategories.addAll(allCategories);
            } else {
                for (Category category : allCategories) {
                    if (category.getCategoryTitle().toLowerCase().contains(constraint.toString().
                            toLowerCase())) {
                        filteredCategories.add(category);
                    }
                }
            }
            FilterResults filterResults = new FilterResults();
            filterResults.values = filteredCategories;
            return filterResults;
        }

        @SuppressWarnings("unchecked")
        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            categories.clear();
            categories.addAll((Collection<? extends Category>) results.values);
            notifyDataSetChanged();
        }
    };
}