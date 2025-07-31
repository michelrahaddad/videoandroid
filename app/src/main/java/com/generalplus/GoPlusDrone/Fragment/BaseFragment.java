package com.generalplus.GoPlusDrone.Fragment;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
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
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.generalplus.GoPlusDrone.Activity.GalleryActivity;
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
 * Base class for fragments that display lists of downloaded photos or videos.
 *
 * <p>
 * This class handles common functionality such as editing mode, multi‑selection
 * support and copy/delete actions. Subclasses can focus on populating the
 * {@link #listImageItem} list with file information and configuring the view.
 * </p>
 */
public class BaseFragment extends Fragment {
    private static final String TAG = "BaseFragment";

    /**
     * Key used to store a page title in {@link Bundle} arguments.
     */
    public static final String DATA_NAME = "name";

    // Keys for the maps stored in {@link #listImageItem}
    protected static final String KEY_ThumbnailFilePath = "KEY_ThumbnailFilePath";
    protected static final String KEY_FilePath = "KEY_FilePath";
    protected static final String KEY_FileType = "KEY_FileType";
    protected static final String KEY_FileName = "KEY_FileName";
    protected static final String KEY_FileIndex = "KEY_FileIndex";

    /**
     * The current context.
     */
    protected Context mContext = null;
    private String title = "";
    private int indicatorColor = Color.BLUE;
    private int dividerColor = Color.GRAY;
    private int iconResId = 0;

    /**
     * Map used to keep track of selected item positions when the user is
     * selecting multiple files for deletion or copying.
     */
    protected HashMap<Integer, Integer> m_HashMap = new HashMap<>();
    /**
     * List of file metadata displayed by the adapter. Subclasses should
     * populate this list in {@code onCreateView}.
     */
    protected ArrayList<HashMap<String, Object>> listImageItem = new ArrayList<>();
    /**
     * Flag indicating whether the user has selected all items in the list.
     */
    protected boolean m_bSelect = false;
    /**
     * Adapter backing the GridView or ListView displaying the files.
     */
    protected ListViewItemAdapter m_Adapter = null;
    /**
     * Path on the device where pictures should be saved when copying files
     * into the system media store.
     */
    private String strDevicePICLocation = "";
    /**
     * Flag indicating whether the list is currently in edit mode.
     */
    public boolean m_bEdit = false;
    /**
     * List of absolute file paths used when launching other activities (e.g. to
     * view individual images or videos).
     */
    protected ArrayList<String> m_ayFilePath = new ArrayList<>();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        // Derive the location used when copying files into the user's album.
        strDevicePICLocation = Environment.getExternalStorageDirectory().getPath()
                + CamWrapper.SaveFileToDevicePath;
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        // Inflate a different menu depending on whether we're in edit mode.
        if (m_bEdit) {
            inflater.inflate(R.menu.menu_downloaded, menu);
        } else {
            inflater.inflate(R.menu.menu_edit, menu);
        }
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        
        int id = item.getItemId();

        if (id == R.id.action_edit) {
            m_bEdit = true;
            GalleryActivity.m_bEdit = m_bEdit;
            requireActivity().invalidateOptionsMenu();
            if (m_Adapter != null) {
                m_Adapter.notifyDataSetChanged();
            }
        } else if (id == R.id.action_done) {
            m_bSelect = false;
            m_HashMap.clear();
            m_bEdit = false;
            GalleryActivity.m_bEdit = m_bEdit;
            requireActivity().invalidateOptionsMenu();
            if (m_Adapter != null) {
                m_Adapter.notifyDataSetChanged();
            }
        } else if (id == R.id.action_Select) {
            m_HashMap.clear();
            if (!m_bSelect) {
                for (int i = 0; i < listImageItem.size(); i++) {
                    m_HashMap.put(i, i);
                }
            }
            if (m_Adapter != null) {
                m_Adapter.notifyDataSetChanged();
            }
            m_bSelect = !m_bSelect;
        } else if (id == R.id.action_delete) {
            ShowDeleteDialog();
        } else if (id == R.id.action_copy) {
            ShowCopyDialog();
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Copy a file from {@code inputPath} to {@code outputPath}. This method
     * performs basic file I/O and notifies the media scanner of the new file.
     */
    private boolean copyFile(String inputPath, String OutputPath) {
        InputStream in = null;
        OutputStream out = null;
        try {
            File dir = new File(OutputPath);
            if (dir.exists() && dir.isFile()) dir.delete();

            in = new FileInputStream(inputPath);
            out = new FileOutputStream(OutputPath);

            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            in.close();
            in = null;

            out.flush();
            out.close();
            out = null;

            mContext.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                    Uri.parse("file://" + dir.getAbsolutePath())));

        } catch (FileNotFoundException fnfe1) {
            Log.e(TAG, fnfe1.getMessage());
            return false;
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
            return false;
        }
        return true;
    }

    /**
     * Show a confirmation dialog before copying selected files to the user's
     * album. On confirmation, each selected file is copied and a toast is
     * displayed to indicate overall success.
     */
    private void ShowCopyDialog() {
        if (m_HashMap.size() == 0) {
            Toast.makeText(mContext, "Please select file.", Toast.LENGTH_SHORT).show();
            return;
        }
        AlertDialog.Builder MyAlertDialog = new AlertDialog.Builder(mContext);
        MyAlertDialog.setTitle("Are you sure to copy the selected file to Album?");
        MyAlertDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                boolean bSuccess = true;
                for (int key : m_HashMap.keySet()) {
                    boolean bCopySuccess = copyFile(
                            (String) listImageItem.get(key).get(KEY_FilePath),
                            strDevicePICLocation + "/" + listImageItem.get(key).get(KEY_FileName));
                    if (!bCopySuccess) {
                        bSuccess = false;
                    }
                }

                if (bSuccess) {
                    Toast.makeText(mContext, "Copy to Album Successfully.", Toast.LENGTH_SHORT)
                            .show();
                } else {
                    Toast.makeText(mContext, "Copy part files Successfully.", Toast.LENGTH_SHORT)
                            .show();
                }
                m_bSelect = false;
                m_HashMap.clear();
                if (m_Adapter != null) {
                    m_Adapter.notifyDataSetChanged();
                }
            }
        });
        MyAlertDialog.setNegativeButton("NO", null);
        MyAlertDialog.show();
    }

    /**
     * Show a confirmation dialog before deleting selected files. On
     * confirmation, each selected file and its associated thumbnail are
     * removed from disk and the list is updated.
     */
    private void ShowDeleteDialog() {
        if (m_HashMap.size() == 0) {
            Toast.makeText(mContext, "Please select file.", Toast.LENGTH_SHORT).show();
            return;
        }
        AlertDialog.Builder MyAlertDialog = new AlertDialog.Builder(mContext);
        MyAlertDialog.setTitle("Are you sure to delete the selected file?");
        MyAlertDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                int iSize = m_HashMap.size();
                int iSuccess = 0;

                // Sort keys in descending order to avoid index shift when removing
                List<Map.Entry<Integer, Integer>> infoIds = new ArrayList<>(m_HashMap.entrySet());
                Collections.sort(infoIds, new Comparator<Map.Entry<Integer, Integer>>() {
                    @Override
                    public int compare(Map.Entry<Integer, Integer> o1, Map.Entry<Integer, Integer> o2) {
                        return o2.getValue() - o1.getValue();
                    }
                });

                for (int i = 0; i < infoIds.size(); i++) {
                    int key = infoIds.get(i).getKey();
                    String strPath = (String) listImageItem.get(key).get(KEY_FilePath);
                    Log.e(TAG, "Path = " + strPath);

                    final File file = new File(strPath);
                    if (file.exists()) {
                        file.delete();
                    }

                    if (!file.exists()) {
                        iSuccess++;
                        String strThumbnailPath =
                                (String) listImageItem.get(key).get(KEY_ThumbnailFilePath);
                        final File fileThumbnail = new File(strThumbnailPath);
                        if (fileThumbnail.exists()) {
                            fileThumbnail.delete();
                        }
                    }

                    listImageItem.remove(key);
                    m_ayFilePath.remove(key);
                }

                if (iSuccess == iSize) {
                    Toast.makeText(mContext, "File deleted successfully.", Toast.LENGTH_SHORT)
                            .show();
                } else {
                    Toast.makeText(mContext, "Delete part files successfully.", Toast.LENGTH_SHORT)
                            .show();
                }
                m_bSelect = false;
                m_HashMap.clear();
                if (m_Adapter != null) {
                    m_Adapter.notifyDataSetChanged();
                }
            }
        });
        MyAlertDialog.setNegativeButton("No", null);
        MyAlertDialog.show();
    }

    /**
     * Custom adapter used to display thumbnails and file names in a grid or list. The
     * layout used should contain an ImageView (id {@code imageView1}), a
     * CheckBox (id {@code checkBox}) and another ImageView for the video
     * indicator (id {@code ivVideo}).
     */
    public class ListViewItemAdapter extends SimpleAdapter {
        private Context context;
        private ArrayList<HashMap> data;
        private int itemLayout;
        private String[] componentTagAry;
        private int[] componentXMLIDAry;
        private boolean[] click;

        public ListViewItemAdapter(Context context,
                                   ArrayList<HashMap<String, Object>> data,
                                   int resource,
                                   String[] from,
                                   int[] to) {
            super(context, data, resource, from, to);
            this.context = context;
            this.itemLayout = resource;
            this.componentTagAry = from;
            this.componentXMLIDAry = to;
            this.click = click;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                holder = new ViewHolder();
                LayoutInflater factory = LayoutInflater.from(context);
                convertView = factory.inflate(R.layout.downloaded_filelist, parent, false);
                holder.myImage = convertView.findViewById(R.id.imageView1);
                holder.checkBox = convertView.findViewById(R.id.checkBox);
                holder.m_iVideo = convertView.findViewById(R.id.ivVideo);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            File imgFile = new File((String) listImageItem.get(position).get(KEY_ThumbnailFilePath));
            if (imgFile.exists()) {
                Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                holder.myImage.setImageBitmap(myBitmap);
            } else {
                holder.myImage.setImageResource(R.drawable.broken);
            }

            // Show or hide the checkbox depending on edit mode and selection
            if (m_bEdit) {
                holder.checkBox.setVisibility(View.VISIBLE);
            } else {
                holder.checkBox.setVisibility(View.INVISIBLE);
            }
            holder.checkBox.setChecked(m_HashMap.get(position) != null);

            // Show video indicator only for video files (KEY_FileType == 1)
            Object typeObj = listImageItem.get(position).get(KEY_FileType);
            int fileType = (typeObj instanceof Integer) ? (int) typeObj : 0;
            if (fileType == 0) {
                holder.m_iVideo.setVisibility(View.INVISIBLE);
            } else {
                holder.m_iVideo.setVisibility(View.VISIBLE);
            }
            return convertView;
        }

        class ViewHolder {
            public CheckBox checkBox;
            public ImageView myImage;
            public ImageView m_iVideo;
        }
    }

    public String getTitle() {
        return "Título Padrão";
    }

    public int getIconResId() {
        return android.R.drawable.ic_menu_info_details;
    }

}
