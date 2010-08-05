/* *********************************************************************** *
 * project: org.matsim.*
 * ScheduleRecyclingConfigGroup.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package playground.mfeil.config;

import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.core.config.Module;

public class ScheduleRecyclingConfigGroup extends Module {

	private static final long serialVersionUID = 1L;

	public static final String GROUP_NAME = "ScheduleRecycling";

	/* Name of parameters */
	private static final String NO_OF_INDIVIDUAL_AGENTS = "noOfIndividualAgents";
	private static final String NO_OF_RECYCLED_AGENTS = "noOfRecycledAgents";
	private static final String ITERATIONS_FIRST_TIME = "iterationsFirstTime";
	private static final String ITERATIONS_FURTHER_TIMES = "iterationsFurtherTimes";
	private static final String PRIM_ACTS_DISTANCE = "primActsDistance";
	private static final String HOME_LOCATION_DISTANCE = "homeLocationDistance";
	private static final String MUNICIPALITY_TYPE = "municipalityType";
	private static final String GENDER = "gender";
	private static final String AGE = "age";
	private static final String LICENSE_OWNERSHIP = "licenseOwnership";
	private static final String CAR_AVAILABILITY = "carAvailability";
	private static final String EMPLOYMENT = "employment";


	//default values
	// TODO all "static" to be removed later, only bypassing solution
	private static String noOfIndividualAgents = "5";
	private static String noOfRecycledAgents = "10";
	private static String iterationsFirstTime = "20";
	private static String iterationsFurtherTimes = "5";
	private static String primActsDistance = "yes";
	private static String homeLocationDistance = "yes";
	private static String municipalityType = "no";
	private static String gender = "no";
	private static String age = "yes";
	private static String licenseOwnership = "no";
	private static String carAvailability = "no";
	private static String employment = "no";

	private final static Logger log = Logger.getLogger(ScheduleRecyclingConfigGroup.class);


	public ScheduleRecyclingConfigGroup() {
		super(GROUP_NAME);
	}

	@Override
	public String getValue(final String key) {
		if (NO_OF_INDIVIDUAL_AGENTS.equals(key)) {
			return getNoOfIndividualAgents();
		}
		if (NO_OF_RECYCLED_AGENTS.equals(key)) {
			return getNoOfRecycledAgents();
		}
		if (ITERATIONS_FIRST_TIME.equals(key)) {
			return getIterationsFirstTime();
		}
		if (ITERATIONS_FURTHER_TIMES.equals(key)) {
			return getIterationsFurtherTimes();
		}
		if (PRIM_ACTS_DISTANCE.equals(key)) {
			return getIterationsFurtherTimes();
		}
		if (HOME_LOCATION_DISTANCE.equals(key)) {
			return getIterationsFurtherTimes();
		}
		if (MUNICIPALITY_TYPE.equals(key)) {
			return getIterationsFurtherTimes();
		}
		if (GENDER.equals(key)) {
			return getIterationsFurtherTimes();
		}
		if (AGE.equals(key)) {
			return getIterationsFurtherTimes();
		}
		if (LICENSE_OWNERSHIP.equals(key)) {
			return getIterationsFurtherTimes();
		}
		if (CAR_AVAILABILITY.equals(key)) {
			return getIterationsFurtherTimes();
		}
		if (EMPLOYMENT.equals(key)) {
			return getIterationsFurtherTimes();
		}
		throw new IllegalArgumentException(key);
	}

	@Override
	public void addParam(final String key, final String value) {
		if (NO_OF_INDIVIDUAL_AGENTS.equals(key)) {
			if (Integer.parseInt(value)<1) {
				log.warn("Parameter NO_OF_INDIVIDUAL_AGENTS has been set to "+value+" but must be equal to or greater than 1. The default value of 5 will be used instead.");
			}
			else {
				setNoOfIndividualAgents(value);
			}


		} else if (NO_OF_RECYCLED_AGENTS.equals(key)) {
			if (Integer.parseInt(value) < 1) {
				log.warn("Parameter NO_OF_RECYCLED_AGENTS has been set to "+value+" but must be equal to or greater than 1. The default value of 10 will be used instead.");
			}
			else {
				setNoOfRecycledAgents(value);
			}


		} else if (ITERATIONS_FIRST_TIME.equals(key)) {
			if ((Integer.parseInt(value)) < 1) {
				log.warn("Parameter ITERATIONS_FIRST_TIME has been set to "+value+" but must be equal to or greater than 1. The default value of 20 will be used instead.");
			}
			else {
				setIterationsFirstTime(value);
			}

		} else if (ITERATIONS_FURTHER_TIMES.equals(key)) {
			if ((Double.parseDouble(value)) < 1) {
				log.warn("Parameter ITERATIONS_FURTHER_TIMES has been set to "+value+" but must be equal to or greater than 1sec. The default value of 5 will be used instead.");
			}
			else {
				setIterationsFurtherTimes(value);
			}
			
		} else if (PRIM_ACTS_DISTANCE.equals(key)) {
			if (value.equals("yes") || value.equals("no")) {
				setPrimActsDistance(value);}
			else {
				log.warn("Parameter PRIM_ACTS_DISTANCE has been set to "+value+" but must either 'yes' or 'no'. The default value 'yes' will be used instead.");
			}
			
		} else if (HOME_LOCATION_DISTANCE.equals(key)) {
			if (value.equals("yes") || value.equals("no")) {
				setHomeLocationDistance(value);}
			else {
				log.warn("Parameter HOME_LOCATION_DISTANCE has been set to "+value+" but must either 'yes' or 'no'. The default value 'yes' will be used instead.");
			}
			
		} else if (MUNICIPALITY_TYPE.equals(key)) {
			if (value.equals("yes") || value.equals("no")) {
				setMunicipalityType(value);}
			else {
				log.warn("Parameter MUNICIPALITY_TYPE has been set to "+value+" but must either 'yes' or 'no'. The default value 'no' will be used instead.");
			}
			
		} else if (GENDER.equals(key)) {
			if (value.equals("yes") || value.equals("no")) {
				setGender(value);}
			else {
				log.warn("Parameter GENDER has been set to "+value+" but must either 'yes' or 'no'. The default value 'no' will be used instead.");
			}
			
		} else if (AGE.equals(key)) {
			if (value.equals("yes") || value.equals("no")) {
				setAge(value);}
			else {
				log.warn("Parameter AGE has been set to "+value+" but must either 'yes' or 'no'. The default value 'yes' will be used instead.");
			}
			
		} else if (LICENSE_OWNERSHIP.equals(key)) {
			if (value.equals("yes") || value.equals("no")) {
				setLicenseOwnership(value);}
			else {
				log.warn("Parameter LICENSE_OWNERSHIP has been set to "+value+" but must either 'yes' or 'no'. The default value 'no' will be used instead.");
			}
			
		} else if (CAR_AVAILABILITY.equals(key)) {
			if (value.equals("yes") || value.equals("no")) {
				setCarAvailability(value);}
			else {
				log.warn("Parameter CAR_AVAILABILITY has been set to "+value+" but must either 'yes' or 'no'. The default value 'no' will be used instead.");
			}
			
		} else if (EMPLOYMENT.equals(key)) {
			if (value.equals("yes") || value.equals("no")) {
				setEmployment(value);}
			else {
				log.warn("Parameter EMPLOYMENT has been set to "+value+" but must either 'yes' or 'no'. The default value 'no' will be used instead.");
			}
			
		} else throw new IllegalArgumentException(key);
	}

	@Override
	public final TreeMap<String, String> getParams() {
		TreeMap<String, String> map = new TreeMap<String, String>();
		this.addParameterToMap(map, NO_OF_INDIVIDUAL_AGENTS);
		this.addParameterToMap(map, NO_OF_RECYCLED_AGENTS);
		this.addParameterToMap(map, ITERATIONS_FIRST_TIME);
		this.addParameterToMap(map, ITERATIONS_FURTHER_TIMES);
		this.addParameterToMap(map, PRIM_ACTS_DISTANCE);
		this.addParameterToMap(map, HOME_LOCATION_DISTANCE);
		this.addParameterToMap(map, MUNICIPALITY_TYPE);
		this.addParameterToMap(map, GENDER);
		this.addParameterToMap(map, AGE);
		this.addParameterToMap(map, LICENSE_OWNERSHIP);
		this.addParameterToMap(map, CAR_AVAILABILITY);
		this.addParameterToMap(map, EMPLOYMENT);
		return map;
	}

	// TODO all "static" to be removed later, only bypassing solution
	public static String getNoOfIndividualAgents() {
		return noOfIndividualAgents;
	}
	public void setNoOfIndividualAgents(final String string) {
		this.noOfIndividualAgents = string;
	}
	public static String getNoOfRecycledAgents() {
		return noOfRecycledAgents;
	}
	public void setNoOfRecycledAgents(final String string) {
		this.noOfRecycledAgents = string;
	}
	public static String getIterationsFirstTime() {
		return iterationsFirstTime;
	}
	public void setIterationsFirstTime(final String string) {
		this.iterationsFirstTime = string;
	}
	public static String getPrimActsDistance() {
		return primActsDistance;
	}
	public void setPrimActsDistance(final String string) {
		this.primActsDistance = string;
	}
	public static String getIterationsFurtherTimes() {
		return iterationsFurtherTimes;
	}
	public void setIterationsFurtherTimes(final String string) {
		this.iterationsFurtherTimes = string;
	}
	public static String getHomeLocationDistance() {
		return homeLocationDistance;
	}
	public void setHomeLocationDistance(final String string) {
		this.homeLocationDistance = string;
	}
	public static String getMunicipalityType() {
		return municipalityType;
	}
	public void setMunicipalityType(final String string) {
		this.municipalityType = string;
	}
	public static String getGender() {
		return gender;
	}
	public void setGender(final String string) {
		this.gender = string;
	}
	public static String getAge() {
		return age;
	}
	public void setAge(final String string) {
		this.age = string;
	}
	public static String getLicenseOwnership() {
		return licenseOwnership;
	}
	public void setLicenseOwnership(final String string) {
		this.licenseOwnership = string;
	}
	public static String getCarAvailability() {
		return carAvailability;
	}
	public void setCarAvailability(final String string) {
		this.carAvailability = string;
	}
	public static String getEmployment() {
		return employment;
	}
	public void setEmployment(final String string) {
		this.employment = string;
	}
}
