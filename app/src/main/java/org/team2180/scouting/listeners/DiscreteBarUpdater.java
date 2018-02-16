package org.team2180.scouting.listeners;

import android.widget.SeekBar;
import android.widget.TextView;

public class DiscreteBarUpdater implements SeekBar.OnSeekBarChangeListener {

    SeekBar seekbar = null;
    TextView targetTextView = null;
    String originalText = null;
    public DiscreteBarUpdater(SeekBar seekbar, TextView targetTextView){
        /**
         * Listener Constructor takes the SeekBar it is tied to and the appropriate text associated with it
         * and updates the text, taggomg the current value of the SeekBar at the end of the text line.
         */
        this.seekbar = seekbar;
        this.targetTextView = targetTextView;
        this.originalText = targetTextView.getText().toString();
    }
    @Override
    public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
        this.targetTextView.setText(this.originalText + ": " + seekbar.getProgress());
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }
}
