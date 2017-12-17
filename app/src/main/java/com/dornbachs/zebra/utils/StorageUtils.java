/**
 * @File StorageUtils.java
 * 
 * @Author ZTB 2013-8-26
 *         Copyright(C) 2013 Dalian Hi-Think Computer Technology, Corp. All rights reserved.
 */
package com.dornbachs.zebra.utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.sql.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Locale;
import java.util.zip.ZipException;

import org.apache.tools.zip.ZipEntry;
import org.apache.tools.zip.ZipFile;
import org.apache.tools.zip.ZipOutputStream;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.os.Environment;

/**
 * 存储工具类
 * 
 * @version 1.00
 * @Author ZTB 2013-8-26
 */
public class StorageUtils {
	private static final String DIR_EXTERNAL = Environment.getExternalStorageDirectory() + "/";

	/** This application's private file dir name */
	public static final String DIR_APP = "LRKJ";

	private static final String DIR_IPC_PHOTO = "IPCamera";
	private static final String DIR_IMG = "._imagesCache";
	private static final String DIR_IMG_ALBUM_CACHE = "._imagesAlbumCache";
	private static final String DIR_IMG_TEMP = "._imagesTempCache";

	private static final int BUFF_SIZE = 1024 * 1024; // 1M Byte

	private static final String DIR_SETTING = "._setting";

	private static final String DIR_DOWNLOAD = "._download";
	private static final String DIR_UPLOAD = "upload";
	private static final String DIR_DCIM = "DCIM/Camera";

	/**
	 * Application image external dir.
	 * 
	 * @return
	 * @Author ZTB 2013-8-27
	 */
	public static File getAppExtImageDir() {
		File file = new File(DIR_EXTERNAL + DIR_APP + "/" + DIR_IMG);
		if (!file.exists()) {
			file.mkdirs();
		}
		return file;
	}

	public static File getAppAlbumCacheImageDir() {
		File file = new File(DIR_EXTERNAL + DIR_APP + "/" + DIR_IMG_ALBUM_CACHE);
		if (!file.exists()) {
			file.mkdirs();
		}
		return file;
	}

	public static File getAppExtTempImageDir() {
		File file = new File(DIR_EXTERNAL + DIR_APP + "/" + DIR_IMG_TEMP);
		if (!file.exists()) {
			file.mkdirs();
		}
		return file;
	}

	// 取得图片文件夹的路径
	public static String getAppExtImageDirPath() {
		return DIR_EXTERNAL + DIR_APP + "/" + DIR_IMG;
	}

	public static String getAppExtAlbumCacheDirPath() {
		return DIR_EXTERNAL + DIR_APP + "/" + DIR_IMG_ALBUM_CACHE;
	}

	public static String getDCIM() {
		return DIR_EXTERNAL + DIR_DCIM;
	}

	public static String getAppExtIPCameraPath() {
		return DIR_EXTERNAL + DIR_APP + "/" + DIR_IPC_PHOTO;
	}

	public static File getAppExtIPCameraDir() {
		File file = new File(DIR_EXTERNAL + DIR_APP + "/" + DIR_IPC_PHOTO);
		if (!file.exists()) {
			file.mkdirs();
		}
		return file;
	}

	// 取得设定文件夹的路径
	public static String getAppExtSettingDirPath() {
		return DIR_EXTERNAL + DIR_APP + "/" + DIR_SETTING;
	}

	// 取得下载文件的路径
	public static String getAppExtDownloadDirPath() {
		return DIR_EXTERNAL + DIR_APP + "/" + DIR_DOWNLOAD;
	}

	/**
	 * Return the <b>Full path</b> of your image file.
	 * 
	 * @param filename
	 * @return
	 * @Author ZTB 2013-8-29
	 */
	public static String getPublicImagePath(String filename) {
		return DIR_EXTERNAL + DIR_APP + "/" + DIR_IMG + "/" + "." + filename;
	}

	public static String getPublicPath(String filename) {
		return DIR_EXTERNAL + DIR_APP + "/" + filename;
	}

	public static String getPublicUploadPath(String filename) {
		File file = new File(DIR_EXTERNAL + DIR_APP + "/" + DIR_UPLOAD);
		if (!file.exists()) {
			file.mkdirs();
		}
		return DIR_EXTERNAL + DIR_APP + "/" + DIR_UPLOAD + "/" + filename;
	}

	/**
	 * Save bitmap to external image dir.
	 * 
	 * @param bm
	 * @param filename
	 * @Author ZTB 2013-8-27
	 */
	public static void save2PublicImageDir(Bitmap bm, String filename) {
		if (bm == null)
			return;
		File file = new File(getAppExtImageDir(), "." + filename);
		if (!file.exists()) {
			try {
				file.createNewFile();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
		try {
			FileOutputStream out = new FileOutputStream(file);
			CompressFormat format = CompressFormat.JPEG;
			if (filename.toLowerCase(Locale.PRC).endsWith("png")) {
				format = CompressFormat.PNG;
			}
			if (bm.compress(format, 90, out)) {
				out.flush();
				out.close();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Save bitmap to external image dir.
	 * 
	 * @param bm
	 * @param filename
	 * @Author ZTB 2013-8-27
	 */
	public static void save2PublicTempImageDir(Bitmap bm, String filename) {
		if (bm == null)
			return;
		File file = new File(getAppExtAlbumCacheDirPath(), "." + filename);
		if (!file.exists()) {
			try {
				file.createNewFile();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
		try {
			FileOutputStream out = new FileOutputStream(file);
			CompressFormat format = CompressFormat.JPEG;
			if (filename.toLowerCase(Locale.PRC).endsWith("png")) {
				format = CompressFormat.PNG;
			}
			if (bm.compress(format, 90, out)) {
				out.flush();
				out.close();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Write bytes to external image dir.
	 * 
	 * @param datas
	 * @param filename
	 * @Author ZTB 2013-8-27
	 */
	public static void save2PublicImageDir(byte[] datas, String filename) {
		File file = new File(getAppExtImageDir(), "." + filename);
		if (!file.exists()) {
			try {
				file.createNewFile();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
		try {
			FileOutputStream out = new FileOutputStream(file);
			out.write(datas, 0, datas.length);
			out.flush();
			out.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Delete public image dir file.
	 * 
	 * @param filename
	 * @Author ZTB 2013-8-29
	 */
	public static boolean deleteImgFromPublicImageDir(String filename) {
		File file = new File(getAppExtImageDir(), "." + filename);
		if (file.exists()) {
			return file.delete();
		}
		return true;
	}

	/**
	 * Get bitmap from external dir by specified name.
	 * 
	 * @param filename
	 * @return
	 * @Author ZTB 2013-8-27
	 */
	public static Bitmap getImgFromPublicDir(String filename) {
		if (null == filename || "".equals(filename)) {
			return null;
		}
		File file = new File(DIR_EXTERNAL + DIR_APP + "/" + DIR_IMG + "/", "." + filename);
		if (!file.exists())
			return null;
		FileInputStream in;
		try {
			in = new FileInputStream(file);
			Bitmap bm = BitmapFactory.decodeStream(in);
			in.close();
			return bm;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Write bytes datas to specified filename file.
	 * 
	 * @param filename
	 * @param datas
	 * @Author ZTB 2013-8-26
	 */
	public static void save2PrivateDir(Context mContext, String filename, byte[] datas) {
		File file = new File(mContext.getDir(DIR_APP, 0), filename);
		if (!file.exists()) {
			try {
				file.createNewFile();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
		try {
			FileOutputStream out = new FileOutputStream(file);
			out.write(datas, 0, datas.length);
			out.flush();
			out.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static boolean deleteFromPrivateDir(Context mContext, String filename) {
		File file = new File(mContext.getDir(DIR_APP, 0), filename);
		if (file.exists()) {
			return file.delete();
		}
		return true;
	}

	/**
	 * Save bitmap tp private dir file.
	 * 
	 * @param filename
	 * @param bm
	 * @Author ZTB 2013-8-26
	 */
	public static void saveBitmap2PrivateDir(Context mContext, String filename, Bitmap bm) {
		File file = new File(mContext.getDir(DIR_APP, 0), filename);
		try {
			FileOutputStream out = new FileOutputStream(file);
			CompressFormat format = CompressFormat.JPEG;
			if (filename.toLowerCase(Locale.PRC).endsWith("png")) {
				format = CompressFormat.PNG;
			}
			if (bm.compress(format, 90, out)) {
				out.flush();
				out.close();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Delete private file if exist.
	 * 
	 * @param filename
	 * @Author ZTB 2013-8-26
	 */
	public static void deletePrivateDirFile(Context mContext, String filename) {
		File file = new File(mContext.getDir(DIR_APP, 0), filename);
		if (file != null && file.exists())
			file.delete();
	}

	/**
	 * Write bytes to private dir file.
	 * 
	 * @param datas
	 * @param filename
	 * @Author WM 2013-10-29
	 */
	public static void save2PrivateFile(byte[] datas, String filename, String filePath) {
		File savePath = new File(filePath);
		if (!savePath.exists()) {
			savePath.mkdirs();
		}

		File file = new File(filePath, filename);
		if (!file.exists()) {
			try {
				file.createNewFile();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
		try {
			FileOutputStream out = new FileOutputStream(file);
			out.write(datas, 0, datas.length);
			out.flush();
			out.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void save2PrivateFile(InputStream input) {
		String filePath = getAppExtDownloadDirPath();
		File savePathFile = new File(filePath);
		if (!savePathFile.exists()) {
			savePathFile.mkdirs();
		}

		File file = new File(filePath, "download.zip");
		if (!file.exists()) {
			try {
				file.createNewFile();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}

		FileOutputStream fileOutStream = null;
		try {
			fileOutStream = new FileOutputStream(file);
			byte[] tmpBuf = new byte[1024];
			int tmpLen = 0;
			while ((tmpLen = input.read(tmpBuf)) > 0) {
				fileOutStream.write(tmpBuf, 0, tmpLen);
			}

		} catch (IllegalStateException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				fileOutStream.flush();
				fileOutStream.close();
				input.close();
			} catch (IOException e) {
				e.printStackTrace();
			}

		}
	}

	// 用于格式化日期,作为日志文件名的一部分
	private static DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");

	public static boolean copyfile(File fromFile, String toFilePath) {
		String str = "";
		try {
			str = new String(toFilePath.getBytes("UTF-8"), "UTF-8");
		} catch (UnsupportedEncodingException e) {
			return false;
		}

		File desFile = new File(toFilePath);
		int i = 1;
		String fileName = desFile.getName();
		String newFileName = "";
		while (desFile.exists()) {
			if (fileName.indexOf(".") >= 0) {
				newFileName = fileName.replace(".", "_" + i + ".");
			} else {
				newFileName = fileName + "_" + i;
			}

			desFile = new File(str.replace(fileName, newFileName));
			i++;
		}

		File fileParentDir = desFile.getParentFile();
		if (!fileParentDir.exists()) {
			fileParentDir.mkdirs();
		}

		try {
			FileInputStream fosfrom = new java.io.FileInputStream(fromFile);
			FileOutputStream fosto = new FileOutputStream(desFile);
			byte bt[] = new byte[1024];
			int c;
			while ((c = fosfrom.read(bt)) > 0) {
				fosto.write(bt, 0, c); // 将内容写到新文件当中
			}
			fosfrom.close();
			fosto.close();

		} catch (Exception ex) {
			try {
				long timestamp = System.currentTimeMillis();
				String time = formatter.format(new Date(System.currentTimeMillis()));
				String fileName111 = "crash-" + time + "-" + timestamp + ".txt";
				if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
					String path = "/sdcard/@author ztb/crash/";
					File dir = new File(path);
					if (!dir.exists()) {
						dir.mkdirs();
					}
					FileOutputStream fos = new FileOutputStream(path + fileName111);
					fos.write((fromFile + "\n" + desFile + "\n" + ex.getMessage() + "\n" + ex.hashCode() + "\n" + ex.toString()).getBytes());
					fos.close();
				}
			} catch (Exception e) {
				// Log.e(TAG, "an error occured while writing file...", e);
			}
			return false;
		}

		return true;
	}

	public static boolean deleteFileFromPublicDir(String filepath) {
		File file = new File(filepath);
		if (file.exists()) {
			return file.delete();
		}
		return true;
	}

	/**
	 * 压缩文件
	 * 
	 * @param resFile
	 *            需要压缩的文件（夹）
	 * @param zipout
	 *            压缩的目的文件
	 * @param rootpath
	 *            压缩的文件路径
	 * @throws FileNotFoundException
	 *             找不到文件时抛出
	 * @throws IOException
	 *             当压缩过程出错时抛出
	 */
	private static void zipFile(File resFile, ZipOutputStream zipout, String rootpath) throws FileNotFoundException, IOException {
		zipout.setEncoding("UTF-8");
		rootpath = rootpath + (rootpath.trim().length() == 0 ? "" : File.separator) + resFile.getName();
		rootpath = new String(rootpath.getBytes("UTF-8"), "UTF-8");

		if (resFile.isDirectory()) {
			File[] fileList = resFile.listFiles();
			if (0 == fileList.length) {
				ZipEntry ze = new ZipEntry(rootpath + File.separator);
				ze.setUnixMode(777);
				zipout.putNextEntry(ze);
				zipout.closeEntry();
			} else {
				for (File file : fileList) {
					zipFile(file, zipout, rootpath);
				}
			}
		} else {
			byte buffer[] = new byte[BUFF_SIZE];
			BufferedInputStream in = new BufferedInputStream(new FileInputStream(resFile), BUFF_SIZE);
			ZipEntry ze = new ZipEntry(rootpath);
			ze.setUnixMode(777);
			zipout.putNextEntry(ze);
			int realLength;
			while ((realLength = in.read(buffer)) != -1) {
				zipout.write(buffer, 0, realLength);
			}
			in.close();
			zipout.flush();
			zipout.closeEntry();
		}
	}

	/**
	 * 批量压缩文件（夹）
	 * 
	 * @param resFileList
	 *            要压缩的文件（夹）列表
	 * @param zipFile
	 *            生成的压缩文件
	 * @throws IOException
	 *             当压缩过程出错时抛出
	 */
	public static void zipFiles(Collection<File> resFileList, File zipFile) throws IOException {
		ZipOutputStream zipout = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(zipFile), BUFF_SIZE));
		for (File resFile : resFileList) {
			zipFile(resFile, zipout, "");
		}
		zipout.close();
	}

	
	/**
	 * Unzip files to specified path, with overwrite option.
	 * @param zipFile
	 * @param folderPath
	 * @param overwrite
	 * @throws ZipException
	 * @throws IOException
	 * @author Ztb, 2015-10-29下午4:17:02
	 */
	public static void unZipFile(File zipFile, String folderPath, boolean overwrite) throws ZipException, IOException {
		File desDir = new File(folderPath);
		if (!desDir.exists()) {
			desDir.mkdirs();
		}
		ZipFile zf = new ZipFile(zipFile.getAbsolutePath(), "UTF-8");
		for (Enumeration<?> entries = zf.getEntries(); entries.hasMoreElements();) {
			ZipEntry entry = ((ZipEntry) entries.nextElement());
			String name = entry.getName();
			String str = folderPath + File.separator + entry.getName();
			str = new String(str.getBytes("UTF-8"), "UTF-8");
			if (name.endsWith(File.separator)) {
				File f = new File(str);
				f.mkdirs();
				continue;
			}

			InputStream in = zf.getInputStream(entry);
			File desFile = new File(str);
			int i = 1;
			String fileName = desFile.getName();
			if (!overwrite) {
				String newFileName = "";
				while (desFile.exists()) {
					if (fileName.indexOf(".") >= 0) {
						newFileName = fileName.replace(".", "_" + i + ".");
					} else {
						newFileName = fileName + "_" + i;
					}

					desFile = new File(str.replace(fileName, newFileName));
					i++;
				}
			}

			File fileParentDir = desFile.getParentFile();
			if (!fileParentDir.exists()) {
				fileParentDir.mkdirs();
			}
			OutputStream out = new FileOutputStream(desFile);
			byte buffer[] = new byte[BUFF_SIZE];
			int realLength;
			while ((realLength = in.read(buffer)) > 0) {
				out.write(buffer, 0, realLength);
			}
			in.close();
			out.close();
		}
	}

	/**
	 * Application setting external dir.
	 * 
	 * @return
	 * @Author dhc 2014-3-14
	 */
	public static File getAppExtSettingDir() {
		File file = new File(DIR_EXTERNAL + DIR_APP + "/" + DIR_SETTING);
		if (!file.exists()) {
			file.mkdirs();
		}
		return file;
	}

	/**
	 * Save setting to external setting dir.
	 * 
	 * @param setting
	 * @param userId
	 * @Author dhc 2014-4-14
	 */
	public static void save2PublicSettingDir(String setting, String userId) {
		if (setting == "") {
			return;
		}

		File file = new File(getAppExtSettingDir(), userId + ".txt");
		if (!file.exists()) {
			try {
				file.createNewFile();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}

		FileOutputStream out = null;
		try {
			out = new FileOutputStream(file);
			out.write(setting.getBytes("utf-8"));
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (out != null) {
				try {
					out.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}


	/**
	 * 获取文件夹大小
	 * 
	 * @param file
	 *            File实例
	 * @return long
	 * @throws Exception
	 */
	public static long getFolderSize(File file) {
		long size = 0;
		if (file.isFile()) {
			size = size + file.length();
		} else {
			File[] fileList = file.listFiles();
			for (int i = 0; i < fileList.length; i++) {
				if (fileList[i].isDirectory()) {
					size = size + getFolderSize(fileList[i]);
				} else {
					size = size + fileList[i].length();
				}
			}
		}
		return size;
	}

	public static void delete(File file) {
		if (file.isFile()) {
			file.delete();
			return;
		}

		if (file.isDirectory()) {
			File[] childFiles = file.listFiles();
			if (childFiles == null || childFiles.length == 0) {
				file.delete();
				return;
			}

			for (int i = 0; i < childFiles.length; i++) {
				delete(childFiles[i]);
			}
			file.delete();
		}
	}

	public static void deleteAllZipFils() {
		File file = new File(DIR_EXTERNAL + DIR_APP + "/" + DIR_UPLOAD);
		if (file.exists()) {
			delete(file);
		}
	}

	public static long countLength(File file) {
		if (file.isFile()) {
			return file.length();
		}
		long total = 0l;
		if (file.isDirectory()) {
			File[] childFiles = file.listFiles();
			if (childFiles == null || childFiles.length == 0) {
				return file.length();
			}

			for (int i = 0; i < childFiles.length; i++) {
				total += countLength(childFiles[i]);
			}

		}
		return total;
	}
}
