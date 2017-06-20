package ssu.zodist.poker;

/**
 * Created by ZoDi on 2017-05-29.
 */

import android.util.Log;
import java.io.Serializable;
import java.util.Random;

public class PokerModel implements Serializable{
    public static Person players[];
    public static Deck deck = new Deck();
    public static int NumOfPlayers = 2;
    public static int cardsNumber = 0;
    public static int actualPlayers;
    public static int betmoney;
    public static int gamemoney;
    public static PokerState state;

    public PokerModel(){
        NumOfPlayers = 2;
        //cardsNumber = 0;
    }
    public int initialize(){
        // 게임 시작 후 상태 초기화=========================================================
        players = new Person[2];
        NumOfPlayers = 2;
        cardsNumber = 0;
        deck = new Deck();
        players[0]=new Person();
        players[1]=new Person();
        return readyForGame();
    }
    public int readyForGame(){
        players[0].resetCards();
        players[1].resetCards();
        //state = PokerState.Wait;
        cardsNumber = 0;
        gamemoney = 0;
        betmoney = 10;	// 게임 참가비 설정
        deck.shuffle();
        actualPlayers = NumOfPlayers;
        state = PokerState.Distrubute;
        for(int i=0;i<actualPlayers;i++){ gamemoney+=players[i].payMoney(); }
        //for(int i=0;i<3;i++){ CardDistribute(); }
        // 게임 시작 후 상태 초기화=========================================================
        //for(int i=0;i<actualPlayers;i++){ gamemoney+=players[i].payMoney(); }
        //return compareCard(2);
        return 0;
    }

    public PokerState getState() { return state; }
    public void setDistributeState() { state = PokerState.Distrubute; }
    public int getGameMoney() { return gamemoney; }
    public int getBetMoney() { return betmoney; }

    public int[][] getmycards(){
        return players[0].getCards();
    }
    public int getMyMoney() { return players[0].getMoney(); }
    public int getMyRank(){ return players[0].getRank(); }

    public int getPeerRank() { return players[1].getRank(); }
    public int getPeerMoney() { return players[1].getMoney(); }
    public int[][] getpeercards(){
        return players[1].getCards();
    }
    //Adding Network Related function=======================================================================
    public void setPeerCard(int card){ players[1].setCard(card); deck.accept(card); }
    public int setMyCard(){
        Random random = new Random(); //난수 발생
        while(true){
            if(deck.accept(random.nextInt(52))){
                players[0].setCard(deck.distribute());
                break;
            }
        }
        cardsNumber++;
        return deck.distribute();
    }
    //Adding Network function=======================================================================

    public PokerState accept(char ch, int whoisturn) {
        PersonState personState;
        PersonState beforePState;
        beforePState = players[whoisturn].getState();
        switch (beforePState){
            case Allin:
                if(cardsNumber==7) {
                    state = PokerState.GameOverWell;
                    return PokerState.GameOverWell;
                }
                state = PokerState.BetOver;
                return PokerState.BetOver;
            case Call:
            case Wait:
            case Bet:
                if(state == PokerState.BetStart){
                    state = PokerState.Betting;
                }
                personState = players[whoisturn].accept(ch,betmoney,gamemoney);
                switch (personState){
                    case Die:
                        actualPlayers--;
                        if(actualPlayers == 1) {    // 종료조건
                            state = PokerState.GiveUp;
                            return PokerState.GiveUp;
                        }
                        break;
                    case Call:
                        int tmp = players[whoisturn].payMoney();
                        betmoney = tmp;
                        Log.d("GameModel","Call : " + whoisturn + " player pay "+ tmp + " //BetMoney=" + betmoney);
                        gamemoney += betmoney;
                        if(cardsNumber == 7) { //종료조건 7장 모두 분배
                            state = PokerState.GameOverWell;
                            return PokerState.GameOverWell;
                        }
                        state = PokerState.BetOver;
                        return PokerState.BetOver;
                    case Bet:
                    case Allin:
                        int tmp2 = players[whoisturn].payMoney();
                        betmoney = tmp2 - betmoney;
                        Log.d("GameModel","Bet : " + whoisturn + " player pay "+ tmp2 + " //BetMoney=" + betmoney);
                        gamemoney += betmoney;
                        state = PokerState.Betting;
                        break;
                    default:
                        Log.d("GameModel", "no input");
                }
                break;
            case Die:break;
        }
        return state;
    }
    public int gameover(PokerState state){
        int winner = 0;
        printGameDesk();
        switch (state){
            case GameOverWell:winner = compareRank(); break;
            case GiveUp:
                for(int i=0;i<NumOfPlayers;i++){
                    if(players[i].getState()!=PersonState.Die)
                        winner = i;
                }
                break;
            default:break;
        }
        players[winner].setMoney(gamemoney);
        gamemoney = 0;
        return winner;
    }

    public static void printGameDesk(){
        System.out.println("GAME_DESK");
        for(int i=0;i<NumOfPlayers;i++){
            System.out.println("===========================");
            System.out.println((i+1)+" player : ");
            System.out.print("   Cards : ");
            players[i].showCard(); System.out.println();
            System.out.print("   Rank : ");
            switch(players[i].getRank()){
                case 0: System.out.println("Top Card");break;
                case 1: System.out.println("One Pair");break;
                case 2: System.out.println("Two Pair");break;
                case 3: System.out.println("Triple");break;
                case 4: System.out.println("Straight");break;
                case 5: System.out.println("Back Straight");break;
                case 6: System.out.println("Mountain");break;
            }
            System.out.print("   Top card : ");
            System.out.println(players[i].getTopCard());
            System.out.print("   Money : ");
            System.out.println(players[i].getMoney());
            System.out.println("===========================");
        }
        System.out.println("GameMoney : " + gamemoney );
        System.out.println("BetMoney : " + betmoney );
        System.out.println();
    }

    public static void CardDistribute(){
        Random random = new Random(); //난수 발생
        for(int i=0;i<NumOfPlayers;i++){
            while(true){
                if(deck.accept(random.nextInt(52))){
                    players[i].setCard(deck.distribute());
                    break;
                }
            }
        }
        cardsNumber++;
        Log.d("GameModel",cardsNumber+" are distributed");
        state = PokerState.BetStart; betmoney = 0;
    }

    public static int compareRank(){
        int big, bigplayer, topcard;
        big = 0; bigplayer = 0; topcard =0;
        for(int i=0;i<NumOfPlayers;i++){
            if(players[i].getRank() >= big){
                if(players[i].getRank() == big){
                    if(players[i].getTopCard() < topcard)
                        continue;
                }
                big = players[i].getRank();
                bigplayer = i;
                topcard = players[i].getTopCard();
            }
        }
        return bigplayer;
    }

    public static int compareCard(int cardidx){
        int bigNum, bigShape, bigPlayer;
        bigNum = 0; bigShape = 0; bigPlayer = 0;
        if(cardidx == 0){	//TopCard를 비교하게 함.

        }
        for(int i=0;i<NumOfPlayers;i++){
            if(players[i].getCardNum(cardidx)>= bigNum){
                if(players[i].getCardNum(cardidx) == bigNum){
                    if(players[i].getCardShape(cardidx)<bigShape)
                        continue;
                }
                bigPlayer = i;
                bigShape = players[i].getCardShape(cardidx);
                bigNum = players[i].getCardNum(cardidx);
            }
        }
        return bigPlayer;
    }

}

enum PokerState{
    Wait(0), BetStart(1), Betting(2), BetOver(3), Distrubute(4), GiveUp(5), GameOverWell(6);
    private final int value;
    private PokerState(int value) {this.value=value;}
    public int value(){return value;}
}

