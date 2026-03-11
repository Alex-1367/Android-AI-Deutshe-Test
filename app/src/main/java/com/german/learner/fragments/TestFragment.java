package com.german.learner.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.german.learner.R;

public class TestFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_test, container, false);

        TextView messageView = view.findViewById(R.id.test_message);
        messageView.setText("Test module coming soon...\n\nThis will contain:\n• Grammar exercises\n• Vocabulary tests\n• Listening comprehension\n• Speaking practice");

        return view;
    }
}