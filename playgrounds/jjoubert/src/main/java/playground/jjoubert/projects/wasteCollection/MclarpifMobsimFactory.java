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
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimUtils;

import com.google.inject.Provider;

/**
 *
 * @author jwjoubert
 */
public class MclarpifMobsimFactory implements Provider<Mobsim> {
	final private Logger log = Logger.getLogger(MclarpifMobsimFactory.class);
	final private Scenario sc;
	final private EventsManager em;
	
	public MclarpifMobsimFactory(Scenario sc, EventsManager em) {
		this.sc = sc;
		this.em = em;
	}

	@Override
	public Mobsim get() {
		final QSim sim = QSimUtils.createDefaultQSim(sc, em);
		
		/* Add the waste collection vehicle agent source. */
		MclarpifAgentSource wasteAgentSource = new MclarpifAgentSource(sim);
		sim.addAgentSource(wasteAgentSource);
		// TODO Auto-generated method stub
		
		log.warn(" ==> MCLARPIF mobsim created.");
		return sim;
	}

}
