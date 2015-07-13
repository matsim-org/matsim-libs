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

import org.matsim.core.config.ReflectiveConfigGroup;

/**
 * This config Module can be used to specify the paths to the
 * xml files configuring the signal systems.
 *
 * @author dgrether
 *
 */
public final class SignalSystemsConfigGroup extends ReflectiveConfigGroup {
	private static final String USE_SIGNALSYSTEMS = "useSignalsystems";

	public  static final String SIGNALSYSTEM_FILE = "signalsystems";
	public  static final String SIGNALCONTROL_FILE = "signalcontrol";
	public  static final String SIGNALGROUPS_FILE = "signalgroups";
	public  static final String USE_AMBER_TIMES = "useAmbertimes";
	public  static final String AMBERTIMES_FILE = "ambertimes";
	public  static final String INTERGREENTIMES_FILE = "intergreentimes";
	public  static final String USE_INTERGREEN_TIMES = "useIntergreentimes";
	public  static final String ACTION_ON_INTERGREEN_VIOLATION = "actionOnIntergreenViolation";
	public static final String WARN_ON_INTERGREEN_VIOLATION = "warn";
	public static final String EXCEPTION_ON_INTERGREEN_VIOLATION = "exception";
	
	public static final String GROUPNAME = "signalsystems";

	private String signalSystemFile;
	private String signalControlFile;

	private String signalGroupsFile;

	private String amberTimesFile;
	
	private String intergreenTimesFile;

	private boolean useIntergreens = false;
	
	private boolean useAmbertimes = false;
	
	private String actionOnIntergreenViolation = WARN_ON_INTERGREEN_VIOLATION;

	private boolean useSignalSystems = false;

	public SignalSystemsConfigGroup() {
		super(GROUPNAME);
	}

	@Override
	protected void checkConsistency() {
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
	public void setSignalGroupsFile(String filename){
		this.signalGroupsFile = filename;
	}

	@StringGetter( AMBERTIMES_FILE )
	public String getAmberTimesFile() {
		return this.amberTimesFile;
	}
	
	@StringSetter( AMBERTIMES_FILE )
	public void setAmberTimesFile(String filename){
		this.amberTimesFile = filename;
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
	public void setSignalControlFile(String filename){
		this.signalControlFile = filename;
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
	public String getActionOnIntergreenViolation() {
		return actionOnIntergreenViolation;
	}

	
	@StringSetter( ACTION_ON_INTERGREEN_VIOLATION )
	public void setActionOnIntergreenViolation(String actionOnIntergreenViolation) {
		// TODO conceptually, this is an enum... change that?
		if ( !WARN_ON_INTERGREEN_VIOLATION.equalsIgnoreCase( actionOnIntergreenViolation ) &&
			 !EXCEPTION_ON_INTERGREEN_VIOLATION.equalsIgnoreCase( actionOnIntergreenViolation ) ){
			throw new IllegalArgumentException("The value " + actionOnIntergreenViolation + " for key : " + ACTION_ON_INTERGREEN_VIOLATION + " is not supported by this config group");
		 }

		this.actionOnIntergreenViolation = actionOnIntergreenViolation;
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



}
