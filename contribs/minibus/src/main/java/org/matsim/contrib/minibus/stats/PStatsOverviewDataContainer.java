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
	
	final static String COMMENTTAG = "#";
	final static String DELIMITER = "\t";
	
	public enum FIELDS {
		iteration("Iteration [ ]", "Iteration [ ]", new DecimalFormat( "#########0", new DecimalFormatSymbols(Locale.US))),
		nOperators("Operators [ ]", "Operators [ ]", new DecimalFormat( "#########0", new DecimalFormatSymbols(Locale.US))),
		nOperatorsInBusiness("Operators IB [ ]", "Operators IB [ ]", new DecimalFormat( "#########0", new DecimalFormatSymbols(Locale.US))),
		nRoutes("Routes [ ]", "Routes [ ]", new DecimalFormat( "#########0", new DecimalFormatSymbols(Locale.US))),
		nRoutesOfInBusiness("Routes IB [ ]", "Routes IB [ ]", new DecimalFormat( "#########0", new DecimalFormatSymbols(Locale.US))),
		nPax("Pax served [ ]", "Pax served [ ]", new DecimalFormat( "#########0", new DecimalFormatSymbols(Locale.US))),
		nPaxServedByInBusiness("Pax served by IB [ ]", "Pax served by IB [ ]", new DecimalFormat( "#########0", new DecimalFormatSymbols(Locale.US))),
		nVehicle("Veh [ ]", "Veh [ ]", new DecimalFormat( "#########0", new DecimalFormatSymbols(Locale.US))),
		nVehicleOfInBusiness("Veh of IB [ ]", "Veh of IB [ ]", new DecimalFormat( "#########0", new DecimalFormatSymbols(Locale.US))),
		
		avgBudgetPerOperator("Avg. budget per operator [$]", "Avg. budget per operator [$]", new DecimalFormat( "#########0.0000000000", new DecimalFormatSymbols(Locale.US))),
		avgBudgetPerInBusinessOperator("Avg. budget per IB operator [$]", "Avg. budget per IB operator [$]", new DecimalFormat( "#########0.0000000000", new DecimalFormatSymbols(Locale.US))),
		avgCashflowPerRoute("Avg. cash flow per route [$]", "Avg. cash flow per route [$]", new DecimalFormat( "#########0.0000000000", new DecimalFormatSymbols(Locale.US))),
		avgCashflowPerRouteOfInBusiness("Avg. cash flow per route of IB [$]", "Avg. cash flow per route of IB [$]", new DecimalFormat( "#########0.0000000000", new DecimalFormatSymbols(Locale.US))),
		
		shareOfInBusinessOperators("Share of IB operators [%]", "Share of IB operators [%]", new DecimalFormat( "#########0.0000000000", new DecimalFormatSymbols(Locale.US))),
		shareOfInBusinessRoutes("Share of IB routes [%]", "Share of IB routes [%]", new DecimalFormat( "#########0.0000000000", new DecimalFormatSymbols(Locale.US))),
		shareOfPaxServedByInBusiness("Share of pax served by IB [%]", "Share of pax served by IB [%]", new DecimalFormat( "#########0.0000000000", new DecimalFormatSymbols(Locale.US))),
		shareOfVehOfInBusiness("Share of veh served by IB [%]", "Share of veh served by IB [%]", new DecimalFormat( "#########0.0000000000", new DecimalFormatSymbols(Locale.US))),
		
		estimatedMeanOperatorsInBusiness("Estimated mean of IB operators [ ]", "Estimated mean of IB operators [ ]", new DecimalFormat( "#########0.0000000000", new DecimalFormatSymbols(Locale.US))),
		estimatedSDOperatorsInBusiness("Estimated SD of IB operators [ ]", "Estimated SD of IB operators [ ]", new DecimalFormat( "#########0.0000000000", new DecimalFormatSymbols(Locale.US))),
		estimatedMeanRouteOfInBusiness("Estimated mean of IB routes [ ]", "Estimated mean of IB routes [ ]", new DecimalFormat( "#########0.0000000000", new DecimalFormatSymbols(Locale.US))),
		estimatedSDRouteOfInBusiness("Estimated SD of IB routes [ ]", "Estimated SD of IB routes [ ]", new DecimalFormat( "#########0.0000000000", new DecimalFormatSymbols(Locale.US))),
		estimatedMeanPaxServedByInBusiness("Estimated mean of pax served by IB [ ]", "Estimated mean of pax served by IB [ ]", new DecimalFormat( "#########0.0000000000", new DecimalFormatSymbols(Locale.US))),
		estimatedSDPaxServedByInBusiness("Estimated SD of pax served by IB [ ]", "Estimated SD of pax served by IB [ ]", new DecimalFormat( "#########0.0000000000", new DecimalFormatSymbols(Locale.US))),
		estimatedMeanVehicleOfInBusiness("Estimated mean of IB veh [ ]", "Estimated mean of IB veh [ ]", new DecimalFormat( "#########0.0000000000", new DecimalFormatSymbols(Locale.US))),
		estimatedSDVehicleOfInBusiness("Estimated SD of IB veh [ ]", "Estimated SD of IB veh [ ]", new DecimalFormat( "#########0.0000000000", new DecimalFormatSymbols(Locale.US))),
		
		exactMeanOperatorsInBusiness("Exact mean of IB operators [ ]", "Exact mean of IB operators [ ]", new DecimalFormat( "#########0.0000000000", new DecimalFormatSymbols(Locale.US))),
		exactSDOperatorsInBusiness("Exact SD of IB operators [ ]", "Exact SD of IB operators [ ]", new DecimalFormat( "#########0.0000000000", new DecimalFormatSymbols(Locale.US))),
		exactMeanRouteOfInBusiness("Exact mean of IB routes [ ]", "Exact mean of IB routes [ ]", new DecimalFormat( "#########0.0000000000", new DecimalFormatSymbols(Locale.US))),
		exactSDRouteOfInBusiness("Exact SD of IB routes [ ]", "Exact SD of IB routes [ ]", new DecimalFormat( "#########0.0000000000", new DecimalFormatSymbols(Locale.US))),
		exactMeanPaxServedByInBusiness("Exact mean of pax served by IB [ ]", "Exact mean of pax served by IB [ ]", new DecimalFormat( "#########0.0000000000", new DecimalFormatSymbols(Locale.US))),
		exactSDPaxServedByInBusiness("Exact SD of pax served by IB [ ]", "Exact SD of pax served by IB [ ]", new DecimalFormat( "#########0.0000000000", new DecimalFormatSymbols(Locale.US))),
		exactMeanVehicleOfInBusiness("Exact mean of IB veh [ ]", "Exact mean of IB veh [ ]", new DecimalFormat( "#########0.0000000000", new DecimalFormatSymbols(Locale.US))),
		exactSDVehicleOfInBusiness("Exact SD of IB veh [ ]", "Exact SD of IB veh [ ]", new DecimalFormat( "#########0.0000000000", new DecimalFormatSymbols(Locale.US)));
		
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
	
	public static String getHeaderLine(){
		StringBuffer strB = new StringBuffer();
		boolean initialized = false;
		for (FIELDS field : FIELDS.values()) {
			if (!initialized) {
				strB.append(COMMENTTAG + " ");
				initialized = true;
			} else {
				strB.append(DELIMITER);
			}
			strB.append(field.getEnName());
		}
		return strB.toString();
	}
}
