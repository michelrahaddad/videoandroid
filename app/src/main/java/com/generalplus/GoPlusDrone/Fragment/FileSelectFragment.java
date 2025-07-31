package com.generalplus.GoPlusDrone.Fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.generalplus.GoPlusDrone.R;

/**
 * A fragment that simply hosts a {@link TabFragment}. This class exists to
 * provide a container for the tabbed interface used to select photos or
 * videos.
 */
public class FileSelectFragment extends Fragment {
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_fileselect, container, false);
        if (savedInstanceState == null) {
            FragmentTransaction transaction = requireActivity().getSupportFragmentManager().beginTransaction();
            Fragment fragment = TabFragment.newInstance();
            transaction.replace(R.id.sample_content_fragment, fragment);
            transaction.commit();
        }
        return view;
    }
}