package com.android.hairyd;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

public class MainMenu extends ActionBarActivity implements OnClickListener{

	Button btn[] = new Button[3];
	ViewPager viewPager = null;
	Thread thread = null;
	Handler handler = null;
	int p=0;	//페이지번호
	int v=1;	//화면 전환 뱡향

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);


		//viewPager
		viewPager = (ViewPager)findViewById(R.id.viewPager);
		ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager());

		viewPager.setAdapter(adapter);

		btn[0] = (Button)findViewById(R.id.btn_a);
		btn[1] = (Button)findViewById(R.id.btn_b);
		btn[2] = (Button)findViewById(R.id.btn_c);

		for(int i=0;i<btn.length; i++){
			btn[i].setOnClickListener(this);
		}

		handler = new Handler() {


			public void handleMessage(android.os.Message msg) {

				btn[p].setBackgroundColor(Color.parseColor("#00000000"));

				if( v == 0){
					if( p == 0 ){
						p = 3;
					}else{
						p--;
					}
					viewPager.setCurrentItem(p);
				}else if ( v == 1 ){
					if( p == 2 ){
						p = 0;
					}else{
						p++;
					}
					viewPager.setCurrentItem(p);
				}else{
					//여기 왜옴
				}

				btn[p].setBackgroundColor(Color.parseColor("#ddaa5533"));
			}
		};

//		thread = new Thread(){
//			//run은 jvm이 쓰레드를 채택하면, 해당 쓰레드의 run메서드를 수행한다.
//			public void run() {
//				super.run();
//				while(true){
//					try {
//						Thread.sleep(4000);
//						handler.sendEmptyMessage(0);
//					} catch (InterruptedException e) {
//						// TODO Auto-generated catch block
//						e.printStackTrace();
//					}
//
//
//				}
//			}
//		};
//		thread.start();
//

	}

	@Override
	public void onClick(View v) {

		switch(v.getId()){
			case R.id.btn_a:
//				btn[p].setBackgroundColor(Color.parseColor("#00000000"));
//				p = 0;
//				v.setBackgroundColor(Color.parseColor("#00000000"));
				viewPager.setCurrentItem(0);
			//	Toast.makeText(this,"홈", Toast.LENGTH_SHORT).show();
				break;

			case R.id.btn_b:
//				btn[p].setBackgroundColor(Color.parseColor("#00000000"));
//				p = 1;
//				v.setBackgroundColor(Color.parseColor("#00000000"));
				viewPager.setCurrentItem(1);
			//	Toast.makeText(this,"머리하기", Toast.LENGTH_SHORT).show();
				break;

			case R.id.btn_c:
//				btn[p].setBackgroundColor(Color.parseColor("#00000000"));
//				p = 2;
//				v.setBackgroundColor(Color.parseColor("#00000000"));
				viewPager.setCurrentItem(2);
			//	Toast.makeText(this,"마이페이지", Toast.LENGTH_SHORT).show();
				break;

			default:
				break;

		}

	}
}

/*package com.android.hairyd;

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
*/