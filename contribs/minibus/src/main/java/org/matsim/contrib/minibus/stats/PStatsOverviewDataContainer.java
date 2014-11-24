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

package org.matsim.contrib.minibus.stats;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

import org.apache.log4j.Logger;

/**
 * 
 * Simple container holding the data of one file.
 * 
 * @author aneumann
 *
 */
public class PStatsOverviewDataContainer {
	
	@SuppressWarnings("unused")
	private static final Logger log = Logger.getLogger(PStatsOverviewDataContainer.class);
	
	public enum FIELDS {
		iteration("iter", "Summe der Reisezeiten [s]", new DecimalFormat( "#########0", new DecimalFormatSymbols(Locale.US))),
		nOperators("operators", "Summe der Reisezeiten [s]", new DecimalFormat( "#########0", new DecimalFormatSymbols(Locale.US))),
		nOperatorsInBusiness("+operators", "Summe der Reisezeiten [s]", new DecimalFormat( "#########0", new DecimalFormatSymbols(Locale.US))),
		nRoutes("routes", "Summe der Reisezeiten [s]", new DecimalFormat( "#########0", new DecimalFormatSymbols(Locale.US))),
		nRoutesOfInBusiness("+routes", "Summe der Reisezeiten [s]", new DecimalFormat( "#########0", new DecimalFormatSymbols(Locale.US))),
		nPax("pax", "Summe der Reisezeiten [s]", new DecimalFormat( "#########0", new DecimalFormatSymbols(Locale.US))),
		nPaxServedByInBusiness("+pax", "Summe der Reisezeiten [s]", new DecimalFormat( "#########0", new DecimalFormatSymbols(Locale.US))),
		nVehicle("veh", "Summe der Reisezeiten [s]", new DecimalFormat( "#########0", new DecimalFormatSymbols(Locale.US))),
		nVehicleOfInBusiness("+veh", "Summe der Reisezeiten [s]", new DecimalFormat( "#########0", new DecimalFormatSymbols(Locale.US))),
		
		avgBudgetPerOperator("budget", "Summe der Reisezeiten [s]", new DecimalFormat( "#########0.0000000000", new DecimalFormatSymbols(Locale.US))),
		avgBudgetPerInBusinessOperator("+budget", "Summe der Reisezeiten [s]", new DecimalFormat( "#########0.0000000000", new DecimalFormatSymbols(Locale.US))),
		avgCashflowPerRoute("score", "Summe der Reisezeiten [s]", new DecimalFormat( "#########0.0000000000", new DecimalFormatSymbols(Locale.US))),
		avgCashflowPerRouteOfInBusiness("+score", "Summe der Reisezeiten [s]", new DecimalFormat( "#########0.0000000000", new DecimalFormatSymbols(Locale.US))),
		
		shareOfInBusinessOperators("sharePosOperators", "Summe der Reisezeiten [s]", new DecimalFormat( "#########0.0000000000", new DecimalFormatSymbols(Locale.US))),
		shareOfInBusinessRoutes("sharePosRoutes", "Summe der Reisezeiten [s]", new DecimalFormat( "#########0.0000000000", new DecimalFormatSymbols(Locale.US))),
		shareOfPaxServedByInBusiness("sharePosPax", "Summe der Reisezeiten [s]", new DecimalFormat( "#########0.0000000000", new DecimalFormatSymbols(Locale.US))),
		shareOfVehOfInBusiness("sharePosVeh", "Summe der Reisezeiten [s]", new DecimalFormat( "#########0.0000000000", new DecimalFormatSymbols(Locale.US))),
		
		estimatedMeanOperatorsInBusiness("ESmeanOperators+", "Summe der Reisezeiten [s]", new DecimalFormat( "#########0.0000000000", new DecimalFormatSymbols(Locale.US))),
		estimatedSDOperatorsInBusiness("ESstdDevOperators+", "Summe der Reisezeiten [s]", new DecimalFormat( "#########0.0000000000", new DecimalFormatSymbols(Locale.US))),
		estimatedMeanRouteOfInBusiness("ESmeanRoutes+", "Summe der Reisezeiten [s]", new DecimalFormat( "#########0.0000000000", new DecimalFormatSymbols(Locale.US))),
		estimatedSDRouteOfInBusiness("ESstdDevRoutes+", "Summe der Reisezeiten [s]", new DecimalFormat( "#########0.0000000000", new DecimalFormatSymbols(Locale.US))),
		estimatedMeanPaxServedByInBusiness("ESmeanPax+", "Summe der Reisezeiten [s]", new DecimalFormat( "#########0.0000000000", new DecimalFormatSymbols(Locale.US))),
		estimatedSDPaxServedByInBusiness("ESstdDevPax+", "Summe der Reisezeiten [s]", new DecimalFormat( "#########0.0000000000", new DecimalFormatSymbols(Locale.US))),
		estimatedMeanVehicleOfInBusiness("ESmeanVeh+", "Summe der Reisezeiten [s]", new DecimalFormat( "#########0.0000000000", new DecimalFormatSymbols(Locale.US))),
		estimatedSDVehicleOfInBusiness("ESstdDevVeh+", "Summe der Reisezeiten [s]", new DecimalFormat( "#########0.0000000000", new DecimalFormatSymbols(Locale.US))),
		
		exactMeanOperatorsInBusiness("meanOperators+", "Summe der Reisezeiten [s]", new DecimalFormat( "#########0.0000000000", new DecimalFormatSymbols(Locale.US))),
		exactSDOperatorsInBusiness("stdDevOperators+", "Summe der Reisezeiten [s]", new DecimalFormat( "#########0.0000000000", new DecimalFormatSymbols(Locale.US))),
		exactMeanRouteOfInBusiness("meanRoutes+", "Summe der Reisezeiten [s]", new DecimalFormat( "#########0.0000000000", new DecimalFormatSymbols(Locale.US))),
		exactSDRouteOfInBusiness("stdDevRoutes+", "Summe der Reisezeiten [s]", new DecimalFormat( "#########0.0000000000", new DecimalFormatSymbols(Locale.US))),
		exactMeanPaxServedByInBusiness("meanPax+", "Summe der Reisezeiten [s]", new DecimalFormat( "#########0.0000000000", new DecimalFormatSymbols(Locale.US))),
		exactSDPaxServedByInBusiness("stdDevPax+", "Summe der Reisezeiten [s]", new DecimalFormat( "#########0.0000000000", new DecimalFormatSymbols(Locale.US))),
		exactMeanVehicleOfInBusiness("meanVeh+", "Summe der Reisezeiten [s]", new DecimalFormat( "#########0.0000000000", new DecimalFormatSymbols(Locale.US))),
		exactSDVehicleOfInBusiness("stdDevVeh+", "Summe der Reisezeiten [s]", new DecimalFormat( "#########0.0000000000", new DecimalFormatSymbols(Locale.US)));
		
		private String enName;
		private String deName;
		private DecimalFormat decimalFormat;

		private FIELDS(String enName, String deName, DecimalFormat decimalFormat){
			this.enName = enName;
			this.deName = deName;
			this.decimalFormat = decimalFormat;
		}
		
		public String getEnName(){
			return this.enName;
		}
		
		public String getDeName(){
			return this.deName;
		}
		
		public DecimalFormat getDecimalFormat(){
			return this.decimalFormat;
		}
	}
	
	public final static int nColumns = FIELDS.values().length;
	
	private double[] datafields = new double[PStatsOverviewDataContainer.nColumns];
	
	public void addData(int column, double data){
			datafields[column] = data;
	}
	
	public double getData(int column){
		return datafields[column];
	}
}
