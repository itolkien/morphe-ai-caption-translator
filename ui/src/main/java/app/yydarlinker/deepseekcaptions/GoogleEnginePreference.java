package app.yydarlinker.deepseekcaptions;

import android.app.AlertDialog;
import android.content.Context;
import android.util.AttributeSet;

/** Select the translation provider without changing existing API profiles. */
@SuppressWarnings("deprecation")
public final class GoogleEnginePreference extends CaptionSettingPreference {
    private static final String PREFS="caption_translation_engine";
    private static final String KEY="engine";
    public GoogleEnginePreference(Context c){super(c);init();}
    public GoogleEnginePreference(Context c,AttributeSet a){super(c,a);init();}
    public GoogleEnginePreference(Context c,AttributeSet a,int d){super(c,a,d);init();}
    public GoogleEnginePreference(Context c,AttributeSet a,int d,int r){super(c,a,d,r);init();}
    static boolean selected(Context c){return "google".equals(c.getSharedPreferences(PREFS,0).getString(KEY,"ai"));}
    private void init(){setPersistent(false);setIconSpaceReserved(false);refresh();}
    private void refresh(){setTitle("Translation engine");setSummary(selected(getContext())?"Google Translate (experimental, no API key)":"AI provider (API key)");}
    @Override protected void onBindView(android.view.View v){refresh();super.onBindView(v);}
    @Override protected void onClick(){
        Context c=getContext();new AlertDialog.Builder(c).setTitle("Translation engine")
            .setSingleChoiceItems(new String[]{"AI provider (API key)","Google Translate (experimental, unofficial endpoint)"},selected(c)?1:0,
                (dialog,which)->{
                    c.getSharedPreferences(PREFS,0).edit().putString(KEY,which==1?"google":"ai").apply();
                    refresh();dialog.dismiss();CaptionActions.configuration();
                }).setNegativeButton(android.R.string.cancel,null).show();
    }
}
