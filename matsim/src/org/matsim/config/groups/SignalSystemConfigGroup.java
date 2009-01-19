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
package org.matsim.config.groups;

import org.matsim.config.Module;


/**
 * This config Module can be used to specify the paths to the 
 * xml files configuring the signal systems.
 * 
 * @author dgrether
 *
 */
public class SignalSystemConfigGroup extends Module {
	/**
	 * 
	 */
	private static final long serialVersionUID = 2346649035049406334L;

	private static final String SIGNALSYSTEMFILE = "signalsystems";
	private static final String SIGNALSYSTEMCONFIGFILE = "signalsystemsconfiguration";

	public static final String GROUPNAME = "signalsystems";
	
	private String signalSystemFile;
	private String signalSystemConfigFile;
	
	public SignalSystemConfigGroup() {
		super(GROUPNAME);
	}

	@Override
	public String getValue(final String key) {
		throw new UnsupportedOperationException("This method is only implemented if compatibility with old code is needed, which is not the case for withinday replanning");
	}

	@Override
	public void addParam(final String key, final String value){
		if (SIGNALSYSTEMFILE.equalsIgnoreCase(key)){
			this.signalSystemFile = value.replace("\\", "/").trim();
		}
		else if (SIGNALSYSTEMCONFIGFILE.equalsIgnoreCase(key)) {
			this.signalSystemConfigFile = value.replace("\\", "/").trim();
		}
		else {
			throw new IllegalArgumentException("The key : " + key + " is not supported by this config group");
		}
	}

	@Override
	protected void checkConsistency() {
		if ((this.signalSystemFile == null) && (this.signalSystemConfigFile != null)) {
			throw new IllegalStateException("For using a SignalSystemConfiguration a definition of the signal systems must exist!");
		}
	}

	public String getSignalSystemFile() {
		return signalSystemFile;
	}


	
	public void setSignalSystemFile(String signalSystemFile) {
		this.signalSystemFile = signalSystemFile;
	}


	
	public String getSignalSystemConfigFile() {
		return signalSystemConfigFile;
	}


	
	public void setSignalSystemConfigFile(String signalSystemConfigFile) {
		this.signalSystemConfigFile = signalSystemConfigFile;
	}
	
	
}
