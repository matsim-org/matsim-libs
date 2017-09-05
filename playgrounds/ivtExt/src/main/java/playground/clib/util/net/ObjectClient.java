// code by jph
package playground.clib.util.net;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;

public class ObjectClient {
    public final String IP;
    private Socket socket;
    ObjectOutputStream objectOutputStream = null;
    ObjectInputStream objectInputStream;
    volatile boolean isLaunched = true;

    public ObjectClient(final String IP, int port, ObjectHandler objectHandler) throws Exception {
        this.IP = IP;
        new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    socket = new Socket(InetAddress.getByName(IP), port); // blocking if IP cannot be reached
                    objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
                    objectOutputStream.flush();
                    if (objectInputStream == null)
                        // constructor blocks until the corresponding ObjectOutputStream has written and flushed the header
                        objectInputStream = new ObjectInputStream(socket.getInputStream());
                    while (isLaunched) {
                        Object object = objectInputStream.readObject(); // blocks until object is available
                        objectHandler.handle(object);
                    }
                } catch (Exception myException) {
                    if (isLaunched)
                        myException.printStackTrace();

                }
                close();
            }
        }).start();
    }

    public void close() {
        isLaunched = false;
        if (socket != null)
            try {
                socket.close();
                socket = null;
            } catch (Exception myException) {
                myException.printStackTrace();
            }
    }

    public boolean isOpen() {
        return socket != null && objectOutputStream != null;
    }
}
