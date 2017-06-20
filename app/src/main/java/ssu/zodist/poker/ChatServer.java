package ssu.zodist.poker;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class ChatServer {
    private String myNickName, peerNickName;
    private InetSocket inetSocket;
    public int numOfPeerCard = 0;

    public ChatServer(Handler h, MainActivity a) { inetSocket = new InetSocket(h, a); numOfPeerCard = 0;}
    public boolean isAvailable() {
        return inetSocket.isAvailable();
    }
    public boolean isAcknowledged() {
        return inetSocket.isAcknowledged();
    }
    public boolean connect(String hname, int hport, String myName, String peerName) {
        myNickName = myName;
        peerNickName = peerName;
        if (inetSocket.connect(hname, hport, "/who\n", peerName) == false)
            return false;
        if (inetSocket.send("/nick " + myNickName + String.valueOf('\n')) == false)
            return false;
        if (inetSocket.send("/who\n") == false)
            return false;
        return true;
    }
    public boolean send(char ch) {
        String string = "/msg " + peerNickName + String.valueOf(' ') + String.valueOf(ch) + String.valueOf('\n');
        return inetSocket.send(string);
    }
    public boolean sendcard(int card) {
        String string = "/msg " + peerNickName + String.valueOf(' ') + String.valueOf(card) + String.valueOf('\n');
        return inetSocket.send(string);
    }
    public int getcard(Message msg) {
        numOfPeerCard++;
        String string = (String) msg.obj; // string delivered from peer
        string = string.replaceAll("\u001B\\[[;\\d]*m", ""); // remove color codes in the line
        String peerNickNameP2 = peerNickName + ": ";
        String nicknameP2 = string.substring(0, peerNickNameP2.length());
        if (peerNickNameP2.compareTo(nicknameP2) != 0) {
            Log.d("ChatServer", "not my peer (" + nicknameP2 + ")");
            return -1;
        }
        Log.d("ChatSerer","receive Card : "+ Integer.parseInt(string.substring(nicknameP2.length())));
        return Integer.parseInt(string.substring(nicknameP2.length()));
    }
    public char getchar(Message msg) {
        String string = (String) msg.obj; // string delivered from peer
        string = string.replaceAll("\u001B\\[[;\\d]*m", ""); // remove color codes in the line
        String peerNickNameP2 = peerNickName + ": ";
        String nicknameP2 = string.substring(0, peerNickNameP2.length());
        if (peerNickNameP2.compareTo(nicknameP2) != 0) {
            Log.d("ChatServer", "not my peer (" + nicknameP2 + ")");
            return 'Q';
        }
        return string.charAt(nicknameP2.length());
    }
    public boolean disconnect() {
        String string = "/quit\n";
        if (inetSocket.send(string) == false)
            return false;
        return inetSocket.disconnect();
    }
}
