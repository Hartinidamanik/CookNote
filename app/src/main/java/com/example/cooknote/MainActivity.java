package com.example.cooknote;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cooknote.adapters.RecipeAdapter;
import com.example.cooknote.database.DatabaseHelper;
import com.example.cooknote.models.Recipe;
import com.example.cooknote.utils.SessionManager;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements RecipeAdapter.OnRecipeClickListener {

    private static final int REQUEST_ADD_RECIPE = 1;
    private static final int REQUEST_EDIT_RECIPE = 2;

    private RecyclerView recyclerView;
    private RecipeAdapter adapter;
    private FloatingActionButton fabAdd;
    private TextView tvEmpty;
    private Toolbar toolbar;

    private DatabaseHelper dbHelper;
    private SessionManager sessionManager;
    private List<Recipe> recipeList;
    private int userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        initDatabase();
        setupToolbar();
        setupRecyclerView();
        setupFab();
        loadRecipes();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recycler_view);
        fabAdd = findViewById(R.id.fab_add);
        tvEmpty = findViewById(R.id.tv_empty);
        toolbar = findViewById(R.id.toolbar);
    }

    private void initDatabase() {
        dbHelper = DatabaseHelper.getInstance(this);
        sessionManager = new SessionManager(this);
        userId = sessionManager.getUserId();
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("My Recipes - " + sessionManager.getUsername());
        }
    }

    private void setupRecyclerView() {
        recipeList = new ArrayList<>();
        adapter = new RecipeAdapter(this, recipeList, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void setupFab() {
        fabAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, AddEditRecipeActivity.class);
                startActivityForResult(intent, REQUEST_ADD_RECIPE);
            }
        });
    }

    private void loadRecipes() {
        recipeList.clear();
        recipeList.addAll(dbHelper.getAllRecipesByUser(userId));
        adapter.notifyDataSetChanged();

        if (recipeList.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onRecipeClick(Recipe recipe) {
        Intent intent = new Intent(this, RecipeDetailActivity.class);
        intent.putExtra("recipe", recipe);
        startActivity(intent);
    }

    @Override
    public void onEditClick(Recipe recipe) {
        Intent intent = new Intent(this, AddEditRecipeActivity.class);
        intent.putExtra("recipe", recipe);
        intent.putExtra("isEdit", true);
        startActivityForResult(intent, REQUEST_EDIT_RECIPE);
    }

    @Override
    public void onDeleteClick(final Recipe recipe) {
        new AlertDialog.Builder(this)
                .setTitle("Hapus Resep")
                .setMessage("Apakah Anda yakin ingin menghapus resep \"" + recipe.getTitle() + "\"?")
                .setPositiveButton("Hapus", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        int result = dbHelper.deleteRecipe(recipe.getRecipeId());
                        if (result > 0) {
                            Toast.makeText(MainActivity.this, "Resep berhasil dihapus",
                                    Toast.LENGTH_SHORT).show();
                            loadRecipes();
                        } else {
                            Toast.makeText(MainActivity.this, "Gagal menghapus resep",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);

        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) searchItem.getActionView();

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                searchRecipes(newText);
                return true;
            }
        });

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_logout) {
            handleLogout();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void searchRecipes(String query) {
        if (query.isEmpty()) {
            loadRecipes();
        } else {
            recipeList.clear();
            recipeList.addAll(dbHelper.searchRecipes(userId, query));
            adapter.notifyDataSetChanged();

            if (recipeList.isEmpty()) {
                tvEmpty.setText("Tidak ada resep yang cocok dengan pencarian");
                tvEmpty.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
            } else {
                tvEmpty.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
            }
        }
    }

    private void handleLogout() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Apakah Anda yakin ingin keluar?")
                .setPositiveButton("Ya", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        sessionManager.logout();
                        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    }
                })
                .setNegativeButton("Tidak", null)
                .show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            loadRecipes();
        }
    }
}