package com.telpo.tps900_demo;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;

import com.telpo.util.StringUtil;

public class ActivityRupayParam extends PreferenceActivity {

    final static public String sp_emv_rupay = "emv_rupay";

    final static public String key_RupayParam_TermId = "RupayParam_TermId";
    final static public String key_RupayParam_TermAppVer = "RupayParam_TermAppVer";
    final static public String key_RupayParam_TermCapabilitis = "RupayParam_TermCapabilitis";
    final static public String key_RupayParam_TermAddCaps = "RupayParam_TermAddCaps";
    final static public String key_RupayParam_TermCountryCode = "RupayParam_TermCountryCode";
    final static public String key_RupayParam_TermType = "RupayParam_TermType";
    final static public String key_RupayParam_MerchCateCode = "RupayParam_MerchCateCode";
    final static public String key_RupayParam_NFC_TransLimit = "RupayParam_NFC_TransLimit";
    final static public String key_RupayParam_NFC_CVMLimit = "RupayParam_NFC_CVMLimit";
    final static public String key_RupayParam_NFC_OffLineFloorLimit = "RupayParam_NFC_OffLineFloorLimit";
    final static public String key_RupayParam_TornTimeLimitSec = "RupayParam_TornTimeLimitSec";

    final static public String key_rupay_RupayServParam_ServiceData = "rupay_RupayServParam_ServiceData";
    final static public String key_rupay_RupayServParam_LegacyPRMacq = "rupay_RupayServParam_LegacyPRMacq";
    final static public String key_rupay_RupayServParam_LegacyKCV = "rupay_RupayServParam_LegacyKCV";
    final static public String key_rupay_RupayServParam_NonLegacyPRMacqIndex = "rupay_RupayServParam_NonLegacyPRMacqIndex";
    final static public String key_rupay_RupayServParam_PRMiss = "rupay_RupayServParam_PRMiss";
    final static public String key_rupay_RupayServParam_ServiceQualifier = "rupay_RupayServParam_ServiceQualifier";
    final static public String key_rupay_RupayServParam_ServiceID = "rupay_RupayServParam_ServiceID";
    final static public String key_rupay_RupayServParam_ServiceManagerInfo = "rupay_RupayServParam_ServiceManagerInfo";
    final static public String key_rupay_RupayServParam_LegacyServiceCreate = "rupay_RupayServParam_LegacyServiceCreate";
    final static public String key_rupay_RupayServParam_ServiceCreate = "rupay_RupayServParam_ServiceCreate";

    EditTextPreference etpf_TermId, etpf_TermAppVer, etpf_TermCapabilitis, etpf_TermAddCaps, etpf_TermCountryCode, etpf_TermType,
            etpf_MerchCateCode, etpf_NFC_TransLimit, etpf_NFC_CVMLimit, etpf_NFC_OffLineFloorLimit, etpf_TornTimeLimitSec;

    EditTextPreference etpf_ServiceData, etpf_LegacyPRMacq, etpf_LegacyKCV, etpf_NonLegacyPRMacqIndex, etpf_PRMiss, etpf_ServiceQualifier,
            etpf_ServiceID, etpf_ServiceManagerInfo;

    //SwitchPreference spf_LegacyServiceCreate,spf_ServiceCreate;
    SharedPreferences sp;
    Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_rupay_param);
        addPreferencesFromResource(R.xml.preference_rupay_config);
        getPreferenceManager().setSharedPreferencesName(sp_emv_rupay);
        context = ActivityRupayParam.this;
        sp = getPreferenceManager().getSharedPreferences();
        viewInit();
    }

    void viewInit() {
        etpf_TermId = (EditTextPreference) findPreference(key_RupayParam_TermId);
        etpf_TermId.setSummary(sp.getString(key_RupayParam_TermId, ""));
        etpf_TermId.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if ((newValue.toString().length() != 8)) {
                    showRemind(context, "len must be 8,\n" + "Please input again");
                    return false;
                }
                etpf_TermId.setSummary((String) newValue);
                return true;
            }
        });

        etpf_TermAppVer = (EditTextPreference) findPreference(key_RupayParam_TermAppVer);
        etpf_TermAppVer.setSummary(sp.getString(key_RupayParam_TermAppVer, ""));
        etpf_TermAppVer.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if ((newValue.toString().length() != 4)) {
                    showRemind(context, "len must be 4," + "\nPlease input again");
                    return false;
                }

                if (StringUtil.hexStringToByte(newValue.toString()) == null) {
                    showRemind(context, "input must be hex format," + "\nPlease input again");
                    return false;
                }

                etpf_TermAppVer.setSummary((String) newValue);
                return true;
            }
        });

        etpf_TermCapabilitis = (EditTextPreference) findPreference(key_RupayParam_TermCapabilitis);
        etpf_TermCapabilitis.setSummary(sp.getString(key_RupayParam_TermCapabilitis, ""));
        etpf_TermCapabilitis.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if ((newValue.toString().length() != 6)) {
                    showRemind(context, "len must be 6," + "\nPlease input again");
                    return false;
                }

                if (StringUtil.hexStringToByte(newValue.toString()) == null) {
                    showRemind(context, "input must be hex format," + "\nPlease input again");
                    return false;
                }

                etpf_TermCapabilitis.setSummary((String) newValue);
                return true;
            }
        });

        etpf_TermAddCaps = (EditTextPreference) findPreference(key_RupayParam_TermAddCaps);
        etpf_TermAddCaps.setSummary(sp.getString(key_RupayParam_TermAddCaps, ""));
        etpf_TermAddCaps.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if ((newValue.toString().length() != 10)) {
                    showRemind(context, "len must be 10," + "\nPlease input again");
                    return false;
                }
                if (StringUtil.hexStringToByte(newValue.toString()) == null) {
                    showRemind(context, "input must be hex format," + "\nPlease input again");
                    return false;
                }
                etpf_TermAddCaps.setSummary((String) newValue);
                return true;
            }
        });

        etpf_TermCountryCode = (EditTextPreference) findPreference(key_RupayParam_TermCountryCode);
        etpf_TermCountryCode.setSummary(sp.getString(key_RupayParam_TermCountryCode, ""));
        etpf_TermCountryCode.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                etpf_TermCountryCode.setSummary((String) newValue);
                return true;
            }
        });

        etpf_TermType = (EditTextPreference) findPreference(key_RupayParam_TermType);
        etpf_TermType.setSummary(sp.getString(key_RupayParam_TermType, ""));
        etpf_TermType.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                etpf_TermType.setSummary((String) newValue);
                return true;
            }
        });

        etpf_MerchCateCode = (EditTextPreference) findPreference(key_RupayParam_MerchCateCode);
        etpf_MerchCateCode.setSummary(sp.getString(key_RupayParam_MerchCateCode, ""));
        etpf_MerchCateCode.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if ((newValue.toString().length() != 4)) {
                    showRemind(context, "len must be 4," + "\nPlease input again");
                    return false;
                }
                if (StringUtil.hexStringToByte(newValue.toString()) == null) {
                    showRemind(context, "input must be hex format," + "\nPlease input again");
                    return false;
                }
                etpf_MerchCateCode.setSummary((String) newValue);
                return true;
            }
        });

        etpf_NFC_TransLimit = (EditTextPreference) findPreference(key_RupayParam_NFC_TransLimit);
        etpf_NFC_TransLimit.setSummary(sp.getString(key_RupayParam_NFC_TransLimit, ""));
        etpf_NFC_TransLimit.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                etpf_NFC_TransLimit.setSummary((String) newValue);
                return true;
            }
        });

        etpf_NFC_CVMLimit = (EditTextPreference) findPreference(key_RupayParam_NFC_CVMLimit);
        etpf_NFC_CVMLimit.setSummary(sp.getString(key_RupayParam_NFC_CVMLimit, ""));
        etpf_NFC_CVMLimit.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                etpf_NFC_CVMLimit.setSummary((String) newValue);
                return true;
            }
        });

        etpf_NFC_OffLineFloorLimit = (EditTextPreference) findPreference(key_RupayParam_NFC_OffLineFloorLimit);
        etpf_NFC_OffLineFloorLimit.setSummary(sp.getString(key_RupayParam_NFC_OffLineFloorLimit, ""));
        etpf_NFC_OffLineFloorLimit.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                etpf_NFC_OffLineFloorLimit.setSummary((String) newValue);
                return true;
            }
        });

        etpf_TornTimeLimitSec = (EditTextPreference) findPreference(key_RupayParam_TornTimeLimitSec);
        etpf_TornTimeLimitSec.setSummary(sp.getString(key_RupayParam_TornTimeLimitSec, ""));
        etpf_TornTimeLimitSec.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                etpf_TornTimeLimitSec.setSummary((String) newValue);
                return true;
            }
        });


        etpf_ServiceData = (EditTextPreference) findPreference(key_rupay_RupayServParam_ServiceData);
        etpf_ServiceData.setSummary(sp.getString(key_rupay_RupayServParam_ServiceData, ""));
        etpf_ServiceData.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (StringUtil.hexStringToByte(newValue.toString()) == null) {
                    showRemind(context, "input must be hex format," + "\nPlease input again");
                    return false;
                }
                etpf_ServiceData.setSummary((String) newValue);
                return true;
            }
        });

        etpf_LegacyPRMacq = (EditTextPreference) findPreference(key_rupay_RupayServParam_LegacyPRMacq);
        etpf_LegacyPRMacq.setSummary(sp.getString(key_rupay_RupayServParam_LegacyPRMacq, ""));
        etpf_LegacyPRMacq.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if ((newValue.toString().length() != 16)) {
                    showRemind(context, "len must be 16," + "\nPlease input again");
                    return false;
                }
                if (StringUtil.hexStringToByte(newValue.toString()) == null) {
                    showRemind(context, "input must be hex format," + "\nPlease input again");
                    return false;
                }
                etpf_LegacyPRMacq.setSummary((String) newValue);
                return true;
            }
        });

        etpf_LegacyKCV = (EditTextPreference) findPreference(key_rupay_RupayServParam_LegacyKCV);
        etpf_LegacyKCV.setSummary(sp.getString(key_rupay_RupayServParam_LegacyKCV, ""));
        etpf_LegacyKCV.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if ((newValue.toString().length() != 6)) {
                    showRemind(context, "len must be 6," + "\nPlease input again");
                    return false;
                }
                if (StringUtil.hexStringToByte(newValue.toString()) == null) {
                    showRemind(context, "input must be hex format," + "\nPlease input again");
                    return false;
                }
                etpf_LegacyKCV.setSummary((String) newValue);
                return true;
            }
        });

        etpf_NonLegacyPRMacqIndex = (EditTextPreference) findPreference(key_rupay_RupayServParam_NonLegacyPRMacqIndex);
        etpf_NonLegacyPRMacqIndex.setSummary(sp.getString(key_rupay_RupayServParam_NonLegacyPRMacqIndex, ""));
        etpf_NonLegacyPRMacqIndex.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                etpf_NonLegacyPRMacqIndex.setSummary((String) newValue);
                return true;
            }
        });

        etpf_PRMiss = (EditTextPreference) findPreference(key_rupay_RupayServParam_PRMiss);
        etpf_PRMiss.setSummary(sp.getString(key_rupay_RupayServParam_PRMiss, ""));
        etpf_PRMiss.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if ((newValue.toString().length() != 32)) {
                    showRemind(context, "len must be 32," + "\nPlease input again");
                    return false;
                }
                if (StringUtil.hexStringToByte(newValue.toString()) == null) {
                    showRemind(context, "input must be hex format," + "\nPlease input again");
                    return false;
                }
                etpf_PRMiss.setSummary((String) newValue);
                return true;
            }
        });

        etpf_ServiceQualifier = (EditTextPreference) findPreference(key_rupay_RupayServParam_ServiceQualifier);
        etpf_ServiceQualifier.setSummary(sp.getString(key_rupay_RupayServParam_ServiceQualifier, ""));
        etpf_ServiceQualifier.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if ((newValue.toString().length() != 10)) {
                    showRemind(context, "len must be 10," + "\nPlease input again");
                    return false;
                }
                if (StringUtil.hexStringToByte(newValue.toString()) == null) {
                    showRemind(context, "input must be hex format," + "\nPlease input again");
                    return false;
                }
                etpf_ServiceQualifier.setSummary((String) newValue);
                return true;
            }
        });

        etpf_ServiceID = (EditTextPreference) findPreference(key_rupay_RupayServParam_ServiceID);
        etpf_ServiceID.setSummary(sp.getString(key_rupay_RupayServParam_ServiceID, ""));
        etpf_ServiceID.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if ((newValue.toString().length() != 4)) {
                    showRemind(context, "len must be 4," + "\nPlease input again");
                    return false;
                }
                if (StringUtil.hexStringToByte(newValue.toString()) == null) {
                    showRemind(context, "input must be hex format," + "\nPlease input again");
                    return false;
                }
                etpf_ServiceID.setSummary((String) newValue);
                return true;
            }
        });

        etpf_ServiceManagerInfo = (EditTextPreference) findPreference(key_rupay_RupayServParam_ServiceManagerInfo);
        etpf_ServiceManagerInfo.setSummary(sp.getString(key_rupay_RupayServParam_ServiceManagerInfo, ""));
        etpf_ServiceManagerInfo.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if ((newValue.toString().length() != 4)) {
                    showRemind(context, "len must be 4," + "\nPlease input again");
                    return false;
                }
                if (StringUtil.hexStringToByte(newValue.toString()) == null) {
                    showRemind(context, "input must be hex format," + "\nPlease input again");
                    return false;
                }
                etpf_ServiceManagerInfo.setSummary((String) newValue);
                return true;
            }
        });

    }

    public void showRemind(Context mContext, String text) {
        new AlertDialog.Builder(mContext)
                .setMessage(text)
                // 为对话框设置一个“确定”按钮
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog,
                                        int which) {

                    }
                })
                .create()
                .show();
    }

}
