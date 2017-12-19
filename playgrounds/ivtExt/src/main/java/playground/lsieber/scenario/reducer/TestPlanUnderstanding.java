package playground.lsieber.scenario.reducer;

import java.io.IOException;
import java.util.HashSet;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.network.algorithms.NetworkCleaner;

public class TestPlanUnderstanding {

    public static void main(String[] args) throws Exception {
        IdscSettingsLoaderImpl settings = new IdscSettingsLoaderImpl();
        Scenario scenario = settings.loadScenario();

        Population population = scenario.getPopulation();

        // population.getPersons();

        HashSet<String> modes = new HashSet<String>();
        // modes.add("car");
        modes.add("pt");
        // modes.add("tram");
        // modes.add("bus")

        // Network publicTransportnetwork = NetworkActions.modeFilter(scenario.getNetwork(), modes);
        Network publicTransportnetwork = scenario.getNetwork();
        new NetworkCleaner().run(publicTransportnetwork);
        new NetworkWriter(publicTransportnetwork).write("BaselNetwork_cleaned.xml");
        System.out.println("END");

    }
}
