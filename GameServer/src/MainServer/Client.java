package MainServer;

import DataClasses.LobbyInfo;
import DataClasses.TTT_GameData;
import DataClasses.User;
import Messages.*;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.BlockingQueue;

public class Client implements Runnable, Serializable {
    private User user = null;

    private Thread thread;

    private Socket socket;
    private ObjectInputStream input;
    private ObjectOutputStream output;

    private List<Client> clients;
    private BlockingQueue<Packet> requests;

    public Client(Socket socket) {
        try {
            this.socket = socket;
            input = new ObjectInputStream(socket.getInputStream());
            output = new ObjectOutputStream(socket.getOutputStream());

            this.clients = MainServer.getInstance().getClients();
            this.requests = MainServer.getInstance().getRequests();

            thread = new Thread(this);
            thread.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized void sendPacket(Packet packet) {
        // Used to set this client socket's user to null if they tried to CREATE an account with an already existing username
        if(packet.getType().equals("ACF-MSG") && user.getId() == 0) {
            user = null;
        }

        try {
            output.writeObject(packet);
            output.flush();
            output.reset();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            while (!thread.isInterrupted()) {
                Packet packet = (Packet) input.readObject();
                System.out.println("Received from Client-side: " + packet.getType());
                switch (packet.getType()) {
                    //--------------------------------------------------------------------------------------------------
                    //                                      Resolve Here
                    //--------------------------------------------------------------------------------------------------
                    case "AAG-MSG": // All active games
                        AllActiveGamesMessage AAG = (AllActiveGamesMessage) packet.getData();
                        List<LobbyInfo> allGames = new ArrayList<>();

                        synchronized (MainServer.getInstance().getActiveGames()) {
                            Iterator<TTT_GameData> iterator = MainServer.getInstance().getActiveGames().iterator();
                            while(iterator.hasNext()) {
                                TTT_GameData current_game = iterator.next();
                                String creator = MainServer.getInstance().getClientIDMap().get(current_game
                                        .getPlayer1Id()).getUser().getUsername();
                                allGames.add(new LobbyInfo(creator, current_game.getId(),
                                        (current_game.getPlayer2Id() == 0)? 1 : 2));
                            }
                        }

                        AAG.setAllActiveGames(allGames);
                        sendPacket(new Packet("AAG-MSG", AAG));
                        break;

                    //--------------------------------------------------------------------------------------------------
                    //                                  send to game service
                    //--------------------------------------------------------------------------------------------------
                    case "SPC-MSG": // Spectate
                    case "CNT-MSG": // Connect to lobby
                    case "CAI-MSG": // Create AI Game Lobby
                    case "CLB-MSG": // Create Game Lobby
                    case "MOV-MSG": // Move
                        EncapsulatedMessage ENC_Game = new EncapsulatedMessage(packet.getType(), user.getId(),
                                packet.getData());
                        GameServiceConnection.getInstance().sendPacket(new Packet("ENC-MSG", ENC_Game));
                        break;

                    //--------------------------------------------------------------------------------------------------
                    //                                  send to sql service
                    //--------------------------------------------------------------------------------------------------
                    case "CAC-MSG": // Create Account
                        CreateAccountMessage CAC = (CreateAccountMessage) packet.getData();
                        user = new User(0, CAC.getNewUser().getUsername(), null, null, null, false);
                        EncapsulatedMessage ENC_CAC = new EncapsulatedMessage(packet.getType(), user.getUsername(),
                                packet.getData());
                        SQLServiceConnection.getInstance().sendPacket(new Packet("ENC-MSG", ENC_CAC));
                        break;

                    case "LOG-MSG": // Login
                        LoginMessage LOG = (LoginMessage) packet.getData();
                        boolean LOF = false;
                        synchronized (MainServer.getInstance().getClients()) {
                            Iterator<Client> iterator = MainServer.getInstance().getClients().iterator();
                            while (iterator.hasNext()) {
                                Client client = iterator.next();
                                if(client.user != null) {
                                    if (client.user.getId() != 0 && client.user.getUsername().equals(LOG.getUsername())) {
                                        LOF = true;
                                        break;
                                    }
                                }
                            }
                        }

                        if (LOF)
                            sendPacket(new Packet("LOF-MSG", (LoginFailedMessage) MessageFactory.getMessage(
                                    "LOF-MSG")));
                        else {
                            user = new User(0, LOG.getUsername(), null, null, null, false);
                            EncapsulatedMessage ENC_LOG = new EncapsulatedMessage(packet.getType(), user.getUsername(),
                                    packet.getData());
                            SQLServiceConnection.getInstance().sendPacket(new Packet("ENC-MSG", ENC_LOG));
                        }
                        break;

                    case "GMP-MSG": // Games played
                    case "GVW-MSG": // Game Viewers
                    case "DAC-MSG": // Deactivate Account
                    case "UPA-MSG": // Update Account Info
                    case "GLG-MSG": // Game Log
                    case "STS-MSG": // Stats
                    case "IAG-MSG": // Inactive game message
                        EncapsulatedMessage ENC = new EncapsulatedMessage(packet.getType(), user.getId(),
                                packet.getData());
                        SQLServiceConnection.getInstance().sendPacket(new Packet("ENC-MSG", ENC));
                        break;
                }
            }
        } catch (IOException | ClassNotFoundException e) {e.printStackTrace();}
        finally {synchronized (clients) {
            if(user != null) {
                System.out.println("Client terminated: " + user.getUsername());
            }
            clients.remove(this);
            SQLServiceConnection.getInstance().updateUI();}
        }
    }

    public void terminateConnection() {
        thread.interrupt();
        try {
            input.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public User getUser() {return user;}
    public void setUser(User user) {this.user = user;}
}
