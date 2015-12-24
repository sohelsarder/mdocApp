package com.mpower.daktar.android.activities;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.mpower.daktar.android.listeners.AccountResponseListener;
import com.mpower.daktar.android.listeners.AccountStatusChangedListener;
import com.mpower.daktar.android.listeners.AccountinfoRetrieveListener;
import com.mpower.daktar.android.tasks.AccountInfoRetrieveTask;
import com.mpower.daktar.android.tasks.AccountRechargeTask;
import com.mpower.daktar.android.tasks.AccountStatusTask;
import com.mpower.daktar.android.R;

public class AccountViewActivity extends Activity implements
		AccountResponseListener, AccountinfoRetrieveListener,
		AccountStatusChangedListener {

	private class ViewHolder {
		EditText trxId;
		Button submit;
	}

	private String rmpId;
	private String rmpAcc;
	private String newApps;
	private String folloApps;

	private AlertDialog waiting;
	
	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Intent caller = getIntent();
		if (caller != null) {
			rmpId = caller.getStringExtra("rmpId");
			rmpAcc = caller.getStringExtra("rmpAcc");
		}
		setContentView(R.layout.activity_account_view);

		final ViewHolder vh = new ViewHolder();
		vh.trxId = (EditText) findViewById(R.id.trxid);
		vh.submit = (Button) findViewById(R.id.submit);
		vh.submit.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				
				AlertDialog.Builder db = new AlertDialog.Builder(AccountViewActivity.this);
				db.setTitle("কিছুক্ষণ অপেখখা করুন");
				db.setMessage("আপ্নার তথ্য পাঠানো হচ্ছে...");
				waiting= db.create();
				waiting.show();
				
				String trxid = vh.trxId.getText().toString();
				AccountRechargeTask acc = new AccountRechargeTask(
						AccountViewActivity.this);
				acc.execute(rmpId, trxid);
			}
		});

		AccountInfoRetrieveTask accTask = new AccountInfoRetrieveTask(this);
		accTask.execute(rmpId);
		AccountStatusTask accStat = new AccountStatusTask(this);
		accStat.execute(rmpId);
	}

	@Override
	public void onAccountResponded(String result) {
		System.err.println(""+result);
		try {
			if (waiting != null)
				waiting.dismiss();
			if (result == null) {
				AlertDialog.Builder db = new AlertDialog.Builder(this);
				db.setTitle("অ্যাকাউন্ট রিচার্জ এর ফলাফল।");
				db.setMessage("সার্ভার সমস্যা করছে কিছুক্ষণ পর আবার চেষ্টা করুন।");
				db.setNeutralButton("Ok",
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								dialog.dismiss();
							}
						});
				db.create().show();
				return;
			}
			JSONObject response = new JSONObject(result);
			rmpAcc = (String) response.get("currBalance");
			AlertDialog.Builder db = new AlertDialog.Builder(this);
			db.setTitle("অ্যাকাউন্ট রিচার্জ এর ফলাফল।");
			db.setMessage("" + (String) response.get("reason"));
			db.setNeutralButton("Ok", new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}
			});
			db.create().show();
		} catch (JSONException jse) {
		}
		AccountStatusTask accTask = new AccountStatusTask(this);
		accTask.execute(rmpId);
	}

	@Override
	public void onAccountInfoRetrieved(String result) {
		try {
			JSONObject response = new JSONObject(result);
			// {"qtyNew":1,"qtyFolluUp":1,"id":["1","2","3"],
			// "costName":["New Appointment","Follow Up","Free"],
			// "cost":["200","150","0"]}
			String newQuantity = response.getString("qtyNew");
			this.newApps = ""+(Integer.parseInt(newQuantity)-1);
			String followupQuantity = response.getString("qtyFolluUp");
			this.folloApps = ""+(Integer.parseInt(followupQuantity)-1);
			JSONArray id = response.getJSONArray("id");
			JSONArray costName = response.getJSONArray("costName");
			JSONArray cost = response.getJSONArray("cost");
			TextView accTitle = (TextView) findViewById(R.id.acc_text);
			accTitle.setText(Html.fromHtml("<h1>আপনার অ্যাকাউন্ট এর অবস্থা<h1/>"));
			TextView accBody = (TextView) findViewById(R.id.acc_body);
			accBody.setText(Html.fromHtml("<h2>আপনার অ্যাকাউন্ট এ " + rmpAcc
					+ " টাকা আছে |</h2><br/><h3>আপনি " + newApps
					+ "টা নিউ অ্যাপইন্টমেন্ট নিয়েছেন</h3><h3> এবং " + folloApps
					+ "টা ফলোআপ নিয়েছেন।</h3>")
					+ "");
			accBody.setTextSize(32);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void onStatusChanged(String response) {
		System.err.println(""+response);
		try {
			
			JSONObject result = new JSONObject(response);
			String balance = (String) result.get("currBal");
			String limit = (String) result.get("creditLimit");
			if (balance != null) {
				rmpAcc = balance;
				TextView accBody = (TextView) findViewById(R.id.acc_body);
				accBody.setText(Html.fromHtml("<h2>আপনার অ্যাকাউন্ট এ " + rmpAcc
						+ " টাকা আছে |</h2><br/><h3>আপনি " + newApps
						+ "টা নিউ অ্যাপইন্টমেন্ট নিয়েছেন</h3><h3> এবং "
						+ folloApps + "টা ফলোআপ নিয়েছেন।</h3>")
						+ "");
				accBody.setTextSize(32);
			}
		} catch (JSONException jse) {
		}
	}
}