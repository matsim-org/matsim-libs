/* *********************************************************************** *
 * project: org.matsim.*
 * RunHotspotPricingMunich.java
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
package playground.benjamin.scenarios.munich;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.emissions.EmissionModule;
import org.matsim.contrib.otfvis.OTFVisFileWriterModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigReader;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;

import playground.benjamin.internalization.EmissionCostModule;
import playground.benjamin.internalization.EmissionTravelDisutilityCalculatorFactory;
import playground.benjamin.internalization.InternalizeEmissionsControlerListener;

/**
 * @author benjamin
 *
 */
public class RunHotspotPricingMunich {
	private static final Logger logger = Logger.getLogger(RunHotspotPricingMunich.class);
	
	static String configFile;
	static String emissionCostFactor;
	static String considerCO2Costs;
	static String hotspotFile;

	public static void main(String[] args) {
		configFile = "../../runs-svn/detEval/test/input/config_munich_1pct_policyCase_hotspotPricing.xml";
		emissionCostFactor = "1.0";
		considerCO2Costs = "false";
		hotspotFile = "../../detailedEval/papers/mobilTUM2011/Hotspots/Hotspots_No2_annualmean1pct.txt";
		
//		configFile = args[0];
//		emissionCostFactor = args[1];
//		considerCO2Costs = args[2];
//		hotspotFile = args[3];
		
		Set<Id<Link>> hotspotLinks = createHotspotLinks(hotspotFile);
		Set<Id<Link>> hotspotLinksMerged = findMergedLinks(hotspotLinks, configFile);
		
		Config config = new Config();
		config.addCoreModules();
		ConfigReader confReader = new ConfigReader(config);
		confReader.readFile(configFile);

		Controler controler = new Controler(config);
		Scenario scenario = controler.getScenario();

		EmissionModule emissionModule = new EmissionModule(scenario);
		emissionModule.createLookupTables();
		emissionModule.createEmissionHandler();
		
		EmissionCostModule emissionCostModule = new EmissionCostModule(Double.parseDouble(emissionCostFactor), Boolean.parseBoolean(considerCO2Costs));

		final EmissionTravelDisutilityCalculatorFactory emissionTducf = new EmissionTravelDisutilityCalculatorFactory(emissionModule, 
				emissionCostModule, config.planCalcScore());
		emissionTducf.setHotspotLinks(hotspotLinks);
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bindCarTravelDisutilityFactory().toInstance(emissionTducf);
			}
		});

		InternalizeEmissionsControlerListener iecl = new InternalizeEmissionsControlerListener(emissionModule, emissionCostModule);
		iecl.setHotspotLinks(hotspotLinksMerged);
		controler.addControlerListener(iecl);

		controler.getConfig().controler().setOverwriteFileSetting(
				OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
		controler.addOverridingModule(new OTFVisFileWriterModule());
		controler.run();
	}

	private static Set<Id<Link>> findMergedLinks(Set<Id<Link>> hotspotLinks, String fileName) {
		logger.info("entering findMergedLinks ...");
		Set<Id<Link>> hotspotLinksMerged = new HashSet<>();
		
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.loadConfig(fileName));
		new MatsimNetworkReader(sc.getNetwork()).readFile(sc.getConfig().network().getInputFile());
		Network network = sc.getNetwork();
		
		for(Link link : network.getLinks().values()){
			String mergedLinkIdString = link.getId().toString();
			for(Id<Link> hotspotLink : hotspotLinks){
				String hotspoLinkString = hotspotLink.toString();
				if(mergedLinkIdString.contains(hotspoLinkString)){
					hotspotLinksMerged.add(link.getId());
				} else {
					// do nothing
				}
			}
		}
		logger.info("Considering " + hotspotLinksMerged.size() + " MERGED links as hotspots ...");
		logger.info("The following MERGED links are considered as hotspots: " + hotspotLinksMerged);
		logger.info("leaving findMergedLinks ...");
		return hotspotLinksMerged;
	}

	private static Set<Id<Link>> createHotspotLinks(String fileName) {
		logger.info("entering createHotspotLinks ...");
		
		Set<Id<Link>> hotspotLinks = new HashSet<Id<Link>>();
		try{
			BufferedReader br = IOUtils.getBufferedReader(fileName);
			String strLine = br.readLine();
			Map<String, Integer> indexFromKey = createIndexFromKey(strLine, ";");
			
			while ((strLine = br.readLine()) != null){
				if ( strLine.contains("\"")) throw new RuntimeException("cannot handle this character in parsing") ;

				String[] inputArray = strLine.split(";");
				Id<Link> linkId1 = Id.create(inputArray[indexFromKey.get("linkId1")], Link.class);
				Id<Link> linkId2 = Id.create(inputArray[indexFromKey.get("linkId2")], Link.class);
				
				hotspotLinks.add(linkId1);					
				hotspotLinks.add(linkId2);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		logger.info("Considering " + hotspotLinks.size() + " ORIGINAL links as hotspots ...");
		logger.info("The following ORIGINAL links are considered as hotspots: " + hotspotLinks);
		logger.info("leaving createHotspotLinks ...");
		return hotspotLinks;
	}

	private static Map<String, Integer> createIndexFromKey(String strLine, String fieldSeparator) {
		String[] keys = strLine.split(fieldSeparator) ;

		Map<String, Integer> indexFromKey = new HashMap<String, Integer>() ;
		for ( int ii = 0; ii < keys.length; ii++ ) {
			indexFromKey.put(keys[ii], ii ) ;
		}
		return indexFromKey ;
	}

}
