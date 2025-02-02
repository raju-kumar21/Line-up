package project.tronku.line_up.login;


import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;
import project.tronku.line_up.API;
import project.tronku.line_up.InstructionsActivity;
import project.tronku.line_up.LineUpApplication;
import project.tronku.line_up.NetworkReceiver;
import project.tronku.line_up.PokeballActivity;
import project.tronku.line_up.QRCodeActivity;
import project.tronku.line_up.R;

import androidx.fragment.app.Fragment;

import static android.app.Activity.RESULT_OK;
import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;

public class LoginFragment extends Fragment implements project.tronku.line_up.login.OnLoginListener {
    private static final String TAG = "LoginFragment";
    private static final int REQUEST_CODE_INTRO = 1;
    private boolean firstTime;
    private EditText zealIdEditText, passwordEditText;
    private String zealid, password;
    private View inflate;
    private View layer;
    private ProgressBar loader;
    private SharedPreferences pref;
    private NetworkReceiver receiver;
    private ImageView visible,invisible;

    public LoginFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        inflate = inflater.inflate(R.layout.fragment_login, container, false);

        firstTime = PreferenceManager.getDefaultSharedPreferences(getActivity())
        .getBoolean("intro", true);

        zealIdEditText = inflate.findViewById(R.id.zeal_id_login);
        passwordEditText = inflate.findViewById(R.id.password_login);
        layer = inflate.findViewById(R.id.login_layer);
        loader = inflate.findViewById(R.id.login_loader);
        visible = inflate.findViewById(R.id.visible);
        invisible = inflate.findViewById(R.id.invisible);
        receiver = new NetworkReceiver();
        pref = PreferenceManager.getDefaultSharedPreferences(getActivity());
        visible.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                passwordEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                passwordEditText.setTypeface(ResourcesCompat.getFont(getContext(), R.font.roboto_thin));
                visible.setVisibility(INVISIBLE);
                invisible.setVisibility(VISIBLE);
            }
        });

        invisible.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                passwordEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                passwordEditText.setTypeface(ResourcesCompat.getFont(getContext(), R.font.roboto_thin));
                invisible.setVisibility(INVISIBLE);
                visible.setVisibility(VISIBLE);
            }
        });
        return inflate;
    }

    @Override
    public void login() {
        hideKeyboard();
        if (receiver.isConnected()) {
            zealid = zealIdEditText.getText().toString();
            password = passwordEditText.getText().toString();

            if (zealid.isEmpty() || password.isEmpty()) {
                Snackbar snackbar = Snackbar.make(inflate, "Enter details.", Snackbar.LENGTH_SHORT);
                View snackbarView = snackbar.getView();
                snackbarView.setBackgroundColor(getResources().getColor(R.color.qr));
                snackbar.show();
            } else{
                layer.setVisibility(View.VISIBLE);
                loader.setVisibility(View.VISIBLE);
                zealIdEditText.setEnabled(false);
                passwordEditText.setEnabled(false);
                login(zealid, password);
            }
        }
        else {
            Toast.makeText(getContext(), "No internet!", Toast.LENGTH_SHORT).show();
        }
    }


    private void hideKeyboard() {
        View view = getActivity().getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private void login(final String username, final String password){
        StringRequest loginReq = new StringRequest(Request.Method.POST,API.BASE + API.LOGIN, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.e(TAG, " Login Activity onResponse: " + response);
                Map<String, String> responseMap = new Gson().fromJson(response, new TypeToken<Map<String, String>>() {}.getType());
                String token = responseMap.get("access_token");
                String code = responseMap.get("uniqueCode");
                Log.e(TAG, "onResponse: " + token);
                SharedPreferences.Editor editor = pref.edit();
                editor.putString("token", token);
                editor.putString("zealid", username);
                editor.putString("uniqueCode", code);
                editor.apply();
                startActivities();
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(TAG, " Login Activity onErrorResponse: " + error.toString());
                if (error.networkResponse!=null) {
                    String json = new String(error.networkResponse.data);
                    try {
                        JSONObject jsonError = new JSONObject(json);
                        if (jsonError.has("error")) {
                            String errorString = "Invalid data! Try again.";
                            final Dialog dialog = new Dialog(getActivity());
                            dialog.setContentView(R.layout.dialog_layout);
                            ImageView close = dialog.findViewById(R.id.close);
                            close.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    dialog.dismiss();
                                }
                            });

                            dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                                @Override
                                public void onDismiss(DialogInterface dialogInterface) {
                                    zealIdEditText.setText("");
                                    passwordEditText.setText("");
                                    zealIdEditText.setEnabled(true);
                                    passwordEditText.setEnabled(true);
                                    layer.setVisibility(View.INVISIBLE);
                                    loader.setVisibility(View.INVISIBLE);
                                }
                            });

                            TextView errorView = dialog.findViewById(R.id.errorText);
                            errorView.setText(errorString);
                            dialog.show();

                            layer.setVisibility(View.INVISIBLE);
                            loader.setVisibility(View.INVISIBLE);
                            zealIdEditText.setEnabled(true);
                            passwordEditText.setEnabled(true);

                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                else {
                    Toast.makeText(getActivity(), "Network issue! Try again.", Toast.LENGTH_SHORT).show();
                    layer.setVisibility(View.INVISIBLE);
                    loader.setVisibility(View.INVISIBLE);
                    zealIdEditText.setEnabled(true);
                    passwordEditText.setEnabled(true);
                }
            }
        }){
            @Override
            protected Map<String,String> getParams(){
                Map<String,String> params = new HashMap<>();
                params.put("username",username);
                params.put("password",password);
                params.put("grant_type", "password");
                return params;
            }

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                String credentials = API.USERNAME + ":" + API.PASSWORD;
                String encoding = Base64.encodeToString(credentials.getBytes(), Base64.NO_WRAP);
                Map<String,String> params = new HashMap<>();
                params.put("Content-Type","application/x-www-form-urlencoded");
                params.put("Authorization", "Basic " + encoding);

                return params;
            }
        };
        LineUpApplication.getInstance().addToRequestQueue(loginReq);
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_INTRO) {
            if (resultCode == RESULT_OK) {
                PreferenceManager.getDefaultSharedPreferences(getActivity()).edit().putBoolean("intro", false).apply();
                Intent qrcode = new Intent(getActivity(), PokeballActivity.class);
                startActivity(qrcode);
            } else {
                //Toast
            }
        }
    }

    private void startActivities() {
        if (firstTime) {
            Intent intro = new Intent(getActivity(), InstructionsActivity.class);
            startActivityForResult(intro, REQUEST_CODE_INTRO);
        }
        else {
            Intent qrcode = new Intent(getActivity(), QRCodeActivity.class);
            getActivity().finishAffinity();
            startActivity(qrcode);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        getActivity().registerReceiver(receiver, filter);
    }

    @Override
    public void onStop() {
        super.onStop();
        getActivity().unregisterReceiver(receiver);
    }

}
