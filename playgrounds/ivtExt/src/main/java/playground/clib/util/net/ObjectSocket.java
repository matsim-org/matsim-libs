// code by jph
package playground.clib.util.net;

import java.io.EOFException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ObjectSocket implements AutoCloseable, ObjectHandler {
    ObjectOutputStream objectOutputStream;
    ObjectInputStream objectInputStream = null;
    final Socket socket;
    volatile boolean launched = true;

    public ObjectSocket(Socket socket) {
        this.socket = socket;
        try {
            // flush the stream immediately to ensure that constructors for receiving ObjectInputStreams will not block when reading the header
            objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
            objectOutputStream.flush();

        } catch (Exception exception) {
            exception.printStackTrace();
        }
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (objectInputStream == null)
                        // constructor blocks until the corresponding ObjectOutputStream has written and flushed the header
                        objectInputStream = new ObjectInputStream(socket.getInputStream());

                    while (launched) {
                        @SuppressWarnings("unused")
                        Object object = objectInputStream.readObject(); // blocking, might give EOFException
                        System.out.println("object received");
                    }
                } catch (EOFException eofException) {
                    System.out.println("client has disconnected");
                } catch (Exception exception) {
                    if (launched)
                        exception.printStackTrace();
                }
                launched = false;
                try {
                    close();
                } catch (Exception exception) {
                    exception.printStackTrace();
                }
            }
        });

        thread.start();
    }

    @Override
    public void close() throws Exception {
        socket.close();
    }

    @Override
    public void handle(Object simulationObject) {
        try {
            objectOutputStream.writeObject(simulationObject);
        } catch (Exception exception) {
            System.err.println(exception.getMessage());
            System.out.println("unsubscribe");
            launched = false;
        }
    }

}
