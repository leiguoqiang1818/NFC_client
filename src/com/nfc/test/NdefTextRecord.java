package com.nfc.test;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import android.nfc.NdefRecord;
import android.widget.Toast;
/**
 * nfc标签：Ndef数据解析封装类
 * ndef数据存储方式：
 * 第一个字节：存放的数据的状态（共8个位，第一位存放文本编码格式0或1，0代表utf-8,1代表utf-16,第二位固定0，无任何含义
 * 后面6位代表语言编码的字节长度，决定下面n的大小）
 * 第2-n个字节：存放的是语言编码格式，n的大小由第一个字节中的后6位的数值大小决定
 * 第n个字节之后：存放的是正常的文本数据，
 * 使用方法：调用静态方法parse（），获取该类的对象，在通过该对象调用getText（）方法获取nfc标签中的数据
 * @author wsd_leiguoqiang
 */
public class NdefTextRecord {
	/**
	 * ndef解析出来的正文本信息
	 */
    private String text;
    /**
     * 语言编码格式
     */
    private String languageCode;

    public NdefTextRecord(String text, String languageCode){
        this.text = text;
        this.languageCode = languageCode;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
    /**
     * 静态方法，返回该NdefTextRecord类对象
     * @param ndefRecord
     * @return NdefTextRecord对象
     */
    public static NdefTextRecord parse(NdefRecord ndefRecord)  {
        try {
        	//双重判断该nfc标签存放的是否是ndef格式数据
        	//判断TNF类型（）类型名格式
            if (ndefRecord.getTnf() != NdefRecord.TNF_WELL_KNOWN){
            	Toast.makeText(MyApplication.getContext(), "该nfc标签TNF（类型名格式）不匹配", Toast.LENGTH_SHORT).show();
            	return  null;
            } 
            //判断可变的长度类型
            if (!Arrays.equals(ndefRecord.getType(), NdefRecord.RTD_TEXT)){
            	Toast.makeText(MyApplication.getContext(), "该nfc标签可变长度类型不匹配", Toast.LENGTH_SHORT).show();
            	return  null;
            };
            //获取nfc标签中byte数组数据对象
            byte[] payload = ndefRecord.getPayload();
            //& 0x80（10000000）,通过位与运算，判断第一位是否为0
            String encoding = ((payload[0] & 0x80) == 0) ? "utf-8" : "utf-16";
            //通过位与运算& 0x3f（00111111）判断后6位的的数值大小，确定语言编码的长度
            int languageCodeLength = payload[0] & 0x3f;
            //确定语言编码格式，确定里面的文本是中文，英文，还是日文。另外这里统一的字符编码格式为US-ASCII
            String languageCode = new String(payload, 1, languageCodeLength, "US-ASCII");
            //创建该nfc标签中正常的文本内容
            String text = new String(payload, languageCodeLength+1, payload.length - languageCodeLength - 1, encoding);
            //返回该类的对象
            return new NdefTextRecord(text, languageCode);
        } catch (UnsupportedEncodingException e) {
            throw  new IllegalArgumentException(e.getMessage());
        }
    }
}