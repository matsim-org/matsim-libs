/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.jbischoff.pt;

import java.util.Collection;

import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ReflectiveConfigGroup;

/**
 * @author  jbischoff
 *
 */
/**
 *
 */
public class VariableAccessConfigGroup extends ReflectiveConfigGroup {

	public static final String GROUPNAME = "variableAccess";
	
	
	public static final String MODEGROUPNAME = "variableAccessMode";

	/**
	 * @param name
	 */
	public VariableAccessConfigGroup() {
		super(GROUPNAME);
		// TODO Auto-generated constructor stub
	}
	
	  public Collection< ConfigGroup> getVariableAccessModeConfigGroups()
	    {
	        return (Collection<ConfigGroup>) getParameterSets(MODEGROUPNAME);
	    }
	  
	  public void setAccessModeGroup(ConfigGroup modeConfig)
	    {
	        addParameterSet(modeConfig);
	    }


}
