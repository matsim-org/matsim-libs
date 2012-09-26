package org.matsim.contrib.freight.mobsim;

import org.junit.Ignore;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.freight.carrier.CarrierPlanReader;
import org.matsim.contrib.freight.carrier.Carriers;
import org.matsim.testcases.MatsimTestCase;

@Ignore
public class CarrierAgentTest extends MatsimTestCase{
	
	CarrierAgentTracker carrierAgentTracker;
	
	Carriers carriers;
	
	Network network;
	
	public void setUp() throws Exception{
		super.setUp();
		
		carriers = new Carriers();
		new CarrierPlanReader(carriers).read(getInputDirectory() + "carrierPlansEquils.xml");
		
//		network = Scenario
//		getInputDirectory() + "network.xml"

	}

	
	public void testCreatesFreightDriverPlansCorrectly(){
		
	}

}
