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

import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.wagonSim.Utils;
import org.matsim.contrib.wagonSim.production.ProductionDataContainer;
import org.matsim.contrib.wagonSim.production.ProductionParser;
import org.matsim.contrib.wagonSim.production.ProductionToMATSimNetworkConverter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;

/**
 * @author balmermi
 *
 */
public class ProductionToMATSimNetworkConverterMain {

	//////////////////////////////////////////////////////////////////////
	// variables
	//////////////////////////////////////////////////////////////////////
	
	private static final Logger log = Logger.getLogger(ProductionToMATSimNetworkConverterMain.class);
	
	private final Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
	private final ObjectAttributes nodeAttributes = new ObjectAttributes();
	private final ObjectAttributes linkAttributes = new ObjectAttributes();
	
	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////
	
	public ProductionToMATSimNetworkConverterMain() {
	}

	//////////////////////////////////////////////////////////////////////
	// methods
	//////////////////////////////////////////////////////////////////////
	
	public final void convertFromFiles(String rbFile, String rcpFile, String satFile, String deliveryTypeFile, String timeVariationFile, String routeFile, Network infraNetwork) throws IOException {
		ProductionDataContainer dataContainer = new ProductionDataContainer();
		ProductionParser productionParser = new ProductionParser(dataContainer);
		productionParser.parseDeliveryAndTimeVariationFiles(deliveryTypeFile,timeVariationFile);
		productionParser.parseRbFile(rbFile);
		productionParser.parseRcpFile(rcpFile);
		productionParser.parseSatFile(satFile);
		productionParser.parseRouteFile(routeFile);
		
		ProductionToMATSimNetworkConverter converter = new ProductionToMATSimNetworkConverter(scenario.getNetwork(),nodeAttributes,linkAttributes);
		converter.convert(dataContainer,infraNetwork);
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
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {

//		args = new String[] {
//				"D:/tmp/sbb/0002/20130604_Daten_Produktionsstruktur/20130604_Daten_Produktionsstruktur/rangierbahnhof_2013_CargoRail.txt",
//				"D:/tmp/sbb/0002/20130604_Daten_Produktionsstruktur/20130604_Daten_Produktionsstruktur/teambahnhof_2013_CargoRail.txt",
//				"D:/tmp/sbb/0002/20130604_Daten_Produktionsstruktur/20130604_Daten_Produktionsstruktur/bedienpunkte_2013_CargoRail.txt",
//				"D:/tmp/sbb/0002/20130604_Daten_Produktionsstruktur/20130604_Daten_Produktionsstruktur/versandtyp_2013_CargoRail.txt",
//				"D:/tmp/sbb/0002/20130604_Daten_Produktionsstruktur/20130604_Daten_Produktionsstruktur/tagesganglinie_2013_CargoRail.txt",
//				"D:/tmp/sbb/0002/20130604_Daten_Produktionsstruktur/20130604_Daten_Produktionsstruktur/leitwege_2013_CargoRail.txt",
//				"D:/tmp/sbb/0002/20130524_Daten_Infrastruktur/._infra.xml",
//				"D:/Users/balmermi/Documents/eclipse/output/sbb/networkProduction",
//		};
		
		if (args.length != 8) {
			log.error(ProductionToMATSimNetworkConverterMain.class.getCanonicalName()+" rbFile rcpFile satFile deliveryTypeFile timeVariationFile routeFile nemoInfraXmlFile outputBase");
			System.exit(-1);
		}
		
		String rbFile = args[0];
		String rcpFile = args[1];
		String satFile = args[2];
		String deliveryTypeFile = args[3];
		String timeVariationFile = args[4];
		String routeFile = args[5];
		String nemoInfraXmlFile = args[6];
		String outputBase = args[7];
		
		log.info("Main: "+ProductionToMATSimNetworkConverterMain.class.getCanonicalName());
		log.info("rbFile: "+rbFile);
		log.info("rcpFile: "+rcpFile);
		log.info("satFile: "+satFile);
		log.info("deliveryTypeFile: "+deliveryTypeFile);
		log.info("timeVariationFile: "+timeVariationFile);
		log.info("routeFile: "+routeFile);
		log.info("nemoInfraXmlFile: "+nemoInfraXmlFile);
		log.info("outputBase: "+outputBase);

		NEMOInfraToMATSimNetworkConverterMain networkConverter = new NEMOInfraToMATSimNetworkConverterMain();
		networkConverter.convertFromFile(nemoInfraXmlFile);
		
		ProductionToMATSimNetworkConverterMain converter = new ProductionToMATSimNetworkConverterMain();
		converter.convertFromFiles(rbFile,rcpFile,satFile,deliveryTypeFile,timeVariationFile,routeFile,networkConverter.getScenario().getNetwork());
		
		if (!Utils.prepareFolder(outputBase)) {
			throw new RuntimeException("Could not prepare output folder for one of the three reasons: (i) folder exists and is not empty, (ii) it's a path to an existing file or (iii) the folder could not be created. Bailing out.");
		}
		
		new NetworkWriter(converter.getScenario().getNetwork()).write(outputBase+"/network.xml.gz");
		new ObjectAttributesXmlWriter(converter.getNodeAttributes()).writeFile(outputBase+"/nodeAttributes.xml.gz");
		new ObjectAttributesXmlWriter(converter.getLinkAttributes()).writeFile(outputBase+"/linkAttributes.xml.gz");
	}
}
