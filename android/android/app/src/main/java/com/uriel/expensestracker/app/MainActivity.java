package com.uriel.expensestracker.app;

import android.os.Bundle;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        // Custom plugins must be registered before super.onCreate() builds the bridge.
        registerPlugin(ReceiptScannerPlugin.class);
        super.onCreate(savedInstanceState);
    }
}
