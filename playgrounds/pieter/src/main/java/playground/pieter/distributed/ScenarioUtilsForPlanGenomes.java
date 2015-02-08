package playground.pieter.distributed;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.facilities.MatsimFacilitiesReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.TransitScheduleReaderV1;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.vehicles.VehicleReaderV1;
import playground.pieter.distributed.plans.PopulationFactoryForPlanGenomes;
import playground.pieter.distributed.plans.PopulationReaderMatsimV5ForPlanGenomes;
import playground.pieter.distributed.plans.PopulationUtilsForPlanGenomes;

/**
 * Created by fouriep on 2/6/15.
 */
public class ScenarioUtilsForPlanGenomes {

    public static Scenario buildAndLoadScenario(Config config, boolean trackGenome, boolean isSlaveControler) {
        ScenarioUtils.ScenarioBuilder builder = new ScenarioUtils.ScenarioBuilder(config);
        if (trackGenome)
            builder.setPopulation(PopulationUtilsForPlanGenomes.createPopulation(config));
        Scenario scenario = builder.createScenario();
        new MatsimNetworkReader(scenario).readFile(config.network().getInputFile());
        if (config.facilities().getInputFile() != null)
            new MatsimFacilitiesReader(scenario).readFile(config.facilities().getInputFile());
        if (config.scenario().isUseTransit()) {
            if (trackGenome)
                new TransitScheduleReaderV1(scenario.getTransitSchedule(),
                        ((PopulationFactoryForPlanGenomes) (scenario.getPopulation().getFactory()))
                                .getModeRouteFactory()).readFile(config.transit().getTransitScheduleFile());
            else
                new TransitScheduleReader(scenario).readFile(config.transit().getTransitScheduleFile());
            if (config.scenario().isUseVehicles()) {
                new VehicleReaderV1(scenario.getVehicles()).readFile(config.transit().getVehiclesFile());
            }
        }
        if (!isSlaveControler) {
            if (trackGenome)
                new PopulationReaderMatsimV5ForPlanGenomes(scenario).readFile(config.plans().getInputFile());
            else
                new MatsimPopulationReader(scenario).readFile(config.plans().getInputFile());
        }
        return scenario;
    }
}
