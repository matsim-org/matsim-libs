/* *********************************************************************** *
 * project: org.matsim.*
 * ConvertCottbusSolution2Matsim
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
package playground.dgrether.koehlerstrehlersignal.analysis.ksvsm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.analysis.VolumesAnalyzer;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import playground.dgrether.DgPaths;
import playground.dgrether.koehlerstrehlersignal.data.DgStreet;
import playground.dgrether.koehlerstrehlersignal.ids.DgIdConverter;
import playground.dgrether.koehlerstrehlersignal.ids.DgIdPool;
import playground.dgrether.koehlerstrehlersignal.solutionconverter.KS2014SolutionXMLParser;

/**
 * @author dgrether
 * @author tthunig
 * 
 */
public class KS2010VsMatimVolumes {

	private static Map<Id<Link>, Double> loadKS2010Volumes(String directory,
			String inputFile) {
//		KS2010SolutionTXTParser10 solutionParser = new KS2010SolutionTXTParser10();
		KS2014SolutionXMLParser solutionParser = new KS2014SolutionXMLParser();
		solutionParser.readFile(directory + inputFile);

		DgIdPool idPool = DgIdPool.readFromFile(directory
				+ "id_conversions.txt");
		DgIdConverter dgIdConverter = new DgIdConverter(idPool);

		Map<Id<DgStreet>, Double> ks2010StreetIdFlow = solutionParser
				.getStreetFlow();

		Map<Id<Link>, Double> ks2010volumes = convertKS2010Volumes(idPool,
				dgIdConverter, ks2010StreetIdFlow);
		return ks2010volumes;
	}

	private static Map<Id<Link>, Double> convertKS2010Volumes(DgIdPool idPool,
			DgIdConverter dgIdConverter, Map<Id<DgStreet>, Double> ks2010StreetIdFlow) {
		// convert ks2010_id to matsim_id in the unsimplified network
		Map<Id<Link>, Double> matsimLinkIdFlow = new HashMap<>();
		for (Id<DgStreet> streetId : ks2010StreetIdFlow.keySet()) {
			Id<Link> linkId = dgIdConverter.convertStreetId2LinkId(Id.create(
					streetId, DgStreet.class));
			// assign the flow to all links that belongs to the simplified link
			String[] unsimplifiedLinks = linkId.toString().split("-");
			for (int i = 0; i < unsimplifiedLinks.length; i++)
				matsimLinkIdFlow.put(Id.create(unsimplifiedLinks[i], Link.class),
						ks2010StreetIdFlow.get(streetId));
		}
		return matsimLinkIdFlow;
	}

	private static Map<Id<Link>, Double> loadMatsimVolumes(Network matsimNetwork,
			Network ks2010Network, String matsimEventsFile, int startTime,
			int endTime, double scalingFactor) {

		VolumesAnalyzer va = loadVolumesFromEvents(matsimEventsFile,
				matsimNetwork);

		// convert matsim flow volumes
		Map<Id<Link>, Double> matsimVolumes = new HashMap<>();
		for (Link l : matsimNetwork.getLinks().values()) {

			// bound matsim flow volumes to KS2010 region
			if (ks2010Network.getLinks().containsKey(l.getId())) {
				double[] volumes = va.getVolumesPerHourForLink(l.getId());

				// aggregate matsim flow volumes for the respective peak
				double aggregatedFlow = 0;
				for (int i = startTime; i < endTime; i++)
					aggregatedFlow += volumes[i];
//				// scale matsim flow volumes to the KS2010 demand
//				aggregatedFlow *= scalingFactor;

				matsimVolumes.put(l.getId(), aggregatedFlow);
			}
		}

		return matsimVolumes;
	}

	private static VolumesAnalyzer loadVolumesFromEvents(
			String matsimEventsFile, Network matsimNetwork) {
		VolumesAnalyzer va = new VolumesAnalyzer(3600, 24 * 3600, matsimNetwork);
		EventsManager events = EventsUtils.createEventsManager();
		events.addHandler(va);
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(matsimEventsFile);
		return va;
	}

	private static Network loadNetwork(String networkFile) {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig()); 
		(new MatsimNetworkReader(scenario)).readFile(networkFile);
		return scenario.getNetwork();
	}

	private static void writeFlowVolumesShp(Network ks2010Network, String srs,
			Map<Id<Link>, Double> ks2010Volumes, Map<Id<Link>, Double> matsimVolumes,
			String outputFile, double scalingFactor) {

		CoordinateReferenceSystem networkSrs = MGC.getCRS(srs);
		
		new VolumesShapefileWriter(ks2010Network, networkSrs).writeShape(
				outputFile, ks2010Volumes, matsimVolumes, scalingFactor);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		List<Tuple<String, String>> input = new ArrayList<Tuple<String, String>>();
		input.add(new Tuple<String, String>("50", "morning"));
		input.add(new Tuple<String, String>("50", "evening"));
		input.add(new Tuple<String, String>("10", "morning"));
		input.add(new Tuple<String, String>("10", "evening"));

		for (Tuple<String, String> i : input) {

			String ksSolutionFile;
			// start and end time of the respective peak in hours
			int startTime;
			int endTime;
			
			if (i.getSecond().equals("evening")){
				ksSolutionFile = "ksm_" + i.getFirst() + "a_sol.txt";
				startTime = 13;
				endTime = 19;
			}
			else{ // equals morning
				ksSolutionFile = "ksm_" + i.getFirst() + "m_sol.txt";
				startTime = 5;
				endTime = 10;
			}

			String runNumber;
			// KS2010 demands proportion of the matsim demand
			double scalingFactor;
			
			if (i.getFirst().equals("50")){
				scalingFactor = 0.55; //TODO
				runNumber = "1912"; //TODO
			}
			else{ // equals 10
				scalingFactor = 0.27; //TODO
				runNumber = "1911"; //TODO
			}
			
			String ksSolutionDirectory = DgPaths.REPOS + "shared-svn/projects/cottbus/cb2ks2010/2013-07-31_minflow_"
					+ i.getFirst() + "_" + i.getSecond() + "_peak/";
			String matsimRunDirectory = DgPaths.REPOS + "runs-svn/cottbus/before2015/run" + runNumber + "/";
			
			// unsimplified networks
			String matsimNetworkFile = matsimRunDirectory + runNumber + ".output_network.xml.gz";
			String ks2010NetworkFile = ksSolutionDirectory + "network_small_clean.xml.gz";
			String matsimEventsFile = matsimRunDirectory + "ITERS/it.2000/" + runNumber + ".2000.events.xml.gz";
			String srs = TransformationFactory.WGS84_UTM33N;

			String outputFile = ksSolutionDirectory + "shapes/KS2010_" + i.getFirst() + "_" + i.getSecond() + "_peak" + "VsMatsimRun" + runNumber + "FlowVolumes";

			
			Network matsimNetwork = loadNetwork(matsimNetworkFile);
			Network ks2010Network = loadNetwork(ks2010NetworkFile);

			Map<Id<Link>, Double> ks2010Volumes = loadKS2010Volumes(ksSolutionDirectory,
					ksSolutionFile);
			Map<Id<Link>, Double> matsimVolumes = loadMatsimVolumes(matsimNetwork,
					ks2010Network, matsimEventsFile, startTime, endTime,
					scalingFactor);

			writeFlowVolumesShp(ks2010Network, srs, ks2010Volumes,
					matsimVolumes, outputFile, scalingFactor);
		}

	}

}
