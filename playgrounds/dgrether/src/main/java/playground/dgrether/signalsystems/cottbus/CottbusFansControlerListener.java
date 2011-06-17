/* *********************************************************************** *
 * project: org.matsim.*
 * CottbusFansControlerListener
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.dgrether.signalsystems.cottbus;

import java.util.Random;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.population.algorithms.PlanAlgorithm;


/**
 * @author dgrether
 *
 */
public class CottbusFansControlerListener implements BeforeMobsimListener{

	private static final Logger log = Logger.getLogger(CottbusFansControlerListener.class);
	
	private Population pop;
	private Random random;
	
	
	public CottbusFansControlerListener(Population fanPop) {
		this.pop = fanPop;
		random = MatsimRandom.getLocalInstance();
	}


	@Override
	public void notifyBeforeMobsim(BeforeMobsimEvent e) {
		Controler c = e.getControler();
		PlanAlgorithm pcr = c.createRoutingAlgorithm();
		for (Person p : pop.getPersons().values()){
			if (random.nextDouble() < 0.1){
//				log.debug(((LinkNetworkRoute)((Leg)p.getSelectedPlan().getPlanElements().get(1)).getRoute()).getLinkIds());
				pcr.run(p.getSelectedPlan());
//				log.debug(((LinkNetworkRoute)((Leg)p.getSelectedPlan().getPlanElements().get(1)).getRoute()).getLinkIds());
			}
		}
	}
	
	
	

}
