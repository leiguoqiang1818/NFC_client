package com.nfc.test;

import java.io.IOException;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.MifareClassic;
import android.nfc.tech.MifareUltralight;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.nfc.tech.NfcA;
import android.nfc.tech.NfcB;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

/**
 * 将外部数据写入nfc标签的activity
 * alertdialog进行写入模式确认
 * 这里侧重google官方推荐的nfc数据格式Ndef进行测试
 * @author wsd_leiguoqiang
 */
@SuppressLint("NewApi")
public class WriteTag extends Activity {
	/**
	 * activity过滤器数组
	 */
	private IntentFilter[] mWriteTagFilters;
	/**
	 * nfc适配器对象
	 */
	private NfcAdapter nfcAdapter;
	/**
	 * 外部调用的pendingIntent对象，被android NFC机制调用，进行该页面的开启
	 */
	private PendingIntent pendingIntent;
	/**
	 * nfc数据格式二位数组，定义该程序允许扫描的nfc标签数据格式
	 */
	private String[][] mTechLists;
	/**
	 * 测试写入模式确认按钮
	 */
	private Button writeBtn;
	/**
	 * 写入模式标记变量
	 */
	private boolean isWrite = false;
	/**
	 * 外部数据输入框对象
	 */
	private EditText mContentEditText;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.write_tag);
		//判断是否支持nfc功能
		nfcEnable();
		init();
	}
	/**
	 * 自定义方法，初始化数据
	 */
	private void init() {
		writeBtn = (Button) findViewById(R.id.writeBtn);
		writeBtn.setOnClickListener(new WriteOnClick());
		mContentEditText = (EditText) findViewById(R.id.content_edit);
		pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this,
				getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
		// 写入标签权限
		IntentFilter writeFilter = new IntentFilter(
				NfcAdapter.ACTION_TECH_DISCOVERED);
		mWriteTagFilters = new IntentFilter[] { writeFilter };
		//定义允许扫描的nfc标签数据类型
		mTechLists = new String[][] {
				new String[] { MifareClassic.class.getName() },
				new String[] { NfcA.class.getName() },
				new String[] { Ndef.class.getName() },
				new String[] { NfcB.class.getName() }};
	}
	/**
	 * 自定义方法，判断设备是否支持nfc标签
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
	/**
	 * 进行写入数据模式的开启
	 */
	class WriteOnClick implements OnClickListener {
		@Override
		public void onClick(View v) {
			//创建原生alertdialog，确定和取消按钮
			AlertDialog.Builder builder = new AlertDialog.Builder(WriteTag.this)
			.setTitle("请将标签靠近！");
			builder.setNegativeButton("确定",
					new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
					mContentEditText.setText("");
					isWrite = true;
				}
			});
			builder.setPositiveButton("取消",
					new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
					isWrite = false;
				}
			});
			builder.create();
			builder.show();
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		//当前页面显示的时候，把该窗口放置于task中最顶端，获取处理nfc标签的最高优先权
		nfcAdapter.enableForegroundDispatch(this, pendingIntent,
				mWriteTagFilters, mTechLists);
	}

	@Override
	protected void onPause() {
		super.onPause();
		//取消当前activity的捕获nfc标签的最高优先权
		nfcAdapter.disableForegroundDispatch(this);
	}
	/**
	 * 重写自定义方法，当再次检测到nfc标签的时候调用
	 */
	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		if (isWrite == true
				&& NfcAdapter.ACTION_TECH_DISCOVERED.equals(intent.getAction())) {
			Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
			//自定义一个标记变量，nfc标签是否包含Ndef数据格式
			boolean flag_Ndef = false;
			//在进行Ndef数据格式写入之前，进行nfc标签判断是否支持Ndef格式
			String[] tech = tag.getTechList();
			//遍历该String数组，进行是否数组包含Ndef格式
			for(String type:tech){
				//通过是否有包含的字符串下标，进行是否有次数个格式，这里进行Ndef格式的判断
				if(type.indexOf("Ndef")>=0){
					flag_Ndef = true;
					break;
				}
			}
			//标记变量为ture时候，说明该nfc标签支持Ndef数据格式
			if(flag_Ndef){
				//通过自定义方法，进行NdefMessage对象的获取
				NdefMessage ndefMessage = getNoteAsNdef();
				if (ndefMessage != null) {
					writeTag(getNoteAsNdef(), tag);
				} else {
					showToast("请输入您要写入标签的内容");
				}
			}else{
				showToast("该nfc标签不支持Ndef数据格式");
			}
		}
	}
	/**
	 * 自定义方法，根据外部数据封装一个NdefMessage对象，用于写入数据的容器
	 * @return NdefMessage对象
	 */
	private NdefMessage getNoteAsNdef() {
		String text = mContentEditText.getText().toString();
		if (text.equals("")) {
			return null;
		} else {
			byte[] textBytes = text.getBytes();
			// image/jpeg text/plain
			NdefRecord textRecord = new NdefRecord(NdefRecord.TNF_MIME_MEDIA,
					"image/jpeg".getBytes(), new byte[] {}, textBytes);
			return new NdefMessage(new NdefRecord[] { textRecord });
		}
	}

	/**
	 * 自定义方法，将自定义数据写入nfc标签中
	 * @param message：NdefMessage对象，封装写入数据的容器
	 * @param tag：nfc标签对象，由android系统nfc机制检测到nfc标签时候，自动生成，并且封装在intent对象中
	 * @return boolean类型变量，返回是否成功写入数据
	 */
	private boolean writeTag(NdefMessage message, Tag tag) {
		/**
		 * 思路：
		 * 1）判断需要写入的数据NdefMessage对象的字节长度与nfc标签的数据长度，在nfc标签数据长度范围内才能成功写入数据
		 * 2）根据是否有返回的数据格式对象，进行判断该nfc标签是否已经格式化，如果没有，则需要进行nfc标签数据格式化
		 * android官方推荐使用Ndef格式
		 * 3）以上两步骤完成，进行外部数据写入nfc标签
		 */

		//获取需要写入数据的字节长度
		int size = message.toByteArray().length;

		//android系统下，默认以ndef数据格式进行数据写入
		try {
			//创建数据格式对象
			Ndef ndef = Ndef.get(tag);
			if (ndef != null) {
				//数据格式对象进行连接，往nfc标签中写入数据，必须连接
				ndef.connect();
				//判断nfc标签是否具有写入权限
				if (!ndef.isWritable()) {
					showToast("nfc标签不允许写入数据");
					return false;
				}
				//判断写入数据的长度和nfc标签最大数据长度的大小关系
				if (ndef.getMaxSize() < size) {
					showToast("文件大小超出nfc标签容量");
					return false;
				}
				//往nfc标签写入数据
				ndef.writeNdefMessage(message);
				showToast("写入数据成功.");
				return true;

				/**
				 * 获取ndef数据格式对象失败，有两种情况：
				 * 1）nfc标签不支持Ndef格式；
				 * 2）nfc标签还没有被格式化
				 * 如果获取Ndef数据格式对象为null值，说明该nfc标签还没有进行数据格式化
				 * 前面对该nfc标签是否支持ndef格式进行了判断，故这里只做nfc标签还没有被格式化处理
				 */
			} else {
				//android系统，默认进行Ndef数据格式化，获取格式化对象
				NdefFormatable format = NdefFormatable.get(tag);
				//判断该nfc标签时候能够被格式化成Ndef格式，或者是有些nfc标签只能进行读取的操作，也无法格式化
				if (format != null) {
					try {
						format.connect();
						//格式化对象进行nfc标签格式化操作，并将数据写入nfc标签中
						format.format(message);
						showToast("格式化nfc标签并且写入数据成功");
						return true;
					} catch (IOException e) {
						showToast("格式化nfc标签失败");
						return false;
					}
					//该nfc标签不能被格式化成Ndef格式，或者该nfc标签只有读取数据权限，没有写入权限
				} else {
					showToast("该nfc标签不支持Ndef格式化或写入数据权限");
					return false;
				}
			}

			//如果出现异常，该nfc标签不能正常写入数据
		} catch (Exception e) {
			showToast("写入数据失败");
		}
		return false;
	}
	private void showToast(String text) {
		Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
	}
}

