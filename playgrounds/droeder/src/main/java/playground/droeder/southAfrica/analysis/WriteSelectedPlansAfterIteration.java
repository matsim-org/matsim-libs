/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.droeder.southAfrica.analysis;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.vehicles.VehicleWriterV1;
import org.matsim.vehicles.Vehicles;

/**
 * @author droeder
 *
 */
public class WriteSelectedPlansAfterIteration implements IterationEndsListener{
	
	
	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		Population p = event.getControler().getPopulation();
		String pFile = event.getControler().getControlerIO().getIterationFilename(event.getIteration(), "selectedPlans.xml");
		
		Population newP = new PopulationImpl((ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig()));
		
		for(Person person : p.getPersons().values()){
			Person temp = newP.getFactory().createPerson(person.getId());
			temp.addPlan(person.getSelectedPlan());
			newP.addPerson(temp);
		}
		
		new PopulationWriter(newP, event.getControler().getNetwork()).write(pFile);
		
		Vehicles v = event.getControler().getScenario().getVehicles();
		String vFile = event.getControler().getControlerIO().getIterationFilename(event.getIteration(), "vehicles.xml.gz");
		new VehicleWriterV1(v).writeFile(vFile);
		
		TransitSchedule sched =event.getControler().getScenario().getTransitSchedule();
		String sFile = event.getControler().getControlerIO().getIterationFilename(event.getIteration(), "schedule.xml.gz");
		new TransitScheduleWriter(sched).writeFile(sFile);
	}

}
