package com.example.projecta;

import android.os.Bundle;
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
        setContentView(R.layout.activity_onboarding);

        viewPager = findViewById(R.id.viewPager);
        fragmentList = new ArrayList<>();

        // Create fragments with different Lottie animations
        fragmentList.add(OnboardingFragment.newInstance(R.raw.onboarding_anim1, "Welcome", "This is the first onboarding screen.", false));
        fragmentList.add(OnboardingFragment.newInstance(R.raw.onboarding_anim2, "Discover", "Explore the features of the app.", false));
        fragmentList.add(OnboardingFragment.newInstance(R.raw.onboarding_anim3, "Get Started", "Let's get started with the app.", true)); // Last screen has the button

        pagerAdapter = new OnboardingPagerAdapter(this, fragmentList);
        viewPager.setAdapter(pagerAdapter);
    }
}

