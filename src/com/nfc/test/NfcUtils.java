package com.nfc.test;

import java.io.IOException;
import java.nio.charset.Charset;

import android.content.Context;
import android.nfc.Tag;
import android.nfc.tech.MifareClassic;
import android.nfc.tech.MifareUltralight;
import android.widget.Toast;

/**
 * 一个工具类，提供各种nfc标签数据格式的读写方法
 * @author wsd_leilguoqiang
 */
public class NfcUtils {
	private Context context = MyApplication.getContext();
	/**
	 * 自定义方法，进行nfc标签MifareClassic数据的读取操作
	 * @param tag
	 */
	public String readTagForMifareClassic(Tag tag){
		//mifareClassic对象
	    MifareClassic mfc = MifareClassic.get(tag);
	    //标记变量，该分区是否验证成功
	    boolean auth = false;
	    //字符串封装对象
	    String metaInfo = "";
	    //读取TAG
	    try {
	      mfc.connect();
	    //获取TAG的类型
	      int type = mfc.getType();
	    //获取TAG中包含的扇区数
	      int sectorCount = mfc.getSectorCount();
	      String typeS = "";
	      //判断tag具体类型
	      switch (type) {
	        case MifareClassic.TYPE_CLASSIC:
	          typeS = "TYPE_CLASSIC";
	          break;
	        case MifareClassic.TYPE_PLUS:
	          typeS = "TYPE_PLUS";
	          break;
	        case MifareClassic.TYPE_PRO:
	          typeS = "TYPE_PRO";
	          break;
	        case MifareClassic.TYPE_UNKNOWN:
	          typeS = "TYPE_UNKNOWN";
	          break;
	      }
	      //封装该nfc标签基本存储情况
	      metaInfo += "卡片类型：" + typeS + "\n共" + sectorCount + "个扇区\n共"
	          + mfc.getBlockCount() + "个块\n存储空间: " + mfc.getSize()
	          + "B\n";
	      //遍历所有nfc分区
	      for (int j = 0; j < sectorCount; j++) {
	    	//验证该分区是否验证成功
	        auth = mfc.authenticateSectorWithKeyA(j,
	            MifareClassic.KEY_NFC_FORUM);
	        int bCount;
	        int bIndex;
	        //扇区验证成功
	        if (auth) {
	          metaInfo += "Sector " + j + ":验证成功\n";
	          //读取扇区中的块的总数量
	          bCount = mfc.getBlockCountInSector(j);
	          //获取当前扇区中第一块的具体位置
	          bIndex = mfc.sectorToBlock(j);
	          //遍历该扇区中所有的块
	          for (int i = 0; i < bCount; i++) {
	        	//读取当前块中的数据
	            byte[] data = mfc.readBlock(bIndex);
	            //封装数据
	            metaInfo += "Block " + bIndex + " : "
	                + bytesToHexString(data) + "\n";
	            bIndex++;
	          }
	        //当前扇区验证失败
	        } else {
	          metaInfo += "Sector " + j + ":验证失败\n";
	        }
	      }
	      return metaInfo;
	    } catch (Exception e) {
	      e.printStackTrace();
	    } finally {
	      if (mfc != null) {
	        try {
	          mfc.close();
	        } catch (IOException e) {
	        }
	      }
	    }
	    return null;
	}
	/**
	 * 自定义方法，进行nfc标签MifareClassic数据的写入操作
	 * @param tag
	 */
	public void writeTagForMifareClassic(Tag tag){
		MifareClassic mfc = MifareClassic.get(tag);
	    try {
	      mfc.connect();
	      boolean auth = false;
	      short sectorAddress = 1;
	      auth = mfc.authenticateSectorWithKeyA(sectorAddress,
	          MifareClassic.KEY_NFC_FORUM);
	      if (auth) {
	        // the last block of the sector is used for KeyA and KeyB cannot be overwritted
	    	//每个块容量为16个字节,gb2312，一个英文字符占用1个字节
	        mfc.writeBlock(4, "1313838438000000".getBytes(Charset.forName("gb2312")));
	        mfc.writeBlock(5, "1322676888000000".getBytes(Charset.forName("gb2312")));
	        mfc.close();
	      }
	    } catch (IOException e) {
	      e.printStackTrace();
	    } finally {
	      try {
	        mfc.close();
	      } catch (IOException e) {
	        e.printStackTrace();
	      }
	    }
	  }
	  /**
	   * 字符序列转换为16进制字符串
	   * 为什么要转换成16进制字符串
	   * @param src
	   * @return
	   */
	  private String bytesToHexString(byte[] src) {
	    StringBuilder stringBuilder = new StringBuilder("0x");
	    if (src == null || src.length <= 0) {
	      return null;
	    }
	    char[] buffer = new char[2];
	    for (int i = 0; i < src.length; i++) {
	      buffer[0] = Character.forDigit((src[i] >>> 4) & 0x0F, 16);
	      buffer[1] = Character.forDigit(src[i] & 0x0F, 16);
	      System.out.println(buffer);
	      stringBuilder.append(buffer);
	    }
	    return stringBuilder.toString();
	  }
	  
	  	/**
		 * 注释：MifareUltralight数据格式在nfc标签上的存储方式，分16个page页存储，每个page是4个byte，共存储64个字节（512位）
		 * 存储方式：前面4页（0，1，2，3）存储的是nfc标签的基本信息，后面的page页真正存储数据
		 * 写入数据方式：从page4开始写入数据，一次写入1page数据，写入中文时候，一般推荐gb2312（一个中文占用2字节）编码格式，utf-8中文占用3字节
		 * 读取数据方式：readPage（int acount）,一次性读取4page数据，而且是循环式的读取（14，15，0，1）
		 */
		private String readTagForMifareUltralight(Tag tag){
			//获取mifareUltralight对象
			MifareUltralight mifare = MifareUltralight.get(tag);
			String text = null;
			//建立连接
			try {
				mifare.connect();
				//读取数据
				byte[] data = mifare.readPages(4);
				//以gb2312编码格式进行合并成String
				text = new String(data,"gb2312");
			} catch (IOException e) {
				e.printStackTrace();
			}finally{
				try {
					mifare.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			return text;
		}
		/**
		 * 自定义方法，以MifareUltralight数据格式往nfc标签写入数据
		 * 写入数据方式：一次只能写入1page的数据，4个字节
		 * @return boolean，返回是否写入成功
		 */
		private boolean writeTagForMifareUltralight(Tag tag){
			//获取数据格式对象
			MifareUltralight mifare = MifareUltralight.get(tag);
			try {
				mifare.connect();
				//写入数据，这里测试写入4page的数据
				mifare.writePage(4, "中国".getBytes("gb2312"));
				mifare.writePage(4, "美国".getBytes("gb2312"));
				mifare.writePage(4, "德国".getBytes("gb2312"));
				mifare.writePage(4, "英国".getBytes("gb2312"));
				showToast("nfc标签写入数据成功");
				return true;
			} catch (IOException e) {
				e.printStackTrace();
			}finally{
				try {
					mifare.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			return false;
		}
		private void showToast(String str) {
			Toast.makeText(context, str, Toast.LENGTH_LONG).show();
		}
}
