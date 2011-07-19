/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.mzilske.teach;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.replanning.PlanStrategyModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.selectors.KeepSelected;
import org.matsim.core.replanning.selectors.PlanSelector;


public class ChangeActivityTimesStrategy implements PlanStrategy {
	
	PlanStrategy planStrategyDelegate;

	public ChangeActivityTimesStrategy(Controler controler) {
		
		// Eine PlanStrategy wird auf eine Person angewendet. Sie kann einen Plan auswählen,
		// und optional eine Kopie dieses Plans modifizieren.
		
		// So wird der aktuell gewählte Plan beibehalten.
		planStrategyDelegate = new PlanStrategyImpl(new KeepSelected());
		
		
		// Hier wird das (selbst geschriebene) Modul zum Umplanen der Aktivitäten hinzugefügt.
		ChangeActivityTimes mod = new ChangeActivityTimes();
		addStrategyModule(mod) ;

		// Wenn das Modul mitlesen soll, was auf dem Netz geschieht, muss es als EventHandler
		// hinzugefügt werden.
		controler.getEvents().addHandler( mod ) ;
		
	}

	@Override
	public void addStrategyModule(PlanStrategyModule module) {
		planStrategyDelegate.addStrategyModule(module);
	}

	@Override
	public void finish() {
		planStrategyDelegate.finish();
	}

	@Override
	public int getNumberOfStrategyModules() {
		return planStrategyDelegate.getNumberOfStrategyModules();
	}

	@Override
	public PlanSelector getPlanSelector() {
		return planStrategyDelegate.getPlanSelector();
	}

	@Override
	public void init() {
		planStrategyDelegate.init();
	}

	@Override
	public void run(Person person) {
		planStrategyDelegate.run(person);
	}

	@Override
	public String toString() {
		return planStrategyDelegate.toString();
	}

}
