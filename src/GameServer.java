import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Stack;

public class GameServer {
    private ServerSocket ss;
    private int numPlayers;
    private ServerSideConnection player1;
    private ServerSideConnection player2;
    private int turnsMade;
    private int maxTurns;
    private int[] values;
    private int player1ButtonNum;
    private int player2ButtonNum;
    public static Stack<Card> deck = new Stack<>();

    public GameServer() {
        System.out.println("---- GAME SERVER ----");
        numPlayers = 0;
        turnsMade = 0;
        maxTurns = 6;
        values = new int[4];

        for (int i = 0; i < values.length; i++) {
            values[i] = (int) Math.ceil(Math.random() * 100);
            System.out.println("Value #" + (i + 1) + " is " + values[i]);
        }

        try {
            ss = new ServerSocket(5000);
        } catch (IOException ex) {
            System.out.println("IOException in GameServer constructor.");
        }
    }

    public void acceptConnections() {
        try {
            System.out.println("Waiting for connections...");
            do {
                Socket s = ss.accept();
                System.out.println("Player #" + ++numPlayers + " has connected.");
                ServerSideConnection ssc = new ServerSideConnection(s, numPlayers);
                if (numPlayers == 1) {
                    player1 = ssc;
                } else {
                    player2 = ssc;
                }
                Thread t = new Thread(ssc);
                t.start();
            } while (numPlayers < 2);
            System.out.println("We now have 2 players. No longer accepting connections.");
            initDeck();
        } catch (IOException ex) {
            System.out.println("IOException in acceptConnections()");
        }
    }

    private class ServerSideConnection implements Runnable {
        private Socket socket;
        private DataInputStream dis;
        private DataOutputStream dos;
        private int playerId;

        public ServerSideConnection(Socket s, int id) {
            socket = s;
            playerId = id;

            try {
                dis = new DataInputStream(socket.getInputStream());
                dos = new DataOutputStream(socket.getOutputStream());
            } catch (IOException ex) {
                System.out.println("IOException from SSC constructor");
            }
        }

        @Override
        public void run() {
            try {
                dos.writeInt(playerId);
                dos.writeInt(maxTurns);
                dos.writeInt(values[0]);
                dos.writeInt(values[1]);
                dos.writeInt(values[2]);
                dos.writeInt(values[3]);
                dos.flush();

                while (true) {
                    if (playerId == 1) {
                        player1ButtonNum = dis.readInt();
                        System.out.println("Player 1 clicked button #" + player1ButtonNum);
                        player2.sendButtonNum(player1ButtonNum);
                    } else {
                        player2ButtonNum = dis.readInt();
                        System.out.println("Player 2 clicked button #" + player2ButtonNum);
                        player1.sendButtonNum(player2ButtonNum);
                    }
                    ++turnsMade;
                    if (turnsMade == maxTurns) {
                        System.out.println("Max turns has been reached.");
                        break;
                    }
                }
                player1.closeConnection();
                player2.closeConnection();
            } catch (IOException ex) {
                System.out.println("IOException in SSC.run()");
            }
        }

        public void sendButtonNum(int n) {
            try {
                dos.writeInt(n);
                dos.flush();
            } catch (IOException ex) {
                System.out.println("IOException from sendButtonNum() ssc");
            }
        }

        public void closeConnection() {
            try {
                socket.close();
                System.out.println("---- CONNECTION CLOSED ----");
            } catch (IOException ex) {
                System.out.println("IOException on closeConnection() ssc");
            }
        }
    }

    private static void initDeck() {
        System.out.println("Initializing Deck...");
        for (CardColor color : CardColor.values()) {
            deck.addAll(generateColorSet(color));
        }
        Collections.shuffle(deck);
    }

    private static ArrayList<Card> generateColorSet(CardColor color) {
        final int COMMON_CARD_COUNT = 2;
        final int SPECIAL_CARD_COUNT = 4;

        ArrayList<Card> colorSet = new ArrayList<Card>();

        if (color == CardColor.WILD)
        {
            for (int i = 0; i < SPECIAL_CARD_COUNT; ++i) {
                colorSet.add(new Card(color, CardValue.DRAW_FOUR));
                colorSet.add(new Card(color, CardValue.EMPTY));
            }
            return colorSet;
        }

        for (CardValue value : CardValue.values()) {
            switch (value) {
                case ONE:
                case TWO:
                case THREE:
                case FOUR:
                case FIVE:
                case SIX:
                case SEVEN:
                case EIGHT:
                case NINE:
                case DRAW_TWO:
                case REVERSE:
                case SKIP:
                    for (int i = 0; i < COMMON_CARD_COUNT; ++i) {
                        colorSet.add(new Card(color, value));
                    }
                    break;
                case ZERO:
                    colorSet.add(new Card(color, value));
                    break;
                default:
                    break;
            }
        }

        return colorSet;
    }

    public static void main(String[] args) {
        GameServer gs = new GameServer();
        gs.acceptConnections();
    }
}
