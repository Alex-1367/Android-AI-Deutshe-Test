package com.german.learner.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.german.learner.fragments.PlayFragment;
import com.german.learner.fragments.TestFragment;
import com.german.learner.fragments.SettingsFragment;

public class TabsPagerAdapter extends FragmentStateAdapter {

    public TabsPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return new PlayFragment();
            case 1:
                return new TestFragment();
            case 2:
                return new SettingsFragment();
            default:
                return new PlayFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 3; // Three tabs: Play, Test, Settings
    }
}