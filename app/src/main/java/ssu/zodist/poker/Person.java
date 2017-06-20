package ssu.zodist.poker;
import android.util.Log;

import java.io.Serializable;
import java.util.Arrays;

public class Person implements Serializable{
    private int HowManyCardToget = 7;
    //private boolean state;			// Die 상태 판별 변수
    private PersonState state;
    private int money;			// 총 보유 돈
    private int betmoney;
    private int numOfRcvCard;	// 받은 카드 갯수(인덱스 개념->(+1 해야 물리적))
    private int Cards[][]	= new int[HowManyCardToget][2];// 받을 5장의 카드
    private int rank;
    private int TopCard;	// 족보 또는 탑카드에서 가장 높은 수
    private int rankNumArr[] = new int[15];	//족보 판별 숫자 배열
    private int rankShapeArr[] = new int[5]; //족보 판별 모양 배열

    public Person(){
        state = PersonState.Wait;
        money = 1000;
        betmoney = 10;		//초기 시작
        resetCards();
    }

    public PersonState accept(char ch,int peerbetmoney, int gamemoney){
        switch(ch){
            case 'k': do_check(); break;
            case 'c': do_call(peerbetmoney); break;
            case 'd': do_die();	break;
            case 'h': do_half(peerbetmoney, gamemoney); break;
            case 'q': do_quartor(peerbetmoney, gamemoney); break;
        }
        return state;
    }

    //_act_Kind==========================================
    public void do_check(){
        betmoney = 0;
        //체크할 때는 상태 변화가 없다.
    }
    public void do_die(){
        betmoney = 0;
        state = PersonState.Die;
    }
    public void do_call(int peerbetmoney){
        betmoney = peerbetmoney;
        state = PersonState.Call;
    }
    public void do_half(int peerbetmoney,int gamemoney){
        betmoney = peerbetmoney + gamemoney/2;
        state = PersonState.Bet;
    }
    public void do_quartor(int peerbetmoney,int gamemoney){
        betmoney = peerbetmoney + gamemoney/4;
        state = PersonState.Bet;
    }
    //_act_Kind==========================================
    public void resetCards(){
        betmoney = 10;
        numOfRcvCard = 0;
        Cards = new int[HowManyCardToget][2];
        rankNumArr = new int[15];
        rankShapeArr = new int[5];
        Arrays.fill(rankNumArr, 0);
        for(int i=0;i<HowManyCardToget;i++) {
            Arrays.fill(Cards[i], -1);
        }
    }

    public PersonState getState(){ return state; }
    public int getMoney(){ return money; }
    public int[][] getCards(){ return Cards; }
    public int getCardNum(int idx){ return Cards[idx][1]; }
    public int getCardShape(int idx){ return Cards[idx][0]; }
    public int getRank(){ return rank; }
    public int getTopCard(){ return TopCard;}
    public int payMoney() {
        if(money > betmoney){
            money -= betmoney;
        }else{
            money = 0;
            state = PersonState.Allin;
        }
        return betmoney;
    }
    public void setMoney(int amoney){ money+=amoney; }
    public void setCard(int card){
        int tmp = 0;
        if(card >= 1 && card <= 13){
            Cards[numOfRcvCard][0] = 4;tmp = card;		// 스페이드
            rankShapeArr[4]++;
        }else if(card <= 26){
            Cards[numOfRcvCard][0] = 3;tmp = card-13;	// 다이아몬드
            rankShapeArr[3]++;
        }else if(card <= 39){
            Cards[numOfRcvCard][0] = 2;tmp = card-26;	// 하트
            rankShapeArr[2]++;
        }else if(card <= 52){
            Cards[numOfRcvCard][0] = 1;tmp = card-39;	// 클로버
            rankShapeArr[1]++;
        }
        Cards[numOfRcvCard][1] = tmp;
        rankNumArr[tmp]++;
        judgementRank();
        numOfRcvCard++;
    }
    public void showCard(){
        for(int i=0;i<numOfRcvCard;i++){
            switch(Cards[i][0]){
                case 4:System.out.print("♠");break;
                case 3:System.out.print("◈");break;
                case 2:System.out.print("♥");break;
                case 1:System.out.print("♣");break;
            }
            switch(Cards[i][1]){
                case 1:	System.out.print("A"); break;
                case 11:System.out.print("J"); break;
                case 12:System.out.print("Q"); break;
                case 13:System.out.print("K"); break;
                default:System.out.print(Cards[i][1]); break;
            }
            System.out.print(" ");
        }
    }
    public void judgementRank(){
		/*
		 Atleast 5card	Royal Straight flush = 12;
		 				Back Straight flush = 11;
		 				Straight flush = 10;
		 Atleast 4card	Four Card = 9;	!Done!
		 Atleast 5card	Full House = 8;	!Done!
		 				Flush = 7;    	!Done!
		 				Mountain = 6;	!Done!
		 				Back Straight = 5;!Done!
		 				Straight = 4; 	!Done!
		 Atleast 3card	Triple = 3;	  	!Done!
		 Atleast 4card	Two Pair = 2; 	!Done!
		 Atleast 2card	One Pair = 1; 	!Done!
		 Top Card = 0;
		 */
        if(numOfRcvCard+1 >= 2){ // 원 페어 검사
            for(int i=14;i>=1;i--){
                if(rankNumArr[i]==2){
                    rank = 1;
                    TopCard = i;
                }
            }
        }
        if(numOfRcvCard+1 >= 3){	// 트리플 검사
            for(int i=14;i>=1;i--){
                if(rankNumArr[i]==3){
                    rank = 3;	TopCard = i;
                }
            }
        }
        if(numOfRcvCard+1 >= 4){	// 포카드, 투 페어 검사
            for(int i=14;i>=1;i--){	// 포카드
                if(rankNumArr[i]==4){
                    rank = 9;	TopCard = i;
                }
            }
            if(rank != 9){ // 포카드일 때 투 페어 검사 필요 없음
                int countPair=0;
                int bigcard=0;
                for(int i=14;i>=1;i--){
                    if(rankNumArr[i]==2){
                        if(countPair==0) bigcard = i;
                        countPair++;
                    }
                    if(countPair == 2){
                        rank = 2; TopCard = bigcard;
                        break;
                    }
                }
            }
        }
        if(numOfRcvCard+1 >= 5){	// 그 외 검사
            int tmpsum;
            for(int i=14;i>=1;i--){ // Full House 판별
                int bigcard = 0;
                boolean tripleT = false, twoT = false;
                if(rankNumArr[i]==3){
                    tripleT = true;	bigcard = i;
                }
                if(rankNumArr[i]==2){
                    twoT = true;
                }
                if(tripleT && twoT){
                    rank = 8; TopCard = bigcard;
                }
            }

            for(int i=14;i>=1;i--){	// straight 판별
                tmpsum = 0;
                if(rankNumArr[i]>=1){
                    for(int j=i+1;j<14;j++){
                        if(rankNumArr[j]>=1)
                            tmpsum++;
                        else
                            break;
                        if( i==14 && tmpsum==3 // Mountain 판별
                                && (rankNumArr[1]>=1)){
                            rank = 6; TopCard = i;
                        }
                        else if(tmpsum>=4){
                            if(i==5){	//back straight 판별
                                rank = 5; TopCard = i;
                            }
                            rank = 4;	TopCard = i;
                        }
                    }
                }
            }

            for(int i=4;i>=1;i--){	// flush 판별
                if(rankShapeArr[i]>=5){
                    rank = 7;
                    int bigcard=0;
                    for(int j=0;j<7;j++){
                        if(Cards[j][1]==i && Cards[j][0] > bigcard){
                            bigcard = Cards[j][0];
                        }
                    }
                    TopCard = bigcard;
                }
            }
        }
    }
}

enum PersonState{
    Wait(0), Bet(1), Call(2), Die(3), Allin(4);
    private final int value;
    private PersonState(int value) {this.value=value;}
    public int value(){return value;}
}

