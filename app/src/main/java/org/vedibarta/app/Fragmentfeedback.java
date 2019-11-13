package org.vedibarta.app;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

//import com.splunk.mint.Mint;

//import org.apache.http.NameValuePair;
//import org.apache.http.client.ClientProtocolException;
//import org.apache.http.client.HttpClient;
//import org.apache.http.client.entity.UrlEncodedFormEntity;
//import org.apache.http.client.methods.HttpPost;
//import org.apache.http.impl.client.DefaultHttpClient;
//import org.apache.http.message.BasicNameValuePair;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class Fragmentfeedback extends Fragment {

	private final String TAG = "Fragmentfeedback";
	Context ctx;
//	HttpClient client;
//	HttpPost post;
	EditText EditTextName;
	EditText EditTextEmail;
	EditText EditTextFeedbackBody;
	Button ButtonSendFeedback;
	Button LinkButton;


	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View mainView =  inflater.inflate(R.layout.feedback2, container,
				false);
		EditTextName = (EditText) mainView.findViewById(R.id.EditTextName);
		EditTextEmail = (EditText) mainView.findViewById(R.id.EditTextEmail);
		EditTextFeedbackBody = (EditText) mainView
				.findViewById(R.id.EditTextFeedbackBody);
		ButtonSendFeedback = (Button) mainView
				.findViewById(R.id.ButtonSendFeedback);
		LinkButton = (Button) mainView.findViewById(R.id.link_vedibarta);
		ButtonSendFeedback.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				String name = EditTextName.getText().toString();
				String email = EditTextEmail.getText().toString();
				String feedback = EditTextFeedbackBody.getText().toString();
				sendFeedback(name, email, feedback);

			}
		});
		LinkButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Uri uri = Uri.parse("https://www.vedibarta.org");
				Intent intent = new Intent(Intent.ACTION_VIEW, uri);
				startActivity(intent);
			}
		});
		ctx =  getActivity();

		return mainView;
	}

//	private class AddComment extends AsyncTask<String, Void, Void> {
//		@Override
//		protected Void doInBackground(String... params) {
//			try {
//				client = new DefaultHttpClient();
//				post = new HttpPost(
//						"http://www.vedibarta.org/guestbook/save.asp");
//				post.setHeader("Proxy-Connection", "keep-alive");
//				post.setHeader("Cache-Control", "max-age=0");
//				post.setHeader("Accept-Encoding", "gzip,deflate");
//				post.setHeader("Content-Type",
//						"application/x-www-form-urlencoded;charset=UTF-8");
//				List<NameValuePair> pairs = new ArrayList<NameValuePair>();
//				pairs.add(new BasicNameValuePair("NAME", params[0]));
//				pairs.add(new BasicNameValuePair("EMAIL", params[1]));
//				pairs.add(new BasicNameValuePair("MESSAGE",
//                        "נשלח מתןך אפליקצית 'ודיברת' לאנדרואיד:    "
//								+ params[2]));
//				post.setEntity(new UrlEncodedFormEntity(pairs, "UTF-8"));
//				client.execute(post);
//			} catch (UnsupportedEncodingException e) {
//                Mint.logException(e);
//			} catch (ClientProtocolException e) {
//                Mint.logException(e);
//			} catch (IOException e) {
//                Mint.logException(e);
//			}
//			return null;
//		}
//	}


	public void sendFeedback(String name, String mail, String text) {
		if (Utilities.isNetworkAvailable(ctx)) {
			if (name.equals("") || mail.equals("") || text.equals("")) {
				Toast.makeText(ctx,
						getResources().getString(R.string.missing_parameters),
						Toast.LENGTH_SHORT).show();
			} else if (!Utilities.isValidEmail(mail))
				Toast.makeText(ctx,
						getResources().getString(R.string.email_not_valid),
						Toast.LENGTH_SHORT).show();
			else {
//				new AddComment().execute(name, mail, text);
				Call<ResponseBody> response = RetrofitHelper.sendFeedback(name, mail, text);
				response.enqueue(new Callback<ResponseBody>() {
					@Override
					public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
						Log.d(TAG, "successfull:" + response.message() + "\t" + response.body());
						Toast.makeText(ctx,
								getResources().getString(R.string.comment_success),
								Toast.LENGTH_SHORT).show();
					}

					@Override
					public void onFailure(Call<ResponseBody> call, Throwable t) {
						Log.e(TAG, "error:" + t.getMessage());

						Toast.makeText(ctx,
								getResources().getString(R.string.comment_error),
								Toast.LENGTH_SHORT).show();
					}
				});
				EditTextName.setText("");
				EditTextEmail.setText("");
				EditTextFeedbackBody.setText("");

			}
		} else
			Toast.makeText(ctx,
					getResources().getString(R.string.comment_no_success),
					Toast.LENGTH_SHORT).show();

	}
}
