package playground.sebhoerl.remote_exec.local;

import org.matsim.core.api.experimental.events.EventsManager;
import playground.sebhoerl.remote_exec.RemoteSimulation;

import java.io.OutputStream;
import java.util.Map;

/**
 * Created by sebastian on 11/10/16.
 */
public class LocalSimulation implements RemoteSimulation {
    final private String id;
    final private LocalInterface local;

    public LocalSimulation(LocalInterface local, String id) {
        this.id = id;
        this.local = local;
    }

    @Override
    public Map<String, String> getParameters() {
        return getInternal().getParameters();
    }

    @Override
    public void update() {
        getInternal().update();
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public Status getStatus() {
        return getInternal().getStatus();
    }

    @Override
    public void start() {
        getInternal().start();
    }

    @Override
    public void stop() {
        getInternal().stop();
    }

    @Override
    public void remove() {
        getInternal().remove();
    }

    @Override
    public void reset() {
        getInternal().reset();
    }

    @Override
    public long getIteration() {
        return getInternal().getIteration();
    }

    @Override
    public void getOutputLog(OutputStream stream) {
        getInternal().getOutputLog(stream);
    }

    @Override
    public void getErrorLog(OutputStream stream) {
        getInternal().getErrorLog(stream);
    }

    @Override
    public void getEvents(EventsManager eventsManager, int iteration) {
        getInternal().getEvents(eventsManager, iteration);
    }

    @Override
    public void getEvents(EventsManager eventsManager) {
        getInternal().getEvents(eventsManager, null);
    }

    @Override
    public void getFile(String path, OutputStream stream) {
        getInternal().getFile(path, stream);
    }

    @Override
    public String getPath(String suffix) {
        return getInternal().getPath(suffix);
    }

    @Override
    public void setParameter(String parameter, String value) {
        getParameters().put(parameter, value);
        update();
    }

    @Override
    public String getParameter(String parameter) {
        return getParameters().get(parameter);
    }

    @Override
    public void setMemory(String bytes) {
        getInternal().setMemory(bytes);
    }

    @Override
    public String getMemory() {
        return getInternal().getMemory();
    }

    private InternalLocalSimulation getInternal() {
        InternalLocalSimulation internal = local.getSimulations().get(id);

        if (internal == null) {
            throw new RuntimeException("Simulation " + id + " does not exist anymore");
        }

        return internal;
    }
}
