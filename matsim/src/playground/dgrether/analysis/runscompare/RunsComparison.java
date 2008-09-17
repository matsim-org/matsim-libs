/* *********************************************************************** *
 * project: org.matsim.*
 * KmlNetworkWriter.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
package playground.dgrether.analysis.runscompare;


/**
 * @author dgrether
 *
 */
public class RunsComparison {

	private static final String RUNPREFIX = "run";
	
	private String runBaseDirectory;
	
	private String run1Number;
	
	private String run2Number;
	
	private String iterationNumber;
	
  private String networkFile;
  
  private String countsFile;
  
  private String roadPricingFile;
  
  private String eventsFile1;
  
  private String eventsFile2;
  
  private String plansFile1;
  
  private String plansFile2;
  
  private String linkStatsFile1;
  
  private String linkStatsFile2;
	
  private String outputDirectory;
  
  
  
  public RunsComparison(ScenarioPathsSetter scenarioPaths) {
  	this.retrieveScenarioPaths(scenarioPaths);
  }
	
	/**
	 * @param scenarioPaths
	 */
	private void retrieveScenarioPaths(ScenarioPathsSetter scenarioPaths) {
		this.networkFile = scenarioPaths.getNetworkFile();
		this.countsFile = scenarioPaths.getCountsFile();
		this.roadPricingFile = scenarioPaths.getRoadPricingFile();
	}

	/**
	 * 
	 */
	public void compareRuns() {
		this.loadData();
	}
	
	
	
	
	
	private void loadData() {
		
	}

	public static void main(String[] args) {
		new RunsComparison(new IvtChScenarioPathsSetter()).compareRuns();
	}

}
