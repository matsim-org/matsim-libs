package playground.florian.gtfsTests;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.transformations.WGS84ToMercator;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.core.utils.misc.RouteUtils;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.vehicles.VehicleReaderV1;
import org.matsim.vehicles.Vehicles;

import playground.florian.GTFSConverter.GtfsConverter;

public class GtfsTests extends MatsimTestCase {
	
	
	public void testGtfsStandardConversion(){
		GtfsConverter gtfs = new GtfsConverter(getPackageInputDirectory(), new WGS84ToMercator.Project(18));
		// The WE-Trip is added on July 11th 2011, so calendar.txt and calendar_dates.txt can be checked
		gtfs.setDate(20110711);
		gtfs.convert();
		ScenarioImpl scenario = (ScenarioImpl) gtfs.getScenario(); 
		
		// The Conversion is done, now read the checked scenario
		Config checkedConfig = ConfigUtils.loadConfig(this.getPackageInputDirectory()+ "/checked/config.xml");
		ScenarioImpl checkedScenario = (ScenarioImpl)(ScenarioUtils.createScenario(checkedConfig));
		new MatsimNetworkReader(checkedScenario).readFile(this.getPackageInputDirectory()+ "/checked/network.xml");
		new VehicleReaderV1(checkedScenario.getVehicles()).readFile(this.getPackageInputDirectory()+ "/checked/transitVehicles.xml");
		new TransitScheduleReader(checkedScenario).readFile(this.getPackageInputDirectory()+ "/checked/transitSchedule.xml");
		
		this.compareResults(checkedScenario, scenario);		
	}
	
	public void testGtfsShapedConversion(){
		GtfsConverter gtfs = new GtfsConverter(getPackageInputDirectory(), new WGS84ToMercator.Project(18));
		// The WE-Trip is added on July 11th 2011, so calendar.txt and calendar_dates.txt can be checked
		gtfs.setDate(20110711);
		gtfs.setCreateShapedNetwork(true);
		gtfs.convert();
		ScenarioImpl scenario = (ScenarioImpl) gtfs.getScenario(); 
		
		// The Conversion is done, now read the checked scenario
		Config checkedConfig = ConfigUtils.loadConfig(this.getPackageInputDirectory()+ "/checked/config_shaped.xml");
		ScenarioImpl checkedScenario = (ScenarioImpl)(ScenarioUtils.createScenario(checkedConfig));
		new MatsimNetworkReader(checkedScenario).readFile(this.getPackageInputDirectory()+ "/checked/network_shaped.xml");
		new VehicleReaderV1(checkedScenario.getVehicles()).readFile(this.getPackageInputDirectory()+ "/checked/transitVehicles.xml");
		new TransitScheduleReader(checkedScenario).readFile(this.getPackageInputDirectory()+ "/checked/transitSchedule_shaped.xml");
		
		this.compareResults(checkedScenario, scenario);	
	}
	
	private void compareResults(ScenarioImpl expected, ScenarioImpl actual){
		this.compareConfigs(expected, actual);
		this.compareNetworks(expected, actual);
		this.compareTransitVehicles(expected, actual);
		this.compareTransitSchedules(expected, actual);
	}

	private void compareTransitSchedules(ScenarioImpl sc1, ScenarioImpl sc2) {
		TransitSchedule ts1 = sc1.getTransitSchedule();
		TransitSchedule ts2 = sc2.getTransitSchedule();
		assertEquals(ts1.getFacilities().size(),ts2.getFacilities().size());
		for(Id stopId: ts1.getFacilities().keySet()){
			assertEquals(ts1.getFacilities().get(stopId).getName(),ts2.getFacilities().get(stopId).getName());
			assertEquals(ts1.getFacilities().get(stopId).getCoord(),ts2.getFacilities().get(stopId).getCoord());
			assertEquals(ts1.getFacilities().get(stopId).getLinkId(),ts2.getFacilities().get(stopId).getLinkId());			
		}
		assertEquals(ts1.getTransitLines().size(),ts2.getTransitLines().size());
		for(Id transitId: ts1.getTransitLines().keySet()){
			assertEquals(ts1.getTransitLines().get(transitId).getRoutes().size(),ts1.getTransitLines().get(transitId).getRoutes().size());
			for(Id routeId: ts1.getTransitLines().get(transitId).getRoutes().keySet()){
				TransitRoute tr1 = ts1.getTransitLines().get(transitId).getRoutes().get(routeId);
				TransitRoute tr2 = ts2.getTransitLines().get(transitId).getRoutes().get(routeId);
				assertEquals(tr1.getStops().size(),tr2.getStops().size());
				assertEquals(tr1.getDepartures().size(),tr2.getDepartures().size());
				assertEquals(tr1.getRoute().getLinkIds().size(),tr2.getRoute().getLinkIds().size());
				assertEquals(RouteUtils.calcDistance(tr1.getRoute(), sc1.getNetwork()),RouteUtils.calcDistance(tr2.getRoute(), sc2.getNetwork()));
			}
		}
	}

	private void compareTransitVehicles(ScenarioImpl sc1, ScenarioImpl sc2) {
		Vehicles v1 = sc1.getVehicles();
		Vehicles v2 = sc2.getVehicles();
		assertEquals(v1.getVehicles().size(),v2.getVehicles().size());
		assertEquals(v1.getVehicleTypes().size(),v2.getVehicleTypes().size());
	}

	private void compareNetworks(ScenarioImpl sc1, ScenarioImpl sc2) {
		Network n1 = sc1.getNetwork();
		Network n2 = sc2.getNetwork();
		assertEquals(n1.getLinks().size(), n2.getLinks().size());
		assertEquals(n1.getNodes().size(), n2.getNodes().size());
	}

	private void compareConfigs(ScenarioImpl sc1,ScenarioImpl sc2) {
		Config c1 = sc1.getConfig();
		Config c2 = sc2.getConfig();
		assertEquals(c1.transit().getTransitModes(),c2.transit().getTransitModes());
		assertEquals(c1.getQSimConfigGroup().getStartTime(),c2.getQSimConfigGroup().getStartTime());		
	}

}
