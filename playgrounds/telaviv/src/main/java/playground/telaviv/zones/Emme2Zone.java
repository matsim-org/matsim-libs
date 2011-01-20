/* *********************************************************************** *
 * project: org.matsim.*
 * Emme2Zone.java
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

package playground.telaviv.zones;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Id;

/*
 * For a description of the data fields look at the
 * "final report" pdf, pages 2-5 and 2-6.
 */
public class Emme2Zone {

	public List<Id> linkIds = new ArrayList<Id>();
	
	public int TAZ;
	public double AREA;
	public int TYPE;
	public int CULTURAL;
	public int EDUCATION;
	public int OFFICE;
	public int SHOPPING;
	public int HEALTH;
	public int RELIGIOSIT;
	public int URBAN;
	public int TRANSPORTA;
	public int EMPL_INDU;
	public int EMPL_COMM;
	public int EMPL_SERV;
	public int EMPL_TOT;
	public int STUDENTS;
	public int POPULATION;
	public int HOUSEHOLDS;
	public int PARKCOST;
	public int PARKWALK;
	public double POPDENS;
	public int GA21;
	public int GA22;
	public int GA23;
	public int GA24;
	public int GA25;
	public int GA26;
	public int GA11;
	public int GA12;
	public int GA13;
	public int GA14;
	public int GA15;
	public int GA16;
	public int WORKERS;
	public double WORKPERC;
	public double AVGHH;
	public int SOCECO;
	public double MLIC2;
	public double MLIC3;
	public double MLIC4;
	public double MLIC5;
	public double MLIC6;
	public double FLIC2;
	public double FLIC3;
	public double FLIC4;
	public double FLIC5;
	public double FLIC6;
	public double EMP2POP;
	public int PARKCAP;
	public double PARKAM;
	public double PARKPM;
	public double PARKOP;
	public double INTLSUME;
	public double INTLSUMO;
	public double INTLSUMS;
	public double INTLSUMW;
	public double LSIZESEC;
	public double LSIZEINTS;
	public double LSUMSIZE0;
	public double LSUMSIZES;
	public int SUPERZONE;
}
