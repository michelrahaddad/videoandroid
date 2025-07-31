package com.generalplus.GoPlusDrone.Fragment;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.generalplus.GoPlusDrone.R;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import generalplus.com.GPCamLib.CamWrapper;

/**
 * Fragment that displays a list of downloaded photo and video files stored on
 * external storage. Users can select multiple files to copy to the system
 * gallery or delete them.
 */
public class DownloadedFileFragment extends BaseFragment {
    private static final String TAG = "DownloadedFileFragment";
    private GridView m_GridView = null;
    private ListViewItemAdapter m_Adapter = null;
    private String strSaveDirectory = "";
    private String strDevicePICLocation = "";
    private ArrayList<HashMap<String, Object>> listImageItem = new ArrayList<>();
    private Context mContext = null;
    private boolean m_bSelect = false;
    private HashMap<Integer, Integer> m_HashMap = new HashMap<>();

    /**
     * Create a new instance of this fragment with a title.
     */
    public static DownloadedFileFragment newInstance(String title) {
        DownloadedFileFragment f = new DownloadedFileFragment();
        f.setTitle(title);
        Bundle args = new Bundle();
        args.putString(DATA_NAME, title);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_downloadedfile, container, false);
        mContext = requireActivity();
        m_GridView = view.findViewById(R.id.gridView);
        listImageItem.clear();
        strSaveDirectory = Environment.getExternalStorageDirectory().getPath() + "/GoPlus_Drone/Photo/";
        strDevicePICLocation = Environment.getExternalStorageDirectory().getPath() + CamWrapper.SaveFileToDevicePath;
        File dir = new File(strSaveDirectory);
        String[] children = dir.list();
        if (children != null) {
            for (int i = 0; i < children.length; i++) {
                String filename = children[i];
                if (filename.contains(".jpg")) {
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
        strSaveDirectory = Environment.getExternalStorageDirectory().getPath() + "/GoPlus_Drone/Video/";
        dir = new File(strSaveDirectory);
        children = dir.list();
        if (children != null) {
            for (int i = 0; i < children.length; i++) {
                String filename = children[i];
                if (filename.contains(".mp4")) {
                    HashMap<String, Object> map = new HashMap<>();
                    map.put(KEY_ThumbnailFilePath, strSaveDirectory + "thumbnails/" + filename.replace("mp4", "jpg"));
                    map.put(KEY_FilePath, strSaveDirectory + filename);
                    map.put(KEY_FileType, 1);
                    map.put(KEY_FileName, filename);
                    map.put(KEY_FileIndex, String.valueOf(i));
                    listImageItem.add(map);
                }
            }
        }
        m_Adapter = new ListViewItemAdapter(mContext,
                listImageItem, R.layout.downloaded_filelist, new String[]{
                KEY_ThumbnailFilePath, KEY_FileName, KEY_FileIndex},
                new int[]{R.id.imageView1, R.id.textView1});
        m_GridView.setAdapter(m_Adapter);
        m_GridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                CheckBox checkBox = view.findViewById(R.id.checkBox);
                checkBox.toggle();
                if (checkBox.isChecked()) {
                    m_HashMap.put(position, position);
                } else {
                    m_HashMap.remove(position);
                }
            }
        });
        m_Adapter.notifyDataSetChanged();
        return view;
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.menu_downloaded, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        
        if (id == R.id.action_Select) {
            m_HashMap.clear();
            if (!m_bSelect) {
                for (int i = 0; i < listImageItem.size(); i++) {
                    m_HashMap.put(i, i);
                }
            }
            m_Adapter.notifyDataSetChanged();
            m_bSelect = !m_bSelect;
        } else if (id == R.id.action_delete) {
            ShowDeleteDialog();
        } else if (id == R.id.action_copy) {
            ShowCopyDialog();
        }
        
        return super.onOptionsItemSelected(item);
    }

    private void ShowDeleteDialog() {
        // Simulação de diálogo de exclusão
    }

    private void ShowCopyDialog() {
        // Simulação de diálogo de cópia
    }

    private String title;

    public void setTitle(String title) {
        this.title = title;
    }
}