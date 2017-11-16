package playground.lsieber.scenario.reducer;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.FacilitiesWriter;

import ch.ethz.idsc.queuey.datalys.MultiFileTools;
import ch.ethz.idsc.queuey.util.GlobalAssert;
import playground.clruch.ScenarioOptions;
import playground.clruch.utils.PropertiesExt;

public abstract class AbstractScenarioReducer {
    protected File workingDirectory;
    private Config config;
    private PropertiesExt simOptions;
    protected Scenario originalScenario;
    protected Network network;
    protected Population population;
    protected ActivityFacilities facilities;
    //TODO create Folder based on this Directory if it does not exist, Make this string Changeable
    private String reducedScenarioDirectory = "reducedScenario";

    public AbstractScenarioReducer() throws IOException {
        this.loadScenario();
        this.run();
    }

    private void loadScenario() throws IOException {
        workingDirectory = MultiFileTools.getWorkingDirectory();
        simOptions = PropertiesExt.wrap(ScenarioOptions.load(workingDirectory));
        File file = new File(workingDirectory, simOptions.getString("simuConfig"));
        config = ConfigUtils.loadConfig(file.toString());
        originalScenario = ScenarioUtils.loadScenario(config);
    }

    public void run() throws MalformedURLException, IOException {
        // Mandatory Fields of a Scenario
        GlobalAssert.that(originalScenario != null);
        network = networkCutter();
        population = populationCutter();
        facilities = facilitiesCutter();
    }

    protected abstract Network networkCutter() throws MalformedURLException, IOException;

    protected abstract Population populationCutter();

    protected abstract ActivityFacilities facilitiesCutter();

    public void writeToXML() {
        
        File dir = new File(reducedScenarioDirectory);
        if (!dir.exists()) {
            dir.mkdir();
        }
        // Mandatory Fields of a Scenario
        GlobalAssert.that(facilities != null && network != null && population != null);

        new NetworkWriter(network).write(reducedScenarioDirectory + "/reducedNetwork.xml");
        new PopulationWriter(population).write(reducedScenarioDirectory + "/reducedPopulation.xml");
        new FacilitiesWriter(facilities).write(reducedScenarioDirectory + "/reducedFacilities.xml");
    }

}
