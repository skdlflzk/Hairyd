
package com.phairy.taxionly;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;


public class MyFragment extends Fragment {
    String TAG = Start.TAG;

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.e(TAG, "--MyFragment--");
        View view = inflater.inflate(R.layout.my_fragment, container, false);



        Button TakeButton = (Button) view.findViewById(R.id.TakeButton);
        TakeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                Intent m_intent = new Intent(getActivity().getApplicationContext(),CameraActivity.class);
//                startActivity(m_intent);
                //    getActivity().finish();
            }
        });

        return view;
    }
}
 