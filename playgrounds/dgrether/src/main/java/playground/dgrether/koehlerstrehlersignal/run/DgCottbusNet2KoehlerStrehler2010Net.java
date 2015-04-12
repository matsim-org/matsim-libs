/* *********************************************************************** *
 * project: org.matsim.*
 * DgCottbusNet2KoehlerStrehler2010Net
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
package playground.dgrether.koehlerstrehlersignal.run;

import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.lanes.data.v20.LaneDefinitions20;
import org.matsim.signals.data.SignalsData;

import playground.dgrether.DgPaths;
import playground.dgrether.koehlerstrehlersignal.conversion.M2KS2010NetworkConverter;
import playground.dgrether.koehlerstrehlersignal.data.DgKSNetwork;
import playground.dgrether.koehlerstrehlersignal.data.KS2010ModelWriter;
import playground.dgrether.koehlerstrehlersignal.ids.DgIdConverter;
import playground.dgrether.koehlerstrehlersignal.ids.DgIdPool;
import playground.dgrether.signalsystems.cottbus.CottbusUtils;


/**
 * @author dgrether
 *
 */
public class DgCottbusNet2KoehlerStrehler2010Net {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String outputNetwork = DgPaths.REPOS + "shared-svn/studies/dgrether/cottbus/cottbus_feb_fix/network_koehler_strehler_format.xml";
		
		ScenarioImpl sc = CottbusUtils.loadCottbusScenrio(true);
		
		DgIdPool idPool = new DgIdPool();
		DgIdConverter idConverter = new DgIdConverter(idPool);
		
		M2KS2010NetworkConverter netConverter = new M2KS2010NetworkConverter(idConverter);
		DgKSNetwork dgNet = netConverter.convertNetworkLanesAndSignals(sc.getNetwork(), (LaneDefinitions20) sc.getScenarioElement(LaneDefinitions20.ELEMENT_NAME), (SignalsData) sc.getScenarioElement(SignalsData.ELEMENT_NAME), 0.0, 3600.0);
		new KS2010ModelWriter().write(dgNet, outputNetwork);

	}

}
