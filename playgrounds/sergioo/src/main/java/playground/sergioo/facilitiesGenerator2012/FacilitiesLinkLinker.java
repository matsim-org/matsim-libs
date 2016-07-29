package playground.sergioo.facilitiesGenerator2012;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityFacilityImpl;
import org.matsim.facilities.FacilitiesWriter;
import org.matsim.facilities.MatsimFacilitiesReader;

import java.util.HashSet;

public class FacilitiesLinkLinker {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimFacilitiesReader((MutableScenario) scenario).readFile(args[0]);
		new MatsimNetworkReader(scenario.getNetwork()).readFile(args[1]);
		TransportModeNetworkFilter filter = new TransportModeNetworkFilter(scenario.getNetwork());
		Network net = NetworkUtils.createNetwork();
		HashSet<String> modes = new HashSet<String>();
		modes.add(TransportMode.car);
		filter.filter(net, modes);
		for(ActivityFacility facility:((MutableScenario)scenario).getActivityFacilities().getFacilities().values())
			((ActivityFacilityImpl)facility).setLinkId(NetworkUtils.getNearestLink(net, facility.getCoord()).getId());
		new FacilitiesWriter(((MutableScenario)scenario).getActivityFacilities()).write(args[2]);
	}

}
