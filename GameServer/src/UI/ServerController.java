package UI;

import DataClasses.TTT_GameData;
import DataClasses.User;
import Database.DBManager;
import MainServer.Client;
import MainServer.*;
import Messages.*;
import ServerInterfaces.ServerListener;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ResourceBundle;

public class ServerController implements Initializable, ServerListener
{
    public ListView registeredPlayersList;
    public ListView onlinePlayerList;
    public Button modifyPlayerButton;
    public ListView activeGamesList;
    public Button activeGameDetailsButton;
    public ListView inactiveGamesList;
    public Button inactiveGameDetailsButton;
    private List<User> allPlayers = new ArrayList<>();


    public void onModifyPlayerClicked()
    {
        try
        {
            int usernameIndex = registeredPlayersList.getSelectionModel().getSelectedIndex();
            User selectedPlayer = allPlayers.get(usernameIndex - 1);

            FXMLLoader loader = new FXMLLoader(getClass().getResource("ModifyPlayer.fxml"));
            Parent root = loader.load();
            ModifyPlayerController mpc = loader.getController();
            mpc.passInfo(selectedPlayer);
            Stage stage = (Stage) modifyPlayerButton.getScene().getWindow();
            stage.close();
            stage.setTitle("Modify Player");
            stage.setScene(new Scene(root));
            stage.show();
        }
        catch(IOException | NullPointerException e)
        {
            e.printStackTrace();
        }
    }

    public void onActiveGameDetailsClicked()
    {
        try
        {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("GameDetails.fxml"));
            Parent root = loader.load();
            GameDetailsController gdc = loader.getController();
            MainServer.getInstance().addObserver(gdc);
            MainServer.getInstance().removeObserver(this);
            Stage stage = (Stage) activeGameDetailsButton.getScene().getWindow();
            stage.close();
            stage.setTitle("Active Game Details");
            stage.setScene(new Scene(root));
            stage.show();
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }
    }

    public void onInactiveGameDetailsClicked()
    {
        try
        {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("GameDetails.fxml"));
            Parent root = loader.load();
            GameDetailsController gdc = loader.getController();
            //gdc.passInfo();
            Stage stage = (Stage) inactiveGameDetailsButton.getScene().getWindow();
            stage.close();
            stage.setTitle("Completed Game Details");
            stage.setScene(new Scene(root));
            stage.show();
        }
        catch(IOException e){e.printStackTrace();}
    }

    @Override
    public void update(Serializable msg, Object data) {
        Platform.runLater(() -> {
            switch (msg.getClass().getSimpleName()) {
                case "EncapsulatedMessage":
                    SQLServiceConnection.getInstance().sendPacket(new Packet("AGS-MSG", new AllGamesMessage()));
                case "AccountSuccessfulMessage":
                    SQLServiceConnection.getInstance().sendPacket(new Packet("RUS-MSG", new RegisteredUsersMessage()));
                    break;

                case "AllGamesMessage":
                    List<Object> all_games = ((AllGamesMessage) msg).getGames();
                    for(Object obj: all_games) {
                        TTT_GameData game = (TTT_GameData) obj;
                        if (game.getWinningPlayerId() != -1) {
                            String p1 = String.valueOf(game.getPlayer1Id());
                            String p2 = (game.getPlayer2Id() == 1) ? "AI" : String.valueOf(game.getPlayer2Id());
                            inactiveGamesList.getItems().add(new Label(p1 + " vs " + p2 + " (" + game.getId() + ")"));
                        }
                    }
                    break;

                case "RegisteredUsersMessage":
                    // update registered player list
                    registeredPlayersList.getItems().clear();
                    List<Object> all_users = ((RegisteredUsersMessage)msg).getUsers();
                    for(Object obj: all_users) {
                        User user = (User) obj;
                        registeredPlayersList.getItems().add(new Label(user.getUsername() + " (" + user.getId() + ")"));
                    }
                    break;

                case "LoginSuccessfulMessage":
                case "DeactivateAccountMessage":
                    onlinePlayerList.getItems().clear();
                    Client client = null;
                    synchronized (MainServer.getInstance().getClients()) {
                        Iterator<Client> iterator = MainServer.getInstance().getClients().iterator();
                        while (iterator.hasNext()) {
                            client = iterator.next();
                            if (client.getUser() != null && client.getUser().getId() != 0) {
                                allPlayers.add(client.getUser());
                                onlinePlayerList.getItems().add(new Label(client.getUser().getUsername() + " (" + client.getUser().getId() + ")"));
                            }
                        }
                    }
                    break;

                case "GameResultMessage":
                    // update completed games list
                    String game_id = (String) data;
                    TTT_GameData game = MainServer.getInstance().getGame_by_id().get(game_id);
                    String p1 = String.valueOf(game.getPlayer1Id());
                    String p2 = (game.getPlayer2Id() == 1) ? "AI" : String.valueOf(game.getPlayer2Id());
                    inactiveGamesList.getItems().add(new Label(p1 + " vs " + p2 + " (" + game.getId() + ")"));
                case "ConnectToLobbyMessage":
                case "CreateAIGameMessage":
                    // update active games
                    activeGamesList.getItems().clear();
                    synchronized (MainServer.getInstance().getActiveGames()) {
                        Iterator<TTT_GameData> iterator = MainServer.getInstance().getActiveGames().iterator();
                        while (iterator.hasNext()) {
                            game = iterator.next();
                            if (game.getPlayer2Id() != 0) {
                                p1 = String.valueOf(game.getPlayer1Id());
                                p2 = (game.getPlayer2Id() == 1) ? "AI" : String.valueOf(game.getPlayer2Id());
                                activeGamesList.getItems().add(new Label(p1 + " vs " + p2 + " (" + game.getId() + ")"));
                            }
                        }
                    }
                    break;
            }
        });
    }



    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        MainServer.getInstance().addObserver(this);
        MainServer.getInstance().notifyObservers(new EncapsulatedMessage(null, null, null), null);
    }
}
