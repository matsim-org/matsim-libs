package playground.andreas.P2.scoring;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.events.EventsFactoryImpl;
import org.matsim.testcases.MatsimTestUtils;

import playground.andreas.P2.PScenarioHelper;
import playground.andreas.P2.helper.PConfigGroup;


public class ScorePlansHandlerTest {
	@Rule public MatsimTestUtils utils = new MatsimTestUtils();
	
	@Test
    public final void testScoreContainer() {
		Network net = PScenarioHelper.createTestNetwork().getNetwork();
		EventsFactoryImpl eF = new EventsFactoryImpl();
		PConfigGroup pC = new PConfigGroup();
		pC.addParam("costPerVehicleAndDay", "40.0");
		pC.addParam("earningsPerKilometerAndPassenger", "0.20");
		pC.addParam("costPerKilometer", "0.30");
		
		ScorePlansHandler handler = new ScorePlansHandler(pC);
		handler.init(net);
		
		Id driverId = new IdImpl("drv_1");
		Id vehicleId = new IdImpl("veh_1");
		Id personId = new IdImpl("p_1");
		Id transitLineId = new IdImpl("A");
		Id transitRouteId = new IdImpl("123");
		Id departureId = new IdImpl("dep_1");
		
		ScoreContainer sC;
		
		handler.handleEvent(eF.createTransitDriverStartsEvent(0.0, driverId, vehicleId, transitLineId, transitRouteId, departureId));
		sC = handler.getDriverId2ScoreMap().get(vehicleId);
		Assert.assertEquals("revenue with zero trips served", -40.0, sC.getTotalRevenue(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("trips served", 0, sC.getTripsServed(), MatsimTestUtils.EPSILON);
		
		handler.handleEvent(eF.createLinkEnterEvent(0.0, driverId, new IdImpl("1112"), vehicleId));
		sC = handler.getDriverId2ScoreMap().get(vehicleId);
		Assert.assertEquals("revenue with zero trips served", -40.36, sC.getTotalRevenue(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("trips served", 0, sC.getTripsServed(), MatsimTestUtils.EPSILON);
		
		handler.handleEvent(eF.createPersonEntersVehicleEvent(0.0, personId, vehicleId));
		sC = handler.getDriverId2ScoreMap().get(vehicleId);
		Assert.assertEquals("revenue with zero trips served", -40.36, sC.getTotalRevenue(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("trips served", 0, sC.getTripsServed(), MatsimTestUtils.EPSILON);
		
		handler.handleEvent(eF.createLinkEnterEvent(0.0, driverId, new IdImpl("1211"), vehicleId));
		sC = handler.getDriverId2ScoreMap().get(vehicleId);
		Assert.assertEquals("revenue with zero trips served", -40.72, sC.getTotalRevenue(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("trips served", 0, sC.getTripsServed(), MatsimTestUtils.EPSILON);
		
		handler.handleEvent(eF.createPersonLeavesVehicleEvent(0.0, personId, vehicleId));
		sC = handler.getDriverId2ScoreMap().get(vehicleId);
		Assert.assertEquals("revenue with zero trips served", -40.72, sC.getTotalRevenue(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("trips served", 0, sC.getTripsServed(), MatsimTestUtils.EPSILON);
		
		handler.reset(10);
		sC = handler.getDriverId2ScoreMap().get(vehicleId);
		Assert.assertNull("There is no score after the reset is triggered", sC);
	}
}