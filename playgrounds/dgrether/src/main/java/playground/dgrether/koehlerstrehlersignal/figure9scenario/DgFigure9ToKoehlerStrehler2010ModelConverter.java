/* *********************************************************************** *
 * project: org.matsim.*
 * DgFigure9ToKoehlerStrehler2010ModelConverter
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
package playground.dgrether.koehlerstrehlersignal.figure9scenario;

import java.io.IOException;

import javax.xml.transform.TransformerConfigurationException;

import org.matsim.core.scenario.ScenarioImpl;
import org.xml.sax.SAXException;

import playground.dgrether.DgPaths;
import playground.dgrether.koehlerstrehlersignal.DgKoehlerStrehler2010ModelWriter;
import playground.dgrether.koehlerstrehlersignal.DgMatsim2KoehlerStrehler2010DemandConverter;
import playground.dgrether.koehlerstrehlersignal.DgMatsim2KoehlerStrehler2010NetworkConverter;
import playground.dgrether.koehlerstrehlersignal.DgMatsim2KoehlerStrehler2010SimpleDemandConverter;
import playground.dgrether.koehlerstrehlersignal.data.DgCommodities;
import playground.dgrether.koehlerstrehlersignal.data.DgNetwork;


public class DgFigure9ToKoehlerStrehler2010ModelConverter {
	
	/**
	 * @param args
	 * @throws SAXException 
	 * @throws IOException 
	 * @throws TransformerConfigurationException 
	 */
	public static void main(String[] args) throws SAXException, TransformerConfigurationException, IOException {
		ScenarioImpl sc = new DgFigure9ScenarioGenerator().loadScenario();
		DgMatsim2KoehlerStrehler2010NetworkConverter converter = new DgMatsim2KoehlerStrehler2010NetworkConverter();
		DgNetwork net = converter.convertNetworkLanesAndSignals(sc);
		
		DgMatsim2KoehlerStrehler2010DemandConverter demandConverter = new DgMatsim2KoehlerStrehler2010SimpleDemandConverter();
		DgCommodities coms = demandConverter.convert(sc, net);
		
		DgKoehlerStrehler2010ModelWriter writer = new DgKoehlerStrehler2010ModelWriter();
		writer.write(sc, net, coms, DgPaths.STUDIESDG + "koehlerStrehler2010/cplex_scenario_population_800_agents.xml");

		
	}

}
