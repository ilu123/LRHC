/*
 * Copyright (C) 2016 TomkidGame
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dornbachs.zebra.utils;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;

public final class DrawUtils {
	// Make dest a resized copy of src. This maintains the aspect ratio. and cuts
	// along edges of the image if src and dest have a different aspect ratio.
	public static void convertSizeClip(Bitmap src, Bitmap dest) {
		Canvas canvas = new Canvas(dest);
		RectF srcRect = new RectF(0, 0, src.getWidth(), src.getHeight());
		RectF destRect = new RectF(0, 0, dest.getWidth(), dest.getHeight());

		// Because the current SDK does not directly support the "dest fits
		// inside src" mode, we calculate the reverse matrix and invert to
		// get what we want.
		Matrix mDestSrc = new Matrix();
		mDestSrc.setRectToRect(destRect, srcRect, Matrix.ScaleToFit.CENTER);
		Matrix mSrcDest = new Matrix();
		mDestSrc.invert(mSrcDest);

		canvas.drawBitmap(src, mSrcDest, new Paint(Paint.DITHER_FLAG));
	}

	/**
	 * 将bitmap中的某种颜色值替换成新的颜色
	 * @param
	 * @param oldColor
	 * @param newColor
	 * @return
	 */
	public static Bitmap replaceBitmapColor(Bitmap oldBitmap,int oldColor,int newColor)
	{
		Bitmap mBitmap = oldBitmap.copy(Bitmap.Config.ARGB_8888, true);
		//循环获得bitmap所有像素点
		int mBitmapWidth = mBitmap.getWidth();
		int mBitmapHeight = mBitmap.getHeight();
		int mArrayColorLengh = mBitmapWidth * mBitmapHeight;
		int[] mArrayColor = new int[mArrayColorLengh];
		int count = 0;
		for (int i = 0; i < mBitmapHeight; i++) {
			for (int j = 0; j < mBitmapWidth; j++) {
				//获得Bitmap 图片中每一个点的color颜色值
				//将需要填充的颜色值如果不是
				//在这说明一下 如果color 是全透明 或者全黑 返回值为 0
				//getPixel()不带透明通道 getPixel32()才带透明部分 所以全透明是0x00000000
				//而不透明黑色是0xFF000000 如果不计算透明部分就都是0了
				int color = mBitmap.getPixel(j, i) & 0x00FFFFFF;
				//将颜色值存在一个数组中 方便后面修改
				if (color < 0xDDDDDD && color > 0x555555) { //这里范围防止毛边
					mBitmap.setPixel(j, i, newColor);  //将白色替换成透明色
				}

			}
		}
		return mBitmap;
	}
}
