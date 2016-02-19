package com.android.hairyd;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.loopj.android.http.RequestParams;

import twitter4j.AccountSettings;
import twitter4j.Twitter;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;


public class LoginActivity extends Activity {
	String TAG = Start.TAG;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.login_activity);
		Log.e(TAG, "--LoginActivity--");
		try {
			Twitter twitter = new TwitterFactory().getInstance();
			// https://dev.twitter.com/apps 에서 앱 선택하면 Consumer key와 Consumer secret을 확인할 수 있다.
			twitter.setOAuthConsumer(
					"Consumer key",
					"Consumer secret");
			RequestToken requestToken = twitter.getOAuthRequestToken();
			String authorizationURL = requestToken.getAuthorizationURL();

			// authorizationURL을 웹뷰에서 띄우고 유저가 로그인 하면 PIN 번호가 나온다.

			AccessToken accessToken = twitter.getOAuthAccessToken(requestToken, pinNumber);
			twitter.setOAuthAccessToken(accessToken);
			AccountSettings settings = twitter.getAccountSettings();
			// 계정이름
			String screenName = settings.getScreenName();
		}catch(Exception e){
			Log.e(TAG, "twit error");
		}

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
		if (id == R.id.action_settings) {
			return true;

		}
		return super.onOptionsItemSelected(item);
	}

	public void onLoginClicked(View v) {
		EditText idInput = (EditText) findViewById(R.id.IDInput);
		EditText pwInput = (EditText) findViewById(R.id.PWInput);

		final String idInputString = idInput.getText().toString();
		final String pwInputString = pwInput.getText().toString();

		if( idInputString.equals("")|idInputString == null |  pwInputString.equals("")| pwInputString == null ){
			Log.i(TAG, "LoginActivity; id = "+ idInputString + ", pw = " + pwInputString);
			Toast.makeText(getApplication(), "ID와 패스워드를 입력해주세요 ;"+Toast.LENGTH_LONG, Toast.LENGTH_LONG).show();
		}else {
			RequestParams params = new RequestParams();
			params.put("id",idInputString);
			params.put("pw",pwInputString);

			//filePath = Environment.getExternalStorageDirectory().getAbsolutePath() + VIDEOFOLDER;
			//File file = new File(file);
			//params.put("v", file);

//			AsyncHttpSet asyncHttpSet = new AsyncHttpSet(true);
//			asyncHttpSet.get("logintest.php", params, new AsyncHttpResponseHandler() {
//				@Override
//				public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
//					Log.i(TAG, "LoginActivity; success");
//
//					SharedPreferences sharedPreferences = getSharedPreferences("test", MODE_PRIVATE);
//					SharedPreferences.Editor editor = sharedPreferences.edit();
//					editor.putString("test",idInputString+"/"+pwInputString);
//					editor.commit();
//
//				}
//
//				@Override
//				public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
//					Log.i("hairyd", "LoginActivity; asyncHttp failed");
//				}
//			});
/* 쿠키 저장
			PersistentCookieStore persistentCookieStore = new PersistentCookieStore(getApplicationContext());
			AsyncHttpClient asyncHttpClient = new AsyncHttpClient();
			asyncHttpClient.setCookieStore(persistentCookieStore);

			Consumer Key (API Key)	y8IChpIaeJpprBRwc7NHSA6ad
Consumer Secret (API Secret)	0FSXyFblozOHZJAfnbYUoZTmviOssqOkc366QPLsqXfijF98J5
Access Level	Read and write (modify app permissions)
Owner	downqu
Owner ID	2471321096
*/
			Intent m_intent = new Intent(getApplicationContext(), MainMenu.class);
			startActivity(m_intent);
			finish();
		}

		return ;
	}

	public void onEnrollButtonClicked(View v) {
		Intent m_intent = new Intent(getApplicationContext(),EnrollActivity.class);
		m_intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);   // 이거 안해주면 안됨
		getApplicationContext().startActivity(m_intent);
		//startActivity(m_intent);
		return ;
	}

	public void onLookAroundButtonClicked(View v) {
		Intent m_intent = new Intent(getApplicationContext(),LookAroundActivity.class);
		startActivity(m_intent);
		return ;
	}

}
