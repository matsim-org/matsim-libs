/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package playground.vsp.bvwp;

import org.matsim.core.config.ReflectiveConfigGroup;

/**
 * @author nagel
 *
 */
public class IVVReaderConfigGroup extends ReflectiveConfigGroup {
	public static final String GROUP_NAME = "IVVReaderConfigGroup" ;
	
	// ---
	// 00
	/*package*/static final String DEMAND_FILE = "demandMatrixFile" ;
	private String demandMatrixFile ;
	
	// 01
	/*package*/static final String REMAINING_DEMAND_FILE = "remainingDemandMatrixFile" ;
	private String demandRemainingMatrixFile ;
	
	// 03
	/*package*/static final String NEW_DEMAND_FILE = "newDemandMatrixFile" ;
	private String demandNewMatrixFile ;
	
	// 04
	/*package*/static final String DROPPED_DEMAND_FILE = "droppedDemandMatrixFile" ;
	private String demandDroppedMatrixFile ;

	// 06
	/*package*/static final String TRAVEL_TIMES_BASE_FILE = "travelTimesBaseCaseMatrixFile" ;
	private String travelTimesBaseMatrixFile ;
	
	// 07
	/*package*/static final String TRAVEL_TIMES_STUDY_FILE = "travelTimesStudyMatrixFile" ;
	private String travelTimesStudyMatrixFile ;
	
	// 06 - 15
	/*package*/static final String IMPEDANCE_FILE = "impedanceMatrixFile" ;
	private String impedanceMatrixFile ;
	
	// 16
	/*package*/static final String IMPEDANCE_SHIFTED_FILE = "impedanceShiftedMatrixFile" ;
	private String impedanceShiftedMatrixFile ;
	// ---
	
	/**
	 * @param name
	 */
	public IVVReaderConfigGroup() {
		super(GROUP_NAME);
	}

	/**
	 * @param name
	 * @param storeUnknownParametersAsStrings
	 */
	public IVVReaderConfigGroup( boolean storeUnknownParametersAsStrings) {
		super(GROUP_NAME, storeUnknownParametersAsStrings);
	}
	
	@StringGetter(TRAVEL_TIMES_BASE_FILE)
	public String getTravelTimesBaseMatrixFile() {
		return travelTimesBaseMatrixFile;
	}
	@StringSetter(TRAVEL_TIMES_BASE_FILE)
	public void setTravelTimesBaseMatrixFile(String travelTimesMatrixFile) {
		this.travelTimesBaseMatrixFile = travelTimesMatrixFile;
	}
	@StringGetter(TRAVEL_TIMES_STUDY_FILE)
	public String getTravelTimesStudyMatrixFile() {
		return travelTimesStudyMatrixFile;
	}
	@StringSetter(TRAVEL_TIMES_STUDY_FILE)
	public void setTravelTimesStudyMatrixFile(String travelTimesStudyMatrixFile) {
		this.travelTimesStudyMatrixFile = travelTimesStudyMatrixFile;
	}
	@StringGetter(IMPEDANCE_FILE)
	public String getImpedanceMatrixFile() {
		return impedanceMatrixFile;
	}
	@StringSetter(IMPEDANCE_FILE)
	public void setImpedanceMatrixFile(String impedanceMatrixFile) {
		this.impedanceMatrixFile = impedanceMatrixFile;
	}
	@StringGetter(IMPEDANCE_SHIFTED_FILE)
	public String getImpedanceShiftedMatrixFile() {
		return impedanceShiftedMatrixFile;
	}
	@StringSetter(IMPEDANCE_SHIFTED_FILE)
	public void setImpedanceShiftedMatrixFile(String impedanceShiftedMatrixFile) {
		this.impedanceShiftedMatrixFile = impedanceShiftedMatrixFile;
	}
	@StringGetter(DEMAND_FILE)
	public String getDemandMatrixFile() {
		return demandMatrixFile;
	}
	@StringSetter(DEMAND_FILE)
	public void setDemandMatrixFile(String demandMatrixFile) {
		this.demandMatrixFile = demandMatrixFile;
	}
	@StringGetter(REMAINING_DEMAND_FILE)
	public String getRemainingDemandMatrixFile() {
		return demandRemainingMatrixFile;
	}
	@StringSetter(REMAINING_DEMAND_FILE)
	public void setRemainingDemandMatrixFile(String demandRemainingMatrixFile) {
		this.demandRemainingMatrixFile = demandRemainingMatrixFile;
	}
	@StringGetter(NEW_DEMAND_FILE)
	public String getNewDemandMatrixFile() {
		return demandNewMatrixFile;
	}
	@StringSetter(NEW_DEMAND_FILE)
	public void setNewDemandMatrixFile(String newDemandMatrixFile) {
		this.demandNewMatrixFile = newDemandMatrixFile;
	}
	@StringGetter(DROPPED_DEMAND_FILE)
	public String getDroppedDemandMatrixFile() {
		return demandDroppedMatrixFile;
	}
	@StringSetter(DROPPED_DEMAND_FILE)
	public void setDroppedDemandMatrixFile(String droppedDemandMatrixFile) {
		this.demandDroppedMatrixFile = droppedDemandMatrixFile;
	}
}
