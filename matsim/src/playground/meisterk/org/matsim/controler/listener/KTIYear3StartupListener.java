/* *********************************************************************** *
 * project: org.matsim.*
 * KTIYear3StartupListener.java
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

package playground.meisterk.org.matsim.controler.listener;

import org.matsim.controler.Controler;
import org.matsim.controler.events.StartupEvent;
import org.matsim.controler.listener.StartupListener;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.population.algorithms.ParallelPersonAlgorithmRunner;

import playground.meisterk.org.matsim.population.algorithms.PersonAddTypicalDurationsToDesires;

public class KTIYear3StartupListener implements StartupListener {

	private Controler controler;
	
	public KTIYear3StartupListener(Controler controler) {
		super();
		this.controler = controler;
	}

	public void notifyStartup(StartupEvent event) {
		ParallelPersonAlgorithmRunner.run(this.controler.getPopulation(), this.controler.getConfig().global().getNumberOfThreads(), new ParallelPersonAlgorithmRunner.PersonAlgorithmProvider() {
			public AbstractPersonAlgorithm getPersonAlgorithm() {
				return new PersonAddTypicalDurationsToDesires();
			}
		});
	}

}
