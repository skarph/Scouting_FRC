package org.team2180.scouting;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.SeekBar;

import org.team2180.scouting.listeners.DiscreteBarUpdater;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Initialize all bars and their labels; we need this to modify the labels based on the bar content
        final SeekBar autoSwi = (SeekBar) findViewById(R.id.autoSwi);
        final SeekBar autoBal = (SeekBar) findViewById(R.id.autoBal);
        final SeekBar teleopCubeCount = (SeekBar) findViewById(R.id.teleopCubeCount);
        final TextView txtAutoSwi = (TextView) findViewById(R.id.autoSwiTxt);
        final TextView txtAutoBal = (TextView) findViewById(R.id.autoBalTxt);
        final TextView txtTeleopCubeCount = (TextView) findViewById(R.id.teleopCubeCountTxt);
        final FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);

        // Anonymous listener for the action button, doesn't deserve it's own class (yet)
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((TextView)findViewById(R.id.comments)).setText(gatherData());
            }
        });
        // These listeners got extremely repetitive, so now they have their own class.
        // See org.team2180.scouting.listeners.DiscreteBarUpdater for more
        autoSwi.setOnSeekBarChangeListener(new DiscreteBarUpdater(autoSwi,txtAutoSwi));
        autoBal.setOnSeekBarChangeListener(new DiscreteBarUpdater(autoBal,txtAutoBal));
        teleopCubeCount.setOnSeekBarChangeListener(new DiscreteBarUpdater(teleopCubeCount,txtTeleopCubeCount));


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
            if(subview instanceof SeekBar){
                compJSONobj = compJSONobj + justAComma + '"' +
                        getResources().getResourceEntryName(subview.getId())
                        + '"' + ":" + ((SeekBar) subview).getProgress();
                itemsIndexed++;
            }else if(subview instanceof EditText){
                compJSONobj = compJSONobj + justAComma + '"' +
                        getResources().getResourceEntryName(subview.getId())
                        + '"' + ":" + '"' + ((EditText) subview).getText().toString()+ '"';
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
}
