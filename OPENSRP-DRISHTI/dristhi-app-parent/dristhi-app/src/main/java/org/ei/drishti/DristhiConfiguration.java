package org.ei.drishti;

import java.io.IOException;
import java.util.Properties;

import org.ei.drishti.util.IntegerUtil;

import android.content.res.AssetManager;
import android.preference.PreferenceManager;

public class DristhiConfiguration {

    private static final String DRISHTI_BASE_URL = "DRISHTI_BASE_URL";
    private static final String HOST = "HOST";
    private static final String PORT = "PORT";
    private static final String SHOULD_VERIFY_CERTIFICATE = "SHOULD_VERIFY_CERTIFICATE";
    private static final String SYNC_DOWNLOAD_BATCH_SIZE = "SYNC_DOWNLOAD_BATCH_SIZE";

    private Properties properties = new Properties();

    public DristhiConfiguration(AssetManager assetManager) {
        try {
            properties.load(assetManager.open("app.properties"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String get(String key) {
        return properties.getProperty(key);
    }

    public String host() {
       // return this.get(HOST);
    	return getPreference(org.ei.drishti.Context.getInstance().applicationContext(), HOST, "localhosted");
    }

    public int port() {
       // return Integer.parseInt(this.get(PORT));
    	return Integer.parseInt(getPreference(org.ei.drishti.Context.getInstance().applicationContext(), PORT, null));
    }

    public boolean shouldVerifyCertificate() {
        return Boolean.parseBoolean(this.get(SHOULD_VERIFY_CERTIFICATE));
    }

    public String dristhiBaseURL() {
        //return this.get(DRISHTI_BASE_URL);
    	return getPreference(org.ei.drishti.Context.getInstance().applicationContext(), DRISHTI_BASE_URL, "localhosted");
    }

    public int syncDownloadBatchSize() {
        return IntegerUtil.tryParse(this.get(SYNC_DOWNLOAD_BATCH_SIZE), 100);
    }
    
    public static String getPreference(android.content.Context context, String key, String defaultVal){
		return PreferenceManager.getDefaultSharedPreferences(context).getString(key, defaultVal);
	}

}
