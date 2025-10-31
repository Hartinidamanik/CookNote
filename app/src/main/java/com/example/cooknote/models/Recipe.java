package com.example.cooknote.models;

import java.io.Serializable;

public class Recipe implements Serializable {
    private int recipeId;
    private int userId;
    private String title;
    private String ingredients;
    private String steps;
    private String imagePath;

    public Recipe() {
    }

    public Recipe(int recipeId, int userId, String title, String ingredients,
                  String steps, String imagePath) {
        this.recipeId = recipeId;
        this.userId = userId;
        this.title = title;
        this.ingredients = ingredients;
        this.steps = steps;
        this.imagePath = imagePath;
    }

    public int getRecipeId() {
        return recipeId;
    }

    public void setRecipeId(int recipeId) {
        this.recipeId = recipeId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getIngredients() {
        return ingredients;
    }

    public void setIngredients(String ingredients) {
        this.ingredients = ingredients;
    }

    public String getSteps() {
        return steps;
    }

    public void setSteps(String steps) {
        this.steps = steps;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }
}