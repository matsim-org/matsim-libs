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

package playground.santiago.population;

import java.util.LinkedHashMap;
import java.util.Map;

import org.matsim.api.core.v01.Coord;

public class Persona {
	
	private String id;
	private int age;
	private int sex;
	private boolean drivingLicence;
	private boolean carAvail;
	
	private LinkedHashMap<String, Viaje> viajes = new LinkedHashMap<>();
	
	private Coord homeCoord;
	private Coord workCoord;
//	private Viaje[] viajes;

	private int currentIdx = 0;
	
	public Persona(String id, int age, String  sex, String drivingLicence, int nCarsAvail, String nViajes){
		
		this.id = id;
		this.age = age;
		this.sex = Integer.valueOf(sex);
		this.drivingLicence = setHasDrivingLicence(drivingLicence);
		this.carAvail = nCarsAvail > 0 ? true : false;
		
	}
	
	private boolean setHasDrivingLicence(String drivingLicence) {
		
		if(drivingLicence.equals("1")){
			
			return false;
			
		} else{
			
			return true;
			
		}
		
	}
	
	public void addViaje(Viaje viaje){
		
		this.viajes.put(viaje.getId(), viaje);
		
	}
	
	public String getId() {
		return id;
	}

	public double getAge() {
		return age;
	}

	public int getSex() {
		return sex;
	}

	public boolean hasDrivingLicence() {
		return drivingLicence;
	}
	
	public boolean hasCar(){
		return carAvail;
	}

	public Map<String, Viaje> getViajes() {
		return viajes;
	}

	public Coord getHomeCoord() {
		return homeCoord;
	}

	public void setHomeCoord(Coord homeCoord) {
		this.homeCoord = homeCoord;
	}

	public Coord getWorkCoord() {
		return workCoord;
	}

	public void setWorkCoord(Coord workCoord) {
		this.workCoord = workCoord;
	}

}
