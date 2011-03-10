/* *********************************************************************** *
 * project: org.matsim.*
 * CommuterGenerator
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
package playground.jbischoff.commuterDemand;

/**
 * @author jbischoff
 *
 */
public class CommuterGenerator {

	public static void main(String[] args) {
		
		CommuterDataReader cdr = new CommuterDataReader();
		cdr.addFilterRange(12071000);
		cdr.addFilter("12052000");
		cdr.readFile("/Users/JB/Documents/Work/brb/pendlerdaten_brb.csv");
		
		
		MunicipalityShapeReader msr = new MunicipalityShapeReader();
		msr.addFilter("12052000");
		msr.addFilterRange(12071000);
		msr.readShapeFile("/Users/JB/Documents/Work/brb/gemeinden/dlm_gemeinden.shp");
		
		CommuterDemandWriter cdw = new CommuterDemandWriter(msr.getShapeMap(),cdr.getCommuterRelations());
		cdw.writeDemand("/Users/JB/Documents/Work/brb/demand.xml");
		
	}

}
