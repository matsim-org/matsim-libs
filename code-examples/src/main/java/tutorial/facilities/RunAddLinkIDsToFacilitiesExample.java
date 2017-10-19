package tutorial.facilities;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.SearchableNetwork;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.FacilitiesUtils;

public class RunAddLinkIDsToFacilitiesExample {
	
	public static void main( String [] args ) {
	
		Config config = ConfigUtils.loadConfig( args[0] ) ;
		
		Scenario scenario = ScenarioUtils.loadScenario( config ) ;
		
		for ( ActivityFacility fac : scenario.getActivityFacilities().getFacilities().values() ) {
			if ( fac.getLinkId()==null ) {
				final Coord coord = fac.getCoord();
				Gbl.assertNotNull(coord);
				Link link = NetworkUtils.getNearestLink( scenario.getNetwork(), coord) ;
				Gbl.assertNotNull(link);
				FacilitiesUtils.setLinkID(fac, link.getId() );
			}
		}
		
		
	}

}
