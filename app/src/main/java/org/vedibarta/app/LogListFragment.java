package org.vedibarta.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

public class LogListFragment extends Fragment {

	private TextView activityLog;
	private TextView messageLog;
	private Button buttonLog;
	Context ctx;
	private View mainView;
	private String logToMail = "";
	boolean first = false;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		Context ctx = getActivity();
		mainView = (TableLayout) inflater.inflate(R.layout.log, container,
				false);
		buttonLog = (Button) mainView.findViewById(R.id.button1);
		buttonLog.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				exportLog();
			}
		});
		ctx = getActivity();
		int index = ((Activity) ctx).getIntent().getIntExtra("index", 0);
		if (VedibartaActivity.firstEntry) {
			for (int j = 0; j < index; j++)
				VedibartaActivity.logList.add(((Activity) ctx).getIntent()
						.getStringExtra("log" + j));
			VedibartaActivity.firstEntry = false;
		}
		for (int i = 0; i < VedibartaActivity.logList.size(); i = i + 2) {
			TableRow row = (TableRow) inflater
					.inflate(R.layout.table_row, null);
			activityLog = (TextView) row.findViewById(R.id.activity);
			messageLog = (TextView) row.findViewById(R.id.message);
			logToMail = logToMail + VedibartaActivity.logList.get(i) + ":   "
					+ VedibartaActivity.logList.get(i + 1) + "\r\n\r\n";
			activityLog.setText(VedibartaActivity.logList.get(i));
			messageLog.setText(VedibartaActivity.logList.get(i + 1));
			((TableLayout) mainView).addView(row);
		}

		return mainView;

	}

	protected void exportLog() {
		Intent i = new Intent(Intent.ACTION_SEND);
		i.setType("message/rfc822");
		i.putExtra(Intent.EXTRA_EMAIL, new String[] { "yshahak@gmail.com" });
		i.putExtra(Intent.EXTRA_SUBJECT, "Log");
		i.putExtra(Intent.EXTRA_TEXT, logToMail);
		try {
			startActivity(Intent.createChooser(i, "Send mail..."));
		} catch (android.content.ActivityNotFoundException ex) {
			Toast.makeText(ctx, "There are no email clients installed.",
					Toast.LENGTH_SHORT).show();
		}
	}
	
}
