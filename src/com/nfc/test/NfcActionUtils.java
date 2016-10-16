package com.nfc.test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.widget.Toast;
/**
 * nfc工具动作工具类，利用nfc标签进行自动开发浏览器和应用程序
 * 这里主要以Ndef格式进行数据的写入
 * @author wsd_leiguoqiang
 */
@SuppressLint("NewApi") 
public class NfcActionUtils {
	private static final boolean PackageInfo = false;
	/**
	 * context对象
	 */
	private Context context = MyApplication.getContext();
	/**
	 * 自定义方法，以Ndef数据格式，将uri资源写入nfc标签
	 * 这里只做Ndef格式进行写入操作
	 * 前提条件：前面代码已经进行了nfc标签是否支持Ndef格式的判断
	 * 当向nfc标签写入uri数据之后，nfc设备再次检测到nfc标签时，nfc设备会自动打开里面的uri网址
	 * @param uri：网络资源标识符
	 * @param tag:nfc标签标签描述对象
	 */
	@SuppressLint("NewApi") 
	private boolean writeUriToTag(Tag tag,String uri){
		//获取Ndef数据格式对象
		Ndef ndef = Ndef.get(tag);
		//说明该nfc标签支持ndef数据格式
		if(ndef!=null){
			//进行ndef对象的连接
			try {
				ndef.connect();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			//获取NdefRecord对象,将uri资源封装到该对象中
			NdefRecord record = NdefRecord.createUri(Uri.parse(uri));
			//获取NdefMessage对象,将ndefrecord对象封装进去
			NdefMessage message = new NdefMessage(new NdefRecord[]{record});
			//获取ndefmessage对象的内容字节长度
			int size = message.toByteArray().length;
			//判断写入内容长度与ndc标签最大内容长度的大小
			if(ndef.getMaxSize()<size){
				showToast("内容过大，无法写入nfc标签");
				return false;
			}
			//利用Ndef对象进行外部uri数据写入nfc标签
			try {
				ndef.writeNdefMessage(message);
				showToast("向nfc标签中写入uri数据成功");
				return true;
			} catch (IOException e) {
				e.printStackTrace();
			} catch (FormatException e) {
				e.printStackTrace();
			}finally{
				try {
					ndef.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			//该nfc标签没有进行格式化，进行nfc格式化操作
		}else{
			//获取ndefformatble对象
			NdefFormatable format = NdefFormatable.get(tag);
			try {
				//建立连接
				format.connect();
				//获取NdefRecord对象,将uri资源封装到该对象中
				NdefRecord record = NdefRecord.createUri(Uri.parse(uri));
				//获取NdefMessage对象,将ndefrecord对象封装进去
				NdefMessage message = new NdefMessage(new NdefRecord[]{record});
				//进行格式化并进行Ndef数据写入
				format.format(message);
				showToast("格式化nfc标签和写入Ndef数据成功");
				return true;
			} catch (IOException e) {
				e.printStackTrace();
			} catch (FormatException e) {
				e.printStackTrace();
				showToast("格式化nfc标签失败");
			}finally{
				try {
					format.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return false;
	}
	/**
	 * 自定义方法：以Ndef格式进行app名称的写入
	 * 实现感应nfc标签自动运行app
	 * 应用场景：上班模式，回家模式，当第二次接触到该nfc标签的时候就自动打开响应的app程序
	 * @param tag：nfc标签信息对象
	 * @ packageName:程序包名
	 */
	private void writeAppNameToTag(Tag tag,String packageName){
		if(tag==null){
			return;
		}
		NdefMessage message = new NdefMessage(new NdefRecord[]{NdefRecord.createApplicationRecord(packageName)});
		Ndef ndef = Ndef.get(tag);
		//因为nfc标签的格式没有做支持判断，故在此作非空判断取代，能够拿到格式对象，说明支持，不能拿到则是不支持，或是nfc还没有格式化
		try {
			if(ndef!=null){
				ndef.connect();
				//判断nfc标签是否可写入数据
				if(!ndef.isWritable()){
					Toast.makeText(MyApplication.getContext(), "nfc标签没有写入权限", Toast.LENGTH_SHORT).show();
					return;
				}
				//判断nfc标签容量
				int size = message.toByteArray().length;
				if(ndef.getMaxSize()<size){
					Toast.makeText(MyApplication.getContext(), "nfc标签数据容量不够", Toast.LENGTH_SHORT).show();
					return;
				}
				//向nfc标签中写入数据
				ndef.writeNdefMessage(message);
				Toast.makeText(MyApplication.getContext(), "nfc标签写入数据成功", Toast.LENGTH_SHORT).show();
				//nfc标签还没有被格式化
			}else{
				try {
					//创建格式化对象NdefFormateble
					NdefFormatable formate = NdefFormatable.get(tag);
					formate.connect();
					formate.format(message);
					Toast.makeText(MyApplication.getContext(), "格式化nfc标签并写入数据成功", Toast.LENGTH_SHORT).show();
				} catch (Exception e) {
					Toast.makeText(MyApplication.getContext(), "格式化nfc标签失败", Toast.LENGTH_SHORT).show();
				}
			}
		} catch (Exception e) {
			Toast.makeText(MyApplication.getContext(), "nfc标签不支持Ndef格式", Toast.LENGTH_SHORT).show();
		}
	}
	/**
	 * 获取系统里面所有程序的包名
	 * 配合利用nfc标签进行自动开启程序使用
	 * @return
	 */
	public List<String> getPackageNameList(Activity activity){
		List<String> package_list = new ArrayList<String>();
		//获取程序包管理对象
		PackageManager manager = activity.getPackageManager();
		//获取所有包名信息集合
		List<PackageInfo> packageInfos = manager.getInstalledPackages(PackageManager.GET_ACTIVITIES);
		//遍历集合，封装包名集合
		for(PackageInfo package_info:packageInfos){
			package_list.add(package_info.applicationInfo.loadLabel(manager)+"/n"+package_info.packageName);
		}
		return package_list;
	}
	/**
	 * 自定义方法，toast信息的提示
	 * @param str
	 */
	private void showToast(String str) {
		Toast.makeText(context, str, Toast.LENGTH_LONG).show();
	}
}
