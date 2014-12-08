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
package org.matsim.core.config.groups;

import java.util.TreeMap;

import org.matsim.core.config.ConfigGroup;

/**
 * This config Module can be used to specify the paths to the
 * xml files configuring the signal systems.
 *
 * @author dgrether
 *
 */
public class SignalSystemsConfigGroup extends ConfigGroup {

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

	public SignalSystemsConfigGroup() {
		super(GROUPNAME);
	}

	@Override
	public String getValue(final String key) {
		throw new UnsupportedOperationException("This method is only implemented if compatibility with old code is needed, which is not the case for signals");
	}

	@Override
	public void addParam(final String key, final String value){
		if (SIGNALSYSTEM_FILE.equalsIgnoreCase(key)){
			this.signalSystemFile = value.trim();
		}
		else if (SIGNALCONTROL_FILE.equalsIgnoreCase(key)) {
			this.signalControlFile = value.trim();
		}
		else if (SIGNALGROUPS_FILE.equalsIgnoreCase(key)){
			this.signalGroupsFile = value.trim();
		}
		else if (AMBERTIMES_FILE.equalsIgnoreCase(key)){
			this.amberTimesFile = value.trim();
		}
		else if (INTERGREENTIMES_FILE.equalsIgnoreCase(key)){
			this.intergreenTimesFile = value.trim();
		}
		else if (USE_INTERGREEN_TIMES.equalsIgnoreCase(key)){
			this.setUseIntergreenTimes(Boolean.parseBoolean(value.trim()));
		}
		else if (USE_AMBER_TIMES.equalsIgnoreCase(key)){
			this.setUseAmbertimes(Boolean.parseBoolean(value.trim()));
		}
		else if (ACTION_ON_INTERGREEN_VIOLATION.equalsIgnoreCase(key)){
			if (WARN_ON_INTERGREEN_VIOLATION.equalsIgnoreCase(value.trim())){
				this.setActionOnIntergreenViolation(WARN_ON_INTERGREEN_VIOLATION);
			}
			else if (EXCEPTION_ON_INTERGREEN_VIOLATION.equalsIgnoreCase(value.trim())){
				this.setActionOnIntergreenViolation(EXCEPTION_ON_INTERGREEN_VIOLATION);
			}
			else {
				throw new IllegalArgumentException("The value " + value.trim() + " for key : " + key + " is not supported by this config group");
			}
		}
		else {
			throw new IllegalArgumentException("The key : " + key + " is not supported by this config group");
		}
	}

	@Override
	public final TreeMap<String, String> getParams() {
		TreeMap<String, String> map = new TreeMap<String, String>();
		map.put(SIGNALSYSTEM_FILE, this.getSignalSystemFile());
		map.put(SIGNALCONTROL_FILE, this.getSignalControlFile());
		map.put(SIGNALGROUPS_FILE, this.getSignalGroupsFile());
		map.put(USE_AMBER_TIMES, Boolean.toString(this.isUseAmbertimes()));
		map.put(AMBERTIMES_FILE, this.getAmberTimesFile());
		map.put(USE_INTERGREEN_TIMES, Boolean.toString(this.isUseIntergreenTimes()));
		map.put(INTERGREENTIMES_FILE, this.getIntergreenTimesFile());
		return map;
	}

	@Override
	protected void checkConsistency() {
		if ((this.signalSystemFile == null) && (this.signalControlFile != null)) {
			throw new IllegalStateException("For using a SignalSystemConfiguration a definition of the signal systems must exist!");
		}
	}

	public String getSignalSystemFile() {
		return this.signalSystemFile;
	}



	public void setSignalSystemFile(final String signalSystemFile) {
		this.signalSystemFile = signalSystemFile;
	}


	public String getSignalGroupsFile() {
		return this.signalGroupsFile;
	}
	
	public void setSignalGroupsFile(String filename){
		this.signalGroupsFile = filename;
	}

	public String getAmberTimesFile() {
		return this.amberTimesFile;
	}
	
	public void setAmberTimesFile(String filename){
		this.amberTimesFile = filename;
	}
	
	public String getIntergreenTimesFile() {
		return intergreenTimesFile;
	}
	
	public void setIntergreenTimesFile(String intergreenTimesFile) {
		this.intergreenTimesFile = intergreenTimesFile;
	}

	public String getSignalControlFile() {
		return this.signalControlFile;
	}
	
	public void setSignalControlFile(String filename){
		this.signalControlFile = filename;
	}

	public boolean isUseIntergreenTimes() {
		return this.useIntergreens;
	}
	
	public void setUseIntergreenTimes(boolean useIntergreens){
		this.useIntergreens = useIntergreens;
	}

	
	public String getActionOnIntergreenViolation() {
		return actionOnIntergreenViolation;
	}

	
	public void setActionOnIntergreenViolation(String actionOnIntergreenViolation) {
		this.actionOnIntergreenViolation = actionOnIntergreenViolation;
	}

	
	public boolean isUseAmbertimes() {
		return useAmbertimes;
	}

	
	public void setUseAmbertimes(boolean useAmbertimes) {
		this.useAmbertimes = useAmbertimes;
	}

}
