import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

public class Player extends JFrame {
    private int width;
    private int height;
    private Container contentPane;
    private JTextArea message;
    private ArrayList<JButton> buttons = new ArrayList<>();
    private ArrayList<Card> hand = new ArrayList<>();
    private int playerId;
    private int otherPlayer;
    private ArrayList<String> values;
    private int maxTurns;
    private int turnsMade;
    private int myPoints;
    private int enemyPoints;
    private boolean buttonsEnabled;

    private ClientSideConnection csc;

    public Player(int w, int h) {
        width = w;
        height = h;
        contentPane = this.getContentPane();
        message = new JTextArea();
        for (int i = 0; i < 8; ++i) {
            buttons.add(new JButton(String.format("%d", i + 1)));
        }
        values = new ArrayList<>();
        maxTurns = 0;
        turnsMade = 0;
        myPoints = 0;
        enemyPoints = 0;
    }

    public void setupGUI() {
        this.setSize(width, height);
        this.setTitle("Player #" + playerId);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        contentPane.setLayout(new GridLayout(1, 8));
        contentPane.add(message);
        message.setText("UNO game in Java using ServerClient");
        message.setWrapStyleWord(true);
        message.setLineWrap(true);
        message.setEditable(false);
        for (JButton jButton : buttons) {
            contentPane.add(jButton);
        }

        if (playerId == 1) {
            message.setText("You are player #1. You go first.");
            otherPlayer = 2;
            buttonsEnabled = true;
        } else {
            message.setText("You are player #2. Wait for your turn.");
            otherPlayer = 1;
            buttonsEnabled = false;
            Thread t = new Thread(() -> updateTurn());
            t.start();
        }

        toggleButtons();

        this.setVisible(true);
    }

    public void connectToServer() {
        csc = new ClientSideConnection();
    }

    public void setupButtons() {
        ActionListener al = e -> {
            JButton b = (JButton) e.getSource();
            String cardString = b.getText();

            if (cardString.equals("DRAW")) {
                Card holdCard = csc.receivedCard;
                do {
                    csc.sendCard(b.getText());
                    Card card = csc.receiveCard();
                    hand.add(card);
                    JButton jButton = new JButton(card.print());
                    if (getPlayableCards(holdCard).size() == 0)
                    {
                        jButton.setEnabled(false);
                    }
                    buttons.add(jButton);
                    contentPane.add(jButton);
                    contentPane.validate();
                    contentPane.repaint();
                } while (getPlayableCards(holdCard).size() == 0);
                addActionListener();
            } else {
                message.setText("You played " + cardString + ". Now wait for player #" + otherPlayer);
                ++turnsMade;
                System.out.println("Turns made: " + turnsMade);

                buttonsEnabled = false;
                toggleButtons();

                csc.sendCard(cardString);
                contentPane.remove(b);
                contentPane.revalidate();
                contentPane.repaint();
                try {
                    hand.remove(hand.stream().filter(x -> x.print().equals(cardString)).findFirst().get());
                } catch (Exception ex){
                    System.out.println("Card String = " + cardString);
                    for (int i = 0; i < hand.size(); ++i) {
                        System.out.println("#" + i + ": " + hand.get(i).print());
                    }
                }
                if (hand.size() == 0) {
                    message.setText("YOU WIN!!");
                    csc.sendCard("WINNER");
                    csc.closeConnection();
                } else {
                    Thread t = new Thread(() -> updateTurn());
                    t.start();
                }
            }
        };

        for (JButton jButton : buttons) {
            jButton.addActionListener(al);
        }
    }

    private void addActionListener() {
        setupButtons();
    }

    public void toggleButtons() {
        for (JButton jButton : buttons) {
            jButton.setEnabled(buttonsEnabled);
        }
    }

    public void updateTurn() {
        Card c = csc.receiveCard();
        if (hand.size() == 0) {
            message.setText("YOU WIN!!");
            csc.sendCard("WINNER");
            csc.closeConnection();
        } else if (c.equals("LOSER")) {
            message.setText("YOU LOSE!!");
            csc.closeConnection();
        }
        else {
            message.setText("Your enemy played " + c.print() + ". Your turn.");
            List<Card> playableCards = getPlayableCards(c);
            if (playableCards.size() == 0) {
                buttons.get(0).setEnabled(true);
            } else {
                List<JButton> buttonsToEnable = new ArrayList<>();
                for (Card card : playableCards) {
                    buttonsToEnable.addAll(buttons.stream().filter(b -> b.getText().equals(card.print())).toList());
                }
                for (JButton button : buttonsToEnable) {
                    button.setEnabled(true);
                }
            }
        }
    }

    public List<Card> getPlayableCards (Card card) {
        return this.hand.stream().filter(c ->
                c.getColor() == card.getColor()
                        || c.getValue() == card.getValue()
                        || c.getColor() == CardColor.WILD
                        || card.getColor() == CardColor.WILD).toList();
    }

    public Card convertStringToCard(String cardString) {
        String color = cardString.split("\s+")[0];
        CardColor cardColor;
        switch (color) {
            case "BLUE":
                cardColor = CardColor.BLUE;
                break;
            case "GREEN":
                cardColor = CardColor.GREEN;
                break;
            case "RED":
                cardColor = CardColor.RED;
                break;
            case "YELLOW":
                cardColor = CardColor.YELLOW;
                break;
            default:
                cardColor = CardColor.WILD;
                break;
        }

        CardValue cardValue = CardValue.EMPTY;
        if (cardString.split("\s").length > 1) {
            switch (cardString.split("\s")[1]) {
                case "ZERO":
                    cardValue = CardValue.ZERO;
                    break;
                case "ONE":
                    cardValue = CardValue.ONE;
                    break;
                case "TWO":
                    cardValue = CardValue.TWO;
                    break;
                case "THREE":
                    cardValue = CardValue.THREE;
                    break;
                case "FOUR":
                    cardValue = CardValue.FOUR;
                    break;
                case "FIVE":
                    cardValue = CardValue.FIVE;
                    break;
                case "SIX":
                    cardValue = CardValue.SIX;
                    break;
                case "SEVEN":
                    cardValue = CardValue.SEVEN;
                    break;
                case "EIGHT":
                    cardValue = CardValue.EIGHT;
                    break;
                case "NINE":
                    cardValue = CardValue.NINE;
                    break;
                case "SKIP":
                    cardValue = CardValue.SKIP;
                    break;
                case "REVERSE":
                    cardValue = CardValue.REVERSE;
                    break;
                case "DRAW_FOUR":
                    cardValue = CardValue.DRAW_FOUR;
                    break;
                case "DRAW_TWO":
                    cardValue = CardValue.DRAW_TWO;
                    break;
            }
        }
        return new Card(cardColor, cardValue);
    }

    // Client Connection Inner Class
    private class ClientSideConnection {
        private Socket s;
        private DataInputStream dis;
        private DataOutputStream dos;
        public Card receivedCard;

        public ClientSideConnection() {
            System.out.println("---- Client ----");
            try {
                s = new Socket("localhost", 5000);
                dis = new DataInputStream(s.getInputStream());
                dos = new DataOutputStream(s.getOutputStream());
                playerId = dis.readInt();
                System.out.println("Connected to server as Player #" + playerId + ".");
                hand.add(convertStringToCard(dis.readUTF()));
                hand.add(convertStringToCard(dis.readUTF()));
                hand.add(convertStringToCard(dis.readUTF()));
                hand.add(convertStringToCard(dis.readUTF()));
                hand.add(convertStringToCard(dis.readUTF()));
                hand.add(convertStringToCard(dis.readUTF()));
                hand.add(convertStringToCard(dis.readUTF()));
                System.out.println("Value #1 is " + hand.get(0).print());
                System.out.println("Value #2 is " + hand.get(1).print());
                System.out.println("Value #3 is " + hand.get(2).print());
                System.out.println("Value #4 is " + hand.get(3).print());
                System.out.println("Value #5 is " + hand.get(4).print());
                System.out.println("Value #6 is " + hand.get(5).print());
                System.out.println("Value #7 is " + hand.get(6).print());
                buttons.set(0, new JButton("DRAW"));
                for (int i = 0; i < 7; ++i) {
                    buttons.set(i + 1, new JButton(hand.get(i).print()));
                }
            } catch (IOException ex) {
                System.out.println("IOException from CSC constructor.");
            }
        }

        public void sendCard(String s) {
            try {
                dos.writeUTF(s);
                dos.flush();
            } catch (IOException ex) {
                System.out.println("IOException from sendButtonNum() CSC");
            }
        }

        public Card receiveCard() {
            Card c = null;
            try {
                String cardString = dis.readUTF();
                c = convertStringToCard(cardString);
                System.out.println("Player #" + otherPlayer + " played " + c.print());
            } catch (IOException ex) {
                System.out.println("IOException from receiveButtonNum() csc");
            }
            receivedCard = c;
            return c;
        }

        public void closeConnection() {
            try {
                s.close();
                System.out.println("---- CONNECTION CLOSED ----");
            } catch (IOException ex) {
                System.out.println("IOException on closeConnection() csc");
            }
        }
    }

    public static void main(String[] args) {
        Player p = new Player(800, 125);
        p.connectToServer();
        p.setupGUI();
        p.setupButtons();
    }
}
