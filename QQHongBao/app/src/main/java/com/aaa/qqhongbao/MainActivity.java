package com.aaa.qqhongbao;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityManager;
import android.widget.Button;

import java.util.List;

public class MainActivity extends AppCompatActivity {
    private final Intent mAccessibleIntent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
    private Button switchPlugin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //you have to start the service once.
        startService(new Intent(MainActivity.this, Service1.class));


        switchPlugin = (Button) findViewById(R.id.button_accessible);
        switchPlugin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(mAccessibleIntent);
            }
        });
        updateServiceStatus();
    }    /*开启插件的按钮*/

    public void onButtonClicked(View view) {
        startActivity(mAccessibleIntent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateServiceStatus();
    }



    private void updateServiceStatus() {
        boolean serviceEnabled = false;
        AccessibilityManager accessibilityManager = (AccessibilityManager) getSystemService(Context.ACCESSIBILITY_SERVICE);
        List<AccessibilityServiceInfo> accessibilityServices = accessibilityManager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC);
        for (AccessibilityServiceInfo info : accessibilityServices) {
            if (info.getId().equals(getPackageName() + "/.QQHongbaoService")) {
                serviceEnabled = true;
                break;
            }
        }
        if (serviceEnabled) {
            switchPlugin.setText("关闭插件");
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            switchPlugin.setText("开启插件");
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }
}