/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package playground.vsp.buildingEnergy;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.OutputDirectoryLogging;
import org.matsim.core.gbl.Gbl;

import playground.vsp.buildingEnergy.energyCalculation.BuildingEnergyAnalyzer;
import playground.vsp.buildingEnergy.energyCalculation.BuildingEnergyConsumptionRule;
import playground.vsp.buildingEnergy.energyCalculation.BuildingEnergyConsumptionRule.BuildingEnergyConsumptionRuleFactory;


/**
 * @author droeder
 * 
 */
class BuildingEnergyAnalyzerMain {

	private static final Logger log = Logger
			.getLogger(BuildingEnergyAnalyzerMain.class);
	
	private static final String[] ARGS = new String[]{
		"E:\\VSP\\svn\\shared-svn\\studies\\droeder\\buildingEnergy\\runs\\",
		"E:\\VSP\\svn\\shared-svn\\studies\\droeder\\buildingEnergy\\runs\\outputCaseStudies\\",
		"900",
		"86400",
		"2kW.15",
		"home",
		"work",
		"0.35625",
		"0.83125",
		"5.0",
		"0.366",
		"0.854",
		"true",
		"2kW.s1"
	};

	private BuildingEnergyAnalyzerMain() {
		// no instance of this class...
	}

	/**
	 * 
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		boolean time = Gbl.enableThreadCpuTimeMeasurement();
		Gbl.startMeasurement();
		if(args.length == 0){
			log.warn("using hard coded arguments. Make sure this what you want.");
			args = ARGS;
		}
		if(args.length < 13){
			throw new IllegalArgumentException("expecting min 12 arguments {inputpath, outputPath, timeSliceSize, tmax, " +
					"baseRunId, homeActivityType, workActivityType, P_bo, P_so, beta, P_bh, P_ah, isBerlinScenario,( runIds...)}");
		}
		String inputPath = new File(args[0]).getAbsolutePath() + System.getProperty("file.separator");
		String outputPath = new File(args[1]).getAbsolutePath() + System.getProperty("file.separator");
		int td = Integer.parseInt(args[2]);
		int tmax = Integer.parseInt(args[3]);
		String baseRun = args[4];
		String homeType = args[5];
		String workType = args[6];
		Double pbo = Double.parseDouble(args[7]);
		Double pso = Double.parseDouble(args[8]);
		Double beta = Double.parseDouble(args[9]);
		Double pbh = Double.parseDouble(args[10]);
		Double pah = Double.parseDouble(args[11]);
		Boolean isBerlin = Boolean.parseBoolean(args[12]);
		List<String> runs = new ArrayList<String>();
		for(int i = 13; i < args.length; i++){
			runs.add(args[i]);
		}
		//catch logEntries
		OutputDirectoryLogging.initLogging(
				new OutputDirectoryHierarchy(outputPath, BuildingEnergyAnalyzer.class.getSimpleName(),
						OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles,
						ControlerConfigGroup.CompressionType.none));
		OutputDirectoryLogging.catchLogEntries();
		// dump input-parameters to log
		log.info("running class: " + System.getProperty("sun.java.command"));
		log.info("inputPath: " + inputPath);
		log.info("outputPath: " + outputPath);
		log.info("timeSliceDuration [s]\t: " + String.valueOf(td));
		log.info("tMax [s]\t\t\t: " + tmax);
		log.info("homeType\t\t\t: " + homeType);
		log.info("workType\t\t\t: " + workType);
		log.info("P_bo [kW]\t\t\t: " + pbo);
		log.info("P_so [kW]\t\t\t: " + pso);
		log.info("beta []\t\t\t: " + beta);
		log.info("P_bh [kW]\t\t\t: " + pbh);
		log.info("P_ah [kW]\t\t\t: " + pah);
		log.info("baseRun\t\t\t: " + baseRun);
		log.info("isBerlin\t\t\t: " + isBerlin);
		for(int i = 0; i < runs.size(); i++){
			log.info("caseStudy " + (i + 1) + "\t\t: " + runs.get(i));
		}
		BuildingEnergyConsumptionRule ecWork = new OfficeEnergyConsumptionRuleImpl(td, pbo, pso, beta);
		BuildingEnergyConsumptionRule ecHome = new HomeEnergyConsumptionRuleImpl(td, pbh, pah);
		BuildingEnergyConsumptionRuleFactory factory =  new BuildingEnergyConsumptionRuleFactory();
		factory.setRule(homeType, ecHome);
		factory.setRule(workType, ecWork);
		// run
		BuildingEnergyAnalyzer analyzer = new BuildingEnergyAnalyzer(inputPath, outputPath, td, tmax, baseRun, runs, homeType, workType, factory);
		analyzer.setBerlin(isBerlin);
		analyzer.run();
		if(time){
			Gbl.printCurrentThreadCpuTime();
		}
		Gbl.printElapsedTime();
		log.info("finished.");
	}
	
	private static class OfficeEnergyConsumptionRuleImpl implements BuildingEnergyConsumptionRule{
		
		private double additional;
		private double baseLoad;
		private double td;
		private double someCoefficient;

		/**
		 * 
		 * @param td, duration timeslice [s]
		 * @param baseLoadPerPerson [kW]
		 * @param additionalLoadPerPerson [kW]
		 */
		OfficeEnergyConsumptionRuleImpl(double td, double baseLoadPerPerson, double additionalLoadPerPerson, double someCoefficient) {
			this.td = td;
			this.baseLoad = baseLoadPerPerson;
			this.additional = additionalLoadPerPerson;
			this.someCoefficient = someCoefficient;
		}
		
		@Override
		public double getEnergyConsumption_kWh(double maxSize, double currentOccupancy) {
			if(currentOccupancy > maxSize) throw new RuntimeException("more persons on the link than expected");
			if(maxSize == 0) return 0.;
			double baseload = this.baseLoad * maxSize;
			double additionalLoad = this.additional * maxSize;
			return (td / 3600. * (baseload + additionalLoad * (1 - Math.exp(-1.0 * currentOccupancy / maxSize * someCoefficient))));
		}
		
	}
	
	private static class HomeEnergyConsumptionRuleImpl implements BuildingEnergyConsumptionRule{
		
		private double td;
		private double baseLoad;
		private double additional;

		/**
		 * 
		 * @param td
		 * @param baseLoadPerPerson
		 * @param additionalLoadPerPerson
		 */
		HomeEnergyConsumptionRuleImpl(double td, double baseLoadPerPerson, double additionalLoadPerPerson) {
			this.td = td;
			this.baseLoad = baseLoadPerPerson;
			this. additional = additionalLoadPerPerson;
		}

		@Override
		public double getEnergyConsumption_kWh(double maxSize, double currentOccupancy) {
			return (td/3600. * (maxSize * baseLoad + currentOccupancy * additional));
		}
		
	}

}

