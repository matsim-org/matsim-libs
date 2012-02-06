/* *********************************************************************** *
 * project: org.matsim.*
 * Plansgenerator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
package playground.droeder.eMobility.v1.subjects;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.geometry.CoordUtils;

import playground.droeder.eMobility.energy.ChargingProfile;
import playground.droeder.eMobility.energy.DisChargingProfile;

/**
 * @author droeder
 *
 */
public class Appointment {
	private Coord startCoord, appCoord, parking1, parking2;
	private Double start, appointStart, appointEnd, chargingStart, ChargingEnd;
	private Id charging;
	private Id disCharging;
//	private String w1,w2,c;
	private boolean finished;
	private int actCnt;
	
	public Appointment(Coord startLocation, Coord appointmentLocation, Double appointmentStart, Double appointmentEnd){
		this(startLocation, appointmentLocation, null, appointmentStart, appointmentEnd);
	}
	
	public Appointment(Coord startLocation, Coord appointmentLocation, Double start, Double appointmentStart, Double appointmentEnd){
		this.startCoord = startLocation;
		this.parking1 = startLocation;
		this.parking2 = appointmentLocation;
		this.appCoord = appointmentLocation;
		this.appointStart = appointmentStart;
		this.appointEnd = appointmentEnd;
		if(start == null){
			this.start = this.appointStart - (1.5 * CoordUtils.calcDistance(this.startCoord, this.appCoord)/13.8);
		}else{
			this.start = start;
		}
		this.chargingStart = appointmentStart;
		this.ChargingEnd = appointmentEnd;
		this.charging = null;
		this.disCharging = new IdImpl("NONE");
		this.finished = false;
	}

	public boolean finished(){
		return this.finished;
	}
	
	public void increase(){
		this.actCnt++;
		if(actCnt == 3){
			this.finished = true;
		}
	}
	
	public boolean isCharging(){
		if(this.actCnt == 2 && !(this.charging == null)){
			return true;
		}else{
			return false;
		}
	}

	/**
	 * @return the parking1
	 */
	public Coord getParking1() {
		return parking1;
	}


	/**
	 * @param parking1 the parking1 to set
	 */
	public void setParking1(Coord parking1) {
		this.parking1 = parking1;
	}


	/**
	 * @return the parking2
	 */
	public Coord getParking2() {
		return parking2;
	}


	/**
	 * @param parking2 the parking2 to set
	 */
	public void setParking2(Coord parking2) {
		this.parking2 = parking2;
	}


	/**
	 * @return the chargingStart
	 */
	public Double getChargingStart() {
		return chargingStart;
	}


	/**
	 * @param chargingStart the chargingStart to set
	 */
	public void setChargingStart(Double chargingStart) {
		this.chargingStart = chargingStart;
	}


	/**
	 * @return the chargingEnd
	 */
	public Double getChargingEnd() {
		return ChargingEnd;
	}


	/**
	 * @param chargingEnd the chargingEnd to set
	 */
	public void setChargingEnd(Double chargingEnd) {
		ChargingEnd = chargingEnd;
	}


	/**
	 * @return the charging
	 */
	public Id getChargingType() {
		return charging;
	}


	/**
	 * @param charging the charging to set
	 */
	public void setCharging(Id charging) {
		this.charging = charging;
	}


	/**
	 * @return the disCharging
	 */
	public Id getDisChargingType() {
		return disCharging;
	}


	/**
	 * @param disCharging the disCharging to set
	 */
	public void setDisCharging(Id disCharging) {
		this.disCharging = disCharging;
	}

	/**
	 * @return the startCoord
	 */
	public Coord getStartCoord() {
		return startCoord;
	}


	/**
	 * @return the appCoord
	 */
	public Coord getAppCoord() {
		return appCoord;
	}


	/**
	 * @return the start
	 */
	public Double getStart() {
		return start;
	}


	/**
	 * @return the appointStart
	 */
	public Double getAppointStart() {
		return appointStart;
	}


	/**
	 * @return the appointEnd
	 */
	public Double getAppointEnd() {
		return appointEnd;
	}

	public List<PlanElement> createMatsimPlanelements(PopulationFactory f, Appointment previousAppointment){
		List<PlanElement> e = new ArrayList<PlanElement>();
		Leg w1, w2, car;
		Activity park1, park2, start, end = null;
		
		if(previousAppointment==null){
			start = createActivity(f, "home",  this.startCoord, 0.0, this.start);
			e.add(start);
		}else{
			start = createActivity(f, "other",  previousAppointment.getParking2(), previousAppointment.getAppointEnd(), this.start);
		}
		park1 = createActivity(f, "multiple", start.getCoord(), start.getEndTime(), start.getEndTime());
		if(!(previousAppointment == null) &&!(park1.getCoord().equals(previousAppointment.getParking2()))){
			System.out.println("falsche Coordinate");
		}
		park2 = createActivity(f, "multiple", this.parking2, this.appointStart, this.appointStart);
		end = createActivity(f, "other", park2.getCoord(), park2.getEndTime(), this.appointEnd);
		w1 = f.createLeg(TransportMode.walk);
		w2 = f.createLeg(TransportMode.walk);
		car = f.createLeg(TransportMode.car);
		
		
		e.add(w1);
		e.add(park1);
		e.add(car);
		e.add(park2);
		e.add(w2);
		e.add(end);
		
		return e;
	}
	
	private Activity createActivity(PopulationFactory f, String type, Coord c, double start, double end){
		Activity a = f.createActivityFromCoord(type, c);
		a.setStartTime(start);
		a.setEndTime(end);
		return a;
	}

}
