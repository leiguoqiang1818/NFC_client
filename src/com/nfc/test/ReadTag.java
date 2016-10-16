package com.nfc.test;

import java.io.UnsupportedEncodingException;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.tech.MifareClassic;
import android.nfc.tech.NfcA;
import android.nfc.tech.NfcB;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

/**
 * NFC读取数据activity，读取nfc标签中的数据
 * 这里以google推荐的数据格式Ndef作为测试
 */
@SuppressLint("NewApi")
public class ReadTag extends Activity {
	/**
	 * NfcAdapter对象，适配器
	 * 当设备检测到nfc标签时候，进行封装intent操作
	 */
	private NfcAdapter nfcAdapter;
	/**
	 * 显示文本
	 */
	private TextView resultText;
	/**
	 * 预处理pendingIntent对象，共其他程序调用内部的intent对象
	 */
	private PendingIntent pendingIntent;
	/**
	 * activity过滤器数组
	 */
	private IntentFilter[] mFilters;
	/**
	 * 二维数组，封装nfc标签数据格式名称
	 */
	private String[][] mTechLists;
	/**
	 * 进入nfc标签写入数据模式
	 */
	private Button mJumpTagBtn;
	/**
	 * 标记首处理nfc数据
	 */
	private boolean isFirst = true;

	@SuppressLint("NewApi")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.read_tag);
		//判断设备是否支持nfc功能
		nfcEnable();
		//初始化数据
		init();
	}
	
	/**
	 * 自定义方法，初始化数据
	 */
	private void init() {
		// 显示结果Text
		resultText = (TextView) findViewById(R.id.resultText);
		// 写入标签按钮
		mJumpTagBtn = (Button) findViewById(R.id.jump);
		mJumpTagBtn.setOnClickListener(new WriteBtnOnClick());

		//pendingIntent对象，用于被androidNFC机制进行远程调用pendingIntent对象封装好的intent对象
		pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this,
				getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
		IntentFilter ndef = new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED);
		ndef.addCategory("*/*");
		mFilters = new IntentFilter[] { ndef };// 过滤器
		mTechLists = new String[][] {
				new String[] { MifareClassic.class.getName() },
				new String[] { NfcA.class.getName() },
				new String[] { NfcB.class.getName() }};// 允许扫描的标签类型
	}
	/**
	 * 自定义方法，检查设备是否支持nfc功能
	 */
	private void nfcEnable() {
		// 获取nfc适配器，判断设备是否支持NFC功能
		nfcAdapter = NfcAdapter.getDefaultAdapter(this);
		if (nfcAdapter == null) {
			Toast.makeText(this, getResources().getString(R.string.no_nfc),
					Toast.LENGTH_SHORT).show();
			finish();
			return;
		} else if (!nfcAdapter.isEnabled()) {
			Toast.makeText(this, getResources().getString(R.string.open_nfc),
					Toast.LENGTH_SHORT).show();
			finish();
			return;
		}
	}

	@SuppressLint("NewApi")
	@Override
	protected void onResume() {
		super.onResume();
		// 前台分发系统,这里的作用在于第二次检测NFC标签时该应用有最高的捕获优先权.
		nfcAdapter.enableForegroundDispatch(this, pendingIntent, mFilters,
				mTechLists);
		//当程序第一次运行时候，运行此段代码，当再次检测到nfc标签的时候，在onNewInent()生命周期中读取nfc标签中数据
		if (isFirst) {
			if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(getIntent().getAction())) {
				String result = processIntent(getIntent());
				if(TextUtils.isEmpty(result)){
					//提示nfc标签中数据为空
					resultText.setText("NFC标签中数据为空 ...");
				}else{
					//显示nfc标签中的数据
					resultText.setText(result);
				}
			}
			isFirst = false;
		}
	}
	/**
	 * 重写activity生命周期方法，当nfc设备再次读取到nfc标签的时候，在此方法里面进行数据的读取
	 * 此方法只在当前activity有新的intent对象时，才会被调用
	 */
	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(intent.getAction())) {
			String result = processIntent(intent);
			if(TextUtils.isEmpty(result)){
				//提示nfc标签中数据为空
				resultText.setText("NFC标签中数据为空 ...");
			}else{
				//显示nfc标签中的数据
				resultText.setText(result);
			}
		}
	}

	/**
	 * 获取tag标签中的内容
	 * @param intent:android系统nfc机制自动封装好的intent对象，里面包含封装了nfc标签数据的的Tag对象
	 */
	@SuppressLint("NewApi")
	private String processIntent(Intent intent) {
		Parcelable[] rawmsgs = intent
				.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
		//ndefMessage对象是个数据容器，里面封装了NdefRecord数据对象
		NdefMessage msg = (NdefMessage) rawmsgs[0];
		NdefRecord[] records = msg.getRecords();
		//从自定义封装类中获取NdefTextRecord对象
		NdefTextRecord ndefTextRecord = NdefTextRecord.parse(records[0]);
		//创建存储文本对象
		String resultStr = null;
		//通过非空判断，进行nfc标签中正常文本的获取
		if(ndefTextRecord!=null){
			resultStr = ndefTextRecord.getText();
			return resultStr;
		}
		return null;
	}

	/**
	 * 按钮点击事件，测试使用，进行nfc写入数据操作
	 */
	class WriteBtnOnClick implements OnClickListener {
		@Override
		public void onClick(View v) {
			switch (v.getId()) {
			case R.id.jump:
				Intent intent = new Intent(ReadTag.this, WriteTag.class);
				startActivity(intent);
			default:
				break;
			}
		}
	}
}
