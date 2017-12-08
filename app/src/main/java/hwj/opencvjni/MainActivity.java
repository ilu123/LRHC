package hwj.opencvjni;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

public class MainActivity extends AppCompatActivity {

    private ImageView img = null;
    private Button bt_photo = null;

    private ImageView img2 = null;
    private Button bt_Gray2 = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView tv = (TextView) findViewById(R.id.sample_text);
        tv.setText(OpenCVHelper.getStringTmp());

        img = (ImageView)findViewById(R.id.img);
        img2 = (ImageView)findViewById(R.id.img2);

        bt_photo = (Button) findViewById(R.id.bt_Gray);
        bt_photo.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub


                Bitmap bitmap = ((BitmapDrawable) getResources().getDrawable(
                        R.drawable.boy)).getBitmap();

                int w = bitmap.getWidth(), h = bitmap.getHeight();
                int[] pix = new int[w * h];
                bitmap.getPixels(pix, 0, w, 0, 0, w, h);
                int[] resultPixes = OpenCVHelper.getGrayImage(pix,w,h);
                Bitmap result = Bitmap.createBitmap(w,h, Bitmap.Config.RGB_565);
                result.setPixels(resultPixes, 0, w, 0, 0,w, h);
                img.setImageBitmap(result);
            }
        });

        bt_Gray2 = (Button) findViewById(R.id.bt_Gray2);
        bt_Gray2.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                OpenCVLoader.initDebug();
                Mat rgbMat = new Mat();
                Mat grayMat = new Mat();
                Bitmap srcBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.xiao_wu);
                Bitmap grayBitmap = Bitmap.createBitmap(srcBitmap.getWidth(), srcBitmap.getHeight(), Bitmap.Config.RGB_565);
                Utils.bitmapToMat(srcBitmap, rgbMat);//convert original bitmap to Mat, R G B.
                Imgproc.cvtColor(rgbMat, grayMat, Imgproc.COLOR_RGB2GRAY);//rgbMat to gray grayMat
                Utils.matToBitmap(grayMat, grayBitmap); //convert mat to bitmap
                img2.setImageBitmap(grayBitmap);
            }
        });

    }
}
