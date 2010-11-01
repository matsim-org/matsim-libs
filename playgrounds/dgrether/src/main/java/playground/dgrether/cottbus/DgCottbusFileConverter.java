/* *********************************************************************** *
 * project: org.matsim.*
 * DgCottbusFileConverter
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
package playground.dgrether.cottbus;

import org.matsim.lanes.run.LaneDefinitonsV11ToV20Converter;

import playground.dgrether.DgPaths;
import playground.dgrether.signalsystems.data.SignalConfig11ToControl20Converter;
import playground.dgrether.signalsystems.data.SignalSystems11To20Converter;


/**
 * @author dgrether
 *
 */
public class DgCottbusFileConverter {

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		String inputDir = DgPaths.STUDIESDG + "cottbus/";
		
		String laneDefinitions = inputDir + "lanes_cottbus_v1.1.xml";
		
		String signalSystems = inputDir + "signalSystemsByNodes.xml";
		String signalSystemConfigurations = inputDir + "signalSystemConfig.xml";
		
		String signalSystems20 = inputDir + "signalSystemsCottbusByNodes_v2.0.xml";
		String signalGroups20 = inputDir + "signalGroupsCottbusByNodes_v2.0.xml";
		String signalControl20 = inputDir + "signalControlCottbusByNodes_v2.0.xml";
		String amberTimes10 = inputDir + "amberTimesCottbusByNodes_v1.0.xml";

		
		new SignalSystems11To20Converter().convert(laneDefinitions, signalSystems, signalSystems20, signalGroups20);
		new SignalConfig11ToControl20Converter().convert(signalSystemConfigurations, signalControl20, amberTimes10);


		inputDir = DgPaths.STUDIESDG + "cottbus/originaldaten/";
		
		String network = inputDir + "network.xml";
		String lanes = inputDir + "laneDefinitions.xml";
		
		signalSystems = inputDir + "signalSystems.xml";
		signalSystemConfigurations = inputDir + "signalSystemsConfigT90.xml";
		
		String lanes20 = inputDir + "lanedefinitionsCottbus_v2.0.xml";
		signalSystems20 = inputDir + "signalSystemsCottbus_v2.0.xml";
		signalGroups20 = inputDir + "signalGroupsCottbus_v2.0.xml";
		signalControl20 = inputDir + "signalControlCottbusT90_v2.0.xml";
		amberTimes10 = inputDir + "amberTimesCottbus_v1.0.xml";

		new LaneDefinitonsV11ToV20Converter().convert(lanes, lanes20, network);
		new SignalSystems11To20Converter().convert(lanes, signalSystems, signalSystems20, signalGroups20);
		new SignalConfig11ToControl20Converter().convert(signalSystemConfigurations, signalControl20, amberTimes10);

		
		
	}

}
