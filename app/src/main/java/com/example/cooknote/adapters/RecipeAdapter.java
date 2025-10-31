package com.example.cooknote.adapters;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cooknote.R;
import com.example.cooknote.models.Recipe;

import java.io.File;
import java.util.List;

public class RecipeAdapter extends RecyclerView.Adapter<RecipeAdapter.RecipeViewHolder> {

    private Context context;
    private List<Recipe> recipes;
    private OnRecipeClickListener listener;

    public interface OnRecipeClickListener {
        void onRecipeClick(Recipe recipe);
        void onEditClick(Recipe recipe);
        void onDeleteClick(Recipe recipe);
    }

    public RecipeAdapter(Context context, List<Recipe> recipes, OnRecipeClickListener listener) {
        this.context = context;
        this.recipes = recipes;
        this.listener = listener;
    }

    @NonNull
    @Override
    public RecipeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_recipe, parent, false);
        return new RecipeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecipeViewHolder holder, int position) {
        Recipe recipe = recipes.get(position);

        holder.tvTitle.setText(recipe.getTitle());

        String[] ingredientsArray = recipe.getIngredients().split("\n");
        int ingredientCount = ingredientsArray.length;
        holder.tvIngredients.setText(ingredientCount + " bahan");

        String[] stepsArray = recipe.getSteps().split("\n");
        int stepCount = stepsArray.length;
        holder.tvSteps.setText(stepCount + " langkah");

        if (recipe.getImagePath() != null && !recipe.getImagePath().isEmpty()) {
            File imgFile = new File(recipe.getImagePath());
            if (imgFile.exists()) {
                holder.ivRecipe.setImageBitmap(BitmapFactory.decodeFile(imgFile.getAbsolutePath()));
            } else {
                holder.ivRecipe.setImageResource(R.drawable.ic_recipe_placeholder);
            }
        } else {
            holder.ivRecipe.setImageResource(R.drawable.ic_recipe_placeholder);
        }

        holder.cardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onRecipeClick(recipe);
            }
        });

        holder.btnEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onEditClick(recipe);
            }
        });

        holder.btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onDeleteClick(recipe);
            }
        });
    }

    @Override
    public int getItemCount() {
        return recipes.size();
    }

    static class RecipeViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        ImageView ivRecipe;
        TextView tvTitle, tvIngredients, tvSteps;
        ImageButton btnEdit, btnDelete;

        public RecipeViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.card_view);
            ivRecipe = itemView.findViewById(R.id.iv_recipe);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvIngredients = itemView.findViewById(R.id.tv_ingredients);
            tvSteps = itemView.findViewById(R.id.tv_steps);
            btnEdit = itemView.findViewById(R.id.btn_edit);
            btnDelete = itemView.findViewById(R.id.btn_delete);
        }
    }
}