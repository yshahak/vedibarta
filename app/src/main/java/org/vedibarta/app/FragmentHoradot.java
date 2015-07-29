package org.vedibarta.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
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

import com.splunk.mint.Mint;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class FragmentHoradot extends ListFragment {

	private AlertDialog mdialog;
	int pstn;
	long freeSD = 0;
	long freeInternal;
	private ParashotData data;
	private ArrayList<String> existParashotlist;
	private ArrayList<String> numbersList;
	private ArrayList<String> dataList;
	private ArrayList<String> tracksList;
	private ArrayList<String> currentPositionList;
	private ArrayList<String> pathesList;

	private ArrayList<Integer> myPosition;
	private TextView mTextView;
	
	Utilities util = new Utilities();
	File SD;
	String path;
	String item = null;
	Context ctx;
	Fragment frag;
	private CustomArray adapter;
	View rootView;
	

	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		rootView = inflater.inflate(R.layout.list, container, false);
		mTextView = (TextView) rootView.findViewById(R.id.memory);
		ctx = getActivity();
		frag = this.getParentFragment();
		SD = ctx.getExternalFilesDir(null);
		freeInternal = (ctx.getFilesDir().getFreeSpace()) / 1048576;
		if (SD != null)
			freeSD = SD.getFreeSpace() / 1048576;
		mTextView.setText(getResources().getString(R.string.spaceSD)
				+ Long.toString(freeSD) + "MB" + '\n'
				+ getResources().getString(R.string.space_internal)
				+ Long.toString(freeInternal) + "MB");
		data = new ParashotData();
		myPosition = new ArrayList<Integer>();
		existParashotlist = new ArrayList<String>();
		numbersList = new ArrayList<String>();
		dataList = new ArrayList<String>();
		pathesList =  new ArrayList<String>();
		tracksList = new ArrayList<String>();
		currentPositionList = new ArrayList<String>();

		getFiles();
		adapter = new CustomArray(getActivity(), R.layout.row2, R.id.text1,
				existParashotlist);
		setListAdapter(adapter);
		if (util.isMyServiceRunning(ctx) && (boolean)((Activity) ctx).getIntent().getBooleanExtra("playing", false)) {
			Button myButton = new Button(ctx);
			myButton.setText(R.string.backToPlayer);
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

	public void onRowClick(final int position) {
		// Use the Builder class for convenient dialog construction
		AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
		builder.setMessage(getResources().getString(R.string.action_to_made))
				.setPositiveButton(
						getResources().getString(R.string.listening),
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								Toast.makeText(
										ctx,
										getResources().getString(
												R.string.begin_playing),
										Toast.LENGTH_SHORT).show();
								Intent i = new Intent(ctx, PlayerActivity.class);
								if (util.isMyServiceRunning(ctx) && (boolean)((Activity) ctx).getIntent().getBooleanExtra("playing", false)){ 
									i.putExtra("launch", true);
								}
								i.putExtra("FILE_EXIST", true);
								i.putExtra("INDEX", position);
								i.putExtra("PATH", path);
								i.putExtra("PARASHA", item);
								i.putExtra("POSITION", pstn);
								i.putExtra("COUNT",  Integer.valueOf(tracksList.get(position)));
								i.putExtra("CURRENT", Long.valueOf(currentPositionList.get(position)));
								startActivity(i);

							}
						})
				.setNegativeButton(getResources().getString(R.string.delete),
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								File mydirIn;
								File mydirEx;
								mydirEx = new File((String) (ctx
										.getExternalFilesDir(null)
										+ File.separator + "AudioFiles"),
										(String) (Integer.toString(myPosition
												.get(position))));
								mydirIn = new File((String) (ctx.getFilesDir()
										+ File.separator + "AudioFiles"),
										(String) (Integer.toString(myPosition
												.get(position))));
								if (mydirIn.isDirectory()) {
									String[] children = mydirIn.list();
									for (int i = 0; i < children.length; i++) {
										new File(mydirIn, children[i]).delete();
									}
								}
								if (mydirEx.isDirectory()) {
									String[] children = mydirEx.list();
									for (int i = 0; i < children.length; i++) {
										new File(mydirEx, children[i]).delete();
									}
								}
								mydirIn.delete();
								mydirEx.delete();
								try {
									util.updateLine(ctx, position, false, null);
								} catch (IOException e) {
                                    Mint.logException(e);
								}
								getFiles();

								adapter.notifyDataSetChanged();
								

							}
						});
		// Create the AlertDialog object and return it
		mdialog = builder.create();
		mdialog.show();

	}

	private void getFiles() {
		int size = util.readFromFile(ctx, true).size();
		existParashotlist.clear();
		if (size > 0) {
			numbersList = util.readFromFile(ctx, true);
			dataList = util.readFromFile(ctx, false);
			for (int i = 0; i < size; i++) {
				existParashotlist.add(i, data.getParashaHeb(Integer.valueOf(numbersList.get(i))));
				String[] separated = dataList.get(i).split(";");
				pathesList.add(i, separated[0]);
				tracksList.add(i, separated[1]);
				currentPositionList.add(i, separated[2]);
			}
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		adapter.notifyDataSetChanged();
	}

	

	class CustomArray extends ArrayAdapter<String> {

		TextView myText;
		ImageButton play;
		ImageButton delete;

		public CustomArray(Context context, int resource1, int resource2,
				ArrayList<String> list) {
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
					pstn = Integer.valueOf(numbersList.get(position));
					path = pathesList.get(position);
					item = existParashotlist.get(position);
					onRowClick((int) position);
				}
			}));

			myText = (TextView) row.findViewById(R.id.text1);
			play = (ImageButton) row.findViewById(R.id.button1);
			delete = (ImageButton) row.findViewById(R.id.button2);

			play.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					Toast.makeText(ctx,
							getResources().getString(R.string.begin_playing),
							Toast.LENGTH_SHORT).show();
					path = pathesList.get(position);
					pstn = Integer.valueOf(numbersList.get(position));
					item = existParashotlist.get(position);
					Intent i = new Intent(ctx, PlayerActivity.class);
					if (util.isMyServiceRunning(ctx) && (boolean)((Activity) ctx).getIntent().getBooleanExtra("playing", false)){ 
						i.putExtra("launch", true);
					}
					i.putExtra("FILE_EXIST", true);
					i.putExtra("PATH", path);
					i.putExtra("INDEX", position);
					i.putExtra("PARASHA", item);
					i.putExtra("POSITION", pstn);
					i.putExtra("COUNT", Integer.valueOf(tracksList.get(position)));
					i.putExtra("CURRENT", Long.valueOf(currentPositionList.get(position)));
					startActivity(i);
				}
			});

			delete.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					//path = patheslist.get(position);
					//item = existParashotlist.get(position);
					pstn = Integer.valueOf(numbersList.get(position));
					File mydirIn;
					File mydirEx;
					mydirEx = new File((String) (ctx.getExternalFilesDir(null)
							+ File.separator + "AudioFiles"), (String) (Integer
							.toString(pstn)));
					mydirIn = new File((String) (ctx.getFilesDir()
							+ File.separator + "AudioFiles"), (String) (Integer
							.toString(pstn)));

					if (mydirIn.isDirectory()) {
						String[] children = mydirIn.list();
						for (int i = 0; i < children.length; i++) {
							new File(mydirIn, children[i]).delete();
						}
					}
					if (mydirEx.isDirectory()) {
						String[] children = mydirEx.list();
						for (int i = 0; i < children.length; i++) {
							new File(mydirEx, children[i]).delete();
						}
					}
					mydirIn.delete();
					mydirEx.delete();
					try {
						util.updateLine(ctx, position, false, null);
					} catch (IOException e) {
                        Mint.logException(e);
					}
					getFiles();

					adapter.notifyDataSetChanged();
					

					
				}
			});
			return row;
		}
	}
}