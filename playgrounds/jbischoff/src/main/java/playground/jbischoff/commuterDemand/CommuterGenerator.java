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
		cdr.addFilter("12051000"); //BB
//		cdr.addFilter("12052000"); //CB
//		cdr.addFilter("12053000"); //FF
		cdr.addFilter("12054000"); //P
		cdr.addFilter("11000000"); //B
//		cdr.addFilterRange(12060000); //BAR
//		cdr.addFilterRange(12061000); //LDS
//		cdr.addFilterRange(12062000); //EE
		cdr.addFilterRange(12063000); //HVL
//		cdr.addFilterRange(12064000); //MOL
//		cdr.addFilterRange(12065000); //OHV
//		cdr.addFilterRange(12066000); //OSL
//		cdr.addFilterRange(12067000); //LOS
//		cdr.addFilterRange(12068000); //OPR
		cdr.addFilterRange(12069000); //PM
//		cdr.addFilterRange(12070000); //PR
//		cdr.addFilterRange(12071000); //SPN
		cdr.addFilterRange(12072000); //TF
//		cdr.addFilterRange(12073000); //UM
		
		cdr.readFile("/home/jbischoff/matsimkurs/Brandenburg 2009/pendlerdaten_brb.csv");
		
		
		MunicipalityShapeReader msr = new MunicipalityShapeReader();
		msr.addFilter("12051000"); //BB
//		msr.addFilter("12052000"); //CB
//		msr.addFilter("12053000"); //FF
		msr.addFilter("12054000"); //P
		msr.addFilter("11000000");
//		msr.addFilterRange(12060000); //BAR
//		msr.addFilterRange(12061000); //LDS
//		msr.addFilterRange(12062000); //EE
		msr.addFilterRange(12063000); //HVL
//		msr.addFilterRange(12064000); //MOL
//		msr.addFilterRange(12065000); //OHV
//		msr.addFilterRange(12066000); //OSL
//		msr.addFilterRange(12067000); //LOS
//		msr.addFilterRange(12068000); //OPR
		msr.addFilterRange(12069000); //PM
//		msr.addFilterRange(12070000); //PR
//		msr.addFilterRange(12071000); //SPN
		msr.addFilterRange(12072000); //TF
//		msr.addFilterRange(12073000); //UM
	
		msr.readShapeFile("/home/jbischoff/matsimkurs/brandenburg_gemeinde_kreisgrenzen/szen/dlm_gemeinden.shp");
		
		CommuterDemandWriter cdw = new CommuterDemandWriter(msr.getShapeMap(),cdr.getCommuterRelations());
		cdw.setScalefactor(0.01 * 1.29);//1.0 is default already
		cdw.setStart(8);
		cdw.setDuration(9);
		cdw.setOffset(2);
		cdw.writeDemand("/home/jbischoff/matsimkurs/demand.xml");
		
	}

}
