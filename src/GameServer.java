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
    private String player1Card;
    private String player2Card;
    public static Stack<Card> deck = new Stack<>();

    public GameServer() {
        System.out.println("---- GAME SERVER ----");
        numPlayers = 0;

        initDeck();

        try {
            ss = new ServerSocket(5000);
        } catch (IOException ex) {
            System.out.println("IOException in GameServer constructor.");
            System.out.println(ex.getMessage());
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
            System.out.println(ex.getMessage());
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
                System.out.println("IOException from ServerSideConnection constructor");
                System.out.println(ex.getMessage());
            }
        }

        @Override
        public void run() {
            try {
                dos.writeInt(playerId);
                for (int i = 0; i < 7; ++i) {
                    dos.writeUTF(deck.pop().print());
                }
                dos.flush();

                while (true) {
                    Thread.sleep(2000);
                    if (playerId == 1) {
                        player1Card = dis.readUTF();
                        if (player1Card.equals("DRAW")) {
                            player1.sendCard(deck.pop().print());
                            continue;
                        } else if (player1Card.equals("WINNER")) {
                            player2.sendCard("LOSER");
                            break;
                        }
                        System.out.println("Player 1 played " + player1Card);
                        player2.sendCard(player1Card);
                    } else {
                        player2Card = dis.readUTF();
                        if (player2Card.equals("DRAW")) {
                            player2.sendCard(deck.pop().print());
                            continue;
                        } else if (player2Card.equals("WINNER")) {
                            player1.sendCard("LOSER");
                            break;
                        }
                        System.out.println("Player 2 played " + player2Card);
                        player1.sendCard(player2Card);
                    }
                }
                player1.closeConnection();
                player2.closeConnection();
            } catch (IOException | InterruptedException ex) {
                System.out.println("IOException from run() in ServerSideConnection");
                System.out.println(ex.getMessage());
            }
        }

        public void sendCard(String s) {
            try {
                dos.writeUTF(s);
                dos.flush();
            } catch (IOException ex) {
                System.out.println("IOException from sendCard() in ServerSideConnection");
                System.out.println(ex.getMessage());
            }
        }

        public void closeConnection() {
            try {
                socket.close();
                System.out.println("---- CONNECTION CLOSED ----");
            } catch (IOException ex) {
                System.out.println("IOException from closeConnection() in ServerSideConnection");
                System.out.println(ex.getMessage());
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
