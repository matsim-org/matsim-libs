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

	public 	static void main(String[] args) {
		
		CommuterDataReader cdr = new CommuterDataReader();
//		cdr.addFilter("12051000"); //BB
		cdr.addFilter("12054000"); //P
//		cdr.addFilterRange(11000000); //B
		cdr.addFilterRange(12069000); //PM
		cdr.addFilterRange(12072000); //TF
		cdr.addFilterRange(12063000); //HVL
		cdr.addFilterRange(12069000); //PM
//		cdr.addFilterRange(12065000); //OHV
		
		cdr.readFile("/home/jbischoff/matsimkurs/Brandenburg 2009/pendlerdaten_brb.csv");
		
		
		MunicipalityShapeReader msr = new MunicipalityShapeReader();
//		msr.addFilter("12051000"); //BB
		msr.addFilter("12054000"); //P
//		msr.addFilterRange(11000000); //B
		msr.addFilterRange(12069000); //PM
		msr.addFilterRange(12072000); //TF
		msr.addFilterRange(12063000); //HVL
		msr.addFilterRange(12069000); //PM
//		msr.addFilterRange(12065000); //OHV

		
		msr.readShapeFile("/home/jbischoff/matsimkurs/brandenburg_gemeinde_kreisgrenzen/gemeinden/dlm_gemeinden.shp");
		
		CommuterDemandWriter cdw = new CommuterDemandWriter(msr.getShapeMap(),cdr.getCommuterRelations());
		cdw.setScalefactor(0.1);//1.0 is default already
		cdw.writeDemand("/home/jbischoff/matsimkurs/demand.xml");
		
	}

}
