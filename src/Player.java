import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.awt.event.*;
import java.util.ArrayList;

public class Player extends JFrame {
    private int width;
    private int height;
    private Container contentPane;
    private JTextArea message;
    private ArrayList<JButton> hand = new ArrayList<>();
    private JButton b1;
    private JButton b2;
    private JButton b3;
    private JButton b4;
    private JButton b5;
    private JButton b6;
    private JButton b7;
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
        for (int i = 0; i < 7; ++i) {
            hand.add(new JButton(String.format("%d", i + 1)));
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
        for (JButton jButton : hand) {
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
            String card = b.getText();

            message.setText("You played " + card + ". Now wait for player #" + otherPlayer);
            ++turnsMade;
            System.out.println("Turns made: " + turnsMade);

            buttonsEnabled = false;
            toggleButtons();

            csc.sendCard(card);
            contentPane.remove(b);
            contentPane.revalidate();
            contentPane.repaint();
            if (playerId == 2 && turnsMade == maxTurns) {
                checkWinner();
            } else {
                Thread t = new Thread(() -> updateTurn());
                t.start();
            }
        };

        for (JButton jButton : hand) {
            jButton.addActionListener(al);
        }
    }

    public void toggleButtons() {
        for (JButton jButton : hand) {
            jButton.setEnabled(buttonsEnabled);
        }
    }

    public void updateTurn() {
        String s = csc.receiveCard();
        message.setText("Your enemy played " + s + ". Your turn.");
        if (playerId == 1 && turnsMade == maxTurns) {
            checkWinner();
        } else {
            buttonsEnabled = true;
        }
        toggleButtons();
    }

    private void checkWinner() {
        buttonsEnabled = false;
        if (myPoints > enemyPoints) {
            message.setText("You WON!\n" + "YOU: " + myPoints + "\nENEMY" + enemyPoints);
        } else if (myPoints < enemyPoints) {
            message.setText("You LOSE!\n" + "YOU: " + myPoints + "\nENEMY" + enemyPoints);
        } else {
            message.setText("It's a TIE!\n" + "You both got: " + myPoints + " points.");
        }

        csc.closeConnection();
    }

    // Client Connection Inner Class
    private class ClientSideConnection {
        private Socket s;
        private DataInputStream dis;
        private DataOutputStream dos;

        public ClientSideConnection() {
            System.out.println("---- Client ----");
            try {
                s = new Socket("localhost", 5000);
                dis = new DataInputStream(s.getInputStream());
                dos = new DataOutputStream(s.getOutputStream());
                playerId = dis.readInt();
                System.out.println("Connected to server as Player #" + playerId + ".");
                values.add(dis.readUTF());
                values.add(dis.readUTF());
                values.add(dis.readUTF());
                values.add(dis.readUTF());
                values.add(dis.readUTF());
                values.add(dis.readUTF());
                values.add(dis.readUTF());
                System.out.println("Value #1 is " + values.get(0));
                System.out.println("Value #2 is " + values.get(1));
                System.out.println("Value #3 is " + values.get(2));
                System.out.println("Value #4 is " + values.get(3));
                System.out.println("Value #5 is " + values.get(4));
                System.out.println("Value #6 is " + values.get(5));
                System.out.println("Value #7 is " + values.get(6));
                for (int i = 0; i < 7; ++i) {
                    hand.set(i, new JButton(values.get(i)));
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

        public String receiveCard() {
            String s = "";
            try {
                s = dis.readUTF();
                System.out.println("Player #" + otherPlayer + " played " + s);
            } catch (IOException ex) {
                System.out.println("IOException from receiveButtonNum() csc");
            }
            return s;
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
