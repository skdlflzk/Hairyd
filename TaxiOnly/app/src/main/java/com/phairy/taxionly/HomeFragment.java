
package com.phairy.taxionly;

import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;


public class HomeFragment extends Fragment implements LocationListener {  //} implements View.OnClickListener{

    private String TAG = Start.TAG;

    private LocationManager locationManager;
    private Location mlocation;
    private TextView latitudeTextView;
    private TextView longitudeTextView;
    private TextView logText;

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.e(TAG, "--HomeFragment--");

        View view = inflater.inflate(R.layout.home_fragment, container, false);

        latitudeTextView = (TextView) view.findViewById(R.id.latitudeTextview);
        longitudeTextView = (TextView) view.findViewById(R.id.longitudeTextview);
        logText = (TextView) view.findViewById(R.id.logText);

        locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);


        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.NO_REQUIREMENT); //정확도 설정
        criteria.setPowerRequirement(Criteria.NO_REQUIREMENT); //전력 소모량

        String bestProvider = locationManager.getBestProvider(criteria, true);

        locationManager.requestLocationUpdates(bestProvider, 1000, 0, this);

    //    location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);


        Button takeButton = (Button) view.findViewById(R.id.TakeButton);

        takeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //     locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
                try {
                    if ((int) mlocation.getLatitude() != 0) {
                        Toast.makeText(getActivity(), "현 위도 : " + mlocation.getLatitude(), Toast.LENGTH_SHORT).show();
                        openMap(mlocation.getLatitude(), mlocation.getLongitude());
                    } else {
                        Toast.makeText(getActivity(), "현 위도 : " + mlocation.getLatitude(), Toast.LENGTH_SHORT).show();

                    }
                } catch (Exception e) {
                    Toast.makeText(getActivity(), "정보가 아직 수신되지 않음", Toast.LENGTH_SHORT).show();
                }
            }
        });
        return view;
    }
    private void openMap(double latitude,double longitude){

        Uri geoURI = Uri.parse(String.format("geo:%f,%f", latitude, longitude));
        Intent intent = new Intent(Intent.ACTION_VIEW, geoURI);
        startActivity(intent);

    }
    @Override
    public void onLocationChanged(Location location) {

        latitudeTextView.setText(String.format("%f", location.getLatitude()));
        longitudeTextView.setText(String.format("%f", location.getLongitude()));
        logText.setText(logText.getText().toString()+"\n("+location.getLatitude()+","+ location.getLongitude()+")");

         mlocation = location;
        Log.e(TAG, "HomeFragment:("+  location.getLatitude() + "), (" + location.getLongitude() + ")");
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {
        switch(i){
            case LocationProvider.OUT_OF_SERVICE :
                Toast.makeText(getActivity(),"서비스 지역이 아닙니다..", Toast.LENGTH_SHORT ).show();
                break;
            case LocationProvider.TEMPORARILY_UNAVAILABLE :
                Toast.makeText(getActivity(),"일시적인 장애로 인하여 위치정보를 수신할 수 없습니다.", Toast.LENGTH_SHORT ).show();
                break;
            case LocationProvider.AVAILABLE :
                Toast.makeText(getActivity(),"GPS 수신 가능 상태", Toast.LENGTH_SHORT ).show();
                break;
        }
    }

    @Override
    public void onProviderEnabled(String s) {
        Toast.makeText(getActivity(),"위치정보 공급 가능.", Toast.LENGTH_SHORT ).show();
    }

    @Override
    public void onProviderDisabled(String s) {
        Toast.makeText(getActivity(),"위치정보 공급받을수 없음.", Toast.LENGTH_SHORT ).show();
    }

    //    @Override
//    public void onClick(View view) {
//        switch( view.getId() ){
//            case R.id.LoginButton :
//                Intent m_intent = new Intent(getActivity().getApplicationContext(),LoginActivity.class);
//                startActivity(m_intent);
//                break;
//        }
//    }

//    public void onLoginButtonClicked(View view){
//        Intent m_intent = new Intent(getActivity().getApplicationContext(),LookAroundActivity.class);
//        startActivity(m_intent);
//
//    }


//    public View.OnClickListener onClick = new View.OnClickListener(){
//        @Override
//        public void onClick(View v) {
//            // TODO Auto-generated method stub
//            switch(v.getId()){
//                case R.id.LoginButton:
//                    Intent m_intent = new Intent(getActivity().getApplicationContext(),LookAroundActivity.class);
//                    startActivity(m_intent);
//                    break;
//
//            }
//        }
//    };
}
 