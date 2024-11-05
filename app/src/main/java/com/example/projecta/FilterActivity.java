// FilterActivity.java
package com.example.projecta;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import androidx.appcompat.app.AppCompatActivity;

public class FilterActivity extends AppCompatActivity {

    private RadioGroup categoryGroup;
    private Button applyButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_filter);

        categoryGroup = findViewById(R.id.category_options);
        applyButton = findViewById(R.id.apply_button);

        applyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Get selected category
                int selectedId = categoryGroup.getCheckedRadioButtonId();
                String selectedCategory = "Show all";

                if (selectedId != -1) {
                    RadioButton selectedRadioButton = findViewById(selectedId);
                    selectedCategory = selectedRadioButton.getText().toString();
                }

                // Return selected category to BrowseFragment
                Intent resultIntent = new Intent();
                resultIntent.putExtra("selected_category", selectedCategory);
                setResult(RESULT_OK, resultIntent);
                finish(); // Close FilterActivity
            }
        });
    }
}
