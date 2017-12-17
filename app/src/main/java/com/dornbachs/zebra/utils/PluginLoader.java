/**
 * TODO
 * By ztb, 2015-10-22
 */
package com.dornbachs.zebra.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.zip.ZipException;

import android.app.ProgressDialog;
import android.os.Environment;
import android.widget.Toast;

import com.dornbachs.zebra.TGApplication;
import com.litesuits.http.HttpConfig;
import com.litesuits.http.LiteHttp;
import com.litesuits.http.exception.HttpException;
import com.litesuits.http.listener.HttpListener;
import com.litesuits.http.request.AbstractRequest;
import com.litesuits.http.request.FileRequest;
import com.litesuits.http.response.Response;

/**
 * This class contains some APIs like {@code compareVersion()} to check plugin version, {@linkplain
 * downloadPlugin()} to download specified plugin bundle from network, {@linkplain
 * uninstallPlugin()} to uninstall plugin bundle from native,
 * 
 * 
 * @author ztb, 2015-10-22
 */
public class PluginLoader {
	private LiteHttp mHttpClient;
	private String mPluginFolderPath;

	/**
	 * Singleton instance.
	 */
	private PluginLoader() {
		this.initLiteHttp();
		this.initFolder();
	}

	private static PluginLoader sInstance = null;

	public static PluginLoader loader() {
		if (sInstance == null)
			sInstance = new PluginLoader();
		return sInstance;
	}

	/**
	 * Initiate http things.
	 * 
	 * @author Ztb, 2015-10-22下午3:06:26
	 */
	private void initLiteHttp() {
		if (mHttpClient == null) {
			HttpConfig config = new HttpConfig(TGApplication.getInstance()).setDebugged(false).setDetectNetwork(true)
			        .setDoStatistics(false).setUserAgent("ABOX(Android)").setTimeOut(10000, 10000);
			mHttpClient = LiteHttp.newApacheHttpClient(config);
		} else {
			mHttpClient.getConfig().setDebugged(false).setDetectNetwork(true).setDoStatistics(false).setUserAgent("ABOX(Android)")
			        .setTimeOut(10000, 10000);
		}
	}

	private void initFolder() {
		// root
		this.mPluginFolderPath = Environment.getExternalStorageDirectory().getPath() + "/" + TGApplication.getInstance().getPackageName()
		        + "/images";
		File f = new File(this.mPluginFolderPath);
		if (!f.exists()) {
			f.mkdirs();
		}
	}

	// This is compositing the 'xxx' folder full path.
	private String getPluginPathBy(String xxx) {
		String f = mPluginFolderPath + "/" + xxx;
		File file = new File(f);
		if (file.exists())
			file.delete();
		file = new File(f + ".bin");
		if (file.exists())
			file.delete();
		return f;
	}

	private boolean copyNeededAssets(String from, String to) {
		boolean ok = false;
		try {
			InputStream in = TGApplication.getInstance().getAssets().open(from);
			File desFile = new File(to);
			File fileParentDir = desFile.getParentFile();
			if (!fileParentDir.exists()) {
				fileParentDir.mkdirs();
			}

			FileOutputStream fosto = new FileOutputStream(desFile);
			byte bt[] = new byte[2048];
			int c;
			while ((c = in.read(bt)) > 0) {
				fosto.write(bt, 0, c);
			}
			in.close();
			fosto.close();
			ok = true;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return ok;
	}

	public boolean downloadPlugin(final String devType, String downloadUrl, final ProgressDialog downProgress) {
		if (downloadUrl == null)
			return false;
		final String localPath = getPluginPathBy(devType);
		mHttpClient.executeAsync(new FileRequest(downloadUrl, localPath + ".bin") {

		}.setHttpListener(new HttpListener<File>(true, true, false) {

			@Override
			public void onLoading(AbstractRequest<File> request, long total, long len) {
				if (!downProgress.isShowing())
					downProgress.show();
				downProgress.setMax((int) total);
				downProgress.setProgress((int) len);
			}

			@Override
			public void onSuccess(File data, Response<File> response) {
				boolean ok = false;
				try {
					StorageUtils.unZipFile(data, mPluginFolderPath, false);
					ok = true;
				} catch (ZipException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
				if (ok) {
					new File(localPath + ".bin").delete();
				}

				Toast.makeText(TGApplication.getInstance(), (ok ? "安装完成" : "安装失败"), Toast.LENGTH_SHORT).show();
				downProgress.dismiss();
			}

			@Override
			public void onCancel(File data, Response<File> response) {
				downProgress.dismiss();
				Toast.makeText(TGApplication.getInstance(), "安装已取消", Toast.LENGTH_SHORT).show();
			}

			@Override
			public void onFailure(HttpException e, Response<File> response) {
				downProgress.dismiss();
				Toast.makeText(TGApplication.getInstance(), "安装失败", Toast.LENGTH_SHORT).show();
			}

		}));
		return true;
	}

	/**
	 * Common usage to download {@code downloadUrl} to sdcard path {@code dest};
	 * 
	 * @param downloadUrl
	 * @param dest
	 * @param downProgress
	 * @return
	 * @author Ztb, 2015-11-25上午11:32:03
	 */
	public boolean downloadFile(String downloadUrl, String dest, HttpListener<File> l) {
		if (downloadUrl == null)
			return false;
		mHttpClient.executeAsync(new FileRequest(downloadUrl, dest) {}.setHttpListener(l));
		return true;
	}

	/**
	 * Uninstall the specified plugin bundle and may delete it from disk.
	 * 
	 * @param xxx
	 * @author Ztb, 2015-10-22上午10:07:40
	 */
	public void uninstallPlugin(String devType) {
		File f = new File(getPluginPathBy(devType));
		f.delete();
	}
	
	public File[] getInstalledPlugins() {
		File f = new File(mPluginFolderPath);
		if (f.exists()) {
			return f.listFiles(new FilenameFilter() {
				
				@Override
				public boolean accept(File dir, String filename) {
					return (!filename.startsWith("__") && !filename.startsWith("."));
				}
			});
		}
		return null;
	}
	
	/**
	 * For get LiteHttp client.
	 * @return
	 * @author Ztb, 2015-12-16上午8:56:59
	 */
	public LiteHttp getHttpClient() {
		return mHttpClient;
	}

	/**
	 * Common method for comparing 2 version string.
	 * 
	 * @param v1
	 * @param v2
	 * @return 0 is same, -1 is lower, 1 is higher.
	 * @author Ztb, 2015-10-22上午10:02:17
	 */
	public static int compareVersion(String v1, String v2) {
		return v1.compareToIgnoreCase(v2);
	}

	public static boolean isDiskMounted() {
		return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
	}
}
