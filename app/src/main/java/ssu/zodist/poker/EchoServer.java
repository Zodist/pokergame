package ssu.zodist.poker;

import android.app.ProgressDialog;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

import static java.lang.Thread.sleep;

/**
 * Created by ZoDi on 2017-05-24.
 */

public class EchoServer {
    private String servName;
    private int servPort;
    private String myNickName, peerNickName;
    private Socket socket;
    private Handler hSendThread, hMainThread;
    private DataOutputStream oStream;
    private ProgressDialog ringProgressDialog;
    private MainActivity mainActivity;
    private boolean isFirstSend = true;
    private final int maxWaitingTime = 3000;
    private final int sleepDuration = 100;
    private final int maxSleepCount = maxWaitingTime/sleepDuration;
    private int serverState = 0;
    private final int flagConnecting = 1;
    private final int flagConnected = 2;
    private final int flagSendRunning = 4;
    private final int flagRecvRunning = 8;
    private final int ServerAvailble = (flagConnected|flagSendRunning|flagRecvRunning);
    private final int ServerUnavailable = 0;
    public int nCharsSent = 0;
    public int nCharsRecv = 0;

    public EchoServer(Handler h, MainActivity a){
        hMainThread = h;
        mainActivity = a;
    }
    public boolean isAvailable(){ return ((serverState & ServerAvailble)==ServerAvailble);}
    public boolean connect(String hname, int hport, String myName, String peerName){
        Log.d("EchoServer","IP address = "+ hname + " PortNum = " + hport + " MyName = "+ myName + " peerName "+ peerName);
        Log.d("EchoServer","connect() called in serverState = "+serverState);
        isFirstSend = true;
        if((serverState & flagConnecting)==flagConnecting || (serverState & flagConnected)==flagConnected)
            return false;
        if(waitForServerState(ServerUnavailable,"MainThread")==false){
            Log.d("EchoServer","waitForServerState(Unavailable) timed out");
            return false;
        }
        setServerStateFlag(flagConnecting);
        servName = hname;
        servPort = hport;
        myNickName = myName;
        peerNickName = peerName;
        ringProgressDialog = ProgressDialog.show(mainActivity,"Please wait...","Connecting to "+ hname + ":" + hport, true);
        ringProgressDialog.setCancelable(true);
        startThread(runnableConnect);
        nCharsSent = 0;
        nCharsRecv = 0;
        return true;
    }
    public boolean disconnect(){
        Log.d("EchoServer","disconnect() called in serverState = "+serverState);
        if((serverState & (flagConnecting | flagConnected))==0)
            return false;
        if(waitForServerState(flagConnected, "MainThread")==false){
            Log.d("EchoServer","waitForServerState(connected) timed out!");
            return false;
        }
        sendMessage(hSendThread,0,'Z');
        sleep(1000);
        if((serverState & flagConnected)==flagConnected){
            try { socket.close();}
            catch(Exception e){ e.printStackTrace();}
        }
        return true;
    }
    private char validKeys[] = { 'a', 'd', 's', 'w',' ', '0', '1', '2', '3', '4', '5', '6', 'Q' };
    private  boolean isValidKey(char key){
        for(int i=0;i<validKeys.length;i++){
            if(validKeys[i]==key) return true;
        }
        return false;
    }
    public boolean send(char ch){
        if(isValidKey(ch)==false){
            Log.d("EchoServer","not valid key ("+ ch + ")");
            return false;
        }
        if((serverState & (flagConnecting | flagConnected))==0)
            return false;
        if(waitForServerState(ServerAvailble, "MainThread")==false){
            Log.d("EchoServer","waitForServerState(connected) timed out!");
            return false;
        }
        if(isFirstSend){
            sendMessage(hSendThread,0,'A');
            isFirstSend = false;
        }
        sendMessage(hSendThread,0,ch);
        return true;
    }
    private Runnable runnableConnect = new Runnable() {
        @Override
        public void run() {
            try{
                SocketAddress socketAddress = new InetSocketAddress(servName,servPort);
                socket = new Socket();
                socket.connect(socketAddress, maxWaitingTime);
                setServerStateFlag(flagConnected);
                startThread(runnableSend);
                startThread(runnableRecv);
            }catch(Exception e){
                Log.d("EchoServer","ConnectThread : connect() fails!");
                e.printStackTrace();
            }
            resetServerStateFlag(flagConnecting);
            ringProgressDialog.dismiss();
        }
    };
    private Runnable runnableSend = new Runnable() {
        @Override
        public void run() {
            setServerStateFlag(flagSendRunning);
            try {
                oStream = new DataOutputStream(socket.getOutputStream());
            } catch (Exception e) {
                e.printStackTrace();
            }

            Looper.prepare();
            hSendThread = new Handler() {
                public void handleMessage(Message msg) {
                    try {
                        String line;
                        char ch = (char) msg.arg1;
                        if (ch == 'A')
                            //line = myNickName + String.valueOf('\n');
                            line = "/nick " + myNickName + String.valueOf('\n');
                        else if (ch == 'Z')
                            line = "/quit\n";
                        else {
                            line = "/msg " + peerNickName + String.valueOf(' ') + String.valueOf((char) msg.arg1) + String.valueOf('\n');
                            nCharsSent++;
                        }
                        Log.d("EchoServer", "SendThread : writeBytes(" + line + ")");
                        oStream.writeBytes(line);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            };
            Looper.loop();
            resetServerStateFlag(flagSendRunning);
            Log.d("EchoServer", "SendThread terminated");

            if ((serverState & flagConnected)==flagConnected){
                try{ socket.close();}
                catch(Exception e){ e.printStackTrace();}
                resetServerStateFlag(flagConnected);
                Log.d("EchoServer","Socket closed");
            }
        }
    };
    private Runnable runnableRecv = new Runnable() {
        @Override
        public void run() {
            setServerStateFlag(flagRecvRunning);
            try{
                BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String line, nicknameP2, peerNickNameP2 = peerNickName + ": ";
                char ch;
                while(true){
                    do{line = br.readLine();}while(line.length()==0);
                    line = line.replaceAll("\u001B\\[[;\\d]*m","");
                    Log.d("EchoServer: ", "RecvThread : ("+line+") = readLine()");
                    nicknameP2 = line.substring(0,peerNickNameP2.length());
                    if(peerNickNameP2.compareTo(nicknameP2)!=0){
                        Log.d("RecvThread","not valid nickname ("+nicknameP2+")");
                        continue;
                    }
                    ch = line.charAt(nicknameP2.length());
                    if(isValidKey(ch)==false){
                        Log.d("RecvThread", "not valid key ("+ ch + ")");
                        continue;
                    }else {
                        nCharsRecv++;
                        Log.d("RecvThread","(nCharsSent, nCharsRecv) = (" + nCharsSent + ", "+ nCharsRecv + ")");
                        sendMessage(hMainThread, 0, ch);
                    }
                }
            }catch (Exception e){
                Log.d("EchoServer", "Socket closed abnormally");
            }
            resetServerStateFlag(flagConnected);
            hSendThread.getLooper().quit();
            resetServerStateFlag(flagRecvRunning);
            Log.d("EchoServer", "RecvThread terminated");
        }
    };
    private void sendMessage(Handler h, int type, char ch){
        Message msg = Message.obtain(h,type);
        msg.arg1 = ch;
        h.sendMessage(msg);
    }
    private void startThread(Runnable runnable){
        Thread thread = new Thread(runnable);
        thread.setDaemon(true);
        thread.start();
    }
    public void sleep(int time){
        try{ Thread.sleep(time);}
        catch (Exception e){ e.printStackTrace(); }
    }
    synchronized private void setServerStateFlag(int flag){ serverState = (serverState | flag);}
    synchronized private void resetServerStateFlag(int flag){ serverState = (serverState & ~flag);}
    private boolean waitForServerState(int flag, String who){
        int count = 0;
        while(((serverState & flag)!=flag)&&count < maxSleepCount){
            Log.d("EchoServer",who+" : waitForServerState("+ flag +"&"+serverState+") waiting...");
            sleep(sleepDuration);
            count++;
        }
        if(((serverState & flag)==flag)) return true;
        else return false;
    }
}
