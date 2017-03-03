package contrib.baseline.modification;

import org.apache.commons.math.distribution.Distribution;
import org.apache.commons.math.distribution.NormalDistributionImpl;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlansConfigGroup;
import org.matsim.core.population.algorithms.PersonAlgorithm;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.population.io.StreamingPopulationReader;
import org.matsim.core.population.io.StreamingPopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.Time;

public class EndTimeDiluter {
    static public void main(String[] args) {
        new EndTimeDiluter().dilutePopulation(args[0], args[1]);
    }

    final private double standardDeviation = 120.0;
    final private double maximumDeviation = 150.0;
    final private NormalDistribution normal = new NormalDistribution(0.0, standardDeviation);

    private double diluteEndTime(double time) {
        double deviation = Double.POSITIVE_INFINITY;

        while (deviation < -maximumDeviation || deviation > maximumDeviation) {
            deviation = normal.sample();
        }

        return time + deviation;
    }

    public void dilutePopulation(String populationSourcePath, String populationTargetPath) {
        Config config = ConfigUtils.createConfig();
        Scenario scenario = ScenarioUtils.createScenario(config);

        StreamingPopulationWriter writer = new StreamingPopulationWriter();
        writer.startStreaming(populationTargetPath);

        StreamingPopulationReader reader = new StreamingPopulationReader(scenario);
        reader.addAlgorithm(new PersonAlgorithm() {
            @Override
            public void run(Person person) {
                // First pass: dilute end times
                for (PlanElement pe : person.getSelectedPlan().getPlanElements()) {
                    if (pe instanceof Activity) {
                        Activity activity = (Activity) pe;

                        if (activity.getEndTime() != Time.UNDEFINED_TIME) {
                            activity.setEndTime(diluteEndTime(activity.getEndTime()));
                        }
                    }
                }

                // Second pass: repair inconsistent end times (they must be increasing!)
                double now = Double.POSITIVE_INFINITY;
                Activity previous = null;

                for (PlanElement pe : person.getSelectedPlan().getPlanElements()) {
                    if (pe instanceof Activity) {
                        Activity activity = (Activity) pe;

                        if (previous != null) {
                            if (activity.getEndTime() != Time.UNDEFINED_TIME && previous.getEndTime() > activity.getEndTime()) {
                                previous.setEndTime(activity.getEndTime());
                            }
                        }

                        previous = activity;
                    }
                }
            }
        });
        reader.addAlgorithm(writer);

        reader.readFile(populationSourcePath);
        writer.closeStreaming();
    }
}
