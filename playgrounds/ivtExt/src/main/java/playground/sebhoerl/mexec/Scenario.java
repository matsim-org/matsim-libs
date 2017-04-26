package playground.sebhoerl.mexec;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface Scenario {
    String getId();

    String getMainConfigPath();
    void setMainConfigPath(String configPath);

    List<String> getAdditionalConfigFiles();

    Set<String> getAvailablePlaceholders();
    Map<String, String> getPlaceholders();

    void save();
}
