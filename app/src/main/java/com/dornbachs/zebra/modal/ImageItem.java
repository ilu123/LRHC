/**
 * TODO
 * By ztb, 2016-11-23
 */
package com.dornbachs.zebra.modal;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * TODO
 * 
 * @author ztb, 2016-11-23
 */
public class ImageItem implements Parcelable {

	public boolean isDrawable = false;
	public int outlineId = -1;
	public int thumbId = -1;
	public String outlinePath = null;
	public String thumbPath = null;

	/*
	 * (non-Javadoc)
	 * @see android.os.Parcelable#describeContents()
	 */
	@Override
	public int describeContents() {
		// TODO Auto-generated method stub
		return 0;
	}

	/*
	 * (non-Javadoc)
	 * @see android.os.Parcelable#writeToParcel(android.os.Parcel, int)
	 */
	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt((isDrawable ? 1 : 0));
		dest.writeInt(outlineId);
		dest.writeInt(thumbId);
		dest.writeString(outlinePath);
		dest.writeString(thumbPath);
	}

	public static final Parcelable.Creator<ImageItem> CREATOR = new Parcelable.Creator<ImageItem>() {
		public ImageItem createFromParcel(Parcel in) {
			return new ImageItem(in);
		}

		public ImageItem[] newArray(int size) {
			return new ImageItem[size];
		}
	};

	private ImageItem(Parcel in) {
		isDrawable = in.readInt() == 1;
		outlineId = in.readInt();
		thumbId = in.readInt();
		outlinePath = in.readString();
		thumbPath = in.readString();
	}

	/**
	 * 
	 */
    public ImageItem() {
	    // TODO Auto-generated constructor stub
    }
}
