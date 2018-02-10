package com.lrkj.views;

import android.content.Intent;
import android.database.DataSetObservable;
import android.database.DataSetObserver;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.android.tu.loadingdialog.LoadingDailog;
import com.lrkj.LrApplication;
import com.lrkj.business.LrNativeApi;
import com.lrkj.business.LrRobot;
import com.lrkj.ctrl.R;
import com.lrkj.defines.LrDefines;
import com.lrkj.utils.LrToast;
import com.lrkj.widget.MyRxDialog;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;

import io.reactivex.functions.Consumer;

public class LrActAllMap extends LrBaseAct implements ListAdapter, View.OnClickListener {

    ListView mListView;

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
        ((TextView) findViewById(R.id.tvTitle)).setText(mIsNavi ? "地图导航" : "地图管理");
        mListView = (ListView) findViewById(android.R.id.list);
        mListView.setAdapter(this);

        if (mIsNavi) {
            mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    final File f = mFiles.get(position);
                    new MyRxDialog(LrActAllMap.this)
                            .setTitle("提示")
                            .setMessage("在此地图导航？")
                            .setPositiveText("确定")
                            .setNegativeText("取消")
                            .dialogToObservable()
                            .subscribe(new Consumer<Integer>() {
                                @Override
                                public void accept(Integer integer) throws Exception {
                                    switch (integer) {
                                        case MyRxDialog.POSITIVE:
                                            Intent i = new Intent(LrActAllMap.this, LrActNavi.class);
                                            i.putExtra("map", f.getName().replaceAll(".jpg", ""));
                                            i.putExtra("ip", mIp);
                                            startActivity(i);
                                            break;
                                    }
                                }
                            });
                }
            });
            loadMapDatas();
        } else {
            mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    final int pos = position;
                    final File f = mFiles.get(position);
                    new MyRxDialog(LrActAllMap.this)
                            .setTitle("选择操作")
                            .setNegativeText("编辑")
                            .setNeutralText("删除地图")
                            .dialogToObservable()
                            .subscribe(new Consumer<Integer>() {
                                @Override
                                public void accept(Integer integer) throws Exception {
                                    switch (integer) {
                                        case MyRxDialog.NEUTRAL:
                                            deleteMap(f, pos);
                                            break;
                                        case MyRxDialog.NEGATIVE:
                                            String test = f.getAbsolutePath().replaceAll(".jpg", ".pgm");
                                            if (new File(test).exists()) {
                                                Intent i = new Intent(LrActAllMap.this, LrActEditMap.class);
                                                i.putExtra("map", test);
                                                startActivity(i);
                                            } else {
                                                LrToast.toast("地图不存在！");
                                            }
                                            break;
                                        case MyRxDialog.POSITIVE:
                                            final String mapPath = f.getAbsolutePath().replaceAll(".jpg", ".pgm");
                                            final String mapName = f.getName().replaceAll(".jpg", "");
                                            LoadingDailog.Builder loadBuilder = new LoadingDailog.Builder(LrActAllMap.this)
                                                    .setMessage("上传中...")
                                                    .setCancelable(false)
                                                    .setCancelOutside(false);
                                            final LoadingDailog dialog = loadBuilder.create();
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
                                                            } else {
                                                                LrToast.toast("上传失败！");
                                                            }
                                                        }
                                                    });
                                                }
                                            }).start();
                                            break;
                                    }
                                }
                            });
                }
            });
            updateSceneList();
        }
    }

    @Override
    public void onPause() {
        super.onPause();

    }

    @Override
    public void onResume() {
        super.onResume();
        LrApplication.mkMapFolder();

        if (!mIsNavi) {
            updateSceneList();
        }
    }

    public void onClickReload(View v) {
        loadMapDatas();
    }

    public void onClickDelAll(View v) {
        new MyRxDialog(this)
                .setTitle("提示")
                .setMessage("确定删除所有地图吗？")
                .setPositiveText("确定")
                .setNegativeText("取消")
                .dialogToObservable()
                .subscribe(new Consumer<Integer>() {
                    @Override
                    public void accept(Integer integer) throws Exception {
                        switch (integer) {
                            case MyRxDialog.POSITIVE:
                                if (LrRobot.sendCmd(mIp, LrDefines.PORT_MAPS, 2020, null)) {
                                    File folder = new File(Prex);
                                    File[] fileArray = folder.listFiles();
                                    if (fileArray != null)
                                        for (File f : fileArray) {
                                            f.delete();
                                        }
                                    updateSceneList();
                                }else{
                                    LrToast.toast("地图删除命令失败！");
                                }
                                break;
                        }
                    }
                });
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

        public FileSortComparator(String type, boolean asc) {
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
            } else if ("time".equalsIgnoreCase(mT)) {
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
        final LoadingDailog dialog = loadBuilder.create();
        dialog.show();

        new Thread(new Runnable() {
            @Override
            public void run() {
                File folder = new File(Prex);
                File[] fileArray = folder.listFiles();
                if (fileArray != null)
                    for (File f : fileArray) {
                        f.delete();
                    }
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
        icon.setImageBitmap(BitmapFactory.decodeFile(Prex + f.getName()));
        name.setText(f.getName().replaceAll(".jpg", ""));
        time.setText(df.format(new Date(lastTime)));


        View v = convertView.findViewById(R.id.btn_del);
        v.setTag("del-" + position);
        v.setOnClickListener(this);
        if (mIsNavi) v.setVisibility(View.GONE);
        v = convertView.findViewById(R.id.btn_edit);
        v.setTag("edit-" + position);
        v.setOnClickListener(this);
        if (mIsNavi) v.setVisibility(View.GONE);
        v = convertView.findViewById(R.id.btn_upload);
        v.setTag("upload-" + position);
        v.setOnClickListener(this);
        if (mIsNavi) v.setVisibility(View.GONE);
        v = convertView.findViewById(R.id.btn_nav);
        v.setTag("nav-" + position);
        v.setOnClickListener(this);
        v.setVisibility(View.GONE);


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
            mSortAsc = !mSortAsc;
            Collections.sort(mFiles, new FileSortComparator(id == R.id.name ? "name" : "time", mSortAsc));
            notifyDataSetChanged();
            return;
        }
        Object o = v.getTag();
        if (o != null && (o + "").contains("-")) {
            final String[] t = (o + "").split("-");
            final File f = mFiles.get(Integer.parseInt(t[1]));
            if (t[0].equalsIgnoreCase("del")) {
                this.deleteMap(f, Integer.parseInt(t[1]));
            } else if (t[0].equalsIgnoreCase("edit")) {
                String test = f.getAbsolutePath().replaceAll(".jpg", ".pgm");
                if (new File(test).exists()) {
                    Intent i = new Intent(this, LrActEditMap.class);
                    i.putExtra("map", test);
                    startActivity(i);
                } else {
                    LrToast.toast("地图不存在！");
                }
            } else if (t[0].equalsIgnoreCase("upload")) {

            } else if (t[0].equalsIgnoreCase("nav")) {
                Intent i = new Intent(this, LrActNavi.class);
                i.putExtra("map", f.getName().replaceAll(".jpg", ""));
                i.putExtra("ip", mIp);
                startActivity(i);
            }
        }
    }

    private void deleteMap(final File f, final int pos){
        new MyRxDialog(this)
                .setTitle("提示")
                .setMessage("确定删除地图吗？")
                .setPositiveText("确定")
                .setNegativeText("取消")
                .dialogToObservable()
                .subscribe(new Consumer<Integer>() {
                    @Override
                    public void accept(Integer integer) throws Exception {
                        switch (integer) {
                            case MyRxDialog.POSITIVE:
                                if (LrRobot.sendCmd(mIp, LrDefines.PORT_MAPS, 2019, f.getName().replaceAll(".jpg", ""))) {
                                    mFiles.remove(pos);
                                    f.delete();
                                    new File(f.getAbsolutePath().replaceAll(".jpg", ".pgm")).delete();
                                    try {
                                        new File(f.getAbsolutePath().replaceFirst("/maps/", "/navi/")).delete();
                                        new File(f.getAbsolutePath().replaceFirst("/maps/", "/navi/").replaceAll(".jpg", ".pgm")).delete();
                                    } catch (Throwable e) {
                                    }
                                    notifyDataSetChanged();
                                }else{
                                    LrToast.toast("地图删除命令失败！");
                                }
                                break;
                        }
                    }
                });
    }
    /*------------------------ End ------------------------*/
}
