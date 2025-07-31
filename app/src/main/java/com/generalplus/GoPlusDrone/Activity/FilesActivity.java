package com.generalplus.GoPlusDrone.Activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import com.generalplus.GoPlusDrone.R;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import generalplus.com.GPCamLib.CamWrapper;

@SuppressWarnings("unchecked")
public class FilesActivity extends Activity {
	private static final String TAG = "FilesActivity";
	private static FilesActivity m_FilesActivityInstance;
	private Context m_Context;
	private Handler m_handler = null;
	private boolean _bUserLeaveHint = true;

	public static final int FileTag_FileCount = 0x00;
	public static final int FileTag_FileName = 0x01;

	public static final int FileFlag_Unknown = 0x00;
	public static final int FileFlag_AVIStreaming = 0x01;
	public static final int FileFlag_JPGStreaming = 0x02;
	public static final int FileFlag_LocalFile = 0x03;

	private static final int FileTag_FileDeviceInit = 0xA0;
	private static final int FileTag_FileDeviceReady = 0xA1;
	private static final int FileTag_FileGettingThumbnail = 0xA2;
	private static final int FileTag_FileGotThumbnail = 0xA3;
	private static final int FileTag_FileDownload = 0xA4;
	private static final int FileTag_FilePalying = 0xA5;
	private static final int FileTag_FileBroken = 0xA6;
	private static final int FileTag_FileGotThumbnailEnd = 0xA7;
	private static ProgressDialog m_DownloadDialog = null;

	private static boolean bSaveImageItem = false;
	private GridView m_Gridview;
	private static ArrayList<HashMap<String, Object>> listImageItem = null;

	private static int[] m_i32AryFileStatus = null;
	private static String[] m_strAryFileName = null;
	private static String[] m_strAryFilePath = null;
	private static int[] m_i32AryFileSize = null;
	private static String[] m_strAryFileTime = null;
	private static Thread m_UpdateThumbnailThread = null;
	private static Thread m_UpdateGridVierThread = null;
	private static boolean m_bRunCreateGridViewDone = false;
	private static boolean m_bPendingGetThumbnail = false;
	private static boolean bIsStopDownload = false;
	private static boolean bIsStopUpdateThumbnail = false;
	private boolean bIsCopingFile = false;
	private int i32GetThumbnailCount = 0;
	private String strDevicePICLocation = "";
	private int _i32CommandIndex = 0;
	private int _i32ErrorCount = 0;
	private int _i32WaitGettingThumbnailCount = 0;
	private static int	_i32GettingThumbnailFileIndex = -1;
	private int _i32GotThumbnailFileIndex = -1;
	private int _i32SelectedFirstItem = -1;
	private int _firstVisibleItem = 0;
	private int _scrollState = 0;
	private CharSequence[] CharSequenceItemsDefault = { "Play", "Download",
			"Info" };
	private CharSequence[] CharSequenceItemsDelete = { "Play", "Delete", "Info" };
	private SimpleAdapter m_saImageItems;
	private static boolean		_bSetModeDone = false;
	private long 				mLastClickTime;
	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_filelist);
//		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);

		m_Context = this;
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		
		strDevicePICLocation = Environment.getExternalStorageDirectory()
				.getPath() + CamWrapper.SaveFileToDevicePath;

		if (m_handler == null)
			m_handler = new Handler();

		m_Gridview = (GridView) findViewById(R.id.gridView);
		m_Gridview.setOnScrollListener(new AbsListView.OnScrollListener() {
			
			@Override
			public void onScrollStateChanged(AbsListView view, int scrollState) {

				_scrollState = scrollState;
				if (scrollState == SCROLL_STATE_IDLE) {
					if (_i32SelectedFirstItem == _firstVisibleItem) {
						return;
					}
					_i32SelectedFirstItem = _firstVisibleItem;
					m_Gridview.setSelection(_i32SelectedFirstItem);
					m_bRunCreateGridViewDone = false;
					if (m_UpdateThumbnailThread != null) {
						m_UpdateThumbnailThread.interrupt();
						m_UpdateThumbnailThread = null;
					}
					m_UpdateThumbnailThread = new Thread(
							new UpdateThumbnailRunnable());
					m_UpdateThumbnailThread.start();
				}
			}
			
			@Override
			public void onScroll(AbsListView view, int firstVisibleItem,
					int visibleItemCount, int totalItemCount) {
				// TODO Auto-generated method stub		
				_firstVisibleItem = firstVisibleItem;
				
				Log.d("tag", "onScroll = " + firstVisibleItem);
			}
		});
		m_Gridview.setOnItemClickListener(new OnItemClickListener() {

			private AdapterView m_Paramet;
			private String strStreamFilePath = "";
			private int m_i32Position;
			private long m_i64ID;
			private CharSequence[] SetCharSequenceItems = null;

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {

				m_Paramet = parent;
				m_i32Position = position;
				m_i64ID = id;

				if (m_i32AryFileStatus[m_i32Position] != FileTag_FileGotThumbnail && m_i32AryFileStatus[m_i32Position] != FileTag_FileGotThumbnailEnd)
					return;

				SetCharSequenceItems = CharSequenceItemsDefault;
				strStreamFilePath = "";

				File dir = new File(strDevicePICLocation
						+ m_strAryFileName[m_i32Position]);

				if (dir.exists()
						&& ((dir.length() / 1024) == m_i32AryFileSize[m_i32Position])) {
					SetCharSequenceItems = CharSequenceItemsDelete;
					strStreamFilePath = strDevicePICLocation
							+ m_strAryFileName[m_i32Position];
				}

				Builder ChoseAlertDialog = new AlertDialog.Builder(m_Context);

				ChoseAlertDialog.setNegativeButton("Cancel",
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								dialog.cancel();
							}
						});
				ChoseAlertDialog.setItems(SetCharSequenceItems,
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								HashMap<String, Object> item = (HashMap<String, Object>) m_Paramet
										.getItemAtPosition(m_i32Position);

								if (SetCharSequenceItems[which].toString()
										.contentEquals("Play")) {
									_bUserLeaveHint = false;
									bIsStopUpdateThumbnail = true;
									bSaveImageItem = true;
									if (m_strAryFileName[m_i32Position].toString().contains(".jpg"))
									{
										if(!strStreamFilePath.isEmpty())
										{
											ImageView iv = new ImageView(
													m_Context);
											iv.setImageURI(Uri
													.parse(strStreamFilePath));

											Builder ShowImgAlertDialog = new AlertDialog.Builder(
													m_Context);
											ShowImgAlertDialog.setView(iv);
											ShowImgAlertDialog
													.setNegativeButton(
															"Cancel",
															new DialogInterface.OnClickListener() {

																@Override
																public void onClick(
																		DialogInterface dialog,
																		int which) {
																	bIsStopUpdateThumbnail = false;

																}
															});
											ShowImgAlertDialog.show();
										}
										else
										{
//											Intent toVlcPlayer = new Intent(FilesActivity.this, FileViewController.class);
//									        Bundle b = new Bundle();
//									        b.putString(CamWrapper.GPFILECALLBACKTYPE_FILEURL, strStreamFilePath);
//									        b.putInt(CamWrapper.GPFILECALLBACKTYPE_FILEFLAG, CamWrapper.GPFILEFLAG_JPGSTREAMING);
//									        b.putInt(CamWrapper.GPFILECALLBACKTYPE_FILEINDEX, m_i32Position);
//									        toVlcPlayer.putExtras(b);
//									        startActivity(toVlcPlayer);
										}
									}
									else
									{
//										Intent toVlcPlayer = new Intent(FilesActivity.this, FileViewController.class);
//								        Bundle b = new Bundle();
//								        b.putString(CamWrapper.GPFILECALLBACKTYPE_FILEURL, strStreamFilePath);
//								        b.putInt(CamWrapper.GPFILECALLBACKTYPE_FILEFLAG, CamWrapper.GPFILEFLAG_AVISTREAMING);
//								        b.putInt(CamWrapper.GPFILECALLBACKTYPE_FILEINDEX, m_i32Position);
//								        toVlcPlayer.putExtras(b);
//								        startActivity(toVlcPlayer);
									}

								} else if (SetCharSequenceItems[which]
										.toString().contentEquals("Download")) {

									//bIsStopUpdateThumbnail = true;
									bIsStopDownload = false;
									m_bPendingGetThumbnail = true;
									if (m_DownloadDialog == null) {
										m_DownloadDialog = new ProgressDialog(
												m_Context);
										m_DownloadDialog
												.setMessage("Prepare to download ...");
										m_DownloadDialog
												.setCanceledOnTouchOutside(false);
										m_DownloadDialog.setMax(100);
										m_DownloadDialog
												.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
										m_DownloadDialog
												.setButton(
														DialogInterface.BUTTON_NEGATIVE,
														"Abort",
														new DialogInterface.OnClickListener() {
															@Override
															public void onClick(
																	DialogInterface dialog,
																	int which) {
																if (!bIsCopingFile) {
																	final File deviceDirDelete = new File(
																			strDevicePICLocation
																					+ m_strAryFileName[m_i32Position]);
																	deviceDirDelete
																			.delete();
																}

																m_DownloadDialog = null;
																bIsStopDownload = true;
															}
														});
										m_DownloadDialog.show();
									}

									CamWrapper.getComWrapperInstance()
											.GPCamSendGetFileRawdata(
													m_i32Position);
								} else if (SetCharSequenceItems[which]
										.toString().contentEquals("Info")) {
									Builder InfoAlertDialog = new AlertDialog.Builder(
											m_Context);
									String strInfoMsg = "", strTime = "";
									strTime = "20"
											+ m_strAryFileTime[m_i32Position]
													.substring(0, 2)
											+ "/"
											+ m_strAryFileTime[m_i32Position]
													.substring(2, 4)
											+ "/"
											+ m_strAryFileTime[m_i32Position]
													.substring(4, 6)
											+ " "
											+ m_strAryFileTime[m_i32Position]
													.substring(6, 8)
											+ ":"
											+ m_strAryFileTime[m_i32Position]
													.substring(8, 10)
											+ ":"
											+ m_strAryFileTime[m_i32Position]
													.substring(10, 12);
									strInfoMsg = "Name: "
											+ m_strAryFileName[m_i32Position]
											+ "\nTime: "
											+ strTime
											+ "\nSize: "
											+ String.valueOf(m_i32AryFileSize[m_i32Position]);
									InfoAlertDialog.setTitle("Info");
									InfoAlertDialog.setMessage(strInfoMsg);
									InfoAlertDialog.setCancelable(true);
									InfoAlertDialog.show();
								} else if (SetCharSequenceItems[which]
										.toString().contentEquals("Delete")) {
									File deviceDirDelete = new File(
											strDevicePICLocation
													+ m_strAryFileName[m_i32Position]);
									deviceDirDelete.delete();
									sendBroadcast(new Intent(
											Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
											Uri.parse("file://"
													+ deviceDirDelete
													.getAbsolutePath())));

								}
							}

						});

				ChoseAlertDialog.setCancelable(false);
				ChoseAlertDialog.show();

			}

		});

		if (!bSaveImageItem) {

			if (m_i32AryFileStatus != null)
				m_i32AryFileStatus = null;

			if (m_strAryFileName != null)
				m_strAryFileName = null;

			if (m_strAryFilePath != null)
				m_strAryFilePath = null;

			if (m_i32AryFileSize != null)
				m_i32AryFileSize = null;

			if (m_strAryFileTime != null)
				m_strAryFileTime = null;

			if (listImageItem == null)
				listImageItem = new ArrayList<HashMap<String, Object>>();
			listImageItem.clear();

			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			_bSetModeDone = false;
			CamWrapper.getComWrapperInstance().GPCamSendSetMode(
					CamWrapper.GPDEVICEMODE_Playback);

			CamWrapper.getComWrapperInstance().GPCamSendGetFullFileList();
			
			
		} else {
			_bSetModeDone = false;
			CamWrapper.getComWrapperInstance().GPCamSendSetMode(
					CamWrapper.GPDEVICEMODE_Playback);

			CamWrapper.getComWrapperInstance().GPCamSendGetFullFileList();
			
			m_bRunCreateGridViewDone = false;
			if (m_UpdateThumbnailThread == null) {
				m_UpdateThumbnailThread = new Thread(
						new UpdateThumbnailRunnable());
				m_UpdateThumbnailThread.start();
			}			
		}
		
	}

	static public FilesActivity getInstance() {
		return m_FilesActivityInstance;
	}

	private void UpdateGridView() {
		runOnUiThread(new Runnable(){

			@Override
			public void run() {	
				if (null == m_saImageItems) {
					if (null == listImageItem) {
						return;
					}
					m_saImageItems = new SimpleAdapter(m_Context,
							listImageItem, R.layout.files_program_list, new String[] {
									"ThumbnailFilePath", "FileName", "FileIndex" },
							new int[] { R.id.imageView1, R.id.textView1 });

					m_Gridview.setAdapter(m_saImageItems);
				}
				m_saImageItems.notifyDataSetChanged();					
			}
			
		});
	}

	class UpdateGridViewRunnable implements Runnable {

		@Override
		public void run() {
			// TODO Auto-generated method stub
			Log.e(TAG, "UpdateGridViewRunnable ...");
			while (m_UpdateThumbnailThread != null) {

				m_handler.post(new Runnable() {
					public void run() {
						if (listImageItem.size() > 0)
							UpdateGridView();
					}
				});

				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			m_handler.post(new Runnable() {
				public void run() {
					if (listImageItem.size() > 0)
						UpdateGridView();
				}
			});
			Log.e(TAG, "UpdateGridViewRunnable ... Done");
			m_UpdateGridVierThread = null;
		}

	}

	class UpdateThumbnailRunnable implements Runnable {

		UpdateThumbnailRunnable() {

			Log.e(TAG, "Create UpdateThumbnailRunnable ... ");
			if (m_UpdateGridVierThread == null) {
				m_UpdateGridVierThread = new Thread(
						new UpdateGridViewRunnable());
				m_UpdateGridVierThread.start();
			}
			bIsStopUpdateThumbnail = false;
			m_bPendingGetThumbnail = false;
		}

		@Override
		public void run() {
			// TODO Auto-generated method stub
			
			while(!m_bRunCreateGridViewDone	&& !bIsStopUpdateThumbnail)
			{
				if(m_bPendingGetThumbnail)
					continue;
				else
				{
					try {
						Thread.sleep(50);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}							
				
				synchronized (m_i32AryFileStatus) {
					m_bRunCreateGridViewDone = false;
					int index = 0;
					if (CamWrapper.getComWrapperInstance().getIsNewFile()) {
						if (-1 != _i32SelectedFirstItem) {
							index = _i32SelectedFirstItem;
						}
					}
					for(int i=index;i<m_i32AryFileStatus.length;i++)
					{
						if(m_i32AryFileStatus[i] == FileTag_FileDeviceReady || m_i32AryFileStatus[i] == FileTag_FileDeviceInit)
						{
							if (CamWrapper.getComWrapperInstance().getIsNewFile()) {
								if (-1 == CamWrapper.getComWrapperInstance().GPCamGetFileIndex(i)) {
									CamWrapper.getComWrapperInstance().GPCamSetNextPlaybackFileListIndex(i);
									try {
										Thread.sleep(2000);
									} catch (InterruptedException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									}
								}
							}
							
							CamWrapper.getComWrapperInstance()
							.GPCamSendGetFileThumbnail(i);
							
							_i32GettingThumbnailFileIndex = i;
							m_i32AryFileStatus[i] = FileTag_FileGettingThumbnail;
							Log.e(TAG, "i = " + i + ", FileTag_FileGettingThumbnail...");
							break;
						}						
						else if(m_i32AryFileStatus[i] == FileTag_FileGettingThumbnail)
						{
							_i32WaitGettingThumbnailCount++;
							if(_i32WaitGettingThumbnailCount > 100)
							{
								_i32WaitGettingThumbnailCount = 0;
								if (CamWrapper.getComWrapperInstance().getIsNewFile()) {
									if (-1 == CamWrapper.getComWrapperInstance().GPCamGetFileIndex(i)) {
										CamWrapper.getComWrapperInstance().GPCamSetNextPlaybackFileListIndex(i);
										try {
											Thread.sleep(2000);
										} catch (InterruptedException e) {
											// TODO Auto-generated catch block
											e.printStackTrace();
										}
									}
								}
								
								CamWrapper.getComWrapperInstance()
								.GPCamSendGetFileThumbnail(i);
								
								
								m_i32AryFileStatus[i] = FileTag_FileGettingThumbnail;
								
							}
							_i32GettingThumbnailFileIndex = i;							
							break;
						}
						else if(m_i32AryFileStatus[i] == FileTag_FileGotThumbnail)
						{
							_i32WaitGettingThumbnailCount = 0;
							HashMap<String, Object> map = new HashMap<String, Object>();
							map.put("ThumbnailFilePath",
									m_strAryFilePath[i].toString());
							
							if (null != m_strAryFileName[i]) {
								map.put("FileName", m_strAryFileName[i].toString());
								map.put("FileTime", m_strAryFileTime[i].toString());
								map.put("FileSize",
										String.valueOf(m_i32AryFileSize[i]));
							}
							map.put("FileIndex", String.valueOf(i));
							
							
							listImageItem.set(i, map);
							_i32GotThumbnailFileIndex = i;
							m_bRunCreateGridViewDone = false;
							if(i == m_i32AryFileStatus.length - 1)
								m_bRunCreateGridViewDone = true;
							
							m_i32AryFileStatus[i] = FileTag_FileGotThumbnailEnd;
						}
						else if(m_i32AryFileStatus[i] == FileTag_FileBroken)
						{
							HashMap<String, Object> map = new HashMap<String, Object>();
							map.put("ThumbnailFilePath", R.drawable.broken);
							map.put("FileName", m_strAryFileName[i].toString());
							listImageItem.set(i, map);
							_i32GotThumbnailFileIndex = i;
							m_bRunCreateGridViewDone = false;
							if(i == m_i32AryFileStatus.length - 1)
								m_bRunCreateGridViewDone = true;
						}						
					}
				}				
			}			
			Log.e(TAG, "m_UpdateThumbnailThread = null");
			UpdateGridView();
			m_UpdateThumbnailThread = null;
		}
	};
	
	private boolean isFastClick() {
        long currentTime = System.currentTimeMillis();

        long time = currentTime - mLastClickTime;
        if ( 0 < time && time < 2000) {   
            return true;   
        }   

        mLastClickTime = currentTime;   
        return false;   
    }

	@Override
	public void onBackPressed() {
		Log.e(TAG, "onBackPressed ...");
		if(false == _bSetModeDone) {
			return;
		}
		if (isFastClick()) {   
            return;
        }
		if(m_UpdateThumbnailThread != null || m_UpdateGridVierThread != null)
		{
			bIsStopUpdateThumbnail = true;
			try {
				Thread.sleep(800);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		m_UpdateThumbnailThread = null;
		m_UpdateGridVierThread = null;

		bSaveImageItem = false;
		CamWrapper.getComWrapperInstance().GPCamClearCommandQueue();
		super.onBackPressed();
	}

	@Override
	protected void onResume() {	
		Log.e(TAG, "onResume ...");
		super.onResume();
		bIsStopUpdateThumbnail = false;
		CamWrapper.getComWrapperInstance().SetViewHandler(m_FromWrapperHandler, CamWrapper.GPVIEW_FILELIST);
		
		if(null != m_i32AryFileStatus) {
			if (m_UpdateThumbnailThread == null) {
				m_UpdateThumbnailThread = new Thread(
						new UpdateThumbnailRunnable());
				m_UpdateThumbnailThread.start();
			}
		}
	}

	@Override
	protected void onDestroy() {
		Log.e(TAG, "onDestroy ...");
		m_DownloadDialog = null;

		if (!bSaveImageItem) {
			File appGoPlusCamDir = new File(Environment
					.getExternalStorageDirectory().getPath()
					+ "/"
					+ CamWrapper.CamDefaulFolderName);
			deleteDir(appGoPlusCamDir);
			bIsStopDownload = true;
		}

		super.onDestroy();
	}

	@Override
	protected void onUserLeaveHint() {
		Log.e(TAG, "onUserLeaveHint ...");
		bIsStopDownload = true;
		if(_bUserLeaveHint)
		{
			CamWrapper.getComWrapperInstance().GPCamSendSetMode(
					CamWrapper.GPDEVICEMODE_Record);
			finish();
		}
		_bUserLeaveHint = true;
		super.onUserLeaveHint();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		// TODO Auto-generated method stub
		super.onConfigurationChanged(newConfig);
	}

	private void copyFile(String inputPath, String OutputPath) {

		InputStream in = null;
		OutputStream out = null;
		try {
			File dir = new File(OutputPath);
			if (dir.exists() && dir.isFile())
				dir.delete();

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

			sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
					Uri.parse("file://" + dir.getAbsolutePath())));

		} catch (FileNotFoundException fnfe1) {
			Log.e(TAG, fnfe1.getMessage());
		} catch (Exception e) {
			Log.e(TAG, e.getMessage());
		}
	}

	public void clearApplicationData() {

		SharedPreferences preferences = PreferenceManager
				.getDefaultSharedPreferences(this);
		SharedPreferences.Editor editor = preferences.edit();
		editor.clear();
		editor.commit();

		File cache = getCacheDir();
		File appDir = new File(cache.getParent());
		if (appDir.exists()) {
			String[] children = appDir.list();
			for (String s : children) {
				if (!s.equals("lib")) {
					deleteDir(new File(appDir, s));
				}
			}
		}
	}

	public boolean deleteDir(File dir) {
		if (dir != null && dir.isDirectory()) {
			String[] children = dir.list();
			for (int i = 0; i < children.length; i++) {
				if (!children[i].toString().contentEquals("Menu.xml")
						&& !children[i].toString().contains("Crash")
						&& !children[i].toString().contains("Logcat")
						&& !children[i].toString().contains(CamWrapper.SaveLogFileName)
						&& !children[i].toString().contentEquals(CamWrapper.ConfigFileName)) {
					boolean success = deleteDir(new File(dir, children[i]));
					if (!success) {
						return false;
					}
				}
			}
		}
		return dir.delete();
	}
	
	private Handler m_FromWrapperHandler = new Handler()
    {
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			switch(msg.what)
			{
			case CamWrapper.GPCALLBACKTYPE_CAMSTATUS:
				Bundle data = msg.getData();
				ParseGPCamStatus(data);
				msg = null;
				break;
			case CamWrapper.GPCALLBACKTYPE_CAMDATA:
				break;
			}			
		}    	
    };
    
    private void ParseGPCamStatus(Bundle StatusBundle)
    {
    	int i32CmdIndex = StatusBundle.getInt(CamWrapper.GPCALLBACKSTATUSTYPE_CMDINDEX);
    	int i32CmdType = StatusBundle.getInt(CamWrapper.GPCALLBACKSTATUSTYPE_CMDTYPE);
    	int i32Mode = StatusBundle.getInt(CamWrapper.GPCALLBACKSTATUSTYPE_CMDMODE);
    	int i32CmdID = StatusBundle.getInt(CamWrapper.GPCALLBACKSTATUSTYPE_CMDID);
    	int i32DataSize = StatusBundle.getInt(CamWrapper.GPCALLBACKSTATUSTYPE_DATASIZE);
    	byte[] pbyData = StatusBundle.getByteArray(CamWrapper.GPCALLBACKSTATUSTYPE_DATA);
    	//Log.e(TAG, "i32CMDIndex = " + i32CmdIndex + ", i32Type = " + i32CmdType + ", i32Mode = " + i32Mode + ", i32CMDID = " + i32CmdID + ", i32DataSize = " + i32DataSize);
    	
    	if (i32CmdType == CamWrapper.GP_SOCK_TYPE_ACK) {
			switch (i32Mode) {
			case CamWrapper.GPSOCK_MODE_General:
				switch(i32CmdID)
				{
				case CamWrapper.GPSOCK_General_CMD_SetMode:
					_bSetModeDone = true;
					Log.e(TAG, "_bSetModeDone = true");
					break;
				case CamWrapper.GPSOCK_General_CMD_GetDeviceStatus:
					break;
				case CamWrapper.GPSOCK_General_CMD_GetParameterFile:
					break;
				case CamWrapper.GPSOCK_General_CMD_RestartStreaming:
					break;
				}
				break;
			case CamWrapper.GPSOCK_MODE_Record:
				Log.e(TAG, "GPSOCK_MODE_Record ... ");
				break;
			case CamWrapper.GPSOCK_MODE_CapturePicture:
				Log.e(TAG, "GPSOCK_MODE_CapturePicture ... ");
				break;
			case CamWrapper.GPSOCK_MODE_Playback:
				Log.e(TAG, "GPSOCK_MODE_Playback ... ");
				switch(i32CmdID)
				{
				case CamWrapper.GPSOCK_Playback_CMD_GetFileCount:
				{
					if(bSaveImageItem)
						return;

					int i32FileCount = (pbyData[0] & 0xFF) | ((pbyData[1] & 0xFF) << 8);
					if (i32FileCount <= 0)
						break;

					_i32GettingThumbnailFileIndex = -1;
					_i32GotThumbnailFileIndex = -1;
					i32GetThumbnailCount = 0;
					if (m_i32AryFileStatus != null)
						m_i32AryFileStatus = null;
					m_i32AryFileStatus = new int[i32FileCount];
					Arrays.fill(m_i32AryFileStatus, FileTag_FileDeviceInit);

					if (m_strAryFileName != null)
						m_strAryFileName = null;
					m_strAryFileName = new String[i32FileCount];

					if (m_strAryFilePath != null)
						m_strAryFilePath = null;
					m_strAryFilePath = new String[i32FileCount];

					if (m_i32AryFileSize != null)
						m_i32AryFileSize = null;
					m_i32AryFileSize = new int[i32FileCount];

					if (m_strAryFileTime != null) {
						m_strAryFileTime = null;
					}
					m_strAryFileTime = new String[i32FileCount];
					m_bRunCreateGridViewDone = false;
					bIsStopDownload = false;

					for (int i = 0; i < i32FileCount; i++) {
						HashMap<String, Object> map = new HashMap<String, Object>();
						map.put("ThumbnailFilePath", R.mipmap.loading);
						map.put("FileName", "Unknown");
						map.put("FileIndex", String.valueOf(i));
						map.put("FileSize", "0");
						map.put("FileTime", "0");
						listImageItem.add(map);
					}

					UpdateGridView();
					break;
				}
				case CamWrapper.GPSOCK_Playback_CMD_GetNameList:
				{
					if (m_i32AryFileStatus == null || m_strAryFileName == null
							|| m_strAryFilePath == null || m_i32AryFileSize == null
							|| m_strAryFileTime == null) {
						m_bRunCreateGridViewDone = true;
						break;
					}
					
					_i32CommandIndex = i32CmdIndex;

					int i32FileIndex = (pbyData[0] & 0xFF) + ((pbyData[1] & 0xFF) << 8);
					int i32FileCount = pbyData[2] & 0xFF;					

					if (bIsStopUpdateThumbnail) {
						CamWrapper.getComWrapperInstance().GPCamAbort(_i32CommandIndex);
						CamWrapper.getComWrapperInstance().GPCamSendGetStatus();
						break;
					}				

					if (i32FileIndex + i32FileCount > m_strAryFileName.length)
						i32FileCount = m_strAryFileName.length - i32FileIndex;

					synchronized (m_i32AryFileStatus) {
						for (int i = i32FileIndex; i < i32FileCount + i32FileIndex; i++) {
							byte[] byTimeData = new byte[6];

							m_strAryFileName[i] = CamWrapper
									.getComWrapperInstance().GPCamGetFileName(i);
							m_i32AryFileSize[i] = CamWrapper
									.getComWrapperInstance().GPCamGetFileSize(i);
							CamWrapper.getComWrapperInstance().GPCamGetFileTime(i,
									byTimeData);
							
							StringBuilder sb = new StringBuilder();
							for (byte b : byTimeData) {
								sb.append(String.format("%02d", b));
							}
							m_strAryFileTime[i] = sb.toString();
							if(m_i32AryFileStatus[i] == FileTag_FileDeviceInit)
								m_i32AryFileStatus[i] = FileTag_FileDeviceReady;
							byTimeData = null;
						}
					}

					m_bRunCreateGridViewDone = false;

					if (m_UpdateThumbnailThread == null) {
						m_UpdateThumbnailThread = new Thread(
								new UpdateThumbnailRunnable());
						m_UpdateThumbnailThread.start();
					}
					break;
				}
				case CamWrapper.GPSOCK_Playback_CMD_GetThumbnail:
				{	
					if (m_i32AryFileStatus == null || m_strAryFileName == null || m_strAryFilePath == null || m_i32AryFileSize == null || m_strAryFileTime == null) {
						m_bRunCreateGridViewDone = true;
						break;
					}
					
					int i32FileIndex = (pbyData[0] & 0xFF)
							+ ((pbyData[1] & 0xFF) << 8);
					int i32Len = (pbyData[2] & 0xFF)
							+ ((pbyData[3] & 0xFF) << 8);
					char[] StringValus = new char[i32Len];

					StringValus[0] = 0;
					for (int i = 0; i < i32Len; i++)
						StringValus[i] = (char) (pbyData[i + 4] & 0xFF);
					
					_i32GotThumbnailFileIndex = i32FileIndex;
					_i32CommandIndex = i32CmdIndex;
					
					if (bIsStopUpdateThumbnail) {
						CamWrapper.getComWrapperInstance().GPCamAbort(_i32CommandIndex);
						CamWrapper.getComWrapperInstance().GPCamSendGetStatus();
						break;
					}

					synchronized (m_i32AryFileStatus) {
						if (m_i32AryFileStatus[_i32GotThumbnailFileIndex] == FileTag_FileGettingThumbnail) {
							i32GetThumbnailCount++;
						}

						m_strAryFilePath[_i32GotThumbnailFileIndex] = String.valueOf(StringValus);
						m_i32AryFileStatus[_i32GotThumbnailFileIndex] = FileTag_FileGotThumbnail;
					}					
					StringValus = null;
					break;
				}
				case CamWrapper.GPSOCK_Playback_CMD_GetRawData:
				{
					_i32CommandIndex = i32CmdIndex;
					
					if (bIsStopDownload) {
						CamWrapper.getComWrapperInstance().GPCamAbort(_i32CommandIndex);
						CamWrapper.getComWrapperInstance().GPCamSendGetStatus();
						bIsStopDownload = false;
						break;
					}
					
					int i32Finish = pbyData[0] & 0xFF;					
					if (i32Finish == 0x01) // Finish
					{
						int i32FileIndex = (pbyData[1] & 0xFF) + ((pbyData[2] & 0xFF) << 8);
						int i32Len = (pbyData[3] & 0xFF) + ((pbyData[4] & 0xFF) << 8);
						char[] StringValus = new char[i32Len];
						StringValus[0] = 0;
						for (int i = 0; i < i32Len; i++)
							StringValus[i] = (char) (pbyData[i + 5] & 0xFF);
						String strFilePath = String.valueOf(StringValus);

						if(m_DownloadDialog != null)
							m_DownloadDialog.setMessage("Copy file, please wait.");
						bIsCopingFile = true;
						copyFile(strFilePath, strDevicePICLocation + "/"
								+ m_strAryFileName[i32FileIndex]);

						if (m_DownloadDialog != null) {
							m_DownloadDialog.dismiss();
							m_DownloadDialog = null;
						}
						
						m_bPendingGetThumbnail = false;
					} else {
						bIsCopingFile = false;
						float fPercent = (pbyData[1] & 0xFF);
						if (m_DownloadDialog != null) {
							m_DownloadDialog.setMessage("Downloading ... ");
							m_DownloadDialog.setProgress((int) fPercent);
						}
					}
					break;
				}
				case CamWrapper.GPSOCK_Playback_CMD_Stop:
					break;
				}	
				break;
			case CamWrapper.GPSOCK_MODE_Menu:
				Log.e(TAG, "GPSOCK_MODE_Menu ... ");
				break;
			case CamWrapper.GPSOCK_MODE_Vendor:
				Log.e(TAG, "GPSOCK_MODE_Vendor ... ");
				break;
			}
    	}
    	else if (i32CmdType == CamWrapper.GP_SOCK_TYPE_NAK)
    	{
    		int i32ErrorCode = (pbyData[0] & 0xFF) + ((pbyData[1] & 0xFF) << 8);
    		
    		switch(i32ErrorCode)
    		{
    		case CamWrapper.Error_ServerIsBusy:
    			Log.e(TAG, "Error_ServerIsBusy ... ");
    			_i32ErrorCount++;

				if (_i32ErrorCount > 20) {
					_i32ErrorCount = 0;
					bIsStopDownload = true;
					CamWrapper.getComWrapperInstance().GPCamAbort(_i32CommandIndex);
					CamWrapper.getComWrapperInstance().GPCamSendGetStatus();
					Toast.makeText(m_Context, "Getting thumbnail is failed.",
							Toast.LENGTH_SHORT).show();
				}
    			break;
    		case CamWrapper.Error_InvalidCommand:
    			Log.e(TAG, "Error_InvalidCommand ... ");
    			m_bPendingGetThumbnail = false;	
				synchronized (m_i32AryFileStatus) {
					if (_i32GotThumbnailFileIndex + 1 < m_i32AryFileStatus.length
							&& !bIsStopDownload)
						m_i32AryFileStatus[_i32GotThumbnailFileIndex + 1] = FileTag_FileBroken;
				}
    			break;
    		case CamWrapper.Error_RequestTimeOut:
				Log.e(TAG, "Error_RequestTimeOut ... ");
				break;
    		case CamWrapper.Error_ModeError:
				Log.e(TAG, "Error_ModeError ... ");
				break;
    		case CamWrapper.Error_NoStorage:
				Log.e(TAG, "Error_NoStorage ... ");
				runOnUiThread(new Runnable() {
			        public void run()     
			        {     
			        	Toast.makeText(m_Context, "Failed. No Storage.",
								Toast.LENGTH_SHORT).show();
			        }     
			    });
				break;
    		case CamWrapper.Error_WriteFail:
				Log.e(TAG, "Error_WriteFail ... ");
				break;
    		case CamWrapper.Error_GetFileListFail:
				Log.e(TAG, "Error_GetFileListFail ... ");
				break;
    		case CamWrapper.Error_GetThumbnailFail:
				Log.e(TAG, "Error_GetThumbnailFail ... ");
				synchronized (m_i32AryFileStatus) {
					if (_i32GotThumbnailFileIndex + 1 < m_i32AryFileStatus.length
							&& !bIsStopDownload)
						m_i32AryFileStatus[_i32GotThumbnailFileIndex + 1] = FileTag_FileBroken;
				}
				break;
    		case CamWrapper.Error_FullStorage:
				Log.e(TAG, "Error_FullStorage ... ");
				break;
    		case CamWrapper.Error_SocketClosed:
    			Log.e(TAG, "Error_SocketClosed ... ");
    			FinishToMainController();
				break;
    		case CamWrapper.Error_LostConnection:
				Log.e(TAG, "Error_LostConnection ...");
				FinishToMainController();
				break;
    		}
    	}
    }
    
    private void FinishToMainController() {
    	Log.e(TAG, "Finish ...");
    	CamWrapper.getComWrapperInstance().GPCamDisconnect();
    	finish();
    }
}
