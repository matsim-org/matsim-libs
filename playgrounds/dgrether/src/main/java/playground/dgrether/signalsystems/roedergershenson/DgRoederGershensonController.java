/* *********************************************************************** *
 * project: org.matsim.*
 * DgRoederGershensonController
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.dgrether.signalsystems.roedergershenson;

import org.apache.log4j.Logger;
import org.matsim.signalsystems.control.SignalGroupState;
import org.matsim.signalsystems.model.SignalController;
import org.matsim.signalsystems.model.SignalPlan;
import org.matsim.signalsystems.model.SignalSystem;
import org.matsim.signalsystems.systems.SignalGroupDefinition;

import playground.dgrether.signalsystems.DgSensorManager;


/**
 * @author dgrether
 *
 */
public class DgRoederGershensonController implements SignalController {
	
	public final static String CONTROLLER_IDENTIFIER = "gretherRoederGershensonSignalControll";

	private static final Logger log = Logger.getLogger(DgRoederGershensonController.class);
	
	private SignalSystem system;

	protected int tGreenMin =  0; // time in seconds
	protected int minCarsTime = 0; //
	protected double capFactor = 0;
	protected double maxRedTime ;
	
	private boolean interim = false;
	private double interimTime;
	private SignalGroupDefinition interimGroup;

	protected boolean outLinkJam;
	protected boolean maxRedTimeActive = false;
	protected double compGreenTime;
	protected double approachingRed;
	protected double approachingGreenLink;
	protected double approachingGreenLane;
	protected double carsOnRefLinkTime;
	protected boolean compGroupsGreen;
	protected SignalGroupState oldState;

	private double switchedGreen = 0;

	private DgSensorManager sensorManager;

	
	public void registerAndInitializeSensorManager(DgSensorManager sensorManager) {
		//TODO initialize required information
		this.sensorManager = sensorManager;
	}

	
	
	@Override
	public void updateState(double timeSeconds) {
		// TODO Auto-generated method stub
/*
 * 
 * 
 * 
 * 
 * Algorithm:
 * 
 *  Für alle Gruppen g:
 *    
 *    falls ein stau auf einem der outlinks von g ist und g grün ist   // regel 5 + 6?
 *      trigger den abwurf
 *    
 *    falls kein stau ist und g und alle anderen rot sind // regel 6
 *      schalte g auf grün
 *      
 *    falls kein auto auf grünes signal in d zufährt und mindestens ein auto in d auf ein rotes signal  //regel 4
 *      schalte rot auf grün und grün auf rot
 *    
 *    falls innerhalb von r vor einem grünen signal fahrzeuge befinden // regel 3
 *       schalte nicht auf rot
 *    
 *    falls g grün ist und noch nicht mindestens u zeitschritte grün war // regel 2
 *      blockiere auf rot schalten
 *      
 *    falls g rot ist und die anzahl der wartenden fahrzeuge größer n ist
 *      schalte g auf grün
 *    
 * 
 *   Abfragen:
 *     
 * 
 * 
 * 
 */
		
		
	}


		
	
	@Override
	public void addPlan(SignalPlan plan) {
		//nothing to do here as we don't deal with plans
	}

	@Override
	public void reset(Integer iterationNumber) {
		// TODO Auto-generated method stub
	}

	@Override
	public void setSignalSystem(SignalSystem system) {
		this.system = system;
	}





}
