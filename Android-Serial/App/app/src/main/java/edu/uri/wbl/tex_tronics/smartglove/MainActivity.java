package edu.uri.wbl.tex_tronics.smartglove;

import android.Manifest;
import android.content.Context;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import edu.uri.wbl.tex_tronics.smartglove.smart_glove.SmartGloveManagerService;
import edu.uri.wbl.tex_tronics.smartglove.ble.GattDevices;
import edu.uri.wbl.tex_tronics.smartglove.smart_glove.SmartGloveState;

public class MainActivity extends AppCompatActivity {
    private static final int PERMISSION_REQUEST_CODE = 340;
    private static final String[] PERMISSIONS = {Manifest.permission.WRITE_EXTERNAL_STORAGE};

    private Context mContext;
    private SmartGloveState mSmartGloveState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Store reference to Activity's Context (used by inner classes and callbacks)
        mContext = this;

        // Initialize SmartGlove State Machine
        mSmartGloveState = SmartGloveState.NOT_SELECTED;
        // TODO: If Device saved, initialize state at Disconnected

        // Initialize UI
        Button connectBtn = findViewById(R.id.connect_btn);
        connectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SmartGloveManagerService.connect(mContext, GattDevices.SMART_GLOVE_DEVICE);
            }
        });

        Button disconnectBtn = findViewById(R.id.disconnect_btn);
        disconnectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SmartGloveManagerService.disconnect(mContext, GattDevices.SMART_GLOVE_DEVICE);
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Request Permissions at Runtime (Marshmallow+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(PERMISSIONS, PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        // TODO: Handle event Permissions denied
    }
}