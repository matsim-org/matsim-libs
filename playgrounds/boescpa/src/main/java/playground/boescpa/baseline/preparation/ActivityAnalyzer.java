package playground.boescpa.baseline.preparation;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.utils.io.IOUtils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Analyzes and prints or writes to file the activity chains of a population.
 *
 * @author boescpa
 */
public class ActivityAnalyzer {
    private final static Logger log = Logger.getLogger(ActivityAnalyzer.class);

    private final Map<String, Integer> actChains;

    protected ActivityAnalyzer() {
        actChains = new HashMap<>();
    }

    protected void addActChain(final String actChain) {
        if (actChains.containsKey(actChain)) {
            actChains.put(actChain, actChains.get(actChain) + 1);
        } else {
            actChains.put(actChain, 1);
        }
    }

    protected void analyzePopulation(final Population population) {
        String actChain;
        for (Person p : population.getPersons().values()) {
            if (p.getSelectedPlan() != null) {
                actChain = "";
                for (PlanElement pe : p.getSelectedPlan().getPlanElements()) {
                    if (pe instanceof ActivityImpl) {
                        ActivityImpl act = (ActivityImpl) pe;
                        actChain = actChain.concat(act.getType().substring(0, 1));
                    }
                    this.addActChain(actChain);
                }
            }
            else {
                log.warn("person " +p.getId().toString()+ " has no plan defined");
            }
        }
    }

    protected void printActChainAnalysis(final String pathToOutputFile) {
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

    protected void printActChainAnalysis() {
        log.info("");
        log.info("The following act chains were found in the population:");
        for (String actChain : actChains.keySet()) {
            log.info(actChain + "; " + actChains.get(actChain));
        }
        log.info("");
    }
}
