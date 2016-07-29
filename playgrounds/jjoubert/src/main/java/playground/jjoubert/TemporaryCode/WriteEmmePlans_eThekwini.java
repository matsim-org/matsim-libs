/* *********************************************************************** *
 * project: org.matsim.*
 * WriteEmmePlans_eThekwini.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package playground.jjoubert.TemporaryCode;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.utils.gis.matsim2esri.plans.SelectedPlans2ESRIShape;

public class WriteEmmePlans_eThekwini {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Scenario s = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		MatsimNetworkReader nr = new MatsimNetworkReader(s.getNetwork());
		nr.readFile("/Users/johanwjoubert/MATSim/workspace/MATSimData/eThekwini/2005/Input/output_network_100_Emme.xml.gz");
		PopulationReader pr = new PopulationReader(s);
		pr.readFile("/Users/johanwjoubert/MATSim/workspace/MATSimData/eThekwini/2005/Input/output_plans_100.xml.gz");
		

		SelectedPlans2ESRIShape sp = new SelectedPlans2ESRIShape(s.getPopulation(), 
				s.getNetwork(),MGC.getCRS("WGS84_UTM36S"), 
				"/Users/johanwjoubert/MATSim/workspace/MATSimData/eThekwini/2005/Emme");
		sp.setWriteActs(true);
		sp.setWriteLegs(false);
		sp.write();
	}

}
