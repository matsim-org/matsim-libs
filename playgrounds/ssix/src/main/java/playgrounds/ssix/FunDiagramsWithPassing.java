/* *********************************************************************** *
 * project: org.matsim.*
 * LangeStreckeSzenario													   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playgrounds.ssix;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.collections.Tuple;


/* A class supposed to go attached to the DreieckStreckeSzenario class (with passing).
 * It aims at analyzing the flow of events in order to detect:
 * The permanent regime of the system and the following searched values:
 * the permanent flow, the permanent density and the permanent average 
 * velocity for each velocity group.
 * */

public class FunDiagramsWithPassing implements LinkEnterEventHandler{
	
	private static final Logger log = Logger.getLogger(FunDiagramsWithPassing.class);
	
	private static final int NUMBER_OF_MEMORIZED_FLOWS = 20;
	
	private Scenario scenario;
	
	private Id studiedMeasuringPointLinkId = new IdImpl(0);
	
	private boolean permanentRegime;
	private boolean permanentRegime_truck;
	//private boolean permanentRegime_med;
	private boolean permanentRegime_fast;
	
	private int permanentRegimeTour;
	private int cachedPermanentRegimeTour;
	
	private double permanentDensity;
	private double permanentDensity_truck;//partial density: number of truck drivers divided by the TOTAL network length.
	//private double permanentDensity_med;
	private double permanentDensity_fast;
	
	private double permanentFlow;
	private double permanentFlow_truck;
	//private double permanentFlow_med;
	private double permanentFlow_fast;
	
	private double permanentAverageVelocity;
	private double permanentAverageVelocity_truck;
	//private double permanentAverageVelocity_med;
	private double permanentAverageVelocity_fast;
	
	private boolean endRegime;
	
	private double endDensity;
	private double endDensity_truck;//partial density: number of truck drivers divided by the TOTAL network length.
	//private double endDensity_med;
	private double endDensity_fast;
	
	private double endFlow;
	private double endFlow_truck;
	//private double endFlow_med;
	private double endFlow_fast;
	
	private double endAverageVelocity;
	private double endAverageVelocity_truck;
	//private double endAverageVelocity_med;
	private double endAverageVelocity_fast;
	
	private Map<Id,Integer> personTour;
	private Map<Id,Double> lastSeenOnStudiedLinkEnter;
	
	private Map<Integer,Tuple<Integer,Double>> tourNumberSpeed;
	private Map<Integer,Tuple<Integer,Double>> tourNumberSpeed_truck;
	//private Map<Integer,Tuple<Integer,Double>> tourNumberSpeed_med;
	 Map<Integer,Tuple<Integer,Double>> tourNumberSpeed_fast;
	private int max_tourNumber_truck;
	private int max_tourNumber_fast;
	private List<Double> speedTable_truck;
	private List<Double> speedTable_fast;
	
	private List<Double> flowTable;//Flows on node 0 for the last 3600s (counted every second)
	private List<Double> flowTable_truck;
	//private List<Integer> flowTable_med;
	private List<Double> flowTable_fast;
	
	private Double flowTime;
	private Double flowTime_truck;
	//private Double flowTime_med;
	private Double flowTime_fast;
	
	private List<Double> lastXFlows;//for detecting permanent Flow more precisely
	
	

	public FunDiagramsWithPassing(Scenario sc){
		this.scenario = sc;
		this.permanentRegime = false;
		this.endRegime = false;
		this.personTour = new TreeMap<Id,Integer>();
		this.lastSeenOnStudiedLinkEnter = new TreeMap<Id,Double>();
		this.tourNumberSpeed = new TreeMap<Integer,Tuple<Integer,Double>>();
		this.permanentDensity = 0.;
		this.permanentFlow = 0.;
		this.permanentAverageVelocity = 0.;
		this.endDensity = 0.;
		this.endFlow = 0.;
		this.endAverageVelocity = 0.;
		this.flowTable = new LinkedList<Double>();
		for (int i=0; i<3600; i++){
			this.flowTable.add(0.);
		}
		this.flowTime = new Double(0.);
		this.lastXFlows = new LinkedList<Double>();
		for (int i=0; i<FunDiagramsWithPassing.NUMBER_OF_MEMORIZED_FLOWS; i++){
			this.lastXFlows.add(0.);
		}
		
		this.initializeGroupDependentVariables();
		this.max_tourNumber_truck = 0;
		this.max_tourNumber_fast = 0;
		this.speedTable_truck = new LinkedList<Double>();
		this.speedTable_fast = new LinkedList<Double>();
		for (int i=0; i<10; i++){
			this.speedTable_truck.add(0.);
			this.speedTable_fast.add(0.);
		}
	}
	
	public void reset(int iteration){
		this.permanentRegime=false;
		this.permanentDensity = 0.;
		this.permanentFlow = 0.;
		this.permanentAverageVelocity = 0.;
		this.personTour.clear();
		this.tourNumberSpeed.clear();
		this.lastSeenOnStudiedLinkEnter.clear();
		this.flowTable.clear();
		this.flowTime = new Double(0.);
		this.lastXFlows.clear();
		
		this.resetGroupDependentVariables();
		this.max_tourNumber_truck = 0;
		this.max_tourNumber_fast = 0;
		this.speedTable_truck.clear();
		this.speedTable_fast.clear();
	}
	
	public void handleEvent(LinkEnterEvent event){
		if (!(endRegime)){//If data is already extracted for that number of agents on the track
		//it is just necessary to let them go home and carry on with their lives instead of running around.
			Id personId = event.getPersonId();
			double pcu_person=0.;
			
			//Disaggregated data updating methods
			String transportMode = (String) scenario.getPopulation().getPersons().get(personId).getCustomAttributes().get("transportMode");
			//System.out.println("transportMode: "+transportMode);
			if (transportMode.equals("truck")){
				handleEvent_truck(event);
				pcu_person = DreieckStreckeSzenarioTest.PCU_TRUCK;
			/*
			} else if (transportMode.equals("med")) {
				handleEvent_med(event);
				pcu_person = DreieckStreckeSzenarioTest.PCU_MED;
			*/
			} else if (transportMode.equals("fast")) {
				handleEvent_fast(event);
				pcu_person = DreieckStreckeSzenarioTest.PCU_FAST;
			} else {
				while(true){
					log.warn("No transport mode acquired in event handling, must be something wrong!");
				}
			}
			
			//Still performing some aggregated data collecting
			int tourNumber;
			double nowTime = event.getTime();
			double networkLength = DreieckStreckeSzenarioTest.length * 3;
			
			if (event.getLinkId().equals(studiedMeasuringPointLinkId)){	
				
				//Updating Flow. NB: Flow is also measured on studiedMeasuringPointLinkId				
				if (nowTime == this.flowTime.doubleValue()){//Still measuring the flow of the same second
					Double nowFlow = this.flowTable.get(0);
					this.flowTable.set(0, nowFlow.doubleValue()+pcu_person);
				} else {//Need to offset the current data in order to update
					int timeDifference = (int) (nowTime-this.flowTime.doubleValue());
					if (timeDifference<3600){
						for (int i=3599-timeDifference; i>=0; i--){
							this.flowTable.set(i+timeDifference, this.flowTable.get(i).doubleValue());
						}
						if (timeDifference > 1){
							for (int i = 1; i<timeDifference; i++){
								this.flowTable.set(i, 0.);
							}
						}
						this.flowTable.set(0, pcu_person);
					} else {
						flowTableReset();
					}
					this.flowTime = new Double(nowTime);
				}
				
				//Permanent Regime handling
				if (permanentRegime){
					tourNumber = this.personTour.get(personId);
					handlePermanentGroupDependentVariables(tourNumber);
					
					this.permanentFlow = getActualFlow();//PCU/h......PCU-correction is done beforehand when updating flow
					//Updating lastXFlows
					for (int j=FunDiagramsWithPassing.NUMBER_OF_MEMORIZED_FLOWS-2; j>=0; j--){
						this.lastXFlows.set(j+1, this.lastXFlows.get(j).doubleValue());
					}
					this.lastXFlows.set(0, this.permanentFlow);
					
					if (tourNumber >= (this.permanentRegimeTour+2)){//Let the simulation go another turn around to eventually fill data gaps
						//This condition is not enough. Need to wait until the slowest mode has reached and totally completed tour permanentRegimeTour.
						//This is guaranteed in handleEvent_mode for each mode.
						
						int numberOfDrivingAgents = this.tourNumberSpeed.get(this.permanentRegimeTour).getFirst();
						
						this.permanentDensity = this.permanentDensity_truck+this.permanentDensity_fast;//PCU/km
					
						//this.permanentAverageVelocity = this.tourNumberSpeed.get(this.permanentRegimeTour).getSecond();//m/s
						//Alternative:
						this.permanentAverageVelocity = this.tourNumberSpeed.get(tourNumber-1).getSecond();
						
	
						if (almostAbsoluteEqualDoubles(this.lastXFlows.get(0),this.lastXFlows.get(FunDiagramsWithPassing.NUMBER_OF_MEMORIZED_FLOWS-1),1.)){
							log.info("Simulation successful.\n" +
									"Densities: truck: "+this.permanentDensity_truck+/*" med: "+this.permanentDensity_med+*/" fast: "+this.permanentDensity_fast+" TOTAL: "+this.permanentDensity+"\n"+
									"Flows: truck: "+this.permanentFlow_truck+/*" med: "+this.permanentFlow_med+*/" fast: "+this.permanentFlow_fast+" TOTAL: "+this.permanentFlow+"\n"+
									"V: truck: "+this.permanentAverageVelocity_truck+/*" med: "+this.permanentAverageVelocity_med+*/" fast: "+this.permanentAverageVelocity_fast+" AVERAGE: "+this.permanentAverageVelocity+"\n");
							
							//Capturing data for writer extraction.
							this.endRegime = true;
							
							this.endDensity = this.permanentDensity;
							this.endDensity_truck = this.permanentDensity_truck;
							//this.endDensity_med = this.permanentDensity_med;
							this.endDensity_fast = this.permanentDensity_fast;
							
							this.endFlow = this.permanentFlow;
							this.endFlow_truck = this.permanentFlow_truck;
							//this.endFlow_med = this.permanentFlow_med;
							this.endFlow_fast = this.permanentFlow_fast;
							
							this.endAverageVelocity = this.permanentAverageVelocity;
							this.endAverageVelocity_truck = this.permanentAverageVelocity_truck;
							//this.endAverageVelocity_med = this.permanentAverageVelocity_med;
							this.endAverageVelocity_fast = this.permanentAverageVelocity_fast;
							
							//TODO: set all goHome to true. Resolved in setting an endTime to the qSim, resulting in an abrupt abort after endTime seconds.
							
							
						}
					}
				}
			}
		}
	}
	
	private void handleEvent_truck(LinkEnterEvent event){
		Id personId = event.getPersonId();
		int tourNumber;
		double nowTime = event.getTime();
		double networkLength = DreieckStreckeSzenarioTest.length * 3;
		int checkingForAgents_fast = this.tourNumberSpeed_fast.size();
		
		if (event.getLinkId().equals(studiedMeasuringPointLinkId)){
			if (this.personTour.containsKey(personId)){
				tourNumber = personTour.get(personId);
				//Saving the speed by updating the previous average speed
				double lastSeenTime = lastSeenOnStudiedLinkEnter.get(personId);
				double speed = networkLength / (nowTime-lastSeenTime);//in m/s!!
				Tuple<Integer,Double> NumberSpeed = this.tourNumberSpeed.get(tourNumber);
				Tuple<Integer,Double> NumberSpeed_truck = this.tourNumberSpeed_truck.get(tourNumber);
				int n = NumberSpeed.getFirst(); int n_truck = NumberSpeed_truck.getFirst(); 
				double sn = NumberSpeed.getSecond(); double sn_truck = NumberSpeed_truck.getSecond();//average speed for n people
				//encountered a few calculatory problems here for still mysterious reasons, 
				//hence the normally very unnecessary detailed programming
				double first = n*sn/(n+1);   	double first_truck = n_truck*sn_truck/(n_truck+1);
				double second = speed/(n+1); 	double second_truck = speed/(n_truck+1);
				Tuple<Integer,Double> newNumberSpeed = new Tuple<Integer,Double>(n+1,first + second/*average speed with n+1 people*/);
				Tuple<Integer,Double> newNumberSpeed_truck = new Tuple<Integer,Double>(n_truck+1,first_truck + second_truck/*truck average speed with n+1 people*/);
				this.tourNumberSpeed.put(tourNumber, newNumberSpeed);
				this.tourNumberSpeed_truck.put(tourNumber, newNumberSpeed_truck);
				
				for (int i=8; i>=0; i--){
					this.speedTable_truck.set(i+1, this.speedTable_truck.get(i).doubleValue());
				}
				this.speedTable_truck.set(0, speed);
				
				//Checking for permanentRegime, in the mode and globally
				if (tourNumber>2){
					//Checking for empty modes
					/*
					if (!(this.permanentRegime_med)){
						//int checkingForAgents_med = this.tourNumberSpeed_med.size();
						if (checkingForAgents_med == 0){
							this.permanentRegime_med = true;//no agents driving, so this variable should always be true
							log.info("Med permanent regime attained because of empty mode.");
						}
					}
					*/
					if (!(this.permanentRegime_fast)){
						if (checkingForAgents_fast == 0){
							this.permanentRegime_fast = true;//no agents driving, so this variable should always be true
							this.cachedPermanentRegimeTour = 0;//lowest value possible for initialization so it will not be considered.
							log.info("Fast permanent regime attained because of empty mode.");
						}
					}
					
					//Permanent in the mode?
					if (!(permanentRegime_truck)){
						double previousLapSpeed_truck = this.tourNumberSpeed_truck.get(tourNumber-1).getSecond();
						double theOneBefore_truck = this.tourNumberSpeed_truck.get(tourNumber-2).getSecond();
						if ((almostRelativeEqualDoubles(speed, previousLapSpeed_truck, 0.02)) && (almostRelativeEqualDoubles(previousLapSpeed_truck, theOneBefore_truck, 0.02))){
							this.permanentRegime_truck=true;
							log.info("Truck permanent regime attained.");
							
							if (permanentRegime_fast){
								if (tourNumber>cachedPermanentRegimeTour){
									this.permanentRegimeTour = tourNumber;
								} else {
									this.permanentRegimeTour = this.cachedPermanentRegimeTour;
									this.cachedPermanentRegimeTour = tourNumber;//so that I remember the two tourNumbers in any situation.
								}
							} else {
								this.cachedPermanentRegimeTour = tourNumber;
							}
						}
					}
					//globally?
					if (!(permanentRegime)){
						double previousLapSpeed = this.tourNumberSpeed.get(tourNumber-1).getSecond();
						double theOneBefore = this.tourNumberSpeed.get(tourNumber-2).getSecond();
						boolean closeSpeeds = false;
						boolean stableModes = false;
						boolean completedPRT = false;
						int n_truck_prt;
						int n_fast_prt;
						
						if ((almostRelativeEqualDoubles(speed, previousLapSpeed, 0.02)) && (almostRelativeEqualDoubles(previousLapSpeed, theOneBefore, 0.02))){
							//then the average speeds of all vehicles (all modes included) has stabilized in these turns=>permanent Regime indicator
							closeSpeeds = true;
						}
						if ((permanentRegime_truck) /*&&(permanentRegime_med)*/ && (permanentRegime_fast)){  //just checking that the modes are effectively stable
							stableModes = true;
						}
						try {
							n_truck_prt = tourNumberSpeed_truck.get(permanentRegimeTour).getFirst();
						} catch (NullPointerException e) {
							n_truck_prt = 0;//means the NumberSpeed_truck for this tour hasn't been created => the mode needs to run around a little more
						}
						try {
							n_fast_prt = tourNumberSpeed_fast.get(permanentRegimeTour).getFirst();
						} catch (NullPointerException e) {
							n_fast_prt = 0;//means the NumberSpeed_fast for this tour hasn't been created => the mode needs to run around a little more
						}
						if ( (n_truck_prt+n_fast_prt) == this.scenario.getPopulation().getPersons().size()){
							completedPRT = true;
						}
						if ((closeSpeeds) && (stableModes) && (completedPRT)){
							this.permanentRegime=true;
							log.info("Global permanent regime attained.");
						}
					}
				}
				
				//Updating aggregated data sources
				tourNumber++;
				this.personTour.put(personId,tourNumber);
				this.lastSeenOnStudiedLinkEnter.put(personId,nowTime);
				
				//Initializing new Maps for next Tour if not already done
				if (!(this.tourNumberSpeed.containsKey(tourNumber)))
					this.tourNumberSpeed.put(tourNumber,new Tuple<Integer,Double>(0,0.));
				if (!(this.tourNumberSpeed_truck.containsKey(tourNumber))){
					max_tourNumber_truck++;
					this.tourNumberSpeed_truck.put(tourNumber,new Tuple<Integer,Double>(0,0.));
				}
			} else {
				//First tour handling
				tourNumber = 1;
				max_tourNumber_truck = 1;
				this.personTour.put(personId, tourNumber);
				this.lastSeenOnStudiedLinkEnter.put(personId, nowTime);
				if (!(this.tourNumberSpeed.containsKey(tourNumber))){
					this.tourNumberSpeed.put(tourNumber,new Tuple<Integer,Double>(0,0.));
					this.flowTime = new Double(nowTime);
				}				
				if (!(this.tourNumberSpeed_truck.containsKey(tourNumber))){
					this.tourNumberSpeed_truck.put(tourNumber,new Tuple<Integer,Double>(0,0.));
					this.flowTime_truck = new Double(nowTime);
				}
			}
			
			
			
			//Updating Flow. NB: Flow is also measured on studiedMeasuringPointLinkId
			if (nowTime == this.flowTime_truck.doubleValue()){//Still measuring the flow of the same second
				Double nowFlow = this.flowTable_truck.get(0);
				this.flowTable_truck.set(0, nowFlow.doubleValue()+DreieckStreckeSzenarioTest.PCU_TRUCK);
			} else {//Need to offset the current data in order to update
				int timeDifference = (int) (nowTime-this.flowTime_truck.doubleValue());
				//log.info("timeDifference is: "+timeDifference);
				if (timeDifference<3600){
					for (int i=3599-timeDifference; i>=0; i--){
						this.flowTable_truck.set(i+timeDifference, this.flowTable_truck.get(i).doubleValue());
					}
					if (timeDifference > 1){
						for (int i = 1; i<timeDifference; i++){
							this.flowTable_truck.set(i, 0.);
						}
					}
					this.flowTable_truck.set(0, DreieckStreckeSzenarioTest.PCU_TRUCK);
				} else {
					flowTableReset_truck();
				}
				this.flowTime_truck = new Double(nowTime);
			}
		}
	}
	
	/*
	private void handleEvent_med(LinkEnterEvent event){//TODO: PCU-dependent things to fix here as in other methods
		Id personId = event.getPersonId();
		int tourNumber;
		double nowTime = event.getTime();
		double networkLength = DreieckStreckeSzenarioTest.length * 3;
		int checkingForAgents_truck = this.tourNumberSpeed_truck.size();
		int checkingForAgents_fast = this.tourNumberSpeed_fast.size();
		
		if (event.getLinkId().equals(studiedMeasuringPointLinkId)){
			if (this.personTour.containsKey(personId)){
				tourNumber = personTour.get(personId);
				//Saving the speed by updating the previous average speed
				double lastSeenTime = lastSeenOnStudiedLinkEnter.get(personId);
				double speed = networkLength / (nowTime-lastSeenTime);//in m/s!!
				Tuple<Integer,Double> NumberSpeed = this.tourNumberSpeed.get(tourNumber);
				Tuple<Integer,Double> NumberSpeed_med = this.tourNumberSpeed_med.get(tourNumber);
				int n = NumberSpeed.getFirst(); int n_med = NumberSpeed_med.getFirst(); 
				double sn = NumberSpeed.getSecond(); double sn_med = NumberSpeed_med.getSecond();//average speed for n people
				//encountered a few calculatory problems here for still mysterious reasons, 
				//hence the normally very unnecessary detailed programming
				double  first = n*sn/(n+1); double first_med = n_med*sn_med/(n_med+1);
				double second = speed/(n+1); double second_med = speed/(n_med+1);
				Tuple<Integer,Double> newNumberSpeed = new Tuple<Integer,Double>(n+1,first + second);//average speed with n+1 people
				Tuple<Integer,Double> newNumberSpeed_med = new Tuple<Integer,Double>(n_med+1,first_med + second_med);//truck average speed with n+1 people
				this.tourNumberSpeed.put(tourNumber, newNumberSpeed);
				this.tourNumberSpeed_med.put(tourNumber, newNumberSpeed_med);
				
				
				//Checking for permanentRegime, in the mode and globally
				if (tourNumber>2){
					//Checking for empty modes
					//TODO: Update this part for 3 modes with the use of cachedPermanentRegimeTour?
					if (!(this.permanentRegime_truck)){
						if (checkingForAgents_truck == 0){
							this.permanentRegime_truck = true;//no agents driving, so this variable should always be true
							log.info("Truck permanent regime attained because of empty mode.");
						}
					}
					if (!(this.permanentRegime_fast)){
						if (checkingForAgents_fast == 0){
							this.permanentRegime_fast = true;//no agents driving, so this variable should always be true
							log.info("Fast permanent regime attained because of empty mode.");
						}
					}
					
					//Permanent in the mode?
					//System.out.println("speed_med: "+speed);
					double previousLapSpeed_med = this.tourNumberSpeed_med.get(tourNumber-1).getSecond();
					//System.out.println("previous_med: "+previousLapSpeed_med);
					double theOneBefore_med = this.tourNumberSpeed_med.get(tourNumber-2).getSecond();
					//System.out.println("before_med: "+theOneBefore_med);
					if ((almostRelativeEqualDoubles(speed, previousLapSpeed_med, 0.02)) && (almostRelativeEqualDoubles(previousLapSpeed_med, theOneBefore_med, 0.02))){
						if (!(permanentRegime_med)){
							//this.permanentRegimeTour=tourNumber; eventually detect the permanent regime as soon as ONE of the modes has reached it?
							this.permanentRegime_med=true;
							log.info("Med permanent regime attained.");
						}
					}
					//globally?
					double previousLapSpeed = this.tourNumberSpeed.get(tourNumber-1).getSecond();
					double theOneBefore = this.tourNumberSpeed.get(tourNumber-2).getSecond();
					if ((almostRelativeEqualDoubles(speed, previousLapSpeed, 0.02)) && (almostRelativeEqualDoubles(previousLapSpeed, theOneBefore, 0.02))){
						//then the average speeds of all vehicles (all modes included) has stabilized in these turns=>permanent Regime indicator
						if ((permanentRegime_truck) && (permanentRegime_med) && (permanentRegime_fast)){//just checking that the modes are effectively stable
							if (!(permanentRegime)){
								this.permanentRegime=true;
								if (checkingForAgents_fast != 0)
									this.permanentRegimeTour=tourNumber;//this must be done only in the fastest mode
							}
						}	
					}
				}
				
				//Updating aggregated data sources
				tourNumber++;
				this.personTour.put(personId,tourNumber);
				this.lastSeenOnStudiedLinkEnter.put(personId,nowTime);
				
				//Initializing new Maps for next Tour if not already done
				if (!(this.tourNumberSpeed.containsKey(tourNumber)))
					this.tourNumberSpeed.put(tourNumber,new Tuple<Integer,Double>(0,0.));
				if (!(this.tourNumberSpeed_med.containsKey(tourNumber)))
					this.tourNumberSpeed_med.put(tourNumber,new Tuple<Integer,Double>(0,0.));
			} else {
				//First tour handling
				tourNumber = 1;
				this.personTour.put(personId, tourNumber);
				this.lastSeenOnStudiedLinkEnter.put(personId, nowTime);
				if (!(this.tourNumberSpeed.containsKey(tourNumber))){
					this.tourNumberSpeed.put(tourNumber,new Tuple<Integer,Double>(0,0.));
					this.flowTime = new Double(nowTime);
				}				
				if (!(this.tourNumberSpeed_med.containsKey(tourNumber))){
					this.tourNumberSpeed_med.put(tourNumber,new Tuple<Integer,Double>(0,0.));
					this.flowTime_med = new Double(nowTime);
				}
			}
			
			
			
			//Updating Flow. NB: Flow is also measured on studiedMeasuringPointLinkId
			if (nowTime == this.flowTime_med.doubleValue()){//Still measuring the flow of the same second
				Integer nowFlow = this.flowTable_med.get(0);
				this.flowTable_med.set(0, nowFlow.intValue()+1);
			} else {//Need to offset the current data in order to update
				int timeDifference = (int) (nowTime-this.flowTime_med.doubleValue());
				//log.info("timeDifference is: "+timeDifference);
				if (timeDifference<3600){
					for (int i=3599-timeDifference; i>=0; i--){
						this.flowTable_med.set(i+timeDifference, this.flowTable_med.get(i).intValue());
					}
					if (timeDifference > 1){
						for (int i = 1; i<timeDifference; i++){
							this.flowTable_med.set(i, 0);
						}
					}
					this.flowTable_med.set(0, 1);
				} else {
					flowTableReset_med();
				}
				this.flowTime_med = new Double(nowTime);
			}
		}
	}
	*/
	
	private void handleEvent_fast(LinkEnterEvent event){
		Id personId = event.getPersonId();
		int tourNumber;
		double nowTime = event.getTime();
		double networkLength = DreieckStreckeSzenarioTest.length * 3;
		
		if (event.getLinkId().equals(studiedMeasuringPointLinkId)){
			if (this.personTour.containsKey(personId)){
				tourNumber = personTour.get(personId);
				//Saving the speed by updating the previous average speed
				double lastSeenTime = lastSeenOnStudiedLinkEnter.get(personId);
				double speed = networkLength / (nowTime-lastSeenTime);//in m/s!!
				Tuple<Integer,Double> NumberSpeed = this.tourNumberSpeed.get(tourNumber);
				Tuple<Integer,Double> NumberSpeed_fast = this.tourNumberSpeed_fast.get(tourNumber);
				int n = NumberSpeed.getFirst(); int n_fast = NumberSpeed_fast.getFirst(); 
				double sn = NumberSpeed.getSecond(); double sn_fast = NumberSpeed_fast.getSecond();//average speed for n people
				//encountered a few calculatory problems here for still mysterious reasons, 
				//hence the normally very unnecessary detailed programming
				double  first = n*sn/(n+1); double first_fast = n_fast*sn_fast/(n_fast+1);
				double second = speed/(n+1); double second_fast = speed/(n_fast+1);
				Tuple<Integer,Double> newNumberSpeed = new Tuple<Integer,Double>(n+1,first + second/*average speed with n+1 people*/);
				Tuple<Integer,Double> newNumberSpeed_fast = new Tuple<Integer,Double>(n_fast+1,first_fast + second_fast/*truck average speed with n+1 people*/);
				this.tourNumberSpeed.put(tourNumber, newNumberSpeed);
				this.tourNumberSpeed_fast.put(tourNumber, newNumberSpeed_fast);
				
				for (int i=8; i>=0; i--){
					this.speedTable_fast.set(i+1, this.speedTable_fast.get(i).doubleValue());
				}
				this.speedTable_fast.set(0, speed);
				
				
				//Checking for permanentRegime, in the mode and globally
				if (tourNumber>2){//>=3 because I want to compare actual speed with *2* previous speeds
					//Checking for empty modes
					if (!(this.permanentRegime_truck)){
						int checkingForAgents_truck = this.tourNumberSpeed_truck.size();
						if (checkingForAgents_truck == 0){
							this.permanentRegime_truck = true;//no agents driving, so this variable should always be true
							this.cachedPermanentRegimeTour = 0;//lowest value possible for initialization so it will not be considered.
							log.info("Truck permanent regime attained because of empty mode.");
						}
					}
					/*
					if (!(this.permanentRegime_med)){
						int checkingForAgents_med = this.tourNumberSpeed_med.size();
						if (checkingForAgents_med == 0){
							this.permanentRegime_med = true;//no agents driving, so this variable should always be true
							log.info("Med permanent regime attained because of empty mode.");
						}
					}
					*/
					
					//Permanent in the mode?
					if (!(permanentRegime_fast)){
						//System.out.println("speed_fast: "+speed);
						double previousLapSpeed_fast = this.tourNumberSpeed_fast.get(tourNumber-1).getSecond();
						//System.out.println("previous_fast: "+previousLapSpeed_fast);
						double theOneBefore_fast = this.tourNumberSpeed_fast.get(tourNumber-2).getSecond();
						//System.out.println("before_fast: "+theOneBefore_fast);
						if ((almostRelativeEqualDoubles(speed, previousLapSpeed_fast, 0.02)) && (almostRelativeEqualDoubles(previousLapSpeed_fast, theOneBefore_fast, 0.02))){
							this.permanentRegime_fast=true;
							log.info("Fast permanent regime attained.");
							
							if (permanentRegime_truck){
								if (tourNumber>cachedPermanentRegimeTour){//taking the highest tourNumber to ensure permanent data collecting
									this.permanentRegimeTour = tourNumber;
								} else {
									this.permanentRegimeTour = this.cachedPermanentRegimeTour;
									this.cachedPermanentRegimeTour = tourNumber;//so that I remember the two tourNumbers in any situation.
								}
							} else {
								this.cachedPermanentRegimeTour = tourNumber;
							}
						}
					}
					
					//globally?
					if (!(permanentRegime)){
						double previousLapSpeed = this.tourNumberSpeed.get(tourNumber-1).getSecond();
						double theOneBefore = this.tourNumberSpeed.get(tourNumber-2).getSecond();
						boolean closeSpeeds = false;
						boolean stableModes = false;
						boolean completedPRT = false;
						int n_truck_prt;
						int n_fast_prt;
						
						if ((almostRelativeEqualDoubles(speed, previousLapSpeed, 0.02)) && (almostRelativeEqualDoubles(previousLapSpeed, theOneBefore, 0.02))){
							//then the average speeds of all vehicles (all modes included) has stabilized in these turns=>permanent Regime indicator
							closeSpeeds = true;
						}
						if ((permanentRegime_truck) /*&&(permanentRegime_med)*/ && (permanentRegime_fast)){  //just checking that the modes are effectively stable
							stableModes = true;
						}
						try {
							n_truck_prt = tourNumberSpeed_truck.get(permanentRegimeTour).getFirst();
						} catch (NullPointerException e) {
							n_truck_prt = 0;//means the NumberSpeed_truck for this tour hasn't been created => the mode needs to run around a little more
						}
						try {
							n_fast_prt = tourNumberSpeed_fast.get(permanentRegimeTour).getFirst();
						} catch (NullPointerException e) {
							n_fast_prt = 0;//means the NumberSpeed_fast for this tour hasn't been created => the mode needs to run around a little more
						}
						if ( (n_truck_prt+n_fast_prt) == this.scenario.getPopulation().getPersons().size()){
							completedPRT = true;
						}
						if ((closeSpeeds) && (stableModes) && (completedPRT)){
							this.permanentRegime=true;
							log.info("Global permanent regime attained.");
						}
					}	
				}
				
				//Updating aggregated data sources
				tourNumber++;
				this.personTour.put(personId,tourNumber);
				this.lastSeenOnStudiedLinkEnter.put(personId,nowTime);
				
				//Initializing new Maps for next Tour if not already done
				if (!(this.tourNumberSpeed.containsKey(tourNumber)))
					this.tourNumberSpeed.put(tourNumber,new Tuple<Integer,Double>(0,0.));
				if (!(this.tourNumberSpeed_fast.containsKey(tourNumber))){
					this.tourNumberSpeed_fast.put(tourNumber,new Tuple<Integer,Double>(0,0.));
					max_tourNumber_fast++;
				}
			} else {
				//First tour handling
				tourNumber = 1;
				this.personTour.put(personId, tourNumber);
				this.lastSeenOnStudiedLinkEnter.put(personId, nowTime);
				if (!(this.tourNumberSpeed.containsKey(tourNumber))){
					this.tourNumberSpeed.put(tourNumber,new Tuple<Integer,Double>(0,0.));
					this.flowTime = new Double(nowTime);
				}				
				if (!(this.tourNumberSpeed_fast.containsKey(tourNumber))){
					this.tourNumberSpeed_fast.put(tourNumber,new Tuple<Integer,Double>(0,0.));
					this.flowTime_fast = new Double(nowTime);
				}
			}
			
			
			
			//Updating Flow. NB: Flow is also measured on studiedMeasuringPointLinkId
			if (nowTime == this.flowTime_fast.doubleValue()){//Still measuring the flow of the same second
				Double nowFlow = this.flowTable_fast.get(0);
				this.flowTable_fast.set(0, nowFlow.doubleValue()+DreieckStreckeSzenarioTest.PCU_FAST);
			} else {//Need to offset the current data in order to update
				int timeDifference = (int) (nowTime-this.flowTime_fast.doubleValue());
				//log.info("timeDifference is: "+timeDifference);
				if (timeDifference<3600){
					for (int i=3599-timeDifference; i>=0; i--){
						this.flowTable_fast.set(i+timeDifference, this.flowTable_fast.get(i).doubleValue());
					}
					if (timeDifference > 1){
						for (int i = 1; i<timeDifference; i++){
							this.flowTable_fast.set(i, 0.);
						}
					}
					this.flowTable_fast.set(0, DreieckStreckeSzenarioTest.PCU_FAST);
				} else {
					flowTableReset_fast();
				}
				this.flowTime_fast = new Double(nowTime);
			}
		}
	}


 	private double getActualFlow(){
		double flowOverLast3600s = 0.;
		for (int i=0; i<3600; i++){
			flowOverLast3600s += this.flowTable.get(i).doubleValue();
		}
		return flowOverLast3600s;//extrapolated hour flow in veh/h
	}
 	
	private double getActualFlow_truck(){
		double flowOverLast3600s = 0.;
		for (int i=0; i<3600; i++){
			flowOverLast3600s += this.flowTable_truck.get(i).doubleValue();
		}
		return flowOverLast3600s;//extrapolated hour flow in veh/h
	}
	
	/*
	private double getActualFlow_med(){
		double flowOverLast3600s = 0.;
		for (int i=0; i<3600; i++){
			flowOverLast3600s += this.flowTable_med.get(i).doubleValue();
		}
		return flowOverLast3600s;//extrapolated hour flow in veh/h
	}
	*/
	
	private double getActualFlow_fast(){
		double flowOverLast3600s = 0.;
		for (int i=0; i<3600; i++){
			flowOverLast3600s += this.flowTable_fast.get(i).doubleValue();
		}
		return flowOverLast3600s;//extrapolated hour flow in veh/h
	}
	
	
	private void flowTableReset(){
		for (int i=0; i<3600; i++){
			this.flowTable.set(i, 0.);
		}
	}
	
	private void flowTableReset_truck(){
		for (int i=0; i<3600; i++){
			this.flowTable_truck.set(i, 0.);
		}
	}
	
	/*
	private void flowTableReset_med(){
		for (int i=0; i<3600; i++){
			this.flowTable_med.set(i, 0.);
		}
	}
	*/
	
	private void flowTableReset_fast(){
		for (int i=0; i<3600; i++){
			this.flowTable_fast.set(i, 0.);
		}
	}
	
	
	private boolean almostRelativeEqualDoubles(double d1, double d2, double MaximumAcceptedDeviance){
		//	not so good, with big flows it starts to detect stability too soon.
		if (((d1-d2)/d2)< MaximumAcceptedDeviance)
			return true;
		return false;
		
	}
	
	private boolean almostAbsoluteEqualDoubles(double d1, double d2, double MaximumAcceptedDeviance){
		if (Math.abs(d1-d2)<MaximumAcceptedDeviance)
			return true;
		return false;
	}
	
	private void initializeGroupDependentVariables(){
		this.permanentRegime_truck = false;
		//this.permanentRegime_med = false;
		this.permanentRegime_fast = false;
		
		this.permanentDensity_truck = 0.;
		//this.permanentDensity_med = 0.;
		this.permanentDensity_fast = 0.;
		
		this.permanentFlow_truck = 0.;
		//this.permanentFlow_med = 0.;
		this.permanentFlow_fast = 0.;
		
		this.permanentAverageVelocity_truck = 0.;
		//this.permanentAverageVelocity_med = 0.;
		this.permanentAverageVelocity = 0.;
		
		this.endDensity_truck = 0.;
		//this.endDensity_med = 0.;
		this.endDensity_fast = 0.;
		
		this.endFlow_truck = 0.;
		//this.endFlow_med = 0.;
		this.endFlow_fast = 0.;
		
		this.endAverageVelocity_truck = 0.;
		//this.endAverageVelocity_med = 0.;
		this.endAverageVelocity_fast = 0.;
		
		this.tourNumberSpeed_truck = new TreeMap<Integer,Tuple<Integer,Double>>();
		//this.tourNumberSpeed_med = new TreeMap<Integer,Tuple<Integer,Double>>();
		this.tourNumberSpeed_fast = new TreeMap<Integer,Tuple<Integer,Double>>();
		
		this.flowTable_truck = new LinkedList<Double>();
		//this.flowTable_med = new LinkedList<Double>();
		this.flowTable_fast = new LinkedList<Double>();
		for (int i=0; i<3600; i++){
			this.flowTable_truck.add(0.);
			//this.flowTable_med.add(0.);
			this.flowTable_fast.add(0.);
		}
		
		this.flowTime_truck = new Double(0.);
		//this.flowTime_med = new Double(0.);
		this.flowTime_fast = new Double(0.);
	}

	private void resetGroupDependentVariables(){
		this.permanentRegime_truck = false;
		//this.permanentRegime_med = false;
		this.permanentRegime_fast = false;
		
		this.permanentDensity_truck = 0.;
		//this.permanentDensity_med = 0.;
		this.permanentDensity_fast = 0.;
		
		this.permanentFlow_truck = 0.;
		//this.permanentFlow_med = 0.;
		this.permanentFlow_fast = 0.;
		
		this.permanentAverageVelocity_truck = 0.;
		//this.permanentAverageVelocity_med = 0.;
		this.permanentAverageVelocity_fast = 0.;
		
		this.tourNumberSpeed_truck.clear();
		//this.tourNumberSpeed_med.clear();
		this.tourNumberSpeed_fast.clear();
		
		this.flowTable_truck.clear();
		//this.flowTable_med.clear();
		this.flowTable_fast.clear();
		
		this.flowTime_truck = new Double(0.);
		//this.flowTime_med = new Double(0.);
		this.flowTime_fast = new Double(0.);
		
		this.endDensity_truck = 0.;
		//this.endDensity_med = 0.;
		this.endDensity_fast = 0.;
		
		this.endFlow_truck = 0.;
		//this.endFlow_med = 0.;
		this.endFlow_fast = 0.;
		
		this.endAverageVelocity_truck = 0.;
		//this.endAverageVelocity_med = 0.;
		this.endAverageVelocity_fast = 0.;
	}
	
	private void handlePermanentGroupDependentVariables(int tourNumber){
		double networkLength = 3*DreieckStreckeSzenarioTest.length;
		
		this.permanentFlow_truck = getActualFlow_truck();
		//this.permanentFlow_med = getActualFlow_med();
		this.permanentFlow_fast = getActualFlow_fast();				
		
		if (tourNumber >= (this.permanentRegimeTour+3)){//Let the simulation go another turn around to eventually fill data gaps
			//This condition is not enough. Need to wait until the slowest mode has reached and totally completed tour permanentRegimeTour.
			//However, this is guaranteed in the methods handleEvent_mode when detecting permanentRegime
			
			int N_truck = 0; /*int N_med = 0;*/ int N_fast = 0;
			
			if (this.tourNumberSpeed_truck.size() != 0){
				 N_truck = this.tourNumberSpeed_truck.get(this.permanentRegimeTour).getFirst();
			}
			/*
			if (this.tourNumberSpeed_med.size() != 0){
				 N_med = this.tourNumberSpeed_med.get(this.permanentRegimeTour).getFirst();
			}
			*/
			if (this.tourNumberSpeed_fast.size() != 0){
				 N_fast = this.tourNumberSpeed_fast.get(this.permanentRegimeTour).getFirst();
			}

			//as stated in the class attribute section, the following densities are PARTIAL densities, which means
			//the saturation density will DEPEND on the chosen probabilities (and hence will not be the usual 148.33 veh/km for all modes)
			this.permanentDensity_truck = N_truck*DreieckStreckeSzenarioTest.PCU_TRUCK/networkLength*1000;
			//this.permanentDensity_med = N_med*DreieckStreckeSzenarioTest.PCU_MED/networkLength*1000;
			this.permanentDensity_fast = N_fast*DreieckStreckeSzenarioTest.PCU_FAST/networkLength*1000;
			
			if (this.tourNumberSpeed_truck.size() != 0){
				//this.permanentAverageVelocity_truck = this.tourNumberSpeed_truck.get(this.max_tourNumber_truck-1).getSecond();//m/s
				double sum =0;
				for (int i=0; i<10; i++){
					sum += this.speedTable_truck.get(i);
				}
				this.permanentAverageVelocity_truck = sum / 10;
			} else {
				this.permanentAverageVelocity_truck = 0.;
			}
			
			
			/*
			if (this.tourNumberSpeed_med.size() != 0){
				this.permanentAverageVelocity_med = this.tourNumberSpeed_med.get(this.permanentRegimeTour).getSecond();//m/s
			} else {
				this.permanentAverageVelocity_med = 0.;
			}
			*/
			if (this.tourNumberSpeed_fast.size() != 0){
				//this.permanentAverageVelocity_fast = this.tourNumberSpeed_fast.get(this.max_tourNumber_fast-1).getSecond();//m/s
				double sum =0;
				for (int i=0; i<10; i++){
					sum += this.speedTable_fast.get(i);
				}
				this.permanentAverageVelocity_fast = sum / 10;
			} else {
				this.permanentAverageVelocity_fast = 0.;
			}
			
		}	
	}

	public boolean isEndRegime() {
		return endRegime;
	}
	
	public double getEndDensity() {
		return endDensity;
	}
	
	public double getEndDensity_truck() {
		return endDensity_truck;
	}

/*
	public double getEndDensity_med() {
		return endDensity_med;
	}
*/
	
	public double getEndDensity_fast() {
		return endDensity_fast;
	}
	
	public double getEndFlow() {
		return endFlow;
	}
	
	public double getEndFlow_truck() {
		return endFlow_truck;
	}
	
	/*
	public double getEndFlow_med() {
		return endFlow_med;
	}
	*/
	
	public double getEndFlow_fast() {
		return endFlow_fast;
	}
	
	public double getEndAverageVelocity() {
		return endAverageVelocity;
	}
	
	public double getEndAverageVelocity_truck() {
		return endAverageVelocity_truck;
	}
	
	/*
	public double getEndAverageVelocity_med() {
		return endAverageVelocity_med;
	}
	*/
	
	public double getEndAverageVelocity_fast() {
		return endAverageVelocity_fast;
	}

}