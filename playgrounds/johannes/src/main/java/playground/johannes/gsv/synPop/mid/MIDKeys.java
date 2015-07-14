/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.johannes.gsv.synPop.mid;

/**
 * @author johannes
 *
 */
public interface MIDKeys {

	public static final String HOUSEHOLD_ID = "hhid";
	
	public static final String PERSON_ID = "pid";
	
	public static final String PERSON_MUNICIPALITY = "polgk";
	
	public static final String LEG_START_TIME_HOUR = "st_std";
	
	public static final String LEG_START_TIME_MIN = "st_min";
	
	public static final String LEG_END_TIME_HOUR = "en_std";
	
	public static final String LEG_END_TIME_MIN = "en_min";
	
	public static final String LEG_MAIN_TYPE = "w04";
	
	public static final String LEG_SUB_TYPE = "w04_dzw";
	
	public static final String LEG_ORIGIN = "w01";
	
	public static final String LEG_DESTINATION = "w13";
	
	public static final String LEG_DISTANCE = "wegkm_k";
	
	public static final String LEG_MODE = "hvm";
	
	public static final String PERSON_MUNICIPALITY_CLASS = "inhabClass";
	
	public static final String PERSON_WEIGHT = "p_gew";
	
	public static final String SURVEY_DAY = "stichtag";
	
	public static final String PERSON_STATE = "state";
	
	public static final String LEG_INDEX = "index";
	
	public static final String START_NEXT_DAY = "st_dat";
	
	public static final String END_NEXT_DAY = "en_dat";
	
	public static final String PERSON_MONTH = "month";
	
	public static final String JANUARY = "jan";
	
	public static final String FEBRUARY = "feb";
	
	public static final String MARCH = "mar";
	
	public static final String APRIL = "apr";
	
	public static final String MAY = "may";
	
	public static final String JUNE = "jun";
	
	public static final String JULY = "jul";
	
	public static final String AUGUST = "aug";
	
	public static final String SEPTEMBER = "sep";
	
	public static final String OCTOBER = "oct";
	
	public static final String NOVEMBER = "nov";
	
	public static final String DECEMBER = "dec";
	
	public static final String MID_JOUNREYS = "midjourneys";
	
	public static final String MID_TRIPS = "midtrips";
	
	public static final String JOURNEY_DAYS = "journeydays";
	
	public static final String HH_INCOME = "hheink";
	
	public static final String PERSON_AGE = "hp_alter";
	
	public static final String HH_MEMEBERS = "h02";

	public static final String PERSON_SEX = "hp_sex";

	public static final String PERSON_CARAVAIL = "p01_1";
}
