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

import org.matsim.core.config.Module;


/**
 * This config Module can be used to specify the paths to the
 * xml files configuring the signal systems.
 *
 * @author dgrether
 *
 */
public class SignalSystemsConfigGroup extends Module {

	private static final long serialVersionUID = 2346649035049406334L;

	private static final String SIGNALSYSTEM_FILE = "signalsystems";
	private static final String SIGNALSYSTEMCONFIG_FILE = "signalsystemsconfiguration";
	private static final String SIGNALCONTROL_FILE = "signalcontrol";
	private static final String SIGNALGROUPS_FILE = "signalgroups";
	private static final String AMBERTIMES_FILE = "ambertimes";
	private static final String INTERGREENTIMES_FILE = "intergreentimes";

	public static final String GROUPNAME = "signalsystems";

	private String signalSystemFile;
	private String signalControlFile;

	private String signalGroupsFile;

	private String amberTimesFile;
	
	private String intergreenTimesFile;

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
			this.signalSystemFile = value.replace("\\", "/").trim();
		}
		else if (SIGNALSYSTEMCONFIG_FILE.equalsIgnoreCase(key) || SIGNALCONTROL_FILE.equalsIgnoreCase(key)) {
			this.signalControlFile = value.replace("\\", "/").trim();
		}
		else if (SIGNALGROUPS_FILE.equalsIgnoreCase(key)){
			this.signalGroupsFile = value.replace("\\", "/").trim();
		}
		else if (AMBERTIMES_FILE.equalsIgnoreCase(key)){
			this.amberTimesFile = value.replace("\\", "/").trim();
		}
		else if (INTERGREENTIMES_FILE.equalsIgnoreCase(key)){
			this.intergreenTimesFile = value.replace("\\", "/").trim();
		}
		else {
			throw new IllegalArgumentException("The key : " + key + " is not supported by this config group");
		}
	}

	@Override
	public final TreeMap<String, String> getParams() {
		TreeMap<String, String> map = new TreeMap<String, String>();
		map.put(SIGNALSYSTEM_FILE, this.getSignalSystemFile());
		map.put(SIGNALCONTROL_FILE, this.getSignalSystemConfigFile());
		map.put(SIGNALGROUPS_FILE, this.getSignalGroupsFile());
		map.put(AMBERTIMES_FILE, this.getAmberTimesFile());
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


	/**
	 * 
	 * @deprecated use getSignalControlFile instead
	 */
	@Deprecated
	public String getSignalSystemConfigFile() {
		return this.signalControlFile;
	}
	/**
	 * @deprecated use setSignalControlFile instead
	 */
	@Deprecated
	public void setSignalSystemConfigFile(final String signalSystemConfigFile) {
		this.signalControlFile = signalSystemConfigFile;
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

}
