package playground.sebhoerl.remote_exec;

import org.matsim.core.api.experimental.events.EventsManager;

import java.io.OutputStream;
import java.util.Map;

public interface RemoteSimulation {
    enum Status {
        PENDING, RUNNING, DONE, IDLE, ERROR, STOPPED
    }

    Map<String, String> getParameters();
    void update();

    String getId();
    Status getStatus();

    void start();
    void stop();

    void remove();
    void reset();

    long getIteration();

    void getOutputLog(OutputStream stream);
    void getErrorLog(OutputStream stream);

    void getEvents(EventsManager eventsManager, int iteration);
    void getEvents(EventsManager eventsManager);
    void getFile(String path, OutputStream stream);

    String getPath(String suffix);

    void setParameter(String parameter, String value);
    String getParameter(String parameter);

    void setMemory(String memory);
    String getMemory();
}
