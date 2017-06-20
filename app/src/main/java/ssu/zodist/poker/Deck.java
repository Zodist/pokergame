package ssu.zodist.poker;
import java.io.Serializable;
import java.util.Arrays;

public class Deck implements Serializable{
    private static boolean cards[] = new boolean[53];
    private static int pickCard;
    public Deck(){
        cards = new boolean[53];
        Arrays.fill(cards, true);
        shuffle();
    }
    public static boolean accept(int x){	//난수 받기
        if(x==0 || cards[x]==false) return false;
        else{
            cards[x] = false;
            pickCard = x;
            return true;
        }
    }
    public static int distribute(){	// 분배 함수
        return pickCard;
    }
    public static void shuffle(){	// 섞기 함수
        Arrays.fill(cards, true);
    }
}
