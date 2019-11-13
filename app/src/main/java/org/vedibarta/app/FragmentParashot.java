package org.vedibarta.app;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

public class FragmentParashot extends ListFragment {

	public View onCreateView(LayoutInflater inflater, ViewGroup container,	 Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.list, container, false);
        CustomArray adapter = new CustomArray(getActivity(), R.layout.row_parasot, R.id.label, ParashotData.getParashotList());
		setListAdapter(adapter);
        TextView mTextView = (TextView) rootView.findViewById(R.id.memory);
		mTextView.setVisibility(View.GONE);
		return rootView;
	}

	class CustomArray extends ArrayAdapter<String> implements OnClickListener {

		ImageButton play;
		ImageButton download;
        String[] list;

		public CustomArray(Context context, int resource1, int resource2, String[] list) {
			super(context, resource1, resource2, list);
            this.list = list;
        }



        class ViewHolder{

			public ViewHolder(TextView label) {
				this.label = label;
			}
			int position;
			TextView label;
		}

		@Override
		public View getView(final int position, View convertView, ViewGroup parent) {
			ViewHolder viewHolder;
			if (convertView == null) {
				convertView = super.getView(position, null, parent);
				play = (ImageButton) convertView.findViewById(R.id.btn_play);
				download = (ImageButton) convertView.findViewById(R.id.btn_download);
				viewHolder = new ViewHolder((TextView)convertView.findViewById(R.id.label));
				play.setOnClickListener(this);
				download.setOnClickListener(this);
				viewHolder.label.setOnClickListener(this);
				convertView.setTag(viewHolder);
			} else
				viewHolder = (ViewHolder) convertView.getTag();
            viewHolder.label.setText(getItem(position));
            viewHolder.position = position;
			return convertView;
		}


		@Override
		public void onClick(View view) {
			if (Utilities.isNetworkAvailable(getActivity())){
				int  position =  ((ViewHolder)((View)view.getParent()).getTag()).position;;
				Intent i;
				switch (view.getId()) {
                    case R.id.label:
					case R.id.btn_play:
						((MyApplication)getActivity().getApplication()).setCurrentParashaPosition(position);
						Toast.makeText(getActivity(), getResources().getString(R.string.begin_playing), Toast.LENGTH_SHORT).show();
						i = new Intent(getContext(), PlayerActivity.class);
						startActivity(i);						break;
					case R.id.btn_download:
						Parasha parasha = ((MyApplication) getActivity().getApplication()).parahsot.get(position);
                        if (parasha.downloaded) {
                            Toast.makeText(getActivity(), R.string.alreadyDownloaded, Toast.LENGTH_LONG).show();
                            return;
                        }
                        long memory = (getActivity().getFilesDir().getFreeSpace()) / 1048576;
                        if ((memory > 300))
                            ((VedibartaActivity) getActivity()).updateDownload(parasha.label, position, memory);
                        else if ((getActivity().getExternalFilesDir(null).getFreeSpace() / 1048576) > 300){

                        } else
                            Toast.makeText(getActivity(),getString(R.string.full_memory), Toast.LENGTH_LONG).show();
                        break;
			    }
			} else {
				Toast.makeText(getActivity(),getResources().getString(R.string.no_internet), Toast.LENGTH_LONG).show();
			}
		}
    }
}