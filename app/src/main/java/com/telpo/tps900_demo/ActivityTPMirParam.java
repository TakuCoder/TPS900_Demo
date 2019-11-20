package com.telpo.tps900_demo;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.telpo.emv.MirParam;
import com.telpo.util.StringUtil;

public class ActivityTPMirParam extends Activity {

    Context context;
    MirParam param;

    EditText et_AppVersion, et_FloorLimit, et_NoCvmLimit, et_NoCDCvmLimit, et_CDCvmLimit,
            et_TpmCaps, et_RecoveryLimit, et_TerminalType, et_TermCountryCode, et_TAC_Denial,
            et_TAC_OnLine, et_TAC_Default, et_TagList;

    Button bt_sava;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tpmir_param);
        context = ActivityTPMirParam.this;
        ViewInit();
        paramInit();
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS);
    }

    void ViewInit() {
        bt_sava = findViewById(R.id.bt_sava);
        bt_sava.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveParam();
            }
        });

        et_AppVersion = findViewById(R.id.et_AppVersion);
        et_FloorLimit = findViewById(R.id.et_FloorLimit);
        et_NoCvmLimit = findViewById(R.id.et_NoCvmLimit);
        et_NoCDCvmLimit = findViewById(R.id.et_NoCDCvmLimit);
        et_CDCvmLimit = findViewById(R.id.et_CDCvmLimit);
        et_TpmCaps = findViewById(R.id.et_TpmCaps);
        et_RecoveryLimit = findViewById(R.id.et_RecoveryLimit);
        et_TerminalType = findViewById(R.id.et_TerminalType);
        et_TermCountryCode = findViewById(R.id.et_TermCountryCode);
        et_TAC_Denial = findViewById(R.id.et_TAC_Denial);
        et_TAC_OnLine = findViewById(R.id.et_TAC_OnLine);
        et_TAC_Default = findViewById(R.id.et_TAC_Default);
        et_TagList = findViewById(R.id.et_TagList);
    }

    void paramInit() {
        param = ActivityTPMir.m_mirParam;
        et_AppVersion.setText(StringUtil.bytesToHexString(param.AppVersion));
        et_FloorLimit.setText(Long.toString(param.FloorLimit));
        et_NoCvmLimit.setText(Long.toString(param.NoCvmLimit));
        et_NoCDCvmLimit.setText(Long.toString(param.NoCDCvmLimit));
        et_CDCvmLimit.setText(Long.toString(param.CDCvmLimit));
        et_TpmCaps.setText(StringUtil.bytesToHexString(param.TpmCaps));
        et_RecoveryLimit.setText(Integer.toString(param.RecoveryLimit));
        et_TerminalType.setText(Integer.toHexString(param.TerminalType));
        et_TermCountryCode.setText(Integer.toString(param.TermCountryCode));
        et_TAC_Denial.setText(StringUtil.bytesToHexString(param.TAC_Denial));
        et_TAC_OnLine.setText(StringUtil.bytesToHexString(param.TAC_OnLine));
        et_TAC_Default.setText(StringUtil.bytesToHexString(param.TAC_Default));
        et_TagList.setText(param.TagList);
    }

    private void saveParam() {
        //byte[] tmpByteArray;
        param.AppVersion = StringUtil.hexStringToByte(et_AppVersion.getText().toString());
        param.FloorLimit = Long.parseLong(et_FloorLimit.getText().toString());
        param.NoCvmLimit = Long.parseLong(et_NoCvmLimit.getText().toString());
        param.NoCDCvmLimit = Long.parseLong(et_NoCDCvmLimit.getText().toString());
        param.CDCvmLimit = Long.parseLong(et_CDCvmLimit.getText().toString());
        param.TpmCaps = StringUtil.hexStringToByte(et_TpmCaps.getText().toString());
        param.RecoveryLimit = Integer.parseInt(et_RecoveryLimit.getText().toString());
        param.TerminalType = Integer.parseInt(et_TerminalType.getText().toString(), 16);
        Log.w("Mir", "param.TerminalType: 0x" + Integer.toHexString(param.TerminalType));
        param.TermCountryCode = Integer.parseInt(et_TermCountryCode.getText().toString());
        param.TAC_Denial = StringUtil.hexStringToByte(et_TAC_Denial.getText().toString());
        param.TAC_OnLine = StringUtil.hexStringToByte(et_TAC_OnLine.getText().toString());
        param.TAC_Default = StringUtil.hexStringToByte(et_TAC_Default.getText().toString());
        param.TagList = et_TagList.getText().toString();
        ActivityTPMir.m_mirParam = param;
        Toast.makeText(context, "Parameter Saved", Toast.LENGTH_SHORT).show();
        finish();
    }
}
