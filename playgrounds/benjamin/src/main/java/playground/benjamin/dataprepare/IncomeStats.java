/* *********************************************************************** *
 * project: org.matsim.*
 * IncomeStats
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.benjamin.dataprepare;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.PriorityQueue;

import org.apache.log4j.Logger;
import org.matsim.core.utils.charts.XYLineChart;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.households.Household;
import org.matsim.households.HouseholdIncomeComparator;
import org.matsim.households.Households;
import org.matsim.households.HouseholdsImpl;
import org.matsim.households.HouseholdsReaderV10;

import playground.benjamin.BkPaths;
import playground.dgrether.analysis.charts.utils.DgChartWriter;


/**
 * @author dgrether
 *
 */
public class IncomeStats {
	
	private static final Logger log = Logger.getLogger(IncomeStats.class);
	
	private final Households households;
	private final double totalIncome;


	public IncomeStats(Households hhs){
		this.households = hhs;
		this.totalIncome = calculateTotalIncome();
		log.error("total income: " + this.totalIncome);

	}
	
	private double calculateTotalIncome(){
		double ti = 0.0;
		for (Household hh : this.households.getHouseholds().values()){
			ti += hh.getIncome().getIncome();
		}
		return ti;
	}

	public void calculateStatistics(String outdir) {
		this.calculateLorenzCurve(outdir);
		this.writeIncomeTable(outdir);
	}
	
	private void writeIncomeTable(String outdir) {
		try {
			BufferedWriter writer = IOUtils.getBufferedWriter(outdir + "hhincomes.txt");
			writer.write("Id \t income \t incomeperiod");
			writer.newLine();
			for (Household hh : this.households.getHouseholds().values()){
				writer.write(hh.getId() + "\t" + hh.getIncome().getIncome() + "\t" + hh.getIncome().getIncomePeriod());
				writer.newLine();
			}
			writer.flush();
			writer.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void calculateLorenzCurve(String outdir) {
		int stepSizePercent = 1;
		PriorityQueue<Household> hhQueue = new PriorityQueue<Household>(this.households.getHouseholds().size(), 
				new HouseholdIncomeComparator());
		hhQueue.addAll(this.households.getHouseholds().values());
    int hhsPerStepSize = this.households.getHouseholds().size() / 100 * stepSizePercent;
		int steps = 100 / stepSizePercent;
    double[] xValues = new double[steps];
		double[] yValues = new double[steps];
		double incomePerStep;
		for (int i = 0; i < steps; i++){
			xValues[i] = i;
			incomePerStep = 0.0;
			for (int j = 0; j < hhsPerStepSize; j++) {
				incomePerStep += hhQueue.poll().getIncome().getIncome();
			}
			if (i != 0) {
				yValues[i] =  yValues[i-1] + incomePerStep  ; 
			}
		}
		
		for (int i = 0; i < yValues.length; i++) {
			yValues[i] = yValues[i] / this.totalIncome;
		}
		
		XYLineChart chart = new XYLineChart("Lorenz", "number of hh percent", "percentage of income");
		chart.addSeries("incomes", xValues, yValues);
		DgChartWriter.writeChartDataToFile(outdir + "lorenz.txt", chart.getChart());
		chart.saveAsPng(outdir + "lorenz.png", 800, 600);
	}


	/**
	 * @param args
	 */
	public static void main(String[] args) {
//	  String outdir = DgPaths.SHAREDSVN + "studies/bkick/oneRouteTwoModeIncomeTest/";
//	  String hhFile = outdir + "households.xml";
	  String outdir = BkPaths.SHAREDSVN + "studies/bkick/einkommenSchweiz/";
	  String hhFile = outdir + "households_all_zrh30km_10pct.xml.gz";
//	  String hhFile = outdir + "households_all_zrh30km_transitincl_10pct.xml.gz";
		Households hhs = new HouseholdsImpl();
		new HouseholdsReaderV10(hhs).readFile(hhFile);
		IncomeStats istats = new IncomeStats(hhs);
		istats.calculateStatistics(outdir);
		log.info("stats written!");
	}
}
