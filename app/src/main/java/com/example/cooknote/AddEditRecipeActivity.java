package com.example.cooknote;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo; // ✅ Tambahan penting
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.example.cooknote.database.DatabaseHelper;
import com.example.cooknote.models.Recipe;
import com.example.cooknote.utils.SessionManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AddEditRecipeActivity extends AppCompatActivity {

    private static final int REQUEST_CAMERA = 1;
    private static final int REQUEST_GALLERY = 2;
    private static final int REQUEST_PERMISSION = 100;

    private EditText etTitle, etIngredients, etSteps;
    private ImageView ivRecipe;
    private Button btnCamera, btnGallery, btnSave;
    private Toolbar toolbar;

    private DatabaseHelper dbHelper;
    private SessionManager sessionManager;

    private String currentImagePath;
    private boolean isEditMode = false;
    private Recipe editRecipe;
    private Uri photoURI; // penting untuk Android 13+

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_recipe);

        initViews();
        initDatabase();
        setupToolbar();
        checkEditMode();
        setupListeners();
    }

    private void initViews() {
        etTitle = findViewById(R.id.et_title);
        etIngredients = findViewById(R.id.et_ingredients);
        etSteps = findViewById(R.id.et_steps);
        ivRecipe = findViewById(R.id.iv_recipe);
        btnCamera = findViewById(R.id.btn_camera);
        btnGallery = findViewById(R.id.btn_gallery);
        btnSave = findViewById(R.id.btn_save);
        toolbar = findViewById(R.id.toolbar);
    }

    private void initDatabase() {
        dbHelper = DatabaseHelper.getInstance(this);
        sessionManager = new SessionManager(this);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
    }

    private void checkEditMode() {
        Intent intent = getIntent();
        isEditMode = intent.getBooleanExtra("isEdit", false);

        if (isEditMode) {
            editRecipe = (Recipe) intent.getSerializableExtra("recipe");
            if (editRecipe != null) {
                loadRecipeData();
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setTitle("Edit Resep");
                }
            }
        } else {
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("Tambah Resep");
            }
        }
    }

    private void loadRecipeData() {
        etTitle.setText(editRecipe.getTitle());
        etIngredients.setText(editRecipe.getIngredients());
        etSteps.setText(editRecipe.getSteps());

        if (editRecipe.getImagePath() != null && !editRecipe.getImagePath().isEmpty()) {
            currentImagePath = editRecipe.getImagePath();
            File imgFile = new File(currentImagePath);
            if (imgFile.exists()) {
                ivRecipe.setImageBitmap(BitmapFactory.decodeFile(imgFile.getAbsolutePath()));
            }
        }
    }

    private void setupListeners() {
        btnCamera.setOnClickListener(v -> {
            if (checkPermissions()) {
                openCamera();
            } else {
                requestPermissions();
            }
        });

        btnGallery.setOnClickListener(v -> {
            if (checkPermissions()) {
                openGallery();
            } else {
                requestPermissions();
            }
        });

        btnSave.setOnClickListener(v -> saveRecipe());
    }

    private boolean checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                            == PackageManager.PERMISSION_GRANTED;
        } else {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                            == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA, Manifest.permission.READ_MEDIA_IMAGES},
                    REQUEST_PERMISSION);
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE},
                    REQUEST_PERMISSION);
        }
    }

    private void openCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) != null) {
            File photoFile = createImageFile();
            if (photoFile != null) {
                photoURI = FileProvider.getUriForFile(
                        this,
                        getApplicationContext().getPackageName() + ".fileprovider",
                        photoFile);

                intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);

                // ✅ Tambahan fix: grant URI permission untuk semua kamera
                List<ResolveInfo> resInfoList = getPackageManager()
                        .queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
                for (ResolveInfo resolveInfo : resInfoList) {
                    String packageName = resolveInfo.activityInfo.packageName;
                    grantUriPermission(packageName, photoURI,
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                }

                startActivityForResult(intent, REQUEST_CAMERA);
            }
        }
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_GALLERY);
    }

    private File createImageFile() {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "RECIPE_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);

        try {
            File image = File.createTempFile(imageFileName, ".jpg", storageDir);
            currentImagePath = image.getAbsolutePath();
            Log.d("CameraFile", "File created: " + currentImagePath);
            return image;
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Gagal membuat file gambar", Toast.LENGTH_SHORT).show();
            return null;
        }
    }

    private void saveRecipe() {
        String title = etTitle.getText().toString().trim();
        String ingredients = etIngredients.getText().toString().trim();
        String steps = etSteps.getText().toString().trim();

        if (TextUtils.isEmpty(title)) {
            etTitle.setError("Judul resep tidak boleh kosong");
            etTitle.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(ingredients)) {
            etIngredients.setError("Bahan-bahan tidak boleh kosong");
            etIngredients.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(steps)) {
            etSteps.setError("Langkah-langkah tidak boleh kosong");
            etSteps.requestFocus();
            return;
        }

        long result;
        if (isEditMode) {
            result = dbHelper.updateRecipe(editRecipe.getRecipeId(), title, ingredients, steps, currentImagePath);
            if (result > 0) {
                Toast.makeText(this, "Resep berhasil diupdate", Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            } else {
                Toast.makeText(this, "Gagal mengupdate resep", Toast.LENGTH_SHORT).show();
            }
        } else {
            int userId = sessionManager.getUserId();
            result = dbHelper.addRecipe(userId, title, ingredients, steps, currentImagePath);
            if (result != -1) {
                Toast.makeText(this, "Resep berhasil ditambahkan", Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            } else {
                Toast.makeText(this, "Gagal menambahkan resep", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_CAMERA) {
                if (currentImagePath != null) {
                    File imgFile = new File(currentImagePath);
                    if (imgFile.exists()) {
                        ivRecipe.setImageBitmap(BitmapFactory.decodeFile(imgFile.getAbsolutePath()));
                        Log.d("CameraResult", "Foto disimpan di: " + currentImagePath);
                    } else {
                        Toast.makeText(this, "Gagal memuat foto dari kamera", Toast.LENGTH_SHORT).show();
                    }
                }
            } else if (requestCode == REQUEST_GALLERY && data != null) {
                Uri selectedImage = data.getData();
                try {
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImage);
                    ivRecipe.setImageBitmap(bitmap);
                    currentImagePath = saveImageToInternalStorage(bitmap);
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(this, "Gagal memuat gambar", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private String saveImageToInternalStorage(Bitmap bitmap) {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "RECIPE_" + timeStamp + ".jpg";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File imageFile = new File(storageDir, imageFileName);

        try (FileOutputStream fos = new FileOutputStream(imageFile)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
            return imageFile.getAbsolutePath();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION) {
            boolean granted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    granted = false;
                    break;
                }
            }
            if (granted) {
                Toast.makeText(this, "Izin diberikan", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Izin ditolak", Toast.LENGTH_SHORT).show();
            }
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
