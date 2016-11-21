package playground.sebhoerl.mexec.generic;

import playground.sebhoerl.mexec.Simulation;
import playground.sebhoerl.mexec.data.SimulationData;

import java.io.InputStream;
import java.util.Map;

public abstract class AbstractSimulation<DataType extends SimulationData> implements Simulation {
    final protected DataType data;

    public AbstractSimulation(DataType data) {
        this.data = data;
    }

    @Override
    public String getId() {
        return data.id;
    }

    @Override
    public Map<String, String> getPlaceholders() {
        return data.placeholders;
    }

    @Override
    public void setMemory(Long bytes) {
        data.memory = bytes;
    }

    @Override
    public Long getMemory() {
        return data.memory;
    }

    @Override
    public InputStream getOutputLog() {
        return getOutputFile("../o.log");
    }

    @Override
    public InputStream getErrorLog() {
        return getOutputFile("../e.log");
    }

    @Override
    public InputStream getEvents(Long iteration) {
        String eventsPath = "output_events.xml.gz";

        if (iteration != null) {
            eventsPath = "ITERS/it." + iteration + "/" + iteration + ".events.xml.gz";
        }

        return getOutputFile(eventsPath);
    }

    @Override
    public InputStream getEvents() {
        return getEvents(null);
    }
}
