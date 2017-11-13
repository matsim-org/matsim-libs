/**
 * 
 */
package playground.clruch.io.fleet;

import java.io.File;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.Route;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlansConfigGroup;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.io.PopulationWriter;
import org.matsim.core.population.routes.GenericRouteFactory;
import org.matsim.core.population.routes.RouteFactory;
import org.matsim.core.scenario.ScenarioUtils;

import ch.ethz.idsc.queuey.datalys.MultiFileTools;
import ch.ethz.idsc.queuey.util.GZHandler;
import playground.clruch.ScenarioOptions;
import playground.clruch.net.DummyStorageSupplier;
import playground.clruch.net.IterationFolder;
import playground.clruch.net.RequestContainer;
import playground.clruch.net.SimulationObject;
import playground.clruch.net.StorageSupplier;
import playground.clruch.net.StorageUtils;
import playground.clruch.utils.PropertiesExt;
import playground.sebhoerl.avtaxi.framework.AVConfigGroup;

/** @author Andreas Aumiller */
public class PopulationCreator {

    public static int STEPSIZE = 10; // TODO this should be derived from storage files
    private static StorageUtils storageUtils;
    private static File[] outputFolders;
    private static String[] outputFolderNames;

    public static void main(String[] args) throws Exception {
        // Loading simulationObjects
        File workingDirectory = MultiFileTools.getWorkingDirectory();
        PropertiesExt simOptions = PropertiesExt.wrap(ScenarioOptions.load(workingDirectory));
        File outputDirectory = new File(workingDirectory, simOptions.getString("visualizationFolder"));
        System.out.println("INFO getting all output folders from: " + outputDirectory.getAbsolutePath());
        outputFolders = MultiFileTools.getAllDirectoriesSorted(outputDirectory);
        outputFolderNames = new String[outputFolders.length];
        for (int i = 0; i < outputFolders.length; ++i) {
            outputFolderNames[i] = outputFolders[i].getName();
        }
        storageUtils = new StorageUtils(new File(outputDirectory, outputFolderNames[0]));

        // Initialize ConfigGroups and Files
        System.out.println("INFO loading simulation configuration");
        DvrpConfigGroup dvrpConfigGroup = new DvrpConfigGroup();
        File configFile = new File(workingDirectory, simOptions.getString("simuConfig"));
        Config config = ConfigUtils.loadConfig(configFile.toString(), new AVConfigGroup(), dvrpConfigGroup);
        Scenario scenario = ScenarioUtils.loadScenario(config);
        Network network = scenario.getNetwork();
        PlansConfigGroup plansConfigGroup = new PlansConfigGroup();

        // Create new population
        System.out.println("INFO creating new population file");
        Population population = PopulationUtils.createPopulation(plansConfigGroup, network);
        populate(population);

        // Write new population to file
        final File populationFile = new File(workingDirectory, "TestPopulation.xml");
        final File populationGzFile = new File(workingDirectory, "TestPopulation.xml.gz");

        // write the modified population to file
        System.out.println("INFO writing new population to:");
        System.out.println(populationFile.getPath());
        System.out.println(populationGzFile.getPath());
        PopulationWriter pw = new PopulationWriter(population);
        pw.write(populationGzFile.toString());

        // extract the created .gz file
        GZHandler.extract(populationGzFile, populationFile);
    }

    private static void populate(Population population) throws Exception {
        // Parse RequestContainer into population
        List<IterationFolder> list = storageUtils.getAvailableIterations();
        if (list.isEmpty() != true) {
            StorageSupplier storageSupplier = new DummyStorageSupplier();
            System.out.println("INFO initializing factories and properties");
            PopulationFactory populationFactory = population.getFactory();
            int id = 0;
            for (IterationFolder iter : list) {
                storageSupplier = iter.storageSupplier;
                final int MAX_ITER = 100; // storageSupplier.size()
                for (int index = 0; index < MAX_ITER; index++) {
                    SimulationObject simulationObject = storageSupplier.getSimulationObject(index);
                    List<RequestContainer> rc = simulationObject.requests;

                    // Initialize all necessary properties
                    for (RequestContainer request : rc) {
                        Id<Person> personID = Id.create(id, Person.class);
                        Person person = populationFactory.createPerson(personID);
                        Plan plan = populationFactory.createPlan();
                        Id<Link> fromLinkID = Id.create(request.fromLinkIndex, Link.class);
                        Id<Link> toLinkID = Id.create(request.toLinkIndex, Link.class);
                        Activity activity = populationFactory.createActivityFromLinkId("activitiy", fromLinkID);
                        Leg leg = populationFactory.createLeg("av");
                        RouteFactory rf = new GenericRouteFactory();
                        Route route = rf.createRoute(fromLinkID, toLinkID);
                        leg.setDepartureTime(request.submissionTime);
                        // leg.setTravelTime(200);
                        leg.setRoute(route);
                        id++;

                        // Add person to the population
                        System.out.println("INFO Adding person ID " + id + " to population");
                        plan.addActivity(activity);
                        plan.addLeg(leg);
                        person.addPlan(plan);
                        population.addPerson(person);
                    }
                }
            }
        }
    }

}
