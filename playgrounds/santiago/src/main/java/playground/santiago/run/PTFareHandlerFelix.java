/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.santiago.run;

import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.controler.MatsimServices;
import org.matsim.pt.PtConstants;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.Vehicles;

import playground.santiago.SantiagoScenarioConstants;

/**
 * Calculates public transport fares for the Santiago scenario.
 * The full fare scheme is available under http://www.transantiago.cl/tarifas-y-pagos/conoce-las-tarifas.
 * 
 * Currently, interchanges do not cost any money.
 * TODO: Integrate different fares for peak/offPeak interchanges between metro and bus. 
 * TODO: Integrate studentSeniorFare
 * 
 * @author benjamin
 * 
 */
public class PTFareHandlerFelix implements ActivityStartEventHandler, PersonDepartureEventHandler, PersonEntersVehicleEventHandler {
	private static final Logger log = Logger.getLogger(PersonDepartureEventHandler.class);

	private final double peakFare = -720.;
	private final double intermediateFare = -660.;
	private final double offPeakFare = -640.;
	
	private final double studentSeniorFare = -210.;
	
	private final double startIntermediateTimeMorning = 6.5 * 3600;
//	private final double endIntermediateTimeMorning = (7.0 * 3600) - 1;
//	private final double startIntermediateTimeDay = 9.0 * 3600;
//	private final double endIntermediateTimeDay = (18.0 * 3600) - 1;
//	private final double startIntermediateTimeEvening = 20.0 * 3600;
	private final double endIntermediateTimeEvening = (20.75 * 3600) - 1;
	private int colectivoRides = 0;
	private final double startPeakTimeMorning = 7.0 * 3600;
	private final double endPeakTimeMorning = (9.0 * 3600) - 1;
	private final double startPeakTimeEvening = 18.0 * 3600;
	private final double endPeakTimeEvening = (20.0 * 3600) - 1;

	private final MatsimServices controler;
	private boolean doModeChoice;
	private Vehicles transitVehicles;
	private TransitSchedule transitSchedule;
	private Population population;
	private Set<String> ptModes;
	private Set<Id<Person>> personsOnPtTrip = new HashSet<Id<Person>>();
	private double colectivoConstantUtils;
	private double ptConstantMonetaryUnits;
	
	public PTFareHandlerFelix(final MatsimServices controler, boolean doModeChoice, Population population, double colectivoASC){
		this.controler = controler;
		this.doModeChoice = doModeChoice;
		if(this.doModeChoice){
			this.transitVehicles = controler.getScenario().getTransitVehicles();
			this.transitSchedule = controler.getScenario().getTransitSchedule();
			this.population = population;
		} else {
			this.ptModes = definePtModes();
		}
		this.colectivoConstantUtils = colectivoASC;
		
		this.ptConstantMonetaryUnits = controler.getConfig().planCalcScore().getModes().get(TransportMode.pt).getConstant() / controler.getConfig().planCalcScore().getMarginalUtilityOfMoney();
		log.warn("This approach throws money events to account for a colectivo ASC. This needs to be considered when computing fare revenues.");
		log.info("colectivoASC: " + colectivoASC);
	}

	@Override
	public void reset(int iteration) {
		personsOnPtTrip.clear();
		Logger.getLogger(getClass()).info("Iteration :" + iteration + " Colectivo Rides: "+ colectivoRides );
		colectivoRides = 0;
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		if(event.getActType().equals(PtConstants.TRANSIT_ACTIVITY_TYPE)){
			// do nothing
		} else {
			if(personsOnPtTrip.contains(event.getPersonId())){
				personsOnPtTrip.remove(event.getPersonId()); // person started activity, i.e. trip is finished
			}
		}
	}
	
	//only if mode choice is ON (i.e. transit vehicles are there)
	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		if(this.doModeChoice){ //only if mode choice is on (i.e. transit vehicles are there)
			double time = event.getTime();
			Id<Person> pid = event.getPersonId();
			Id<Vehicle> vid = event.getVehicleId();
			if(vid == null || vid.equals("")){
				log.warn("Vehicle is not properly defined; cannot prove if it is a PT vehicle. Hence, no fare is collected from person " + pid);
			}
			
//	colectivo fares
//			if(this.transitVehicles.getVehicles().get(vid).toString().contains("co")){
			
			 if(this.transitVehicles.getVehicles().get(vid) == null){
				// person is entering a non-transit vehicle; no fare to pay.
			 }
			 else if(this.transitVehicles.getVehicles().get(vid).getId().toString().startsWith("co")){
				 	if(population.getPersons().get(pid) == null){
						//this is a TransitDriverAgent who should not pay fare.
					} else{
						String line = vid.toString().split("_")[0];
						colectivoRides++;
						Double fare = (Double) this.transitSchedule.getTransitLinesAttributes().getAttribute(line, "fare");
						if (fare!=null){
							
						
						this.controler.getEvents().processEvent(new PersonMoneyEvent(time, pid, -fare));
						//ASC of the pt is replaced by colectivo ASC converted to money.
						this.controler.getEvents().processEvent(new PersonMoneyEvent(time, pid, -ptConstantMonetaryUnits));
						this.controler.getEvents().processEvent(new PersonMoneyEvent(time, pid, colectivoConstantUtils/controler.getConfig().planCalcScore().getMarginalUtilityOfMoney()));
						}
						else{
							this.controler.getEvents().processEvent(new PersonMoneyEvent(time, pid, -800));

							Logger.getLogger(getClass()).warn("Fare for line "+line+ "not found.");
						}}
			 } else {
				if(population.getPersons().get(pid) == null){
					//this is a TransitDriverAgent who should not pay fare.
				} else {
					if(personsOnPtTrip.contains(pid)){
						// person has already paid fare for this trip.
					} else {
						double fare = getTimedependentFare(time);
						this.controler.getEvents().processEvent(new PersonMoneyEvent(time, pid, fare));
						personsOnPtTrip.add(pid);
					}
				}
			}
		}
	}
	
	//only if mode choice is OFF (i.e. teleported pt is on)
	@Override
	public void handleEvent(PersonDepartureEvent event) {
		if(!this.doModeChoice){
			double time = event.getTime();
			Id<Person> id = event.getPersonId();
			String legMode = event.getLegMode();
			if(this.ptModes.contains(legMode)){
				double fare = getTimedependentFare(time);
				this.controler.getEvents().processEvent(new PersonMoneyEvent(time, id, fare));
			}
		}
	}
	
	private double getTimedependentFare(double time) {
		double fare = offPeakFare;
		if(time >= startPeakTimeMorning && time <= endPeakTimeMorning){
			fare = peakFare;
		} else if(time >= startPeakTimeEvening && time <= endPeakTimeEvening){
			fare = peakFare;
		} else if(time >= startIntermediateTimeMorning && time <= endIntermediateTimeEvening){
			fare = intermediateFare;
		}
		return fare;
	}

	private Set<String> definePtModes() {
		Set<String> ptModes = new HashSet<String>();
//		ptModes.add(TransportMode.pt);
		ptModes.add(SantiagoScenarioConstants.Modes.bus.toString());
		ptModes.add(SantiagoScenarioConstants.Modes.metro.toString());
		ptModes.add(SantiagoScenarioConstants.Modes.train.toString());
		ptModes.add(SantiagoScenarioConstants.Modes.colectivo.toString());
		return ptModes;
	}
}