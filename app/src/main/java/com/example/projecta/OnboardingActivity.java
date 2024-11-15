package com.example.projecta;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import java.util.ArrayList;
import java.util.List;

public class OnboardingActivity extends AppCompatActivity {
    private ViewPager2 viewPager;
    private OnboardingPagerAdapter pagerAdapter;
    private List<Fragment> fragmentList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Check if onboarding has been completed
        SharedPreferences preferences = getSharedPreferences("OnboardingPrefs", MODE_PRIVATE);
        boolean isOnboardingCompleted = preferences.getBoolean("OnboardingCompleted", false);

        if (isOnboardingCompleted) {
            // Skip onboarding and go straight to LoginActivity
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        // If onboarding not completed, proceed with showing it
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_onboarding);

        viewPager = findViewById(R.id.viewPager);
        fragmentList = new ArrayList<>();

        // Create fragments with different Lottie animations
        fragmentList.add(OnboardingFragment.newInstance(R.raw.onboarding_anim1, "Save More, Waste Less", "Help reduce food waste by rescuing delicious meals from your favorite local restaurants. Grab your surprise meal at a fraction of the price and make a positive impact today!", false));
        fragmentList.add(OnboardingFragment.newInstance(R.raw.onboarding_anim2, "Rescue Tasty Treats", "Enjoy fresh, high-quality meals that would otherwise go to waste. Discover hidden culinary gems and do your part for the planet, all while saving money!", false));
        fragmentList.add(OnboardingFragment.newInstance(R.raw.onboarding_anim3, "Join the Food Revolution", "Be a part of the change! Fight food waste by picking up surplus meals from local eateries at a discount. Eat well, save big, and help build a sustainable future.", true)); // Last screen has the button

        pagerAdapter = new OnboardingPagerAdapter(this, fragmentList);
        viewPager.setAdapter(pagerAdapter);
    }

    public void markOnboardingComplete() {
        // Save the completion flag in SharedPreferences
        SharedPreferences preferences = getSharedPreferences("OnboardingPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("OnboardingCompleted", true);
        editor.apply();
    }
}
