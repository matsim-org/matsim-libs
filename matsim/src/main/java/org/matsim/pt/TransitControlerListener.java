/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package org.matsim.pt;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.router.TransitRouterFactory;
import org.matsim.pt.transitSchedule.TransitScheduleReaderV1;
import org.matsim.vehicles.VehicleReaderV1;
import org.xml.sax.SAXException;

/**
 * @author mrieser
 */
public class TransitControlerListener implements StartupListener {

	@Override
	public void notifyStartup(final StartupEvent event) {
		if (event.getControler().getConfig().transit().getTransitScheduleFile() != null) {
			try {
				new TransitScheduleReaderV1(event.getControler().getScenario().getTransitSchedule(),
						event.getControler().getScenario().getNetwork()).readFile(
								event.getControler().getConfig().transit().getTransitScheduleFile());
			} catch (SAXException e) {
				throw new RuntimeException("could not read transit schedule.", e);
			} catch (ParserConfigurationException e) {
				throw new RuntimeException("could not read transit schedule.", e);
			} catch (IOException e) {
				throw new RuntimeException("could not read transit schedule.", e);
			}
		}
		if (event.getControler().getConfig().transit().getVehiclesFile() != null) {
			try {
				new VehicleReaderV1(event.getControler().getScenario().getVehicles()).parse(
						event.getControler().getConfig().transit().getVehiclesFile());
			} catch (SAXException e) {
				throw new RuntimeException("could not read vehicles.", e);
			} catch (ParserConfigurationException e) {
				throw new RuntimeException("could not read vehicles.", e);
			} catch (IOException e) {
				throw new RuntimeException("could not read vehicles.", e);
			}
		}
		ReconstructingUmlaufBuilder reconstructingUmlaufBuilder = new ReconstructingUmlaufBuilder(
				event.getControler().getScenario().getNetwork(),
				event.getControler().getScenario().getTransitSchedule().getTransitLines().values(),
				event.getControler().getScenario().getVehicles(),
				event.getControler().getScenario().getConfig().charyparNagelScoring());
		reconstructingUmlaufBuilder.build();
		event.getControler().getScenario().addScenarioElement( reconstructingUmlaufBuilder ) ;

		event.getControler().setTransitRouterFactory(new TransitRouterFactory(event.getControler().getScenario().getTransitSchedule(), new TransitRouterConfig()));

	}

}