package com.company;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;


public class Server {
    private Map<String, String> nums;
    private StopWatch stopWatch = new StopWatch();
    private Users_data[] usersData = new Users_data[100];
    private static int uniqueId;
    private ArrayList<ClientThread> al;
    private ArrayList<String> tasks;
    private SimpleDateFormat sdf;
    private int port;
    private boolean keepGoing;
    private String notif = " *** ";


    public Server(int port) throws InterruptedException {
        this.port = port;
        sdf = new SimpleDateFormat("HH:mm:ss");
        al = new ArrayList<ClientThread>();
    }

    public ArrayList<ClientThread> getList() {
        return al;
    }

    public void start() throws InterruptedException {
        nums = new HashMap<>();
        keepGoing = true;
        try {
            ServerSocket serverSocket = new ServerSocket(port);

            while (keepGoing) {
                display("Server waiting for Clients on port " + port + ".");

                Socket socket = serverSocket.accept();
                if (!keepGoing)
                    break;
                ClientThread t = new ClientThread(socket);
                al.add(t);

                t.start();
            }
            try {
                serverSocket.close();
                for (int i = 0; i < al.size(); ++i) {
                    ClientThread tc = al.get(i);
                    try {
                        tc.sInput.close();
                        tc.sOutput.close();
                        tc.socket.close();
                    } catch (IOException ioE) {
                    }
                }
            } catch (Exception e) {
                display("Exception closing the server and clients: " + e);
            }
        } catch (IOException e) {
            String msg = sdf.format(new Date()) + " Exception on new ServerSocket: " + e + "\n";
            display(msg);
        }
        System.out.println("test");
        for (int i = 0; i < al.size(); i++) {
            System.out.println(i);
            al.get(i).join();
        }
    }

    protected void stop() {
        keepGoing = false;
        try {
            new Socket("localhost", port);
        } catch (Exception e) {
        }
    }

    private void display(String msg) {
        String time = sdf.format(new Date()) + " " + msg;
        System.out.println(time);
    }

    private synchronized boolean broadcast(String message) {
        String time = sdf.format(new Date());

        String[] w = message.split(" ", 3);

        boolean isPrivate = false;
        if (w[1].charAt(0) == '@')
            isPrivate = true;


        if (isPrivate == true) {
            String tocheck = w[1].substring(1);

            message = w[0] + w[2];
            String messageLf = time + " " + message + "\n";
            boolean found = false;
            for (int y = al.size(); --y >= 0; ) {
                ClientThread ct1 = al.get(y);
                String check = ct1.getUsername();
                if (check.equals(tocheck)) {
                    if (!ct1.writeMsg(messageLf)) {
                        al.remove(y);
                        display("Disconnected Client " + ct1.username + " removed from list.");
                    }
                    found = true;
                    break;
                }


            }
            return found == true;
        }
        else {
            String messageLf = time + " " + message + "\n";
            System.out.print(messageLf);

            for (int i = al.size(); --i >= 0; ) {
                ClientThread ct = al.get(i);
                if (!ct.writeMsg(messageLf)) {
                    al.remove(i);
                    display("Disconnected Client " + ct.username + " removed from list.");
                }
            }
        }
        return true;


    }

    synchronized void remove(int id) {

        String disconnectedClient = "";
        for (int i = 0; i < al.size(); ++i) {
            ClientThread ct = al.get(i);
            if (ct.id == id) {
                disconnectedClient = ct.getUsername();
                al.remove(i);
                break;
            }
        }
        broadcast(notif + disconnectedClient + " has left the chat room." + notif);
    }

    public static void main(String[] args) throws InterruptedException {
        int portNumber = 1500;
        switch (args.length) {
            case 1:
                try {
                    portNumber = Integer.parseInt(args[0]);
                } catch (Exception e) {
                    System.out.println("Invalid port number.");
                    System.out.println("Usage is: > java Server [portNumber]");
                    return;
                }
            case 0:
                break;
            default:
                System.out.println("Usage is: > java Server [portNumber]");
                return;

        }
        Server server = new Server(1500);
        server.start();
    }

    class ClientThread extends Thread {
        private Instant starts = Instant.ofEpochSecond(0);
        Socket socket;
        ObjectInputStream sInput;
        ObjectOutputStream sOutput;
        int id;
        String username;
        ChatMessage cm;
        String date;

        ClientThread(Socket socket) throws InterruptedException {
            id = ++uniqueId;
            this.socket = socket;
            try {
                date = new Date().toString() + "\n";
                sOutput = new ObjectOutputStream(socket.getOutputStream());
                sInput = new ObjectInputStream(socket.getInputStream());
                username = (String) sInput.readObject();
                stopWatch.addeleModel(username);
                stopWatch.addPlainTexter(" > " + sdf.format(new Date()) + " " + username + " подключился к серверу.");
            } catch (IOException e) {
                display("Exception creating new Input/output Streams: " + e);
                return;
            } catch (ClassNotFoundException e) {
            }
            date = new Date().toString() + "\n";
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public void run() {
            boolean keepGoing = true;
            while (keepGoing) {
                try {
                    cm = (ChatMessage) sInput.readObject();
                } catch (IOException e) {
                    display(username + " Exception reading Streams: " + e);
                    break;
                } catch (ClassNotFoundException e2) {
                    break;
                }
                String message = cm.getMessage();
                if (message.toLowerCase().equals("logout")) {
                    display(username + " disconnected with a LOGOUT message.");
                    keepGoing = false;
                } else {
                    nums.put(username, message);
                    if (nums.size() == 1000) {
                        calculator_one_thread();
                        calculator_several_thread();
                    }
                }
            }
            remove(id);
            close();
        }

        private void close() {
            try {
                if (sOutput != null) sOutput.close();
            } catch (Exception e) {
            }
            try {
                if (sInput != null) sInput.close();
            } catch (Exception e) {
            }
            try {
                if (socket != null) socket.close();
            } catch (Exception e) {
            }
        }

        private boolean writeMsg(String msg) {
            if (!socket.isConnected()) {
                close();
                return false;
            }
            try {
                sOutput.writeObject(msg);
            }
            catch (IOException e) {
                display(notif + "Error sending message to " + username + notif);
                display(e.toString());
            }
            return true;
        }

        private BigInteger calculate(String numbers) {
            int value = Integer.parseInt(numbers);
            BigInteger temp = BigInteger.ONE;
            for (int i = 1; i <= value; i++) {
                int number = i;
                while (number != 0) {
                    temp = temp.multiply(BigInteger.valueOf(number--));
                }
            }
            return temp;
        }

        private void calculator_several_thread() {
            starts = Instant.now();
            sdf = new SimpleDateFormat("HH:mm:ss");
            ExecutorService executor = Executors.newFixedThreadPool(10);

            Worker[] workers = new Worker[10];

            int range = nums.size() / 10;
            for (int index = 0; index < 10; index++) {
                int startAt = index * range;
                int endAt = startAt + range;
                workers[index] = new Worker(startAt, endAt, nums);
            }

            try {
                List<Future<BigInteger>> results = executor.invokeAll(Arrays.asList(workers));
                for (Future<BigInteger> future : results) {
                    System.out.println(future.get());
                }
            } catch (InterruptedException | ExecutionException ex) {
                ex.printStackTrace();
            }
            Instant ends = Instant.now();
            stopWatch.several_thread.setText("<html>Несколько<br>" + String.valueOf(Duration.between(starts, ends)).substring(2) + "</html");
        }

        private void calculator_one_thread() {
            starts = Instant.now();
            sdf = new SimpleDateFormat("HH:mm:ss");
            for (Map.Entry<String, String> entry : nums.entrySet()) {
                stopWatch.addPlainTexter("(ОДП) Ответ пользователю: " + entry.getKey() + " | " + entry.getValue() + " = " + " " + calculate(entry.getValue()));
            }
            Instant ends = Instant.now();
            stopWatch.one_thread.setText("<html>Один поток<br>" + String.valueOf(Duration.between(starts, ends)).substring(2) + "</html");
        }

        public class Worker implements Callable<BigInteger> {

            private int startAt;
            private int endAt;
            private Map<String, String> numbers;


            public Worker(int startAt, int endAt, Map<String, String> numbers) {
                this.startAt = startAt;
                this.endAt = endAt;
                this.numbers = numbers;
            }

            @Override
            public BigInteger call() throws Exception {
                List<String> keyList = new ArrayList<String>(nums.keySet());
                for (int index = startAt; index < endAt; index++) {
                    String key = keyList.get(index);
                    stopWatch.addPlainTexter("(МНГ) Ответ пользователю: " + key + " | " + nums.get(key) + " = " + calculate(nums.get(key)));
                }
                return null;
            }
        }
    }
}