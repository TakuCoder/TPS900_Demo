package com.telpo.tps900_demo;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.telpo.emv.EmvAmountData;
import com.telpo.emv.EmvApp;
import com.telpo.emv.EmvCAPK;
import com.telpo.emv.EmvCandidateApp;
import com.telpo.emv.EmvOnlineData;
import com.telpo.emv.EmvPinData;
import com.telpo.emv.EmvService;
import com.telpo.emv.EmvServiceListener;
import com.telpo.emv.EmvTLV;
import com.telpo.emv.MirParam;
import com.telpo.emv.MirResult;
import com.telpo.util.StringUtil;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ActivityTPMir extends Activity {

    public static MirParam m_mirParam = new MirParam();
    Context context;
    EmvService emvService;
    Button bn_emvDeviceOpen, bn_emvDeviceClose, bn_AddAid, bn_AddCapk, bn_AddCapkTest, bn_readCard, bn_set;
    Button[] buttons;
    EditText et_reslut;
    StringBuffer logBuf = new StringBuffer("");
    ProgressDialog dialog = null;
    boolean userCancel = false;
    TextView title_tv;
    EmvServiceListener listener = new EmvServiceListener() {

        @Override
        public int onInputAmount(EmvAmountData AmountData) {
            return EmvService.EMV_TRUE;
        }

        @Override
        public int onInputPin(EmvPinData PinData) {
            return EmvService.EMV_TRUE;
        }

        @Override
        public int onSelectApp(EmvCandidateApp[] appList) {
            return appList[0].index;
        }

        @Override
        public int onSelectAppFail(int ErrCode) {
            return EmvService.EMV_TRUE;
        }

        @Override
        public int onFinishReadAppData() {
            return EmvService.EMV_TRUE;
        }

        @Override
        public int onVerifyCert() {
            return EmvService.EMV_TRUE;
        }

        @Override
        public int onOnlineProcess(EmvOnlineData OnlineData) {
            return EmvService.EMV_TRUE;
        }

        @Override
        public int onRequireTagValue(int tag, int len, byte[] value) {
            Log.w("emvlistener", "onRequireTagValue: " + tag);
            return EmvService.EMV_TRUE;
        }

        @Override
        public int onRequireDatetime(byte[] datetime) {
            SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
            Date curDate = new Date(System.currentTimeMillis());//获取当前时间
            String str = formatter.format(curDate);
            byte[] time = new byte[0];

            time = str.getBytes(StandardCharsets.US_ASCII);
            System.arraycopy(time, 0, datetime, 0, datetime.length);
            return EmvService.EMV_TRUE;
        }

        @Override
        public int onReferProc() {
            return EmvService.EMV_TRUE;
        }

        @Override
        public int OnCheckException(String PAN) {
            return EmvService.EMV_FALSE;
        }

        @Override
        public int OnCheckException_qvsdc(int index, String PAN) {
            return EmvService.EMV_TRUE;
        }

        @Override
        public int onMir_DataExchange() {
            final String TestPan = "2201389400000030";
            int TransAmount = 0;
            int ret;
            EmvTLV PanTag = null;
            EmvTLV AmountTag = null;
            String Pan;

            AmountTag = new EmvTLV(0x9F02);
            ret = emvService.Emv_GetTLV(AmountTag);

            if (ret != EmvService.EMV_TRUE) {
                return EmvService.EMV_TRUE;
            }

            try {
                TransAmount = Integer.parseInt(StringUtil.bytesToHexString(AmountTag.Value));
            } catch (Exception e) {

            }

            if (TransAmount <= 20000) {
                return EmvService.EMV_TRUE;
            }

            PanTag = new EmvTLV(0x5A);
            ret = emvService.Emv_GetTLV(PanTag);

            if (ret == EmvService.EMV_TRUE) {
                StringBuffer p = new StringBuffer(StringUtil.bytesToHexString(PanTag.Value));

                if (p.charAt(p.toString().length() - 1) == 'F') {
                    p.deleteCharAt(p.toString().length() - 1);
                }

                Pan = p.toString();
            } else {
                if (EmvService.EMV_TRUE != emvService.Mir_IsUseTrack2Pan()) {
                    return EmvService.EMV_TRUE;
                }

                PanTag = new EmvTLV(0x57);
                ret = emvService.Emv_GetTLV(PanTag);

                if (ret != EmvService.EMV_TRUE) {
                    return EmvService.EMV_TRUE;
                }

                StringBuffer p = new StringBuffer(StringUtil.bytesToHexString(PanTag.Value));

                int i = p.indexOf("D");

                if (i <= 0) {
                    return EmvService.EMV_TRUE;
                }

                Pan = p.substring(0, i);
            }

            if (!TestPan.equals(Pan)) {
                return EmvService.EMV_TRUE;
            }

            TransAmount -= 20000;
            Pan = String.format("%012d", TransAmount);

            byte[] Amount = StringUtil.hexStringToByte(Pan);

            AmountTag = new EmvTLV(0x9F02);
            AmountTag.Value = new byte[6];
            System.arraycopy(Amount, 0, AmountTag.Value, 0, 6);
            emvService.Emv_SetTLV(AmountTag);

            return EmvService.EMV_TRUE;
        }

        @Override
        public int onMir_FinishReadAppData() {
            try {
                AppendDis("Read OK, Please, Remove Card");
            } catch (Exception e) {

            }

            return EmvService.EMV_TRUE;
        }

        @Override
        public int onMir_Hint() {
            try {
                AppendDis("Look at the screen");
            } catch (Exception e) {

            }

            return EmvService.EMV_TRUE;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tppaypass);
        context = ActivityTPMir.this;
        viewInit();
        title_tv=findViewById(R.id.title_tv);
        title_tv.setText("MIR Test");
        ActivityInit();
    }

    private void ActivityInit() {
        EmvService.Emv_SetDebugOn(1);
        emvService = EmvService.getInstance();
        emvService.setListener(listener);
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
                Add_Mir_AID();
            }
        });

        bn_AddCapkTest.setVisibility(View.GONE);

        bn_AddCapk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Add_Mir_Capk();
            }
        });

        bn_set.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(ActivityTPMir.this, ActivityTPMirParam.class));
            }
        });
        bn_readCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        int ret;
                        AppendDis("========================");
                        AppendDis("try to detect Mir card");
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                dialog.show();
                            }
                        });

                        int i = EmvService.NfcOpenReader(1000);
                        AppendDis("Open NFC : " + i);
                        if (i != 0) {
                            return;
                        }

                        ret = detectNFC();
                        if (ret == -4) {
                            AppendDis("user cancel");
                        } else if (ret == -1003) {
                            AppendDis("timeout");
                        } else if (ret == 0) {
                            Mir_process_demo();
                        } else {
                            AppendDis("detect error:" + ret);
                        }
                        EmvService.NfcCloseReader();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                dialog.hide();
                            }
                        });

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

        dialog = new ProgressDialog(context);
        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                emvService.NfcCloseReader();
                userCancel = true;
            }
        });
        //dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        dialog.setIndeterminate(false);
        dialog.setTitle("detecting Mir card...");
        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setMessage("pls tap your card");

        setButtonsEnable(false);

        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        //imm.showSoftInput(et_reslut,InputMethodManager.SHOW_FORCED);
        imm.hideSoftInputFromWindow(et_reslut.getWindowToken(), 0); //强制隐藏键盘


    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dialog != null) {
            dialog.dismiss();
        }
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

    void Mir_process_demo() {
        int ret;
        MirParam mirParam = m_mirParam;
        // Log.d("fanz", "paypass_process_demo: "+ m_PaypassParam.toString());
        ret = emvService.Mir_TransInitEx(mirParam);
        AppendDis("Mir_TransInitEx: " + ret);
        if (ret != EmvService.EMV_TRUE) {
            return;
        }

        ret = emvService.Mir_StartEx(900, 0, 840, 2, 0);
        AppendDis("Mir_StartEx: " + ret + "(0x" + Integer.toHexString(ret) + ")");

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
            if (EmvService.EMV_TRUE != emvService.Mir_IsUseTrack2Pan()) {
                Pan = null;
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
        }

        if (Pan == null) {
            AppendDis("PAN data miss");
        } else {
            AppendDis("PAN:" + Pan);
        }

        ret = emvService.Mir_GetOutComeProtocol();
        if (ret == MirResult.MIR_PROTCL_1) {
            AppendDis("Transaction under protocol 1");
        } else {
            AppendDis("Transaction under protocol 2");
        }

        ret = emvService.Mir_GetOutComeResult();
        Show_OutComeResult(ret);

        ret = emvService.Mir_GetOutComeCVM();
        Show_outCVM(ret);

        ret = emvService.Mir_GetOutComeMessID();
        Show_OutComeMess(ret);

        ret = emvService.Mir_GetOutComeStatus();
        Show_OutComeStatus(ret);


    }

    void Show_OutComeResult(int outcome) {
        String mes = "";
        mes = "===> OutComeResult : ";
        switch (outcome) {
            case MirResult.MIR_RESULT_DECLINED:
                mes += "Decline";
                break;
            case MirResult.MIR_RESULT_APPROVED:
                mes += "Approve";
                break;
            case MirResult.MIR_RESULT_ONLINE:
                mes += "Need to go online";
                break;
            case MirResult.MIR_RESULT_2PRESENT:
                mes += "Need 2 present";
                break;
            case MirResult.MIR_RESULT_ENDAPP:
                mes += "Terminate";
                break;
            default:
                mes += "unknow : " + outcome;
                break;
        }
        AppendDis(mes);
    }

    void Show_outCVM(int CVM) {
        String mes = "";
        mes = "===> Need CVM : ";
        switch (CVM) {
            case MirResult.MIR_CVM_SKIPCVM:
                mes += "Skip CVM";
                break;
            case MirResult.MIR_CVM_NOCVM:
                mes += "No CVM";
                break;
            case MirResult.MIR_CVM_ONLINEPIN:
                mes += "Need Online PIN";
                break;
            case MirResult.MIR_CVM_CDCVM:
                mes += "Need CDCVM";
                break;
            case MirResult.MIR_CVM_SIGNATURE:
                mes += "Need signature on receipt";
                break;
            case MirResult.MIR_CVM_FAIL:
                mes += "Fail";
                break;
            default:
                mes += "unknow : " + CVM;
                break;
        }
        AppendDis(mes);
    }

    void Show_OutComeMess(int ID) {
        String mes = "";
        mes = "===> Mess : ";
        switch (ID) {
            case MirResult.MIR_MESS_APPROVED:
                mes += "Transaction approve";
                break;
            case MirResult.MIR_MESS_DECLINED:
                mes += "Transaction decline";
                break;
            case MirResult.MIR_MESS_ENTER_PIN:
                mes += "Please enter Online PIN";
                break;
            case MirResult.MIR_MESS_CARD_READ_OK:
                mes += "Card read OK";
                break;
            case MirResult.MIR_MESS_OTHER_INTERFACE:
                mes += "Please use onther interface";
                break;
            case MirResult.MIR_MESS_SIGN_RECEIPT:
                mes += "Please sign on the receipt";
                break;
            case MirResult.MESS_AUTHORISING:
                mes += "Need further authorising . pls go online";
                break;
            case MirResult.MIR_MESS_OTHER_CARD:
                mes += "Pls use other card";
                break;
            case MirResult.MIR_MESS_INSERT_CARD:
                mes += "Please insert card";
                break;
            case MirResult.MIR_MESS_SEE_PHONE:
                mes += "Please see phone";
                break;
            case MirResult.MIR_MESS_TRY_AGAIN:
                mes += "Please try again";
                break;
            case MirResult.MIR_MESS_SELE_NEXT:
                mes += "Please select the next app";
                break;
            default:
                mes += "unknow : " + ID;
                break;
        }
        AppendDis(mes);
    }

    void Show_OutComeStatus(int ID) {
        String mes = "";
        mes = "===> Status : ";
        switch (ID) {
            case MirResult.MIR_STATUS_READY_READ:
                mes += "Ready to read";
                break;
            case MirResult.MIR_STATUS_READ_OK:
                mes += "Read OK";
                break;
            case MirResult.MIR_STATUS_DATA_ERROR:
                mes += "Data error";
                break;
            case MirResult.MIR_STATUS_GAC_NOANSWER:
                mes += "GAC no answer";
                break;
            case MirResult.MIR_STATUS_GAC_BAD_FORMAT:
                mes += "GAC bad format";
                break;
            case MirResult.MIR_STATUS_PER_RECOVER_NOTSUPPORT:
                mes += "MIR_STATUS_PER_RECOVER_NOTSUPPORT";
                break;
            case MirResult.MIR_STATUS_PER_RECOVER_OVERLIMIT:
                mes += "MIR_STATUS_PER_RECOVER_OVERLIMIT";
                break;
            case MirResult.MIR_STATUS_PER_TRANS_BAD_SW:
                mes += "MIR_STATUS_PER_TRANS_BAD_SW";
                break;
            case MirResult.MIR_STATUS_COM_RECOVER_NOTSUPPORT:
                mes += "MIR_STATUS_COM_RECOVER_NOTSUPPORT";
                break;
            case MirResult.MIR_STATUS_COM_RECOVER_OVERLIMIT:
                mes += "MIR_STATUS_COM_RECOVER_OVERLIMIT";
                break;
            case MirResult.MIR_STATUS_COM_TRANS_BAD_SW:
                mes += "MIR_STATUS_COM_TRANS_BAD_SW";
                break;
            case MirResult.MIR_STATUS_READ_RECOVER_NOTSUPPORT:
                mes += "MIR_STATUS_READ_RECOVER_NOTSUPPORT";
                break;
            case MirResult.MIR_STATUS_READ_RECOVER_OVERLIMIT:
                mes += "MIR_STATUS_READ_RECOVER_OVERLIMIT";
                break;
            default:
                mes += "unknow : " + ID;
                break;
        }
        AppendDis(mes);
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

    public void Add_Mir_AID() {
        String name = "";
        int result = 0;
        boolean dbResult = false;

        EmvService.Emv_RemoveAllApp();

        EmvApp App_Mir = new EmvApp();
        name = "Mir";

        App_Mir.AppName = name.getBytes(StandardCharsets.US_ASCII);

        App_Mir.AID = new byte[]{(byte) 0xA0, (byte) 0x00, (byte) 0x00, (byte) 0x06, (byte) 0x58};
        App_Mir.SelFlag = (byte) 0x00;
        App_Mir.Priority = (byte) 0x00;
        App_Mir.TargetPer = (byte) 20;
        App_Mir.MaxTargetPer = (byte) 50;
        App_Mir.FloorLimitCheck = (byte) 1;
        App_Mir.RandTransSel = (byte) 1;
        App_Mir.VelocityCheck = (byte) 1;
        App_Mir.FloorLimit = new byte[]{(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0x50, (byte) 0x00};//9F1B:FloorLimit
        App_Mir.Threshold = new byte[]{(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0x00};
        App_Mir.TACDenial = new byte[]{(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00};
        App_Mir.TACOnline = new byte[]{(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00};
        App_Mir.TACDefault = new byte[]{(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00};
        App_Mir.AcquierId = new byte[]{(byte) 0x01, (byte) 0x23, (byte) 0x45, (byte) 0x67, (byte) 0x89, (byte) 0x10};
        App_Mir.DDOL = new byte[]{(byte) 0x03, (byte) 0x9F, (byte) 0x37, (byte) 0x04};
        App_Mir.TDOL = new byte[]{(byte) 0x03, (byte) 0x9F, (byte) 0x02, (byte) 0x06};
        App_Mir.Version = new byte[]{(byte) 0x00, (byte) 0x96};

        result = EmvService.Emv_AddApp(App_Mir);


        if (result == EmvService.EMV_TRUE) {

        }

    }


    public void Add_Mir_Capk() {
        int result = 0;
        int capkID = 0;
        boolean dbResult = false;

        EmvService.Emv_RemoveAllCapk();

        /*----------------------------------------------------------------------- division line-----------------------------------------------------------------------------------------*/

        EmvCAPK Mir_Capk_31 = new EmvCAPK();
        Mir_Capk_31.RID = new byte[]{(byte) 0xA0, (byte) 0x00, (byte) 0x00, (byte) 0x06, (byte) 0x58};
        Mir_Capk_31.KeyID = (byte) 0x31;
        Mir_Capk_31.HashInd = (byte) 0x01;
        Mir_Capk_31.ArithInd = (byte) 0x01;
        Mir_Capk_31.Modul = new byte[]
                {
                        (byte) 0xDB, (byte) 0x2E, (byte) 0x92, (byte) 0x58, (byte) 0xEA, (byte) 0x94, (byte) 0x6B, (byte) 0xE4, (byte) 0x5E, (byte) 0xB4,
                        (byte) 0xDD, (byte) 0x8C, (byte) 0xC5, (byte) 0xEB, (byte) 0xE9, (byte) 0xC0, (byte) 0x95, (byte) 0x84, (byte) 0xC1, (byte) 0x85,
                        (byte) 0x62, (byte) 0x52, (byte) 0x22, (byte) 0x37, (byte) 0x2D, (byte) 0xE4, (byte) 0x05, (byte) 0x0A, (byte) 0xB0, (byte) 0x44,
                        (byte) 0xC6, (byte) 0xEE, (byte) 0x75, (byte) 0x4D, (byte) 0x37, (byte) 0x4E, (byte) 0x75, (byte) 0x15, (byte) 0x6B, (byte) 0x4F,
                        (byte) 0xB2, (byte) 0x63, (byte) 0x25, (byte) 0x7B, (byte) 0x7D, (byte) 0x4D, (byte) 0xB9, (byte) 0xDB, (byte) 0x28, (byte) 0xB6,
                        (byte) 0x94, (byte) 0x38, (byte) 0x09, (byte) 0x80, (byte) 0xA7, (byte) 0x5C, (byte) 0xC7, (byte) 0x27, (byte) 0x45, (byte) 0x3B,
                        (byte) 0x80, (byte) 0x16, (byte) 0xE0, (byte) 0x6A, (byte) 0x1D, (byte) 0xB1, (byte) 0xCA, (byte) 0x5E, (byte) 0x1C, (byte) 0x3E,
                        (byte) 0xBB, (byte) 0x33, (byte) 0x14, (byte) 0xA9, (byte) 0x00, (byte) 0x25, (byte) 0xD3, (byte) 0x7D, (byte) 0xFC, (byte) 0x9C,
                        (byte) 0x88, (byte) 0xCA, (byte) 0x2F, (byte) 0xA0, (byte) 0x43, (byte) 0x4F, (byte) 0x60, (byte) 0x71, (byte) 0x3A, (byte) 0xD3,
                        (byte) 0x6F, (byte) 0x9D, (byte) 0x49, (byte) 0x3E, (byte) 0xB4, (byte) 0x79, (byte) 0x4E, (byte) 0x3F, (byte) 0x96, (byte) 0x2C,
                        (byte) 0x9E, (byte) 0xC1, (byte) 0x4B, (byte) 0x6A, (byte) 0xCF, (byte) 0xDF, (byte) 0xC3, (byte) 0xC0, (byte) 0x94, (byte) 0xEB,
                        (byte) 0x1A, (byte) 0xE8, (byte) 0x04, (byte) 0x22, (byte) 0x29, (byte) 0xA1, (byte) 0x38, (byte) 0x7C, (byte) 0xF8, (byte) 0x64,
                        (byte) 0x0F, (byte) 0x19, (byte) 0x89, (byte) 0x13, (byte) 0x03, (byte) 0xBE, (byte) 0x95, (byte) 0x23, (byte) 0x80, (byte) 0xDB,
                        (byte) 0xF8, (byte) 0x20, (byte) 0xED, (byte) 0xBD, (byte) 0x9C, (byte) 0x3F, (byte) 0x08, (byte) 0x98, (byte) 0xE1, (byte) 0x46,
                        (byte) 0x73, (byte) 0x3B, (byte) 0x87, (byte) 0xEE, (byte) 0x9F, (byte) 0x48, (byte) 0x76, (byte) 0x82, (byte) 0x11, (byte) 0xD8,
                        (byte) 0xC2, (byte) 0x8A, (byte) 0x60, (byte) 0x42, (byte) 0xE6, (byte) 0x54, (byte) 0x90, (byte) 0x54, (byte) 0xCA, (byte) 0x60,
                        (byte) 0x90, (byte) 0xE3, (byte) 0x1F, (byte) 0x6B, (byte) 0x00, (byte) 0xF5, (byte) 0x9A, (byte) 0x19, (byte) 0x14, (byte) 0x16,
                        (byte) 0x9E, (byte) 0x8D, (byte) 0x55, (byte) 0xC2, (byte) 0xC2, (byte) 0x44, (byte) 0xFF, (byte) 0xE8, (byte) 0x17, (byte) 0xFD,
                        (byte) 0x9A, (byte) 0x33, (byte) 0x09, (byte) 0x11, (byte) 0x2D, (byte) 0xD0, (byte) 0x8E, (byte) 0x56, (byte) 0x4F, (byte) 0x7C,
                        (byte) 0x44, (byte) 0xEE, (byte) 0x0C, (byte) 0x3E, (byte) 0x99, (byte) 0x67, (byte) 0x62, (byte) 0x0D, (byte) 0x94, (byte) 0x2C,
                        (byte) 0x95, (byte) 0x6B, (byte) 0x0F, (byte) 0x3B, (byte) 0x93, (byte) 0x42, (byte) 0x27, (byte) 0x58, (byte) 0x76, (byte) 0x84,
                        (byte) 0x80, (byte) 0x2B, (byte) 0x75, (byte) 0x1B, (byte) 0x07, (byte) 0x07, (byte) 0x4F, (byte) 0x9F, (byte) 0xB0, (byte) 0x63,
                        (byte) 0xF2, (byte) 0x31, (byte) 0xDC, (byte) 0xEE, (byte) 0xB8, (byte) 0x99, (byte) 0x70, (byte) 0x18, (byte) 0x58, (byte) 0x46,
                        (byte) 0xD9, (byte) 0x4F, (byte) 0xB1, (byte) 0x47, (byte) 0x77, (byte) 0xAE, (byte) 0x9D, (byte) 0xE4, (byte) 0x34, (byte) 0x4D
                };
        Mir_Capk_31.Exponent = new byte[]{0x01, 0x00, 0x01};
        Mir_Capk_31.ExpDate = new byte[]{0x25, 0x12, 0x31};
        Mir_Capk_31.CheckSum = new byte[]
                {
                        (byte) 0x77, (byte) 0x76, (byte) 0xF0, (byte) 0xA4, (byte) 0x8A, (byte) 0x23, (byte) 0x3A, (byte) 0x13, (byte) 0xEA, (byte) 0xAE,
                        (byte) 0xA5, (byte) 0xD9, (byte) 0x01, (byte) 0xB3, (byte) 0x0E, (byte) 0xD2, (byte) 0x83, (byte) 0x64, (byte) 0x0C, (byte) 0x37
                };

        result = EmvService.Emv_AddCapk(Mir_Capk_31);


        /*----------------------------------------------------------------------- division line-----------------------------------------------------------------------------------------*/

        EmvCAPK Mir_Capk_32 = new EmvCAPK();
        Mir_Capk_32.RID = new byte[]{(byte) 0xA0, (byte) 0x00, (byte) 0x00, (byte) 0x06, (byte) 0x58};
        Mir_Capk_32.KeyID = (byte) 0x32;
        Mir_Capk_32.HashInd = (byte) 0x01;
        Mir_Capk_32.ArithInd = (byte) 0x01;
        Mir_Capk_32.Modul = new byte[]
                {
                        (byte) 0xD4, (byte) 0xD0, (byte) 0xA1, (byte) 0xA2, (byte) 0x62, (byte) 0x21, (byte) 0xE0, (byte) 0x3E, (byte) 0x4D, (byte) 0xB8,
                        (byte) 0x14, (byte) 0x61, (byte) 0xF4, (byte) 0x7D, (byte) 0xB1, (byte) 0x93, (byte) 0x76, (byte) 0x94, (byte) 0x83, (byte) 0x3F,
                        (byte) 0x97, (byte) 0x3A, (byte) 0x38, (byte) 0x91, (byte) 0xCA, (byte) 0x63, (byte) 0x27, (byte) 0x01, (byte) 0x42, (byte) 0xF5,
                        (byte) 0xEC, (byte) 0x67, (byte) 0x31, (byte) 0x8E, (byte) 0x2B, (byte) 0xA7, (byte) 0xB2, (byte) 0x83, (byte) 0x39, (byte) 0xC7,
                        (byte) 0x8F, (byte) 0x17, (byte) 0x0A, (byte) 0x21, (byte) 0xB9, (byte) 0x93, (byte) 0x67, (byte) 0x76, (byte) 0x69, (byte) 0xAA,
                        (byte) 0x47, (byte) 0x19, (byte) 0x4A, (byte) 0xAB, (byte) 0x89, (byte) 0x45, (byte) 0x10, (byte) 0xA9, (byte) 0x37, (byte) 0x8E,
                        (byte) 0xF1, (byte) 0x77, (byte) 0xF9, (byte) 0xE2, (byte) 0x59, (byte) 0x08, (byte) 0x72, (byte) 0x28, (byte) 0xC7, (byte) 0x38,
                        (byte) 0x30, (byte) 0xFF, (byte) 0x59, (byte) 0x3E, (byte) 0xE5, (byte) 0xE4, (byte) 0xDD, (byte) 0x4B, (byte) 0xD4, (byte) 0xBB,
                        (byte) 0xE9, (byte) 0x50, (byte) 0xFA, (byte) 0x44, (byte) 0x87, (byte) 0x61, (byte) 0xBA, (byte) 0xDE, (byte) 0x6F, (byte) 0x50,
                        (byte) 0x93, (byte) 0xC5, (byte) 0x62, (byte) 0xB5, (byte) 0x82, (byte) 0xB8, (byte) 0xE0, (byte) 0xD0, (byte) 0x7E, (byte) 0x03,
                        (byte) 0x07, (byte) 0xA6, (byte) 0x30, (byte) 0x34, (byte) 0xA4, (byte) 0xCF, (byte) 0x69, (byte) 0xB4, (byte) 0x69, (byte) 0x2B,
                        (byte) 0x10, (byte) 0x39, (byte) 0x05, (byte) 0x21, (byte) 0x97, (byte) 0xDB, (byte) 0xF8, (byte) 0x8F, (byte) 0xCD, (byte) 0x1C,
                        (byte) 0x77, (byte) 0xCF, (byte) 0xE8, (byte) 0x65, (byte) 0x41, (byte) 0x05, (byte) 0x93, (byte) 0xCE, (byte) 0x8D, (byte) 0x6D,
                        (byte) 0xEB, (byte) 0x41, (byte) 0xFE, (byte) 0xF7, (byte) 0xA1, (byte) 0x02, (byte) 0x37, (byte) 0x4C, (byte) 0xC0, (byte) 0x6D,
                        (byte) 0x28, (byte) 0xD8, (byte) 0xBB, (byte) 0xAA, (byte) 0xB5, (byte) 0x8B, (byte) 0x60, (byte) 0xA5, (byte) 0xD3, (byte) 0xD9,
                        (byte) 0xD2, (byte) 0x38, (byte) 0xCE, (byte) 0x3F, (byte) 0x2D, (byte) 0x9F, (byte) 0x3A, (byte) 0x2C, (byte) 0xA6, (byte) 0x3E,
                        (byte) 0x15, (byte) 0xAD, (byte) 0xD8, (byte) 0x39, (byte) 0x63, (byte) 0x85, (byte) 0x56, (byte) 0xF4, (byte) 0xCA, (byte) 0x8D,
                        (byte) 0x59, (byte) 0xB3, (byte) 0x9D, (byte) 0xAB, (byte) 0xB1, (byte) 0x14, (byte) 0xD3, (byte) 0x55, (byte) 0x80, (byte) 0x83,
                        (byte) 0xD8, (byte) 0xEB, (byte) 0x9C, (byte) 0x7F, (byte) 0x97, (byte) 0xDD, (byte) 0xF0, (byte) 0x2B, (byte) 0x7D, (byte) 0x94,
                        (byte) 0x7F, (byte) 0x3C, (byte) 0xB1, (byte) 0x67, (byte) 0xCB, (byte) 0x86, (byte) 0x9C, (byte) 0xC4, (byte) 0xC5, (byte) 0x47
                };
        Mir_Capk_32.Exponent = new byte[]{0x01, 0x00, 0x01};
        Mir_Capk_32.ExpDate = new byte[]{0x25, 0x12, 0x31};
        Mir_Capk_32.CheckSum = new byte[]
                {
                        (byte) 0x3B, (byte) 0x6F, (byte) 0x29, (byte) 0x84, (byte) 0x25, (byte) 0x64, (byte) 0xBE, (byte) 0x86, (byte) 0xAC, (byte) 0x9D,
                        (byte) 0xAF, (byte) 0xCA, (byte) 0xFA, (byte) 0x65, (byte) 0x79, (byte) 0x65, (byte) 0xD8, (byte) 0x63, (byte) 0xC6, (byte) 0x3B
                };

        result = EmvService.Emv_AddCapk(Mir_Capk_32);


        /*----------------------------------------------------------------------- division line-----------------------------------------------------------------------------------------*/


        EmvCAPK Mir_Capk_33 = new EmvCAPK();
        Mir_Capk_33.RID = new byte[]{(byte) 0xA0, (byte) 0x00, (byte) 0x00, (byte) 0x06, (byte) 0x58};
        Mir_Capk_33.KeyID = (byte) 0x33;
        Mir_Capk_33.HashInd = (byte) 0x01;
        Mir_Capk_33.ArithInd = (byte) 0x01;
        Mir_Capk_33.Modul = new byte[]
                {
                        (byte) 0xA8, (byte) 0x30, (byte) 0x6E, (byte) 0x9E, (byte) 0x57, (byte) 0x9A, (byte) 0xA0, (byte) 0x71, (byte) 0xAE, (byte) 0x06,
                        (byte) 0xDC, (byte) 0x87, (byte) 0xB8, (byte) 0xDC, (byte) 0x5E, (byte) 0x1F, (byte) 0x61, (byte) 0x8B, (byte) 0x6F, (byte) 0x37,
                        (byte) 0xDE, (byte) 0x4A, (byte) 0xFB, (byte) 0xBA, (byte) 0xB9, (byte) 0xA3, (byte) 0x7E, (byte) 0x21, (byte) 0x73, (byte) 0x8C,
                        (byte) 0x5E, (byte) 0x19, (byte) 0xBC, (byte) 0x4A, (byte) 0x13, (byte) 0x06, (byte) 0x89, (byte) 0xA1, (byte) 0x1E, (byte) 0xD1,
                        (byte) 0x0D, (byte) 0xD6, (byte) 0xBE, (byte) 0xDD, (byte) 0xFD, (byte) 0xA6, (byte) 0x31, (byte) 0x8E, (byte) 0x44, (byte) 0xAE,
                        (byte) 0x2A, (byte) 0xEB, (byte) 0x5E, (byte) 0x5C, (byte) 0x62, (byte) 0xAF, (byte) 0xCF, (byte) 0xBF, (byte) 0x54, (byte) 0x71,
                        (byte) 0x71, (byte) 0x35, (byte) 0xEE, (byte) 0x0D, (byte) 0xC0, (byte) 0x71, (byte) 0x55, (byte) 0x61, (byte) 0x9D, (byte) 0x20,
                        (byte) 0x74, (byte) 0xD6, (byte) 0x6D, (byte) 0xBA, (byte) 0xA0, (byte) 0x1A, (byte) 0xDF, (byte) 0xDA, (byte) 0x76, (byte) 0x38,
                        (byte) 0xA6, (byte) 0x61, (byte) 0xC8, (byte) 0xEA, (byte) 0x31, (byte) 0xBE, (byte) 0x6D, (byte) 0x05, (byte) 0x30, (byte) 0xBE,
                        (byte) 0xBF, (byte) 0xBA, (byte) 0x9E, (byte) 0x4C, (byte) 0x74, (byte) 0x88, (byte) 0x73, (byte) 0xC6, (byte) 0x57, (byte) 0x58,
                        (byte) 0x87, (byte) 0x76, (byte) 0x1F, (byte) 0xA4, (byte) 0xE0, (byte) 0xE2, (byte) 0xC5, (byte) 0x30, (byte) 0xF1, (byte) 0xD8,
                        (byte) 0x1E, (byte) 0xA3, (byte) 0xE2, (byte) 0xF0, (byte) 0x40, (byte) 0x8B, (byte) 0x13, (byte) 0xFD, (byte) 0xCA, (byte) 0x97,
                        (byte) 0x59, (byte) 0x5D, (byte) 0x69, (byte) 0x5E, (byte) 0x92, (byte) 0xB4, (byte) 0x52, (byte) 0x6A, (byte) 0x86, (byte) 0xF0,
                        (byte) 0x33, (byte) 0xAE, (byte) 0x95, (byte) 0xC7, (byte) 0x98, (byte) 0x72, (byte) 0xAF, (byte) 0xF7, (byte) 0x16, (byte) 0x5C,
                        (byte) 0xCF, (byte) 0x7A, (byte) 0x08, (byte) 0xFD
                };

        Mir_Capk_33.Exponent = new byte[]{0x01, 0x00, 0x01};
        Mir_Capk_33.ExpDate = new byte[]{0x25, 0x12, 0x31};

        Mir_Capk_33.CheckSum = new byte[]
                {
                        (byte) 0x1D, (byte) 0x1D, (byte) 0x46, (byte) 0x0D, (byte) 0x33, (byte) 0x8C, (byte) 0xF4, (byte) 0x36, (byte) 0xF9, (byte) 0xA0,
                        (byte) 0x58, (byte) 0x7E, (byte) 0x83, (byte) 0xA8, (byte) 0x59, (byte) 0x27, (byte) 0xDD, (byte) 0xD1, (byte) 0x74, (byte) 0xE8
                };

        result = EmvService.Emv_AddCapk(Mir_Capk_33);

        /*----------------------------------------------------------------------- division line-----------------------------------------------------------------------------------------*/


        EmvCAPK Mir_Capk_34 = new EmvCAPK();
        Mir_Capk_34.RID = new byte[]{(byte) 0xA0, (byte) 0x00, (byte) 0x00, (byte) 0x06, (byte) 0x58};
        Mir_Capk_34.KeyID = (byte) 0x34;
        Mir_Capk_34.HashInd = (byte) 0x01;
        Mir_Capk_34.ArithInd = (byte) 0x01;
        Mir_Capk_34.Modul = new byte[]
                {
                        (byte) 0xDA, (byte) 0xB0, (byte) 0x8F, (byte) 0xFC, (byte) 0xDF, (byte) 0x84, (byte) 0x31, (byte) 0xAE, (byte) 0x35, (byte) 0xAD,
                        (byte) 0xDC, (byte) 0x37, (byte) 0x49, (byte) 0xFF, (byte) 0x56, (byte) 0x47, (byte) 0x7A, (byte) 0xE0, (byte) 0x30, (byte) 0x9E,
                        (byte) 0xF2, (byte) 0xED, (byte) 0xC1, (byte) 0x23, (byte) 0x54, (byte) 0x2C, (byte) 0xEE, (byte) 0x63, (byte) 0xBD, (byte) 0x97,
                        (byte) 0x25, (byte) 0x8F, (byte) 0x7A, (byte) 0xC6, (byte) 0xA4, (byte) 0xBF, (byte) 0x86, (byte) 0x25, (byte) 0x6D, (byte) 0x10,
                        (byte) 0xCA, (byte) 0xE9, (byte) 0x39, (byte) 0x3D, (byte) 0x76, (byte) 0x2E, (byte) 0x83, (byte) 0xEB, (byte) 0x42, (byte) 0x12,
                        (byte) 0xBF, (byte) 0x38, (byte) 0xC0, (byte) 0xA9, (byte) 0x48, (byte) 0xCB, (byte) 0xAA, (byte) 0x7A, (byte) 0xA1, (byte) 0x99,
                        (byte) 0xB0, (byte) 0x7A, (byte) 0xF6, (byte) 0x7F, (byte) 0xCA, (byte) 0xBA, (byte) 0x39, (byte) 0x12, (byte) 0x3E, (byte) 0x54,
                        (byte) 0x35, (byte) 0xA4, (byte) 0x34, (byte) 0x6F, (byte) 0x7E, (byte) 0xC2, (byte) 0x43, (byte) 0xFE, (byte) 0xD0, (byte) 0xE2,
                        (byte) 0x04, (byte) 0x15, (byte) 0x29, (byte) 0x78, (byte) 0xAD, (byte) 0x99, (byte) 0xD1, (byte) 0x05, (byte) 0x7E, (byte) 0x21,
                        (byte) 0x87, (byte) 0xC0, (byte) 0x5E, (byte) 0x59, (byte) 0xFE, (byte) 0xD3, (byte) 0x27, (byte) 0xC2, (byte) 0x1D, (byte) 0xE9,
                        (byte) 0x4B, (byte) 0xF0, (byte) 0xB2, (byte) 0x9D, (byte) 0x7B, (byte) 0x4A, (byte) 0xB8, (byte) 0xE2, (byte) 0x38, (byte) 0x86,
                        (byte) 0x44, (byte) 0x6C, (byte) 0x35, (byte) 0x35, (byte) 0x13, (byte) 0xAE, (byte) 0x73, (byte) 0x45, (byte) 0xB6, (byte) 0x1F,
                        (byte) 0xA5, (byte) 0x63, (byte) 0x59, (byte) 0xCF, (byte) 0x51, (byte) 0x53, (byte) 0xCF, (byte) 0x58, (byte) 0xD6, (byte) 0xF6,
                        (byte) 0x32, (byte) 0x53, (byte) 0x66, (byte) 0x34, (byte) 0x36, (byte) 0x54, (byte) 0x9E, (byte) 0x80, (byte) 0x24, (byte) 0x69,
                        (byte) 0xF5, (byte) 0x26, (byte) 0x32, (byte) 0x14, (byte) 0x18, (byte) 0x0E, (byte) 0x91, (byte) 0x5F, (byte) 0x7C, (byte) 0xDB,
                        (byte) 0xBD, (byte) 0x0B, (byte) 0xD7, (byte) 0x30, (byte) 0x46, (byte) 0xD1, (byte) 0xDD, (byte) 0x8F, (byte) 0x88, (byte) 0x69,
                        (byte) 0x68, (byte) 0xAB, (byte) 0x2E, (byte) 0xE4, (byte) 0x3C, (byte) 0x01, (byte) 0xCD, (byte) 0xAA, (byte) 0x1E, (byte) 0xC9,
                        (byte) 0x68, (byte) 0x47, (byte) 0x88, (byte) 0x62, (byte) 0x24, (byte) 0xBE, (byte) 0x14, (byte) 0x9C, (byte) 0x13, (byte) 0x01,
                        (byte) 0xFA, (byte) 0xEA, (byte) 0x68, (byte) 0x6D, (byte) 0xA3, (byte) 0x42, (byte) 0x1C, (byte) 0x12, (byte) 0x38, (byte) 0x9A,
                        (byte) 0x9C, (byte) 0x30, (byte) 0x54, (byte) 0x5D, (byte) 0x17, (byte) 0x47, (byte) 0x9C, (byte) 0x3A, (byte) 0x24, (byte) 0x89,
                        (byte) 0x93, (byte) 0x33, (byte) 0xDE, (byte) 0x0B, (byte) 0xE6, (byte) 0x6A, (byte) 0x46, (byte) 0x01, (byte) 0xE8, (byte) 0x74,
                        (byte) 0x4D, (byte) 0xA0, (byte) 0x44, (byte) 0x95, (byte) 0x38, (byte) 0x8C, (byte) 0xA5, (byte) 0xE1, (byte) 0x22, (byte) 0xD8,
                        (byte) 0x6F, (byte) 0xD4, (byte) 0x2C, (byte) 0x1D, (byte) 0x37, (byte) 0x19, (byte) 0x0A, (byte) 0x8C, (byte) 0x47, (byte) 0xF4,
                        (byte) 0x2A, (byte) 0x8A, (byte) 0x8B, (byte) 0xAE, (byte) 0xA0, (byte) 0xFF, (byte) 0x9E, (byte) 0x18, (byte) 0xFD, (byte) 0x41,
                        (byte) 0x72, (byte) 0xD7, (byte) 0x65, (byte) 0x47, (byte) 0x15, (byte) 0x75, (byte) 0x21, (byte) 0x7B
                };

        Mir_Capk_34.Exponent = new byte[]{0x01, 0x00, 0x01};
        Mir_Capk_34.ExpDate = new byte[]{0x25, 0x12, 0x31};

        Mir_Capk_34.CheckSum = new byte[]
                {
                        (byte) 0xAD, (byte) 0xBF, (byte) 0x17, (byte) 0x06, (byte) 0xAD, (byte) 0xD0, (byte) 0x2A, (byte) 0xA0, (byte) 0xF2, (byte) 0x2A,
                        (byte) 0xE7, (byte) 0xA1, (byte) 0xF1, (byte) 0x25, (byte) 0x63, (byte) 0x62, (byte) 0xD4, (byte) 0xAE, (byte) 0x33, (byte) 0xF7
                };

        result = EmvService.Emv_AddCapk(Mir_Capk_34);


        /*----------------------------------------------------------------------- division line-----------------------------------------------------------------------------------------*/


        EmvCAPK Mir_Capk_D1 = new EmvCAPK();
        Mir_Capk_D1.RID = new byte[]{(byte) 0xA0, (byte) 0x00, (byte) 0x00, (byte) 0x06, (byte) 0x58};
        Mir_Capk_D1.KeyID = (byte) 0xD1;
        Mir_Capk_D1.HashInd = (byte) 0x01;
        Mir_Capk_D1.ArithInd = (byte) 0x01;
        Mir_Capk_D1.Modul = new byte[]
                {
                        (byte) 0xA3, (byte) 0x54, (byte) 0x74, (byte) 0x15, (byte) 0xA7, (byte) 0xD2, (byte) 0x37, (byte) 0xC0, (byte) 0x9F, (byte) 0xC8,
                        (byte) 0xAF, (byte) 0xF9, (byte) 0x89, (byte) 0xFD, (byte) 0xA4, (byte) 0x9E, (byte) 0x5B, (byte) 0x32, (byte) 0x75, (byte) 0x54,
                        (byte) 0x50, (byte) 0x26, (byte) 0x36, (byte) 0x1C, (byte) 0x1A, (byte) 0x8D, (byte) 0xE4, (byte) 0x77, (byte) 0x46, (byte) 0x7F,
                        (byte) 0x96, (byte) 0x3D, (byte) 0x8F, (byte) 0x6F, (byte) 0x58, (byte) 0xA2, (byte) 0xF1, (byte) 0x6E, (byte) 0x08, (byte) 0x85,
                        (byte) 0xE4, (byte) 0x75, (byte) 0x9C, (byte) 0xA5, (byte) 0x8F, (byte) 0x72, (byte) 0xA5, (byte) 0xB5, (byte) 0x44, (byte) 0x6C,
                        (byte) 0xE3, (byte) 0x89, (byte) 0x31, (byte) 0x55, (byte) 0xEF, (byte) 0xD9, (byte) 0x78, (byte) 0xB2, (byte) 0xF0, (byte) 0xD8,
                        (byte) 0xD1, (byte) 0xA7, (byte) 0x29, (byte) 0x4A, (byte) 0xC7, (byte) 0x87, (byte) 0x0D, (byte) 0x65, (byte) 0xB5, (byte) 0xCC,
                        (byte) 0x78, (byte) 0x28, (byte) 0x6F, (byte) 0x96, (byte) 0x23, (byte) 0x7E, (byte) 0xFC, (byte) 0xBA, (byte) 0x02, (byte) 0xC6,
                        (byte) 0x84, (byte) 0x4A, (byte) 0x84, (byte) 0xDB, (byte) 0x79, (byte) 0xA0, (byte) 0x1D, (byte) 0x22, (byte) 0x5F, (byte) 0xF3,
                        (byte) 0xBE, (byte) 0xAB, (byte) 0x37, (byte) 0x61, (byte) 0xAF, (byte) 0xC5, (byte) 0x2A, (byte) 0xED, (byte) 0xD5, (byte) 0x77,
                        (byte) 0x64, (byte) 0x48, (byte) 0x3C, (byte) 0x98, (byte) 0x00, (byte) 0x76, (byte) 0xD1, (byte) 0x0E, (byte) 0x4C, (byte) 0x34,
                        (byte) 0x85, (byte) 0x01, (byte) 0x1D, (byte) 0xD9, (byte) 0x3A, (byte) 0x97, (byte) 0x0C, (byte) 0x57, (byte) 0xFC, (byte) 0x72,
                        (byte) 0xA1, (byte) 0xCC, (byte) 0xA4, (byte) 0x7C, (byte) 0x7D, (byte) 0x1B, (byte) 0x57, (byte) 0xE5, (byte) 0xD7, (byte) 0x79,
                        (byte) 0x8A, (byte) 0x18, (byte) 0x0B, (byte) 0xF0, (byte) 0x84, (byte) 0x55, (byte) 0xA4, (byte) 0xD6, (byte) 0x02, (byte) 0xCF,
                        (byte) 0xC3, (byte) 0xC8, (byte) 0x81, (byte) 0x03, (byte) 0x4B, (byte) 0x52, (byte) 0xD6, (byte) 0xDF, (byte) 0x2C, (byte) 0x3B,
                        (byte) 0x1A, (byte) 0x8F, (byte) 0xEE, (byte) 0x7E, (byte) 0x65, (byte) 0x39, (byte) 0xEA, (byte) 0x35, (byte) 0xF6, (byte) 0xB5,
                        (byte) 0xC1, (byte) 0x23, (byte) 0xA8, (byte) 0x22, (byte) 0xAA, (byte) 0x73, (byte) 0xFB, (byte) 0x6B, (byte) 0xDF, (byte) 0xD8,
                        (byte) 0x94, (byte) 0xAE, (byte) 0xB8, (byte) 0x38, (byte) 0x1A, (byte) 0x62, (byte) 0x41, (byte) 0x3E, (byte) 0xFB, (byte) 0x03,
                        (byte) 0x0F, (byte) 0x85, (byte) 0xDC, (byte) 0x45, (byte) 0xD7, (byte) 0x1B, (byte) 0x66, (byte) 0xA3, (byte) 0x22, (byte) 0xF1,
                        (byte) 0x53, (byte) 0x2A, (byte) 0x91, (byte) 0xC9, (byte) 0xAD, (byte) 0x8E, (byte) 0x48, (byte) 0x20, (byte) 0xAC, (byte) 0x18,
                        (byte) 0xC5, (byte) 0x44, (byte) 0xA6, (byte) 0x23, (byte) 0xFC, (byte) 0x3E, (byte) 0x40, (byte) 0x1D, (byte) 0x42, (byte) 0x49,
                        (byte) 0x8C, (byte) 0x1C, (byte) 0x9B, (byte) 0x88, (byte) 0xE5, (byte) 0xA6, (byte) 0xB7, (byte) 0xDA, (byte) 0x2D, (byte) 0x9E,
                        (byte) 0x0B, (byte) 0xF7, (byte) 0xCB, (byte) 0x3F, (byte) 0x92, (byte) 0x12, (byte) 0x42, (byte) 0xB5, (byte) 0x35, (byte) 0x23,
                        (byte) 0x02, (byte) 0xB9, (byte) 0x5E, (byte) 0xE1, (byte) 0x34, (byte) 0x4D, (byte) 0x79, (byte) 0xEC, (byte) 0xE4, (byte) 0x9D
                };

        Mir_Capk_D1.Exponent = new byte[]{0x01, 0x00, 0x01};
        Mir_Capk_D1.ExpDate = new byte[]{0x25, 0x12, 0x31};

        Mir_Capk_D1.CheckSum = new byte[]
                {
                        (byte) 0x05, (byte) 0x56, (byte) 0x76, (byte) 0x28, (byte) 0x48, (byte) 0x0B, (byte) 0x75, (byte) 0x7F, (byte) 0xE6, (byte) 0x33,
                        (byte) 0x99, (byte) 0x9C, (byte) 0x9A, (byte) 0xE1, (byte) 0xD9, (byte) 0xF4, (byte) 0x20, (byte) 0xF8, (byte) 0x4E, (byte) 0xE3
                };

        result = EmvService.Emv_AddCapk(Mir_Capk_D1);


        /*----------------------------------------------------------------------- division line-----------------------------------------------------------------------------------------*/

        EmvCAPK Mir_Capk_D2 = new EmvCAPK();
        Mir_Capk_D2.RID = new byte[]{(byte) 0xA0, (byte) 0x00, (byte) 0x00, (byte) 0x06, (byte) 0x58};
        Mir_Capk_D2.KeyID = (byte) 0xD2;
        Mir_Capk_D2.HashInd = (byte) 0x01;
        Mir_Capk_D2.ArithInd = (byte) 0x01;
        Mir_Capk_D2.Modul = new byte[]
                {
                        (byte) 0x87, (byte) 0xDF, (byte) 0x1B, (byte) 0x2E, (byte) 0x39, (byte) 0x25, (byte) 0x2E, (byte) 0xA1, (byte) 0x4A, (byte) 0xA9,
                        (byte) 0x37, (byte) 0xD0, (byte) 0xEF, (byte) 0x2B, (byte) 0x35, (byte) 0xDC, (byte) 0x5C, (byte) 0x15, (byte) 0xE5, (byte) 0xA8,
                        (byte) 0xDE, (byte) 0xEC, (byte) 0x15, (byte) 0x10, (byte) 0xBA, (byte) 0x0A, (byte) 0x27, (byte) 0x59, (byte) 0x50, (byte) 0x0E,
                        (byte) 0x86, (byte) 0x68, (byte) 0x5F, (byte) 0xB7, (byte) 0x65, (byte) 0xB1, (byte) 0x02, (byte) 0xF4, (byte) 0x40, (byte) 0xBE,
                        (byte) 0xC8, (byte) 0x72, (byte) 0x50, (byte) 0x3E, (byte) 0xD7, (byte) 0x91, (byte) 0x9A, (byte) 0xF7, (byte) 0x2D, (byte) 0xEB,
                        (byte) 0xA2, (byte) 0xF5, (byte) 0x0B, (byte) 0x7C, (byte) 0xC6, (byte) 0xA4, (byte) 0xC4, (byte) 0xBC, (byte) 0x05, (byte) 0x48,
                        (byte) 0xAF, (byte) 0x20, (byte) 0x1C, (byte) 0x72, (byte) 0x74, (byte) 0xFA, (byte) 0xF2, (byte) 0x48, (byte) 0x23, (byte) 0x9D,
                        (byte) 0x67, (byte) 0xFC, (byte) 0x72, (byte) 0xA1, (byte) 0x23, (byte) 0x69, (byte) 0x03, (byte) 0x00, (byte) 0xC9, (byte) 0xA0,
                        (byte) 0x64, (byte) 0xA5, (byte) 0xAB, (byte) 0x97, (byte) 0xB7, (byte) 0xF2, (byte) 0x6C, (byte) 0xFA, (byte) 0x37, (byte) 0x8A,
                        (byte) 0x7A, (byte) 0x0B, (byte) 0xA3, (byte) 0xD5, (byte) 0x51, (byte) 0xB7, (byte) 0x4E, (byte) 0xA9, (byte) 0x53, (byte) 0x49,
                        (byte) 0x68, (byte) 0x58, (byte) 0xA8, (byte) 0x1E, (byte) 0xF6, (byte) 0xA4, (byte) 0x4A, (byte) 0xA6, (byte) 0x59, (byte) 0xC2,
                        (byte) 0x53, (byte) 0x55, (byte) 0x1D, (byte) 0xD4, (byte) 0x51, (byte) 0x74, (byte) 0xBB, (byte) 0x2A, (byte) 0x24, (byte) 0x8F,
                        (byte) 0xDB, (byte) 0xB6, (byte) 0x6D, (byte) 0x61, (byte) 0x4B, (byte) 0xA1, (byte) 0xC0, (byte) 0x18, (byte) 0xF9, (byte) 0x52,
                        (byte) 0xA4, (byte) 0x5C, (byte) 0x73, (byte) 0xE1, (byte) 0x14, (byte) 0x9F, (byte) 0xEA, (byte) 0xDC, (byte) 0x5B, (byte) 0x3E,
                        (byte) 0x25, (byte) 0x61, (byte) 0xBE, (byte) 0xF8, (byte) 0xD4, (byte) 0xEE, (byte) 0x01, (byte) 0x50, (byte) 0x80, (byte) 0x7E,
                        (byte) 0xA1, (byte) 0xDD, (byte) 0xE9, (byte) 0xB3, (byte) 0xDD, (byte) 0xEC, (byte) 0xD5, (byte) 0x42, (byte) 0x8E, (byte) 0x3E,
                        (byte) 0x79, (byte) 0x72, (byte) 0x1A, (byte) 0xDD, (byte) 0xC6, (byte) 0x60, (byte) 0xDD, (byte) 0x28, (byte) 0xB9, (byte) 0xCC,
                        (byte) 0x3B, (byte) 0xF0, (byte) 0x60, (byte) 0x66, (byte) 0x17, (byte) 0x64, (byte) 0x05, (byte) 0xD8, (byte) 0x22, (byte) 0xD6,
                        (byte) 0x59, (byte) 0xAC, (byte) 0x7B, (byte) 0xB9, (byte) 0x35, (byte) 0x4A, (byte) 0xA3, (byte) 0xBA, (byte) 0x33, (byte) 0xDD,
                        (byte) 0xEA, (byte) 0xD4, (byte) 0x7D, (byte) 0xD3, (byte) 0x4B, (byte) 0x42, (byte) 0x64, (byte) 0xE3, (byte) 0x45, (byte) 0x81
                };

        Mir_Capk_D2.Exponent = new byte[]{0x01, 0x00, 0x01};
        Mir_Capk_D2.ExpDate = new byte[]{0x25, 0x12, 0x31};

        Mir_Capk_D2.CheckSum = new byte[]
                {
                        (byte) 0x92, (byte) 0xC8, (byte) 0x8F, (byte) 0x72, (byte) 0xE3, (byte) 0xDF, (byte) 0xC1, (byte) 0x67, (byte) 0x8E, (byte) 0xA4,
                        (byte) 0x0D, (byte) 0x5D, (byte) 0xA5, (byte) 0x51, (byte) 0x9B, (byte) 0xE1, (byte) 0xAE, (byte) 0xC0, (byte) 0x01, (byte) 0xB7
                };

        result = EmvService.Emv_AddCapk(Mir_Capk_D2);


        /*----------------------------------------------------------------------- division line-----------------------------------------------------------------------------------------*/

        EmvCAPK Mir_Capk_D3 = new EmvCAPK();
        Mir_Capk_D3.RID = new byte[]{(byte) 0xA0, (byte) 0x00, (byte) 0x00, (byte) 0x06, (byte) 0x58};
        Mir_Capk_D3.KeyID = (byte) 0xD3;
        Mir_Capk_D3.HashInd = (byte) 0x01;
        Mir_Capk_D3.ArithInd = (byte) 0x01;
        Mir_Capk_D3.Modul = new byte[]
                {
                        (byte) 0x9A, (byte) 0xF6, (byte) 0x92, (byte) 0xFF, (byte) 0xA0, (byte) 0x1A, (byte) 0x2C, (byte) 0xC6, (byte) 0x1B, (byte) 0x97,
                        (byte) 0x82, (byte) 0x0A, (byte) 0xAF, (byte) 0xCB, (byte) 0xF0, (byte) 0x84, (byte) 0x4B, (byte) 0x85, (byte) 0x97, (byte) 0x26,
                        (byte) 0xDE, (byte) 0x13, (byte) 0xAD, (byte) 0x4C, (byte) 0xAA, (byte) 0x8D, (byte) 0x33, (byte) 0x89, (byte) 0xA1, (byte) 0x37,
                        (byte) 0x28, (byte) 0xB5, (byte) 0x88, (byte) 0xE1, (byte) 0xDD, (byte) 0x33, (byte) 0x37, (byte) 0x3C, (byte) 0x86, (byte) 0x24,
                        (byte) 0xD9, (byte) 0xD4, (byte) 0xBE, (byte) 0x46, (byte) 0x8F, (byte) 0xA7, (byte) 0x2F, (byte) 0x5E, (byte) 0xEE, (byte) 0xBB,
                        (byte) 0x5F, (byte) 0xC9, (byte) 0x04, (byte) 0xA3, (byte) 0x86, (byte) 0xAC, (byte) 0xEF, (byte) 0x2A, (byte) 0x4D, (byte) 0x8A,
                        (byte) 0x6F, (byte) 0x8A, (byte) 0xEF, (byte) 0xD6, (byte) 0x91, (byte) 0x64, (byte) 0xCD, (byte) 0x56, (byte) 0x18, (byte) 0x3C,
                        (byte) 0x38, (byte) 0x1B, (byte) 0xD7, (byte) 0xFC, (byte) 0xC2, (byte) 0xA1, (byte) 0x6C, (byte) 0x6B, (byte) 0x12, (byte) 0x30,
                        (byte) 0x57, (byte) 0x29, (byte) 0xFF, (byte) 0xEA, (byte) 0x76, (byte) 0x66, (byte) 0x99, (byte) 0xF5, (byte) 0x4C, (byte) 0xA0,
                        (byte) 0x1D, (byte) 0x97, (byte) 0x76, (byte) 0x11, (byte) 0x70, (byte) 0xC7, (byte) 0xA7, (byte) 0x7B, (byte) 0x02, (byte) 0x70,
                        (byte) 0x28, (byte) 0xF1, (byte) 0x2F, (byte) 0xB7, (byte) 0x9F, (byte) 0xCB, (byte) 0x5E, (byte) 0x03, (byte) 0x49, (byte) 0x83,
                        (byte) 0x74, (byte) 0x3A, (byte) 0x5C, (byte) 0xE9, (byte) 0xC2, (byte) 0xFE, (byte) 0x07, (byte) 0x53, (byte) 0x02, (byte) 0x1B,
                        (byte) 0xC9, (byte) 0xC8, (byte) 0xCA, (byte) 0x75, (byte) 0x55, (byte) 0x32, (byte) 0x0D, (byte) 0x0C, (byte) 0xF4, (byte) 0xE1,
                        (byte) 0x18, (byte) 0x2F, (byte) 0x96, (byte) 0xBC, (byte) 0x3A, (byte) 0x15, (byte) 0x00, (byte) 0xC3, (byte) 0x35, (byte) 0x19,
                        (byte) 0x9E, (byte) 0x70, (byte) 0x14, (byte) 0x59
                };

        Mir_Capk_D3.Exponent = new byte[]{0x01, 0x00, 0x01};
        Mir_Capk_D3.ExpDate = new byte[]{0x25, 0x12, 0x31};

        Mir_Capk_D3.CheckSum = new byte[]
                {
                        (byte) 0x24, (byte) 0x56, (byte) 0x8C, (byte) 0x62, (byte) 0xD6, (byte) 0x9F, (byte) 0xBF, (byte) 0x64, (byte) 0xFD, (byte) 0xD0,
                        (byte) 0x0A, (byte) 0x16, (byte) 0xA3, (byte) 0xB9, (byte) 0xF1, (byte) 0x32, (byte) 0xA1, (byte) 0x24, (byte) 0xEE, (byte) 0x92
                };

        result = EmvService.Emv_AddCapk(Mir_Capk_D3);

    }
}
