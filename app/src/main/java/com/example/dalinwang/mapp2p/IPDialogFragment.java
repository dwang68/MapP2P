package com.example.dalinwang.mapp2p;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;

/**
 * Created by Dario on 4/30/2015.
 */

public class IPDialogFragment extends DialogFragment {
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View v = inflater.inflate(R.layout.ip_dialog, null);

        final EditText editTextAddress = (EditText)v.findViewById(R.id.address);
        final EditText editTextPort = (EditText)v.findViewById(R.id.port);

        builder.setView(v)
                .setTitle(R.string.ip_dialog_title)
                .setNegativeButton(R.string.connect, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        String address = editTextAddress.getText().toString();
                        int port = Integer.parseInt(editTextPort.getText().toString());
                        ((MapsActivity)getActivity()).connect(address, port);
                    }
                });
        return builder.create();
    }
}