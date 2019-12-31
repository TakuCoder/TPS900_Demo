package com.telpo.tps900_demo;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.telpo.emv.EmvService;
import com.telpo.pinpad.PinParam;
import com.telpo.pinpad.PinTextInfo;
import com.telpo.pinpad.PinpadService;
import com.telpo.util.StringUtil;

import java.io.File;

/*
 示例说明：（数据都是用十六进制表示）
        主密钥：sMasterKey = "30313233343536373839414243444546";
        Pin密钥：明文为："32323232323232323131313131313131"， 主密钥加密(3DES-ECB)得到sPinKey = "50B55FE757865000498C189C17F5E377";
        Des密钥：明文为："31313131313131313232323232323232"， 主密钥加密(3DES-ECB)得到sDesKey = "498C189C17F5E37750B55FE757865000";

 */

public class ActivitySimple extends Activity {

    public static int currMasterKeyIndex;
    public static int currMasterKeyLeft;
    public static int currMasterKeyRight;
    public static int currPinKeyIndex;
    public static int currDesKeyIndex;
    public static int currMacKeyIndex;
    Context mContext = ActivitySimple.this;

    private  final int MSG_SHOW_PINBLOCK = 1;
    private  final int MSG_SHOW_FAIL = 2;

    //pin参数
    PinParam pinParam;

    String sMasterKey;
    String sPinKey;
    String sDesKey;
    Button btn_masterkey;
    Button btn_pin_amount_cardno;
    Button btn_pin_amount;
    Button btn_pin_cardno;
    Button btn_writedeskey;
    Button btn_des;
    Button btn_writepinkey;
    Button bn_dukpt_getpin;
    Button bn_pin_Customize;
    Button bn_wrt_bdk;
    EditText edt_pinblock;
    EditText edt_pindes;

    Button btn_1;
    Button btn_2;
    Button btn_3;
    Handler handler;
    PinTextInfo[] pinText;
    TextView title_tv;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simple);

        title_tv=findViewById(R.id.title_tv);
        title_tv.setText("PINPAD EXAMPLE");

        handler = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == MSG_SHOW_FAIL){
                    String sErrorCode = (String)msg.obj;
                    Toast.makeText(ActivitySimple.this,"FAIL! ERROR CODE " + sErrorCode , Toast.LENGTH_SHORT).show();
                }else {
                    edt_pinblock.setText((String)msg.obj);
                }
            }
        };
//        sMasterKey = "30313233343536373839414243444546";  //主密钥
//        sPinKey = "50B55FE757865000498C189C17F5E377";     //用主密钥加密后的pin密钥
//        sDesKey = "498C189C17F5E37750B55FE757865000";     //用主密钥加密后的des加密密钥

        sMasterKey = "55DD21B40C3CB4F6CFC393A960123CE8";  //主密钥
        sPinKey = "68C888ED19B82501BBD7F4A1EFD1AFF2";     //用主密钥加密后的pin密钥
        sDesKey = "498C189C17F5E37750B55FE757865000";     //用主密钥加密后的des加密密钥

        currMasterKeyIndex = 0;
        currMasterKeyLeft = 1;
        currMasterKeyRight = 2;
        currPinKeyIndex = 3;
        currDesKeyIndex = 4;
        currMacKeyIndex = 5;
        int i;
        i = EmvService.Open(ActivitySimple.this); //返回1成功， 其他失败
        Log.d("telpo", "EMVservice open:" + i);
        if(i != 1){
            Toast.makeText(ActivitySimple.this,"EMVservice open fail", Toast.LENGTH_SHORT).show();
        }

        i = EmvService.deviceOpen();//返回0成功，其他失败
        Log.d("telpo", "EMVservice deviceOpen open:" + i);
        if(i != 0){
            Toast.makeText(ActivitySimple.this," EmvService.deviceOpen fail", Toast.LENGTH_SHORT).show();
        }

        i = PinpadService.Open((ActivitySimple.this));//返回0成功其他失败
        Log.d("telpo", "PinpadService deviceOpen open:" + i);

        if (i == PinpadService.PIN_ERROR_NEED_TO_FOMRAT){
            PinpadService.TP_PinpadFormat(mContext);
            i = PinpadService.Open((ActivitySimple.this));//返回0成功其他失败
        }
        Log.d("telpo", "PinpadService deviceOpen open:" + i);
        if(i != 0){
            Toast.makeText(ActivitySimple.this,"PinpadService open faol", Toast.LENGTH_SHORT).show();
        }

        edt_pindes = (EditText)findViewById(R.id.edt_pindes);
        edt_pinblock = (EditText)findViewById(R.id.edt_pinblock);

        //写密钥加密密钥
        btn_masterkey = (Button) findViewById(R.id.btn_masterkey);
        btn_masterkey.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //第一个参数为密钥索引，用加密函数时指定加密密钥
                //第二个参数为密钥
                //第三个参数为模式KEY_WRITE_DIRECT：
                //第四个参数为 给第三个参数加密的密钥
                int i = PinpadService.TP_WriteMasterKey(currMasterKeyIndex,hexStringToByte(sMasterKey), PinpadService.KEY_WRITE_DIRECT);

                Log.d("telpo", "TP_WritePinKey:" + i);
                if (i == 0){
                    Toast.makeText(ActivitySimple.this,"success!", Toast.LENGTH_SHORT).show();
                }else
                {
                    Toast.makeText(ActivitySimple.this,"FAIL!", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
        });


        //写加密密钥
        btn_writedeskey = (Button) findViewById(R.id.btn_writedeskey);
        btn_writedeskey.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //第一个参数为密钥索引，用加密函数时指定加密密钥
                //第二个参数为密钥
                //第三个参数为模式KEY_WRITE_DIRECT：直接写进去  KEY_WRITE_DECRYPT：先用第四个参数所指定的密钥解密第三个参数所代表加密的密钥，再写进去
                //第四个参数为 给第三个参数加密的密钥
                int i = PinpadService.TP_WriteDesKey(currDesKeyIndex,hexStringToByte(sDesKey), PinpadService.KEY_WRITE_DECRYPT, currMasterKeyIndex);
                if (i == 0){
                    Toast.makeText(ActivitySimple.this,"success!", Toast.LENGTH_SHORT).show();
                }else
                {
                    Toast.makeText(ActivitySimple.this,"FAIL!", Toast.LENGTH_SHORT).show();
                    return;
                }
                Log.d("telpo", "TP_WritePinKey:" + i);



            }
        });

        //写pin密钥
        btn_writepinkey = (Button) findViewById(R.id.btn_writepinkey);
        btn_writepinkey.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //第一个参数为密钥索引，用加密函数时指定加密密钥
                //第二个参数为密钥
                //第三个参数为模式KEY_WRITE_DIRECT：直接写进去 直接写进去  KEY_WRITE_DECRYPT：先用第四个参数所指定的密钥解密第三个参数所代表加密的密钥，再写进去。
                //第四个参数为 给第三个参数加密的密钥
                int i = PinpadService.TP_WritePinKey(currPinKeyIndex, hexStringToByte(sPinKey), PinpadService.KEY_WRITE_DECRYPT, currMasterKeyIndex);
                Log.d("telpo", "TP_WritePinKey:" + i);
                if (i == 0){
                    Toast.makeText(ActivitySimple.this,"success!", Toast.LENGTH_SHORT).show();
                }else
                {
                    Toast.makeText(ActivitySimple.this,"FAIL!", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
        });


       // EmvService.Poweroff_Resume();

        //加密数据
        btn_des = (Button) findViewById(R.id.btn_des);
        btn_des.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                byte[] value = {0x00, 0x00, 0x00, 0x00, 0x00, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39, 0x30, 0x31, 0x32, 0x33};
                int len = value.length/8*8 + (value.length%8==0? 0:8);  //加密后的数据长度是8的倍数，如果value长度是8的倍数则与value一样长，如果不是8的倍数就则是比value大的第一个8的倍数。
                byte[] encryptBlock = new byte[len];
                //第一个参数为密钥索引，用加密函数时指定加密密钥
                //第二个参数为要加密的数据
                //第三个参数为模式加密后的数据
                int i = PinpadService.TP_DesByKeyIndex(currDesKeyIndex, value, encryptBlock, PinpadService.PIN_DES_ENCRYPT );

                if (i == PinpadService.PIN_OK){
                    edt_pindes.setText(bytesToHexString_upcase(encryptBlock));
                }else {
                    Toast.makeText(ActivitySimple.this,"FAIL! ERROR CODE " + i, Toast.LENGTH_SHORT).show();
                }
            }
        });

        //调用密码键盘 有金额和卡号
        //例子：
        //密码：1234 卡号：4838340177005006 加密密钥：32323232323232323131313131313131 （前面已经写入）
        // 则PINBlock：0412B7BFE88FFAFF
        //加密结果：8C54067D0F21CF25
        btn_pin_amount_cardno = (Button) findViewById(R.id.btn_pin_amount_cardno);
        btn_pin_amount_cardno.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pinParam = new PinParam(ActivitySimple.this);
                pinParam.CardNo = "4838340177005006";   //银行卡号
                pinParam.IsShowCardNo = 1;  //IsShowCardNo为1表示显示银行卡卡号，否则不现实
                pinParam.Amount = "100.00"; //pinParam.Amount有赋值则显示金额，否则不显示
                pinParam.KeyIndex = currPinKeyIndex;    //密钥索引
                pinParam.MaxPinLen = 4;                 //密码最大长度
                pinParam.MinPinLen = 4;                 //密码最小长度
                pinParam.WaitSec = 1000;                  //密码键盘超时时间
                //最后加密结果在pinParam.Pin_Block中
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        //返回PinpadService.PIN_OK成功，其他失败
                        int i = PinpadService.TP_PinpadGetPin(pinParam);
                        Log.d("FanZ", "TP_PinpadGetPin：" + i);
                        if (i == PinpadService.PIN_OK){
                            Message m = new Message();
                            m.what = MSG_SHOW_PINBLOCK;
                            //获取结果
                            m.obj = StringUtil.bytesToHexString_upcase(pinParam.Pin_Block);
                            handler.sendMessage(m);
                        }
                        else
                        {
                            Message m = new Message();
                            m.what = MSG_SHOW_FAIL;
                            m.obj = ""+i;
                            handler.sendMessage(m);
                        }
                        Log.d("FanZ", "run: " + bytesToHexString_upcase(pinParam.Pin_Block));

                    }
                }).start();
           }
        });

        //调用密码键盘 有金额
        btn_pin_amount = (Button) findViewById(R.id.btn_pin_amount);
        btn_pin_amount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pinParam = new PinParam(ActivitySimple.this);
                pinParam.IsShowCardNo = 0;
                pinParam.CardNo = "4838340177005006";   //银行卡号
                pinParam.Amount = "100.00";
                pinParam.KeyIndex = currPinKeyIndex;    //密钥索引
                pinParam.MaxPinLen = 4;                 //密码最大长度
                pinParam.MinPinLen = 4;                 //密码最小长度
                pinParam.WaitSec = 20;                  //密码键盘超时时间
                //最后加密结果在pinParam.Pin_Block中
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        //返回PinpadService.PIN_OK成功，其他失败
                        int i = PinpadService.TP_PinpadGetPin(pinParam);
                        Log.d("FanZ", "TP_PinpadGetPin：" + i);
                        if (i == PinpadService.PIN_OK){
                            Message m = new Message();
                            m.what = MSG_SHOW_PINBLOCK;
                            //获取结果
                            m.obj = StringUtil.bytesToHexString_upcase(pinParam.Pin_Block);
                            handler.sendMessage(m);
                        }
                        else
                        {
                            Message m = new Message();
                            m.what = MSG_SHOW_FAIL;
                            m.obj = ""+i;
                            handler.sendMessage(m);
                        }
                        Log.d("FanZ", "run: " + bytesToHexString_upcase(pinParam.Pin_Block));

                    }
                }).start();
            }
        });


        //调用密码键盘 卡号
        btn_pin_cardno = (Button) findViewById(R.id.btn_pin_cardno);
        btn_pin_cardno.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pinParam = new PinParam(ActivitySimple.this);
                pinParam.CardNo = "4838340177005006";   //银行卡号
                pinParam.IsShowCardNo = 1;
                pinParam.KeyIndex = currPinKeyIndex;    //密钥索引
                pinParam.MaxPinLen = 4;                 //密码最大长度
                pinParam.MinPinLen = 4;                 //密码最小长度
                pinParam.WaitSec = 60;                  //密码键盘超时时间
                //最后加密结果在pinParam.Pin_Block中
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        //返回PinpadService.PIN_OK成功，其他失败
                        int i = PinpadService.TP_PinpadGetPin(pinParam);
                        Log.d("FanZ", "TP_PinpadGetPin：" + i);
                        if (i == PinpadService.PIN_OK){
                            Message m = new Message();
                            m.what = MSG_SHOW_PINBLOCK;
                            //获取结果
                            m.obj = StringUtil.bytesToHexString_upcase(pinParam.Pin_Block);
                            handler.sendMessage(m);
                        }
                        else
                        {
                            Message m = new Message();
                            m.what = MSG_SHOW_FAIL;
                            m.obj = ""+i;
                            handler.sendMessage(m);
                        }
                        Log.d("FanZ", "run: " + bytesToHexString_upcase(pinParam.Pin_Block));

                    }
                }).start();
            }
        });

        //使用Dukpt（一次一密）步骤：
        //1、写入BDK,KSN。（在这之前必须已经成功打开PinpadService）
        //2、调用PinpadService.TP_PinpadDukptSessionStart()。
        //3、PinpadService.TP_PinpadDukptGetPin(pinParam)或者 PinpadService.TP_PinpadDukptGetMac(Indate,MAC,KSN)（MAC,KSN为函数输出）;
        //4、PinpadService.TP_PinpadDukptSessionEnd();
        bn_wrt_bdk = (Button) findViewById(R.id.btn_pin_write_dbk_ksn);
        bn_wrt_bdk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int ret = -1;
                byte[] BDK = StringUtil.hexStringToByte("0123456789ABCDEFFEDCBA9876543210");
                byte[] KSN = StringUtil.hexStringToByte("FFFF9876543210E00000");
                ret = PinpadService.TP_PinpadWriteDukptKey(BDK, KSN, 1, PinpadService.KEY_WRITE_DIRECT, 0);
                if (ret == 0) {
                    Toast.makeText(ActivitySimple.this, "success!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(ActivitySimple.this, "FAIL!", Toast.LENGTH_SHORT).show();
                    return;
                }
                Log.d("FanZ", "TP_PinpadWriteDukptKey:" + ret);
            }
        });


        bn_dukpt_getpin = (Button) findViewById(R.id.btn_pin_dukpt);
        bn_dukpt_getpin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        PinpadService.TP_PinpadDukptSessionStart(1);
                        int ret;
                        //ret = PinpadService.TP_PinpadDukptSessionStart();
                        pinParam = new PinParam(ActivitySimple.this);
                        pinParam.KeyIndex = 1;
                        pinParam.WaitSec = 60;
                        pinParam.MaxPinLen = 6;
                        pinParam.MinPinLen = 4;
                        pinParam.CardNo = "4012345678909";
                        pinParam.IsShowCardNo = 1;
                        pinParam.Amount = "123.00";
                        ret = PinpadService.TP_PinpadDukptGetPin(pinParam);
                        if (ret == PinpadService.PIN_OK) {
                            byte[] MAC = new byte[8];
                            byte[] KSN = new byte[10];
                            byte Indate[] = "4012345678909D987".getBytes();
                            ret = PinpadService.TP_PinpadDukptGetMac(Indate, MAC, KSN);
                            Log.d("FanZ", "GetMac: " + ret);
                            Log.d("FanZ", "mac: " + StringUtil.bytesToHexString(MAC));
                            Log.d("FanZ", "KSN: " + StringUtil.bytesToHexString(KSN));
                            final String mes = "PIN:" + StringUtil.bytesToHexString_upcase(pinParam.Pin_Block) +
                                    "\nMAC:" + StringUtil.bytesToHexString_upcase(MAC) +
                                    "\nKSN:" + StringUtil.bytesToHexString_upcase(KSN);
                            Message m = new Message();
                            m.what = MSG_SHOW_PINBLOCK;
                            //获取结果
                            m.obj = mes;
                            handler.sendMessage(m);
                        } else {
                            Message m = new Message();
                            m.what = MSG_SHOW_FAIL;
                            m.obj = "" + ret;
                            handler.sendMessage(m);
                        }

                        Log.d("FanZ", "TP_PinpadDukptGetPin: " + ret);
                        Log.d("FanZ", "Pin_Block: " + StringUtil.bytesToHexString(pinParam.Pin_Block));
                        Log.d("FanZ", "Curr_KSN: " + StringUtil.bytesToHexString(pinParam.Curr_KSN));
                        ret = PinpadService.TP_PinpadDukptSessionEnd();
                        Log.d("FanZ", "session end: " + ret);
                        PinpadService.TP_PinpadDukptSessionEnd();
                    }
                }).start();
            }
        });

        //Customize
        bn_pin_Customize = (Button) findViewById(R.id.btn_pin_Customize);
        bn_pin_Customize.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pinParam = new PinParam(ActivitySimple.this);
                pinParam.CardNo = "4838340177005006";   //银行卡号
                pinParam.IsShowCardNo = 0;
                pinParam.KeyIndex = currPinKeyIndex;    //密钥索引
                pinParam.MaxPinLen = 4;                 //密码最大长度
                pinParam.MinPinLen = 4;                 //密码最小长度
                pinParam.WaitSec = 60;                  //密码键盘超时时间
                pinText = new PinTextInfo[4];
                pinText[0] = new PinTextInfo();
                pinText[0].FontColor = 0x0000FF;
                pinText[0].FontFile = "";
                pinText[0].FontSize = 48;
                pinText[0].PosX = 60;
                pinText[0].PosY = 60;
//pinText[0].sText = "الهروي (ت 401هـ) في";
                pinText[0].sText = "مع CNN بالعربية، ";

                pinText[0].LanguageID = "ar";

                pinText[1] = new PinTextInfo();
                pinText[1].FontColor = 0xFF00FF;
                pinText[1].FontFile = "";
                pinText[1].FontSize = 32;
                pinText[1].PosX = 20;
                pinText[1].PosY = 140;
                pinText[1].sText = "Islámico:niño";
                pinText[1].LanguageID = "en";

                pinText[2] = new PinTextInfo();
                pinText[2].FontColor = 0xFF0000;

                pinText[2].FontFile = new File(getFilesDir(),"DroidSansHindi.ttf").getAbsolutePath();
                pinText[2].FontSize = 48;
                pinText[2].PosX = 280;
                pinText[2].PosY = 140;
                pinText[2].sText = "ताजा ख़बरें";
                pinText[2].LanguageID = "en";

                pinText[3] = new PinTextInfo();
                pinText[3].FontColor = 0xFF0000;
                pinText[3].FontFile = "";
                pinText[3].FontSize = 48;
                pinText[3].PosX = 20;
                pinText[3].PosY = 200;
                pinText[3].sText = "天波";
                pinText[3].LanguageID = "zh";
                //最后加密结果在pinParam.Pin_Block中
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        //返回PinpadService.PIN_OK成功，其他失败
                        int i =PinpadService.TP_PinpadGetPinCustomize(pinParam, pinText,0,0,0);
                        Log.d("FanZ", "TP_PinpadGetPin：" + i);
                        if (i == PinpadService.PIN_OK) {
                            Message m = new Message();
                            m.what = MSG_SHOW_PINBLOCK;
                            //获取结果
                            m.obj = StringUtil.bytesToHexString_upcase(pinParam.Pin_Block);
                            handler.sendMessage(m);
                        } else {
                            Message m = new Message();
                            m.what = MSG_SHOW_FAIL;
                            m.obj = "" + i;
                            handler.sendMessage(m);
                        }
                        Log.d("FanZ", "run: " + bytesToHexString_upcase(pinParam.Pin_Block));

                    }
                }).start();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        int i = EmvService.deviceClose();
        Log.d("telpo", "Device close: " + i);
        PinpadService.Close();
        Log.d("telpo", "Device close");
    }
    public static byte[] hexStringToByte(String hex) {
        if(hex == null || hex.length()==0){
            return null;
        }
        int len = (hex.length() / 2);
        byte[] result = new byte[len];
        char[] achar = hex.toUpperCase().toCharArray();
        for (int i = 0; i < len; i++) {
            int pos = i * 2;
            result[i] = (byte) (toByte(achar[pos]) << 4 | toByte(achar[pos + 1]));
        }
        return result;
    }
    public static byte toByte(char c) {
        byte b = (byte) "0123456789ABCDEF".indexOf(c);
        return b;
    }

    public static String bytesToHexString_upcase(byte[] src) {
        StringBuilder stringBuilder = new StringBuilder("");

        if (src == null || src.length <= 0) {
            return "";
        }
        char[] buffer = new char[2];
        for (int i = 0; i < src.length ; i++) {
            buffer[0] = Character.forDigit((src[i] >>> 4) & 0x0F, 16);
            buffer[1] = Character.forDigit(src[i] & 0x0F, 16);
            stringBuilder.append(buffer);
            stringBuilder.append(" ");
        }
        return stringBuilder.toString().toUpperCase();
    }

}
