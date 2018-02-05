package com.lrkj.views;

import android.app.Activity;
import android.content.Intent;
import android.database.DataSetObservable;
import android.database.DataSetObserver;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.android.tu.loadingdialog.LoadingDailog;
import com.lrkj.LrApplication;
import com.lrkj.business.LrNativeApi;
import com.lrkj.business.LrRobot;
import com.lrkj.ctrl.R;
import com.lrkj.defines.LrDefines;
import com.lrkj.utils.LrToast;
import com.mobeta.android.dslv.DragSortListView;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;

public class LrActAllMap extends LrBaseAct implements ListAdapter, View.OnClickListener {

    DragSortListView mListView;

    String Prex = "/mnt/sdcard/com.lrkj.ctrl/";
    final String PrexF = "/mnt/sdcard/com.lrkj.ctrl/";

    ArrayList<File> mFiles = new ArrayList<>();
    String mIp = null;
    boolean mIsNavi = false;
    boolean mSortAsc = true;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_all_maps);

        mIp = getIntent().getStringExtra("ip");
        mIsNavi = getIntent().getBooleanExtra("navi", false);
        Prex += (mIsNavi ? "navi/" : "maps/");
        ((TextView)findViewById(R.id.tvTitle)).setText(mIsNavi ? "地图导航":"地图管理");
        mListView = (DragSortListView) findViewById(android.R.id.list);
        mListView.setAdapter(this);
        mListView.setDragEnabled(false);
        mListView.setRemoveListener(onMore);

        if (mIsNavi) {
            loadMapDatas();
        }else {
            updateSceneList();
        }
    }

    @Override
    public void onPause()
    {
        super.onPause();

    }

    @Override
    public void onResume()
    {
        super.onResume();
        LrApplication.mkMapFolder();
    }

    public void onClickReload(View v) {
        loadMapDatas();
    }

    public void onClickDelAll(View v) {
        if (LrRobot.sendCmd(mIp, LrDefines.PORT_MAPS, 2020, null)) {
            File folder = new File(Prex);
            File[] fileArray = folder.listFiles();
            if (fileArray != null)
                for (File f : fileArray) {
                    f.delete();
                }
            updateSceneList();
        }
    }


    void updateSceneList() {
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                mFiles.clear();
                File folder = new File(Prex);
                File[] fileArray = folder.listFiles();
                if (fileArray != null)
                for (File f : fileArray) {
                    if (f.isFile() && f.getName().contains(".jpg")) {
                        mFiles.add(f);
                    }
                }
                notifyDataSetChanged();
            }
        });
    }

    public class FileSortComparator implements Comparator<File> {
        String mT = "name";
        boolean mAsc = true;
        public FileSortComparator(String type, boolean asc){
            super();
            mT = type;
            mAsc = asc;
        }
        @Override
        public int compare(File pFile1, File pFile2) {
            if ("name".equalsIgnoreCase(mT)) {
                if (mAsc)
                    return pFile1.getName().compareToIgnoreCase(pFile2.getName());
                else
                    return pFile2.getName().compareToIgnoreCase(pFile1.getName());
            } else if ("time".equalsIgnoreCase(mT)){
                if (mAsc)
                    return pFile1.lastModified() > pFile2.lastModified() ? 1 : -1;
                else
                    return pFile1.lastModified() > pFile2.lastModified() ? -1 : 1;
            }

            return 0;
        }
    }

    private void loadMapDatas() {
        LoadingDailog.Builder loadBuilder = new LoadingDailog.Builder(this)
                .setMessage("加载中...")
                .setCancelable(true)
                .setCancelOutside(false);
        final LoadingDailog dialog=loadBuilder.create();
        dialog.show();

        new Thread(new Runnable() {
            @Override
            public void run() {
                LrNativeApi.getAllMaps(mIsNavi ? 1 : 0);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        dialog.dismiss();
                        updateSceneList();
                    }
                });
            }
        }).start();
    }

    private DragSortListView.RemoveListener onMore = new DragSortListView.RemoveListener() {
        @Override
        public boolean remove(final int which) {

            return true;
        }
    };

    public void onDestroy() {
        super.onDestroy();

    }

	/*------------------------ ListAdapter - Start ------------------------*/

    SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private final DataSetObservable mDataSetObservable = new DataSetObservable();

    public void registerDataSetObserver(DataSetObserver observer) {
        mDataSetObservable.registerObserver(observer);
    }

    public void unregisterDataSetObserver(DataSetObserver observer) {
        mDataSetObservable.unregisterObserver(observer);
    }

    /**
     * Notifies the attached observers that the underlying data has been changed
     * and any View reflecting the data set should refresh itself.
     */
    public void notifyDataSetChanged() {
        mDataSetObservable.notifyChanged();
    }

    /**
     * Notifies the attached observers that the underlying data is no longer valid
     * or available. Once invoked this adapter is no longer valid and should
     * not report further data set changes.
     */
    public void notifyDataSetInvalidated() {
        mDataSetObservable.notifyInvalidated();
    }

    @Override
    public int getCount() {
        return mFiles.size();
    }

    @Override
    public Object getItem(int position) {
        return mFiles.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = this.getLayoutInflater().inflate(R.layout.item_map, parent, false);
        } else {

        }
        File f = mFiles.get(position);
        ImageView icon = (ImageView) convertView.findViewById(R.id.icon);
        TextView name = (TextView) convertView.findViewById(R.id.name);
        TextView time = (TextView) convertView.findViewById(R.id.time);

        long lastTime = f.lastModified();
        icon.setImageBitmap(BitmapFactory.decodeFile(Prex+f.getName()));
        name.setText(f.getName().replaceAll(".jpg", ""));
        time.setText(df.format(new Date(lastTime)));


        View v = convertView.findViewById(R.id.btn_del);
        v.setTag("del-"+position);
        v.setOnClickListener(this);
        if (mIsNavi) v.setVisibility(View.GONE);
        v = convertView.findViewById(R.id.btn_edit);
        v.setTag("edit-"+position);
        v.setOnClickListener(this);
        if (mIsNavi) v.setVisibility(View.GONE);
        v = convertView.findViewById(R.id.btn_upload);
        v.setTag("upload-"+position);
        v.setOnClickListener(this);
        if (mIsNavi) v.setVisibility(View.GONE);
        v = convertView.findViewById(R.id.btn_nav);
        v.setTag("nav-"+position);
        v.setOnClickListener(this);
        if (!mIsNavi) v.setVisibility(View.GONE);


        return convertView;
    }

    @Override
    public int getItemViewType(int position) {
        return IGNORE_ITEM_VIEW_TYPE;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return mFiles.isEmpty();
    }

    @Override
    public boolean areAllItemsEnabled() {
        return true;
    }

    @Override
    public boolean isEnabled(int position) {
        return true;
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.name || id == R.id.time) {
            Collections.sort(mFiles, new FileSortComparator(id == R.id.name ? "name" : "time", !mSortAsc));
            updateSceneList();
            return;
        }
        Object o = v.getTag();
        if (o != null && (o+"").contains("-")) {
            String[] t = (o+"").split("-");
            File f = mFiles.get(Integer.parseInt(t[1]));
            if (t[0].equalsIgnoreCase("del")) {
                if (LrRobot.sendCmd(mIp, LrDefines.PORT_MAPS, 2019, f.getName().replaceAll(".jpg", ""))) {
                    mFiles.remove(Integer.parseInt(t[1]));
                    f.delete();
                    new File(f.getAbsolutePath().replaceAll(".jpg", ".pgm")).delete();
                    try {
                        new File(f.getAbsolutePath().replaceFirst("/maps/", "/navi/")).delete();
                        new File(f.getAbsolutePath().replaceFirst("/maps/", "/navi/").replaceAll(".jpg", ".pgm")).delete();
                    }catch (Throwable e) {}
                    notifyDataSetChanged();
                }
            }else if (t[0].equalsIgnoreCase("edit")) {
                String test = f.getAbsolutePath().replaceAll(".jpg", ".pgm");
                if (new File(test).exists()) {
                    Intent i = new Intent(this, LrActEditMap.class);
                    i.putExtra("map", test);
                    startActivity(i);
                }else{
                    LrToast.toast("地图不存在！");
                }
            }else if (t[0].equalsIgnoreCase("upload")) {
                final String mapPath = f.getAbsolutePath().replaceAll(".jpg", ".pgm");
                final String mapName = f.getName().replaceAll(".jpg", "");
                LoadingDailog.Builder loadBuilder = new LoadingDailog.Builder(this)
                        .setMessage("上传中...")
                        .setCancelable(false)
                        .setCancelOutside(false);
                final LoadingDailog dialog=loadBuilder.create();
                dialog.show();

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        final int result = LrNativeApi.sendEditMap(mapName, mapPath);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                dialog.dismiss();
                                if (result == 1) {
                                    LrToast.toast("上传成功！");
                                }else{
                                    LrToast.toast("上传失败！");
                                }
                            }
                        });
                    }
                }).start();
            }else if (t[0].equalsIgnoreCase("nav")) {
                    Intent i = new Intent(this, LrActNavi.class);
                    i.putExtra("map", f.getName().replaceAll(".jpg", ""));
                    i.putExtra("ip", mIp);
                    startActivity(i);
            }
        }
    }
	/*------------------------ End ------------------------*/
}
