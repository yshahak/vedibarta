package org.vedibarta.app;

import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class List_View1 extends ListFragment {
	private AlertDialog mdialog;
	int pstn;
	ParashotData data;
	Utilities util;
	String path;
	String item = null;
	Context ctx;
	CustomArray adapter;
	private TextView mTextView;

	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.list, container, false);
		ctx = getActivity();
		data = new ParashotData();
		util = new Utilities();
		adapter = new CustomArray(ctx, R.layout.row1, R.id.text1,
				data.getParashotList());
		setListAdapter(adapter);
		mTextView = (TextView) rootView.findViewById(R.id.memory);
		mTextView.setVisibility(View.GONE);
		//mTextView.setText("גרסא 1.8");
		
		if (util.isMyServiceRunning(ctx) && (boolean)((Activity) ctx).getIntent().getBooleanExtra("playing", false)) {
			Button myButton = new Button(ctx);
			myButton.setText(R.string.backToPlayer);
			myButton.setTextSize(18);
			myButton.setOnClickListener(new View.OnClickListener() {
			    @Override
			    public void onClick(View v) {
			    	startActivity(new Intent(ctx, PlayerActivity.class));
			    }
			});
			LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1);
			LinearLayout ll = (LinearLayout) rootView
					.findViewById(R.id.llForButton);
			myButton.setLayoutParams(params);
			ll.addView(myButton, params);
		}
		return rootView;
	}

	public void onRowClick(String parashaName) {
		// Use the Builder class for convenient dialog construction
		final boolean network = util.isNetworkAvailable(ctx);
		AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
		builder.setMessage("'" + parashaName + "'" + " - " + getResources().getString(R.string.action_to_made))
				.setPositiveButton(
						getResources().getString(R.string.listening),
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								if (network) {
									Toast.makeText(
											ctx,
											getResources().getString(
													R.string.begin_playing),
											Toast.LENGTH_SHORT).show();
									Intent i = new Intent(ctx,
											PlayerActivity.class);
									if (util.isMyServiceRunning(ctx) && (boolean)((Activity) ctx).getIntent().getBooleanExtra("playing", false)){ 
										i.putExtra("launch", true);
									}
									i.putExtra("PATH", path);
									i.putExtra("PARASHA", item);
									i.putExtra("POSITION", pstn);
									startActivity(i);
								} else {
									Toast.makeText(
											ctx,
											(String) getResources().getString(
													R.string.no_internet),
											Toast.LENGTH_LONG).show();
								}

							}
						})
				.setNegativeButton(
						getResources().getString(R.string.downloading),
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								ArrayList<String> existslist = new ArrayList<String>();
								existslist = util.readFromFile(ctx, true);
								boolean already = false;

								int size = existslist.size();
								if (size > 0) {
									for (int i = 0; i < size; i++) {
										if (Integer.valueOf(existslist.get(i)) == pstn)
											already = true;
									}
								}
								if (!already) {
									long memory = (ctx.getFilesDir()
											.getFreeSpace()) / 1048576;
									long memoryExternal = (ctx
											.getExternalFilesDir(null)
											.getFreeSpace()) / 1048576;
									if (network
											&& (memory > 300 || memoryExternal > 300)) {

										((VedibartaActivity) ctx).updateDownload(
												item, pstn, memory);
									} else {
										if (!network)
											Toast.makeText(
													ctx,
													getResources()
															.getString(
																	R.string.no_internet),
													Toast.LENGTH_LONG).show();
										else
											Toast.makeText(
													ctx,
													getResources()
															.getString(
																	R.string.full_memory),
													Toast.LENGTH_LONG).show();
									}

								} else
									Toast.makeText(ctx,
											R.string.alreadyDownloaded,
											Toast.LENGTH_LONG).show();
							}
						});
		// Create the AlertDialog object and return it
		mdialog = builder.create();
		mdialog.show();

	}

	class CustomArray extends ArrayAdapter<String> {

		TextView myText;
		ImageButton play;
		ImageButton download;

		public CustomArray(Context context, int resource1, int resource2,
				String[] list) {
			super(context, resource1, resource2, list);

		}

		@Override
		public View getView(final int position, View convertView,
				ViewGroup parent) {

			View row = convertView;
			row = super.getView(position, convertView, parent);
			row.setOnClickListener((new OnClickListener() {
				@Override
				public void onClick(View v) {
					pstn = position;
					item = data.getParashaHeb(position);
					path = data.getPath(position, 0)[1];
					onRowClick(item);
				}
			}));

			myText = (TextView) row.findViewById(R.id.text1);
			play = (ImageButton) row.findViewById(R.id.button1);
			download = (ImageButton) row.findViewById(R.id.button2);

			play.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					boolean network = util.isNetworkAvailable(ctx);
					if (network) {
						Toast.makeText(
								ctx,
								getResources()
										.getString(R.string.begin_playing),
								Toast.LENGTH_SHORT).show();
						pstn = position;
						item = data.getParashaHeb(position);
						path = data.getPath(position, 0)[1];
						Intent i = new Intent(ctx, PlayerActivity.class);
						if (util.isMyServiceRunning(ctx) && (boolean)((Activity) ctx).getIntent().getBooleanExtra("playing", false)){ 
							i.putExtra("launch", true);
						}
						i.putExtra("PATH", path);
						i.putExtra("PARASHA", item);
						i.putExtra("POSITION", pstn);
						startActivity(i);
					} else {
						Toast.makeText(ctx,
								getResources().getString(R.string.no_internet),
								Toast.LENGTH_LONG).show();
					}
				}
			});

			download.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					boolean network = util.isNetworkAvailable(ctx);
					boolean already = false;
					pstn = position;
					item = data.getParashaHeb(position);
					ArrayList<String> existslist = new ArrayList<String>();
					existslist = util.readFromFile(ctx, true);
					int size = existslist.size();
					if (size > 0) {
						for (int i = 0; i < size; i++) {
							if (Integer.valueOf(existslist.get(i)) == position)
								already = true;
						}
					}
					if (!already) {
						long memory = (ctx.getFilesDir().getFreeSpace()) / 1048576;
						long memoryExternal = (ctx.getExternalFilesDir(null)
								.getFreeSpace()) / 1048576;
						if (network && (memory > 300 || memoryExternal > 300)) {
							((VedibartaActivity) ctx).updateDownload(item, pstn,
									memory);
						} else {
							if (!network)
								Toast.makeText(
										ctx,
										getResources().getString(
												R.string.no_internet),
										Toast.LENGTH_LONG).show();
							else
								Toast.makeText(
										ctx,
										getResources().getString(
												R.string.full_memory),
										Toast.LENGTH_LONG).show();

						}
					} else
						Toast.makeText(ctx, R.string.alreadyDownloaded,
								Toast.LENGTH_LONG).show();
				}
			});
			return row;
		}

	}
}