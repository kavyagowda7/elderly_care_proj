package com.example.elderlycare;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;
import java.util.ArrayList;
import java.util.List;

public class OnboardingActivity extends BaseActivity {

    private OnboardingAdapter onboardingAdapter;
    private ViewPager2 onboardingViewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        onboardingViewPager = findViewById(R.id.onboardingViewPager);
        setupOnboardingItems();
        onboardingViewPager.setAdapter(onboardingAdapter);

        Button buttonNext = findViewById(R.id.buttonOnboardingAction);
        TextView textSkip = findViewById(R.id.textSkip);

        // NEXT Button
        buttonNext.setOnClickListener(v -> {
            if (onboardingViewPager.getCurrentItem() + 1 < onboardingAdapter.getItemCount()) {
                onboardingViewPager.setCurrentItem(onboardingViewPager.getCurrentItem() + 1);
            } else {
                markOnboardingSeen();
                startActivity(new Intent(getApplicationContext(), RoleSelectionActivity.class));

                finish();
            }
        });

        // SKIP Button → directly go to login
        textSkip.setOnClickListener(v -> {
            startActivity(new Intent(getApplicationContext(), RoleSelectionActivity.class));
            finish();
        });

    }

    private void setupOnboardingItems() {
        List<OnboardingItem> onboardingItems = new ArrayList<>();

        onboardingItems.add(new OnboardingItem(
                R.drawable.onboard1,
                "Welcome to ElderlyCare",
                "Its time to putting yourself first-your peace, your health, your happiness."
        ));

        onboardingItems.add(new OnboardingItem(
                R.drawable.onboard2,
                "Taking Care of Family ",
                "When you care for your loved ones, every reminder becomes an act of love."
        ));

        onboardingItems.add(new OnboardingItem(
                R.drawable.onboard3,
                "Together We Care Better",
                "With AI and companion hand in hand, we make every day healthier and happier."
        ));

        onboardingAdapter = new OnboardingAdapter(onboardingItems);
    }

    private void markOnboardingSeen() {
        getSharedPreferences("ElderlyCarePrefs", MODE_PRIVATE)
                .edit()
                .putBoolean("onboardingShown", true)
                .apply();
    }
}
