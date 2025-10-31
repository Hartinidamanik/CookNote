package com.example.cooknote.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.example.cooknote.models.Recipe;
import com.example.cooknote.models.User;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String TAG = "DatabaseHelper";
    private static final String DATABASE_NAME = "cooknote_db";
    private static final int DATABASE_VERSION = 1;

    // Table User
    private static final String TABLE_USER = "user";
    private static final String COL_USER_ID = "user_id";
    private static final String COL_USERNAME = "username";
    private static final String COL_PASSWORD = "password";

    // Table Recipe
    private static final String TABLE_RECIPE = "recipe";
    private static final String COL_RECIPE_ID = "recipe_id";
    private static final String COL_RECIPE_USER_ID = "user_id";
    private static final String COL_TITLE = "title";
    private static final String COL_INGREDIENTS = "ingredients";
    private static final String COL_STEPS = "steps";
    private static final String COL_IMAGE = "image";

    private static DatabaseHelper instance;

    public static synchronized DatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }

    private DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createUserTable = "CREATE TABLE " + TABLE_USER + " (" +
                COL_USER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_USERNAME + " TEXT UNIQUE NOT NULL, " +
                COL_PASSWORD + " TEXT NOT NULL)";

        String createRecipeTable = "CREATE TABLE " + TABLE_RECIPE + " (" +
                COL_RECIPE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_RECIPE_USER_ID + " INTEGER NOT NULL, " +
                COL_TITLE + " TEXT NOT NULL, " +
                COL_INGREDIENTS + " TEXT NOT NULL, " +
                COL_STEPS + " TEXT NOT NULL, " +
                COL_IMAGE + " TEXT, " +
                "FOREIGN KEY(" + COL_RECIPE_USER_ID + ") REFERENCES " +
                TABLE_USER + "(" + COL_USER_ID + ") ON DELETE CASCADE)";

        db.execSQL(createUserTable);
        db.execSQL(createRecipeTable);

        Log.d(TAG, "Database created successfully");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_RECIPE);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USER);
        onCreate(db);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    // ===== USER OPERATIONS =====

    public long registerUser(String username, String password) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_USERNAME, username);
        values.put(COL_PASSWORD, hashPassword(password));

        long result = db.insert(TABLE_USER, null, values);
        db.close();

        if (result != -1) {
            Log.d(TAG, "User registered: " + username);
        } else {
            Log.e(TAG, "Failed to register user: " + username);
        }

        return result;
    }

    public User loginUser(String username, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        String hashedPassword = hashPassword(password);

        Cursor cursor = db.query(TABLE_USER,
                new String[]{COL_USER_ID, COL_USERNAME},
                COL_USERNAME + "=? AND " + COL_PASSWORD + "=?",
                new String[]{username, hashedPassword},
                null, null, null);

        User user = null;
        if (cursor != null && cursor.moveToFirst()) {
            int userId = cursor.getInt(cursor.getColumnIndexOrThrow(COL_USER_ID));
            String userName = cursor.getString(cursor.getColumnIndexOrThrow(COL_USERNAME));
            user = new User(userId, userName);
            Log.d(TAG, "User logged in: " + username);
        }

        if (cursor != null) {
            cursor.close();
        }
        db.close();

        return user;
    }

    public boolean isUsernameExists(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USER,
                new String[]{COL_USER_ID},
                COL_USERNAME + "=?",
                new String[]{username},
                null, null, null);

        boolean exists = cursor != null && cursor.getCount() > 0;

        if (cursor != null) {
            cursor.close();
        }
        db.close();

        return exists;
    }

    // ===== RECIPE OPERATIONS =====

    public long addRecipe(int userId, String title, String ingredients, String steps, String imagePath) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_RECIPE_USER_ID, userId);
        values.put(COL_TITLE, title);
        values.put(COL_INGREDIENTS, ingredients);
        values.put(COL_STEPS, steps);
        values.put(COL_IMAGE, imagePath);

        long result = db.insert(TABLE_RECIPE, null, values);
        db.close();

        if (result != -1) {
            Log.d(TAG, "Recipe added: " + title);
        } else {
            Log.e(TAG, "Failed to add recipe: " + title);
        }

        return result;
    }

    public List<Recipe> getAllRecipesByUser(int userId) {
        List<Recipe> recipes = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TABLE_RECIPE,
                null,
                COL_RECIPE_USER_ID + "=?",
                new String[]{String.valueOf(userId)},
                null, null,
                COL_RECIPE_ID + " DESC");

        if (cursor != null && cursor.moveToFirst()) {
            do {
                Recipe recipe = new Recipe();
                recipe.setRecipeId(cursor.getInt(cursor.getColumnIndexOrThrow(COL_RECIPE_ID)));
                recipe.setUserId(cursor.getInt(cursor.getColumnIndexOrThrow(COL_RECIPE_USER_ID)));
                recipe.setTitle(cursor.getString(cursor.getColumnIndexOrThrow(COL_TITLE)));
                recipe.setIngredients(cursor.getString(cursor.getColumnIndexOrThrow(COL_INGREDIENTS)));
                recipe.setSteps(cursor.getString(cursor.getColumnIndexOrThrow(COL_STEPS)));
                recipe.setImagePath(cursor.getString(cursor.getColumnIndexOrThrow(COL_IMAGE)));
                recipes.add(recipe);
            } while (cursor.moveToNext());

            cursor.close();
        }

        db.close();
        Log.d(TAG, "Retrieved " + recipes.size() + " recipes for user " + userId);

        return recipes;
    }

    public Recipe getRecipeById(int recipeId) {
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TABLE_RECIPE,
                null,
                COL_RECIPE_ID + "=?",
                new String[]{String.valueOf(recipeId)},
                null, null, null);

        Recipe recipe = null;
        if (cursor != null && cursor.moveToFirst()) {
            recipe = new Recipe();
            recipe.setRecipeId(cursor.getInt(cursor.getColumnIndexOrThrow(COL_RECIPE_ID)));
            recipe.setUserId(cursor.getInt(cursor.getColumnIndexOrThrow(COL_RECIPE_USER_ID)));
            recipe.setTitle(cursor.getString(cursor.getColumnIndexOrThrow(COL_TITLE)));
            recipe.setIngredients(cursor.getString(cursor.getColumnIndexOrThrow(COL_INGREDIENTS)));
            recipe.setSteps(cursor.getString(cursor.getColumnIndexOrThrow(COL_STEPS)));
            recipe.setImagePath(cursor.getString(cursor.getColumnIndexOrThrow(COL_IMAGE)));
            cursor.close();
        }

        db.close();
        return recipe;
    }

    public int updateRecipe(int recipeId, String title, String ingredients, String steps, String imagePath) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_TITLE, title);
        values.put(COL_INGREDIENTS, ingredients);
        values.put(COL_STEPS, steps);
        if (imagePath != null) {
            values.put(COL_IMAGE, imagePath);
        }

        int result = db.update(TABLE_RECIPE, values,
                COL_RECIPE_ID + "=?",
                new String[]{String.valueOf(recipeId)});
        db.close();

        if (result > 0) {
            Log.d(TAG, "Recipe updated: " + recipeId);
        } else {
            Log.e(TAG, "Failed to update recipe: " + recipeId);
        }

        return result;
    }

    public int deleteRecipe(int recipeId) {
        SQLiteDatabase db = this.getWritableDatabase();
        int result = db.delete(TABLE_RECIPE,
                COL_RECIPE_ID + "=?",
                new String[]{String.valueOf(recipeId)});
        db.close();

        if (result > 0) {
            Log.d(TAG, "Recipe deleted: " + recipeId);
        } else {
            Log.e(TAG, "Failed to delete recipe: " + recipeId);
        }

        return result;
    }

    public List<Recipe> searchRecipes(int userId, String query) {
        List<Recipe> recipes = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String searchQuery = "%" + query + "%";
        Cursor cursor = db.query(TABLE_RECIPE,
                null,
                COL_RECIPE_USER_ID + "=? AND (" + COL_TITLE + " LIKE ? OR " +
                        COL_INGREDIENTS + " LIKE ?)",
                new String[]{String.valueOf(userId), searchQuery, searchQuery},
                null, null,
                COL_RECIPE_ID + " DESC");

        if (cursor != null && cursor.moveToFirst()) {
            do {
                Recipe recipe = new Recipe();
                recipe.setRecipeId(cursor.getInt(cursor.getColumnIndexOrThrow(COL_RECIPE_ID)));
                recipe.setUserId(cursor.getInt(cursor.getColumnIndexOrThrow(COL_RECIPE_USER_ID)));
                recipe.setTitle(cursor.getString(cursor.getColumnIndexOrThrow(COL_TITLE)));
                recipe.setIngredients(cursor.getString(cursor.getColumnIndexOrThrow(COL_INGREDIENTS)));
                recipe.setSteps(cursor.getString(cursor.getColumnIndexOrThrow(COL_STEPS)));
                recipe.setImagePath(cursor.getString(cursor.getColumnIndexOrThrow(COL_IMAGE)));
                recipes.add(recipe);
            } while (cursor.moveToNext());

            cursor.close();
        }

        db.close();
        return recipes;
    }

    // ===== HELPER METHODS =====

    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes());
            StringBuilder hexString = new StringBuilder();

            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }

            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "Error hashing password", e);
            return password;
        }
    }
}