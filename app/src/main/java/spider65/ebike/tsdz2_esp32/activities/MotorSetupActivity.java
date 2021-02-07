package spider65.ebike.tsdz2_esp32.activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import android.view.View;
import android.widget.TextView;

import java.util.Locale;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import spider65.ebike.tsdz2_esp32.R;
import spider65.ebike.tsdz2_esp32.TSDZBTService;
import spider65.ebike.tsdz2_esp32.TSDZConst;
import spider65.ebike.tsdz2_esp32.data.TSDZ_Config;


public class MotorSetupActivity extends AppCompatActivity {

    private static final int MAX_ANGLE = 10;
    private static final int MIN_OFFSET = 7;
    private static final int MAX_OFFSET = 40;
    private static final int MAX_DIFF = 30;

    private final IntentFilter mIntentFilter = new IntentFilter();
    private TextView angleValTV, hall124ValTV, hall356ValTV;
    private final TSDZ_Config cfg = new TSDZ_Config();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_motor_setup);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        angleValTV = findViewById(R.id.angleValTV);
        hall124ValTV = findViewById(R.id.hall124ValTV);
        hall356ValTV = findViewById(R.id.hall356ValTV);

        mIntentFilter.addAction(TSDZBTService.TSDZ_CFG_READ_BROADCAST);
        mIntentFilter.addAction(TSDZBTService.TSDZ_CFG_WRITE_BROADCAST);

        if (TSDZBTService.getBluetoothService() == null
                || TSDZBTService.getBluetoothService().getConnectionStatus() != TSDZBTService.ConnectionState.CONNECTED)
            showDialog("", getString(R.string.connection_error), true);
        else
            TSDZBTService.getBluetoothService().readCfg();
        showDialog(getString(R.string.warning), getString(R.string.warning_cfg), false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, mIntentFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
    }

    public void onButtonClick(View view) {
        int val;
        if (view.getId() == R.id.exitButton)
            finish();
        else if (view.getId() == R.id.saveButton) {
            boolean updated = false;

            int angleAdj = Integer.parseInt(angleValTV.getText().toString());
            if (angleAdj < 0) angleAdj = (256 + angleAdj);
            if (angleAdj != cfg.ui8_phase_angle_adj) {
                cfg.ui8_phase_angle_adj = angleAdj;
                updated = true;
            }

            int hall124Offset = Integer.parseInt(hall124ValTV.getText().toString());
            if (hall124Offset != cfg.ui8_hall_counter_offset_down) {
                cfg.ui8_hall_counter_offset_down = hall124Offset;
                updated = true;
            }

            int hall356Offset = Integer.parseInt(hall356ValTV.getText().toString());
            if (hall356Offset != cfg.ui8_hall_counter_offset_up) {
                cfg.ui8_hall_counter_offset_up = hall356Offset;
                updated = true;
            }

            if (updated)
                TSDZBTService.getBluetoothService().writeCfg(cfg);
            else
                showDialog("", getString(R.string.valuesNotChanged), false);
        } else if (view.getId() == R.id.angleAddBT) {
            val = Integer.parseInt(angleValTV.getText().toString());
            if (val < MAX_ANGLE)
                angleValTV.setText(String.valueOf(++val));
        } else if (view.getId() == R.id.angleSubBT) {
            val = Integer.parseInt(angleValTV.getText().toString());
            if (val > -MAX_ANGLE)
                angleValTV.setText(String.valueOf(--val));
        } else if (view.getId() == R.id.hall124AddBT) {
            val = Integer.parseInt(hall124ValTV.getText().toString());
            if ((val < MAX_OFFSET) && (val < Integer.parseInt(hall356ValTV.getText().toString())))
                hall124ValTV.setText(String.valueOf(++val));
        }  else if (view.getId() == R.id.hall124SubBT) {
            val = Integer.parseInt(hall124ValTV.getText().toString());
            if (val > MIN_OFFSET)
                hall124ValTV.setText(String.valueOf(--val));
        } else if (view.getId() == R.id.hall356AddBT) {
            val = Integer.parseInt(hall356ValTV.getText().toString());
            if (val < MAX_OFFSET+MAX_DIFF)
                hall356ValTV.setText(String.valueOf(++val));
        }  else if (view.getId() == R.id.hall356SubBT) {
            val = Integer.parseInt(hall356ValTV.getText().toString());
            if (val > Integer.parseInt(hall124ValTV.getText().toString()))
                hall356ValTV.setText(String.valueOf(--val));
        } else if (view.getId() == R.id.defaultBT) {
            angleValTV.setText("0");
            hall124ValTV.setText(String.format(Locale.getDefault(), "%d", TSDZConst.DEFAULT_HALL_DOWN_OFFSET));
            hall356ValTV.setText(String.format(Locale.getDefault(), "%d", TSDZConst.DEFAULT_HALL_UP_OFFSET));
            showDialog("", getString(R.string.defaultLoaded), false);
        }
    }

    private void showDialog (String title, String message, boolean exit) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        if (title != null)
            builder.setTitle(title);
        builder.setMessage(message);
        if (exit) {
            builder.setOnCancelListener((dialog) -> finish());
            builder.setPositiveButton(android.R.string.ok, (dialog, which) -> finish());
        } else
            builder.setPositiveButton(android.R.string.ok, null);
        builder.show();
    }

    private final BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() == null)
                return;
            switch (intent.getAction()) {
                case TSDZBTService.TSDZ_CFG_READ_BROADCAST:
                    if (!cfg.setData(intent.getByteArrayExtra(TSDZBTService.VALUE_EXTRA))) {
                        showDialog(getString(R.string.error), getString(R.string.cfgReadError), false);
                        break;
                    }
                    int v = cfg.ui8_phase_angle_adj;
                    if (v >= 128) v-=256;
                    angleValTV.setText(String.format(Locale.getDefault(),"%d", v));
                    hall124ValTV.setText(String.format(Locale.getDefault(), "%d", cfg.ui8_hall_counter_offset_down));
                    hall356ValTV.setText(String.format(Locale.getDefault(), "%d", cfg.ui8_hall_counter_offset_up));
                    findViewById(R.id.saveButton).setEnabled(true);
                    break;
                case TSDZBTService.TSDZ_CFG_WRITE_BROADCAST:
                    if (intent.getBooleanExtra(TSDZBTService.VALUE_EXTRA,false))
                        showDialog("", getString(R.string.cfgSaved), true);
                    else
                        showDialog(getString(R.string.error), getString(R.string.cfgSaveError), false);
                    break;
            }
        }
    };
}