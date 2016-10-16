package com.nfc.test;

import android.app.Application;
import android.content.Context;
/**
 * 自定义application
 * @author wsd_leiguoqiang
 */
public class MyApplication extends Application{
	private static Context context;
	@Override
	public void onCreate() {
		super.onCreate();
		context = this;
	}
	public static Context getContext() {
		return context;
	}
}
