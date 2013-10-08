package playground.mzilske.cdr;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.router.PlanRouter;
import org.matsim.core.router.TripRouter;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.population.algorithms.ParallelPersonAlgorithmRunner;
import org.matsim.population.algorithms.ParallelPersonAlgorithmRunner.PersonAlgorithmProvider;
import org.matsim.population.algorithms.PersonAlgorithm;

import playground.mzilske.cdr.ZoneTracker.LinkToZoneResolver;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

import d4d.NetworkRoutingModule;
import d4d.Sighting;
import d4d.Sightings;

public class PopulationFromSightings {
	

	private static Random rnd = MatsimRandom.getRandom();
	
	public static void readSampleWithOneRandomPointForEachSightingInNewCell(Scenario scenario, LinkToZoneResolver zones, final Map<Id, List<Sighting>> sightings) throws FileNotFoundException {
		Map<Activity, String> cellsOfSightings;
		cellsOfSightings = new HashMap<Activity, String>();
		for (Entry<Id, List<Sighting>> sightingsPerPerson : sightings.entrySet()) {
			for (Sighting sighting : sightingsPerPerson.getValue()) {
				String zoneId = sighting.getCellTowerId();
				Activity activity = createActivityInZone(scenario, zones,
						zoneId);
				cellsOfSightings.put(activity, zoneId);
				activity.setEndTime(sighting.getTime());
				Id personId = sightingsPerPerson.getKey();
				Person person = scenario.getPopulation().getPersons().get(personId);
				if (person == null) {
					person = scenario.getPopulation().getFactory().createPerson(personId);
					person.addPlan(scenario.getPopulation().getFactory().createPlan());
					person.getSelectedPlan().addActivity(activity);
					scenario.getPopulation().addPerson(person);
				} else {
					Activity lastActivity = (Activity) person.getSelectedPlan().getPlanElements().get(person.getSelectedPlan().getPlanElements().size()-1);
					if ( !(zoneId.equals(cellsOfSightings.get(lastActivity))) ) {
						Leg leg = scenario.getPopulation().getFactory().createLeg("unknown");
						person.getSelectedPlan().addLeg(leg);
						person.getSelectedPlan().addActivity(activity);
					} else {
						lastActivity.setEndTime(sighting.getTime());
					}
				}
			}
		}
	}


	
	public static Activity createActivityInZone(Scenario scenario, LinkToZoneResolver zones, String zoneId) {
		Activity activity = scenario.getPopulation().getFactory().createActivityFromLinkId("sighting", zones.chooseLinkInZone(zoneId));
		return activity;
	}

	
	
	public static void preparePopulation(final ScenarioImpl scenario, final Zones zones, final Map<Id, List<Sighting>> allSightings) {
		ParallelPersonAlgorithmRunner.run(scenario.getPopulation(), 8, new org.matsim.population.algorithms.XY2Links(scenario));
		ParallelPersonAlgorithmRunner.run(scenario.getPopulation(), 8, new PersonAlgorithmProvider() {

			@Override
			public PersonAlgorithm getPersonAlgorithm() {
				TripRouter tripRouter = new TripRouter();
				tripRouter.setRoutingModule("unknown", new NetworkRoutingModule(scenario.getPopulation().getFactory(), (NetworkImpl) scenario.getNetwork(), new FreeSpeedTravelTime()));
				return new PlanRouter(tripRouter);
			}

		});
		
		Population unfeasiblePeople = new PopulationImpl(scenario);
		
		for (int i=0; i<0; i++) {
			unfeasiblePeople = new PopulationImpl(scenario);
			for (Person person : scenario.getPopulation().getPersons().values()) {
				Plan plan = person.getSelectedPlan();
				if (!isFeasible(plan)) {
					unfeasiblePeople.addPerson(person);
				}
			}
			System.out.println("Unfeasible plans: " + unfeasiblePeople.getPersons().size() + " of " +scenario.getPopulation().getPersons().size());	
			
			ParallelPersonAlgorithmRunner.run(unfeasiblePeople, 8, new PersonAlgorithm() {

				@Override
				public void run(Person person) {
					Sightings sightingsForThisAgent = new Sightings(allSightings.get(person.getId()));
					for (PlanElement planElement : person.getSelectedPlan().getPlanElements()) {
						if (planElement instanceof Activity) {
							Sighting sighting = sightingsForThisAgent.sightings.next();
							ActivityImpl activity = (ActivityImpl) planElement;
							activity.setLinkId(null);
							Geometry cell = zones.getCell(sighting.getCellTowerId());
							Point p = getRandomPointInFeature(rnd, cell);
							Coord newCoord = new CoordImpl(p.getX(), p.getY());
							activity.setCoord(newCoord);
						}
					}
				}
				
			});

			ParallelPersonAlgorithmRunner.run(unfeasiblePeople, 8, new org.matsim.population.algorithms.XY2Links(scenario));
			
			
			ParallelPersonAlgorithmRunner.run(unfeasiblePeople, 8, new PersonAlgorithmProvider() {

				@Override
				public PersonAlgorithm getPersonAlgorithm() {
					TripRouter tripRouter = new TripRouter();
					tripRouter.setRoutingModule("car", new NetworkRoutingModule(scenario.getPopulation().getFactory(), (NetworkImpl) scenario.getNetwork(), new FreeSpeedTravelTime()));
					return new PlanRouter(tripRouter);
				}

			});
			
//			ParallelPersonAlgorithmRunner.run(unfeasiblePeople, 8, new PersonAlgorithmProvider() {
//				
//				@Override
//				public PersonAlgorithm getPersonAlgorithm() {
//					TripRouter tripRouter = new TripRouter();
//					tripRouter.setRoutingModule("unknown", new BushwhackingRoutingModule(scenario.getPopulation().getFactory(), (NetworkImpl) scenario.getNetwork()));
//					return new PlanRouter(tripRouter);
//				}
//	
//			});
			
		}
		
		for (Person person : unfeasiblePeople.getPersons().values()) {
			((PopulationImpl) scenario.getPopulation()).getPersons().remove(person.getId());
		}
	}


	private static Point getRandomPointInFeature(Random rnd, Geometry ft) {
		Point p = null;
		double x, y;
		do {
			x = ft.getEnvelopeInternal().getMinX() + rnd.nextDouble() * (ft.getEnvelopeInternal().getMaxX() - ft.getEnvelopeInternal().getMinX());
			y = ft.getEnvelopeInternal().getMinY() + rnd.nextDouble() * (ft.getEnvelopeInternal().getMaxY() - ft.getEnvelopeInternal().getMinY());
			p = MGC.xy2Point(x, y);
		} while (!ft.contains(p));
		return p;
	}

	private static boolean isFeasible(Plan plan) {
		double currentTime = 0.0;
		for (PlanElement planElement : plan.getPlanElements()) {
			if (planElement instanceof Leg) {
				LegImpl leg = (LegImpl) planElement;
				double arrivalTime = leg.getArrivalTime();
				currentTime = arrivalTime;
			} else if (planElement instanceof Activity) {
				ActivityImpl activity = (ActivityImpl) planElement;
				double sightingTime = activity.getEndTime();
				if (sightingTime < currentTime) {
					return false;
				}
			}
		}
		return true;
	}
	
}
