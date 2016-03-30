package playground.polettif.boescpa.analysis;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.utils.io.IOUtils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Analyzes and prints or writes to file the activity chains of a population.
 *
 * @author boescpa
 */
public class ActivityAnalyzer {
    private final static Logger log = Logger.getLogger(ActivityAnalyzer.class);

    private final Map<String, Integer> actChains;
    private final Set<String> actToIgnore;

    public ActivityAnalyzer() {
        actChains = new HashMap<>();
        actToIgnore = new HashSet<>();
        actToIgnore.add("pt interaction");
    }

    public void addActChain(final String actChain) {
        if (actChains.containsKey(actChain)) {
            actChains.put(actChain, actChains.get(actChain) + 1);
        } else {
            actChains.put(actChain, 1);
        }
    }

    public void analyzePopulation(final Population population) {
        String actChain;
        for (Person p : population.getPersons().values()) {
            if (p.getSelectedPlan() != null) {
                actChain = "";
                for (PlanElement pe : p.getSelectedPlan().getPlanElements()) {
                    if (pe instanceof ActivityImpl) {
                        ActivityImpl act = (ActivityImpl) pe;
                        if (!actToIgnore.contains(act.getType())) {
                            actChain = actChain.concat(act.getType().substring(0,2) + "-");
                        }
                    }
                }
                this.addActChain(actChain.substring(0, actChain.length()-1));
            }
            else {
                log.warn("person " + p.getId().toString() + " has no plan defined");
            }
        }
    }

    public void printActChainAnalysis(final String pathToOutputFile) {
        try {
            BufferedWriter writer = IOUtils.getBufferedWriter(pathToOutputFile);
            for (String actChain : actChains.keySet()) {
                writer.write(actChain + "; " + actChains.get(actChain));
                writer.newLine();
            }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void printActChainAnalysis() {
        log.info("");
        log.info("The following act chains were found in the population:");
        for (String actChain : actChains.keySet()) {
            log.info(actChain + "; " + actChains.get(actChain));
        }
        log.info("");
    }
}
