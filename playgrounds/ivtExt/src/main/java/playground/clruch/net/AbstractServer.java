package playground.clruch.net;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.Timer;

public abstract class AbstractServer {

    private volatile boolean isRunning = false;
    private ServerSocket serverSocket = null;

    protected abstract int getPort();

    protected abstract void handleConnection(Socket mySocket, Timer myTimer);

    public final void startAcceptingNonBlocking() {
        isRunning = true;
        new Thread(new Runnable() {
            @Override
            public void run() {
                int port = getPort();
                Timer timer = new Timer();
                try {
                    serverSocket = new ServerSocket(port);
                    System.out.println("server available...");
                    while (isRunning) {
                        Socket socket = serverSocket.accept();
                        handleConnection(socket, timer); // this blocks until socket connection or server is closed
                    }
                } catch (Exception exception) {
                    if (!exception.getMessage().equals("socket closed"))
                        exception.printStackTrace();
                }
                timer.cancel();
            }
        }).start();
    }

    public final boolean isRunning() {
        return isRunning;
    }

    /** closes server socket */
    public final void stopAccepting() {
        isRunning = false;
        if (serverSocket != null)
            try {
                serverSocket.close();
                serverSocket = null;
            } catch (Exception myException) {
                myException.printStackTrace();
            }
    }

}
