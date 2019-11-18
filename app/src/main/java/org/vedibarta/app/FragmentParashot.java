package org.vedibarta.app;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class FragmentParashot extends ListFragment {

    private ArrayList<Integer> downloadedParsIndexes;
    private CustomArray adapter;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        downloadedParsIndexes = ParashotData.getDownloadedParsIndexes(getContext());
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.list, container, false);
        ((VedibartaActivity) getActivity()).addBtnToPlayerActivityIfNeeded(rootView);
        adapter = new CustomArray(getActivity(), R.layout.row_parasot, R.id.label, ParashotData.getParashotList());
        setListAdapter(adapter);
        TextView mTextView = rootView.findViewById(R.id.memory);
        mTextView.setVisibility(View.GONE);
        return rootView;
    }

    public void refreshList(){
        downloadedParsIndexes = ParashotData.getDownloadedParsIndexes(getContext());
        adapter.notifyDataSetChanged();
    }

    class CustomArray extends ArrayAdapter<String> implements OnClickListener {


        String[] list;

        CustomArray(Context context, int resource1, int resource2, String[] list) {
            super(context, resource1, resource2, list);
            this.list = list;
        }


        class ViewHolder {

            ViewHolder(TextView label, View play, View downloadBtn) {
                this.label = label;
                this.play = play;
                this.downloadBtn = downloadBtn;
            }

            int position;
            final TextView label;
            final View play;
            final View downloadBtn;
        }

        @NonNull
        @Override
        public View getView(final int position, View convertView, @NonNull ViewGroup parent) {
            ViewHolder viewHolder;
            if (convertView == null) {
                convertView = super.getView(position, null, parent);
                viewHolder = new ViewHolder((TextView) convertView.findViewById(R.id.label), convertView.findViewById(R.id.btn_play), convertView.findViewById(R.id.btn_download));
                viewHolder.play.setOnClickListener(this);
                viewHolder.downloadBtn.setOnClickListener(this);
                viewHolder.label.setOnClickListener(this);
                convertView.setTag(viewHolder);
            } else
                viewHolder = (ViewHolder) convertView.getTag();
            viewHolder.label.setText(getItem(position));
            viewHolder.position = position;
            viewHolder.downloadBtn.setEnabled(!downloadedParsIndexes.contains(position));
            viewHolder.downloadBtn.setVisibility(downloadedParsIndexes.contains(position) ? View.INVISIBLE : View.VISIBLE);
            return convertView;
        }


        @Override
        public void onClick(View view) {
            final FragmentActivity activity = getActivity();
            if (activity != null) {
                if (Utilities.isNetworkAvailable(activity)) {
                    int position = ((ViewHolder) ((View) view.getParent()).getTag()).position;
                    switch (view.getId()) {
                        case R.id.label:
                        case R.id.btn_play:
                            ((OnStartPlayClicked) activity).onStartPlayClicked(position);
                            break;
                        case R.id.btn_download:
                            Parasha parasha = ((MyApplication) activity.getApplication()).getParahsot().get(position);
//                            if (parasha.downloaded) {
//                                Toast.makeText(activity, R.string.alreadyDownloaded, Toast.LENGTH_LONG).show();
//                                return;
//                            }
                            long memory = (activity.getFilesDir().getFreeSpace()) / 1048576;
                            if ((memory > 300))
                                ((VedibartaActivity) activity).startDownload(parasha.label, position);
//                            else if ((activity.getExternalFilesDir(null).getFreeSpace() / 1048576) > 300) {
//
//                            }
                            else
                                Toast.makeText(activity, getString(R.string.full_memory), Toast.LENGTH_LONG).show();
                            break;
                    }
                } else {
                    Toast.makeText(activity, getResources().getString(R.string.no_internet), Toast.LENGTH_LONG).show();
                }

            }
        }
    }
}