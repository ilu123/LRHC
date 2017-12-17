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
import java.util.Date;

public class LrActAllMap extends Activity implements ListAdapter, View.OnClickListener {

    DragSortListView mListView;

    static final String Prex = "/mnt/sdcard/com.lrkj.ctrl/maps/";

    ArrayList<File> mFiles = new ArrayList<>();
    String mIp = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_all_maps);

        mIp = getIntent().getStringExtra("ip");

        mListView = (DragSortListView) findViewById(android.R.id.list);
        mListView.setAdapter(this);
        mListView.setDragEnabled(false);
        mListView.setRemoveListener(onMore);

        loadMapDatas();
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

    }

    public void onClickReload(View v) {
        loadMapDatas();
    }

    public void onClickDelAll(View v) {
        LrRobot.sendCmd(mIp, LrDefines.PORT_MAPS, 2020, null);
    }


    void updateSceneList() {
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                mFiles.clear();
                File folder = new File(Prex);
                File[] fileArray = folder.listFiles();
                for (File f : fileArray) {
                    if (f.isFile() && f.getName().contains(".jpg")) {
                        mFiles.add(f);
                    }
                }
                notifyDataSetChanged();
            }
        });
    }

    private void loadMapDatas() {
        LoadingDailog.Builder loadBuilder = new LoadingDailog.Builder(this)
                .setMessage("加载中...")
                .setCancelable(false)
                .setCancelOutside(false);
        final LoadingDailog dialog=loadBuilder.create();
        dialog.show();

        new Thread(new Runnable() {
            @Override
            public void run() {
                LrNativeApi.getAllMaps();
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
        v = convertView.findViewById(R.id.btn_edit);
        v.setTag("edit-"+position);
        v.setOnClickListener(this);
        v = convertView.findViewById(R.id.btn_upload);
        v.setTag("upload-"+position);
        v.setOnClickListener(this);
        v = convertView.findViewById(R.id.btn_nav);
        v.setTag("nav-"+position);
        v.setOnClickListener(this);


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
        Object o = v.getTag();
        if (o != null && (o+"").contains("-")) {
            String[] t = (o+"").split("-");
            if (t[0].equalsIgnoreCase("del")) {
                File f = mFiles.get(Integer.parseInt(t[1]));
                LrRobot.sendCmd(mIp, LrDefines.PORT_MAPS, 2019, f.getName().replaceAll(".jpg", ""));
                mFiles.remove(Integer.parseInt(t[1]));
                f.delete();
                notifyDataSetChanged();
            }else if (t[0].equalsIgnoreCase("edit")) {
                String test = Prex + "/test.pgm";
                Intent i = new Intent(this, LrActEditMap.class);
                i.putExtra("map", test);
                startActivity(i);

            }else if (t[0].equalsIgnoreCase("upload")) {

            }else if (t[0].equalsIgnoreCase("nav")) {
                File f = mFiles.get(Integer.parseInt(t[1]));
                if (LrRobot.getRobot(mIp).sendCommand(LrDefines.Cmds.CMD_NAVI_START, f.getName().replaceAll(".jpg", ""))) {
                    Intent i = new Intent(this, LrActNavi.class);
                    i.putExtra("map", f.getName().replaceAll(".jpg", ""));
                    i.putExtra("ip", mIp);
                    startActivity(i);
                }else{
                    LrToast.toast("无法获取地图信息", LrApplication.sApplication);
                }
            }
        }
    }
	/*------------------------ End ------------------------*/
}
