/* *********************************************************************** *
 * project: org.matsim.*                                                   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

/**
 * 
 */
package org.matsim.contrib.wagonSim.run;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.wagonSim.Utils;
import org.matsim.contrib.wagonSim.network.NEMOInfraDataContainer;
import org.matsim.contrib.wagonSim.network.NEMOInfraParser;
import org.matsim.contrib.wagonSim.network.NEMOInfraToMATSimNetworkConverter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;

/**
 * @author balmermi
 *
 */
public class NEMOInfraToMATSimNetworkConverterMain {

	//////////////////////////////////////////////////////////////////////
	// variables
	//////////////////////////////////////////////////////////////////////
	
	private static final Logger log = Logger.getLogger(NEMOInfraToMATSimNetworkConverterMain.class);
	
	private final Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
	private final ObjectAttributes nodeAttributes = new ObjectAttributes();
	private final ObjectAttributes linkAttributes = new ObjectAttributes();
	
	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////
	
	public NEMOInfraToMATSimNetworkConverterMain() {
	}

	//////////////////////////////////////////////////////////////////////
	// methods
	//////////////////////////////////////////////////////////////////////
	
	public final void convertFromFile(String nemoInfraXmlFile) {
		NEMOInfraDataContainer dataContainer = new NEMOInfraDataContainer();
		new NEMOInfraParser(dataContainer).readFile(nemoInfraXmlFile);
		NEMOInfraToMATSimNetworkConverter converter = new NEMOInfraToMATSimNetworkConverter(scenario.getNetwork(),nodeAttributes,linkAttributes);
		converter.convert(dataContainer);
		converter.makeNetworkBiDirectional();
		log.info("network contains "+scenario.getNetwork().getNodes().size()+" nodes and "+scenario.getNetwork().getLinks().size()+" links.");
		if (converter.validateNetwork()) { log.info("network is valid."); }
		else { log.warn("network is not yet valid."); }
	}
	
	//////////////////////////////////////////////////////////////////////

	public final Scenario getScenario() {
		return this.scenario;
	}
	
	//////////////////////////////////////////////////////////////////////
	
	public final ObjectAttributes getNodeAttributes() {
		return this.nodeAttributes;
	}
	
	//////////////////////////////////////////////////////////////////////
	
	public final ObjectAttributes getLinkAttributes() {
		return this.linkAttributes;
	}
	
	//////////////////////////////////////////////////////////////////////
	// main
	//////////////////////////////////////////////////////////////////////
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {

//		args = new String[] {
//				"S:/raw/europe/ch/ch/sbb/0002/20130524_Daten_Infrastruktur/._infra.xml",
//				"D:/Users/balmermi/Documents/eclipse/output/sbb/networkNemoInfra",
//		};
		
		if (args.length != 2) {
			log.error(NEMOInfraToMATSimNetworkConverterMain.class.getCanonicalName()+" nemoInfraXmlFile outputBase");
			System.exit(-1);
		}
		
		String nemoInfraXmlFile = args[0];
		String outputBase = args[1];
		
		log.info("Main: "+NEMOInfraToMATSimNetworkConverterMain.class.getCanonicalName());
		log.info("NemoInfraXmlFile: "+nemoInfraXmlFile);
		log.info("outputBase: "+outputBase);
		
		NEMOInfraToMATSimNetworkConverterMain converter = new NEMOInfraToMATSimNetworkConverterMain();
		converter.convertFromFile(nemoInfraXmlFile);
		
		if (!Utils.prepareFolder(outputBase)) {
			throw new RuntimeException("Could not prepare output folder for one of the three reasons: (i) folder exists and is not empty, (ii) it's a path to an existing file or (iii) the folder could not be created. Bailing out.");
		}
		
		new NetworkWriter(converter.getScenario().getNetwork()).write(outputBase+"/network.infra.xml.gz");
		new ObjectAttributesXmlWriter(converter.getNodeAttributes()).writeFile(outputBase+"/nodeAttributes.infra.xml.gz");
		new ObjectAttributesXmlWriter(converter.getLinkAttributes()).writeFile(outputBase+"/linkAttributes.infra.xml.gz");
	}
}
