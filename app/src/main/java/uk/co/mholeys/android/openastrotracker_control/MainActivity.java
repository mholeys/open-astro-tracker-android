package uk.co.mholeys.android.openastrotracker_control;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainAct";

    private MountViewModel mountViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        BottomNavigationView navView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_control, R.id.navigation_polar_align, R.id.navigation_connection)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(navView, navController);

        mountViewModel = MountViewModel.getInstance(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mountViewModel.doUnbindService();
    }

    @Override
    protected void onResume() {
        super.onResume();

        final ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        final List<ActivityManager.RunningServiceInfo> services = activityManager.getRunningServices(Integer.MAX_VALUE);
        boolean running = false;
        for (ActivityManager.RunningServiceInfo runningServiceInfo : services) {
            if (runningServiceInfo.service.getClassName().equals(BluetoothOTAService.class.getName())){
                running = true;
            }
        }
        if (running) {
            mountViewModel.doBindService();
        }
        // TODO: investigate
//        incomingMessenger = new Messenger(new OTAHandler(this));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mountViewModel.unregisterReceivers(this);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case BluetoothSearch.REQUEST_ENABLE_BT:
                if (resultCode == Activity.RESULT_OK) {
                    // Start searching/connect
                    mountViewModel.enableBluetoothUse();
                    mountViewModel.discover();
                } else {
                    // Handle disabled bluetooth
                    mountViewModel.disableBluetoothUse();
                }
                break;
            default:
                break;
        }
    }
}
