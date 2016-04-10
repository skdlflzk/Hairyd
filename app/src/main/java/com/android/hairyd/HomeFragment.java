
package com.android.hairyd;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;


public class HomeFragment extends Fragment {  //} implements View.OnClickListener{

    String TAG = Start.TAG;


    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        Log.e(TAG, "--HomeFragment--");
        View view = inflater.inflate(R.layout.home_fragment, container, false);

        Button loginButton = (Button) view.findViewById(R.id.LoginButton);
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent m_intent = new Intent(getActivity().getApplicationContext(),LoginActivity.class);
                startActivity(m_intent);
                //    getActivity().finish();
            }
        });

        return view;
    }

//    @Override
//    public void onClick(View view) {
//        switch( view.getId() ){
//            case R.id.LoginButton :
//                Intent m_intent = new Intent(getActizvity().getApplicationContext(),LoginActivity.class);
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
 