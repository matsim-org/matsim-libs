package playground.pieter.singapore.utils.plans;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.population.routes.RouteFactoryImpl;
import org.matsim.core.router.FastAStarLandmarks;
import org.matsim.core.router.util.FastAStarLandmarksFactory;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.facilities.ActivityFacility;
import org.matsim.vehicles.Vehicle;

import others.sergioo.util.dataBase.*;

class PlanFindLegDistances {
	private final MutableScenario scenario;
	private final Map<Id<ActivityFacility>, ? extends ActivityFacility> facilities;
	private final NetworkImpl network;
	private final RouteFactoryImpl routeFactory;
	private final DataBaseAdmin dba;
	private final FastAStarLandmarks leastCostPathCalculator;

	public PlanFindLegDistances(Scenario scenario, DataBaseAdmin dba) {
		super();
		this.scenario = (MutableScenario) scenario;
		this.network = (NetworkImpl) scenario.getNetwork();
		this.facilities = this.scenario.getActivityFacilities().getFacilities();
		TravelDisutility travelMinCost = new TravelDisutility() {

			@Override
			public double getLinkTravelDisutility(Link link, double time,
					Person person, Vehicle vehicle) {
				// TODO Auto-generated method stub
				return getLinkMinimumTravelDisutility(link);
			}

			@Override
			public double getLinkMinimumTravelDisutility(Link link) {
				// TODO Auto-generated method stub
				return link.getLength();
			}
		};

		TravelTime timeFunction = new TravelTime() {


			public double getLinkTravelTime(Link link, double time, Person person, Vehicle vehicle) {
				return link.getLength() / link.getFreespeed();
			}
		};

		LeastCostPathCalculatorFactory routerFactory = new FastAStarLandmarksFactory(network, travelMinCost);
		leastCostPathCalculator = (FastAStarLandmarks) routerFactory.createPathCalculator(network, travelMinCost, timeFunction);
		this.dba = dba;
		routeFactory = ((PopulationFactoryImpl) scenario.getPopulation()
				.getFactory()).getRouteFactory();
	}

	public double getShortestPathDistance(Coord startCoord, Coord endCoord) {
		double distance = 0;
		Node startNode = network.getNearestNode(startCoord);
		Node endNode = network.getNearestNode(endCoord);

		Path path = leastCostPathCalculator.calcLeastCostPath(startNode,
				endNode, 0, null, null);
		for (Link l : path.links) {
			distance += l.getLength();
		}
		return distance;
	}

	public double getStraightLineDistance(Coord startCoord,
			Coord endCoord) {
		double x1 = startCoord.getX();
		double x2 = endCoord.getX();
		double y1 = startCoord.getY();
		double y2 = endCoord.getY();
		double xsq = Math.pow(x2 - x1, 2);
		double ysq = Math.pow(y2 - y1, 2);
		return (Math.sqrt(xsq + ysq));
	}
	
	public void run() throws SQLException, NoConnectionException {
//		dba.executeStatement("DROP TABLE IF EXISTS hits.synthpoptripdistances;");
//		dba.executeStatement("CREATE TABLE hits.synthpoptripdistances (id int, fromAct varchar(50), toAct varchar(50), travelMode varchar(50), distance double, beelineDistance double);");
		
		ResultSet rs = dba.executeQuery("select distinct `id` from hits.synthpoptripdistances" );
		rs.beforeFirst();
		LinkedHashSet<Integer> procids = new LinkedHashSet<>();
		
		while(rs.next()){
			procids.add(rs.getInt(1));
		}
		Collection<? extends Person> persons = scenario.getPopulation()
				.getPersons().values();
		int j = 1;
		for (Person pax : persons) {
			if(procids.contains(Integer.parseInt(pax.getId().toString()))){
				
				j++;
				continue;
			}else{
				System.out.println("1");
			}
				
			
			if (pax.getPlans().size() == 0)
				continue;
			PlanImpl plan = (PlanImpl) pax.getPlans().get(0);
//			new TransitActsRemover().run(plan);
			for (int i = 0; i < plan.getPlanElements().size(); i += 2) {
				ActivityImpl act = (ActivityImpl) plan.getPlanElements().get(i);
				if (act.equals(plan.getLastActivity()))
					break;
				LegImpl leg =  (LegImpl) plan.getPlanElements()
						.get(i + 1);
				ActivityImpl nextact = (ActivityImpl) plan.getPlanElements()
						.get(i + 2);
				Coord startCoord = facilities.get(act.getFacilityId())
						.getCoord();
				Coord endCoord = facilities.get(nextact.getFacilityId())
						.getCoord();
//				dba.executeUpdate(String
//						.format("INSERT INTO hits.synthpoptripdistances VALUES (%d,\'%s\',\'%s\',\'%s\',%f, %f);",
//								Integer.parseInt(pax.getId().toString()),
//								act.getType(), nextact.getType(), leg.getMode(),
//								getShortestPathDistance(startCoord, endCoord),
//								getStraightLineDistance(startCoord, endCoord)));
			}
			j++;
			if (j % 1000 == 0)
				System.out.println(j + " agents processed");
		}
		System.out.println(j + " agents processed");

	}
}
