package com.example.compsci.btplay01;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

// Ver03 -- send BT command as stream through socket
// Copy Listing 14.8 pp 402 and 14.3
// connects to an already enabled, paired LEGONXT with a given name

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    final String CC_ROBOTNAME = "NXT03";
    TextView cv_label, cv_conncStatusmsg;
    boolean cv_moveFlag = false;
    ListView cv_Blist;

    // BT Variables
    private BluetoothAdapter btInterface;
    private Set<BluetoothDevice> pairedDevices;
    private BluetoothSocket socket;
    private BluetoothDevice bd;

    private BroadcastReceiver btMonitor = null;
    private InputStream is = null;
    private OutputStream os = null;

    private Button cv_connectBtn, cv_disconnectBtn;
    private ImageButton cv_driveBackBtn,cv_driveStraightBtn,cv_driveLeftBtn,cv_driveRightBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        cv_label = (TextView)findViewById(R.id.xv_txtLabel);
        cv_connectBtn = (Button) findViewById(R.id.xv_connectBtn);
        cv_connectBtn.setOnClickListener(this);
        cv_disconnectBtn = (Button) findViewById(R.id.xv_diconnectBtn);
        cv_disconnectBtn.setOnClickListener(this);
        cv_disconnectBtn.setEnabled(false);

        cv_driveBackBtn=(ImageButton) findViewById(R.id.xv_driveBackBtn);
        cv_driveBackBtn.setOnClickListener(this);

        cv_driveStraightBtn=(ImageButton) findViewById(R.id.xv_driveStraightBtn);
        cv_driveStraightBtn.setOnClickListener(this);

        cv_driveLeftBtn=(ImageButton) findViewById(R.id.xv_driveLeftBtn);
        cv_driveLeftBtn.setOnClickListener(this);

        cv_driveRightBtn=(ImageButton) findViewById(R.id.xv_driveRightBtn);
        cv_driveRightBtn.setOnClickListener(this);



        cv_conncStatusmsg=(TextView)findViewById(R.id.xv_connectionStatus);

        setupBTMonitor();

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (cv_moveFlag) {
                    cfp_moveMotor(0, 75, 0x20);
                } else {
                    cfp_moveMotor(0, 75, 0x00);
                }
                cv_moveFlag = !cv_moveFlag;
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
       /* if (id == R.id.rmenu_conn) {
            cfp_connectNXT();
        }
        else if (id == R.id.rmenu_disconn) {
            cfp_disconnectNXT();
        }*/

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        super.onResume();
        registerReceiver(btMonitor, new IntentFilter("android.bluetooth.device.action.ACL_CONNECTED"));
        registerReceiver(btMonitor, new IntentFilter("android.bluetooth.device.action.ACL_DISCONNECTED"));
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(btMonitor);
    }

    private void cfp_connectNXT() {

        btInterface = BluetoothAdapter.getDefaultAdapter();
        pairedDevices = btInterface.getBondedDevices();
        ArrayList<String> btNames = new ArrayList<String>();


        Iterator<BluetoothDevice> it = pairedDevices.iterator();
        while (it.hasNext()) {
            bd = it.next();
            btNames.add(bd.getName() + "\n" + bd.getAddress());
        }

        final AlertDialog.Builder alertMsg = new AlertDialog.Builder(this);
        alertMsg.setTitle("   Select NXT Device");

        LayoutInflater inflater = getLayoutInflater();
        View convertView = (View) inflater.inflate(R.layout.bluetoothpairlist, null);
        alertMsg.setView(convertView);
        cv_Blist = (ListView) convertView.findViewById(R.id.listView1);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, btNames);
        cv_Blist.setAdapter(adapter);
        final AlertDialog cv_ad = alertMsg.show();

        TextView listHeader=new TextView(this);
        listHeader.setTextSize(18);
        listHeader.setText("\n        Paired Bluetooth Devices");
        listHeader.setTextColor(Color.BLACK);

        cv_Blist.addHeaderView(listHeader);

        cv_Blist.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Toast.makeText(getApplicationContext(),
                        String.format("Click ListItem " + position, position), Toast.LENGTH_SHORT).show();
                String deviceNm = (String) parent.getItemAtPosition(position);
                System.out.println("deviceNm---------------->>>>" + deviceNm);
                String[] lv_deviceNm = deviceNm.split("\n");
                Iterator<BluetoothDevice> it = pairedDevices.iterator();

                while (it.hasNext()) {
                    bd = it.next();
                    System.out.println("bd.getName()"+bd.getName());
                    if (bd.getName().equalsIgnoreCase(lv_deviceNm[0])){
                        System.out.println("inside");
                        try {
                            socket = bd.createRfcommSocketToServiceRecord(
                                    java.util.UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
                            socket.connect();

                        }
                        catch (Exception e) {
                            cv_conncStatusmsg.setText("Error interacting with remote device [" +
                                    e.getMessage() + "]");
                        }
                        break;
                    }
                }

                cv_ad.dismiss();

            }
        });
    }

    private void cfp_disconnectNXT() {
        try {
            socket.close();
            is.close();
            os.close();
            cv_conncStatusmsg.setText(bd.getName() + " is disconnected " );
        } catch (Exception e) {
            cv_conncStatusmsg.setText("Error in disconnect -> " + e.getMessage());
        }
    }

    private void setupBTMonitor() {
        btMonitor = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(
                        "android.bluetooth.device.action.ACL_CONNECTED")) {
                    try {
                        is = socket.getInputStream();
                        os = socket.getOutputStream();
                        cv_conncStatusmsg.setText("Connected to " + bd.getName());
                    } catch (Exception e) {
                        cfp_disconnectNXT();
                        is = null;
                        os = null;
                    }
                    ////cv_label.setText("Connection is good");
                }
                if (intent.getAction().equals(
                        "android.bluetooth.device.action.ACL_DISCONNECTED")) {
                    cv_conncStatusmsg.setText("Connection is broken");
                }
            }
        };
    }

    private void cfp_moveMotor(int motor,int speed, int state) {
        try {
            byte[] buffer = new byte[15];

            buffer[0] = (byte) (15-2);			//length lsb
            buffer[1] = 0;						// length msb
            buffer[2] =  0;						// direct command (with response)
            buffer[3] = 0x04;					// set output state
            buffer[4] = (byte) motor;			// output 1 (motor B)
            buffer[5] = (byte) speed;			// power
            buffer[6] = 1 + 2;					// motor on + brake between PWM
            buffer[7] = 0;						// regulation
            buffer[8] = 0;						// turn ration??
            buffer[9] = (byte) state; //0x20;					// run state
            buffer[10] = 0;
            buffer[11] = 0;
            buffer[12] = 0;
            buffer[13] = 0;
            buffer[14] = 0;

            os.write(buffer);
            os.flush();
        }
        catch (Exception e) {
            cv_conncStatusmsg.setText("Error in MoveForward(" + e.getMessage() + ")");
        }
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()){
            case R.id.xv_connectBtn:
                cfp_connectNXT();
                cv_connectBtn.setEnabled(false);
                cv_disconnectBtn.setEnabled(true);
                break;
            case R.id.xv_diconnectBtn:
                cfp_disconnectNXT();
                cv_connectBtn.setEnabled(true);
                cv_disconnectBtn.setEnabled(false);
                break;

            case R.id.xv_driveBackBtn:
                break;

            case R.id.xv_driveStraightBtn:
                break;

            case R.id.xv_driveLeftBtn:
                break;

            case R.id.xv_driveRightBtn:
                break;



        }

    }
}
