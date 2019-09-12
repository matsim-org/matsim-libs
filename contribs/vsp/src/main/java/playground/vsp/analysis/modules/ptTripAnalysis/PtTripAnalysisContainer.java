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

package playground.vsp.analysis.modules.ptTripAnalysis;

import org.apache.log4j.Logger;

/**
 * 
 * Simple container holding the data of one file.
 * 
 * @author aneumann
 *
 */
public class PtTripAnalysisContainer {
	
	@SuppressWarnings("unused")
	private static final Logger log = Logger.getLogger(PtTripAnalysisContainer.class);
	
	public enum ZONES {
		INSIDE_ZONE, LEAVING_ZONE, ENTERING_ZONE, OUTSIDE_ZONE
	}
	
	public enum FIELDS {
		sumTTime("Sum of travel time [s]", "Summe der Reisezeiten [s]"),
		tripCnt("Sum of travel time [s]", "Summe der Reisezeiten [s]"),
		accesWalkCnt("Sum of travel time [s]", "Summe der Reisezeiten [s]"),
		accesWaitCnt("Sum of travel time [s]", "Summe der Reisezeiten [s]"),
		egressWalkCnt("Sum of travel time [s]", "Summe der Reisezeiten [s]"),
		switchWalkCnt("Sum of travel time [s]", "Summe der Reisezeiten [s]"),
		switchWaitCnt("Sum of travel time [s]", "Summe der Reisezeiten [s]"),
		lineCnt("Sum of travel time [s]", "Summe der Reisezeiten [s]"),
		accesWalkTTime("Sum of travel time [s]", "Summe der Reisezeiten [s]"),
		accesWaitTime("Sum of travel time [s]", "Summe der Reisezeiten [s]"),
		egressWalkTTime("Sum of travel time [s]", "Summe der Reisezeiten [s]"),
		switchWalkTTime("Sum of travel time [s]", "Summe der Reisezeiten [s]"),
		switchWaitTime("Sum of travel time [s]", "Summe der Reisezeiten [s]"),
		lineTTime("Sum of travel time [s]", "Summe der Reisezeiten [s]"),
		line1cnt("Sum of travel time [s]", "Summe der Reisezeiten [s]"),
		line2cnt("Sum of travel time [s]", "Summe der Reisezeiten [s]"),
		line3cnt("Sum of travel time [s]", "Summe der Reisezeiten [s]"),
		line4cnt("Sum of travel time [s]", "Summe der Reisezeiten [s]"),
		line5cnt("Sum of travel time [s]", "Summe der Reisezeiten [s]"),
		line6cnt("Sum of travel time [s]", "Summe der Reisezeiten [s]"),
		line7cnt("Sum of travel time [s]", "Summe der Reisezeiten [s]"),
		line8cnt("Sum of travel time [s]", "Summe der Reisezeiten [s]"),
		line9cnt("Sum of travel time [s]", "Summe der Reisezeiten [s]"),
		line10cnt("Sum of travel time [s]", "Summe der Reisezeiten [s]"),
		lineGt10cnt("Sum of travel time [s]", "Summe der Reisezeiten [s]"),
		
		avgTT("Avg. door-to-door travel time [s]", "Summe der Reisezeiten [s]"),
		avgAccessWalkTime("Avg. access walk time [s]", "Summe der Reisezeiten [s]"),
		avgSwitchWalkTime("Avg. transfer walk time [s]", "Summe der Reisezeiten [s]"),
		avgEgressWalkTime("Avg. egress walk time [s]", "Summe der Reisezeiten [s]"),
		avgAccessWaitTime("Avg. waiting time at first stop [s]", "Summe der Reisezeiten [s]"),
		avgSwitchWaitTime("Avg. waiting time at transfers [s]", "Summe der Reisezeiten [s]"),
		avgNTransfers("Avg. number of transfers [ ]", "Summe der Reisezeiten [s]");
		
		private String enName;
		private String deName;

		private FIELDS(String enName, String deName){
			this.enName = enName;
			this.deName = deName;
		}
		
		public String getEnName(){
			return this.enName;
		}
		
		public String getDeName(){
			return this.deName;
		}
	}
	
	public final static int nRows = FIELDS.values().length;
	public final static int nColumns = ZONES.values().length;
	
	private double[][] datafields = new double[PtTripAnalysisContainer.nRows][PtTripAnalysisContainer.nColumns];
	
	public void addData(int row, int column, double data){
			datafields[row][column] = data;
	}
	
	public double getData(int row, int column){
		return datafields[row][column];
	}
	
	public void calculateAverages() {
		for (int i = 0; i < PtTripAnalysisContainer.nColumns; i++) {
			datafields[FIELDS.avgTT.ordinal()][i] = datafields[FIELDS.sumTTime.ordinal()][i] / datafields[FIELDS.tripCnt.ordinal()][i];
			datafields[FIELDS.avgAccessWalkTime.ordinal()][i] = datafields[FIELDS.accesWalkTTime.ordinal()][i] / datafields[FIELDS.accesWalkCnt.ordinal()][i];
			datafields[FIELDS.avgSwitchWalkTime.ordinal()][i] = datafields[FIELDS.switchWalkTTime.ordinal()][i] / datafields[FIELDS.switchWalkCnt.ordinal()][i];
			datafields[FIELDS.avgEgressWalkTime.ordinal()][i] = datafields[FIELDS.egressWalkTTime.ordinal()][i] / datafields[FIELDS.egressWalkCnt.ordinal()][i];
			datafields[FIELDS.avgAccessWaitTime.ordinal()][i] = datafields[FIELDS.accesWaitTime.ordinal()][i] / datafields[FIELDS.accesWaitCnt.ordinal()][i];
			datafields[FIELDS.avgSwitchWaitTime.ordinal()][i] = datafields[FIELDS.switchWaitTime.ordinal()][i] / datafields[FIELDS.switchWaitCnt.ordinal()][i];
			datafields[FIELDS.avgNTransfers.ordinal()][i] = datafields[FIELDS.lineCnt.ordinal()][i] / datafields[FIELDS.tripCnt.ordinal()][i] - 1;
		}
	}
}
