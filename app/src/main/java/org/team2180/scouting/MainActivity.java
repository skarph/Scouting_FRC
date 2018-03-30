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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    public JSONObject teamData = new JSONObject();
    static long countDown = 3;
    public final int REQUEST_ENABLE_BT = 910;
    public final String deviceName = "2180-3";
    public final UUID serviceUUID = UUID.fromString("94f39d29-7d6d-437d-973b-fba39e49d4ed" +
            "");
    public ArrayAdapter<CharSequence> reportArrayAdapter;
    public ArrayAdapter<CharSequence> selectorArrayAdapter;
    ArrayAdapter<CharSequence> selectorAdapter;
    BluetoothDevice bluetoothDevice;
    public boolean hasReceivedTeams = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.viewContainer).setVisibility(View.GONE);

        // Initialize all bars and their labels; we need this to modify the labels based on the bar content

        //Spinners
        final Spinner teamSelector = (Spinner) findViewById(R.id.teamID);
        selectorAdapter = new ArrayAdapter(this, android.R.layout.simple_spinner_item,new ArrayList<CharSequence>());
        selectorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        teamSelector.setAdapter(selectorAdapter);

        final Spinner reportSelector = (Spinner) findViewById(R.id.viewTeamID);
        reportArrayAdapter = new ArrayAdapter<CharSequence>(
                this, android.R.layout.simple_spinner_dropdown_item, new ArrayList<CharSequence>());
        reportArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        reportSelector.setAdapter(reportArrayAdapter);
        //FABs
        final FloatingActionButton sendButton = (FloatingActionButton) findViewById(R.id.sendData);
        final FloatingActionButton swapButton = (FloatingActionButton) findViewById(R.id.switchMode);
        final FloatingActionButton refreshButton = (FloatingActionButton) findViewById(R.id.refreshButton);
        // /Initialize other variables

        final String spinnerTitle = ((TextView) findViewById(R.id.teamIDText)).getText().toString();
        //Bluetooth
        getEnabledBlueOrExit(swapButton,false);
        //--END INITIALIZATION--\\

        // Anonymous listener for the action buttons, doesn't deserve it's own class (yet)

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String text  = gatherData();
                Log.d("refresh",(((EditText)findViewById(R.id.comments)).getText().toString()));
                if(!(((EditText)findViewById(R.id.comments)).getText().toString().equals(""))) {
                    try {
                        JSONObject teamEntry = new JSONObject(text);
                        boolean didSend = sendEntry(text);

                        if (isDataInSet(teamEntry) == null && didSend) {
                            appendToTeamData(teamEntry);
                            ((EditText) findViewById(R.id.comments)).setText("");
                            Log.d("refresh", "adding new local data to entries!");
                            dispSnack(swapButton,"New information sent!",Snackbar.LENGTH_SHORT);
                        } else if (!didSend) {
                            dispSnack(swapButton,"A bad error occured when uploading! Please retry!",Snackbar.LENGTH_LONG);
                            Log.d("refresh", "failed to send entry!");
                        }

                    } catch (JSONException e) {
                        dispSnack(swapButton,"A bad error occured! Please restart your app and inform the developer",Snackbar.LENGTH_LONG);
                    }
                    ((TextView) findViewById(R.id.JSONoutput)).setText(text);
                }else{
                    dispSnack(swapButton,"Please write a comment in order to send data!",Snackbar.LENGTH_SHORT);
                    Log.d("refresh","no comments, not sending data!");
                }

            }
        });
        refreshButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                boolean didGet = receiveEntry();
                if(didGet){
                    dispSnack(swapButton,"Your device is now current with our database!",Snackbar.LENGTH_SHORT);
                }else{
                    dispSnack(swapButton,"A problem occured with trying to sync, please try again!",Snackbar.LENGTH_LONG);
                }
            }
        });

        swapButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (findViewById(R.id.mainContainer).getVisibility() == View.VISIBLE) {
                    findViewById(R.id.mainContainer).setVisibility(View.GONE);
                    findViewById(R.id.viewContainer).setVisibility(View.VISIBLE);
                    ((FloatingActionButton) view).setImageResource(drawable.ic_menu_edit);
                } else {
                    //mainContainer is gone
                    Log.d("viewCont", Boolean.toString(findViewById(R.id.viewComments).getVisibility() == View.VISIBLE));
                    findViewById(R.id.mainContainer).setVisibility(View.VISIBLE);
                    findViewById(R.id.viewContainer).setVisibility(View.GONE);
                    ((FloatingActionButton) view).setImageResource(drawable.ic_menu_info_details);
                }
            }
        });

        //--Team/Report Selector--\\
        teamSelector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                ((TextView) findViewById(R.id.teamIDText)).setText(
                        spinnerTitle + ": " + adapterView.getItemAtPosition(i).toString());
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        reportSelector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                ((TextView) findViewById(R.id.viewTeamIDText)).setText(
                        spinnerTitle + ": " + adapterView.getItemAtPosition(i).toString());
                String reportID = adapterView.getItemAtPosition(i).toString().replaceAll(" Report ", "");
                displayTeamInfo(reportID);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        //--END TEAM/REPORT SELECTOR--\\
        // These listeners got extremely repetitive, so now they have their own class.
        // See org.team2180.scouting.listeners.DiscreteBarUpdater for more;
        assignAllDiscreteBars();
        getTeamsFromServer(selectorAdapter);
    }

    //Auto-generated
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    public JSONObject isDataInSet(JSONObject data) throws JSONException{
        Iterator<?> teamItr = teamData.keys();
        while(teamItr.hasNext()) {
            JSONObject jobj= teamData.getJSONObject((String)teamItr.next());
            if(jobj.toString().equals(data.toString())){
                return jobj;
            }
        }
        return null;
    }

    public boolean sendEntry(String text){
        getTeamsFromServer(selectorAdapter);
        BluetoothSocket bS = null;
        try{
            bS = getSockToServer();
            Log.d("sendButton.onClick","found a sock! ");
            bS.connect();
        }catch(Exception e){
            Log.e("sendButton.onClick","trying fallback");
            try{
                bS = (BluetoothSocket) bS.getClass().getMethod("createRfcommSocket", new Class[] {int.class}).invoke(bS,1);
                bS.connect();
            }catch(Exception ioE){
                Log.e("sendButton.onClick{SNC}",e.toString());
                return false;
            }
        }

        if(bS==null){return false;}

        try {
            DataOutputStream bsO = new DataOutputStream(bS.getOutputStream());
            DataInputStream bsI = new DataInputStream(bS.getInputStream());
            bsO.writeInt(1);//Code upload
            bsO.flush();

            bsI.readInt();
            Log.d("sendButton.onClick", "sent condition code 1");
            bsO.writeUTF(text);
            bsO.flush();

            bS.close();
        }catch(Exception e){
            Log.e("sendBUtton.onclick",e.toString());
            return false;
        }
        return true;
    }

    public boolean receiveEntry(){
        getTeamsFromServer(selectorAdapter);

        BluetoothSocket bS = null;
        try{
            bS = getSockToServer();
            Log.d("receive.onClick","found a sock! ");
            bS.connect();
        }catch(Exception e){
            Log.e("receive.onClick","trying fallback");
            try{
                bS = (BluetoothSocket) bS.getClass().getMethod("createRfcommSocket", new Class[] {int.class}).invoke(bS,1);
                bS.connect();
            }catch(Exception ioE){
                Log.e("receive.onClick{SNC}",e.toString());
                return false;
            }
        }
        if(bS==null){return false;}
        try {
            DataInputStream bsI = new DataInputStream(bS.getInputStream());
            DataOutputStream bsO = new DataOutputStream(bS.getOutputStream());
            bsO.writeInt(2);//Code download
            bsO.flush();

            boolean serverStillHasData = true;
            int count = 0;
            int response = 2;

            while (serverStillHasData) {
                Log.d("refresh", "reading entry " + count);
                //check if data is a clone
                try {
                    JSONObject teamEntry = new JSONObject(bsI.readUTF());
                    if (isDataInSet(teamEntry) != null) {
                        Log.d("refresh", "entry " + count + " is the same as anoter entry, removing...");

                    }else {
                        Log.d("refresh", "entry " + count + " is new!");
                        appendToTeamData(teamEntry);
                    }
                } catch (JSONException e) {
                    bsO.writeInt(1);
                    bsO.close();
                    Log.e("refresh: ", "error", e);
                    bS.close();
                    return true;
                }
                try{
                    bsO.writeInt(2);
                    bsO.flush();
                    count++;
                    response = bsI.readInt();
                }catch(IOException e){};
                Log.d("refresh","server response:"+response);

                if (response==2){
                    Log.d("refresh", "Continuing on to " + count);
                    serverStillHasData = true;
                }else if(response==1){
                    Log.d("refresh", "Possible finish up at entry " + (count-1));
                    serverStillHasData = true;
                }else if(response==0){
                    Log.d("refresh", "Finishing up at entry " + (count-1)+"...");
                    serverStillHasData = false;
                }
            }

            bS.close();
            Log.d("refresh", "Finished up at entry " + (count-1));
        } catch (IOException e) {
            Log.e("receive.onClick", e.toString());
            try {
                bS.close();
                return false;
            } catch (IOException ioE) {
                Log.e("receive.onClick{SNC}", e.toString());
                return false;
            }
        }
        return true;
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

    public void displayTeamInfo(String reportID){
        try {
            JSONObject obj = teamData.getJSONObject(reportID);
            if (obj == null) {
                Log.e("MainActivity", reportID + "is not a valid Report ID!");
                return;
            }
            Iterator<?> keys = obj.keys();
            while (keys.hasNext()) {
                String key = (String) keys.next();
                String viewID = "view" + (key.charAt(0) + "").toUpperCase() + key.substring(1);
                View viewView = findViewById(getResources().getIdentifier(viewID, "id", MainActivity.this.getPackageName()));

                try {
                    if (viewView instanceof CheckBox) {
                        ((CheckBox) viewView).setChecked(Boolean.valueOf(obj.get(key).toString()));
                        viewView.setClickable(false);
                    } else if (viewView instanceof TextView) {
                        ((TextView) viewView).setText((obj.get(key).toString()));
                    }
                } catch (Exception e) {
                    Log.e("EXCEPTION", e.toString());
                }
            }
        }catch(Exception e){
            Log.e("displayTeamInfo", e.toString());
        }
    }
    public void appendToTeamData(JSONObject obj){
        try {
            int i = 0;
            try {
                while (teamData.get(obj.getString("teamID") + ":" + i) != null) {
                    i++;
                }
            } catch (JSONException e) {
            }
            teamData.put(obj.getString("teamID") + ":" + i, obj);
            reportArrayAdapter.add(obj.getString("teamID") + ": Report " + i);
        } catch (JSONException e) {
            Log.e("Exception", e.toString());
    }
}

    @Override
    public void onActivityResult (int requestCode, int resultCode, Intent data){
        if(requestCode==REQUEST_ENABLE_BT){
            if(!BluetoothAdapter.getDefaultAdapter().isEnabled())
                getEnabledBlueOrExit(findViewById(R.id.switchMode),true);
        }
    }
    protected void onDestroy() {
        super.onDestroy();
        // Don't forget to unregister the ACTION_FOUND receiver.
        try{
            unregisterReceiver(mReceiver);
        }catch(IllegalArgumentException e){};
    }

    public BluetoothAdapter getEnabledBlueOrExit(View snackAttach,boolean fail){
        BluetoothAdapter blu = BluetoothAdapter.getDefaultAdapter();
        if(blu==null||fail){
            dispSnack(snackAttach,"No bluetooth stack found, exiting...",Snackbar.LENGTH_INDEFINITE);
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
    public BluetoothSocket getSockToServer(){

        BluetoothAdapter  mBA = getEnabledBlueOrExit(findViewById(R.id.switchMode), false);
        Set<BluetoothDevice> pairedDevices = mBA.getBondedDevices();
        bluetoothDevice = null;
        //Already have device? Do nothing.
        if(bluetoothDevice==null) {
            //Already Paired?
            if (pairedDevices.size() > 0) {
                // There are paired devices. Get the name and address of each paired device.
                for (BluetoothDevice device : pairedDevices) {
                    if (device.getName().equals(deviceName)) {
                        bluetoothDevice = device;
                        break;
                    }
                }
            }
            //End; already paired
            //Found a device in already paired?
            if(bluetoothDevice==null) {
                //If not, then discover it.
                BluetoothAdapter.getDefaultAdapter().startDiscovery();
                IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
                registerReceiver(mReceiver, filter);
                //bluetoothDevice = device; ^^
            }
            if(bluetoothDevice==null){
                //Give up
                Log.e("getSockToServer","Could not find device'"+deviceName+"'");
                return null;
            }
        }
        try {
            return bluetoothDevice.createRfcommSocketToServiceRecord(serviceUUID);
        }catch(IOException e){
            //Probably a UUID error?
            Log.e("getSockToServer","Could not create a socket with '"+deviceName+"'");
            return null;
        }
    }
    public boolean getTeamsFromServer(ArrayAdapter<CharSequence> teamNumList){
        if(hasReceivedTeams){return false;}
        Log.d("recTeams.onClick","getting teams... ");
        BluetoothSocket bS = null;
        dispSnack(findViewById(R.id.switchMode),"Getting team numbers...",Snackbar.LENGTH_SHORT);
        try{
            bS = getSockToServer();
            Log.d("recTeams.onClick","found a sock! ");
            bS.connect();
        }catch(Exception e){
            Log.e("recTeams.onClick","trying fallback");
            try{
                bS = (BluetoothSocket) bS.getClass().getMethod("createRfcommSocket", new Class[] {int.class}).invoke(bS,1);
                bS.connect();
            }catch(Exception ioE){
                Log.e("recTeams.onClick{SNC}",e.toString());
                return false;
            }
        }
        if(bS==null){return false;}

        try {
            DataInputStream bsI = new DataInputStream(bS.getInputStream());
            DataOutputStream bsO = new DataOutputStream(bS.getOutputStream());
            bsO.writeInt(3);//Code team query
            bsO.flush();

            boolean serverStillHasData = true;
            int count = 0;
            int response = 2;

            while (serverStillHasData) {
                Log.d("recTeams", "reading entry " + count);
                //check if data is a clone
                try {
                    CharSequence teamNumber = (String)bsI.readUTF();
                    Log.d("recTeams",(String)teamNumber);
                    teamNumList.add(teamNumber);
                    Log.d("recTeams","sending ok!");
                    bsO.writeInt(2);
                    bsO.flush();
                    count++;
                    response = bsI.readInt();
                }catch(Exception e){
                    Log.e("recTeams","Couldn't get the team number because:",e);
                    bsO.writeInt(1);
                    bS.close();
                    return false;
                };//shh...
                Log.d("recTeams","server response:"+response);

                if (response==2){
                    Log.d("recTeams", "Continuing on to " + count);
                    serverStillHasData = true;
                }else if(response==0){
                    Log.d("recTeams", "Finishing up at entry " + (count-1)+"...");
                    serverStillHasData = false;
                }
                count++;
            }

            bS.close();
            Log.d("refresh", "Finished up at entry " + (count-1));
        } catch (IOException e) {
            Log.e("recTeams.onClick", e.toString());
            try {
                bS.close();
                return false;
            } catch (IOException ioE) {
                Log.e("recTeams.onClick{SNC}", e.toString());
                return false;
            }
        }
        hasReceivedTeams = true;
        return true;
    }
    public void dispSnack(View snackAttach, String text, int len){
        final Snackbar snackbar = Snackbar.make(snackAttach,text,len);
        snackbar.show();

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
                if(device.getName().equals(deviceName)){
                    bluetoothDevice = device;
                }
            }
            BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
        }
    };
    public void assignAllDiscreteBars(){
        int childTotal = ((ViewGroup)findViewById(R.id.mainContainer)).getChildCount();
        for(int i = 0; i<childTotal; i++){
            View child = ((ViewGroup)findViewById(R.id.mainContainer)).getChildAt(i);
            if(child instanceof SeekBar){
                Log.d("assignAll","Attempting for id:"+getResources().getResourceName(child.getId()));
                ((SeekBar) child).setOnSeekBarChangeListener(new DiscreteBarUpdater(
                        (SeekBar) child,
                        (TextView) findViewById(getResources().getIdentifier(child.getResources().getResourceName(child.getId())+"Txt","id",this.getPackageName()))));
                Log.d("assignAll","Is paired:"+getResources().getResourceName(child.getId()));
            }
        }
    }
}