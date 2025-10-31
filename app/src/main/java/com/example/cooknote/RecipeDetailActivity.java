package com.example.cooknote;

import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.cooknote.models.Recipe;

import java.io.File;

public class RecipeDetailActivity extends AppCompatActivity {

    private ImageView ivRecipe;
    private TextView tvTitle, tvIngredients, tvSteps;
    private Toolbar toolbar;

    private Recipe recipe;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recipe_detail);

        initViews();
        loadRecipeData();
        setupToolbar();
    }

    private void initViews() {
        ivRecipe = findViewById(R.id.iv_recipe);
        tvTitle = findViewById(R.id.tv_title);
        tvIngredients = findViewById(R.id.tv_ingredients);
        tvSteps = findViewById(R.id.tv_steps);
        toolbar = findViewById(R.id.toolbar);
    }

    private void loadRecipeData() {
        Intent intent = getIntent();
        recipe = (Recipe) intent.getSerializableExtra("recipe");

        if (recipe != null) {
            tvTitle.setText(recipe.getTitle());

            // Format ingredients
            String[] ingredientsArray = recipe.getIngredients().split("\n");
            StringBuilder formattedIngredients = new StringBuilder();
            for (int i = 0; i < ingredientsArray.length; i++) {
                formattedIngredients.append((i + 1)).append(". ")
                        .append(ingredientsArray[i].trim()).append("\n");
            }
            tvIngredients.setText(formattedIngredients.toString().trim());

            // Format steps
            String[] stepsArray = recipe.getSteps().split("\n");
            StringBuilder formattedSteps = new StringBuilder();
            for (int i = 0; i < stepsArray.length; i++) {
                formattedSteps.append((i + 1)).append(". ")
                        .append(stepsArray[i].trim()).append("\n\n");
            }
            tvSteps.setText(formattedSteps.toString().trim());

            // Load image
            if (recipe.getImagePath() != null && !recipe.getImagePath().isEmpty()) {
                File imgFile = new File(recipe.getImagePath());
                if (imgFile.exists()) {
                    ivRecipe.setImageBitmap(BitmapFactory.decodeFile(imgFile.getAbsolutePath()));
                } else {
                    ivRecipe.setImageResource(R.drawable.ic_recipe_placeholder);
                }
            } else {
                ivRecipe.setImageResource(R.drawable.ic_recipe_placeholder);
            }
        }
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("Detail Resep");
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}