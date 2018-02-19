package org.team2180.scouting;

import android.R.drawable;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;
import org.team2180.scouting.listeners.DiscreteBarUpdater;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    public HashMap<String,JSONObject> teamData = new HashMap<>();
    static long countDown = 3;
    public final int REQUEST_ENABLE_BT = 910;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.viewContainer).setVisibility(View.GONE);

        // Initialize all bars and their labels; we need this to modify the labels based on the bar content

        //Spinners
        final Spinner teamSelector = (Spinner) findViewById(R.id.teamID);
            final ArrayAdapter<CharSequence> selectorAdapter = ArrayAdapter.createFromResource(this, R.array.team_numbers, android.R.layout.simple_spinner_item);
            teamSelector.setAdapter(selectorAdapter);
        final Spinner reportSelector = (Spinner) findViewById(R.id.viewTeamID);
            final ArrayAdapter<CharSequence> reportArrayAdapter = new ArrayAdapter<CharSequence>(
                    this, android.R.layout.simple_spinner_dropdown_item, new ArrayList<CharSequence>());
            reportArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            reportSelector.setAdapter(reportArrayAdapter);

        //SeekBars
        final SeekBar autoSwi = (SeekBar) findViewById(R.id.autoSwi);
        final SeekBar autoBal = (SeekBar) findViewById(R.id.autoBal);
        final SeekBar teleopCubeCount = (SeekBar) findViewById(R.id.teleopCubeCount);

        //Text Views
        final TextView txtAutoSwi = (TextView) findViewById(R.id.autoSwiTxt);
        final TextView txtAutoBal = (TextView) findViewById(R.id.autoBalTxt);
        final TextView txtTeleopCubeCount = (TextView) findViewById(R.id.teleopCubeCountTxt);

        //FABs
        final FloatingActionButton sendButton = (FloatingActionButton) findViewById(R.id.sendData);
        final FloatingActionButton swapButton = (FloatingActionButton) findViewById(R.id.switchMode);
        // /Initialize other variables

        final String spinnerTitle = ((TextView)findViewById(R.id.teamIDText)).getText().toString();
        //Bluetooth
        BluetoothAdapter blu = getEnabledBlueOrExit(swapButton, false);
        //--END INITIALIZATION--\\

        // Anonymous listener for the action buttons, doesn't deserve it's own class (yet)
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String text = gatherData();
                ((TextView)findViewById(R.id.JSONoutput)).setText(text);
                try {
                    JSONObject obj = new JSONObject(text);
                    int i = 0;
                    try {
                        while (teamData.get(obj.getString("teamID") + ":" + i) != null) {i++;}
                    }catch(JSONException e){}
                    teamData.put(obj.getString("teamID") + ":" + i,obj);
                    reportArrayAdapter.add(obj.getString("teamID") + ": Report "+i);
                }catch(JSONException e){
                    Log.e("Exception",e.toString());
                }
                ((EditText)findViewById(R.id.comments)).setText("");
            }
        });
        swapButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                if(findViewById(R.id.mainContainer).getVisibility()==View.VISIBLE){
                    findViewById(R.id.mainContainer).setVisibility(View.GONE);
                    findViewById(R.id.viewContainer).setVisibility(View.VISIBLE);
                    ((FloatingActionButton)view).setImageResource(drawable.ic_menu_edit);
                }else{
                    //mainContainer is gone
                    Log.d("viewCont",Boolean.toString(findViewById(R.id.viewComments).getVisibility()==View.VISIBLE));
                    findViewById(R.id.mainContainer).setVisibility(View.VISIBLE);
                    findViewById(R.id.viewContainer).setVisibility(View.GONE);
                    ((FloatingActionButton)view).setImageResource(drawable.ic_menu_info_details);
                }
            }
        });
        //--Team/Report Selector--\\
        teamSelector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                ((TextView)findViewById(R.id.teamIDText)).setText(
                        spinnerTitle + ": " + adapterView.getItemAtPosition(i).toString());
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        reportSelector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                ((TextView)findViewById(R.id.viewTeamIDText)).setText(
                        spinnerTitle + ": " + adapterView.getItemAtPosition(i).toString());
                    String reportID = adapterView.getItemAtPosition(i).toString().replaceAll(" Report ","");
                    displayTeamInfo(reportID);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        //--END TEAM/REPORT SELECTOR--\\

        // These listeners got extremely repetitive, so now they have their own class.
        // See org.team2180.scouting.listeners.DiscreteBarUpdater for more
        autoSwi.setOnSeekBarChangeListener(new DiscreteBarUpdater(autoSwi,txtAutoSwi));
        autoBal.setOnSeekBarChangeListener(new DiscreteBarUpdater(autoBal,txtAutoBal));
        teleopCubeCount.setOnSeekBarChangeListener(new DiscreteBarUpdater(teleopCubeCount,txtTeleopCubeCount));
        txtAutoSwi.setText(txtAutoSwi.getText() + ": " + autoSwi.getProgress());
        txtAutoBal.setText(txtAutoBal.getText() + ": " + autoBal.getProgress());
        txtTeleopCubeCount.setText(txtTeleopCubeCount.getText() + ": " + teleopCubeCount.getProgress());
        //Test
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver, filter);
        BluetoothAdapter.getDefaultAdapter().startDiscovery();
    }
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Discovery has found a device. Get the BluetoothDevice
                // object and its info from the Intent.
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address
                Log.d(deviceName,deviceHardwareAddress);
                int line = 173;
                if(deviceName.equals("max-pc") && deviceHardwareAddress.equals("10:02:B5:79:5E:D1")){
                    try {
                        BluetoothSocket sock = device.createRfcommSocketToServiceRecord(UUID.fromString("94f39d29-7d6d-437d-973b-fba39e49d4ee"));
                        line += 2;
                        BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
                        line += 2;
                        sock.connect();
                        sock.getOutputStream().write(10);
                        sock.getOutputStream().flush();
                        line += 2;
                        sock.close();
                    }catch(Exception e){
                        Log.e("ERROR@"+line,e.toString());
                    }
                }

            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Don't forget to unregister the ACTION_FOUND receiver.
        unregisterReceiver(mReceiver);
    }

    //Auto-generated
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }
    // Auto-generated
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public String gatherData(){
        // Probably not the best way to go about this.
        ViewGroup root = (ViewGroup) findViewById(R.id.mainContainer);
        String compJSONobj = "{";
        String justAComma = "";
        int itemsIndexed = 0;// Different from the actual index, counts the times data was put into the JSON string

        for(int i=0;i<root.getChildCount();i++){
            View subview = root.getChildAt(i);
            // Instead of adding a comma at the end of a entry and checking if there is a next entry,
            // Just put a line at the beginning of the entry if it's not the first one.
            if(itemsIndexed!=0){
                justAComma = ",";
            }
            // Could have made a conditional that tests if any of these are true, add name and syntax, and goes into subconditionals
            // But it feels more compact this way.
            if(subview instanceof SeekBar) {
                compJSONobj = compJSONobj + justAComma + '"' +
                        getResources().getResourceEntryName(subview.getId())
                        + '"' + ":" + '"' + ((TextView)findViewById(getResources().getIdentifier(getResources().getResourceEntryName(subview.getId())+"Txt","id",MainActivity.this.getPackageName()))).getText() + '"';
                itemsIndexed++;
            }else if(subview instanceof Spinner){
                compJSONobj = compJSONobj + justAComma + '"' +
                        getResources().getResourceEntryName(subview.getId())
                        + '"' + ":" + ((Spinner) subview).getSelectedItem().toString();
                itemsIndexed++;
            }else if(subview instanceof EditText){
                //Sanatize \ and "
                String cleanText = ((EditText) subview).getText().toString();
                int sI = 0;
                while(sI < cleanText.length()){
                   if(cleanText.charAt(sI)=='\\'){
                       cleanText = cleanText.substring(0,sI) + '\\' +
                               cleanText.substring(sI,cleanText.length());
                       sI++;
                   }else if(cleanText.charAt(sI)=='"'){
                       cleanText = cleanText.substring(0,sI) + '\\' +
                               cleanText.substring(sI,cleanText.length());
                       sI++;
                   }
                    sI++;

                }

                compJSONobj = compJSONobj + justAComma + '"' +
                        getResources().getResourceEntryName(subview.getId())
                        + '"' + ":" + '"' + cleanText+ '"';
                itemsIndexed++;
            }else if(subview instanceof CheckBox){
                compJSONobj = compJSONobj + justAComma + '"' +
                        getResources().getResourceEntryName(subview.getId())
                        + '"' + ":" + ((CheckBox) subview).isChecked();
                itemsIndexed++;
            }
        }
        compJSONobj = compJSONobj + "}";
        return compJSONobj;
    }
    public Byte[] toEncrypted(){
        //TODO: AES encryption of output from gatherData

        Byte[] byteArr = {};
        return byteArr;
    }
    public void displayTeamInfo(String reportID){
        JSONObject obj = teamData.get(reportID);
        if(obj==null){Log.e("MainActivity",reportID + "is not a valid Report ID!"); return;}
        Iterator<?> keys = obj.keys();
        while(keys.hasNext()){
            String key = (String)keys.next();
            String viewID = "view" + (key.charAt(0)+"").toUpperCase() + key.substring(1);
            View viewView = findViewById(getResources().getIdentifier(viewID,"id",MainActivity.this.getPackageName()));

            try{
                 if(viewView instanceof CheckBox){
                     ((CheckBox) viewView).setChecked(Boolean.valueOf(obj.get(key).toString()));
                     viewView.setClickable(false);
                }else if(viewView instanceof TextView) {
                     ((TextView) viewView).setText((obj.get(key).toString()));
                 }
            }catch(Exception e){
                Log.e("EXCEPTION",e.toString());
            }
        }
    }

    public BluetoothAdapter getEnabledBlueOrExit(View snackAttach,boolean fail){
        BluetoothAdapter blu = BluetoothAdapter.getDefaultAdapter();
        if(blu==null||fail){
            final String snackBarExitText = "No Bluetooth Adapter detected. App is now exiting";
            final Snackbar exitNoBlu = Snackbar.make(snackAttach,snackBarExitText,Snackbar.LENGTH_INDEFINITE);
            exitNoBlu.show();
            Handler handler = new Handler();
            handler.postDelayed(new Runnable(){
                public void run(){
                    //After delayMillis milliseconds
                    finish();
                    System.exit(1);
                }
            },countDown*1000);

        }else if (!blu.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent,REQUEST_ENABLE_BT);
        }

        return blu;
    }

    @Override
    public void onActivityResult (int requestCode, int resultCode, Intent data){
        if(requestCode==REQUEST_ENABLE_BT){
            if(!BluetoothAdapter.getDefaultAdapter().isEnabled())
                getEnabledBlueOrExit(findViewById(R.id.switchMode),true);
        }
    }
}
