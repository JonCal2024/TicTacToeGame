package SQLService;

import Messages.Packet;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;

public class SQLHandler implements Runnable{
    SQLHandler instance = new SQLHandler();

    private BlockingQueue<Packet> requests;
    private Thread thread;

    private SQLHandler() {
        requests = SQLServer.getInstance().getRequests();

        thread = new Thread(this);
        thread.run();
    }

    @Override
    public void run() {
        try {
            while(!thread.isInterrupted()) {
                Packet packet = requests.take();


            }
        } catch (IOException | InterruptedException | NullPointerException e) {
            e.printStackTrace();
        }
    }

    public synchronized void terminateServer() {
        thread.interrupt();
        requests = null;
    }
}
