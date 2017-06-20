package ssu.zodist.poker;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;

/**
 * Created by ZoDi on 2017-05-24.
 */

public class SettingActivity extends AppCompatActivity {
    private EditText viewHostName,viewPortNum;
    private EditText viewMyNickName,viewPeerNickName;
    private String serverHostName,serverPortNum;
    private String myNickName, peerNickName;

    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
        viewHostName = (EditText)findViewById(R.id.editview1);
        viewPortNum = (EditText)findViewById(R.id.editview2);
        viewMyNickName = (EditText)findViewById(R.id.editview3);
        viewPeerNickName = (EditText)findViewById(R.id.editview4);
        Intent intent = getIntent();
        serverHostName = intent.getStringExtra("serverHostName");
        serverPortNum = intent.getStringExtra("serverPortNum");
        myNickName = intent.getStringExtra("myNickName");
        peerNickName = intent.getStringExtra("peerNickName");
        if(serverHostName == null){
            serverHostName = "255.255.255.255";
            serverPortNum = "9999";
            myNickName = "me";
            peerNickName = "you";
        }
        viewHostName.setText(serverHostName);
        viewPortNum.setText(serverPortNum);
        viewMyNickName.setText(myNickName);
        viewPeerNickName.setText(peerNickName);
    }
    public void onClick(View v){
        switch (v.getId()){
            case R.id.confirm:
                Intent intent = new Intent();
                intent.putExtra("serverHostName", viewHostName.getText().toString());
                intent.putExtra("serverPortNum",viewPortNum.getText().toString());
                intent.putExtra("myNickName",viewMyNickName.getText().toString());
                intent.putExtra("peerNickName",viewPeerNickName.getText().toString());
                setResult(RESULT_OK, intent);
                break;
            case R.id.cancel:
                setResult(RESULT_CANCELED);
                break;
        }
        finish();
    }
}
