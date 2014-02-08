package playground.tnicolai.matsim4opus.matsim4urbansim;

import java.util.Iterator;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;

import playground.tnicolai.matsim4opus.constants.InternalConstants;
import playground.tnicolai.matsim4opus.utils.helperObjects.Benchmark;
import playground.tnicolai.matsim4opus.utils.io.writer.UrbanSimPersonCSVWriter;

/**
 * implements agent-based performance feedback for UrbanSim population (time spent traveling, money spent traveling, etc.)
 * 
 * @author thomas
 *
 */
public class AgentPerformanceControlerListener implements ShutdownListener{

	private static final Logger log = Logger.getLogger(AgentPerformanceControlerListener.class);
	
	private Benchmark benchmark;
	
	public AgentPerformanceControlerListener(Benchmark benchmark){
		this.benchmark = benchmark;
		// writing agent performances continuously into "persons.csv"-file. Naming of this 
		// files is given by the UrbanSim convention importing a csv file into a identically named 
		// data set table. THIS PRODUCES URBANSIM INPUT
		UrbanSimPersonCSVWriter.initUrbanSimPersonWriter();
	}
	
	/**
	 * agent-based performance feedback (time spent traveling, money spent traveling, etc.)
	 */
	@Override
	public void notifyShutdown(ShutdownEvent event) {
		
		int benchmarkID = this.benchmark.addMeasure("Agent performance controler");
		
		// get the controller and scenario
		Controler controler = event.getControler();
		// get network
		Network network = controler.getNetwork();
		// get persons
		Population population = controler.getPopulation();
		Iterator<? extends Person> persons = population.getPersons().values().iterator();
		
		while(persons.hasNext()){
			
			// tnicolai: add monetary costs !!!
			double duration_home_work_min 	= -1.;
			double distance_home_work_meter	= -1.;
			double duration_work_home_min	= -1.;
			double distance_work_home_meter	= -1.;
			String mode						= "none";
			
			Person p = persons.next();
			// dumping out travel times/distances from selected plan
			Plan plan = p.getSelectedPlan();
			
			boolean isHomeActivity = true;
			
			// check if activities available (then size of plan elements > 1)
			// what happens with persons which no activities???
			if(plan.getPlanElements().size() <= 1){
				write(duration_home_work_min, distance_home_work_meter,
					  duration_work_home_min, distance_work_home_meter, 
					  mode, p);
				continue;
			}
			
			for ( PlanElement pe : plan.getPlanElements() ) {
				if ( pe instanceof Activity ) {
					Activity activity = (Activity) pe;
					
					if(activity.getType().endsWith(InternalConstants.ACT_HOME))
						isHomeActivity = true;
					else
						isHomeActivity = false;
				}
				else if(pe instanceof Leg){
					Leg leg = (Leg) pe;
					//mode
					mode = ((Leg) pe).getMode();
					// if pe is a leg
					Route route = leg.getRoute();
					// tnicolai: or should we use route.getDistance ???
					double distance = RouteUtils.calcDistance( (NetworkRoute)route ,network); 
					if(isHomeActivity){
						distance_home_work_meter = distance;
						// to get minutes in time format mm:ss use TimeUtil.convertSeconds2Minutes(leg.getTravelTime()); 
						// or see org.matsim.core.utils.misc.Time
						duration_home_work_min   = leg.getTravelTime() / 60.;
					}
					else{
						distance_work_home_meter = distance;
						// to get minutes in time format mm:ss use TimeUtil.convertSeconds2Minutes(leg.getTravelTime()); 
						// or see org.matsim.core.utils.misc.Time
						duration_work_home_min	 = leg.getTravelTime() / 60.;
					}
				}
			}
			write(duration_home_work_min, distance_home_work_meter,
				  duration_work_home_min, distance_work_home_meter, 
				  mode, p);
			// for debugging
			// String personID = p.getId().toString();
			// log.info("Person[" + personID + "],Home2WorkTravelTime[" + duration_home_work_min
			//		+ "],Home2WorkDistance[" + distance_home_work_meter
			//		+ "],Work2HomeTravelTime[" + duration_work_home_min
			//		+ "],Work2HomeDistance[" + distance_work_home_meter + "]");
		}
		// close writer
		UrbanSimPersonCSVWriter.close();
		// print computation time 
		if (this.benchmark != null && benchmarkID > 0) {
			this.benchmark.stoppMeasurement(benchmarkID);
			log.info("Agent Performance Feedback with population size:"
					+ population.getPersons().size() + " took "
					+ this.benchmark.getDurationInSeconds(benchmarkID)
					+ " seconds ("
					+ this.benchmark.getDurationInSeconds(benchmarkID) / 60.
					+ " minutes).");
		}
	}
	
	/**
	 * writing agent performances to csv file
	 * 
	 * @param duration_home_work_min
	 * @param distance_home_work_meter
	 * @param duration_work_home_min
	 * @param distance_work_home_meter
	 * @param p
	 */
	private void write(double duration_home_work_min,
			double distance_home_work_meter, double duration_work_home_min,
			double distance_work_home_meter, String mode, Person p) {
		
		UrbanSimPersonCSVWriter.write(p.getId().toString(), 
									  duration_home_work_min,
									  distance_home_work_meter, 
									  duration_work_home_min,
									  distance_work_home_meter,
									  mode);
	}
}
