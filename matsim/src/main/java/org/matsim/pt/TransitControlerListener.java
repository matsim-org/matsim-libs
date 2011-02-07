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

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.router.TransitRouterImplFactory;

/**
 * @author mrieser
 */
public class TransitControlerListener implements StartupListener {

	@Override
	public void notifyStartup(final StartupEvent event) {
		final Scenario scenario = event.getControler().getScenario();
//		if (event.getControler().getConfig().transit().getTransitScheduleFile() != null) {
//			try {
//				new TransitScheduleReaderV1(event.getControler().getScenario().getTransitSchedule(),
//						event.getControler().getScenario().getNetwork()).readFile(
//								event.getControler().getConfig().transit().getTransitScheduleFile());
//			} catch (SAXException e) {
//				throw new RuntimeException("could not read transit schedule.", e);
//			} catch (ParserConfigurationException e) {
//				throw new RuntimeException("could not read transit schedule.", e);
//			} catch (IOException e) {
//				throw new RuntimeException("could not read transit schedule.", e);
//			}
//		}
//		if (event.getControler().getConfig().transit().getVehiclesFile() != null) {
//			try {
//				new VehicleReaderV1(event.getControler().getScenario().getVehicles()).parse(
//						event.getControler().getConfig().transit().getVehiclesFile());
//			} catch (SAXException e) {
//				throw new RuntimeException("could not read vehicles.", e);
//			} catch (ParserConfigurationException e) {
//				throw new RuntimeException("could not read vehicles.", e);
//			} catch (IOException e) {
//				throw new RuntimeException("could not read vehicles.", e);
//			}
//		}
		ReconstructingUmlaufBuilder reconstructingUmlaufBuilder = new ReconstructingUmlaufBuilder(
				scenario.getNetwork(),
				((ScenarioImpl)scenario).getTransitSchedule().getTransitLines().values(),
				((ScenarioImpl)scenario).getVehicles(),
				scenario.getConfig().planCalcScore());
		reconstructingUmlaufBuilder.build();
		scenario.addScenarioElement( reconstructingUmlaufBuilder ) ;

		if (event.getControler().getTransitRouterFactory() == null) {
			
			TransitRouterConfig transitRouterConfig = new TransitRouterConfig( scenario.getConfig().planCalcScore()
					, scenario.getConfig().plansCalcRoute() ) ;
			
			event.getControler().setTransitRouterFactory(new TransitRouterImplFactory(
					((ScenarioImpl)scenario).getTransitSchedule(), transitRouterConfig ));
		}

	}

}