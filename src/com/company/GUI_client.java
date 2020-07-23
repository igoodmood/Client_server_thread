package com.company;

import javax.swing.*;
import javax.swing.text.DefaultCaret;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Random;

class StopWatch extends JFrame {
    private static Client[] clients = new Client[1000];
    private static JTextArea log = new JTextArea();
    private static JList<String> l = new JList<>();
    private static JScrollPane scrollPane = new JScrollPane(l);
    private static JScrollPane scrollPane2 = new JScrollPane(log);
    private static DefaultListModel<String> model = new DefaultListModel<String>();
    private JLabel name_users, logs;
    public static JButton one_thread = new JButton("Один поток");
    public static JButton several_thread;
    private static JProgressBar aJProgressBar = new JProgressBar(0, 1000);
    private JButton connect;

    void setClients(Client[] clients) {
        this.clients = clients;
    }

    public void init() throws InterruptedException {
        several_thread = new JButton("Несколько");
        DefaultCaret caret = (DefaultCaret) log.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
        l.setModel(model);
        JFrame jfrm = new JFrame("Client's form");
        jfrm.getContentPane().setLayout(null);
        jfrm.setSize(725, 500);
        jfrm.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        connect = new JButton("Подключить");
        one_thread.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Generate generate = new Generate();
                generate.start();
            }
        });
        name_users = new JLabel("Список клиентов");
        logs = new JLabel("Лог сервера");
        one_thread.setBounds(5, 380, 120, 50);
        connect.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                GenerateNumber generateNumber = new GenerateNumber();
                generateNumber.start();
            }
        });
        several_thread.setBounds(150, 380, 120, 50);
        connect.setBounds(300, 5, 120, 20);
        name_users.setBounds(5, 10, 100, 10);
        scrollPane.setBounds(5, 30, 100, 345);
        scrollPane2.setBounds(110, 30, 600, 345);
        logs.setBounds(635, 8, 100, 15);
        aJProgressBar.setBounds(5, 450, 700, 30);
        jfrm.getContentPane().add(connect);
        Container contentPane = jfrm.getContentPane();
        contentPane.add(scrollPane, BorderLayout.CENTER);
        jfrm.getContentPane().add(name_users);
        jfrm.getContentPane().add(logs);
        jfrm.getContentPane().add(one_thread);
        jfrm.getContentPane().add(several_thread);
        jfrm.getContentPane().add(scrollPane2);
        jfrm.getContentPane().add(aJProgressBar);
        jfrm.setVisible(true);
        StartServer startServer = new StartServer();
        startServer.start();
    }

    public void addeleModel(String elem) {
        model.addElement(elem);
        System.out.println(Thread.currentThread().getName());
        setModeler(model);
    }

    public void setModeler(DefaultListModel<String> modelt) {
        l.setModel(modelt);
    }

    public void addPlainTexter(String str) {
        log.append(str + "\n");
    }

    public Client[] getClients() {
        return clients.clone();
    }

    public static void main(String args[]) {
        JFrame.setDefaultLookAndFeelDecorated(true);
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                StopWatch stopWatch = new StopWatch();
                try {
                    stopWatch.init();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}

class GenerateNumber extends Thread {
    private StopWatch stopWatch = new StopWatch();
    private Client[] clientser = stopWatch.getClients();

    @Override
    public void run() {
        for (int i = 0; i < 1000; i++) {
            clientser[i] = new Client("localhost", 1500, "user" + i);
            clientser[i].start();
        }
        stopWatch.setClients(clientser);
    }
}

class StartServer extends Thread {
    @Override
    public void run() {
        try {
            Server server = new Server(1500);
            server.start();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}