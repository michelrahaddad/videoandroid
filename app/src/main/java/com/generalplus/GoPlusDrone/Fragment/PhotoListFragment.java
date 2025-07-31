package com.generalplus.GoPlusDrone.Fragment;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.GridView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.generalplus.GoPlusDrone.Activity.FullImageActivity;
import com.generalplus.GoPlusDrone.R;

import java.io.File;
import java.util.HashMap;

/**
 * Fragment that displays a grid of photo thumbnails stored on external storage. When
 * not in edit mode, tapping a photo opens it in a full‑screen viewer. In edit
 * mode, multiple photos can be selected for deletion or copying via the base
 * fragment controls.
 */
public class PhotoListFragment extends BaseFragment {
    private GridView m_GridView = null;
    private String strSaveDirectory;

    /**
     * Create a new instance of this fragment with a title.
     */
    public static PhotoListFragment newInstance(String title) {
        PhotoListFragment f = new PhotoListFragment();
        f.setTitle(title);
        Bundle args = new Bundle();
        args.putString(DATA_NAME, title);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {
            getFile();
        }
    }

    private void getFile() {
        listImageItem.clear();
        m_ayFilePath.clear();
        strSaveDirectory = Environment.getExternalStorageDirectory().getPath() + "/GoPlus_Drone/Photo/";
        File dir = new File(strSaveDirectory);
        String[] children = dir.list();
        if (children != null) {
            for (int i = 0; i < children.length; i++) {
                String filename = children[i];
                if (filename.contains(".jpg")) {
                    m_ayFilePath.add(strSaveDirectory + "/" + filename);
                    HashMap<String, Object> map = new HashMap<>();
                    map.put(KEY_ThumbnailFilePath, strSaveDirectory + filename);
                    map.put(KEY_FilePath, strSaveDirectory + filename);
                    map.put(KEY_FileType, 0);
                    map.put(KEY_FileName, filename);
                    map.put(KEY_FileIndex, String.valueOf(i));
                    listImageItem.add(map);
                }
            }
        }
        if (m_Adapter != null) {
            m_Adapter.notifyDataSetChanged();
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_filelist, container, false);
        mContext = requireActivity();
        m_GridView = view.findViewById(R.id.gridView);
        m_GridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (m_bEdit) {
                    CheckBox checkBox = view.findViewById(R.id.checkBox);
                    checkBox.toggle();
                    if (checkBox.isChecked()) {
                        m_HashMap.put(position, position);
                    } else {
                        m_HashMap.remove(position);
                    }
                } else {
                    Intent intent = new Intent(requireActivity(), FullImageActivity.class);
                    intent.putExtra("position", position);
                    intent.putStringArrayListExtra("FilePath", m_ayFilePath);
                    startActivity(intent);
                }
            }
        });
        getFile();
        m_Adapter = new ListViewItemAdapter(mContext,
                listImageItem, R.layout.downloaded_filelist, new String[]{
                KEY_ThumbnailFilePath, KEY_FileName, KEY_FileIndex},
                new int[]{R.id.imageView1, R.id.textView1});
        m_GridView.setAdapter(m_Adapter);
        m_Adapter.notifyDataSetChanged();
        return view;
    }

    private String title;

    public void setTitle(String title) {
        this.title = title;
    }

}
