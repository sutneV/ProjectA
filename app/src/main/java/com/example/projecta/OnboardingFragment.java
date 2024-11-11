package com.example.projecta;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.airbnb.lottie.LottieAnimationView;

public class OnboardingFragment extends Fragment {
    private static final String ARG_ANIMATION = "animation";
    private static final String ARG_TITLE = "title";
    private static final String ARG_DESCRIPTION = "description";
    private static final String ARG_IS_LAST = "is_last";

    private int animationResId;
    private String title;
    private String description;
    private boolean isLast;

    public static OnboardingFragment newInstance(int animationResId, String title, String description, boolean isLast) {
        OnboardingFragment fragment = new OnboardingFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_ANIMATION, animationResId);
        args.putString(ARG_TITLE, title);
        args.putString(ARG_DESCRIPTION, description);
        args.putBoolean(ARG_IS_LAST, isLast);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            animationResId = getArguments().getInt(ARG_ANIMATION);
            title = getArguments().getString(ARG_TITLE);
            description = getArguments().getString(ARG_DESCRIPTION);
            isLast = getArguments().getBoolean(ARG_IS_LAST);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_onboarding_page, container, false);

        LottieAnimationView animationView = view.findViewById(R.id.lottie_animation_view);
        TextView titleTextView = view.findViewById(R.id.title_text_view);
        TextView descriptionTextView = view.findViewById(R.id.description_text_view);
        Button getStartedButton = view.findViewById(R.id.getStartedButton);

        // Set animation, title, and description
        animationView.setAnimation(animationResId);
        titleTextView.setText(title);
        descriptionTextView.setText(description);

        // Handle "Get Started" button visibility and click
        if (isLast) {
            getStartedButton.setVisibility(View.VISIBLE);
            getStartedButton.setOnClickListener(v -> {
                // Mark onboarding as complete
                if (getActivity() instanceof OnboardingActivity) {
                    ((OnboardingActivity) getActivity()).markOnboardingComplete();
                }
                // Navigate to LoginActivity
                Intent intent = new Intent(getActivity(), LoginActivity.class);
                startActivity(intent);
                getActivity().finish(); // Close onboarding activity
            });
        } else {
            getStartedButton.setVisibility(View.GONE);
        }

        return view;
    }
}
