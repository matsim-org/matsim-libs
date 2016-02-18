/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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
package playground.gregor.gis.worldmercatorvis;

import org.geotools.referencing.CRS;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.operation.MathTransform;
import playground.gregor.sim2d_v4.debugger.eventsbaseddebugger.Branding;
import playground.gregor.sim2d_v4.debugger.eventsbaseddebugger.EventBasedVisDebuggerEngine;
import playground.gregor.sim2d_v4.debugger.eventsbaseddebugger.InfoBox;
import playground.gregor.sim2d_v4.events.EventsReaderXMLv1ExtendedSim2DVersion;

public class WorldMercatorVis {
	public static void main(String [] args) throws NoSuchAuthorityCodeException, FactoryException {

		String confFile = "/Users/laemmel/devel/nyc/gct_vicinity/config.xml.gz";

		Config c = ConfigUtils.createConfig();
		
		
		
		ConfigUtils.loadConfig(c, confFile);
		String sourceCRS = c.global().getCoordinateSystem();
		c.global().setCoordinateSystem("EPSG:3395");
		MathTransform transform = CRS.findMathTransform(CRS.decode(sourceCRS), CRS.decode("EPSG:3395"));
		Scenario sc = ScenarioUtils.loadScenario(c);
		
		new NetworkConverter(sc, transform).run();
		
//		Sim2DConfig conf2d = Sim2DConfigUtils.createConfig();
//		Sim2DScenario sc2d = Sim2DScenarioUtils.createSim2dScenario(conf2d);
//
//		sc.addScenarioElement(Sim2DScenario.ELEMENT_NAME, sc2d);
		EventBasedVisDebuggerEngine dbg = new EventBasedVisDebuggerEngine(sc);
		InfoBox iBox = new InfoBox(dbg, sc);
		dbg.addAdditionalDrawer(iBox);
		dbg.addAdditionalDrawer(new Branding());
//		QSimDensityDrawer qDbg = new QSimDensityDrawer(sc);
//		dbg.addAdditionalDrawer(qDbg);
		
		EventsManagerImpl em = new EventsManagerImpl();
//		em.addHandler(qDbg);
		em.addHandler(dbg);
		
		EventsManagerImpl em1 = new EventsManagerImpl();
		
		
		CoordinateConverter conv = new  CoordinateConverter(em,transform);
		em1.addHandler(conv);
		new EventsReaderXMLv1ExtendedSim2DVersion(em1).parse("/Users/laemmel/devel/nyc/output/ITERS/it.100/100.events.xml.gz");
	}
}
