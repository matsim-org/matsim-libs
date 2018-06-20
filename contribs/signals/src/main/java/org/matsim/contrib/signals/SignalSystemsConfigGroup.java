/* *********************************************************************** *
 * project: org.matsim.*
 * SignalSystemConfigGroup
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
package org.matsim.contrib.signals;

import org.matsim.core.config.Config;
import org.matsim.core.config.ReflectiveConfigGroup;

/**
 * This config Module can be used to specify the paths to the
 * xml files configuring the signals.
 *
 * @author dgrether, tthunig
 */
public final class SignalSystemsConfigGroup extends ReflectiveConfigGroup {

	public static final String GROUPNAME = "signalsystems";
	public static final String USE_SIGNALSYSTEMS = "useSignalsystems";
	public static final String SIGNALSYSTEM_FILE = "signalsystems";
	public static final String SIGNALCONTROL_FILE = "signalcontrol";
	public static final String SIGNALGROUPS_FILE = "signalgroups";
	public static final String USE_AMBER_TIMES = "useAmbertimes";
	public static final String AMBERTIMES_FILE = "ambertimes";
	public static final String CONFLICTING_DIRECTIONS_FILE = "conflictingDirections";
	public static final String INTERSECTION_LOGIC = "intersectionLogic"; 
	public static final String INTERGREENTIMES_FILE = "intergreentimes";
	public static final String USE_INTERGREEN_TIMES = "useIntergreentimes";
	public static final String ACTION_ON_INTERGREEN_VIOLATION = "actionOnIntergreenViolation";
	public static final String ACTION_ON_CONFLICTING_DIRECTION_VIOLATION = "actionOnConflictingDirectionViolation";
	public enum ActionOnSignalSpecsViolation{
		WARN, EXCEPTION
	}
	public enum IntersectionLogic{
		/* vehicles drive through each other at intersections */
		NONE,
		/* vehicles still drive through each other, but data about conflicting directions is stored 
		 * e.g. for possible signal phase combinations */
		CONFLICTING_DIRECTIONS_NO_TURN_RESTRICTIONS,
		/* data about conflicting directions is used to forbid turns when oncoming traffic 
		 * (from directions with right of way) is approaching */
		CONFLICTING_DIRECTIONS_AND_TURN_RESTRICTIONS
	}

	private String signalSystemFile;
	private String signalControlFile;
	private String signalGroupsFile;
	private String amberTimesFile;
	private String intergreenTimesFile;
	private String conflictingDirectionsFile;
	private boolean useIntergreens = false;
	private boolean useAmbertimes = false;
	private boolean useSignalSystems = false;
	private IntersectionLogic intersectionLogic = IntersectionLogic.NONE;
	private ActionOnSignalSpecsViolation actionOnIntergreenViolation = ActionOnSignalSpecsViolation.WARN;
	private ActionOnSignalSpecsViolation actionOnConflictingDirectionViolation = ActionOnSignalSpecsViolation.WARN;
	
	public SignalSystemsConfigGroup() {
		super(GROUPNAME);
	}

	@Override
	protected void checkConsistency(Config config) {
		if ((this.signalSystemFile == null) && (this.signalControlFile != null)) {
			throw new IllegalStateException("For using a SignalSystemConfiguration a definition of the signal systems must exist!");
		}
	}

	@StringGetter( SIGNALSYSTEM_FILE )
	public String getSignalSystemFile() {
		return this.signalSystemFile;
	}

	@StringSetter( SIGNALSYSTEM_FILE )
	public void setSignalSystemFile(final String signalSystemFile) {
		this.signalSystemFile = signalSystemFile;
	}


	@StringGetter( SIGNALGROUPS_FILE )
	public String getSignalGroupsFile() {
		return this.signalGroupsFile;
	}
	
	@StringSetter( SIGNALGROUPS_FILE )
	public void setSignalGroupsFile(String signalGroupsFile){
		this.signalGroupsFile = signalGroupsFile;
	}

	@StringGetter( AMBERTIMES_FILE )
	public String getAmberTimesFile() {
		return this.amberTimesFile;
	}
	
	@StringSetter( AMBERTIMES_FILE )
	public void setAmberTimesFile(String amberTimesFile){
		this.amberTimesFile = amberTimesFile;
	}
	
	@StringGetter( INTERGREENTIMES_FILE )
	public String getIntergreenTimesFile() {
		return intergreenTimesFile;
	}
	
	@StringSetter( INTERGREENTIMES_FILE )
	public void setIntergreenTimesFile(String intergreenTimesFile) {
		this.intergreenTimesFile = intergreenTimesFile;
	}

	@StringGetter( SIGNALCONTROL_FILE )
	public String getSignalControlFile() {
		return this.signalControlFile;
	}
	
	@StringSetter( SIGNALCONTROL_FILE )
	public void setSignalControlFile(String signalControlFile){
		this.signalControlFile = signalControlFile;
	}
	
	@StringGetter( CONFLICTING_DIRECTIONS_FILE )
	public String getConflictingDirectionsFile() {
		return this.conflictingDirectionsFile;
	}
	
	@StringSetter( CONFLICTING_DIRECTIONS_FILE )
	public void setConflictingDirectionsFile(String conflictingDirectionsFile){
		this.conflictingDirectionsFile = conflictingDirectionsFile;
	}

	@StringGetter( USE_INTERGREEN_TIMES )
	public boolean isUseIntergreenTimes() {
		return this.useIntergreens;
	}
	
	@StringSetter( USE_INTERGREEN_TIMES )
	public void setUseIntergreenTimes(boolean useIntergreens){
		this.useIntergreens = useIntergreens;
	}
	
	@StringGetter( ACTION_ON_INTERGREEN_VIOLATION )
	public ActionOnSignalSpecsViolation getActionOnIntergreenViolation() {
		return actionOnIntergreenViolation;
	}

	@StringSetter( ACTION_ON_INTERGREEN_VIOLATION )
	public void setActionOnIntergreenViolation(ActionOnSignalSpecsViolation actionOnIntergreenViolation) {
		switch (actionOnIntergreenViolation){
		// set the value for the supported actions
		case WARN:
		case EXCEPTION:
			this.actionOnIntergreenViolation = actionOnIntergreenViolation;
			break;
		// throw an exception if the value is not supported
		default:
			throw new IllegalArgumentException("The value " + actionOnIntergreenViolation 
					+ " for key : " + ACTION_ON_INTERGREEN_VIOLATION + " is not supported by this config group");
		}
	}
	
	@StringGetter( ACTION_ON_CONFLICTING_DIRECTION_VIOLATION )
	public ActionOnSignalSpecsViolation getActionOnConflictingDirectionViolation() {
		return actionOnConflictingDirectionViolation;
	}

	@StringSetter( ACTION_ON_CONFLICTING_DIRECTION_VIOLATION )
	public void setActionOnConflictingDirectionViolation(ActionOnSignalSpecsViolation actionOnConflictingDirectionViolation) {
		switch (actionOnConflictingDirectionViolation){
		// set the value for the supported actions
		case WARN:
		case EXCEPTION:
			this.actionOnConflictingDirectionViolation = actionOnConflictingDirectionViolation;
			break;
		// throw an exception if the value is not supported
		default:
			throw new IllegalArgumentException("The value " + actionOnConflictingDirectionViolation 
					+ " for key : " + ACTION_ON_CONFLICTING_DIRECTION_VIOLATION + " is not supported by this config group");
		}
	}
	
	@StringGetter( USE_AMBER_TIMES )
	public boolean isUseAmbertimes() {
		return useAmbertimes;
	}
	
	@StringSetter( USE_AMBER_TIMES )
	public void setUseAmbertimes(boolean useAmbertimes) {
		this.useAmbertimes = useAmbertimes;
	}

	@StringGetter( USE_SIGNALSYSTEMS )
	public boolean isUseSignalSystems() {
		return this.useSignalSystems;
	}

	@StringSetter( USE_SIGNALSYSTEMS )
	public void setUseSignalSystems(final boolean useSignalSystems) {
		this.useSignalSystems = useSignalSystems;
	}
	
	public IntersectionLogic getIntersectionLogic() {
		return intersectionLogic;
	}

	public void setIntersectionLogic(IntersectionLogic intersectionLogic) {
		this.intersectionLogic = intersectionLogic;
	}
}

