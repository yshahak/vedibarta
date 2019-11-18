package org.vedibarta.app;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

//import com.splunk.mint.Mint;

public class FragmentHoradot extends ListFragment {

    private long freeSD = 0;
    private ArrayList<String> existParashotList = new ArrayList<>();
    private ArrayList<Integer> downloadedParsIndexes;
    private CustomArray adapter;


    @SuppressLint("SetTextI18n")
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.list, container, false);
        ((VedibartaActivity) getActivity()).addBtnToPlayerActivityIfNeeded(rootView);
        TextView mTextView = rootView.findViewById(R.id.memory);
        File SD = getActivity().getExternalFilesDir(null);
        long freeInternal = (getActivity().getFilesDir().getFreeSpace()) / 1048576;
        if (SD != null)
            freeSD = SD.getFreeSpace() / 1048576;
        mTextView.setText(getResources().getString(R.string.spaceSD)
                + freeSD + "MB" + '\n'
                + getResources().getString(R.string.space_internal)
                + freeInternal + "MB");
        adapter = new CustomArray(getActivity(), R.layout.row2, R.id.text1, existParashotList);
        setListAdapter(adapter);
        getFiles();


        return rootView;
    }

    public void onRowClick(final int position) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(getResources().getString(R.string.action_to_made))
                .setPositiveButton(
                        getResources().getString(R.string.listening),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                Toast.makeText(
                                        getActivity(),
                                        getResources().getString(
                                                R.string.begin_playing),
                                        Toast.LENGTH_SHORT).show();
                                playParasha(position);

                            }
                        })
                .setNegativeButton(getResources().getString(R.string.delete),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                deleteParasha(position);
                            }
                        });
        // Create the AlertDialog object and return it
        AlertDialog mdialog = builder.create();
        mdialog.show();

    }

    private void getFiles() {
        existParashotList.clear();
        final Context activity = getActivity();
        if (activity != null) {
            downloadedParsIndexes = ParashotData.getDownloadedParsIndexes(activity);
            if (downloadedParsIndexes.size() > 0) {
                for (int i = 0; i < downloadedParsIndexes.size(); i++) {
                    existParashotList.add(i, ParashotData.getParashaHeb(downloadedParsIndexes.get(i)));
                }
            }
        }
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onResume() {
        super.onResume();
        adapter.notifyDataSetChanged();
    }

    private void playParasha(int position) {
        final FragmentActivity activity = getActivity();
        if (activity != null) {
            ((OnStartPlayClicked) activity).onStartPlayClicked(downloadedParsIndexes.get(position));
        }
    }

    private void deleteParasha(int position) {
        String relativePath = ParashotData.getRelativePath(downloadedParsIndexes.get(position), 0);
        Utilities.deleteParasha(getContext(), relativePath);
        try {
            Utilities.updateLine(getActivity(), position);
        } catch (IOException e) {
            e.printStackTrace();
        }
        getFiles();

    }

    class CustomArray extends ArrayAdapter<String> {

        CustomArray(Context context, int resource1, int resource2, ArrayList<String> list) {
            super(context, resource1, resource2, list);
        }

        @NonNull
        @Override
        public View getView(final int position, View convertView, @NonNull ViewGroup parent) {

            View row;
            row = super.getView(position, convertView, parent);
            row.setOnClickListener((new OnClickListener() {
                @Override
                public void onClick(View v) {
                    onRowClick(position);
                }
            }));
            row.findViewById(R.id.button1).setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    playParasha(position);
                }
            });
            row.findViewById(R.id.button2).setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    deleteParasha(position);
                }
            });
            return row;
        }
    }
}