package com;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;

import com.lrkj.ctrl.R;
import com.lrkj.widget.KnobView;

/**
 * Created by tianbao.zhao on 2017/12/28.
 */

public class Test extends Activity {
    KnobView croller;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.test);

        croller = (KnobView) findViewById(R.id.croller);
//        croller.setIndicatorWidth(10);
//        croller.setBackCircleColor(Color.parseColor("#EDEDED"));
//        croller.setMainCircleColor(Color.WHITE);
//        croller.setMax(50);
//        croller.setStartOffset(45);
//        croller.setIsContinuous(false);
//        croller.setLabelColor(Color.BLACK);
//        croller.setProgressPrimaryColor(Color.parseColor("#0B3C49"));
//        croller.setIndicatorColor(Color.parseColor("#0B3C49"));
//        croller.setProgressSecondaryColor(Color.parseColor("#EEEEEE"));
//        croller.setProgressRadius(380);
//        croller.setBackCircleRadius(300);

        croller.setOnCrollerChangeListener(new KnobView.OnCrollerChangeListener() {
            @Override
            public void onProgressChanged(KnobView croller, int progress) {

            }

            @Override
            public void onStartTrackingTouch(KnobView croller) {
                Toast.makeText(Test.this, "Start", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onStopTrackingTouch(KnobView croller) {
                Toast.makeText(Test.this, "Stop", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
