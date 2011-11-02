package playground.gregor;

import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.run.NetworkCleaner;
import org.matsim.utils.gis.matsim2esri.network.Links2ESRIShape;
import org.matsim.utils.gis.matsim2esri.plans.SelectedPlans2ESRIShape;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public class Startup {


	public static void main(String [] args) {

		String [] argsII = {"/Users/laemmel/devel/dfg/input/network.xml","/Users/laemmel/devel/dfg/raw_input/networkL.shp","/Users/laemmel/devel/dfg/raw_input/networkP.shp","EPSG:3395"};
		Links2ESRIShape.main(argsII);


	}

}
