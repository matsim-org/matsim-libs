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
package playground.dgrether.signalsystems.cottbus.scripts;

import org.matsim.core.config.Config;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;

import playground.dgrether.DgPaths;
import playground.dgrether.koehlerstrehlersignal.DgKoehlerStrehler2010ModelWriter;
import playground.dgrether.koehlerstrehlersignal.DgMatsim2KoehlerStrehler2010NetworkConverter;
import playground.dgrether.koehlerstrehlersignal.data.DgNetwork;
import playground.dgrether.signalsystems.cottbus.DgCottbusScenarioPaths;


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
		
		Config c2 = ConfigUtils.createConfig();
		c2.scenario().setUseLanes(true);
		c2.scenario().setUseSignalSystems(true);
		c2.network().setInputFile(DgCottbusScenarioPaths.NETWORK_FILENAME);
		c2.network().setLaneDefinitionsFile(DgCottbusScenarioPaths.LANES_FILENAME);
		c2.signalSystems().setSignalSystemFile(DgCottbusScenarioPaths.SIGNALS_FILENAME);
		c2.signalSystems().setSignalGroupsFile(DgCottbusScenarioPaths.SIGNAL_GROUPS_FILENAME);
		c2.signalSystems().setSignalControlFile(DgCottbusScenarioPaths.SIGNAL_CONTROL_FIXEDTIME_FILENAME);
		ScenarioImpl sc = (ScenarioImpl) ScenarioUtils.loadScenario(c2);
		
		DgMatsim2KoehlerStrehler2010NetworkConverter netConverter = new DgMatsim2KoehlerStrehler2010NetworkConverter();
		DgNetwork dgNet = netConverter.convertNetworkLanesAndSignals(sc);
		new DgKoehlerStrehler2010ModelWriter().write(sc, dgNet, outputNetwork);

	}

}
