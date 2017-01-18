/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

import java.io.BufferedWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

import org.apache.log4j.Logger;
import org.matsim.contrib.minibus.PConfigGroup;
import org.matsim.contrib.minibus.PConstants.OperatorState;
import org.matsim.contrib.minibus.operator.Operator;
import org.matsim.contrib.minibus.operator.POperators;
import org.matsim.contrib.minibus.operator.PPlan;
import org.matsim.contrib.minibus.stats.PStatsOverviewDataContainer.FIELDS;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.utils.charts.XYLineChart;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.UncheckedIOException;

/**
 * Calculates at the end of each iteration the following statistics:
 * <ul>
 * <li>average score of the operators</li>
 * <li>average positive score of the operators</li>
 * <li>average budget of the operators</li>
 * <li>average vehicle fleet size of the operators</li>
 * <li>average score per vehicle</li>
 * <li>average number of trips served</li>
 * <li>percentage of operators with positive score</li>
 * </ul>
 * The calculated values are written to a file, each iteration on a separate line, and as png.
 *
 * @author aneumann based on {@link org.matsim.analysis.ScoreStatsControlerListener} by mrieser
 */
final class PStatsOverview implements StartupListener, IterationEndsListener, ShutdownListener {

	private final static Logger log = Logger.getLogger(PStatsOverview.class);
	
	enum INDEX {
		INDEX_NOPERATORS("N operators", "N operators", new DecimalFormat( "#########0", new DecimalFormatSymbols(Locale.US))),
		INDEX_NOPERATORSPOS("N pos operators", "N pos operators", new DecimalFormat( "#########0", new DecimalFormatSymbols(Locale.US))),
		INDEX_NROUTES("N routes", "N routes", new DecimalFormat( "#########0", new DecimalFormatSymbols(Locale.US))),
		INDEX_NROUTESPOS("N pos routes", "N pos routes", new DecimalFormat( "#########0", new DecimalFormatSymbols(Locale.US))),
		INDEX_NPAX("N pax", "N pax", new DecimalFormat( "#########0", new DecimalFormatSymbols(Locale.US))),
		INDEX_NPAXPOS("N pos pax", "N pos pax", new DecimalFormat( "#########0", new DecimalFormatSymbols(Locale.US))),
		INDEX_NVEH("N veh", "N veh", new DecimalFormat( "#########0", new DecimalFormatSymbols(Locale.US))),
		INDEX_NVEHPOS("N pos veh", "N pos veh", new DecimalFormat( "#########0", new DecimalFormatSymbols(Locale.US))),
		
		INDEX_NBUDGET("budget per operator", "budget per operator", new DecimalFormat( "#########0", new DecimalFormatSymbols(Locale.US))),
		INDEX_NBUDGETPOS("pos budget per pos operator", "pos budget per pos operator", new DecimalFormat( "#########0.00", new DecimalFormatSymbols(Locale.US))),
		INDEX_NSCORE("score per route", "score per route", new DecimalFormat( "#########0.00", new DecimalFormatSymbols(Locale.US))),
		INDEX_NSCOREPOS("pos score per pos route", "pos score per pos route", new DecimalFormat( "#########0.00", new DecimalFormatSymbols(Locale.US))),
		
		INDEX_SHAREPOSOPERATORS("share pos operators", "share pos operators", new DecimalFormat( "#########0.00", new DecimalFormatSymbols(Locale.US))),
		INDEX_SHAREPOSROUTES("share pos routes", "share pos routes", new DecimalFormat( "#########0.0", new DecimalFormatSymbols(Locale.US))),
		INDEX_SHAREPOSPAX("share pos pax", "share pos pax", new DecimalFormat( "#########0.0", new DecimalFormatSymbols(Locale.US))),
		INDEX_SHAREPOSVEH("share pos veh", "share pos veh", new DecimalFormat( "#########0.0", new DecimalFormatSymbols(Locale.US))),
		
		INDEX_MEANPOSOPERATORS("average number of pos operators", "average number of pos operators", new DecimalFormat( "#########0.0", new DecimalFormatSymbols(Locale.US))),
		INDEX_MEANPOSROUTES("average number of pos routes", "average number of pos routes", new DecimalFormat( "#########0.00000", new DecimalFormatSymbols(Locale.US))),
		INDEX_MEANPOSPAX("average number of pos pax", "average number of pos pax", new DecimalFormat( "#########0.00000", new DecimalFormatSymbols(Locale.US))),
		INDEX_MEANPOSVEH("average number of pos veh", "average number of pos veh", new DecimalFormat( "#########0.00000", new DecimalFormatSymbols(Locale.US))),

		INDEX_SIGMAUPPERPOSOPERATORS("average number of pos operators + 1 sigma", "average number of pos operators + 1 sigma", new DecimalFormat( "#########0.00000", new DecimalFormatSymbols(Locale.US))),
		INDEX_SIGMAUPPERPOSROUTES("average number of pos routes + 1 sigma", "average number of pos routes + 1 sigma", new DecimalFormat( "#########0.00000", new DecimalFormatSymbols(Locale.US))),
		INDEX_SIGMAUPPERPOSPAX("average number of pos pax + 1 sigma", "average number of pos pax + 1 sigma", new DecimalFormat( "#########0.00000", new DecimalFormatSymbols(Locale.US))),
		INDEX_SIGMAUPPERPOSVEH("average number of pos veh + 1 sigma", "average number of pos veh + 1 sigma", new DecimalFormat( "#########0.00000", new DecimalFormatSymbols(Locale.US))),
		
		INDEX_SIGMALOWERPOSOPERATORS("average number of pos operators - 1 sigma", "average number of pos operators - 1 sigma", new DecimalFormat( "#########0.00000", new DecimalFormatSymbols(Locale.US))),
		INDEX_SIGMALOWERPOSROUTES("average number of pos routes - 1 sigma", "average number of pos routes - 1 sigma", new DecimalFormat( "#########0.00000", new DecimalFormatSymbols(Locale.US))),
		INDEX_SIGMALOWERPOSPAX("average number of pos pax - 1 sigma", "average number of pos pax - 1 sigma", new DecimalFormat( "#########0.00000", new DecimalFormatSymbols(Locale.US))),
		INDEX_SIGMALOWERPOSVEH("average number of pos veh - 1 sigma", "average number of pos veh - 1 sigma", new DecimalFormat( "#########0.00000", new DecimalFormatSymbols(Locale.US)));
		
		private String enName;
		private String deName;
		private DecimalFormat decimalFormat;

		private INDEX(String enName, String deName, DecimalFormat decimalFormat){
			this.enName = enName;
			this.deName = deName;
			this.decimalFormat = decimalFormat;
		}
		
		private String getEnName(){
			return this.enName;
		}
		
		private String getDeName(){
			return this.deName;
		}
		
		private DecimalFormat getDecimalFormat(){
			return this.decimalFormat;
		}
	}
	
	private BufferedWriter pStatsWriter;

	private double[][] history = null;
	private int minIteration = 0;
	@Inject private POperators operators;
	@Inject private PConfigGroup pConfig;

	private RecursiveStatsContainer statsContainer;
	private RecursiveStatsApproxContainer statsApproxContainer;

	public PStatsOverview() {
	}

	@Override
	public void notifyStartup(final StartupEvent event) {
		
		MatsimServices controler = event.getServices();
		
		if(this.pConfig.getWriteStatsInterval() > 0){
			log.info("enabled");
			this.pStatsWriter = IOUtils.getBufferedWriter(controler.getControlerIO().getOutputFilename("pStats.txt"));
			try {
				this.pStatsWriter.write(PStatsOverviewDataContainer.getHeaderLine());
				this.pStatsWriter.newLine();
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		} else {
			this.pStatsWriter = null;
		}		
		
		this.minIteration = controler.getConfig().controler().getFirstIteration();
		int maxIter = controler.getConfig().controler().getLastIteration();
		int iterations = maxIter - this.minIteration;
		if (iterations > 10000) iterations = 10000; // limit the history size
		this.history = new double[29][iterations+1];
		this.statsContainer = new RecursiveStatsContainer();
		this.statsApproxContainer = new RecursiveStatsApproxContainer(0.1, 10);
	}

	@Override
	public void notifyIterationEnds(final IterationEndsEvent event) {
		if(this.pConfig.getWriteStatsInterval() > 0){
			
			PStatsOverviewDataContainer pStats = new PStatsOverviewDataContainer();
			
			pStats.addData(PStatsOverviewDataContainer.FIELDS.iteration.ordinal(), event.getIteration());
			
			pStats.addData(PStatsOverviewDataContainer.FIELDS.nOperators.ordinal(), 0.0);
			pStats.addData(PStatsOverviewDataContainer.FIELDS.nOperatorsInBusiness.ordinal(), 0.0);
			pStats.addData(PStatsOverviewDataContainer.FIELDS.nRoutes.ordinal(), 0.0);
			pStats.addData(PStatsOverviewDataContainer.FIELDS.nRoutesOfInBusiness.ordinal(), 0.0);
			pStats.addData(PStatsOverviewDataContainer.FIELDS.nPax.ordinal(), 0.0);
			pStats.addData(PStatsOverviewDataContainer.FIELDS.nPaxServedByInBusiness.ordinal(), 0.0);
			pStats.addData(PStatsOverviewDataContainer.FIELDS.nVehicle.ordinal(), 0.0);
			pStats.addData(PStatsOverviewDataContainer.FIELDS.nVehicleOfInBusiness.ordinal(), 0.0);
			pStats.addData(PStatsOverviewDataContainer.FIELDS.avgBudgetPerOperator.ordinal(), 0.0);
			pStats.addData(PStatsOverviewDataContainer.FIELDS.avgBudgetPerInBusinessOperator.ordinal(), 0.0);
			pStats.addData(PStatsOverviewDataContainer.FIELDS.avgCashflowPerRoute.ordinal(), 0.0);
			pStats.addData(PStatsOverviewDataContainer.FIELDS.avgCashflowPerRouteOfInBusiness.ordinal(), 0.0);

			for (Operator operator : this.operators.getOperators()) {
				List<PPlan> plans = operator.getAllPlans();
				
				double operatorRoutes = 0.0;
				double operatorPax = 0.0;
				double operatorVeh = 0.0;
				double operatorScore = 0.0;				

				for (PPlan plan : plans) {
					operatorRoutes++;
					operatorPax += plan.getTripsServed();
					operatorVeh += plan.getNVehicles();
					operatorScore += plan.getScore();
				}
				
				pStats.addData(PStatsOverviewDataContainer.FIELDS.nOperators.ordinal(), pStats.getData(PStatsOverviewDataContainer.FIELDS.nOperators.ordinal()) + 1.0);
				pStats.addData(PStatsOverviewDataContainer.FIELDS.nRoutes.ordinal(), pStats.getData(PStatsOverviewDataContainer.FIELDS.nRoutes.ordinal()) + operatorRoutes);
				pStats.addData(PStatsOverviewDataContainer.FIELDS.nPax.ordinal(), pStats.getData(PStatsOverviewDataContainer.FIELDS.nPax.ordinal()) + operatorPax);
				pStats.addData(PStatsOverviewDataContainer.FIELDS.nVehicle.ordinal(), pStats.getData(PStatsOverviewDataContainer.FIELDS.nVehicle.ordinal()) + operatorVeh);
				
				pStats.addData(PStatsOverviewDataContainer.FIELDS.avgBudgetPerOperator.ordinal(), pStats.getData(PStatsOverviewDataContainer.FIELDS.avgBudgetPerOperator.ordinal()) + operator.getBudget());
				pStats.addData(PStatsOverviewDataContainer.FIELDS.avgCashflowPerRoute.ordinal(), pStats.getData(PStatsOverviewDataContainer.FIELDS.avgCashflowPerRoute.ordinal()) + operatorScore);
				
				// statistics for each operator in business
				if(operator.getOperatorState().equals(OperatorState.INBUSINESS)){
					pStats.addData(PStatsOverviewDataContainer.FIELDS.nOperatorsInBusiness.ordinal(), pStats.getData(PStatsOverviewDataContainer.FIELDS.nOperatorsInBusiness.ordinal()) + 1.0);
					pStats.addData(PStatsOverviewDataContainer.FIELDS.nRoutesOfInBusiness.ordinal(), pStats.getData(PStatsOverviewDataContainer.FIELDS.nRoutesOfInBusiness.ordinal()) + operatorRoutes);
					pStats.addData(PStatsOverviewDataContainer.FIELDS.nPaxServedByInBusiness.ordinal(), pStats.getData(PStatsOverviewDataContainer.FIELDS.nPaxServedByInBusiness.ordinal()) + operatorPax);
					pStats.addData(PStatsOverviewDataContainer.FIELDS.nVehicleOfInBusiness.ordinal(), pStats.getData(PStatsOverviewDataContainer.FIELDS.nVehicleOfInBusiness.ordinal()) + operatorVeh);
					
					pStats.addData(PStatsOverviewDataContainer.FIELDS.avgBudgetPerInBusinessOperator.ordinal(), pStats.getData(PStatsOverviewDataContainer.FIELDS.avgBudgetPerInBusinessOperator.ordinal()) + operator.getBudget());
					pStats.addData(PStatsOverviewDataContainer.FIELDS.avgCashflowPerRouteOfInBusiness.ordinal(), pStats.getData(PStatsOverviewDataContainer.FIELDS.avgCashflowPerRouteOfInBusiness.ordinal()) + operatorScore);
				}				
			}
			
			pStats.addData(PStatsOverviewDataContainer.FIELDS.shareOfInBusinessOperators.ordinal(), pStats.getData(PStatsOverviewDataContainer.FIELDS.nOperatorsInBusiness.ordinal()) / pStats.getData(PStatsOverviewDataContainer.FIELDS.nOperators.ordinal()) * 100.0);
			pStats.addData(PStatsOverviewDataContainer.FIELDS.shareOfInBusinessRoutes.ordinal(), pStats.getData(PStatsOverviewDataContainer.FIELDS.nRoutesOfInBusiness.ordinal()) / pStats.getData(PStatsOverviewDataContainer.FIELDS.nRoutes.ordinal()) * 100.0);
			pStats.addData(PStatsOverviewDataContainer.FIELDS.shareOfPaxServedByInBusiness.ordinal(), pStats.getData(PStatsOverviewDataContainer.FIELDS.nPaxServedByInBusiness.ordinal()) / pStats.getData(PStatsOverviewDataContainer.FIELDS.nPax.ordinal()) * 100.0);
			pStats.addData(PStatsOverviewDataContainer.FIELDS.shareOfVehOfInBusiness.ordinal(), pStats.getData(PStatsOverviewDataContainer.FIELDS.nVehicleOfInBusiness.ordinal()) / pStats.getData(PStatsOverviewDataContainer.FIELDS.nVehicle.ordinal()) * 100.0);
			
			
			this.statsContainer.handleNewEntry(pStats.getData(PStatsOverviewDataContainer.FIELDS.nOperatorsInBusiness.ordinal()), pStats.getData(PStatsOverviewDataContainer.FIELDS.nRoutesOfInBusiness.ordinal()), pStats.getData(PStatsOverviewDataContainer.FIELDS.nPaxServedByInBusiness.ordinal()), pStats.getData(PStatsOverviewDataContainer.FIELDS.nVehicleOfInBusiness.ordinal()));
			this.statsApproxContainer.handleNewEntry(pStats.getData(PStatsOverviewDataContainer.FIELDS.nOperatorsInBusiness.ordinal()), pStats.getData(PStatsOverviewDataContainer.FIELDS.nRoutesOfInBusiness.ordinal()), pStats.getData(PStatsOverviewDataContainer.FIELDS.nPaxServedByInBusiness.ordinal()), pStats.getData(PStatsOverviewDataContainer.FIELDS.nVehicleOfInBusiness.ordinal()));
			
			
			pStats.addData(PStatsOverviewDataContainer.FIELDS.avgBudgetPerOperator.ordinal(), pStats.getData(PStatsOverviewDataContainer.FIELDS.avgBudgetPerOperator.ordinal()) / pStats.getData(PStatsOverviewDataContainer.FIELDS.nOperators.ordinal()));
			pStats.addData(PStatsOverviewDataContainer.FIELDS.avgBudgetPerInBusinessOperator.ordinal(), pStats.getData(PStatsOverviewDataContainer.FIELDS.avgBudgetPerInBusinessOperator.ordinal()) / pStats.getData(PStatsOverviewDataContainer.FIELDS.nOperatorsInBusiness.ordinal()));
			
			pStats.addData(PStatsOverviewDataContainer.FIELDS.avgCashflowPerRoute.ordinal(), pStats.getData(PStatsOverviewDataContainer.FIELDS.avgCashflowPerRoute.ordinal()) / pStats.getData(PStatsOverviewDataContainer.FIELDS.nRoutes.ordinal()));
			pStats.addData(PStatsOverviewDataContainer.FIELDS.avgCashflowPerRouteOfInBusiness.ordinal(), pStats.getData(PStatsOverviewDataContainer.FIELDS.avgCashflowPerRouteOfInBusiness.ordinal()) / pStats.getData(PStatsOverviewDataContainer.FIELDS.nRoutesOfInBusiness.ordinal()));
			
			pStats.addData(PStatsOverviewDataContainer.FIELDS.estimatedMeanOperatorsInBusiness.ordinal(), statsApproxContainer.getArithmeticMeanOperators());
			pStats.addData(PStatsOverviewDataContainer.FIELDS.estimatedSDOperatorsInBusiness.ordinal(), statsApproxContainer.getStdDevOperators());

			pStats.addData(PStatsOverviewDataContainer.FIELDS.estimatedMeanRouteOfInBusiness.ordinal(), statsApproxContainer.getArithmeticMeanRoutes());
			pStats.addData(PStatsOverviewDataContainer.FIELDS.estimatedSDRouteOfInBusiness.ordinal(), statsApproxContainer.getStdDevRoutes());
			
			pStats.addData(PStatsOverviewDataContainer.FIELDS.estimatedMeanPaxServedByInBusiness.ordinal(), statsApproxContainer.getArithmeticMeanPax());
			pStats.addData(PStatsOverviewDataContainer.FIELDS.estimatedSDPaxServedByInBusiness.ordinal(), statsApproxContainer.getStdDevPax());
			
			pStats.addData(PStatsOverviewDataContainer.FIELDS.estimatedMeanVehicleOfInBusiness.ordinal(), statsApproxContainer.getArithmeticMeanVeh());
			pStats.addData(PStatsOverviewDataContainer.FIELDS.estimatedSDVehicleOfInBusiness.ordinal(), statsApproxContainer.getStdDevVeh());
			
			pStats.addData(PStatsOverviewDataContainer.FIELDS.exactMeanOperatorsInBusiness.ordinal(), statsContainer.getArithmeticMeanOperators());
			pStats.addData(PStatsOverviewDataContainer.FIELDS.exactSDOperatorsInBusiness.ordinal(), statsContainer.getStdDevOperators());

			pStats.addData(PStatsOverviewDataContainer.FIELDS.exactMeanRouteOfInBusiness.ordinal(), statsContainer.getArithmeticMeanRoutes());
			pStats.addData(PStatsOverviewDataContainer.FIELDS.exactSDRouteOfInBusiness.ordinal(), statsContainer.getStdDevRoutes());
			
			pStats.addData(PStatsOverviewDataContainer.FIELDS.exactMeanPaxServedByInBusiness.ordinal(), statsContainer.getArithmeticMeanPax());
			pStats.addData(PStatsOverviewDataContainer.FIELDS.exactSDPaxServedByInBusiness.ordinal(), statsContainer.getStdDevPax());
			
			pStats.addData(PStatsOverviewDataContainer.FIELDS.exactMeanVehicleOfInBusiness.ordinal(), statsContainer.getArithmeticMeanVeh());
			pStats.addData(PStatsOverviewDataContainer.FIELDS.exactSDVehicleOfInBusiness.ordinal(), statsContainer.getStdDevVeh());
			
			try {
				StringBuffer strB = new StringBuffer();
				for (FIELDS field : PStatsOverviewDataContainer.FIELDS.values()) {
					String value;
					
					if (Double.isNaN(pStats.getData(field.ordinal()))) {
						value = Double.toString(pStats.getData(field.ordinal()));
					} else {
						value = field.getDecimalFormat().format(pStats.getData(field.ordinal()));
					}
					
					strB.append(value).append(PStatsOverviewDataContainer.DELIMITER);
				}
				strB.append("\n");
				this.pStatsWriter.write(strB.toString());
				this.pStatsWriter.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}

			if (this.history != null) {
				int index = event.getIteration() - this.minIteration;
				
				this.history[INDEX.INDEX_NOPERATORS.ordinal()][index] = pStats.getData(PStatsOverviewDataContainer.FIELDS.nOperators.ordinal());
				this.history[INDEX.INDEX_NOPERATORSPOS.ordinal()][index] = pStats.getData(PStatsOverviewDataContainer.FIELDS.nOperatorsInBusiness.ordinal());
				this.history[INDEX.INDEX_NROUTES.ordinal()][index] = pStats.getData(PStatsOverviewDataContainer.FIELDS.nRoutes.ordinal());
				this.history[INDEX.INDEX_NROUTESPOS.ordinal()][index] = pStats.getData(PStatsOverviewDataContainer.FIELDS.nRoutesOfInBusiness.ordinal());
				this.history[INDEX.INDEX_NPAX.ordinal()][index] = pStats.getData(PStatsOverviewDataContainer.FIELDS.nPax.ordinal());
				this.history[INDEX.INDEX_NPAXPOS.ordinal()][index] = pStats.getData(PStatsOverviewDataContainer.FIELDS.nPaxServedByInBusiness.ordinal());
				this.history[INDEX.INDEX_NVEH.ordinal()][index] = pStats.getData(PStatsOverviewDataContainer.FIELDS.nVehicle.ordinal());
				this.history[INDEX.INDEX_NVEHPOS.ordinal()][index] = pStats.getData(PStatsOverviewDataContainer.FIELDS.nVehicleOfInBusiness.ordinal());
				this.history[INDEX.INDEX_NBUDGET.ordinal()][index] = pStats.getData(PStatsOverviewDataContainer.FIELDS.avgBudgetPerOperator.ordinal());
				this.history[INDEX.INDEX_NBUDGETPOS.ordinal()][index] = pStats.getData(PStatsOverviewDataContainer.FIELDS.avgBudgetPerInBusinessOperator.ordinal());
				this.history[INDEX.INDEX_NSCORE.ordinal()][index] = pStats.getData(PStatsOverviewDataContainer.FIELDS.avgCashflowPerRoute.ordinal());
				this.history[INDEX.INDEX_NSCOREPOS.ordinal()][index] = pStats.getData(PStatsOverviewDataContainer.FIELDS.avgCashflowPerRouteOfInBusiness.ordinal());
				
				this.history[INDEX.INDEX_SHAREPOSOPERATORS.ordinal()][index] = pStats.getData(PStatsOverviewDataContainer.FIELDS.shareOfInBusinessOperators.ordinal());
				this.history[INDEX.INDEX_SHAREPOSROUTES.ordinal()][index] = pStats.getData(PStatsOverviewDataContainer.FIELDS.shareOfInBusinessRoutes.ordinal());
				this.history[INDEX.INDEX_SHAREPOSPAX.ordinal()][index] = pStats.getData(PStatsOverviewDataContainer.FIELDS.shareOfPaxServedByInBusiness.ordinal());
				this.history[INDEX.INDEX_SHAREPOSVEH.ordinal()][index] = pStats.getData(PStatsOverviewDataContainer.FIELDS.shareOfVehOfInBusiness.ordinal());
				
				this.history[INDEX.INDEX_MEANPOSOPERATORS.ordinal()][index] = pStats.getData(PStatsOverviewDataContainer.FIELDS.estimatedMeanOperatorsInBusiness.ordinal());
				this.history[INDEX.INDEX_MEANPOSROUTES.ordinal()][index] = pStats.getData(PStatsOverviewDataContainer.FIELDS.estimatedMeanRouteOfInBusiness.ordinal());
				this.history[INDEX.INDEX_MEANPOSPAX.ordinal()][index] = pStats.getData(PStatsOverviewDataContainer.FIELDS.estimatedMeanPaxServedByInBusiness.ordinal());
				this.history[INDEX.INDEX_MEANPOSVEH.ordinal()][index] = pStats.getData(PStatsOverviewDataContainer.FIELDS.estimatedMeanVehicleOfInBusiness.ordinal());

				this.history[INDEX.INDEX_SIGMAUPPERPOSOPERATORS.ordinal()][index] = pStats.getData(PStatsOverviewDataContainer.FIELDS.estimatedMeanOperatorsInBusiness.ordinal()) + pStats.getData(PStatsOverviewDataContainer.FIELDS.estimatedSDOperatorsInBusiness.ordinal());
				this.history[INDEX.INDEX_SIGMAUPPERPOSROUTES.ordinal()][index] = pStats.getData(PStatsOverviewDataContainer.FIELDS.estimatedMeanRouteOfInBusiness.ordinal()) + pStats.getData(PStatsOverviewDataContainer.FIELDS.estimatedSDRouteOfInBusiness.ordinal());
				this.history[INDEX.INDEX_SIGMAUPPERPOSPAX.ordinal()][index] = pStats.getData(PStatsOverviewDataContainer.FIELDS.estimatedMeanPaxServedByInBusiness.ordinal()) + pStats.getData(PStatsOverviewDataContainer.FIELDS.estimatedSDPaxServedByInBusiness.ordinal());
				this.history[INDEX.INDEX_SIGMAUPPERPOSVEH.ordinal()][index] = pStats.getData(PStatsOverviewDataContainer.FIELDS.estimatedMeanVehicleOfInBusiness.ordinal()) + pStats.getData(PStatsOverviewDataContainer.FIELDS.estimatedSDVehicleOfInBusiness.ordinal());

				this.history[INDEX.INDEX_SIGMALOWERPOSOPERATORS.ordinal()][index] = pStats.getData(PStatsOverviewDataContainer.FIELDS.estimatedMeanOperatorsInBusiness.ordinal()) - pStats.getData(PStatsOverviewDataContainer.FIELDS.estimatedSDOperatorsInBusiness.ordinal());
				this.history[INDEX.INDEX_SIGMALOWERPOSROUTES.ordinal()][index] = pStats.getData(PStatsOverviewDataContainer.FIELDS.estimatedMeanRouteOfInBusiness.ordinal()) - pStats.getData(PStatsOverviewDataContainer.FIELDS.estimatedSDRouteOfInBusiness.ordinal());
				this.history[INDEX.INDEX_SIGMALOWERPOSPAX.ordinal()][index] = pStats.getData(PStatsOverviewDataContainer.FIELDS.estimatedMeanPaxServedByInBusiness.ordinal()) - pStats.getData(PStatsOverviewDataContainer.FIELDS.estimatedSDPaxServedByInBusiness.ordinal());
				this.history[INDEX.INDEX_SIGMALOWERPOSVEH.ordinal()][index] = pStats.getData(PStatsOverviewDataContainer.FIELDS.estimatedMeanVehicleOfInBusiness.ordinal()) - pStats.getData(PStatsOverviewDataContainer.FIELDS.estimatedSDVehicleOfInBusiness.ordinal());

				if ((event.getIteration() % this.pConfig.getWriteStatsInterval() == 0) ) {
					if (event.getIteration() != this.minIteration) {
						// create chart when data of more than one iteration is available.

						XYLineChart size = new XYLineChart("Paratransit Statistics", "iteration", "operators/routes/fleet size");
						XYLineChart scores = new XYLineChart("Paratransit Statistics", "iteration", "score/budget");
						XYLineChart passengers = new XYLineChart("Paratransit Statistics", "iteration", "pax");
						XYLineChart shares = new XYLineChart("Paratransit Statistics", "iteration", "shares of operators in business");
						XYLineChart relaxOperator = new XYLineChart("Paratransit Statistics", "iteration", "average and deviation of operators");
						XYLineChart relaxRoutes = new XYLineChart("Paratransit Statistics", "iteration", "average and deviation of routes");
						XYLineChart relaxPax = new XYLineChart("Paratransit Statistics", "iteration", "average and deviation of passengers");
						XYLineChart relaxVeh = new XYLineChart("Paratransit Statistics", "iteration", "average and deviation of vehicles");

						double[] iterations = new double[index + 1];
						for (int i = 0; i <= index; i++) {
							iterations[i] = i + this.minIteration;
						}

						addSeriesToPlot(index, size, iterations, INDEX.INDEX_NOPERATORS.ordinal());
						addSeriesToPlot(index, size, iterations, INDEX.INDEX_NOPERATORSPOS.ordinal());
						addSeriesToPlot(index, size, iterations, INDEX.INDEX_NROUTES.ordinal());
						addSeriesToPlot(index, size, iterations, INDEX.INDEX_NROUTESPOS.ordinal());
						addSeriesToPlot(index, size, iterations, INDEX.INDEX_NVEH.ordinal());
						addSeriesToPlot(index, size, iterations, INDEX.INDEX_NVEHPOS.ordinal());
						
						addSeriesToPlot(index, scores, iterations, INDEX.INDEX_NBUDGET.ordinal());
						addSeriesToPlot(index, scores, iterations, INDEX.INDEX_NBUDGETPOS.ordinal());
						addSeriesToPlot(index, scores, iterations, INDEX.INDEX_NSCORE.ordinal());
						addSeriesToPlot(index, scores, iterations, INDEX.INDEX_NSCOREPOS.ordinal());

						addSeriesToPlot(index, passengers, iterations, INDEX.INDEX_NPAX.ordinal());
						addSeriesToPlot(index, passengers, iterations, INDEX.INDEX_NPAXPOS.ordinal());
						
						addSeriesToPlot(index, shares, iterations, INDEX.INDEX_SHAREPOSOPERATORS.ordinal());
						addSeriesToPlot(index, shares, iterations, INDEX.INDEX_SHAREPOSROUTES.ordinal());
						addSeriesToPlot(index, shares, iterations, INDEX.INDEX_SHAREPOSPAX.ordinal());
						addSeriesToPlot(index, shares, iterations, INDEX.INDEX_SHAREPOSVEH.ordinal());
						
						addSeriesToPlot(index, relaxOperator, iterations, INDEX.INDEX_MEANPOSOPERATORS.ordinal());
						addSeriesToPlot(index, relaxOperator, iterations, INDEX.INDEX_SIGMAUPPERPOSOPERATORS.ordinal());
						addSeriesToPlot(index, relaxOperator, iterations, INDEX.INDEX_SIGMALOWERPOSOPERATORS.ordinal());

						addSeriesToPlot(index, relaxRoutes, iterations, INDEX.INDEX_MEANPOSROUTES.ordinal());
						addSeriesToPlot(index, relaxRoutes, iterations, INDEX.INDEX_SIGMAUPPERPOSROUTES.ordinal());
						addSeriesToPlot(index, relaxRoutes, iterations, INDEX.INDEX_SIGMALOWERPOSROUTES.ordinal());
						
						addSeriesToPlot(index, relaxPax, iterations, INDEX.INDEX_MEANPOSPAX.ordinal());
						addSeriesToPlot(index, relaxPax, iterations, INDEX.INDEX_SIGMAUPPERPOSPAX.ordinal());
						addSeriesToPlot(index, relaxPax, iterations, INDEX.INDEX_SIGMALOWERPOSPAX.ordinal());

						addSeriesToPlot(index, relaxVeh, iterations, INDEX.INDEX_MEANPOSVEH.ordinal());
						addSeriesToPlot(index, relaxVeh, iterations, INDEX.INDEX_SIGMAUPPERPOSVEH.ordinal());
						addSeriesToPlot(index, relaxVeh, iterations, INDEX.INDEX_SIGMALOWERPOSVEH.ordinal());

						size.addMatsimLogo();
						scores.addMatsimLogo();
						passengers.addMatsimLogo();
						shares.addMatsimLogo();
						relaxOperator.addMatsimLogo();
						relaxRoutes.addMatsimLogo();
						relaxPax.addMatsimLogo();
						relaxVeh.addMatsimLogo();

						size.saveAsPng(event.getServices().getControlerIO().getOutputFilename("pStats_size.png"), 800, 600);
						scores.saveAsPng(event.getServices().getControlerIO().getOutputFilename("pStats_score.png"), 800, 600);
						passengers.saveAsPng(event.getServices().getControlerIO().getOutputFilename("pStats_pax.png"), 800, 600);
						shares.saveAsPng(event.getServices().getControlerIO().getOutputFilename("pStats_shares.png"), 800, 600);
						relaxOperator.saveAsPng(event.getServices().getControlerIO().getOutputFilename("pStats_relaxOperators.png"), 800, 600);
						relaxRoutes.saveAsPng(event.getServices().getControlerIO().getOutputFilename("pStats_relaxRoutes.png"), 800, 600);
						relaxPax.saveAsPng(event.getServices().getControlerIO().getOutputFilename("pStats_relaxPax.png"), 800, 600);
						relaxVeh.saveAsPng(event.getServices().getControlerIO().getOutputFilename("pStats_relaxVeh.png"), 800, 600);
					}
				}
				if (index == (this.history[0].length - 1)) {
					// we cannot store more information, so disable the graph feature.
					this.history = null;
				}
			}
		}
	}

	private void addSeriesToPlot(int index, XYLineChart xyLineChart, double[] iterations, int series) {
		double[] values = new double[index + 1];
		System.arraycopy(this.history[series], 0, values, 0, index + 1);
		xyLineChart.addSeries(INDEX.values()[series].enName, iterations, values);
	}

	@Override
	public void notifyShutdown(final ShutdownEvent controlerShudownEvent) {
		if(this.pConfig.getWriteStatsInterval() > 0){
			try {
				if (this.pStatsWriter != null) {
					this.pStatsWriter.close();
				} else {
					log.warn("Tried to close the pStatsWriter. However, it hasn't been initialized yet. Maybe the run suffered from an unexpected shutdown request.");
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

	/**
	 * @return the history of scores in last iterations
	 */
	public double[][] getHistory() {
		if (this.history == null) {
			return null;
		}
		return this.history.clone();
	}
}