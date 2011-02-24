/* *********************************************************************** *
 * project: org.matsim.*
 * CottbusLiveVis
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
package playground.dgrether.signalsystems.otfvis;

import playground.dgrether.DgOTFVis;
import playground.dgrether.DgPaths;


/**
 * @author dgrether
 * @see jbischoff playground for more "intelligent" live vis code including xy2links and routing
 *
 */
public class CottbusLiveVis {

	public static final String inputDir = DgPaths.STUDIESDG + "cottbus/";
//	private String signalSystems20 = inputDir + "signalSystemsCottbusByNodes_v2.0.xml";
//	private String signalGroups20 = inputDir + "signalGroupsCottbusByNodes_v2.0.xml";
//	private String signalControl20 = inputDir + "signalControlCottbusByNodes_v2.0.xml";
//	private String amberTimes10 = inputDir + "amberTimesCottbusByNodes_v1.0.xml";
//	
//	private String config = inputDir + "cottbusConfig.xml";
	private static final String oldCottbusConfig = inputDir + "originaldaten/config_dg_livevis.xml";
	
	public static final  String newCottbusConfig = inputDir + "cottbus_feb_fix/config.xml";

	private void run() {
		String config = newCottbusConfig;
		new DgOTFVis().playAndRouteConfig(config);
	}
	
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		new CottbusLiveVis().run();
	}

}
