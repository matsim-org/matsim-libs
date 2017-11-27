package playground.lsieber.scenario.reducer;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.facilities.ActivityFacilities;

import ch.ethz.idsc.owly.data.GlobalAssert;
import playground.lsieber.networkshapecutter.FacilityPopulationBasedCutter;
import playground.lsieber.networkshapecutter.NetworkCutterShape;
import playground.lsieber.networkshapecutter.PopulationCutterShape;
import playground.lsieber.networkshapecutter.PopulationFilter;

public class ShapeScenarioReducer extends AbstractScenarioReducer {

    protected Network targetAreaNetwork;

    public ShapeScenarioReducer() throws IOException {
        super();
    }

    @Override
    protected Network networkCutter() throws MalformedURLException, IOException {
        // TODO @ lukas Decouple Modes and Nettworkcutting

        Set<String> modes = new HashSet<>();
        // modes.add("car");
        modes.add("pt");
        // modes.add("tram");
        // modes.add("bus");

        // TODO @ Lukas Implenment Shapefile from IDSC Options
        File shapefileAvAccessArea = new File("shapefiles/AvAccess.shp");
        return new NetworkCutterShape(shapefileAvAccessArea).filter(originalScenario.getNetwork(), modes);
    }

    @Override
    protected Population populationCutter() {
        GlobalAssert.that(network.getNodes().size() > 0);

        Population populationAVAccessArea = new PopulationCutterShape().elminateOutsideNetwork(this.originalScenario.getPopulation(), this.network);

        return populationAVAccessArea;
    }

    @Override
    protected ActivityFacilities facilitiesCutter() {
        return new FacilityPopulationBasedCutter(this.population).filter(originalScenario.getActivityFacilities());
    }

    protected void setTargetNetwork() throws MalformedURLException, IOException {
        Set<String> modes = new HashSet<String>();
        // modes.add("car");
        modes.add("pt");
        modes.add("tram");
        modes.add("bus");

        File shapefileTargetArea = new File("shapefiles/TargetArea.shp");
        targetAreaNetwork = new NetworkCutterShape(shapefileTargetArea).filter(originalScenario.getNetwork(), modes);
    }

    protected void filterPopulationOfTargetAreaOnlyPt() throws MalformedURLException, IOException {
        System.out.println(" # people before Target Area: " + population.getPersons().size());
        this.setTargetNetwork();
        GlobalAssert.that(population != null && targetAreaNetwork != null);
        PopulationFilter populationFilter = new PopulationFilter(population);
        populationFilter.run(targetAreaNetwork);
        System.out.println(" # people after Target Area: " + population.getPersons().size());

    }
}
