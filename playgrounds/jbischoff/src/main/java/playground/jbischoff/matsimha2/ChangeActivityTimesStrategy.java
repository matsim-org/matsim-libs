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

package playground.jbischoff.matsimha2;

import org.matsim.api.core.v01.population.HasPlansAndId;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.replanning.PlanStrategyModule;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.replanning.selectors.KeepSelected;


public class ChangeActivityTimesStrategy implements PlanStrategy {
	
	PlanStrategyImpl planStrategyDelegate;

	public ChangeActivityTimesStrategy(MatsimServices controler) {
		
		// Eine PlanStrategy wird auf eine Person angewendet. Sie kann einen Plan auswählen,
		// und optional eine Kopie dieses Plans modifizieren.
		
		// So wird der aktuell gewählte Plan beibehalten.
		planStrategyDelegate = new PlanStrategyImpl(new KeepSelected());
		
		
		// Hier wird das (selbst geschriebene) Modul zum Umplanen der Aktivitäten hinzugefügt.
//		ChangeActivityTimesAdvanced mod = new ChangeActivityTimesAdvanced();
		ChangeActivityTimesEasy mod = new ChangeActivityTimesEasy();
		addStrategyModule(mod) ;

		// Wenn das Modul mitlesen soll, was auf dem Netz geschieht, muss es als EventHandler
		// hinzugefügt werden.
		controler.getEvents().addHandler( mod ) ;
		
		
	}

	public void addStrategyModule(PlanStrategyModule module) {
		planStrategyDelegate.addStrategyModule(module);
	}

	@Override
	public void finish() {
		planStrategyDelegate.finish();
	}

	public int getNumberOfStrategyModules() {
		return planStrategyDelegate.getNumberOfStrategyModules();
	}


	@Override
	public void init(ReplanningContext replanningContext) {
		planStrategyDelegate.init(replanningContext);
	}

	@Override
	public void run(HasPlansAndId<Plan, Person> person) {
		planStrategyDelegate.run(person);
	}

	@Override
	public String toString() {
		return planStrategyDelegate.toString();
	}

}
