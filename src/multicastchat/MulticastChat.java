package multicastchat;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

public class MulticastChat extends JFrame implements ActionListener {
    private MulticastSocket socket;
    private InetAddress group;
    private JTextField messageField;
    private JTextArea chatArea;
    private JButton sendButton;
    private String username;
    private String userID;
    private boolean isLeader;
    private List<String> members;

    public MulticastChat(String username, String userID) {
        super("Multicast Chat - " + username + " (" + userID + ")");
        this.username = username;
        this.userID = userID;
        this.isLeader = false;
        this.members = new ArrayList<>();

        messageField = new JTextField();
        chatArea = new JTextArea();
        sendButton = new JButton("Enviar");
        sendButton.addActionListener(this);

        JScrollPane chatScrollPane = new JScrollPane(chatArea);
        chatScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        chatScrollPane.setPreferredSize(new Dimension(400, 250));

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(chatScrollPane, BorderLayout.CENTER);

        JPanel messagePanel = new JPanel(new BorderLayout());
        messagePanel.add(messageField, BorderLayout.CENTER);
        messagePanel.add(sendButton, BorderLayout.EAST);

        panel.add(messagePanel, BorderLayout.SOUTH);

        setContentPane(panel);
        setSize(400, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);

        try {
            socket = new MulticastSocket(6789);
            group = InetAddress.getByName("230.0.0.1");
            socket.joinGroup(group);

            new Thread(new ReceiverThread()).start();

            new Thread(new TransmitterConnectionThread()).start();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == sendButton) {
            try {
                String message = userID + " - " + username + ": " + messageField.getText() + "\n";
                byte[] buffer = message.getBytes();
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length, group, 6789);
                socket.send(packet);

                messageField.setText("");
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    private class ReceiverThread implements Runnable {
        public void run() {
            try {
                while (true) {
                    byte[] buffer = new byte[1000];
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);

                    String message = new String(packet.getData(), 0, packet.getLength());
                    String[] parts = message.split(" - ");
                    if (parts.length >= 2) {
                        String receivedUserID = parts[0];
                        String receivedMessage = parts[1];
                        chatArea.append(receivedUserID + " - " + receivedMessage);

                        if (!members.contains(receivedUserID)) {
                            members.add(receivedUserID);
                        }
                    }
                    updateLeader();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    private class TransmitterConnectionThread implements Runnable {
        public void run() {
            Timer timer = new Timer();
            timer.schedule(new ConnectTransmitterTask(), 5000);
        }
    }

    private class ConnectTransmitterTask extends TimerTask {
        public void run() {
            try {
                for (String member : members) {
                    if (!member.equals(userID)) {
                        String connectMessage = "CONNECT - " + userID;
                        byte[] buffer = connectMessage.getBytes();
                        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, group, 6789);
                        socket.send(packet);
                    }
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            updateLeader();
        }
    }

    private void updateLeader() {
        
        String currentLeader = getLeader();
        if (currentLeader.equals(userID)) {
            isLeader = true;
            setTitle("Multicast Chat - " + username + " (" + userID + ") [Líder]");
        } else {
            isLeader = false;
            setTitle("Multicast Chat - " + username + " (" + userID + ")");
        }
    }

    private String getLeader() {
        String leader = userID;
        for (String member : members) {
            if ( Integer.parseInt(member) > Integer.parseInt(userID)) {
                leader = member;
            }
        }
        return leader;
    }

    public static void main(String[] args) {
        String username = JOptionPane.showInputDialog("Digite seu nome de usuário:");
        String userID = JOptionPane.showInputDialog("Digite seu ID:");
        new MulticastChat(username, userID);
    }
}
