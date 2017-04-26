package playground.sebhoerl.mexec;

import java.io.InputStream;
import java.util.Map;

public interface Simulation {
    String getId();

    void save();

    void start();
    void stop();
    void reset();

    Map<String, String> getPlaceholders();
    Config getConfig();

    void setMemory(Long bytes);
    Long getMemory();

    Long getIteration();

    InputStream getOutputLog();
    InputStream getErrorLog();

    InputStream getEvents(Long iteration);
    InputStream getEvents();

    InputStream getOutputFile(String suffix);
    String getOutputPath(String suffix);

    boolean isActive();
}
