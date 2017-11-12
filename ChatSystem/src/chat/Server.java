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
import java.io.*;

import java.net.*;

import java.text.SimpleDateFormat;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import chat.Client.CommandLineValues;

public class Server {

    // a unique ID for each connection
    private static int uniqueId;

    // an ArrayList to keep the list of the Client
    private static ArrayList<ClientThread> clients;
    private static ArrayList<RoomList> rooms;
    // to display time
    private static SimpleDateFormat sdf;

    // the port number to listen for connection
    private int port;
    private JSONArray j = new JSONArray();

    // the boolean that will be turned of to stop the server
    private boolean keepGoing;

    /*
     
     *  server constructor that receive the port to listen to for connection as parameter
     
     *  in console
     
     */
    public Server(int port) {

        this.port = port;

        // to display hh:mm:ss
        sdf = new SimpleDateFormat("HH:mm:ss");

        // ArrayList for the Client list
        clients = new ArrayList<ClientThread>();
        rooms = new ArrayList<RoomList>();

    }

    public void start() {

        keepGoing = true;


        /* create socket server and wait for connection requests */
        try {

            // the socket used by the server
            ServerSocket serverSocket = new ServerSocket(port);

            // infinite loop to wait for connections
            while (keepGoing) {

                // format message saying we are waiting
                display("Server waiting for Clients on port " + port + ".");

                Socket socket = serverSocket.accept();      // accept connectio

                // if I was asked to stop
                if (!keepGoing) {
                    break;
                }

                ClientThread t = new ClientThread(socket);  // make a thread of it

                clients.add(t);  // save all clients to the ArrayList
                t.start();

            }

            // I was asked to stop
            try {

                serverSocket.close();

                for (int i = 0; i < clients.size(); ++i) {

                    ClientThread tc = clients.get(i);

                    try {

                        tc.sInput.close();

                        tc.sOutput.close();

                        tc.socket.close();

                    } catch (IOException ioE) {

                        // not much I can do
                    }

                }

            } catch (IOException e) {

                display("Exception closing the server and clients: " + e);

            }

        } // something went bad
        catch (IOException e) {

            String msg = sdf.format(new Date()) + " Exception on new ServerSocket: " + e + "\n";

            display(msg);

        }

    }

    /*
     
     * For the GUI to stop the server
     
     */
    protected void stop() {

        keepGoing = false;

        // connect to myself as Client to exit statement
        // Socket socket = serverSocket.accept();
        try {

            new Socket("localhost", port);

        } catch (IOException e) {

            // nothing I can really do
        }

    }

    /*
     
     * Display an event (not a message) to the console or the GUI
     
     */
    private static void display(String msg) {

        String time = sdf.format(new Date()) + " " + msg;

        System.out.println(time);

    }

    /*
     
     *  to broadcast a message to all Clients
     
     */
    //have problems
    private static synchronized void broadcast(String message) {

        // add HH:mm:ss and \n to the message
        String time = sdf.format(new Date());

        String messageLf = time + " " + message + "\n";

        // display message on console or GUI
        System.out.print(messageLf);
        JSONObject j = new JSONObject();
        j.put("type", "message");
        //j.put("identity", id);
        j.put("content", messageLf);

        // we loop in reverse order in case we would have to remove a Client
        // because it has disconnected
        for (int i = clients.size(); --i >= 0;) {

            ClientThread ct = clients.get(i);

            // try to write to the Client if it fails remove it from the list
            if (!ct.writeMsg(j)) {

                clients.remove(i);

                display("Disconnected Client " + ct.username + " removed from list.");

            }

        }

    }

    // for a client who logoff using the LOGOUT message
    synchronized static void remove(int id) {

        // scan the array list until we found the Id
        for (int i = 0; i < clients.size(); ++i) {

            ClientThread ct = clients.get(i);

            // found it
            if (ct.id == id) {

                clients.remove(i);

                return;

            }

        }

    }

    /*
     
     *  To run as a console application just open a console window and:
    
     * > java Server
    
     * > java Server portNumber
     
     * If the port number is not specified 4444 is used
     
     */
    public static void main(String[] args) {

        // start server on port 4444 unless a PortNumber is specified
        //int portNumber = 4444;
        
        CommandLineValues values = new CommandLineValues();
        CmdLineParser parser = new CmdLineParser(values);

        try {
            // parse the command line options with the args4j library
            parser.parseArgument(args);
            // print values of the command line options
//            System.out.println(values.getPort());

        } catch (CmdLineException e) {
            System.err.println(e.getMessage());
            parser.printUsage(System.err);
            System.exit(-1);
        }


        // create a server object and start it
        Server server = new Server(values.getPort());
        //create a main hall room at the begining
        RoomList mainHall = new RoomList("MainHall");
        rooms.add(mainHall);

        server.start();

    }

    private static class RoomList {

        String roomid;
        ArrayList<ClientThread> roomclients = new ArrayList<ClientThread>();

        public RoomList(String roomid) {
            this.roomid = roomid;
        }

        public String getRoomid() {
            return roomid;
        }

        public ArrayList<ClientThread> getRoomclients() {
            return roomclients;
        }

        public void addclients(ClientThread t) {
            roomclients.add(t);
        }

        public void deleteClients(ClientThread t) {
            roomclients.remove(t);
        }
    }

    /**
     * One instance of this thread will run for each client
     */
    static class ClientThread extends Thread {

        // the socket where to listen/talk
        static Socket socket;

        ObjectInputStream sInput;

        ObjectOutputStream sOutput;

        DataInputStream input;
        DataOutputStream output;

        // my unique id (easier for deconnection)
        int id;

        // the Username of the Client
        static String username;
        static String roomName;
        
        // the only type of message a will receive
        JSONObject json, j;
        String s;

        // the date I connect
        String date;

        // Constructore
        ClientThread(Socket socket) {

            // a unique id
            id = ++uniqueId;

            this.socket = socket;

            /* Creating both Data Stream */
            System.out.println("Thread trying to create Object Input/Output Streams");

            try {

                // create output first
                output = new DataOutputStream(socket.getOutputStream());

                input = new DataInputStream(socket.getInputStream());

                // read the username from client and create a new json object called "New identity"
                username = "guest" + id;
                j = new JSONObject();
                j.put("type", "newidentity");
                j.put("former", "");
                j.put("identity", username);

                display(username + " just connected.");
                //send it to client
                output.writeUTF(j.toString());
                output.flush();

            } catch (IOException e) {

                display("Exception creating new Input/output Streams: " + e);

                return;

            } // have to catch ClassNotFoundException
            // but I read a String, I am sure it will work

            date = new Date().toString() + "\n";
        }

        public static String getUsername() {
            return username;
        }

        public static String getRoomName() {
            return roomName;
        }

        public static void setRoomName(String roomName) {
            ClientThread.roomName = roomName;
        }
        
        

        // what will run forever
        @Override
        public void run() {

            // to loop until LOGOUT
            boolean keepGoing = true;

            while (keepGoing) {

                // read a String (which is an object)
                try {
                    //receive json message from client
                    s = input.readUTF();
                    JSONParser parser = new JSONParser();
                    json = (JSONObject) parser.parse(s);

                } catch (IOException e) {

                    display(username + " Exception reading Streams: " + e);
                    break;

                } catch (ParseException ex) {
                    Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
                }

                String type = json.get("type").toString();

                if (type.equals("quit")) {
                    display(username + " disconnected with a LOGOUT message.");
                    keepGoing = false;
                    break;

                } 
                else if (type.equals("message")) {
                    String m = json.get("content").toString();
                    String time = sdf.format(new Date());
                    String messageLf = time + " " + m;

                    System.out.print(messageLf);

                    JSONObject j = new JSONObject();
                    j.put("type", "message");
                    j.put("identity", username);
                    j.put("content", messageLf);

                    for (int i = clients.size(); --i >= 0;) {
                        ClientThread ct = clients.get(i);
                        // try to write to the Client if it fails remove it from the list
                        if (!ct.writeMsg(j)) {
                            clients.remove(i);
                            display("Disconnected Client " + ct.username + " removed from list.");
                        }
                    }

                } 
                else if (type.equals("identitychange")) {
                    String newid = json.get("identity").toString();
                    if (!newid.equals(username)) {
                        if (newid.length() >= 3 && newid.length() <= 16) {
                            JSONObject j = new JSONObject();
                            j.put("type", "newidentity");
                            j.put("former", username);
                            j.put("identity", newid);

                            for (int i = clients.size(); --i >= 0;) {
                                ClientThread ct = clients.get(i);
                                // try to write to the Client if it fails remove it from the list
                                if (!ct.writeMsg(j)) {
                                    clients.remove(i);
                                    display("Disconnected Client " + ct.username + " removed from list.");
                                }
                            }
                            username = newid;
                        } else {
                            try {
                                output.writeUTF(json.toString());
                            } catch (IOException ex) {
                                Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }

                    } else {
                        try {
                            JSONObject j = new JSONObject();
                            j.put("type", "newidentity");
                            j.put("former", username);
                            j.put("identity", newid);
                            output.writeUTF(j.toString());
                        } catch (IOException ex) {
                            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }

                } 
                else if (type.equals("join")) {
                    String roomid = json.get("roomid").toString();
                    for (int i = 0; i < rooms.size(); i++) {
                        if (roomid.equals(rooms.get(i).getRoomid())) {
                            
                            JSONObject j = new JSONObject();
                            j.put("type", "roomchange");
                            j.put("identity", username);
                            j.put("former", "MainHall");
                            j.put("roomid", roomid);
                            try {
                                output.writeUTF(j.toString());
                            } catch (IOException ex) {
                                Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
                            }
                            //add client to new room
                            for(int a=0;a<clients.size();a++){
                                if((clients.get(a).getUsername()).equals(username)){
                                    rooms.get(i).addclients(clients.get(a));
                                }
                            }
                            for(int a=0;a<rooms.size();a++){
                                if(rooms.get(a).getRoomid().equals(roomName)){
                                   for(int b=0;b<rooms.get(a).roomclients.size();b++)
                                       if(rooms.get(a).roomclients.get(b).getUsername().equals(username))
                                    rooms.get(a).deleteClients(rooms.get(a).roomclients.get(b));
                                }
                            }

                        } else {
                            JSONObject j = new JSONObject();
                            j.put("type", "roomchange");
                            j.put("identity", username);
                            j.put("former", "MainHall");
                            j.put("roomid", roomid);
                            try {
                                output.writeUTF(j.toString());
                            } catch (IOException ex) {
                                Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }

                    }
                } 
                else if (type.equals("who")) {
                    String roomid = null;
                    //writeMsg("List of the users connected at " + sdf.format(new Date()) + "\n");
                    //scan al the users connected
                    for (int i = 0; i < clients.size(); ++i) {

                        ClientThread ct = clients.get(i);

                        //writeMsg((i + 1) + ") " + ct.username + " since " + ct.date);
                    }

                } else if (type.equals("list")) {
                    String m = json.get("content").toString();
                    broadcast(username + ": " + m);

                } else if (type.equals("createroom")) {
                    String m = json.get("content").toString();
                    broadcast(username + ": " + m);

                } else if (type.equals("kick")) {
                    String m = json.get("content").toString();
                    broadcast(username + ": " + m);

                } else if (type.equals("delete")) {
                    String m = json.get("content").toString();
                    broadcast(username + ": " + m);

                }
            }

            // remove myself from the arrayList containing the list of the
            // connected Clients
            remove(id);

            close();

        }

        // try to close everything
        private void close() {

            // try to close the connection
            try {

                if (sOutput != null) {
                    sOutput.close();
                }

            } catch (IOException e) {
            }

            try {

                if (sInput != null) {
                    sInput.close();
                }

            } catch (IOException e) {
            };

            try {

                if (socket != null) {
                    socket.close();
                }

            } catch (IOException e) {
            }

        }

        /*
        
         * Write a String to the Client output stream
        
         */
        private boolean writeMsg(JSONObject j) {

            // if Client is still connected send the message to it
            if (!socket.isConnected()) {

                close();

                return false;

            }

            // write the message to the stream
            try {

                output.writeUTF(j.toString());

            } // if an error occurs, do not abort just inform the user
            catch (IOException e) {

                display("Error sending message to " + username);

                display(e.toString());

            }

            return true;

        }

    }

    public static class CommandLineValues {

        // Give it a default value of 4444 sec
        @Option(name = "-p", aliases = {"port"}, usage = "Port Address")
        private int port = 4444;

        public int getPort() {
            return port;
        }

    }
}
