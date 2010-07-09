package playground.wrashid.parkingSearch.planLevel;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.controler.Controler;
import org.matsim.core.facilities.MatsimFacilitiesReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationReader;
import org.matsim.testcases.MatsimTestCase;

public class LinkFacilityAssociationTest extends MatsimTestCase {
/*
	public void testConstructor(){   
		Controler controler;
		String configFilePath="test/input/playground/wrashid/parkingSearch/planLevel/chessConfig2.xml";
		controler = new Controler(configFilePath);
	
		
		controler.setOverwriteFiles(true);
		
		controler.run();
		
		LinkFacilityAssociation lfa=new LinkFacilityAssociation(controler);
		
		assertEquals("1", lfa.getFacilities(new IdImpl("1")).get(0).getId().toString());
		
		
		
		
		
	}	
	*/
	
	public void testConstructor(){
		ScenarioImpl sc = new ScenarioImpl();
		
		String facilitiesPath = "test/input/playground/wrashid/parkingSearch/planLevel/chessFacilities.xml";
		String networkFile = "test/input/playground/wrashid/parkingSearch/planLevel/network.xml";

		new MatsimFacilitiesReader(sc).readFile(facilitiesPath);

		Population inPop = sc.getPopulation();

		NetworkLayer net = sc.getNetwork();
		new MatsimNetworkReader(sc).readFile(networkFile);

		
		LinkFacilityAssociation lfa=new LinkFacilityAssociation(sc.getActivityFacilities(),net);
		
		assertEquals("19", lfa.getFacilities(new IdImpl("1")).get(0).getId().toString());
		assertEquals(1, lfa.getFacilities(new IdImpl("1")).size());
	}
	
}
