package playground.lsieber.scenario.reducer;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.FacilitiesWriter;

import ch.ethz.idsc.queuey.util.GlobalAssert;
import playground.clruch.ScenarioOptions;

public abstract class AbstractScenarioReducer extends IdscSettingsLoader {

    protected Scenario originalScenario;
    protected Network network;
    protected Population population;
    protected ActivityFacilities facilities;
    // TODO create Folder based on this Directory if it does not exist, Make this string Changeable
    private String reducedScenarioDirectory = "reducedScenario";

    public AbstractScenarioReducer() throws IOException {
        super();
        originalScenario = this.loadScenario();
    }

    public void run() throws MalformedURLException, IOException {
        // Mandatory Fields of a Scenario
        GlobalAssert.that(originalScenario != null);
        network = networkCutter();
        population = populationCutter();
        facilities = facilitiesCutter();
        System.out.println("------RUN() Results: ---------");
        System.out.println("Network: a) # Nodes: " + network.getNodes().size() + ", b) # Links: " + network.getLinks().size());
        System.out.println("Population: # people: " + population.getPersons().size());
        System.out.println("Facilities: # facilities: " + facilities.getFacilities().size());
        System.out.println("-------------------------------");
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

    public void ConvertPtToAV() {
        System.out.println(" # people before conversion to AV: " + population.getPersons().size());

        if (population == null) {
            population = originalScenario.getPopulation();
        }
        population = new ModeConverter(population).run();
        new PopulationWriter(population).write(reducedScenarioDirectory + "/reducedPopulation.xml");
        System.out.println(" # people after conversion to AV: " + population.getPersons().size());

    }

    public void addConfigFilesinFolder() throws IOException {
        // TODO Improve Copy Mechanism

        // TODO @ LUKAS Change ConfigFile Such that Filenames are already correct
        copyFileWorkDir2ReducedScenarioDir(simOptions.getString("simuConfig"));

        copyFileWorkDir2ReducedScenarioDir("av.xml");

        copyFileWorkDir2ReducedScenarioDir(ScenarioOptions.getOptionsFileName());

    }

    private void copyFileWorkDir2ReducedScenarioDir(String filename) throws IOException {
        Path src = new File(workingDirectory + "/" + filename).toPath();
        System.out.println(src);

        Path dest = new File(workingDirectory + "/" + reducedScenarioDirectory + "/" + filename).toPath();
        System.out.println(dest);

        if (new File(src.toString()).exists() && !new File(dest.toString()).exists()) {
            Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("added " + filename + "  into folder " + dest);
        } else {
            System.out.println("could not find " + filename);
        }

    }

    public Scenario getOriginalScenario() {
        return this.originalScenario;
    }

    public Network getNetwork() {
        return this.network;
    }

    public Population getPopulation() {
        return this.population;
    }

    

}
