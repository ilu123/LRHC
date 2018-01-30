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

}
