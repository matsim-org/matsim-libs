package sensitivity.scenarios;

import java.util.Arrays;

import jsprit.core.problem.VehicleRoutingProblem;
import jsprit.core.problem.io.VrpXMLWriter;
import jsprit.core.problem.vehicle.VehicleImpl;
import jsprit.core.problem.vehicle.VehicleTypeImpl;
import jsprit.core.util.Coordinate;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.freight.utils.MatsimStuffLoader;
import org.matsim.contrib.freight.utils.MatsimStuffLoader.MatsimStuff;
import org.matsim.core.basic.v01.IdImpl;

import sensitivity.scenarios.ServiceScenarioCreator.BoundingDemand;
import sensitivity.scenarios.ServiceScenarioCreator.BoundingGeoByLink;

public class RoutingScenarioCreator {
	
	public static void main(String[] args) {
		
		VehicleRoutingProblem.Builder vrpBuilder = VehicleRoutingProblem.Builder.newInstance();
		
		MatsimStuff matsimStuff = MatsimStuffLoader.loadNetworkAndGetStuff("sensitivity/network.xml");
		
		new ServiceScenarioCreator(matsimStuff.getNetwork(),new BoundingGeoByLink(makeId(600),makeId(5695)),100)
			.setBoundingDemand(new BoundingDemand(1,10))
			.setTimeWindowLengths(Arrays.asList(0.2*3600), 1*3600, 2*3600)
			.setServiceTime(10*60)
			.createAndLoad(vrpBuilder);
		
		VehicleTypeImpl vType = VehicleTypeImpl.Builder.newInstance("type1").addCapacityDimension(0, 40).setCostPerDistance(1.0).setFixedCost(100).build();
		Coord vCoord = matsimStuff.getNetwork().getLinks().get(makeId(25594)).getCoord();
		VehicleImpl v = VehicleImpl.Builder.newInstance("v1")
				.setEarliestStart(0.0).setLatestArrival(24*3600)
				.setStartLocationId("25594")
				.setStartLocationCoordinate(Coordinate.newInstance(vCoord.getX(), vCoord.getY()))
				.setType(vType)
				.build();
		
		vrpBuilder.addVehicle(v);
		
		VehicleRoutingProblem vrp = vrpBuilder.build();
		
		new VrpXMLWriter(vrp).write("sensitivity/vrp_tight_tw.xml");
	}

	

	private static Id makeId(int i) {
		return new IdImpl(i);
	}

}
