package org.matsim.contrib.matsim4urbansim.matsim4urbansim;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.*;
import org.matsim.contrib.matrixbasedptrouter.PtMatrix;
import org.matsim.contrib.matsim4urbansim.config.modules.UrbanSimParameterConfigModuleV3;
import org.matsim.contrib.matsim4urbansim.constants.InternalConstants;
import org.matsim.contrib.matsim4urbansim.utils.helperobjects.Benchmark;
import org.matsim.contrib.matsim4urbansim.utils.io.writer.UrbanSimPersonCSVWriter;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;

import java.util.Iterator;

/**
 * implements agent-based performance feedback for UrbanSim population (time spent traveling, money spent traveling, etc.)
 * 
 * improvements jan'13
 * - added pt for accessibility calculation
 * 
 * @author thomas
 *
 */
public class AgentPerformanceControlerListener implements ShutdownListener{

	private static final Logger log = Logger.getLogger(AgentPerformanceControlerListener.class);
	
	private Benchmark benchmark;
	private PtMatrix ptMatrix = null;
	UrbanSimParameterConfigModuleV3 module;
	
	public AgentPerformanceControlerListener(Benchmark benchmark, PtMatrix ptMatrix, UrbanSimParameterConfigModuleV3 module){
		this.benchmark = benchmark;
		this.ptMatrix = ptMatrix;
		// writing agent performances continuously into "persons.csv"-file. Naming of this 
		// files is given by the UrbanSim convention importing a csv file into a identically named 
		// data set table. THIS PRODUCES INPUT FOR URBANSIM 
		UrbanSimPersonCSVWriter.initUrbanSimPersonWriter(module);
		this.module =  module;
	}
	
	/**
	 * agent-based performance feedback (time spent traveling, money spent traveling, etc.)
	 */
	@Override
	public void notifyShutdown(ShutdownEvent event) {
		
		int benchmarkID = this.benchmark.addMeasure("Agent performance services");
		
		long carModeCounter = 0;
		long ptModeCounter  = 0;
		long bicycleModeCounter=0;
		long walkModeCounter= 0;
		
		// get the controller and scenario
		MatsimServices controler = event.getServices();
		// get network
        Network network = controler.getScenario().getNetwork();
		// get persons
        Population population = controler.getScenario().getPopulation();
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
			
			Coord homeCoord = getActivityLocation(plan, InternalConstants.ACT_HOME);
			Coord workCoord = getActivityLocation(plan, InternalConstants.ACT_WORK);

			for ( PlanElement pe : plan.getPlanElements() ) {
				if ( pe instanceof Activity ) {
					Activity activity = (Activity) pe;
					
					if(activity.getType().endsWith(InternalConstants.ACT_HOME))
						isHomeActivity = true;
					else
						isHomeActivity = false;
				}
				else if (pe instanceof Leg) {
					Leg leg = (Leg) pe;
					// mode
					mode = ((Leg) pe).getMode();
					
					double distance = -1.;
					if (mode.equalsIgnoreCase(TransportMode.car)) {
						// if pe is a leg
						Route route = leg.getRoute();
						distance = RouteUtils.calcDistanceExcludingStartEndLink(
								(NetworkRoute) route, network);
					}
					else if(mode.equalsIgnoreCase(TransportMode.pt)){
						if(homeCoord != null && workCoord != null && ptMatrix != null)
							distance = ptMatrix.getTotalTravelDistance_meter(homeCoord, workCoord);
					}
					
					if (isHomeActivity) {
						distance_home_work_meter = distance;
						// to get minutes in time format mm:ss use
						// TimeUtil.convertSeconds2Minutes(leg.getTravelTime());
						// or see org.matsim.core.utils.misc.Time
						duration_home_work_min = leg.getTravelTime() / 60.;
					} else {
						distance_work_home_meter = distance;
						// to get minutes in time format mm:ss use
						// TimeUtil.convertSeconds2Minutes(leg.getTravelTime());
						// or see org.matsim.core.utils.misc.Time
						duration_work_home_min = leg.getTravelTime() / 60.;
					}
				}
			}
			// update counter
			if(mode.equalsIgnoreCase(TransportMode.car))
				carModeCounter++;
			else if(mode.equalsIgnoreCase(TransportMode.pt))
				ptModeCounter++;
			else if (mode.equalsIgnoreCase(TransportMode.bike))
				bicycleModeCounter++;
			else if (mode.equalsIgnoreCase(TransportMode.walk))
				walkModeCounter++;
			
			// write current person dates into UrbanSim input table
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
		UrbanSimPersonCSVWriter.close(module);
		
		log.info("Used transport modes ...");
		log.info("Car " + carModeCounter);
		log.info("Pt " + ptModeCounter);
		log.info("Bicycle " + bicycleModeCounter);
		log.info("Walk " + walkModeCounter);
		
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
	
	private Coord getActivityLocation(Plan plan,String activityType){
		for( PlanElement pe : plan.getPlanElements() ) {
			if ( pe instanceof Activity ) {
				Activity activity = (Activity) pe;
				if(activity.getType().endsWith( activityType ))
					return activity.getCoord();
			}
		}
		return null;
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
