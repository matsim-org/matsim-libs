/* *********************************************************************** *
 * project: org.matsim.*
 * ConvertOsmToMatsim.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.jjoubert.projects.wasteCollection;

import org.apache.log4j.Logger;
import org.matsim.core.mobsim.framework.AgentSource;
import org.matsim.core.mobsim.framework.Mobsim;

/**
 * TODO Still trying to get waste collection vehicles into the mobsim.
 * 
 * @author jwjoubert
 */
public class MclarpifAgentSource implements AgentSource {
	final private Logger log = Logger.getLogger(MclarpifAgentSource.class);
	private Mobsim sim;
	
	public MclarpifAgentSource(Mobsim sim) {
		this.sim = sim;
		log.warn(" ==> MCLARPIF Agent source created.");
	}
	
	@Override
	public void insertAgentsIntoMobsim() {
		// TODO Auto-generated method stub
		log.warn(" ==> Waste collection vehicles injected");
	}

}
