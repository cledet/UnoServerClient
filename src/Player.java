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
    private int[] values;
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
        b1 = new JButton("1");
        b2 = new JButton("2");
        b3 = new JButton("3");
        b4 = new JButton("4");
        b5 = new JButton("5");
        b6 = new JButton("6");
        b7 = new JButton("7");
        values = new int[4];
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
        // contentPane.add(b1);
        // contentPane.add(b2);
        // contentPane.add(b3);
        // contentPane.add(b4);
        // contentPane.add(b5);
        // contentPane.add(b6);
        // contentPane.add(b7);

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
            int bNum = Integer.parseInt(b.getText());

            message.setText("You clicked button #" + bNum + ". Now wait for player #" + otherPlayer);
            ++turnsMade;
            System.out.println("Turns made: " + turnsMade);

            buttonsEnabled = false;
            toggleButtons();

            myPoints += values[bNum - 1];
            System.out.println("My points: " + myPoints);

            csc.sendButtonNum(bNum);
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

        // b1.addActionListener(al);
        // b2.addActionListener(al);
        // b3.addActionListener(al);
        // b4.addActionListener(al);
    }

    public void toggleButtons() {
        b1.setEnabled(buttonsEnabled);
        b2.setEnabled(buttonsEnabled);
        b3.setEnabled(buttonsEnabled);
        b4.setEnabled(buttonsEnabled);
    }

    public void updateTurn() {
        int n = csc.receiveButtonNum();
        message.setText("Your enemy clicked button #" + n + ". Your turn.");
        enemyPoints += values[n -1];
        System.out.println("Your enemy has " + enemyPoints + " points.");
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
                maxTurns = dis.readInt() / 2;
                values[0] = dis.readInt();
                values[1] = dis.readInt();
                values[2] = dis.readInt();
                values[3] = dis.readInt();
                System.out.println("maxTurns: " + maxTurns);
                System.out.println("Value #1 is " + values[0]);
                System.out.println("Value #2 is " + values[1]);
                System.out.println("Value #3 is " + values[2]);
                System.out.println("Value #4 is " + values[3]);
            } catch (IOException ex) {
                System.out.println("IOException from CSC constructor.");
            }
        }

        public void sendButtonNum(int n) {
            try {
                dos.writeInt(n);
                dos.flush();
            } catch (IOException ex) {
                System.out.println("IOException from sendButtonNum() CSC");
            }
        }

        public int receiveButtonNum() {
            int n = -1;
            try {
                n = dis.readInt();
                System.out.println("Player #" + otherPlayer + " clicked button #" + n);
            } catch (IOException ex) {
                System.out.println("IOException from receiveButtonNum() csc");
            }
            return n;
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
        Player p = new Player(500, 125);
        p.connectToServer();
        p.setupGUI();
        p.setupButtons();
    }
}
