package com.qg.reshade;
import android.app.Activity;
import android.os.Bundle;
public class MainActivity extends Activity {
    static { System.loadLibrary("qgreshade"); }
    
    private native String probeKernel();
    private native float[] getGtaoParams();
    
    public float[] readGtaoParams() { 
        return getGtaoParams(); 
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(new FieldRenderer(this));
    }
}
