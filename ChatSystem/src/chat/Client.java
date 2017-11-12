package chat;
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author lenovoMH
 */
import java.net.*;

import java.io.*;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

public class Client {

    private DataInputStream input;
    private DataOutputStream output;

    private Socket socket;

    private String server, username;

    private int port;

    Client(String server, int port, String username) {

        this.server = server;

        this.port = port;

        this.username = username;

    }

    public boolean start() {

        // try to connect to the server
        try {

            socket = new Socket(server, port);

        } catch (Exception ec) {

            System.out.println("Error connectiong to server:" + ec);

            return false;

        }

        String msg = "Connection accepted " + socket.getInetAddress() + ":" + socket.getPort();

        System.out.println(msg);

        /* Creating both Data Stream */
        try {

            input = new DataInputStream(socket.getInputStream());

            output = new DataOutputStream(socket.getOutputStream());

        } catch (IOException eIO) {

            System.out.println("Exception creating new Input/output Streams: " + eIO);

            return false;

        }

        new ListenFromServer().start();
//
//        try {
//
//            output.writeUTF(username);
//
//        } catch (IOException eIO) {
//
//            display("Exception doing login : " + eIO);
//
//            disconnect();
//
//            return false;
//
//        }

        // success we inform the caller that it worked
        return true;

    }


    /*
     
     * To send a message to the server
     
     */
    void sendMessage(JSONObject object) {

        try {

            output.writeUTF(object.toString());

        } catch (IOException e) {

            System.out.println("Exception writing to server: " + e);

        }

    }

    private void disconnect() {

        try {

            if (input != null) {
                input.close();
            }

        } catch (IOException e) {
            e.getMessage();
        }

        try {

            if (output != null) {
                output.close();
            }

        } catch (IOException e) {
            e.getMessage();
        }

        try {

            if (socket != null) {
                socket.close();
            }

        } catch (IOException e) {
            e.getMessage();
        }
    }

    public static void main(String[] args) throws InterruptedException {
        // default values

//        int portNumber = 4444;
//
//        String serverAddress = "localhost";

        String userName = "guest";

        CommandLineValues values = new CommandLineValues();
        CmdLineParser parser = new CmdLineParser(values);

        try {
            // parse the command line options with the args4j library
            parser.parseArgument(args);
            // print values of the command line options
//            System.out.println(values.getHost());
//            System.out.println(values.getPort());

        } catch (CmdLineException e) {
            System.err.println(e.getMessage());
            parser.printUsage(System.err);
            System.exit(-1);
        }


        // create the Client object

        Client client = new Client(values.getHost(), values.getPort(), userName);
        // test if we can start the connection to the Server

        if (!client.start()) {
            return;
        }
        // wait for messages from user
        Scanner scan = new Scanner(System.in);

        while (true) {

            // read message from user
            String msg = scan.nextLine();

            if (msg.equalsIgnoreCase("#quit")) {
                JSONObject j1 = new JSONObject();
                j1.put("type", "quit");
                client.sendMessage(j1);
                // break to do the disconnect
                break;

            } else if (msg.startsWith("#identitychange")) {
                String idc, newid = null;
                Scanner scan2 = new Scanner(msg);
                scan2.useDelimiter(" ");
                while (scan2.hasNext()) {
                    idc = scan2.next();
                    newid = scan2.next();
                }
                JSONObject j2 = new JSONObject();
                j2.put("type", "identitychange");
                j2.put("identity", newid);
                client.sendMessage(j2);

            } else if (msg.startsWith("#join")) {
                String j, id = null;
                Scanner scan2 = new Scanner(msg);
                scan2.useDelimiter(" ");
                while (scan2.hasNext()) {
                    j = scan2.next();
                    id = scan2.next();
                }
                JSONObject j3 = new JSONObject();
                j3.put("type", "join");
                j3.put("roomid", id);
                client.sendMessage(j3);

            } else if (msg.startsWith("#who")) {
                String w, id = null;
                Scanner scan2 = new Scanner(msg);
                scan2.useDelimiter(" ");
                while (scan2.hasNext()) {
                    w = scan2.next();
                    id = scan2.next();
                }
                JSONObject j4 = new JSONObject();
                j4.put("type", "who");
                j4.put("roomid", id);
                client.sendMessage(j4);

            } else if (msg.startsWith("#list")) {

                JSONObject j5 = new JSONObject();
                j5.put("type", "list");

                client.sendMessage(j5);

            } else if (msg.startsWith("#createroom")) {
                String c, id = null;
                Scanner scan2 = new Scanner(msg);
                scan2.useDelimiter(" ");
                while (scan2.hasNext()) {
                    c = scan2.next();
                    id = scan2.next();
                }
                JSONObject j6 = new JSONObject();
                j6.put("type", "createroom");
                j6.put("roomid", id);
                client.sendMessage(j6);

            } else if (msg.startsWith("#kick")) {
                String k, rid = null, t = null, username = null;
                Scanner scan2 = new Scanner(msg);
                scan2.useDelimiter(" ");
                while (scan2.hasNext()) {
                    k = scan2.next();
                    username = scan2.next();
                    rid = scan2.next();
                    t = scan2.next();
                }
                JSONObject j7 = new JSONObject();
                j7.put("type", "kick");
                j7.put("roomid", rid);
                j7.put("time", t);
                j7.put("identity", username);
                client.sendMessage(j7);

            } else if (msg.startsWith("#delete")) {
                String d, id = null;
                Scanner scan2 = new Scanner(msg);
                scan2.useDelimiter(" ");
                while (scan2.hasNext()) {
                    d = scan2.next();
                    id = scan2.next();
                }
                JSONObject j8 = new JSONObject();
                j8.put("type", "delete");
                j8.put("roomid", id);
                client.sendMessage(j8);

            } else {
                JSONObject j9 = new JSONObject();
                j9.put("type", "message");
                j9.put("content", msg);
                client.sendMessage(j9);

            }

        }
        // done disconnect
        client.disconnect();

    }

    class ListenFromServer extends Thread {

        String msg;
        JSONObject j;
        String name;

        @Override
        public void run() {
            while (true) {
                try {
                    msg = input.readUTF();
                    JSONParser parser = new JSONParser();
                    j = (JSONObject) parser.parse(msg);
                    String type = j.get("type").toString();

                    if (type.equals("newidentity")) {
                        if (j.get("former").equals("")) {
                            name = j.get("identity").toString();
                            System.out.println("Connected to localhost as " + name);
                            System.out.print(name + "> ");
                        } else if ((j.get("former").toString()).equals(j.get("identity").toString())) {
                            System.out.println("Requested identity invalid or in use");
                        } else {
                            name = j.get("identity").toString();
                            System.out.println(j.get("former") + " is now " + name);
                            System.out.print(name + "> ");
                        }
                        System.out.println(name + " moves to MainHall");

                    } else if (type.equals("message")) {
                        System.out.print(j.get("identity") + ": ");
                        System.out.println(j.get("content"));
                        System.out.print(name + "> ");
                    } else if (type.equals("roomchange")) {
                        if (j.get("former").equals(j.get("roomid"))) {
                            System.out.println("The requested room is invalid or non existent.");
                        } else {
                            System.out.println(name + " moves from " + j.get("former") + " to " + j.get("roomid"));
                        }
                    } else if (type.equals("roomlist")) {

                    }

                } catch (IOException ex) {
                    Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
                } catch (ParseException ex) {
                    Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
                }

            }

        }

    }

    public static class CommandLineValues {

        @Argument(required = true, usage = "Host Name")
        private String host;

        // Give it a default value of 4444 sec
        @Option(required = false, name = "-p", aliases = {"port"}, usage = "Port Address")
        private int port = 4444;

        public int getPort() {
            return port;
        }

        public String getHost() {
            return host;
        }
    }

}

