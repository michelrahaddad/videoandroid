package com.generalplus.GoPlusDrone.Fragment;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.GridView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.generalplus.GoPlusDrone.Activity.FileViewController;
import com.generalplus.GoPlusDrone.R;
import com.generalplus.ffmpegLib.ffmpegWrapper;

import java.io.File;
import java.util.HashMap;

import generalplus.com.GPCamLib.CamWrapper;

/**
 * Fragment that displays a list of video files stored on external storage. When
 * not in edit mode, tapping a video launches a playback activity. In edit
 * mode, multiple videos can be selected for deletion or copying via the base
 * fragment controls.
 */
public class VideoListFragment extends BaseFragment {
    private static final String TAG = "VideoListFragment";
    private GridView m_GridView = null;

    /**
     * Create a new instance of this fragment with a title.
     */
    public static VideoListFragment newInstance(String title) {
        VideoListFragment f = new VideoListFragment();
        f.setTitle(title);
        Bundle args = new Bundle();
        args.putString(DATA_NAME, title);
        f.setArguments(args);
        return f;
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
        String strSaveDirectory = Environment.getExternalStorageDirectory().getPath() + "/GoPlus_Drone/Video/";
        File dir = new File(strSaveDirectory);
        String[] children = dir.list();
        if (children != null) {
            for (int i = 0; i < children.length; i++) {
                String filename = children[i];
                if (filename.contains(".mp4")) {
                    m_ayFilePath.add(strSaveDirectory + filename);
                    HashMap<String, Object> map = new HashMap<>();
                    map.put(KEY_ThumbnailFilePath, strSaveDirectory + "thumbnails/" + filename.replace("mp4", "jpg"));
                    map.put(KEY_FilePath, strSaveDirectory + filename);
                    map.put(KEY_FileType, 1);
                    map.put(KEY_FileName, filename);
                    map.put(KEY_FileIndex, String.valueOf(i));
                    listImageItem.add(map);
                } else if (filename.contains(".avi")) {
                    m_ayFilePath.add(strSaveDirectory + filename);
                    HashMap<String, Object> map = new HashMap<>();
                    map.put(KEY_ThumbnailFilePath, strSaveDirectory + "thumbnails/" + filename.replace("avi", "jpg"));
                    map.put(KEY_FilePath, strSaveDirectory + filename);
                    map.put(KEY_FileType, 1);
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
                    Intent toVlcPlayer = new Intent(requireActivity(), FileViewController.class);
                    Bundle b = new Bundle();
                    b.putString(CamWrapper.GPFILECALLBACKTYPE_FILEURL, m_ayFilePath.get(position));
                    b.putInt(CamWrapper.GPFILECALLBACKTYPE_FILEFLAG, CamWrapper.GPFILEFLAG_AVISTREAMING);
                    toVlcPlayer.putExtras(b);
                    startActivity(toVlcPlayer);
                }
            }
        });
        getFile();
        getThumbnail();
        m_Adapter = new ListViewItemAdapter(mContext,
                listImageItem, R.layout.downloaded_filelist, new String[]{
                KEY_ThumbnailFilePath, KEY_FileName, KEY_FileIndex},
                new int[]{R.id.imageView1, R.id.textView1});
        m_GridView.setAdapter(m_Adapter);
        m_Adapter.notifyDataSetChanged();
        return view;
    }

    private void getThumbnail() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String strThumbnailFilePath;
                File fileThumbnail;
                for (int position = 0; position < listImageItem.size(); position++) {
                    strThumbnailFilePath = (String) listImageItem.get(position).get(KEY_ThumbnailFilePath);
                    fileThumbnail = new File(strThumbnailFilePath);
                    if (!fileThumbnail.exists()) {
                        int iResult = ffmpegWrapper.getInstance().naExtractFrame((String) listImageItem.get(position).get(KEY_FilePath), strThumbnailFilePath, 0);
                        Log.e(TAG, "not exists");
                        if (iResult == 0) {
                            requireActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (m_Adapter != null) {
                                        m_Adapter.notifyDataSetChanged();
                                    }
                                }
                            });
                        }
                    }
                }
            }
        }).start();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    




    this.title = title;
}

    private String title;

    public void setTitle(String title) {
        this.title = title;
    }

}
