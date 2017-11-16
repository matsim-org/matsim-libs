package playground.lsieber.scenario.reducer;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.HashSet;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.facilities.ActivityFacilities;

import playground.lsieber.networkshapecutter.FacilityPopulationBasedCutter;
import playground.lsieber.networkshapecutter.NetworkCutterShape;

public class ShapeScenarioReducer extends AbstractScenarioReducer {

    public ShapeScenarioReducer() throws IOException {
        // TODO Auto-generated constructor stub
        super();
    }

    @Override
    protected Network networkCutter() throws MalformedURLException, IOException {
        // TODO @ lukas Decouple Modes and Nettworkcutting

        HashSet<String> modes = new HashSet<String>();
        modes.add("car");
        modes.add("pt");
        modes.add("tram");
        modes.add("bus");

        // TODO @ Lukas Implenment Shapefile from IDSC Options
        File shapefile = new File("shapefiles/Export_Output_2.shp");

        return new NetworkCutterShape(shapefile).filter(this.originalScenario.getNetwork(), modes);
    }

    @Override
    protected Population populationCutter() {
        // TODO @ Lukas generate Population Cutter
        return originalScenario.getPopulation();
    }

    @Override
    protected ActivityFacilities facilitiesCutter() {
        return new FacilityPopulationBasedCutter(this.population).filter(originalScenario.getActivityFacilities());
    }

}
