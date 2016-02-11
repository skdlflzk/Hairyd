package com.android.hairyd;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

public class MainMenu extends Activity {


    static String TAG = Start.TAG;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_menu);
		Log.e(TAG, "--MainMenu--");
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu_start, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == com.android.hairyd.R.id.action_settings) {
			return true;

		}
		return super.onOptionsItemSelected(item);
	}

	public void onCameraButtonClicked(View v) {
		Intent m_intent = new Intent(getApplicationContext(),CameraActivity.class);
		startActivity(m_intent);
		return ;
	}

	public void onViewButtonClicked(View v) {
		//Intent m_intent = new Intent(getApplicationContext(),Start.class);
		//startActivity(m_intent);
		return ;
	}

}
