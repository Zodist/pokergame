package ssu.zodist.poker;

import android.content.Intent;
import android.content.DialogInterface;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.util.Log;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    private ImageView peerCardView[] = new ImageView[7];
    private ImageView myCardView[] = new ImageView[7];
    private TextView myMoneyView, myRankView;
    private TextView peerMoneyView, peerRankView;
    private TextView gameMoneyView, turnView, winnerView, betMoneyView;
    private Button startBtn, settingBtn, halfBtn, quartorBtn, callBtn, dieBtn, modeBtn, checkBtn, shuffleBtn, masterModeBtn;
    private AlertDialog quitBtn;

    private int person;
    private int winner;
    private PokerModel pokerModel = new PokerModel();
    private PokerState pokerState;

    private GameState savedState = GameState.Running;
    private GameState gameState = GameState.Initial;
    private char savedKey;
    private Handler mhandler = new Handler();

    private boolean myTurn = false;
    private boolean getPeerCard = false;
    private boolean masterMode = true;
    private boolean battleMode = false;

    private int numOfpeerCard = 0;
    private int peercard;
    private final int reqCode4SettingActivity = 0;
    private String serverHostName = "10.0.2.2";
    private int serverPortNum = 9000;
    private String myNickName = "me";
    private String peerNickName = "you";
    private ChatServer echoServer;

    public enum GameState {
        Error(-1), Initial(0), Running(1), Paused(2);
        private final int value;
        private GameState(int value) {
            this.value = value;
        }
        public int value() {
            return value;
        }
        public static GameState fromInteger(int value) {
            switch (value) {
                case -1:return Error;
                case 0:return Initial;
                case 1:return Running;
                case 2:return Paused;
                default:return null;
            }
        }
    }
    int stateMatrix[][] = {
            {-1, 1, -1, -1, -1, -1, -1, 1},
            {0, -1, 2, 1, 1, 1, 1, 1},
            {0, 1, -1, 1, 2, 1, -1, 1}
    };
    public enum UserCommand {
        NOP(-1), Quit(0), Start(1), Pause(2), Resume(3), Reshuffle(4), Distribute(5), Update(6), Recover(7), Win(8);
        private final int value;
        private UserCommand(int value) { this.value = value; }
        public int value() {
            return value;
        }
    }
    private void executeUserCommand(UserCommand cmd) {
        Log.d("Executing","["+gameState.value()+"] ["+cmd.value()+"]"+"!!! cmd before");
        GameState preState = gameState;
        gameState = GameState.fromInteger(stateMatrix[gameState.value()][cmd.value()]);
        if (gameState == GameState.Error) {
            Log.d("MainActivity", "game state error! (state.cmd)=(" + preState.value() + "," + cmd.value() + ")");
            gameState = preState;
            return;
        }

        switch (cmd.value()) {
            case 0:mhandler.post(runnableQuit);break;
            case 1:mhandler.post(runnableStart);break;
            case 2:mhandler.post(runnablePause);break;
            case 3:mhandler.post(runnableResume);break;
            case 4:mhandler.post(runnableReShuffle);break;
            case 5:mhandler.post(runnableDistribute);break;
            case 6:mhandler.post(runnableUpdate);break;
            case 7:mhandler.post(runnableRecover);break;
            default:
                Log.d("MainActivity", "unknown user command!");
                break;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowManager wm = getWindowManager();
        if(wm == null) return;
        int rotation = wm.getDefaultDisplay().getRotation();
        if(rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180){
            setContentView(R.layout.activity_main);
        }else{
            setContentView(R.layout.activity_main_landscape);
        }
        peerCardView[0] = (ImageView)this.findViewById(R.id.peercardview1);
        peerCardView[1] = (ImageView)this.findViewById(R.id.peercardview2);
        peerCardView[2] = (ImageView)this.findViewById(R.id.peercardview3);
        peerCardView[3] = (ImageView)this.findViewById(R.id.peercardview4);
        peerCardView[4] = (ImageView)this.findViewById(R.id.peercardview5);
        peerCardView[5] = (ImageView)this.findViewById(R.id.peercardview6);
        peerCardView[6] = (ImageView)this.findViewById(R.id.peercardview7);
        myCardView[0] = (ImageView)this.findViewById(R.id.mycardview1);
        myCardView[1] = (ImageView)this.findViewById(R.id.mycardview2);
        myCardView[2] = (ImageView)this.findViewById(R.id.mycardview3);
        myCardView[3] = (ImageView)this.findViewById(R.id.mycardview4);
        myCardView[4] = (ImageView)this.findViewById(R.id.mycardview5);
        myCardView[5] = (ImageView)this.findViewById(R.id.mycardview6);
        myCardView[6] = (ImageView)this.findViewById(R.id.mycardview7);

        myMoneyView = (TextView)this.findViewById(R.id.myMoneyView);
        myRankView = (TextView)this.findViewById(R.id.myRankView);
        peerMoneyView = (TextView)this.findViewById(R.id.peerMoneyView);
        peerRankView = (TextView)this.findViewById(R.id.peerRankView);
        gameMoneyView = (TextView)this.findViewById(R.id.gameMoneyView);
        turnView = (TextView)this.findViewById(R.id.turnView);
        betMoneyView = (TextView)this.findViewById(R.id.BetMoneyView);
        winnerView = (TextView)this.findViewById(R.id.winnerView);

        startBtn = (Button)this.findViewById(R.id.startBtn);
        settingBtn = (Button)this.findViewById(R.id.settingBtn);
        modeBtn = (Button)this.findViewById(R.id.modeBtn);
        halfBtn = (Button)this.findViewById(R.id.HalfBtn);
        quartorBtn = (Button)this.findViewById(R.id.QuartorBtn);
        callBtn = (Button)this.findViewById(R.id.CallBtn);
        dieBtn = (Button)this.findViewById(R.id.DieBtn);
        checkBtn = (Button)this.findViewById(R.id.CheckBtn);
        shuffleBtn = (Button)this.findViewById(R.id.shuffleBtn);
        masterModeBtn = (Button)this.findViewById(R.id.masterModeBtn);
        quitBtn = (AlertDialog) AlertDialogBtnCreate();

        startBtn.setOnClickListener(OnclickListner);
        settingBtn.setOnClickListener(OnclickListner);
        modeBtn.setOnClickListener(OnclickListner);
        halfBtn.setOnClickListener(OnclickListner);
        quartorBtn.setOnClickListener(OnclickListner);
        callBtn.setOnClickListener(OnclickListner);
        dieBtn.setOnClickListener(OnclickListner);
        checkBtn.setOnClickListener(OnclickListner);
        shuffleBtn.setOnClickListener(OnclickListner);
        masterModeBtn.setOnClickListener(OnclickListner);

        halfBtn.setEnabled(false);
        quartorBtn.setEnabled(false);
        callBtn.setEnabled(false);
        dieBtn.setEnabled(false);
        checkBtn.setEnabled(false);

        echoServer = new ChatServer(hPeerViews, MainActivity.this);
    }
    private AlertDialog AlertDialogBtnCreate() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("BattleTetris: ")
                .setMessage("Do you want to quit?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        //gameResult = "You Loose!";
                        executeUserCommand(UserCommand.Quit);
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        if (savedState == GameState.Running)
                            executeUserCommand(UserCommand.Resume);
                    }
                });
        return builder.create();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {   //Save Informatino
        Log.d("MainActivity", "onSave");
        super.onSaveInstanceState(outState);
        outState.putSerializable("pokerModel", pokerModel);
        outState.putInt("saveState", savedState.value());
    }

    @Override
    protected void onRestoreInstanceState(Bundle inState) { //restore information
        super.onRestoreInstanceState(inState);
        Log.d("MainActivity", "onRestore");
        savedState = GameState.fromInteger(inState.getInt("saveState"));
        if (savedState != GameState.Initial) {
            pokerModel = (PokerModel) inState.getSerializable("pokerModel");
        }
        Log.d("MainActivity", pokerModel.getGameMoney()+" is gamemoney");
        executeUserCommand(UserCommand.Recover);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d("MainActivity", "onDestroy");
        if (battleMode && echoServer.isAvailable()) {
            echoServer.send('Q');
            echoServer.disconnect();
        }
    }
    @Override
    public void onPause(){
        super.onPause();
        executeUserCommand(UserCommand.Pause);
    }
    @Override
    public void onResume(){
        super.onResume();
        executeUserCommand(UserCommand.Resume);
    }

    //Runnable function declare========================================================================
    private Runnable runnableQuit = new Runnable() {
        @Override
        public void run() {
            setButtonState();
            startBtn.setText("Start");
            halfBtn.setEnabled(false);
            quartorBtn.setEnabled(false);
            callBtn.setEnabled(false);
            dieBtn.setEnabled(false);
            checkBtn.setEnabled(false);
            Toast.makeText(MainActivity.this, "Game Over!", Toast.LENGTH_SHORT).show();
            if (battleMode && echoServer.isAvailable()) {
                echoServer.send('Q');
                echoServer.disconnect();
                //initPeerGame();
            }
        }
    };
    private Runnable runnableStart = new Runnable() {
        @Override
        public void run() {
            if(masterMode)  myTurn = true;
            else myTurn = false;
            winner = -1;
            person = 1;
            numOfpeerCard = 0;
            if(gameState==GameState.Running) {
                startBtn.setText("Quit");
                person = pokerModel.initialize();
                updateCardView();
                updateMyStatusView();
                updatePeerStatusView();
                updateGameStatusView();
            }else {
                startBtn.setText("Start");
                halfBtn.setEnabled(false);
                quartorBtn.setEnabled(false);
                callBtn.setEnabled(false);
                dieBtn.setEnabled(false);
                checkBtn.setEnabled(false);
            }
            //tmp 2인용 test
            if (battleMode ) {
                int numOfStartCard = 3;
                if (echoServer.connect(serverHostName, serverPortNum, myNickName, peerNickName) != false){
                    //while(echoServer.isAvailable()==false){}
                    if(masterMode){     // 주인 상태일 때 3장의 카드를 먼저 보낸다.
                        echoServer.send('s');
                        for(int i=0;i<numOfStartCard;i++){
                            if(echoServer.sendcard(pokerModel.setMyCard()) == false)
                                Log.d("MainActivity","Error On Sending Cards");
                        }
                        //while(numOfpeerCard!=3){ setButtonState(); }// 모두 disable 되어야한다.
                    }else{              // 주인 상태가 아니면 3장의 카드를 받고 3장을 보낸다.
                        //getPeerCard = false;
                        //while(numOfpeerCard!=3){ setButtonState(); }// 모두 disable 되어야한다.
                        //while(!getPeerCard){ setButtonState(); }// 모두 disable 되어야한다.
                        //getPeerCard = false;
                        //for(int i=0;i<numOfStartCard;i++){
                        //    if(echoServer.sendcard(pokerModel.setMyCard()) == false)
                        //        Log.d("MainActivity","Error On Sending Cards");
                        //}
                    }
                }else{
                    Log.d("MainActivity","Error On connecting");
                    executeUserCommand(UserCommand.Quit);
                    return;
                }
            }else{      //1인용 모드
                for(int i=0;i<3;i++){ pokerModel.CardDistribute(); }
            }
            updateCardView();
            updateMyStatusView();
            updatePeerStatusView();
            updateGameStatusView();
            setButtonState();
            Toast.makeText(MainActivity.this, "Game Started", Toast.LENGTH_SHORT).show();
        }
    };
    private Runnable runnablePause = new Runnable() {
        @Override
        public void run() {
            Toast.makeText(MainActivity.this, "Game Pause", Toast.LENGTH_SHORT).show();
        }
    };
    private Runnable runnableReShuffle = new Runnable() {
        @Override
        public void run() {
            winner = -1;
            pokerModel.readyForGame();
            for(int i=0;i<7;i++) { peerCardView[i].setImageResource(android.R.color.transparent);}
            for(int i=0;i<7;i++) { myCardView[i].setImageResource(android.R.color.transparent);}
            for(int i=0;i<3;i++){ pokerModel.CardDistribute(); }
            updateCardView();
            updateMyStatusView();
            updatePeerStatusView();
            updateGameStatusView();
            setButtonState();
        }
    };
    private Runnable runnableDistribute = new Runnable() {
        @Override
        public void run() {
            Toast.makeText(MainActivity.this, "Card Distributing", Toast.LENGTH_LONG).show();
            if(battleMode) {
                if (masterMode == true) {       //leader로서 먼저 카드를 보낸다.
                    int mycard = pokerModel.setMyCard();
                    //echoServer.sendcard(mycard);   //내 카드를 보낸다.
                    getPeerCard = false;
                } else {                      //leader에게서 먼저 카드를 받은 뒤 카드를 보낸다.
                    while (!getPeerCard) {    //카드를 받을 때까지 기다린다.
                        setButtonState();   //버튼을 모두 disable 해야 한다.
                    }
                    getPeerCard = false;
                    pokerModel.setPeerCard(peercard);  // 임시로 설정
                    int mycard = pokerModel.setMyCard();
                    //echoServer.sendcard(mycard);   //내 카드를 보낸다.
                }
                return;
            }else{
                setButtonState();
                pokerModel.CardDistribute();
            }
            pokerModel.state = PokerState.BetStart;
            updateCardView();
        }
    };
    private Runnable runnableUpdate = new Runnable() {
        @Override
        public void run() {
            setButtonState();
            if (battleMode) {
                pokerState = pokerModel.accept(savedKey, 0);    //내 턴으로 입력
                if (!echoServer.send(savedKey)) {
                    //gameResult = "Connection Error!";
                    //executeUserCommand(UserCommand.Quit);
                    return;
                }
            }else
                pokerState = pokerModel.accept(savedKey, person);    //배틀 모드가 아닌 경우

            switch(pokerState){
                case GiveUp:        winner = pokerModel.gameover(pokerState);Log.d("MainActivity","!GiveUp!");
                                    Toast.makeText(MainActivity.this, (winner+1)+" player is win!", Toast.LENGTH_LONG).show();break;
                case GameOverWell:  winner = pokerModel.gameover(pokerState);Log.d("MainActivity","!GameOverWell!");
                                    Toast.makeText(MainActivity.this, (winner+1)+" player is win!", Toast.LENGTH_LONG).show();break;
                case Betting:
                    if(!battleMode) person = (person + 1) % 2;
                    Log.d("MainActivity","!Betting!"); break;
                case BetOver:
                    if(!battleMode) { person = (person + 1) % 2;pokerModel.setDistributeState(); }
                    else{ pokerModel.setDistributeState(); executeUserCommand(UserCommand.Distribute); }
                    Log.d("MainActivity","!BetOver!");break;
            }
            updateMyStatusView();
            updatePeerStatusView();
            updateGameStatusView();
            if(!battleMode && pokerModel.getState()==PokerState.Distrubute) {
                executeUserCommand(UserCommand.Distribute);
            }
            setButtonState();
            if(battleMode) myTurn = false;     //상대방에게 나의 선택을 보내고 내 턴을 종료
        }
    };
    private Runnable runnableResume = new Runnable() {
        @Override
        public void run() {
            Toast.makeText(MainActivity.this, "Game Resume", Toast.LENGTH_SHORT).show();
        }
    };
    private Runnable runnableRecover = new Runnable() {
        @Override
        public void run() {
            updateMyStatusView();
            updatePeerStatusView();
            updateGameStatusView();
            updateCardView();
            setButtonState();
        }
    };
    //Runnable function declare========================================================================

    //Network Related Function==============================================================================
    private Handler hPeerViews = new Handler(){
        public void handleMessage(Message msg){
            Log.d("MainActivity","hPeerViews Called");
            if (echoServer.isAvailable() == false || echoServer.isAcknowledged()== false) {
                Log.d("MainActivity","hPeerView : echoServer is not Available");
                return;
            }
            if(pokerModel.getState() == PokerState.Distrubute){
                peercard = echoServer.getcard(msg);                                 // int형으로 카드를 받는다.
                if(peercard == -1)
                    return;
                Log.d("MainActivity", "hPeerView get card : " + peercard );
                pokerModel.setPeerCard(peercard);
                numOfpeerCard++;
                if(masterMode==false && numOfpeerCard==3){
                    for(int i=0;i<3;i++){
                        if(echoServer.sendcard(pokerModel.setMyCard()) == false)
                            Log.d("MainActivity","Error On Sending Cards");
                        pokerModel.state = PokerState.BetStart;
                    }
                }else if(masterMode=true && numOfpeerCard==3){
                    pokerModel.state = PokerState.BetStart;
                }
                updateCardView();
                getPeerCard = true;
                return;
            }
            char key = echoServer.getchar(msg);
            if (key == 'Q') {
                //gameResult = "You Win!";
                //executeUserCommand(UserCommand.Win);
                return;
            }
            setButtonState();
            pokerState = pokerModel.accept(key, 1);    //상대방 턴으로 입력
            switch(pokerState){
                case GiveUp:        winner = pokerModel.gameover(pokerState);Log.d("MainActivity","!GiveUp!");
                    Toast.makeText(MainActivity.this, (winner+1)+" player is win!", Toast.LENGTH_LONG).show();break;
                case GameOverWell:  winner = pokerModel.gameover(pokerState);Log.d("MainActivity","!GameOverWell!");
                    Toast.makeText(MainActivity.this, (winner+1)+" player is win!", Toast.LENGTH_LONG).show();break;
                case Betting:       Log.d("MainActivity","!Betting!"); break;
                case BetOver:       executeUserCommand(UserCommand.Distribute);Log.d("MainActivity","!BetOver!");break;
            }
            updateMyStatusView();
            updatePeerStatusView();
            updateGameStatusView();
            setButtonState();
            myTurn = true;      //상대방의 턴을 끝내면 내 턴으로 만든다.
        }
    };
    private void startSettingActivity(int reqCode) {
        Intent intent = new Intent(MainActivity.this, SettingActivity.class);
        intent.putExtra("serverHostName", serverHostName);
        intent.putExtra("serverPortNum", String.valueOf(serverPortNum));
        intent.putExtra("myNickName", myNickName);
        intent.putExtra("peerNickName", peerNickName);
        startActivityForResult(intent, reqCode);
    }
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case reqCode4SettingActivity:
                if (resultCode == RESULT_OK) {
                    serverHostName = data.getStringExtra("serverHostName");
                    String s = data.getStringExtra("serverPortNum");
                    serverPortNum = Integer.parseInt(s);
                    myNickName = data.getStringExtra("myNickName");
                    peerNickName = data.getStringExtra("peerNickName");
                    Log.d("MainActivity", "SettingActivity returned (" + serverHostName + "." + serverPortNum);
                } else if (resultCode == RESULT_CANCELED) {
                    Log.d("MainActivity", "SettingActivity canceled");
                }
                break;
        }
    }
    //Network Related Function==============================================================================

    public View.OnClickListener OnclickListner = new View.OnClickListener(){
        public void onClick(View v) {
            int id = v.getId();
            UserCommand cmd = UserCommand.NOP;
            switch (id) {
                case R.id.startBtn:
                    if(gameState==GameState.Initial) { cmd = UserCommand.Start; break; }
                    else { quitBtn.show(); return; }
                case R.id.shuffleBtn:   cmd = UserCommand.Reshuffle;break;
                case R.id.settingBtn:
                    if (gameState == GameState.Initial)
                        startSettingActivity(reqCode4SettingActivity); return;
                case R.id.masterModeBtn:
                    masterMode = !masterMode;
                    if(masterMode) masterModeBtn.setText("M");
                    else masterModeBtn.setText("S");
                    return;
                case R.id.modeBtn:
                    battleMode = !battleMode;
                    if (battleMode) modeBtn.setText("2");
                    else modeBtn.setText("1");
                    return;
                case R.id.HalfBtn:      savedKey = 'h'; cmd = UserCommand.Update; break;
                case R.id.QuartorBtn:   savedKey = 'q'; cmd = UserCommand.Update; break;
                case R.id.CheckBtn:     savedKey = 'k'; cmd = UserCommand.Update; break;
                case R.id.CallBtn:      savedKey = 'c'; cmd = UserCommand.Update; break;
                case R.id.DieBtn:       savedKey = 'd'; cmd = UserCommand.Update; break;
                default:                Log.d("MainActivity","Unknown Key!");
            }
            executeUserCommand(cmd);
        }
    };
    public void setButtonState(){
        startBtn.setEnabled(true);
        /*
        if(!myTurn){
            shuffleBtn.setEnabled(false);
            settingBtn.setEnabled(false);
            masterModeBtn.setEnabled(false);
            modeBtn.setEnabled(false);
            halfBtn.setEnabled(false);
            quartorBtn.setEnabled(false);
            callBtn.setEnabled(false);
            dieBtn.setEnabled(false);
            checkBtn.setEnabled(false);
            return;
        }
        */
        switch(pokerModel.getState()){
            case Distrubute:
                shuffleBtn.setEnabled(false);
                settingBtn.setEnabled(false);
                masterModeBtn.setEnabled(false);
                modeBtn.setEnabled(false);
                halfBtn.setEnabled(false);
                quartorBtn.setEnabled(false);
                callBtn.setEnabled(false);
                dieBtn.setEnabled(false);
                checkBtn.setEnabled(false);
            case Wait:
            case BetStart:
                shuffleBtn.setEnabled(false);
                settingBtn.setEnabled(false);
                masterModeBtn.setEnabled(false);
                modeBtn.setEnabled(false);
                halfBtn.setEnabled(true);
                quartorBtn.setEnabled(true);
                callBtn.setEnabled(true);
                dieBtn.setEnabled(true);
                checkBtn.setEnabled(true);
                break;
            case GameOverWell:
            case GiveUp:
                shuffleBtn.setEnabled(true);
                settingBtn.setEnabled(true);
                masterModeBtn.setEnabled(true);
                modeBtn.setEnabled(true);
                halfBtn.setEnabled(false);
                quartorBtn.setEnabled(false);
                callBtn.setEnabled(false);
                dieBtn.setEnabled(false);
                checkBtn.setEnabled(false);
                break;
            case BetOver:
            case Betting:
                shuffleBtn.setEnabled(false);
                settingBtn.setEnabled(false);
                masterModeBtn.setEnabled(false);
                modeBtn.setEnabled(false);
                halfBtn.setEnabled(true);
                quartorBtn.setEnabled(true);
                callBtn.setEnabled(true);
                dieBtn.setEnabled(true);
                checkBtn.setEnabled(false);
                break;
        }
    }
    public void updateMyStatusView(){
        int money = pokerModel.getMyMoney();
        int rank = pokerModel.getMyRank();
        if(money == 0) myMoneyView.setText("All in");
        else myMoneyView.setText(String.valueOf(money));
        String tmp ="";
        switch(rank){
            case 0: tmp = "Top Card";break;
            case 1: tmp = "One Pair";break;
            case 2: tmp = "Two Pair";break;
            case 3: tmp = "Triple";break;
            case 4: tmp = "Straight";break;
            case 5: tmp = "Back Straight";break;
            case 6: tmp = "Mountain";break;
            case 7: tmp = "Flush";break;
        }
        myRankView.setText(tmp);
    }
    public void updatePeerStatusView(){
        int money = pokerModel.getPeerMoney();
        int rank = pokerModel.getPeerRank();
        if(money == 0) peerMoneyView.setText("All in");
        else peerMoneyView.setText(String.valueOf(money));
        String tmp ="";
        switch(rank){
            case 0: tmp = "Top Card";break;
            case 1: tmp = "One Pair";break;
            case 2: tmp = "Two Pair";break;
            case 3: tmp = "Triple";break;
            case 4: tmp = "Straight";break;
            case 5: tmp = "Back Straight";break;
            case 6: tmp = "Mountain";break;
            case 7: tmp = "Flush";break;
        }
        peerRankView.setText(tmp);
    }
    public void updateGameStatusView(){
        int gamemoney = pokerModel.getGameMoney();
        int bettingMoney = pokerModel.getBetMoney();
        int turn = person;
        int awinner = winner;
        gameMoneyView.setText(String.valueOf(gamemoney));
        if(turn == 1) turnView.setText("Peer Turn");
        else turnView.setText("My Turn");
        betMoneyView.setText(String.valueOf(bettingMoney));
        if(awinner==-1) winnerView.setText("NOT END");
        else if(awinner==0) winnerView.setText("Mcard win");
        else winnerView.setText("Pcard win");
    }
    public void updateCardView() {
        int mycards[][] = pokerModel.getmycards();
        int peercards[][] = pokerModel.getpeercards();
        for (int i = 0; i < 7; i++) {
            int tmp = 0;
            switch (mycards[i][0]) {
                case 4:
                    switch (mycards[i][1]) {
                        case 1:tmp = R.drawable.sa;break;
                        case 2:tmp = R.drawable.s2;break;
                        case 3:tmp = R.drawable.s3;break;
                        case 4:tmp = R.drawable.s4;break;
                        case 5:tmp = R.drawable.s5;break;
                        case 6:tmp = R.drawable.s6;break;
                        case 7:tmp = R.drawable.s7;break;
                        case 8:tmp = R.drawable.s8;break;
                        case 9:tmp = R.drawable.s9;break;
                        case 10:tmp = R.drawable.st;break;
                        case 11:tmp = R.drawable.sj;break;
                        case 12:tmp = R.drawable.sq;break;
                        case 13:tmp = R.drawable.sk;break;
                    }
                    break;
                case 3:
                    switch (mycards[i][1]) {
                        case 1:tmp = R.drawable.da;break;
                        case 2:tmp = R.drawable.d2;break;
                        case 3:tmp = R.drawable.d3;break;
                        case 4:tmp = R.drawable.d4;break;
                        case 5:tmp = R.drawable.d5;break;
                        case 6:tmp = R.drawable.d6;break;
                        case 7:tmp = R.drawable.d7;break;
                        case 8:tmp = R.drawable.d8;break;
                        case 9:tmp = R.drawable.d9;break;
                        case 10:tmp = R.drawable.dt;break;
                        case 11:tmp = R.drawable.dj;break;
                        case 12:tmp = R.drawable.dq;break;
                        case 13:tmp = R.drawable.dk;break;
                    }
                    break;
                case 2:
                    switch (mycards[i][1]) {
                        case 1:tmp = R.drawable.ha;break;
                        case 2:tmp = R.drawable.h2;break;
                        case 3:tmp = R.drawable.h3;break;
                        case 4:tmp = R.drawable.h4;break;
                        case 5:tmp = R.drawable.h5;break;
                        case 6:tmp = R.drawable.h6;break;
                        case 7:tmp = R.drawable.h7;break;
                        case 8:tmp = R.drawable.h8;break;
                        case 9:tmp = R.drawable.h9;break;
                        case 10:tmp = R.drawable.ht;break;
                        case 11:tmp = R.drawable.hj;break;
                        case 12:tmp = R.drawable.hq;break;
                        case 13:tmp = R.drawable.hk;break;
                    }
                    break;
                case 1:
                    switch (mycards[i][1]) {
                        case 1:tmp = R.drawable.ca;break;
                        case 2:tmp = R.drawable.c2;break;
                        case 3:tmp = R.drawable.c3;break;
                        case 4:tmp = R.drawable.c4;break;
                        case 5:tmp = R.drawable.c5;break;
                        case 6:tmp = R.drawable.c6;break;
                        case 7:tmp = R.drawable.c7;break;
                        case 8:tmp = R.drawable.c8;break;
                        case 9:tmp = R.drawable.c9;break;
                        case 10:tmp = R.drawable.ct;break;
                        case 11:tmp = R.drawable.cj;break;
                        case 12:tmp = R.drawable.cq;break;
                        case 13:tmp = R.drawable.ck;break;
                    }
                    break;
                default:break;
            }
            myCardView[i].setImageResource(tmp);
        }
        for (int i = 0; i < 7; i++) {
            int tmp = 0;
            switch (peercards[i][0]) {
                case 4:
                    switch (peercards[i][1]) {
                        case 1:tmp = R.drawable.sa;break;
                        case 2:tmp = R.drawable.s2;break;
                        case 3:tmp = R.drawable.s3;break;
                        case 4:tmp = R.drawable.s4;break;
                        case 5:tmp = R.drawable.s5;break;
                        case 6:tmp = R.drawable.s6;break;
                        case 7:tmp = R.drawable.s7;break;
                        case 8:tmp = R.drawable.s8;break;
                        case 9:tmp = R.drawable.s9;break;
                        case 10:tmp = R.drawable.st;break;
                        case 11:tmp = R.drawable.sj;break;
                        case 12:tmp = R.drawable.sq;break;
                        case 13:tmp = R.drawable.sk;break;
                    }
                    break;
                case 3:
                    switch (peercards[i][1]) {
                        case 1:tmp = R.drawable.da;break;
                        case 2:tmp = R.drawable.d2;break;
                        case 3:tmp = R.drawable.d3;break;
                        case 4:tmp = R.drawable.d4;break;
                        case 5:tmp = R.drawable.d5;break;
                        case 6:tmp = R.drawable.d6;break;
                        case 7:tmp = R.drawable.d7;break;
                        case 8:tmp = R.drawable.d8;break;
                        case 9:tmp = R.drawable.d9;break;
                        case 10:tmp = R.drawable.dt;break;
                        case 11:tmp = R.drawable.dj;break;
                        case 12:tmp = R.drawable.dq;break;
                        case 13:tmp = R.drawable.dk;break;
                    }
                    break;
                case 2:
                    switch (peercards[i][1]) {
                        case 1:tmp = R.drawable.ha;break;
                        case 2:tmp = R.drawable.h2;break;
                        case 3:tmp = R.drawable.h3;break;
                        case 4:tmp = R.drawable.h4;break;
                        case 5:tmp = R.drawable.h5;break;
                        case 6:tmp = R.drawable.h6;break;
                        case 7:tmp = R.drawable.h7;break;
                        case 8:tmp = R.drawable.h8;break;
                        case 9:tmp = R.drawable.h9;break;
                        case 10:tmp = R.drawable.ht;break;
                        case 11:tmp = R.drawable.hj;break;
                        case 12:tmp = R.drawable.hq;break;
                        case 13:tmp = R.drawable.hk;break;
                    }
                    break;
                case 1:
                    switch (peercards[i][1]) {
                        case 1:tmp = R.drawable.ca;break;
                        case 2:tmp = R.drawable.c2;break;
                        case 3:tmp = R.drawable.c3;break;
                        case 4:tmp = R.drawable.c4;break;
                        case 5:tmp = R.drawable.c5;break;
                        case 6:tmp = R.drawable.c6;break;
                        case 7:tmp = R.drawable.c7;break;
                        case 8:tmp = R.drawable.c8;break;
                        case 9:tmp = R.drawable.c9;break;
                        case 10:tmp = R.drawable.ct;break;
                        case 11:tmp = R.drawable.cj;break;
                        case 12:tmp = R.drawable.cq;break;
                        case 13:tmp = R.drawable.ck;break;
                    }
                    break;
                default:break;
            }
            peerCardView[i].setImageResource(tmp);
        }
    }
}
