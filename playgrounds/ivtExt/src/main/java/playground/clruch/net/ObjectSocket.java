package playground.clruch.net;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ObjectSocket implements AutoCloseable, SimulationSubscriber {
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
            SimulationSubscriberSet.INSTANCE.add(this); // TODO not the right location
        } catch (Exception myException) {
            myException.printStackTrace();
        }
        Thread myThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (objectInputStream == null)
                        // constructor blocks until the corresponding ObjectOutputStream has written and flushed the header
                        objectInputStream = new ObjectInputStream(socket.getInputStream());
                    
                    while (launched) {
                        Object myObject = objectInputStream.readObject(); // blocking, might give EOFException
                        System.out.println("object received");
                    }
                } catch (Exception exception) {
                    if (launched)
                        exception.printStackTrace();
                }
                launched = false;
                try {
                    close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        
        myThread.start();
    }

    @Override
    public void close() throws Exception {
        SimulationSubscriberSet.INSTANCE.remove(this);
        socket.close();
    }

    @Override
    public void handle(SimulationObject simulationObject) {
        try {
            objectOutputStream.writeObject(simulationObject);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("unsubscribe");
            launched = false;
        }
    }

}
