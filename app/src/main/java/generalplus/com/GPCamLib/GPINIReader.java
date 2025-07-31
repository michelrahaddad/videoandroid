package generalplus.com.GPCamLib;

import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.FileInputStream;
import java.util.Properties;

/**
 * Helper class that reads and writes a simple properties-based
 * configuration file for the GeneralPlus camera library.  The file
 * lives under the camera folder defined by {@link CamWrapper#CamDefaulFolderName}
 * and is named {@link CamWrapper#ConfigFileName}.  Two boolean
 * properties are currently supported:
 *
 * <ul>
 *   <li>{@code ShowLog} – when set to true, verbose logging is
 *   enabled in the native library.</li>
 *   <li>{@code SaveLog} – when set to true, logs are written to a
 *   file on external storage.</li>
 * </ul>
 *
 * The file is created with default values on first run if it does
 * not already exist.  To retrieve an instance of this class, call
 * {@link #getInstance()} after construction.  Note that this class
 * maintains static flags for enabling logging that can be queried via
 * {@link #isEnableShowLog()} and {@link #isEnableSaveLog()}.
 */
public class GPINIReader {
    private static final String TAG = "GPINIReader";

    private static final String INIReader_ShowLog = "ShowLog";
    private static final String INIReader_SaveLog = "SaveLog";

    private static GPINIReader sInstance;

    private final Properties configuration = new Properties();
    private final String configurationFile;

    private static boolean sEnableShowLog;
    private static boolean sEnableSaveLog;

    public GPINIReader() {
        sInstance = this;
        configurationFile = Environment.getExternalStorageDirectory().getPath()
                + "/" + CamWrapper.CamDefaulFolderName + "/" + CamWrapper.ConfigFileName;

        File f = new File(configurationFile);
        if (!f.exists()) {
            try {
                // Ensure the parent directory exists
                File parent = f.getParentFile();
                if (parent != null && !parent.exists()) {
                    if (!parent.mkdirs()) {
                        Log.w(TAG, "Unable to create configuration directory: " + parent.getAbsolutePath());
                    }
                }
                FileWriter fw = new FileWriter(f);
                StringBuilder strTemp = new StringBuilder();
                strTemp.append(String.format("%s = false\n", INIReader_SaveLog));
                strTemp.append(String.format("%s = false\n", INIReader_ShowLog));
                fw.write(strTemp.toString());
                fw.close();
            } catch (IOException e) {
                Log.e(TAG, "Failed to create default configuration", e);
            }
        }

        if (load()) {
            String strProperty = get(INIReader_SaveLog);
            sEnableSaveLog = strProperty != null && strProperty.trim().equalsIgnoreCase("true");
            strProperty = get(INIReader_ShowLog);
            sEnableShowLog = strProperty != null && strProperty.trim().equalsIgnoreCase("true");
        }
    }

    /**
     * Returns the singleton instance.  You must instantiate this class
     * before calling getInstance(), otherwise null will be returned.
     */
    public static GPINIReader getInstance() {
        return sInstance;
    }

    public boolean isEnableShowLog() {
        return sEnableShowLog;
    }

    public boolean isEnableSaveLog() {
        return sEnableSaveLog;
    }

    /**
     * Loads the configuration file from disk into the Properties
     * object.  If loading fails, no properties are modified.
     *
     * @return true if the configuration file was loaded successfully
     */
    public boolean load() {
        boolean retval = false;
        try (FileInputStream fis = new FileInputStream(this.configurationFile)) {
            configuration.load(fis);
            retval = true;
        } catch (IOException e) {
            Log.e(TAG, "Configuration error", e);
        }
        return retval;
    }

    /**
     * Retrieves the value of the given key from the loaded
     * configuration.
     *
     * @param key property name
     * @return the property value or null if not found
     */
    public String get(String key) {
        return configuration.getProperty(key);
    }
}