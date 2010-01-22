package playground.wrashid.PSF.converter.addingParkings;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.testcases.MatsimTestCase;

public class GenerateParkingFacilitiesTest extends MatsimTestCase {

	/**
	 * test, that the number of created facilities corresponds to what is expected.
	 */
	public void testGenerateParkingFacilities(){
		
		String inputPlansFile = getPackageInputDirectory() + "plans2.xml";
		String networkFile = "test/scenarios/berlin/network.xml.gz";

		ScenarioImpl scenario = new ScenarioImpl();
		new MatsimNetworkReader(scenario).readFile(networkFile);
		new MatsimPopulationReader(scenario).readFile(inputPlansFile);
		
		GenerateParkingFacilities.generateParkingFacilties(scenario);
		
		ActivityFacilitiesImpl facilities = scenario.getActivityFacilities();
		
		assertEquals(4, facilities.getFacilities().size());
	}
	
}
