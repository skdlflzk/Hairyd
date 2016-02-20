package com.android.hairyd;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.loopj.android.http.RequestParams;

import java.util.List;

import twitter4j.AccountSettings;
import twitter4j.Paging;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;
import twitter4j.conf.ConfigurationBuilder;


public class LoginActivity extends Activity {
	String TAG = Start.TAG;

	private Twitter twitter;
	private AccessToken accessToken = null;
	private RequestToken requestToken = null;

	private Status status = null;
	private static final String CONSUMER_KEY = "y8IChpIaeJpprBRwc7NHSA6ad";
	private static final String CONSUMER_SECRET = "0FSXyFblozOHZJAfnbYUoZTmviOssqOkc366QPLsqXfijF98J";


	/** 생성. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.login_activity);

		Log.e(TAG, "--LoginActivity--");


		Button twitterButton = (Button) findViewById(R.id.twitterButton);
		twitterButton.setOnClickListener(new Button.OnClickListener(){
			@Override
			public void onClick(View view) {


				ConfigurationBuilder builder = new ConfigurationBuilder();
				builder.setOAuthConsumerKey(CONSUMER_KEY);
				builder.setOAuthConsumerSecret(CONSUMER_SECRET);
				twitter4j.conf.Configuration configuration = builder.build();
				twitter = new TwitterFactory(configuration).getInstance();

				new Thread(new Runnable()
				{
					@Override
					public void run()
					{
						try
						{
							requestToken = twitter.getOAuthRequestToken("sample://twitter");
							//성공시 requestTOken에 해당정보가 담긴다
							//그 후 requestToken을 반드시 세션에 담아주어야 함.

						}
						catch (TwitterException e)
						{
							e.printStackTrace();
						}

						Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(requestToken.getAuthorizationURL()));
						intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
						startActivity(intent);
					}
				}).start();
			}
		});
	}

	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		Log.e(TAG, "success");
//		Uri uri = intent.getData();
//		if (uri != null && CALLBACK_URL.getScheme().equals(uri.getScheme())) {
//			String oauth_verifier = uri.getQueryParameter("oauth_verifier");
//			try {
//				acToken = twitter.getOAuthAccessToken(rqToken, oauth_verifier);
//				saveData(S_CONSUMER_KEY, acToken.getToken());
//				saveData(S_CONSUMER_SECRET, acToken.getTokenSecret());
//			} catch (TwitterException e) {
//				Log.e("coolsharp", e.getMessage());
//			}
//		}
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
