package playground.johannes.gsv.demand.tasks;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.common.collections.CollectionUtils;
import org.matsim.contrib.common.util.ProgressLogger;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.contrib.common.collections.ChoiceSet;
import playground.johannes.gsv.demand.PopulationTask;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class PlanPrimaryActivity2 implements PopulationTask {

	private final List<Coord> stops;
	
	private final int choiceSetSize = 100;
	
	private final Random random;
	
	private RunThread[] threads;
	
	private Future<?>[] futures;
	
	private final ExecutorService executor;
	
	private final int numThreads = 24;
	
	public PlanPrimaryActivity2(TransitSchedule schedule, Random random, Geometry geometry) {
		this.random = random;
		
		GeometryFactory factory = new GeometryFactory();
		stops = new ArrayList<Coord>(schedule.getFacilities().size());
		for(TransitStopFacility stop : schedule.getFacilities().values()) {
			Point point = factory.createPoint(new Coordinate(stop.getCoord().getX(), stop.getCoord().getY()));
			if(geometry.contains(point))
				stops.add(stop.getCoord());
		}
		
		executor = Executors.newFixedThreadPool(numThreads);
		
		threads = new RunThread[numThreads];
		for(int i = 0; i < numThreads; i++)
			threads[i] = new RunThread();
		
		futures = new Future[numThreads];
	}
	
	@Override
	public void apply(Population pop) {
		Collection<? extends Person> persons = pop.getPersons().values();
		/*
		 * split collection in approx even segments
		 */
		int n = Math.min(persons.size(), threads.length);
		List<? extends Person>[] segments = CollectionUtils.split(persons, n);
		/*
		 * submit tasks
		 */
		ProgressLogger.init(persons.size(), 1, 10);
		for(int i = 0; i < segments.length; i++) {
			threads[i].persons = segments[i];
			threads[i].pop = pop;
			futures[i] = executor.submit(threads[i]);
		}
		/*
		 * wait for threads
		 */
		for(int i = 0; i < segments.length; i++) {
			try {
				futures[i].get();
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (ExecutionException e) {
				e.printStackTrace();
			}
		}
		ProgressLogger.terminate();
		
	}
	
	private class RunThread implements Runnable {

		private List<? extends Person> persons;
		
		private Population pop;
		
		@Override
		public void run() {
			for(Person person : persons) {
				Plan plan = person.getPlans().get(0);
				Activity act = (Activity) plan.getPlanElements().get(0);
				Coord coord = act.getCoord();
				
				ChoiceSet<Coord> choiceSet = new ChoiceSet<Coord>(random);
				for(int i = 0; i < choiceSetSize; i++) {
					Coord target = stops.get(random.nextInt(stops.size()));
					double dx = coord.getX() - target.getX();
					double dy = coord.getY() - target.getY();
					double d = Math.sqrt(dx*dx + dy*dy);
					double p = Math.pow(d, -1.3);
					choiceSet.addOption(target, p);
				}
				
				Coord choice = choiceSet.randomWeightedChoice();
				
				plan.addLeg(pop.getFactory().createLeg("undefined"));
				plan.addActivity(pop.getFactory().createActivityFromCoord("work", choice));
				
				ProgressLogger.step();
			}
			
		}
		
	}

}
