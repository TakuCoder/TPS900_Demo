package com.telpo.tps900_demo;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.telpo.emv.AmexAmount;
import com.telpo.emv.AmexLimits;
import com.telpo.emv.AmexListener;
import com.telpo.emv.AmexParam;
import com.telpo.emv.AmexResult;
import com.telpo.emv.EmvApp;
import com.telpo.emv.EmvCAPK;
import com.telpo.emv.EmvCertRevo;
import com.telpo.emv.EmvService;
import com.telpo.emv.EmvTLV;
import com.telpo.pinpad.PinParam;
import com.telpo.pinpad.PinpadService;
import com.telpo.util.StringUtil;

import java.io.UnsupportedEncodingException;

import static com.telpo.tps900_demo.DefaultAPPCAPK.Log;


public class ActivityAmex extends Activity {
    final int pinkey_index = 2;
    Context context;
    EmvService emvService;
    private SharedPreferences sp;

    Button bn_emvDeviceOpen, bn_emvDeviceClose, bn_AddAid, bn_AddCapk, bn_readCard, bn_set;
    Button[] buttons;
    EditText et_reslut, et_amount;

    StringBuffer logBuf = new StringBuffer("");
    ProgressDialog processingDialog = null;
    ProgressDialog readCardDialog = null;
    boolean userCancel = false;

    AmexListener amexListener = new AmexListener() {
        @Override
        public int OnAmexMessage(int MessageID, int HoldTimesMs) {
            AppendDis("OnAmexMessage: "+messStr(MessageID));
            return 0;
        }

        @Override
        public int OnAmexCheckException(int PSN, String PAN) {
            String ExpPan = "373737345678904";
            int PanIndex = 0;
            boolean terminal_bEnableExcepion = sp.getBoolean("setting_exception",false);
            if (terminal_bEnableExcepion)
            {
                AppendDis("===Check exception===");

                if ((PAN.equals(ExpPan) && (PanIndex == PSN)))
                {
                    return EmvService.EMV_TRUE;
                }
            }else {
                AppendDis("Disable exception ");
            }

            return EmvService.EMV_FALSE;
        }

        @Override
        public int OnAmexRequireOnline() {
            return AmexResult.AMEX_ONLINE_APPROVED;
        }

        @Override
        public int OnAmexInputPin() {
            int ret;
            EmvTLV PanTag;
            String Pan = "";

            PanTag = new EmvTLV(0x5A);
            ret = emvService.Emv_GetTLV(PanTag);
            if (ret == EmvService.EMV_TRUE) {
                StringBuffer p = new StringBuffer(StringUtil.bytesToHexString(PanTag.Value));

                if (p.charAt(p.toString().length() - 1) == 'F') {
                    p.deleteCharAt(p.toString().length() - 1);
                }
                Pan = p.toString();

                PinParam param = new PinParam(context);
                param.KeyIndex = pinkey_index;
                param.WaitSec = 10;
                param.MaxPinLen = 6;
                param.MinPinLen = 4;
                param.CardNo = Pan;
                param.IsShowCardNo = 1;
                param.Amount = et_amount.getText().toString();
                PinpadService.Open(context);
                ret = PinpadService.TP_PinpadGetPin(param);
                if(ret == 0){
                    AppendDis("PinBlock:"+ StringUtil.bytesToHexString(param.Pin_Block));
                    return EmvService.EMV_TRUE;
                }
            }
            return EmvService.EMV_FALSE;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_amex);

        TextView title_tv=findViewById(R.id.title_tv);
        title_tv.setText("Amex Test");
        context = ActivityAmex.this;
        sp = PreferenceManager.getDefaultSharedPreferences(context);

        ActivityInit();
        ViewInit();
    }

    private void ActivityInit() {
        EmvService.Emv_SetDebugOn(1);
        emvService = EmvService.getInstance();
        emvService.setListener(amexListener);
    }

    void ViewInit() {
        bn_emvDeviceOpen = findViewById(R.id.bn_emvDeviceOpen);
        bn_emvDeviceClose = findViewById(R.id.bn_emvDeviceClose);
        bn_AddAid = findViewById(R.id.bn_AddAid);
        bn_AddCapk = findViewById(R.id.bn_AddCapk);
        bn_readCard = findViewById(R.id.bn_readCard);
        bn_set = findViewById(R.id.bn_set);
        buttons = new Button[]{bn_AddAid, bn_AddCapk, bn_readCard};

        et_reslut = findViewById(R.id.et_reslut);
        et_amount = findViewById(R.id.et_amount);

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
                Add_AID();
                AppendDis("add aid");
            }
        });

        bn_AddCapk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Add_Capk();
                AppendDis("add Capk");
            }
        });

        bn_set.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(context, AmexConfig_Activity.class));
            }
        });

        bn_readCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        ClearDis();
                        Amex_process_demo();
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
        readCardDialog.setTitle("Detecting Amex card...");
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
    }

    int Amex_process_demo() {
        int ret;

        AmexTransactionInit();

        AmexTransactionLimitsSet();

        ret = AmexTransactionProcess();

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

        if (emvService.Amex_IsNeedSignature() == EmvService.EMV_TRUE) {
            AppendDis("======= Need signature on receipt =======");
        }
        return ret;
    }

    void AmexTransactionInit(){
        AmexAmount amexAmount = new AmexAmount();
        amexAmount.Amount = Long.parseLong(et_amount.getText().toString());
        amexAmount.CashbackAmount = 0;
        amexAmount.CurrExp = 2;
        amexAmount.CurrCode = 978;

        int[] trantype = {0,1};
        amexAmount.TransType = 0x00;
        AmexParam amexParam = new AmexParam();
        Log("setting_appver:"+sp.getString("setting_appver","0001"));
        amexParam.AmexVersion = StringUtil.hexStringToByte(sp.getString("setting_appver","0001"));
        amexParam.TermCapability = StringUtil.hexStringToByte(sp.getString("setting_termcap","E000C8"));
        amexParam.TermCountryCode = Integer.parseInt(sp.getString("setting_countrycode","0620"));
        amexParam.TermType = Integer.parseInt(sp.getString("setting_termianltype","34"));
        if(sp.getBoolean("setting_atm",true)){
            amexParam.TermType = 0x14+( amexParam.TermType & 0x03 ) -1;
        }
        amexParam.ContactlessCap = Integer.parseInt(sp.getString("setting_CL_Reader_Caps","200"));

        byte[] tag9F6E = new byte[]{0x18,0x00,0x00,0x03};
        boolean setting_contact_mode = sp.getBoolean("setting_contact_mode",false);
        boolean setting_Mag_Mode_b = sp.getBoolean("setting_Mag_Mode",false);
        boolean setting_moblie_cvm_b = sp.getBoolean("setting_moblie_cvm",false);
        boolean setting_onlinepin_b = sp.getBoolean("setting_onlinepin",false);
        boolean setting_signature_b = sp.getBoolean("setting_signature",false);
        boolean setting_delayauthor_b = sp.getBoolean("setting_delayauthor",false);
        boolean setting_termexempt_b = sp.getBoolean("setting_termexempt",false);

        if(setting_contact_mode) tag9F6E[0] |= 0x84;
        if(setting_Mag_Mode_b) tag9F6E[0] |= 0x40;
        if(setting_moblie_cvm_b) tag9F6E[1] |= 0x80;
        if(setting_onlinepin_b) tag9F6E[1] |= 0x40;
        if(setting_signature_b) tag9F6E[1] |= 0x20;
        if(setting_delayauthor_b) tag9F6E[3] |= 0x40;
        if(setting_termexempt_b) tag9F6E[3] |= 0x80;

        amexParam.EnhanceContactlessCap = tag9F6E;
        amexParam.MerchantName = sp.getString("setting_merchantname","Telpo");
        amexParam.MerchantCode = StringUtil.hexStringToByte(sp.getString("setting_merchantcode","4112"));
        amexParam.HoldTimeMs = Integer.parseInt(sp.getString("setting_holdtime","300"));
        amexParam.MagStripeRangeNumber = Integer.parseInt(sp.getString("setting_magrangenum","60"));
        amexParam.TAC_Denial = StringUtil.hexStringToByte(sp.getString("setting_tac_denial","0000000000"));
        amexParam.TAC_OnLine = StringUtil.hexStringToByte(sp.getString("setting_tac_online","0000000000"));
        amexParam.TAC_Default = StringUtil.hexStringToByte(sp.getString("setting_tac_default","0000000000"));
        amexParam.IsUnableOnline = sp.getBoolean("setting_unableonline",false)?1:0;
        amexParam.CheckCDAMode = Integer.parseInt(sp.getString("setting_checkcda","0"));

        {
            emvService.Emv_CertRevoList_Clear();
            if(sp.getBoolean("setting_key_revocation",false)){
                emvService.Emv_CertRevoList_Add(new EmvCertRevo("A000000025",0x01,"001000"));
                emvService.Emv_CertRevoList_Add(new EmvCertRevo("A000000025",0x01,"110001"));
            }
        }

        emvService.Amex_TransInit(amexParam,amexAmount);
    }

    void AmexTransactionLimitsSet(){
        emvService.Amex_DefaultLimit_Clear();
        emvService.Amex_DynamicLimit_Clear();
        emvService.Amex_AidLimit_Clear();

        emvService.Amex_AidLimit_Add(new AmexLimits(15000,10000,1000), StringUtil.hexStringToByte("A00000002501"));

        boolean setting_dynamiclimits_b = sp.getBoolean("setting_dynamiclimits",false);

        if(setting_dynamiclimits_b){
            emvService.Amex_DynamicLimit_Add(new AmexLimits(15000,10000,1000),0);
            emvService.Amex_DynamicLimit_Add(new AmexLimits(15500,10500,1500),2);
        }

        boolean setting_defaultlimits_b = sp.getBoolean("setting_defaultlimits",false);
        if(setting_defaultlimits_b){
            emvService.Amex_DefaultLimit_Set(new AmexLimits(15000,10000,1000));
        }
    }

    int AmexTransactionProcess(){
        int ret;
        ret = emvService.Amex_Preprocess();
        if(ret != EmvService.EMV_TRUE){
            AppendDis("Preprocess fail:"+ret);
            return ret ;
        }
        AppendDis("detect card .....");
        readCardDialogShow("Please tap the Amex Card ");
        ret = detectNFC();
        readCardDialogHide();
        if (ret == -4) {
            //ShowResultDialog("Ternimated","User cancel");
            AppendDis("Ternimated, User cancel");
        } else if (ret == -1003) {
            //ShowResultDialog("Ternimated","Timeout");
            AppendDis("Ternimated, Timeout");
        } else if (ret == 0) {

            processingDialogShow("Prcessing , please wait ...");
            ret = emvService.Amex_StartApp();
            //DisAppend("StartApp:"+AmexResult(ret));
            ret = emvService.Amex_GetOutComeResult();
            if( ret == AmexResult.AMEX_RESULT_AGAIN  ){
                EmvService.NfcCloseReader();
                AppendDis("tap again ...");
                AppendDis("detect card again.....");
                readCardDialogShow("Please tap the Amex Card again");
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                ret = detectNFC();
                readCardDialogHide();
                if( ret != 0 ){
                    AppendDis("detect error  " + " code:"+ret);
                    EmvService.NfcCloseReader();
                    return ret;
                }
                //CVM_PROC_79, 80
                ret = emvService.Amex_RetryApp();

            }
            processingDialogHide();
            EmvService.NfcCloseReader();
            ret = emvService.Amex_GetOutComeResult();
            AppendDis("Final Result:"+AmexResult(ret));

            if(emvService.Amex_GetTransMode() == AmexResult.AMEX_CARD_MAG){
                String Track1 = emvService.Amex_MagStripeMakeTrack1();
                String Track2 = emvService.Amex_MagStripeMakeTrack2();
                AppendDis("Track1: "+Track1);
                AppendDis("Track2: "+Track2);
            }

            EmvTLV tag = new EmvTLV(0x95);
            emvService.Emv_GetTLV(tag);
            AppendDis("TVR: "+ StringUtil.bytesToHexString_upcase(tag.Value));

            tag = new EmvTLV(0x9B);
            emvService.Emv_GetTLV(tag);
            AppendDis("TSI: "+ StringUtil.bytesToHexString_upcase(tag.Value));

            tag = new EmvTLV(0x9F34);
            emvService.Emv_GetTLV(tag);
            AppendDis("CVMR: "+ StringUtil.bytesToHexString_upcase(tag.Value));

        } else {
            //ShowResultDialog("detect error","code:"+ret);
            AppendDis("detect card error:"+ret);
        }
        return ret;
    }

    int detectNFC() {

        emvService.NfcOpenReader(1000);
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
    String messStr(int messID){
        String mes = " Unknown";

        switch (messID){
            case AmexResult.AMEX_MESS_APPROVED:
                mes = " APPROVED";
                break;
            case AmexResult.AMEX_MESS_CALLBANK:
                mes = " CALLBANK";
                break;
            case AmexResult.AMEX_MESS_DECLINED:
                mes = " DECLINED";
                break;
            case AmexResult.AMEX_MESS_ENTER_PIN:
                mes = " ENTER_PIN";
                break;
            case AmexResult.AMEX_MESS_ERROR:
                mes = " ERROR";
                break;
            case AmexResult.AMEX_MESS_REMOVE_CARD:
                mes = " REMOVE_CARD";
                break;
            case AmexResult.AMEX_MESS_WELCOME:
                mes = " WELCOME";
                break;
            case AmexResult.AMEX_MESS_PRESENT_CARD:
                mes = " PRESENT_CARD";
                break;
            case AmexResult.AMEX_MESS_PROCESSING:
                mes = " PROCESSING";
                break;
            case AmexResult.AMEX_MESS_CARD_READ_OK:
                mes = " CARD_READ_OK";
                break;
            case AmexResult.AMEX_MESS_OTHER_INTERFACE:
                mes = " OTHER_INTERFACE";
                break;
            case AmexResult.AMEX_MESS_ONE_CARD_ONLY:
                mes = " ONE_CARD_ONLY";
                break;
            case AmexResult.AMEX_MESS_APPROVE_SIGN:
                mes = " AMEX_MESS_APPROVED";
                break;
            case AmexResult.AMEX_MESS_AUTHORISING:
                mes = " AUTHORISING";
                break;
            case AmexResult.AMEX_MESS_OTHER_CARD:
                mes = " OTHER_CARD";
                break;
            case AmexResult.AMEX_MESS_INSERT_CARD:
                mes = " INSERT_CARD";
                break;
            case AmexResult.AMEX_MESS_CLEAR:
                mes = " CLEAR";
                break;
            case AmexResult.AMEX_MESS_SEE_PHONE:
                mes = " SEE_PHONE";
                break;
            case AmexResult.AMEX_MESS_TRY_AGAIN:
                mes = " TRY_AGAIN";
                break;
        }

        return mes;
    }

    String AmexResult(int ret){

        String result = "Other:"+ret;
        switch (ret){
            //------------ Amex Result -----------
            case AmexResult.AMEX_RESULT_AGAIN:

                result = "Try Again";
                break;

            case AmexResult.AMEX_RESULT_APPROVED:

                result = "APPROVE";
                break;

            case AmexResult.AMEX_RESULT_DECLINED:

                result = "DECLINED";
                break;

            case AmexResult.AMEX_RESULT_ONLINE:

                result = "RESULT_ONLINE";
                break;

            case AmexResult.AMEX_RESULT_ANOTHER_INTERFACE:

                result = "Another otherInterface";
                break;

            case AmexResult.AMEX_RESULT_ANOTHER_CARD:

                result = "Another Payment";
                break;

            case AmexResult.AMEX_RESULT_ENDAPP:

                result = "End Application";
                break;

            case AmexResult.AMEX_RESULT_DELAYAUTHOR:

                result = "Delay Authorization";
                break;
        }

        return "  "+result;
    }

    public static void Add_AID() {
        String name = "";
        EmvService.Emv_RemoveAllApp();
        EmvApp App_Amex = new EmvApp();
        name = "Amex";

        try {
            App_Amex.AppName = name.getBytes("ascii");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        App_Amex.AID = new byte[]{(byte) 0xA0, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte)0x25,0x01};
        App_Amex.SelFlag = (byte) 0x00;
        App_Amex.Priority = (byte) 0x00;
        App_Amex.TargetPer = (byte) 00;
        App_Amex.MaxTargetPer = (byte) 00;
        App_Amex.FloorLimitCheck = (byte) 1;
        App_Amex.RandTransSel = (byte) 1;
        App_Amex.VelocityCheck = (byte) 1;
        App_Amex.FloorLimit = new byte[]{(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0x50, (byte) 0x00};
        App_Amex.Threshold = new byte[]{(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0x00};
        App_Amex.TACDenial = new byte[]{(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00};
        App_Amex.TACOnline = new byte[]{(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00};
        App_Amex.TACDefault = new byte[]{(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00};
        App_Amex.AcquierId = new byte[]{(byte) 0x01, (byte) 0x23, (byte) 0x45, (byte) 0x67, (byte) 0x89, (byte) 0x10};
        App_Amex.DDOL = new byte[]{(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00};
        App_Amex.TDOL = new byte[]{(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00};
        App_Amex.Version = new byte[]{(byte) 0x00, (byte) 0x01};

        EmvService.Emv_AddApp(App_Amex);

    }

    public static void Add_Capk()
    {
        int result = 0;
        int capkID = 0;
        boolean dbResult = false;

        EmvService.Emv_RemoveAllCapk();

        /*----------------------------------------------------------------------- division line-----------------------------------------------------------------------------------------*/

        EmvCAPK Amex_Capk1 = new EmvCAPK();
        Amex_Capk1.RID =  new byte[]{(byte)0xA0,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x25};
        Amex_Capk1.KeyID = (byte)0x01;
        Amex_Capk1.HashInd = (byte)0x01;
        Amex_Capk1.ArithInd = (byte)0x01;
        Amex_Capk1.Modul = new byte[]
                {
                        (byte)0xA2,(byte)0x0D,(byte)0xAA,(byte)0xD5,(byte)0xD5,(byte)0xF6,(byte)0x2E,(byte)0x40,(byte)0x85,(byte)0x25,
                        (byte)0x21,(byte)0xDC,(byte)0x9D,(byte)0x5A,(byte)0xB9,(byte)0xF8,(byte)0x7C,(byte)0x61,(byte)0x08,(byte)0x88,
                        (byte)0xA3,(byte)0x23,(byte)0x67,(byte)0x60,(byte)0x1E,(byte)0x27,(byte)0x31,(byte)0x1D,(byte)0x6D,(byte)0x3D,
                        (byte)0xFB,(byte)0x5B,(byte)0xB6,(byte)0x14,(byte)0x2D,(byte)0xB4,(byte)0x00,(byte)0x46,(byte)0x51,(byte)0xA0,
                        (byte)0x9C,(byte)0x8B,(byte)0x3E,(byte)0xD2,(byte)0x29,(byte)0xA9,(byte)0x72,(byte)0x00,(byte)0xB3,(byte)0x83,
                        (byte)0x68,(byte)0x9A,(byte)0xFB,(byte)0x2E,(byte)0x55,(byte)0xA3,(byte)0xF0,(byte)0xC1,(byte)0x6D,(byte)0x03,
                        (byte)0x3A,(byte)0x60,(byte)0xA1,(byte)0x43,(byte)0x8C,(byte)0x7C,(byte)0x5D,(byte)0x08,(byte)0xE4,(byte)0x96,
                        (byte)0x7D,(byte)0x29,(byte)0x53,(byte)0x30,(byte)0x1D,(byte)0x32,(byte)0xDF,(byte)0xE0,(byte)0x79,(byte)0x99,
                        (byte)0x03,(byte)0x9F,(byte)0xFE,(byte)0x12,(byte)0x20,(byte)0x24,(byte)0x91,(byte)0xCE,(byte)0xEF,(byte)0xCC,
                        (byte)0x4D,(byte)0x01,(byte)0x4A,(byte)0xF2,(byte)0xA3,(byte)0x85,(byte)0xB3,(byte)0xEA,(byte)0xE2,(byte)0xAD,
                        (byte)0xA0,(byte)0x13,(byte)0x4A,(byte)0x76,(byte)0x42,(byte)0xB5,(byte)0x13,(byte)0xA7,(byte)0x33,(byte)0x08,
                        (byte)0x79,(byte)0xF4,(byte)0x60,(byte)0x35,(byte)0xE2,(byte)0x0F,(byte)0x27,(byte)0x57,(byte)0x8D,(byte)0x23,
                        (byte)0x3E,(byte)0xCF,(byte)0x35,(byte)0xE6,(byte)0xCE,(byte)0x9B,(byte)0x17,(byte)0xD9,
                };
        Amex_Capk1.Exponent = new byte[]{0x03};
        Amex_Capk1.ExpDate = new byte[]{0x25,0x12,0x31};
        Amex_Capk1.CheckSum = new byte[]
                {
                        (byte)0x77,(byte)0x76,(byte)0xF0,(byte)0xA4,(byte)0x8A,(byte)0x23,(byte)0x3A,(byte)0x13,(byte)0xEA,(byte)0xAE,
                        (byte)0xA5,(byte)0xD9,(byte)0x01,(byte)0xB3,(byte)0x0E,(byte)0xD2,(byte)0x83,(byte)0x64,(byte)0x0C,(byte)0x37
                };

        result =  EmvService.Emv_AddCapk(Amex_Capk1);


        /*----------------------------------------------------------------------- division line-----------------------------------------------------------------------------------------*/

        EmvCAPK Amex_Capk1_1 = new EmvCAPK();
        Amex_Capk1_1.RID =  new byte[]{(byte)0xA0,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x25};
        Amex_Capk1_1.KeyID = (byte)0x02;
        Amex_Capk1_1.HashInd = (byte)0x01;
        Amex_Capk1_1.ArithInd = (byte)0x01;
        Amex_Capk1_1.Modul = new byte[]
                {
                        (byte)0x94,(byte)0xEA,(byte)0x62,(byte)0xF6,(byte)0xD5,(byte)0x83,(byte)0x20,(byte)0xE3,(byte)0x54,(byte)0xC0,
                        (byte)0x22,(byte)0xAD,(byte)0xDC,(byte)0xF0,(byte)0x55,(byte)0x9D,(byte)0x8C,(byte)0xF2,(byte)0x06,(byte)0xCD,
                        (byte)0x92,(byte)0xE8,(byte)0x69,(byte)0x56,(byte)0x49,(byte)0x05,(byte)0xCE,(byte)0x21,(byte)0xD7,(byte)0x20,
                        (byte)0xF9,(byte)0x71,(byte)0xB7,(byte)0xAE,(byte)0xA3,(byte)0x74,(byte)0x83,(byte)0x0E,(byte)0xBE,(byte)0x17,
                        (byte)0x57,(byte)0x11,(byte)0x5A,(byte)0x85,(byte)0xE0,(byte)0x88,(byte)0xD4,(byte)0x1C,(byte)0x6B,(byte)0x77,
                        (byte)0xCF,(byte)0x5E,(byte)0xC8,(byte)0x21,(byte)0xF3,(byte)0x0B,(byte)0x1D,(byte)0x89,(byte)0x04,(byte)0x17,
                        (byte)0xBF,(byte)0x2F,(byte)0xA3,(byte)0x1E,(byte)0x59,(byte)0x08,(byte)0xDE,(byte)0xD5,(byte)0xFA,(byte)0x67,
                        (byte)0x7F,(byte)0x8C,(byte)0x7B,(byte)0x18,(byte)0x4A,(byte)0xD0,(byte)0x90,(byte)0x28,(byte)0xFD,(byte)0xDE,
                        (byte)0x96,(byte)0xB6,(byte)0xA6,(byte)0x10,(byte)0x98,(byte)0x50,(byte)0xAA,(byte)0x80,(byte)0x01,(byte)0x75,
                        (byte)0xEA,(byte)0xBC,(byte)0xDB,(byte)0xBB,(byte)0x68,(byte)0x4A,(byte)0x96,(byte)0xC2,(byte)0xEB,(byte)0x63,
                        (byte)0x79,(byte)0xDF,(byte)0xEA,(byte)0x08,(byte)0xD3,(byte)0x2F,(byte)0xE2,(byte)0x33,(byte)0x1F,(byte)0xE1,
                        (byte)0x03,(byte)0x23,(byte)0x3A,(byte)0xD5,(byte)0x8D,(byte)0xCD,(byte)0xB1,(byte)0xE6,(byte)0xE0,(byte)0x77,
                        (byte)0xCB,(byte)0x9F,(byte)0x24,(byte)0xEA,(byte)0xEC,(byte)0x5C,(byte)0x25,(byte)0xAF,
                };
        Amex_Capk1_1.Exponent = new byte[]{0x01,0x00,0x01};
        Amex_Capk1_1.ExpDate = new byte[]{0x25,0x12,0x31};
        Amex_Capk1_1.CheckSum = new byte[]
                {
                        (byte)0x3B,(byte)0x6F,(byte)0x29,(byte)0x84,(byte)0x25,(byte)0x64,(byte)0xBE,(byte)0x86,(byte)0xAC,(byte)0x9D,
                        (byte)0xAF,(byte)0xCA,(byte)0xFA,(byte)0x65,(byte)0x79,(byte)0x65,(byte)0xD8,(byte)0x63,(byte)0xC6,(byte)0x3B
                };

        result = EmvService.Emv_AddCapk(Amex_Capk1_1);


        /*----------------------------------------------------------------------- division line-----------------------------------------------------------------------------------------*/


        EmvCAPK Amex_Capk3 = new EmvCAPK();
        Amex_Capk3.RID =  new byte[]{(byte)0xA0,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x25};
        Amex_Capk3.KeyID = (byte)0x03;
        Amex_Capk3.HashInd = (byte)0x01;
        Amex_Capk3.ArithInd = (byte)0x01;
        Amex_Capk3.Modul = new byte[]
                {
                        (byte)0x9C,(byte)0x6B,(byte)0xE5,(byte)0xAD,(byte)0xB1,(byte)0x0B,(byte)0x4B,(byte)0xE3,(byte)0xDC,(byte)0xE2,
                        (byte)0x09,(byte)0x9B,(byte)0x4B,(byte)0x21,(byte)0x06,(byte)0x72,(byte)0xB8,(byte)0x96,(byte)0x56,(byte)0xEB,
                        (byte)0xA0,(byte)0x91,(byte)0x20,(byte)0x4F,(byte)0x61,(byte)0x3E,(byte)0xCC,(byte)0x62,(byte)0x3B,(byte)0xED,
                        (byte)0xC9,(byte)0xC6,(byte)0xD7,(byte)0x7B,(byte)0x66,(byte)0x0E,(byte)0x8B,(byte)0xAE,(byte)0xEA,(byte)0x7F,
                        (byte)0x7C,(byte)0xE3,(byte)0x0F,(byte)0x1B,(byte)0x15,(byte)0x38,(byte)0x79,(byte)0xA4,(byte)0xE3,(byte)0x64,
                        (byte)0x59,(byte)0x34,(byte)0x3D,(byte)0x1F,(byte)0xE4,(byte)0x7A,(byte)0xCD,(byte)0xBD,(byte)0x41,(byte)0xFC,
                        (byte)0xD7,(byte)0x10,(byte)0x03,(byte)0x0C,(byte)0x2B,(byte)0xA1,(byte)0xD9,(byte)0x46,(byte)0x15,(byte)0x97,
                        (byte)0x98,(byte)0x2C,(byte)0x6E,(byte)0x1B,(byte)0xDD,(byte)0x08,(byte)0x55,(byte)0x4B,(byte)0x72,(byte)0x6F,
                        (byte)0x5E,(byte)0xFF,(byte)0x79,(byte)0x13,(byte)0xCE,(byte)0x59,(byte)0xE7,(byte)0x9E,(byte)0x35,(byte)0x72,
                        (byte)0x95,(byte)0xC3,(byte)0x21,(byte)0xE2,(byte)0x6D,(byte)0x0B,(byte)0x8B,(byte)0xE2,(byte)0x70,(byte)0xA9,
                        (byte)0x44,(byte)0x23,(byte)0x45,(byte)0xC7,(byte)0x53,(byte)0xE2,(byte)0xAA,(byte)0x2A,(byte)0xCF,(byte)0xC9,
                        (byte)0xD3,(byte)0x08,(byte)0x50,(byte)0x60,(byte)0x2F,(byte)0xE6,(byte)0xCA,(byte)0xC0,(byte)0x0C,(byte)0x6D,
                        (byte)0xDF,(byte)0x6B,(byte)0x8D,(byte)0x9D,(byte)0x9B,(byte)0x48,(byte)0x79,(byte)0xB2,(byte)0x82,(byte)0x6B,
                        (byte)0x04,(byte)0x2A,(byte)0x07,(byte)0xF0,(byte)0xE5,(byte)0xAE,(byte)0x52,(byte)0x6A,(byte)0x3D,(byte)0x3C,
                        (byte)0x4D,(byte)0x22,(byte)0xC7,(byte)0x2B,(byte)0x9E,(byte)0xAA,(byte)0x52,(byte)0xEE,(byte)0xD8,(byte)0x89,
                        (byte)0x38,(byte)0x66,(byte)0xF8,(byte)0x66,(byte)0x38,(byte)0x7A,(byte)0xC0,(byte)0x5A,(byte)0x13,(byte)0x99,
                };

        Amex_Capk3.Exponent = new byte[]{0x03};
        Amex_Capk3.ExpDate = new byte[]{0x25,0x12,0x31};

        Amex_Capk3.CheckSum = new byte[]
                {
                        (byte)0x1D,(byte)0x1D,(byte)0x46,(byte)0x0D,(byte)0x33,(byte)0x8C,(byte)0xF4,(byte)0x36,(byte)0xF9,(byte)0xA0,
                        (byte)0x58,(byte)0x7E,(byte)0x83,(byte)0xA8,(byte)0x59,(byte)0x27,(byte)0xDD,(byte)0xD1,(byte)0x74,(byte)0xE8
                };

        result = EmvService.Emv_AddCapk(Amex_Capk3);

        /*----------------------------------------------------------------------- division line-----------------------------------------------------------------------------------------*/


        EmvCAPK Amex_Capk4 = new EmvCAPK();
        Amex_Capk4.RID =  new byte[]{(byte)0xA0,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x25};
        Amex_Capk4.KeyID = (byte)0x04;
        Amex_Capk4.HashInd = (byte)0x01;
        Amex_Capk4.ArithInd = (byte)0x01;
        Amex_Capk4.Modul = new byte[]
                {
                        (byte)0xA9,(byte)0x9A,(byte)0x6D,(byte)0x3E,(byte)0x07,(byte)0x18,(byte)0x89,(byte)0xED,(byte)0x9E,(byte)0x3A,
                        (byte)0x0C,(byte)0x39,(byte)0x1C,(byte)0x69,(byte)0xB0,(byte)0xB8,(byte)0x04,(byte)0xFC,(byte)0x16,(byte)0x0B,
                        (byte)0x2B,(byte)0x4B,(byte)0xDD,(byte)0x57,(byte)0x0C,(byte)0x92,(byte)0xDD,(byte)0x5A,(byte)0x0F,(byte)0x45,
                        (byte)0xF5,(byte)0x3E,(byte)0x86,(byte)0x21,(byte)0xF7,(byte)0xC9,(byte)0x6C,(byte)0x40,(byte)0x22,(byte)0x42,
                        (byte)0x66,(byte)0x73,(byte)0x5E,(byte)0x1E,(byte)0xE1,(byte)0xB3,(byte)0xC0,(byte)0x62,(byte)0x38,(byte)0xAE,
                        (byte)0x35,(byte)0x04,(byte)0x63,(byte)0x20,(byte)0xFD,(byte)0x8E,(byte)0x81,(byte)0xF8,(byte)0xCE,(byte)0xB3,
                        (byte)0xF8,(byte)0xB4,(byte)0xC9,(byte)0x7B,(byte)0x94,(byte)0x09,(byte)0x30,(byte)0xA3,(byte)0xAC,(byte)0x5E,
                        (byte)0x79,(byte)0x00,(byte)0x86,(byte)0xDA,(byte)0xD4,(byte)0x1A,(byte)0x6A,(byte)0x4F,(byte)0x51,(byte)0x17,
                        (byte)0xBA,(byte)0x1C,(byte)0xE2,(byte)0x43,(byte)0x8A,(byte)0x51,(byte)0xAC,(byte)0x05,(byte)0x3E,(byte)0xB0,
                        (byte)0x02,(byte)0xAE,(byte)0xD8,(byte)0x66,(byte)0xD2,(byte)0xC4,(byte)0x58,(byte)0xFD,(byte)0x73,(byte)0x35,
                        (byte)0x90,(byte)0x21,(byte)0xA1,(byte)0x20,(byte)0x29,(byte)0xA0,(byte)0xC0,(byte)0x43,(byte)0x04,(byte)0x5C,
                        (byte)0x11,(byte)0x66,(byte)0x4F,(byte)0xE0,(byte)0x21,(byte)0x9E,(byte)0xC6,(byte)0x3C,(byte)0x10,(byte)0xBF,
                        (byte)0x21,(byte)0x55,(byte)0xBB,(byte)0x27,(byte)0x84,(byte)0x60,(byte)0x9A,(byte)0x10,(byte)0x64,(byte)0x21,
                        (byte)0xD4,(byte)0x51,(byte)0x63,(byte)0x79,(byte)0x97,(byte)0x38,(byte)0xC1,(byte)0xC3,(byte)0x09,(byte)0x09,
                        (byte)0xBB,(byte)0x6C,(byte)0x6F,(byte)0xE5,(byte)0x2B,(byte)0xBB,(byte)0x76,(byte)0x39,(byte)0x7B,(byte)0x97,
                        (byte)0x40,(byte)0xCE,(byte)0x06,(byte)0x4A,(byte)0x61,(byte)0x3F,(byte)0xF8,(byte)0x41,(byte)0x11,(byte)0x85,
                        (byte)0xF0,(byte)0x88,(byte)0x42,(byte)0xA4,(byte)0x23,(byte)0xEA,(byte)0xD2,(byte)0x0E,(byte)0xDF,(byte)0xFB,
                        (byte)0xFF,(byte)0x1C,(byte)0xD6,(byte)0xC3,(byte)0xFE,(byte)0x0C,(byte)0x98,(byte)0x21,(byte)0x47,(byte)0x91,
                        (byte)0x99,(byte)0xC2,(byte)0x6D,(byte)0x85,(byte)0x72,(byte)0xCC,(byte)0x8A,(byte)0xFF,(byte)0xF0,(byte)0x87,
                        (byte)0xA9,(byte)0xC3,
                };

        Amex_Capk4.Exponent = new byte[]{0x03};
        Amex_Capk4.ExpDate = new byte[]{0x25,0x12,0x31};

        Amex_Capk4.CheckSum = new byte[]
                {
                        (byte)0xAD,(byte)0xBF,(byte)0x17,(byte)0x06,(byte)0xAD,(byte)0xD0,(byte)0x2A,(byte)0xA0,(byte)0xF2,(byte)0x2A,
                        (byte)0xE7,(byte)0xA1,(byte)0xF1,(byte)0x25,(byte)0x63,(byte)0x62,(byte)0xD4,(byte)0xAE,(byte)0x33,(byte)0xF7
                };

        result = EmvService.Emv_AddCapk(Amex_Capk4);


        /*----------------------------------------------------------------------- division line-----------------------------------------------------------------------------------------*/


        EmvCAPK Amex_Capk5 = new EmvCAPK();
        Amex_Capk5.RID =  new byte[]{(byte)0xA0,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x25};
        Amex_Capk5.KeyID = (byte)0x05;
        Amex_Capk5.HashInd = (byte)0x01;
        Amex_Capk5.ArithInd = (byte)0x01;
        Amex_Capk5.Modul = new byte[]
                {
                        (byte)0xA2,(byte)0x5A,(byte)0x6B,(byte)0xD7,(byte)0x83,(byte)0xA5,(byte)0xEF,(byte)0x6B,(byte)0x8F,(byte)0xB6,
                        (byte)0xF8,(byte)0x30,(byte)0x55,(byte)0xC2,(byte)0x60,(byte)0xF5,(byte)0xF9,(byte)0x9E,(byte)0xA1,(byte)0x66,
                        (byte)0x78,(byte)0xF3,(byte)0xB9,(byte)0x05,(byte)0x3E,(byte)0x0F,(byte)0x64,(byte)0x98,(byte)0xE8,(byte)0x2C,
                        (byte)0x3F,(byte)0x5D,(byte)0x1E,(byte)0x8C,(byte)0x38,(byte)0xF1,(byte)0x35,(byte)0x88,(byte)0x01,(byte)0x7E,
                        (byte)0x2B,(byte)0x12,(byte)0xB3,(byte)0xD8,(byte)0xFF,(byte)0x6F,(byte)0x50,(byte)0x16,(byte)0x7F,(byte)0x46,
                        (byte)0x44,(byte)0x29,(byte)0x10,(byte)0x72,(byte)0x9E,(byte)0x9E,(byte)0x4D,(byte)0x1B,(byte)0x37,(byte)0x39,
                        (byte)0xE5,(byte)0x06,(byte)0x7C,(byte)0x0A,(byte)0xC7,(byte)0xA1,(byte)0xF4,(byte)0x48,(byte)0x7E,(byte)0x35,
                        (byte)0xF6,(byte)0x75,(byte)0xBC,(byte)0x16,(byte)0xE2,(byte)0x33,(byte)0x31,(byte)0x51,(byte)0x65,(byte)0xCB,
                        (byte)0x14,(byte)0x2B,(byte)0xFD,(byte)0xB2,(byte)0x5E,(byte)0x30,(byte)0x1A,(byte)0x63,(byte)0x2A,(byte)0x54,
                        (byte)0xA3,(byte)0x37,(byte)0x1E,(byte)0xBA,(byte)0xB6,(byte)0x57,(byte)0x2D,(byte)0xEE,(byte)0xBA,(byte)0xF3,
                        (byte)0x70,(byte)0xF3,(byte)0x37,(byte)0xF0,(byte)0x57,(byte)0xEE,(byte)0x73,(byte)0xB4,(byte)0xAE,(byte)0x46,
                        (byte)0xD1,(byte)0xA8,(byte)0xBC,(byte)0x4D,(byte)0xA8,(byte)0x53,(byte)0xEC,(byte)0x3C,(byte)0xC1,(byte)0x2C,
                        (byte)0x8C,(byte)0xBC,(byte)0x2D,(byte)0xA1,(byte)0x83,(byte)0x22,(byte)0xD6,(byte)0x85,(byte)0x30,(byte)0xC7,
                        (byte)0x0B,(byte)0x22,(byte)0xBD,(byte)0xAC,(byte)0x35,(byte)0x1D,(byte)0xD3,(byte)0x60,(byte)0x68,(byte)0xAE,
                        (byte)0x32,(byte)0x1E,(byte)0x11,(byte)0xAB,(byte)0xF2,(byte)0x64,(byte)0xF4,(byte)0xD3,(byte)0x56,(byte)0x9B,
                        (byte)0xB7,(byte)0x12,(byte)0x14,(byte)0x54,(byte)0x50,(byte)0x05,(byte)0x55,(byte)0x8D,(byte)0xE2,(byte)0x60,
                        (byte)0x83,(byte)0xC7,(byte)0x35,(byte)0xDB,(byte)0x77,(byte)0x63,(byte)0x68,(byte)0x17,(byte)0x2F,(byte)0xE8,
                        (byte)0xC2,(byte)0xF5,(byte)0xC8,(byte)0x5E,(byte)0x8B,(byte)0x5B,(byte)0x89,(byte)0x0C,(byte)0xC6,(byte)0x82,
                        (byte)0x91,(byte)0x1D,(byte)0x2D,(byte)0xE7,(byte)0x1F,(byte)0xA6,(byte)0x26,(byte)0xB8,(byte)0x81,(byte)0x7F,
                        (byte)0xCC,(byte)0xC0,(byte)0x89,(byte)0x22,(byte)0xB7,(byte)0x03,(byte)0x86,(byte)0x9F,(byte)0x3B,(byte)0xAE,
                        (byte)0xAC,(byte)0x14,(byte)0x59,(byte)0xD7,(byte)0x7C,(byte)0xD8,(byte)0x53,(byte)0x76,(byte)0xBC,(byte)0x36,
                        (byte)0x18,(byte)0x2F,(byte)0x42,(byte)0x38,(byte)0x31,(byte)0x4D,(byte)0x6C,(byte)0x42,(byte)0x12,(byte)0xFB,
                        (byte)0xDD,(byte)0x7F,(byte)0x23,(byte)0xD3,
                };

        Amex_Capk5.Exponent = new byte[]{0x03};
        Amex_Capk5.ExpDate = new byte[]{0x25,0x12,0x31};

        Amex_Capk5.CheckSum = new byte[]
                {
                        (byte)0x05,(byte)0x56,(byte)0x76,(byte)0x28,(byte)0x48,(byte)0x0B,(byte)0x75,(byte)0x7F,(byte)0xE6,(byte)0x33,
                        (byte)0x99,(byte)0x9C,(byte)0x9A,(byte)0xE1,(byte)0xD9,(byte)0xF4,(byte)0x20,(byte)0xF8,(byte)0x4E,(byte)0xE3
                };

        result = EmvService.Emv_AddCapk(Amex_Capk5);


        /*----------------------------------------------------------------------- division line-----------------------------------------------------------------------------------------*/

        EmvCAPK Amex_Capk6 = new EmvCAPK();
        Amex_Capk6.RID =  new byte[]{(byte)0xA0,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x25};
        Amex_Capk6.KeyID = (byte)0x06;
        Amex_Capk6.HashInd = (byte)0x01;
        Amex_Capk6.ArithInd = (byte)0x01;
        Amex_Capk6.Modul = new byte[]
                {
                        (byte)0xA1,(byte)0x91,(byte)0xCB,(byte)0x87,(byte)0x47,(byte)0x3F,(byte)0x29,(byte)0x34,(byte)0x9B,(byte)0x5D,
                        (byte)0x60,(byte)0xA8,(byte)0x8B,(byte)0x3E,(byte)0xAE,(byte)0xE0,(byte)0x97,(byte)0x3A,(byte)0xA6,(byte)0xF1,
                        (byte)0xA0,(byte)0x82,(byte)0xF3,(byte)0x58,(byte)0xD8,(byte)0x49,(byte)0xFD,(byte)0xDF,(byte)0xF9,(byte)0xC0,
                        (byte)0x91,(byte)0xF8,(byte)0x99,(byte)0xED,(byte)0xA9,(byte)0x79,(byte)0x2C,(byte)0xAF,(byte)0x09,(byte)0xEF,
                        (byte)0x28,(byte)0xF5,(byte)0xD2,(byte)0x24,(byte)0x04,(byte)0xB8,(byte)0x8A,(byte)0x22,(byte)0x93,(byte)0xEE,
                        (byte)0xBB,(byte)0xC1,(byte)0x94,(byte)0x9C,(byte)0x43,(byte)0xBE,(byte)0xA4,(byte)0xD6,(byte)0x0C,(byte)0xFD,
                        (byte)0x87,(byte)0x9A,(byte)0x15,(byte)0x39,(byte)0x54,(byte)0x4E,(byte)0x09,(byte)0xE0,(byte)0xF0,(byte)0x9F,
                        (byte)0x60,(byte)0xF0,(byte)0x65,(byte)0xB2,(byte)0xBF,(byte)0x2A,(byte)0x13,(byte)0xEC,(byte)0xC7,(byte)0x05,
                        (byte)0xF3,(byte)0xD4,(byte)0x68,(byte)0xB9,(byte)0xD3,(byte)0x3A,(byte)0xE7,(byte)0x7A,(byte)0xD9,(byte)0xD3,
                        (byte)0xF1,(byte)0x9C,(byte)0xA4,(byte)0x0F,(byte)0x23,(byte)0xDC,(byte)0xF5,(byte)0xEB,(byte)0x7C,(byte)0x04,
                        (byte)0xDC,(byte)0x8F,(byte)0x69,(byte)0xEB,(byte)0xA5,(byte)0x65,(byte)0xB1,(byte)0xEB,(byte)0xCB,(byte)0x46,
                        (byte)0x86,(byte)0xCD,(byte)0x27,(byte)0x47,(byte)0x85,(byte)0x53,(byte)0x0F,(byte)0xF6,(byte)0xF6,(byte)0xE9,
                        (byte)0xEE,(byte)0x43,(byte)0xAA,(byte)0x43,(byte)0xFD,(byte)0xB0,(byte)0x2C,(byte)0xE0,(byte)0x0D,(byte)0xAE,
                        (byte)0xC1,(byte)0x5C,(byte)0x7B,(byte)0x8F,(byte)0xD6,(byte)0xA9,(byte)0xB3,(byte)0x94,(byte)0xBA,(byte)0xBA,
                        (byte)0x41,(byte)0x9D,(byte)0x3F,(byte)0x6D,(byte)0xC8,(byte)0x5E,(byte)0x16,(byte)0x56,(byte)0x9B,(byte)0xE8,
                        (byte)0xE7,(byte)0x69,(byte)0x89,(byte)0x68,(byte)0x8E,(byte)0xFE,(byte)0xA2,(byte)0xDF,(byte)0x22,(byte)0xFF,
                        (byte)0x7D,(byte)0x35,(byte)0xC0,(byte)0x43,(byte)0x33,(byte)0x8D,(byte)0xEA,(byte)0xA9,(byte)0x82,(byte)0xA0,
                        (byte)0x2B,(byte)0x86,(byte)0x6D,(byte)0xE5,(byte)0x32,(byte)0x85,(byte)0x19,(byte)0xEB,(byte)0xBC,(byte)0xD6,
                        (byte)0xF0,(byte)0x3C,(byte)0xDD,(byte)0x68,(byte)0x66,(byte)0x73,(byte)0x84,(byte)0x7F,(byte)0x84,(byte)0xDB,
                        (byte)0x65,(byte)0x1A,(byte)0xB8,(byte)0x6C,(byte)0x28,(byte)0xCF,(byte)0x14,(byte)0x62,(byte)0x56,(byte)0x2C,
                        (byte)0x57,(byte)0x7B,(byte)0x85,(byte)0x35,(byte)0x64,(byte)0xA2,(byte)0x90,(byte)0xC8,(byte)0x55,(byte)0x6D,
                        (byte)0x81,(byte)0x85,(byte)0x31,(byte)0x26,(byte)0x8D,(byte)0x25,(byte)0xCC,(byte)0x98,(byte)0xA4,(byte)0xCC,
                        (byte)0x6A,(byte)0x0B,(byte)0xDF,(byte)0xFF,(byte)0xDA,(byte)0x2D,(byte)0xCC,(byte)0xA3,(byte)0xA9,(byte)0x4C,
                        (byte)0x99,(byte)0x85,(byte)0x59,(byte)0xE3,(byte)0x07,(byte)0xFD,(byte)0xDF,(byte)0x91,(byte)0x50,(byte)0x06,
                        (byte)0xD9,(byte)0xA9,(byte)0x87,(byte)0xB0,(byte)0x7D,(byte)0xDA,(byte)0xEB,(byte)0x3B,
                };

        Amex_Capk6.Exponent = new byte[]{0x03};
        Amex_Capk6.ExpDate = new byte[]{0x25,0x12,0x31};

        Amex_Capk6.CheckSum = new byte[]
                {
                        (byte)0x92,(byte)0xC8,(byte)0x8F,(byte)0x72,(byte)0xE3,(byte)0xDF,(byte)0xC1,(byte)0x67,(byte)0x8E,(byte)0xA4,
                        (byte)0x0D,(byte)0x5D,(byte)0xA5,(byte)0x51,(byte)0x9B,(byte)0xE1,(byte)0xAE,(byte)0xC0,(byte)0x01,(byte)0xB7
                };

        result = EmvService.Emv_AddCapk(Amex_Capk6);


        /*----------------------------------------------------------------------- division line-----------------------------------------------------------------------------------------*/

        EmvCAPK Amex_Capk7 = new EmvCAPK();
        Amex_Capk7.RID =       new byte[]{(byte)0xA0,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x25};
        Amex_Capk7.KeyID =    (byte)0x07;
        Amex_Capk7.HashInd =  (byte)0x01;
        Amex_Capk7.ArithInd = (byte)0x01;
        Amex_Capk7.Modul = new byte[]
                {
                        (byte)0xA2,(byte)0x0D,(byte)0xAA,(byte)0xD5,(byte)0xD5,(byte)0xF6,(byte)0x2E,(byte)0x40,(byte)0x85,(byte)0x25,
                        (byte)0x21,(byte)0xDC,(byte)0x9D,(byte)0x5A,(byte)0xB9,(byte)0xF8,(byte)0x7C,(byte)0x61,(byte)0x08,(byte)0x88,
                        (byte)0xA3,(byte)0x23,(byte)0x67,(byte)0x60,(byte)0x1E,(byte)0x27,(byte)0x31,(byte)0x1D,(byte)0x6D,(byte)0x3D,
                        (byte)0xFB,(byte)0x5B,(byte)0xB6,(byte)0x14,(byte)0x2D,(byte)0xB4,(byte)0x00,(byte)0x46,(byte)0x51,(byte)0xA0,
                        (byte)0x9C,(byte)0x8B,(byte)0x3E,(byte)0xD2,(byte)0x29,(byte)0xA9,(byte)0x72,(byte)0x00,(byte)0xB3,(byte)0x83,
                        (byte)0x68,(byte)0x9A,(byte)0xFB,(byte)0x2E,(byte)0x55,(byte)0xA3,(byte)0xF0,(byte)0xC1,(byte)0x6D,(byte)0x03,
                        (byte)0x3A,(byte)0x60,(byte)0xA1,(byte)0x43,(byte)0x8C,(byte)0x7C,(byte)0x5D,(byte)0x08,(byte)0xE4,(byte)0x96,
                        (byte)0x7D,(byte)0x29,(byte)0x53,(byte)0x30,(byte)0x1D,(byte)0x32,(byte)0xDF,(byte)0xE0,(byte)0x79,(byte)0x99,
                        (byte)0x03,(byte)0x9F,(byte)0xFE,(byte)0x12,(byte)0x20,(byte)0x24,(byte)0x91,(byte)0xCE,(byte)0xEF,(byte)0xCC,
                        (byte)0x4D,(byte)0x01,(byte)0x4A,(byte)0xF2,(byte)0xA3,(byte)0x85,(byte)0xB3,(byte)0xEA,(byte)0xE2,(byte)0xAD,
                        (byte)0xA0,(byte)0x13,(byte)0x4A,(byte)0x76,(byte)0x42,(byte)0xB5,(byte)0x13,(byte)0xA7,(byte)0x33,(byte)0x08,
                        (byte)0x79,(byte)0xF4,(byte)0x60,(byte)0x35,(byte)0xE2,(byte)0x0F,(byte)0x27,(byte)0x57,(byte)0x8D,(byte)0x23,
                        (byte)0x3E,(byte)0xCF,(byte)0x35,(byte)0xE6,(byte)0xCE,(byte)0x9B,
                };

        Amex_Capk7.Exponent = new byte[]{0x03};
        Amex_Capk7.ExpDate = new byte[]{0x25,0x12,0x31};

        Amex_Capk7.CheckSum = new byte[]
                {
                        (byte)0x24,(byte)0x56,(byte)0x8C,(byte)0x62,(byte)0xD6,(byte)0x9F,(byte)0xBF,(byte)0x64,(byte)0xFD,(byte)0xD0,
                        (byte)0x0A,(byte)0x16,(byte)0xA3,(byte)0xB9,(byte)0xF1,(byte)0x32,(byte)0xA1,(byte)0x24,(byte)0xEE,(byte)0x92
                };

        result = EmvService.Emv_AddCapk(Amex_Capk7);


        /*----------------------------------------------------------------------- division line-----------------------------------------------------------------------------------------*/

        EmvCAPK Amex_Capk9 = new EmvCAPK();
        Amex_Capk9.RID =  new byte[]{(byte)0xA0,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x25};
        Amex_Capk9.KeyID = (byte)0x09;
        Amex_Capk9.HashInd = (byte)0x01;
        Amex_Capk9.ArithInd = (byte)0x01;
        Amex_Capk9.Modul = new byte[]
                {
                        (byte)0xA3,(byte)0x83,(byte)0x4C,(byte)0xB1,(byte)0xD1,(byte)0x90,(byte)0x54,(byte)0x5D,(byte)0x22,(byte)0xF0,
                        (byte)0xEB,(byte)0xE2,(byte)0xFF,(byte)0xF7,(byte)0x5E,(byte)0x5B,(byte)0xE5,(byte)0x75,(byte)0x5A,(byte)0x16,
                        (byte)0x9A,(byte)0x7F,(byte)0xE4,(byte)0x2D,(byte)0x0A,(byte)0x2E,(byte)0xC1,(byte)0x66,(byte)0x96,(byte)0x5A,
                        (byte)0x27,(byte)0xB0,(byte)0x43,(byte)0x78,(byte)0xCE,(byte)0xA5,(byte)0xA3,(byte)0x83,(byte)0x9A,(byte)0x43,
                        (byte)0x27,(byte)0xB5,(byte)0xE6,(byte)0x8F,(byte)0x23,(byte)0xDD,(byte)0x98,(byte)0xE9,(byte)0x49,(byte)0x7B,
                        (byte)0xC8,(byte)0xDB,(byte)0xAD,(byte)0xC0,(byte)0x4E,(byte)0x7F,(byte)0x86,(byte)0xE8,(byte)0xFF,(byte)0x1C,
                        (byte)0x23,(byte)0x13,(byte)0xD7,(byte)0x09,(byte)0x47,(byte)0xBE,(byte)0xB7,(byte)0xAB,(byte)0x7C,(byte)0x9F,
                        (byte)0xC4,(byte)0x4F,(byte)0xD6,(byte)0xAF,(byte)0x43,(byte)0x27,(byte)0x2F,(byte)0x29,(byte)0xB9,(byte)0x0D,
                        (byte)0x65,(byte)0x8C,(byte)0x0F,(byte)0x77,(byte)0x27,(byte)0x3A,(byte)0x20,(byte)0xE7,(byte)0x0C,(byte)0xD8,
                        (byte)0x57,(byte)0xE7,(byte)0x68,(byte)0x45,(byte)0x96,(byte)0xD5,(byte)0x69,(byte)0x4C,(byte)0xA7,(byte)0xBA,
                        (byte)0x50,(byte)0xDB,(byte)0xAA,(byte)0xF4,(byte)0x0C,(byte)0xB4,(byte)0xBB,(byte)0x39,(byte)0xFE,(byte)0x0E,
                        (byte)0xA0,(byte)0xC9,(byte)0x44,(byte)0x13,(byte)0x99,(byte)0xD7,(byte)0x2A,(byte)0x35,(byte)0x1F,(byte)0x38,
                        (byte)0x80,(byte)0x10,(byte)0x74,(byte)0x86,(byte)0x6F,(byte)0x37,(byte)0xD4,(byte)0x8A,(byte)0x4F,(byte)0x15,
                        (byte)0x6F,(byte)0x9C,(byte)0xAD,(byte)0x6F,(byte)0x10,(byte)0xDD,(byte)0x64,(byte)0x46,(byte)0x49,(byte)0xA2,
                        (byte)0x0D,(byte)0xBF,(byte)0x59,(byte)0x23,(byte)0x74,(byte)0x3F,(byte)0x57,(byte)0x8F,(byte)0x14,(byte)0x6A,
                        (byte)0xF8,(byte)0xC4,(byte)0xBA,(byte)0x2F,(byte)0xBB,(byte)0xD4,(byte)0x1F,(byte)0xAA,(byte)0x04,(byte)0x86,
                        (byte)0xA5,(byte)0x59,(byte)0xE0,(byte)0x7D,(byte)0xF5,(byte)0xD3,(byte)0xA4,(byte)0xBF,(byte)0x85,(byte)0xF5,
                        (byte)0x54,(byte)0x59,(byte)0x98,(byte)0x6F,(byte)0xC3,(byte)0x45,
                };

        Amex_Capk9.Exponent = new byte[]{0x03};
        Amex_Capk9.ExpDate = new byte[]{0x25,0x12,0x31};

        Amex_Capk9.CheckSum = new byte[]
                {
                        (byte)0x24,(byte)0x56,(byte)0x8C,(byte)0x62,(byte)0xD6,(byte)0x9F,(byte)0xBF,(byte)0x64,(byte)0xFD,(byte)0xD0,
                        (byte)0x0A,(byte)0x16,(byte)0xA3,(byte)0xB9,(byte)0xF1,(byte)0x32,(byte)0xA1,(byte)0x24,(byte)0xEE,(byte)0x92
                };

        result = EmvService.Emv_AddCapk(Amex_Capk9);

    }


}




