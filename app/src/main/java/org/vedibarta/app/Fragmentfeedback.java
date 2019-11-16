package org.vedibarta.app;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class Fragmentfeedback extends Fragment {

    private final String TAG = "Fragmentfeedback";
    EditText EditTextName;
    EditText EditTextEmail;
    EditText EditTextFeedbackBody;
    Button ButtonSendFeedback;
    Button LinkButton;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View mainView = inflater.inflate(R.layout.feedback2, container,
                false);
        EditTextName = mainView.findViewById(R.id.EditTextName);
        EditTextEmail = mainView.findViewById(R.id.EditTextEmail);
        EditTextFeedbackBody = mainView
                .findViewById(R.id.EditTextFeedbackBody);
        ButtonSendFeedback = mainView
                .findViewById(R.id.ButtonSendFeedback);
        LinkButton = mainView.findViewById(R.id.link_vedibarta);
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
                Uri uri = Uri.parse("https://vedibarta.org");
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
            }
        });

        return mainView;
    }


    public void sendFeedback(String name, String mail, String text) {
        final Activity ctx = getActivity();
        if (ctx == null) {
            return;
        }
        hideKeyboard(ctx);
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
                EditTextFeedbackBody.setText("");
                String content = "נשלח מתוך אפליקצית 'ודיברת' לאנדרואיד: " + text;
                Call<ResponseBody> response = RetrofitHelper.sendFeedback(name, mail, content);
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
            }
        } else
            Toast.makeText(ctx,
                    getResources().getString(R.string.comment_no_success),
                    Toast.LENGTH_SHORT).show();

    }

    public void hideKeyboard(Activity activity) {
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        //Find the currently focused view, so we can grab the correct window token from it.
        View view = activity.getCurrentFocus();
        //If no view currently has focus, create a new one, just so we can grab a window token from it
        if (view == null) {
            view = new View(activity);
        }
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
}
