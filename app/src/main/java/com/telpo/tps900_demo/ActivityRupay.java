package com.telpo.tps900_demo;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.telpo.emv.EmvAmountData;
import com.telpo.emv.EmvApp;
import com.telpo.emv.EmvCAPK;
import com.telpo.emv.EmvCandidateApp;
import com.telpo.emv.EmvOnlineData;
import com.telpo.emv.EmvPinData;
import com.telpo.emv.EmvScriptResult;
import com.telpo.emv.EmvService;
import com.telpo.emv.EmvServiceListener;
import com.telpo.emv.EmvTLV;
import com.telpo.emv.RupayAmount;
import com.telpo.emv.RupayListener;
import com.telpo.emv.RupayOnlineData;
import com.telpo.emv.RupayPRMacq;
import com.telpo.emv.RupayParam;
import com.telpo.emv.RupayServParam;
import com.telpo.pinpad.PinParam;
import com.telpo.pinpad.PinpadService;
import com.telpo.util.StringUtil;

import java.math.BigDecimal;
import java.util.ArrayList;

public class ActivityRupay extends Activity {

    final int pinkey_index = 1;
    Context context;
    SharedPreferences.Editor editor;

    EmvService emvService;
    Button bn_emvDeviceOpen, bn_emvDeviceClose, bn_AddAid, bn_AddCapk, bn_AddCapkTest, bn_readCard, bn_set;
    Button[] buttons;
    EditText et_reslut, et_amount;
    StringBuffer logBuf = new StringBuffer("");
    ProgressDialog processingDialog = null;
    ProgressDialog readCardDialog = null;
    boolean userCancel = false;
    CheckBox cb_isservice;
    boolean isNeedRemoveCard;
    EmvServiceListener emvlistener = new EmvServiceListener() {
        @Override
        public int onInputAmount(EmvAmountData AmountData) {
            return 0;
        }

        @Override
        public int onInputPin(EmvPinData PinData) {
            return 0;
        }

        @Override
        public int onSelectApp(EmvCandidateApp[] appList) {
            return 0;
        }

        @Override
        public int onSelectAppFail(int ErrCode) {
            return 0;
        }

        @Override
        public int onFinishReadAppData() {
            return 0;
        }

        @Override
        public int onVerifyCert() {
            return 0;
        }

        @Override
        public int onOnlineProcess(EmvOnlineData OnlineData) {
            return 0;
        }

        @Override
        public int onRequireTagValue(int tag, int len, byte[] value) {
            return 0;
        }

        @Override
        public int onRequireDatetime(byte[] datetime) {
            return 0;
        }

        @Override
        public int onReferProc() {
            return 0;
        }

        @Override
        public int OnCheckException(String PAN) {
            return 0;
        }

        @Override
        public int OnCheckException_qvsdc(int index, String PAN) {
            return 0;
        }

        @Override
        public int onMir_FinishReadAppData() {
            return 0;
        }

        @Override
        public int onMir_DataExchange() {
            return 0;
        }

        @Override
        public int onMir_Hint() {
            return 0;
        }
    };
    RupayListener listener = new RupayListener() {
        @Override
        public int OnFinishReadAppData() {
            return EmvService.EMV_TRUE;
        }

        @Override
        public int OnRupayCheckException(int PSN, String PAN) {
            boolean is_pan_exception = false;
            if (is_pan_exception) {
                return EmvService.EMV_TRUE;
            }
            return EmvService.EMV_FALSE;
        }

        @Override
        public int OnRupayRequireOnline(RupayOnlineData onlineData) {
            AppendDis("OnRupayRequireOnline");
            Log.w("rupay activity", "OnRupayRequireOnline ");

            if (emvService.Rupay_IsNeedPin() == EmvService.EMV_TRUE) {

                AppendDis("User Input Pin");
                EmvTLV PanTag = null;
                String Pan = null;
                PanTag = new EmvTLV(0x5A);
                int ret = emvService.Emv_GetTLV(PanTag);
                if (ret == EmvService.EMV_TRUE) {
                    StringBuffer p = new StringBuffer(StringUtil.bytesToHexString(PanTag.Value));
                    if (p.charAt(p.toString().length() - 1) == 'F') {
                        p.deleteCharAt(p.toString().length() - 1);
                    }
                    Pan = p.toString();
                } else {
                    PanTag = new EmvTLV(0x57);
                    ret = emvService.Emv_GetTLV(PanTag);
                    if (ret != EmvService.EMV_TRUE) {
                        Pan = null;
                    } else {
                        StringBuffer p = new StringBuffer(StringUtil.bytesToHexString(PanTag.Value));
                        int i = p.indexOf("D");
                        Pan = p.substring(0, i);
                    }
                }

                PinParam pinParam = new PinParam(context);

                pinParam.Amount = new BigDecimal(et_amount.getText().toString()).movePointLeft(2).toString();
                pinParam.CardNo = Pan;
                pinParam.IsShowCardNo = 1;
                pinParam.PinBlockFormat = 0;
                pinParam.KeyIndex = 1;

                ret = PinpadService.TP_PinpadGetPin(pinParam);
                if (ret == PinpadService.PIN_OK) {
                    AppendDis("PinBlock " + StringUtil.bytesToHexString(pinParam.Pin_Block));
                } else {
                    AppendDis("Pin input error: " + ret);
                }

            }
            onlineData.ResponeCode = "00".getBytes();
            onlineData.IssuAuthenData = StringUtil.hexStringToByte("0102030480834000FFFE000000010000");
            //onlineData.ScriptData72 = StringUtil.hexStringToByte("01020304050607080000000000000000");
            AppendDis("ARC:" + new String(onlineData.ResponeCode));

            return EmvService.ONLINE_APPROVE;
        }

        @Override
        public int OnCanRemoveCard() {
            RemoveCard();
            emvService.NfcCloseReader();
            return EmvService.EMV_TRUE;
        }
    };
    private SharedPreferences sp;

    public static void Rupay_InitDefaultPRMac() {
        EmvService.Rupay_RemoveAllServiceMac();
        ArrayList<RupayPRMacq> prmList = new ArrayList<RupayPRMacq>();
        ;
        prmList.add(new RupayPRMacq(0x0010, 0x01, "3E77D2912D5D1BBF7F222496618D8E0B", "244AF7"));
        prmList.add(new RupayPRMacq(0x0011, 0x01, "3E77D2912D5D1BBF7F222496618D8E0B", "244AF7"));
        prmList.add(new RupayPRMacq(0x0012, 0x01, "3E77D2912D5D1BBF7F222496618D8E0B", "244AF7"));
        prmList.add(new RupayPRMacq(0x0016, 0x01, "3E77D2912D5D1BBF7F222496618D8E0B", "244AF7"));
        prmList.add(new RupayPRMacq(0x0017, 0x01, "3E77D2912D5D1BBF7F222496618D8E0B", "244AF7"));
        prmList.add(new RupayPRMacq(0x0018, 0x01, "3E77D2912D5D1BBF7F222496618D8E0B", "244AF7"));
        prmList.add(new RupayPRMacq(0x0019, 0x01, "3E77D2912D5D1BBF7F222496618D8E0B", "244AF7"));
        prmList.add(new RupayPRMacq(0x1010, 0x01, "3E77D2912D5D1BBF7F222496618D8E0B", "244AF7"));
        prmList.add(new RupayPRMacq(0x1010, 0x02, "3E77D2912D5D1BBF0000000000000000", "86FD01"));
        prmList.add(new RupayPRMacq(0x1011, 0x03, "CBB826D0D083F4EDE56600AEB1C1E8D7", "A88FE2"));
        prmList.add(new RupayPRMacq(0x1011, 0x04, "13D66ECA08206CC423A7ABC71C9DE722", "1A0E56"));
        prmList.add(new RupayPRMacq(0x1011, 0x05, "C8E3F1ADDC1289E0DBAE50633EA9EA27", "754C59"));
        prmList.add(new RupayPRMacq(0x1011, 0x06, "ADE64C252863E9B3B575803213E73B44", "7C1BE3"));
        prmList.add(new RupayPRMacq(0x1011, 0x07, "1D67D8DEA1D63C4F2A489252C57B6EB7", "18F889"));
        prmList.add(new RupayPRMacq(0x1011, 0x08, "4EE0D317861851931ABDC0D555766D59", "6CD581"));

        for (RupayPRMacq macq : prmList) {
            int ret = EmvService.Rupay_AddServiceMac(macq);
            Log.w("RupayPRMacq", "AddServiceMac:" + ret);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rupay);
        TextView title_tv=findViewById(R.id.title_tv);
        title_tv.setText("Rupay Test");

        context = ActivityRupay.this;
        sp = getSharedPreferences(ActivityRupayParam.sp_emv_rupay, Context.MODE_PRIVATE);
        editor = sp.edit();
        ParamInit();
        ActivityInit();
        viewInit();
    }

    private void ActivityInit() {
        EmvService.Emv_SetDebugOn(1);
        emvService = EmvService.getInstance();
        emvService.setListener(listener);
        //emvService.setListener(emvlistener);
    }

    void viewInit() {
        bn_emvDeviceOpen = (Button) findViewById(R.id.bn_emvDeviceOpen);
        bn_emvDeviceClose = (Button) findViewById(R.id.bn_emvDeviceClose);
        bn_AddAid = (Button) findViewById(R.id.bn_AddAid);
        bn_AddCapk = (Button) findViewById(R.id.bn_AddCapk);
        bn_AddCapkTest = (Button) findViewById(R.id.bn_AddCapkTest);
        bn_readCard = (Button) findViewById(R.id.bn_readCard);
        bn_set = (Button) findViewById(R.id.bn_set);
        buttons = new Button[]{bn_AddAid, bn_AddCapk, bn_AddCapkTest, bn_readCard};

        et_reslut = (EditText) findViewById(R.id.et_reslut);

        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(et_reslut.getWindowToken(), 0); //强制隐藏键盘
        //imm.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS);

        et_amount = findViewById(R.id.et_amount);
        et_amount.setVisibility(View.VISIBLE);
        bn_emvDeviceOpen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int ret;
                //ClearDis();
                ret = EmvService.Open(context);
                if (ret != EmvService.EMV_TRUE) {
                    AppendDis("Emv open failed ! " + ret);
                    return;
                }
                AppendDis("Emv open success !");
                ret = EmvService.deviceOpen();
                if (ret != 0) {
                    AppendDis("device open failed ! " + ret);
                    return;
                }

                ret = PinpadService.Open(context);
                if (ret != 0) {
                    AppendDis("Pinpad init failed ! " + ret);
                    return;
                }
                ret = PinpadService.TP_WritePinKey(pinkey_index, StringUtil.hexStringToByte("1234567812345678"), PinpadService.KEY_WRITE_DIRECT, 0);
                if (ret != 0) {
                    AppendDis("Wrt pinkey failed ! " + ret);
                    return;
                }
                AppendDis("device open success !");
                setButtonsEnable(true);

                Rupay_InitDefaultPRMac();
            }
        });

        bn_emvDeviceClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int ret = EmvService.deviceClose();
                if (ret != 0) {
                    AppendDis("device close failed !");
                    return;
                }
                AppendDis("device close success !");
                setButtonsEnable(false);
            }
        });

        bn_AddAid.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Add_Rupay_AID();
            }
        });

        //bn_AddCapkTest.setVisibility(View.GONE);
        bn_AddCapkTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Add_Rupay_TestCapk();
            }
        });

        bn_AddCapk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Add_Rupay_Capk();
            }
        });

        bn_set.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(ActivityRupay.this, ActivityRupayParam.class));
            }
        });
        bn_readCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        ClearDis();

                        isNeedRemoveCard = false;

                        if (!Rupay_Init()) {
                            return;
                        }

                        int ret;
                        AppendDis("========================");
                        AppendDis("try to detect Rupay card");

                        int i = EmvService.NfcOpenReader(1000);
                        AppendDis("Open NFC : " + i);
                        if (i != 0) {
                            return;
                        }
                        readCardDialogShow("Please tap the Rupay Card...");
                        ret = detectNFC();
                        readCardDialogHide();
                        if (ret == -4) {
                            AppendDis("user cancel");
                        } else if (ret == -1003) {
                            AppendDis("timeout");
                        } else if (ret == 0) {
                            isNeedRemoveCard = true;
                            Rupay_process_demo();
                            RemoveCard();
                        } else {
                            AppendDis("detect error:" + ret);
                        }
                        EmvService.NfcCloseReader();

                    }
                }).start();
            }
        });

        Button bn_cls = (Button) findViewById(R.id.bn_ref);
        bn_cls.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ClearDis();
            }
        });

        readCardDialog = new ProgressDialog(context);
        readCardDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                emvService.NfcCloseReader();
                userCancel = true;
            }
        });
        readCardDialog.setIndeterminate(false);
        readCardDialog.setTitle("Detecting Rupay card...");
        readCardDialog.setCancelable(true);
        readCardDialog.setCanceledOnTouchOutside(false);
        readCardDialog.setMessage("pls tap your card");

        processingDialog = new ProgressDialog(context);
        processingDialog.setIndeterminate(false);
        processingDialog.setTitle("Processing, please wait...");
        processingDialog.setCancelable(false);
        processingDialog.setCanceledOnTouchOutside(false);
        processingDialog.setMessage("");

        setButtonsEnable(false);

        cb_isservice = findViewById(R.id.cb_isservice);

    }

    private void setButtonsEnable(boolean flag) {
        if (flag) {
            bn_emvDeviceOpen.setEnabled(false);
            bn_emvDeviceClose.setEnabled(true);
            for (Button i : buttons) {
                i.setEnabled(flag);
            }
        } else {
            bn_emvDeviceOpen.setEnabled(true);
            bn_emvDeviceClose.setEnabled(false);
            for (Button i : buttons) {
                i.setEnabled(flag);
            }
        }
    }

    void AppendDis(String Mes) {
        logBuf.append(Mes);
        logBuf.append("\n");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                et_reslut.setText(logBuf.toString());
                et_reslut.setSelection(et_reslut.getText().length());
            }
        });
    }

    void ClearDis() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                et_reslut.setText("");
                logBuf = new StringBuffer("");
            }
        });
    }

    public void readCardDialogShow(final String msg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                readCardDialog.setMessage(msg);
                if (!readCardDialog.isShowing()) {
                    readCardDialog.show();
                }
            }
        });
    }

    public void readCardDialogHide() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (readCardDialog.isShowing()) {
                    readCardDialog.dismiss();
                }
            }
        });
    }

    public void processingDialogShow(final String msg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                processingDialog.setMessage(msg);
                if (!processingDialog.isShowing()) {
                    processingDialog.show();
                }
            }
        });
    }

    public void processingDialogHide() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (processingDialog.isShowing()) {
                    processingDialog.dismiss();
                }
            }
        });
    }

    void Log(String mes) {
        Log.w("rupay test", mes);
    }

    public void Add_Rupay_AID() {
        String name = "";
        int result = 0;
        boolean dbResult = false;

        EmvService.Emv_RemoveAllApp();
        AppendDis("==AID clear ==");

        EmvApp App_Rupay = new EmvApp();
        App_Rupay.Version = StringUtil.hexStringToByte("0002");
        App_Rupay.FloorLimit = StringUtil.hexStringToByte("000000010000");
        App_Rupay.TargetPer = 0;
        App_Rupay.MaxTargetPer = 0;
        App_Rupay.Threshold = StringUtil.hexStringToByte("000000000500");
        App_Rupay.TDOL = StringUtil.hexStringToByte("039F0802");
        App_Rupay.AID = StringUtil.hexStringToByte("A0000005241010");
        App_Rupay.SelFlag = 1;
        result = EmvService.Emv_AddApp(App_Rupay);
        AppendDis("add aid " + StringUtil.bytesToHexString(App_Rupay.AID) + " : " + result);

        App_Rupay = new EmvApp();
        App_Rupay.Version = StringUtil.hexStringToByte("0002");
        App_Rupay.FloorLimit = StringUtil.hexStringToByte("000000010000");
        App_Rupay.TargetPer = 0;
        App_Rupay.MaxTargetPer = 0;
        App_Rupay.Threshold = StringUtil.hexStringToByte("000000000500");
        App_Rupay.TDOL = StringUtil.hexStringToByte("039F0802");
        App_Rupay.AID = StringUtil.hexStringToByte("A0000005241011");
        App_Rupay.SelFlag = 1;
        result = EmvService.Emv_AddApp(App_Rupay);
        AppendDis("add aid " + StringUtil.bytesToHexString(App_Rupay.AID) + " : " + result);

        App_Rupay = new EmvApp();
        App_Rupay.Version = StringUtil.hexStringToByte("0002");
        App_Rupay.FloorLimit = StringUtil.hexStringToByte("000000010000");
        App_Rupay.TargetPer = 0;
        App_Rupay.MaxTargetPer = 0;
        App_Rupay.Threshold = StringUtil.hexStringToByte("000000000500");
        App_Rupay.TDOL = StringUtil.hexStringToByte("039F0802");
        App_Rupay.AID = StringUtil.hexStringToByte("A000000152");
        App_Rupay.SelFlag = 0;
        result = EmvService.Emv_AddApp(App_Rupay);
        AppendDis("add aid " + StringUtil.bytesToHexString(App_Rupay.AID) + " : " + result);

    }

    public void Add_Rupay_TestCapk() {
        int result = 0;
        int capkID = 0;
        boolean dbResult = false;

        EmvService.Emv_RemoveAllCapk();
        AppendDis("==CAPK clear ==");

        RupayCAPK capk_FE = new RupayCAPK(0xFE, "03", "98F0C770F23864C2E766DF02D1E833DFF4FFE92D696E1642F0A88C5694C6479D16DB1537BFE29E4FDC6E6E8AFD1B0EB7EA0124723C333179BF19E93F10658B2F776E829E87DAEDA9C94A8B3382199A350C077977C97AFF08FD11310AC950A72C3CA5002EF513FCCC286E646E3C5387535D509514B3B326E1234F9CB48C36DDD44B416D23654034A66F403BA511C5EFA3");
        RupayCAPK capk_FA = new RupayCAPK(0xFA, "03", "9C6BE5ADB10B4BE3DCE2099B4B210672B89656EBA091204F613ECC623BEDC9C6D77B660E8BAEEA7F7CE30F1B153879A4E36459343D1FE47ACDBD41FCD710030C2BA1D9461597982C6E1BDD08554B726F5EFF7913CE59E79E357295C321E26D0B8BE270A9442345C753E2AA2ACFC9D30850602FE6CAC00C6DDF6B8D9D9B4879B2826B042A07F0E5AE526A3D3C4D22C72B9EAA52EED8893866F866387AC05A1399");
        RupayCAPK capk_F9 = new RupayCAPK(0xF9, "03", "A99A6D3E071889ED9E3A0C391C69B0B804FC160B2B4BDD570C92DD5A0F45F53E8621F7C96C40224266735E1EE1B3C06238AE35046320FD8E81F8CEB3F8B4C97B940930A3AC5E790086DAD41A6A4F5117BA1CE2438A51AC053EB002AED866D2C458FD73359021A12029A0C043045C11664FE0219EC63C10BF2155BB2784609A106421D45163799738C1C30909BB6C6FE52BBB76397B9740CE064A613FF8411185F08842A423EAD20EDFFBFF1CD6C3FE0C9821479199C26D8572CC8AFFF087A9C3");
        RupayCAPK capk_F8 = new RupayCAPK(0xF8, "03", "A1F5E1C9BD8650BD43AB6EE56B891EF7459C0A24FA84F9127D1A6C79D4930F6DB1852E2510F18B61CD354DB83A356BD190B88AB8DF04284D02A4204A7B6CB7C5551977A9B36379CA3DE1A08E69F301C95CC1C20506959275F41723DD5D2925290579E5A95B0DF6323FC8E9273D6F849198C4996209166D9BFC973C361CC826E1");
        RupayCAPK capk_F7 = new RupayCAPK(0xF7, "010001", "924D9576F8FB29F7E086265004EFB5897123F4FC6264E7AA61A53A352D83EFEC14B895101E8F9A00DF895FC780F13CFB5E43471E56BD51B7A6DC48044FA9BEE87032ACBBFB256E9B2559EF6A922F760AEDA1720818A954D6B0DA61F0E101371649898B8E18DCDEAA4BC7867D600A21D6CD462ACDE99F95672D52FECE228DE493");
        RupayCAPK capk_F6 = new RupayCAPK(0xF6, "03", "A25A6BD783A5EF6B8FB6F83055C260F5F99EA16678F3B9053E0F6498E82C3F5D1E8C38F13588017E2B12B3D8FF6F50167F46442910729E9E4D1B3739E5067C0AC7A1F4487E35F675BC16E233315165CB142BFDB25E301A632A54A3371EBAB6572DEEBAF370F337F057EE73B4AE46D1A8BC4DA853EC3CC12C8CBC2DA18322D68530C70B22BDAC351DD36068AE321E11ABF264F4D3569BB71214545005558DE26083C735DB776368172FE8C2F5C85E8B5B890CC682911D2DE71FA626B8817FCCC08922B703869F3BAEAC1459D77CD85376BC36182F4238314D6C4212FBDD7F23D3");
        RupayCAPK capk_F5 = new RupayCAPK(0xF5, "010001", "9C40C83BA1B9CA48296D1F7284553ED0BEB2D8D746034EEAD841FE0DB5F031D8EF70FB7E1A3FD479864551ADB333F59EBB9DFE200D813CF777133D51A402C29C282364A9FF4BD3FAD979DE18725BEDBD21B7175A06817BD21EEE4164E84B91F636D79D2BC66E6FFC45A3C75DB507AA086E993B88364C3AF6CBC2D0A34FDC91BF82DB9D750E44358E99D07406B06D7549EDCCD6164FB84C29258B655C2AEB98886BC4AF12AD151ED695B77434C2F857E981B332A9CF5959540CFBC7D1A197256BE75C200D94EF0B16FC34C1ED33D72CA9AABE06EC9019F299B5A322923E5A396C3A59D819BF2627DF82A10F29A1431492D1CCDD9FADA64CB7");
        RupayCAPK capk_F3 = new RupayCAPK(0xF3, "03", "98F0C770F23864C2E766DF02D1E833DFF4FFE92D696E1642F0A88C5694C6479D16DB1537BFE29E4FDC6E6E8AFD1B0EB7EA0124723C333179BF19E93F10658B2F776E829E87DAEDA9C94A8B3382199A350C077977C97AFF08FD11310AC950A72C3CA5002EF513FCCC286E646E3C5387535D509514B3B326E1234F9CB48C36DDD44B416D23654034A66F403BA511C5EFA3");
        RupayCAPK capk_00 = new RupayCAPK(0x00, "03", "98F0C770F23864C2E766DF02D1E833DFF4FFE92D696E1642F0A88C5694C6479D16DB1537BFE29E4FDC6E6E8AFD1B0EB7EA0124723C333179BF19E93F10658B2F776E829E87DAEDA9C94A8B3382199A350C077977C97AFF08FD11310AC950A72C3CA5002EF513FCCC286E646E3C5387535D509514B3B326E1234F9CB48C36DDD44B416D23654034A66F403BA511C5EFA3");
        RupayCAPK capk_01 = new RupayCAPK(0x01, "010001", "A6E6FB72179506F860CCCA8C27F99CECD94C7D4F3191D303BBEE37481C7AA15F233BA755E9E4376345A9A67E7994BDC1C680BB3522D8C93EB0CCC91AD31AD450DA30D337662D19AC03E2B4EF5F6EC18282D491E19767D7B24542DFDEFF6F62185503532069BBB369E3BB9FB19AC6F1C30B97D249EEE764E0BAC97F25C873D973953E5153A42064BBFABFD06A4BB486860BF6637406C9FC36813A4A75F75C31CCA9F69F8DE59ADECEF6BDE7E07800FCBE035D3176AF8473E23E9AA3DFEE221196D1148302677C720CFE2544A03DB553E7F1B8427BA1CC72B0F29B12DFEF4C081D076D353E71880AADFF386352AF0AB7B28ED49E1E672D11F9");
        RupayCAPK capk_02 = new RupayCAPK(0x02, "03", "A25A6BD783A5EF6B8FB6F83055C260F5F99EA16678F3B9053E0F6498E82C3F5D1E8C38F13588017E2B12B3D8FF6F50167F46442910729E9E4D1B3739E5067C0AC7A1F4487E35F675BC16E233315165CB142BFDB25E301A632A54A3371EBAB6572DEEBAF370F337F057EE73B4AE46D1A8BC4DA853EC3CC12C8CBC2DA18322D68530C70B22BDAC351DD36068AE321E11ABF264F4D3569BB71214545005558DE26083C735DB776368172FE8C2F5C85E8B5B890CC682911D2DE71FA626B8817FCCC08922B703869F3BAEAC1459D77CD85376BC36182F4238314D6C4212FBDD7F23D3");
        RupayCAPK capk_03 = new RupayCAPK(0x03, "010001", "94EA62F6D58320E354C022ADDCF0559D8CF206CD92E869564905CE21D720F971B7AEA374830EBE1757115A85E088D41C6B77CF5EC821F30B1D890417BF2FA31E5908DED5FA677F8C7B184AD09028FDDE96B6A6109850AA800175EABCDBBB684A96C2EB6379DFEA08D32FE2331FE103233AD58DCDB1E6E077CB9F24EAEC5C25AF");
        RupayCAPK capk_04 = new RupayCAPK(0x04, "03", "A1F5E1C9BD8650BD43AB6EE56B891EF7459C0A24FA84F9127D1A6C79D4930F6DB1852E2510F18B61CD354DB83A356BD190B88AB8DF04284D02A4204A7B6CB7C5551977A9B36379CA3DE1A08E69F301C95CC1C20506959275F41723DD5D2925290579E5A95B0DF6323FC8E9273D6F849198C4996209166D9BFC973C361CC826E1");
        RupayCAPK capk_05 = new RupayCAPK(0x05, "03", "A99A6D3E071889ED9E3A0C391C69B0B804FC160B2B4BDD570C92DD5A0F45F53E8621F7C96C40224266735E1EE1B3C06238AE35046320FD8E81F8CEB3F8B4C97B940930A3AC5E790086DAD41A6A4F5117BA1CE2438A51AC053EB002AED866D2C458FD73359021A12029A0C043045C11664FE0219EC63C10BF2155BB2784609A106421D45163799738C1C30909BB6C6FE52BBB76397B9740CE064A613FF8411185F08842A423EAD20EDFFBFF1CD6C3FE0C9821479199C26D8572CC8AFFF087A9C3");
        RupayCAPK capk_F1 = new RupayCAPK(0xF1, "03", "A4DC71056B6607EFD116625AB0506D11DEEB4BAED6475AEF11702C90604BA5D7F2F632236474F0C79E3FBE160A6ABAC126730BD6853ECA412F38CD16DD48129CD53D91F1BB9196F2465C3014FCE2CA702C41472ED0609BD238052FE9C07F38DE7268DF1A0083E4DE20814B5BBFA9ADC33916A049155951648821A05C20CCFD7E8BC141EF3E29A3F306325B13017EDC38D62E03B57A371DFC578274DC78C3FBD6C5E60A0AF2901CAF3B0DD6975EFB5421");
        RupayCAPK capk_06 = new RupayCAPK(0x06, "03", "A1F5E1C9BD8650BD43AB6EE56B891EF7459C0A24FA84F9127D1A6C79D4930F6DB1852E2510F18B61CD354DB83A356BD190B88AB8DF04284D02A4204A7B6CB7C5551977A9B36379CA3DE1A08E69F301C95CC1C20506959275F41723DD5D2925290579E5A95B0DF6323FC8E9273D6F849198C4996209166D9BFC973C361CC826E1");

        RupayCAPK[] ca_list = new RupayCAPK[]{capk_FE, capk_FA, capk_F9, capk_F8, capk_F7, capk_F6, capk_F5, capk_F3, capk_00,
                capk_01, capk_02, capk_03, capk_04, capk_05, capk_06, capk_F1};

        EmvCAPK capk_rupay;

        for (RupayCAPK temp : ca_list) {
            capk_rupay = new EmvCAPK();
            capk_rupay.RID = StringUtil.hexStringToByte("A000000524");
            capk_rupay.HashInd = (byte) 0x01;
            capk_rupay.ArithInd = (byte) 0x01;
            capk_rupay.KeyID = (byte) temp.keyId;
            capk_rupay.Modul = StringUtil.hexStringToByte(temp.module);
            capk_rupay.Exponent = StringUtil.hexStringToByte(temp.exp);
            capk_rupay.ExpDate = new byte[]{0x30, 0x12, 0x31};
            capk_rupay.CheckSum = new byte[]{
                    (byte) 0xCC, (byte) 0x95, (byte) 0x85, (byte) 0xE8, (byte) 0xE6, (byte) 0x37, (byte) 0x19, (byte) 0x1C, (byte) 0x10, (byte) 0xFC, (byte) 0xEC, (byte) 0xB3, (byte) 0x2B, (byte) 0x5A, (byte) 0xE1, (byte) 0xB9, (byte) 0xD4, (byte) 0x10, (byte) 0xB5, (byte) 0x2D
            };
            result = EmvService.Emv_AddCapk(capk_rupay);
            AppendDis("Add capk_rupay:" + result + " ID:" + Integer.toHexString(capk_rupay.KeyID & 0xFF));

        }
    }

    public void Add_Rupay_Capk() {
        int result = 0;
        int capkID = 0;
        boolean dbResult = false;

        EmvService.Emv_RemoveAllCapk();
        AppendDis("==CAPK clear ==");

        RupayCAPK capk_03 = new RupayCAPK(0x03, "03", "E703A908FFAE3730F82E550869A294C1FF1DA25F2B53D2C8BB18F770DAD505135D03D5EC8EE3926550051C3D4857F6FEDB882C2889E0B25F389F78741F2931A92D45D3A47E62810D3253653AB0AB3570C35DFD08D3167B6DB42ED28F765186F4287CDAF9D9BAD20BCE2C4ECFECDD218E50F1FCC718878882F3934A6FEB502CFCAD615A2B2E279A0868DDA9489DFA9CD9");
        RupayCAPK capk_04 = new RupayCAPK(0x04, "03", "AC0019624FC0A72270C6885CC0B3C9140C351FCFE6F8145881A27750393453D3265F69E7658132D8D253EDF8991E2BA32B782D39ADE1FF1FC8F211F5DF51A0007C761AD9882587BD6A36AECD3ABBF944307AC97A2D905FAB489C3E1CCD76DE9EB93ECFAB2BB84F34E770119E356DC6372D8685DA8EB92FCAC7B53C0167100E4CDFB9830D1C45E787E44C9F6A42EC131A6A4CD66BBE4F93CA91FDF157C7B22FC7221A6348F0EDA6151302A80EF77D6CA5");
        RupayCAPK capk_05 = new RupayCAPK(0x05, "03", "C04E80180369898AAEF6EE7741EDED25239D765301614B5B41A008CA3009358D626D828BC5F1B1E04A2DC1367101266905D262003BE747FD231C9B0011F2F2B21BA8E4C0F4CA5E93ED9DBB2E92ABC450576A4EB59AD00DCA59C8BF3230E4B19D43452871C6215D837663310DF43CAEA1B9B08C1F500AF1B550F62E18D70EEE9E9475321BCD1799AB193E0BC849DACE892A0E6A1F42FE0786DB30345AE1A0E7E4C4B71640E03BFD2832C491A7D83F3B4EF4D388CDDBB748C2FD1D9D4A9BF52FC856CBA088D4B274846002C23CDA722C5CFF3B1F8218A1843B0426474BDC92F2F5E31FBF321CC17480AD069DF55381F2E601D5CBA7B871253F");
        RupayCAPK capk_06 = new RupayCAPK(0x06, "03", "9D8A75B36BCBDF250B87615A46F6EA35DE35226EEAB7B473D7DC0A28B5DF075C83B2775F23337E6CEE36CCFE3A6568C9C822D6DE81299565A829348E03D479B631BB18A2429A8590C597F446A3CEA3BE2E822106F43DFBB981EC0F1121919CB35F85DBA3355C5E7FF35F2B221FD65EDBEA41F23A7A109FBBC4A774A756D89B593B199E1E9DA9A99217D4BF31F67CDA8C4E1B81FA2A377C83B5D1CD6AF1F1880448CFF48D3A4ADBBC7FBD730061508A6EA8FDFC5BD66A2E94E33B83F81E0E56CF1C9473E4426EE435F9E80136760D8F4AD946805B03A67C55361582F5AD8F40404392FA4CB4F5C2BAF6E26857A1D60941E3D055ACD9AC0BEF");
        RupayCAPK capk_6A = new RupayCAPK(0x6A, "03", "92795EAA4FE39EB30441FE952D5423778E02F86783B89DD7C587AE80A69F4D6DC55EAFB6604040D875C72002425EE529CE4EA26FD864BAD760160C2AA0C5AF92381894A5CBBC8AB3AF2641606C379B927A397CB1E9B9EA2EF8C0A9C0DDEBB81B0F8913A118F7044156EA7D23AF626EAF30C2C9ECE8534D3563EF5FE95DE76249");
        RupayCAPK capk_6B = new RupayCAPK(0x6B, "03", "C9DFDB625ADA4B5E86049F85A0237627B59524F52BD499B4C5482C1EE012D61A1446E9383CC0B7EE2922D323A5ECDA12941EA8177CFA512DA6B5B7663A89B793B10D314CBB776EB96D0B1734EDE7E1591713915E9991B7B4E8A017A6901279AEBDD6136C9FE7E0C6CBF94C77FA606B629D00B1F890473905EB4DAD1AD93B29C2C1829A82F880B08986B9387611EE409D");
        RupayCAPK capk_6C = new RupayCAPK(0x6C, "03", "C76259FF785ABD5FF613223C01F5BDA0F36F9342CF336B66C32D4B2CD5096E094D8E04DFA11A9B2E3BC78DA63B5C10148D8ED79EBA685D5D0EFE1C58B3F929D861B40FF3AAA3B527148D0C24921EE42DA048E01E38F6A3A49DFA67DD1CD5DD2091412DD36D3269FAF7D2E0FFB1A3E028969CB6BA5A9303A6FF65540F421B069A31B553398EE525EFA5C2CE26BCB81C5345018D5E3E9B7130F72F598C0EAA4682D4DA2F2204518780A8108F82DDC9CF1F");
        RupayCAPK capk_6D = new RupayCAPK(0x6D, "03", "B747E8CB3615E8D26231355488F3C76C4746F7BB1C381E6C6E6ABF0A6D7CD93CFC6B2C310288CA8BE7EE1730DE621A59D1BB2D8C02C9148FA06E5D1F5E672EEFCE8AECBAD4A1C18F3175F1BEA1AEF539376592366B46A5044E32E59B3F35F50E85F843BA01851E5386B7EBE27367D3D483C5472D3020AF42116DDDA32341557EBABB043EBC6006B99A652009045BFA50C527028586E05942E1D594223B49FE8566931C31FBE8C903ABD4F283E1FAB03D758247EC4B728A85A9897601B753293263ADBD10BE988D0C52FE0091C2721DC02C5130FC7663E95739A70EE2F84DFD2E50C88A1A26587EF7CC047FCA2D03C2CF0CE4B524B4EC3F07");

        RupayCAPK[] ca_list = new RupayCAPK[]{capk_03, capk_04, capk_05, capk_06, capk_6A, capk_6B, capk_6C, capk_6D};

        EmvCAPK capk_rupay;

        for (RupayCAPK temp : ca_list) {
            capk_rupay = new EmvCAPK();
            capk_rupay.RID = StringUtil.hexStringToByte("A000000524");
            capk_rupay.HashInd = (byte) 0x01;
            capk_rupay.ArithInd = (byte) 0x01;
            capk_rupay.KeyID = (byte) temp.keyId;
            capk_rupay.Modul = StringUtil.hexStringToByte(temp.module);
            capk_rupay.Exponent = StringUtil.hexStringToByte(temp.exp);
            capk_rupay.ExpDate = new byte[]{0x30, 0x12, 0x31};
            capk_rupay.CheckSum = new byte[]{
                    (byte) 0xCC, (byte) 0x95, (byte) 0x85, (byte) 0xE8, (byte) 0xE6, (byte) 0x37, (byte) 0x19, (byte) 0x1C, (byte) 0x10, (byte) 0xFC, (byte) 0xEC, (byte) 0xB3, (byte) 0x2B, (byte) 0x5A, (byte) 0xE1, (byte) 0xB9, (byte) 0xD4, (byte) 0x10, (byte) 0xB5, (byte) 0x2D
            };
            result = EmvService.Emv_AddCapk(capk_rupay);
            AppendDis("Add capk_rupay:" + result + " ID:" + Integer.toHexString(capk_rupay.KeyID & 0xFF));

        }
    }

    public void showToast(final String text) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, text, Toast.LENGTH_LONG).show();
            }
        });
    }

    public void RemoveCard() {

        showToast("Can Remove The Card");
        isNeedRemoveCard = false;

    }

    int detectNFC() {
        userCancel = false;
        long j = System.currentTimeMillis();
        int ret = -1;
        while (System.currentTimeMillis() - j < 20 * 1000) {

            if (userCancel == true) {
                return -4;
            }

            ret = EmvService.NfcCheckCard(100);
            if (ret == 0) {
                return 0;
            }
            j++;
        }
        return ret;
    }

    boolean Rupay_Init() {
        int ret;
        RupayParam rupayParam = new RupayParam();
        RupayAmount rupayAmount = new RupayAmount();

        rupayAmount.Amount = Long.parseLong(et_amount.getText().toString());
        rupayAmount.CurrCode = 356;
        rupayAmount.CurrExp = 2;
        rupayAmount.TransType = 0x33;

        rupayParam.TermId = sp.getString(ActivityRupayParam.key_RupayParam_TermId, "").getBytes();
        rupayParam.TermAppVer = StringUtil.hexStringToByte(sp.getString(ActivityRupayParam.key_RupayParam_TermAppVer, ""));
        rupayParam.TermCapabilitis = StringUtil.hexStringToByte(sp.getString(ActivityRupayParam.key_RupayParam_TermCapabilitis, ""));
        rupayParam.TermAddCaps = StringUtil.hexStringToByte(sp.getString(ActivityRupayParam.key_RupayParam_TermAddCaps, ""));
        rupayParam.TermCountryCode = Integer.parseInt(sp.getString(ActivityRupayParam.key_RupayParam_TermCountryCode, ""));
        rupayParam.TermType = StringUtil.hexStringToByte(sp.getString(ActivityRupayParam.key_RupayParam_TermType, ""))[0];
        rupayParam.MerchCateCode = StringUtil.hexStringToByte(sp.getString(ActivityRupayParam.key_RupayParam_MerchCateCode, ""));
        rupayParam.NFC_TransLimit = Integer.parseInt(sp.getString(ActivityRupayParam.key_RupayParam_NFC_TransLimit, ""));
        rupayParam.NFC_CVMLimit = Integer.parseInt(sp.getString(ActivityRupayParam.key_RupayParam_NFC_CVMLimit, ""));
        rupayParam.NFC_OffLineFloorLimit = Integer.parseInt(sp.getString(ActivityRupayParam.key_RupayParam_NFC_OffLineFloorLimit, ""));
        rupayParam.TornTimeLimitSec = Integer.parseInt(sp.getString(ActivityRupayParam.key_RupayParam_TornTimeLimitSec, ""));

        emvService.Rupay_TransInit(rupayParam, rupayAmount);

        if (cb_isservice.isChecked()) {
            AppendDis("Service-based Transaction");
            RupayServParam rupayServParam = new RupayServParam();

            rupayServParam.ServiceData = StringUtil.hexStringToByte(sp.getString(ActivityRupayParam.key_rupay_RupayServParam_ServiceData, ""));
            rupayServParam.LegacyPRMacq = StringUtil.hexStringToByte(sp.getString(ActivityRupayParam.key_rupay_RupayServParam_LegacyPRMacq, ""));
            rupayServParam.LegacyKCV = StringUtil.hexStringToByte(sp.getString(ActivityRupayParam.key_rupay_RupayServParam_LegacyKCV, ""));
            rupayServParam.NonLegacyPRMacqIndex = Integer.parseInt(sp.getString(ActivityRupayParam.key_rupay_RupayServParam_NonLegacyPRMacqIndex, ""));
            rupayServParam.PRMiss = StringUtil.hexStringToByte(sp.getString(ActivityRupayParam.key_rupay_RupayServParam_PRMiss, ""));
            rupayServParam.ServiceQualifier = StringUtil.hexStringToByte(sp.getString(ActivityRupayParam.key_rupay_RupayServParam_ServiceQualifier, ""));
            byte[] tmp = StringUtil.hexStringToByte(sp.getString(ActivityRupayParam.key_rupay_RupayServParam_ServiceID, ""));
            rupayServParam.ServiceID = ((tmp[0] & 0xFF) << 8) + tmp[1] & 0xFF;
            rupayServParam.ServiceManagerInfo = StringUtil.hexStringToByte(sp.getString(ActivityRupayParam.key_rupay_RupayServParam_ServiceManagerInfo, ""));
            rupayServParam.LegacyServiceCreate = sp.getBoolean(ActivityRupayParam.key_rupay_RupayServParam_LegacyServiceCreate, true) ? 1 : 0;
            rupayServParam.ServiceCreate = sp.getBoolean(ActivityRupayParam.key_rupay_RupayServParam_ServiceCreate, true) ? 1 : 0;

            Log.w("rupayServParam", rupayServParam.toString());

            emvService.Rupay_SetServiceParam(rupayServParam);

        } else {
            AppendDis("non-Service Transaction");
        }

        ret = emvService.Rupay_Preprocess();
        if (ret != EmvService.RUPAY_RESULT_CONTINUE) {
            if (ret == EmvService.RUPAY_RESULT_ANOTHER_INTERFACE) {
                AppendDis("Please swtich interface");
            } else {
                AppendDis("Preprocess fail:" + ret);
            }
            return false;
        }

        return true;
    }

    private int StartTransaction() {
        int ret = EmvService.EMV_FALSE;
        int ret2 = EmvService.EMV_FALSE;
        processingDialogShow("Start transaction......");
        ret = emvService.Rupay_StartApp(0);
        ret2 = emvService.Rupay_GetOutComeResult();
        AppendDis("Rupay_StartApp  OutCome :0x" + Integer.toHexString(ret2));
        processingDialogHide();

        if (ret2 == EmvService.RUPAY_RESULT_2TAP) {
            ret = Porcess_2ndTap();
            ret2 = emvService.Rupay_GetOutComeResult();
            AppendDis("2ndTap :" + ret + ", OutCome :0x" + Integer.toHexString(ret2));
        }
        return ret2;
    }

    private int Porcess_2ndTap() {
        int ret = EmvService.EMV_FALSE;
        EmvService.NfcOpenReader(1000);
        AppendDis("Please tap the Card again.");
        readCardDialogShow("Please tap the Card again.");
        ret = detectNFC();
        readCardDialogHide();
        if (ret != 0) {
            return EmvService.RUPAY_RESULT_ENDAPP;
        }
        isNeedRemoveCard = true;
        AppendDis(">>Processing 2nd Tap ......");
        processingDialogShow("Processing 2nd Tap ......");
        ret = emvService.Rupay_StartApp2nd();
        processingDialogHide();
        ret = EmvService.RUPAY_RESULT_2TAP;
        return ret;
    }


    int Rupay_process_demo() {
        int ret;

        ret = StartTransaction();

        ProcessRupayTranResult(ret);

        if (ret != EmvService.RUPAY_RESULT_APPROVED) {
            return 0;
        }

        EmvTLV PanTag = null;
        String Pan = null;
        PanTag = new EmvTLV(0x5A);
        ret = emvService.Emv_GetTLV(PanTag);

        if (ret == EmvService.EMV_TRUE) {
            StringBuffer p = new StringBuffer(StringUtil.bytesToHexString(PanTag.Value));

            if (p.charAt(p.toString().length() - 1) == 'F') {
                p.deleteCharAt(p.toString().length() - 1);
            }
            Pan = p.toString();
        } else {

            PanTag = new EmvTLV(0x57);
            ret = emvService.Emv_GetTLV(PanTag);

            if (ret != EmvService.EMV_TRUE) {
                Pan = null;
            } else {
                StringBuffer p = new StringBuffer(StringUtil.bytesToHexString(PanTag.Value));
                int i = p.indexOf("D");
                Pan = p.substring(0, i);
            }
        }

        if (Pan == null) {
            AppendDis("PAN data miss");
        } else {
            AppendDis("PAN:" + Pan);
        }

        EmvTLV tlv;

        tlv = new EmvTLV(0x84);
        ret = emvService.Emv_GetTLV(tlv);
        if (ret == EmvService.EMV_TRUE) {
            AppendDis("AID:" + StringUtil.bytesToHexString(tlv.Value));
        }

        tlv = new EmvTLV(0x95);
        ret = emvService.Emv_GetTLV(tlv);
        if (ret == EmvService.EMV_TRUE) {
            AppendDis("TVR:" + StringUtil.bytesToHexString(tlv.Value));
        }

        tlv = new EmvTLV(0x9B);
        ret = emvService.Emv_GetTLV(tlv);
        if (ret == EmvService.EMV_TRUE) {
            AppendDis("TSI:" + StringUtil.bytesToHexString(tlv.Value));
        }

        if (emvService.Rupay_IsNeedSignture() == EmvService.EMV_TRUE) {
            AppendDis("======= Need signature on receipt =======");
        }
        return ret;

    }

    private int ProcessRupayTranResult(int i) {
        int ifScript;
        EmvScriptResult emvScriptResult;
        //if (i == EmvService.EMV_TRUE)
        {
            emvScriptResult = new EmvScriptResult();
            ifScript = emvService.Rupay_GetScriptResult(emvScriptResult);
            if (ifScript == EmvService.EMV_TRUE) {
                EmvTLV emvTLVT = new EmvTLV(0x9F5B);
                emvTLVT.Value = emvScriptResult.Result;
                emvService.Emv_SetTLV(emvTLVT);

                EmvTLV emvTLVScriptResult = new EmvTLV(0xDF31);
                emvTLVScriptResult.Value = emvScriptResult.Result;
                emvService.Emv_SetTLV(emvTLVScriptResult);
            } else {
                Log("no script ");
            }
        }

        //判断交易结果
        EmvTLV get9F27Tag = new EmvTLV(0x9F27);
        emvService.Emv_GetTLV(get9F27Tag);
        Log("(Start App) over --> Get 9F27 :" + StringUtil.bytesToHexString_upcase(get9F27Tag.Value));

        if (i == EmvService.RUPAY_RESULT_APPROVED) {
            AppendDis("===Transaction Approve===");
        }
        //卡片拒绝交易
        else if (i == EmvService.RUPAY_RESULT_DECLINED) {
            AppendDis("===Transaction Decline===");
        } else {
            AppendDis("===Transaction Terminated===");
        }
        return 0;
    }

    void ParamInit() {
        if (!sp.getBoolean("is_emv_rupay_init", false)) {
            editor.putBoolean("is_emv_rupay_init", true);

            RupayParam rupayParam = new RupayParam();
            editor.putString(ActivityRupayParam.key_RupayParam_TermId, "11223344");
            editor.putString(ActivityRupayParam.key_RupayParam_TermAppVer, StringUtil.bytesToHexString(rupayParam.TermAppVer));
            editor.putString(ActivityRupayParam.key_RupayParam_TermCapabilitis, StringUtil.bytesToHexString(rupayParam.TermCapabilitis));
            editor.putString(ActivityRupayParam.key_RupayParam_TermAddCaps, StringUtil.bytesToHexString(rupayParam.TermAddCaps));
            editor.putString(ActivityRupayParam.key_RupayParam_TermCountryCode, "" + rupayParam.TermCountryCode);
            editor.putString(ActivityRupayParam.key_RupayParam_TermType, Integer.toHexString(rupayParam.TermType));
            editor.putString(ActivityRupayParam.key_RupayParam_MerchCateCode, StringUtil.bytesToHexString(rupayParam.MerchCateCode));
            editor.putString(ActivityRupayParam.key_RupayParam_NFC_TransLimit, "" + rupayParam.NFC_TransLimit);
            editor.putString(ActivityRupayParam.key_RupayParam_NFC_CVMLimit, "" + rupayParam.NFC_CVMLimit);
            editor.putString(ActivityRupayParam.key_RupayParam_NFC_OffLineFloorLimit, "" + rupayParam.NFC_OffLineFloorLimit);
            editor.putString(ActivityRupayParam.key_RupayParam_TornTimeLimitSec, "" + rupayParam.TornTimeLimitSec);

            editor.putString(ActivityRupayParam.key_rupay_RupayServParam_ServiceData, "09101506150101112233445566778800000100010061150406125703000000010000020000000A0101000A01020502000102010206000A01010302");
            editor.putString(ActivityRupayParam.key_rupay_RupayServParam_LegacyPRMacq, "3E77D2912D5D1BBF");
            editor.putString(ActivityRupayParam.key_rupay_RupayServParam_LegacyKCV, "000000");
            editor.putString(ActivityRupayParam.key_rupay_RupayServParam_NonLegacyPRMacqIndex, "1");
            editor.putString(ActivityRupayParam.key_rupay_RupayServParam_PRMiss, "431878BAF5E33E2A77C859A907C0B1CA");
            editor.putString(ActivityRupayParam.key_rupay_RupayServParam_ServiceQualifier, "0810109500");
            editor.putString(ActivityRupayParam.key_rupay_RupayServParam_ServiceID, "1010");
            editor.putString(ActivityRupayParam.key_rupay_RupayServParam_ServiceManagerInfo, "9500");
            editor.putBoolean(ActivityRupayParam.key_rupay_RupayServParam_LegacyServiceCreate, true);
            editor.putBoolean(ActivityRupayParam.key_rupay_RupayServParam_ServiceCreate, true);

            editor.commit();
        }

    }

    class RupayCAPK {
        int keyId;
        String module;
        String exp;

        public RupayCAPK(int keyId, String exp, String module) {
            this.keyId = keyId;
            this.exp = exp;
            this.module = module;
        }
    }

}
