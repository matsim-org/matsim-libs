package playground.clruch.demo.temp;

import org.matsim.core.config.ReflectiveConfigGroup;

/**
 * DOES NOT WORK YET
 */
public class ApocalypseConfigGroup extends ReflectiveConfigGroup {
    final static String APOCALYPSE = "apocalypse";
    final static String MAXPOPULATIONSIZE = "maxPopulationSize";

    private String maxPopulationSize;

    public ApocalypseConfigGroup() {
        super(APOCALYPSE);
    }

    @StringGetter(MAXPOPULATIONSIZE)
    public String getConfigPath() {
        return maxPopulationSize;
    }

    @StringSetter(MAXPOPULATIONSIZE)
    public void setConfigPath(String path) {
        maxPopulationSize = path;
    }
}
