/* *********************************************************************** *
 * project: org.matsim.*
 * Cottbus2KoehlerStrehler2010
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
package playground.dgrether.koehlerstrehlersignal;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.controler.OutputDirectoryLogging;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.utils.misc.Time;
import org.matsim.signalsystems.data.SignalsData;
import org.matsim.signalsystems.data.signalsystems.v20.SignalSystemsData;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import playground.dgrether.koehlerstrehlersignal.data.DgCommodities;
import playground.dgrether.koehlerstrehlersignal.data.DgCommodity;
import playground.dgrether.koehlerstrehlersignal.data.DgCrossing;
import playground.dgrether.koehlerstrehlersignal.data.DgKSNet2MatsimNet;
import playground.dgrether.koehlerstrehlersignal.data.DgKSNetwork;
import playground.dgrether.koehlerstrehlersignal.gexf.DgKSNetwork2Gexf;
import playground.dgrether.koehlerstrehlersignal.ids.DgIdConverter;
import playground.dgrether.koehlerstrehlersignal.ids.DgIdPool;
import playground.dgrether.signalsystems.utils.DgSignalsUtils;
import playground.dgrether.utils.zones.DgZone;
import playground.dgrether.utils.zones.DgZoneUtils;
import playground.dgrether.utils.zones.DgZones;

/**
 * Converts a MATSim Scenario to the traffic signal optimization model published in 
 * <ul>
 * <li>K&ouml;hler, E. &amp; Strehler, M.</li>
 * <li><i>Traffic Signal Optimization Using Cyclically Expanded Networks</i></li>
 * <li>Erlebach, T. &amp; L&uuml;bbecke, M. <i>(ed.)</i></li>
 * <li>Proc. 10th Workshop Algorithmic Approaches for Transportation Modelling, Optimization, and Systems</li>
 * <li><b>2010</b>(14), pp. 114-129</li>
 * </ul>
 * 
 * 
 * @author dgrether
 * @author tthunig
 */
public class Scenario2KoehlerStrehler2010 {
	
	public static final Logger log = Logger.getLogger(Scenario2KoehlerStrehler2010.class);
	
	private static final String modelOutfile = "koehler_strehler_model.xml";
	
	private double matsimPopSampleSize = 1.0;
	private double ksModelCommoditySampleSize = 1.0;
	private String shapeFileDirectory = "shapes/";

	private Scenario scenario;

	private CoordinateReferenceSystem crs;
	
	
	public Scenario2KoehlerStrehler2010(Scenario scenario, CoordinateReferenceSystem crs) {
		this.scenario = scenario;
		this.crs = crs;
	}

	
	public void convert(String outputDirectory, String name, DgZones zones, double boundingBoxOffset, int cellsX, int cellsY, double startTimeSec, double endTimeSec) throws IOException{
		OutputDirectoryLogging.initLoggingWithOutputDirectory(outputDirectory);

		Map<DgZone, Link> zones2LinkMap = DgZoneUtils.createZoneCenter2LinkMapping(zones, (NetworkImpl) scenario.getNetwork());
		DgZoneUtils.writeLinksOfZones2Shapefile(zones, zones2LinkMap, crs, shapeFileDirectory + "links_for_zones.shp");

		//create koehler strehler network
		DgIdPool idPool = new DgIdPool();
		DgIdConverter idConverter = new DgIdConverter(idPool);
		
		Set<Id> signalizedLinks = this.getSignalizedLinkIds(this.scenario.getScenarioElement(SignalsData.class).getSignalSystemsData());
		DgMatsim2KoehlerStrehler2010NetworkConverter netConverter = new DgMatsim2KoehlerStrehler2010NetworkConverter(idConverter);
		netConverter.setSignalizedLinks(signalizedLinks);
		DgKSNetwork ksNet = netConverter.convertNetworkLanesAndSignals(scenario, startTimeSec, endTimeSec);
		DgKSNetwork2Gexf converter = new DgKSNetwork2Gexf();
		converter.convertAndWrite(ksNet, outputDirectory + "network_small.gexf");
		
		DgMatsim2KoehlerStrehler2010DemandConverter demandConverter = new DgMatsim2KoehlerStrehler2010Zones2Commodities(zones2LinkMap, idConverter);
		DgCommodities commodities = demandConverter.convert(scenario, ksNet);
		
		if (ksModelCommoditySampleSize != 1.0){
			for (DgCommodity com : commodities.getCommodities().values()) {
				double flow = com.getFlow() * ksModelCommoditySampleSize;
				com.setSourceNode(com.getSourceNode(), flow);
			}
		}
		
		String description = "offset: " + boundingBoxOffset + "cellsX: " + cellsX + " cellsY: " + cellsY + " startTimeSec: " + startTimeSec + " endTimeSec: " + endTimeSec;
		description += "matsimPopsampleSize: " + this.matsimPopSampleSize + " ksModelCommoditySampleSize: " + this.ksModelCommoditySampleSize;

		Network newMatsimNetwork = new DgKSNet2MatsimNet().convertNetwork(ksNet);
		DgKoehlerStrehler2010Router router = new DgKoehlerStrehler2010Router();
		List<Id> invalidCommodities = router.routeCommodities(newMatsimNetwork, commodities);
		for (Id id : invalidCommodities) {
			commodities.getCommodities().remove(id);
		}
		log.info("testing routing again...");
		router.routeCommodities(newMatsimNetwork, commodities);
		
		DgNetworkUtils.writeNetwork(newMatsimNetwork, outputDirectory + "matsimNetworkKsModel.xml.gz");
		DgNetworkUtils.writeNetwork2Shape(newMatsimNetwork, shapeFileDirectory + "matsimNetworkKsModel.shp");
		
		new DgKoehlerStrehler2010ModelWriter().write(ksNet, commodities, name, description, outputDirectory + modelOutfile);
		writeStats(cellsX, cellsY, boundingBoxOffset, startTimeSec, endTimeSec, ksNet, commodities);
		
		idPool.writeToFile(outputDirectory + "id_conversions.txt");
		
		OutputDirectoryLogging.closeOutputDirLogging();		
	}

	private Set<Id> getSignalizedLinkIds(SignalSystemsData signals){
		Map<Id, Set<Id>> signalizedLinksPerSystem = DgSignalsUtils.calculateSignalizedLinksPerSystem(signals);
		Set<Id> signalizedLinks = new HashSet<Id>();
		for (Set<Id> signalizedLinksOfSystem : signalizedLinksPerSystem.values()){
			signalizedLinks.addAll(signalizedLinksOfSystem);
		}
		return signalizedLinks;
	}

	public void writeStats(int cellsX, int cellsY, double boundingBoxOffset, double startTime, double endTime, DgKSNetwork ksNet, DgCommodities commodities){
		log.info("Cells:");
		log.info("  X " + cellsX + " Y " + cellsY);
		log.info("Bounding Box: ");
		log.info("  Offset: " + boundingBoxOffset);
		log.info("Time: " );
		log.info("  startTime: " + startTime + " " + Time.writeTime(startTime));
		log.info("  endTime: " + endTime  + " " + Time.writeTime(endTime));
		log.info("Network: ");
		log.info("  # Streets: " + ksNet.getStreets().size() );
		log.info("  # Crossings: " + ksNet.getCrossings().size());
		int noLights = 0;
		int noNodes = 0;
		for (DgCrossing c : ksNet.getCrossings().values()){
			noLights += c.getLights().size();
			noNodes += c.getNodes().size();
		}
		log.info("  # Lights: " + noLights);
		log.info("  # Nodes: " + noNodes);
		log.info("Commodities: ");
		log.info("  # Commodities: " + commodities.getCommodities().size());
	}
	

	
	
	
	public double getKsModelCommoditySampleSize() {
		return ksModelCommoditySampleSize;
	}

	
	public void setKsModelCommoditySampleSize(double ksModelCommoditySampleSize) {
		this.ksModelCommoditySampleSize = ksModelCommoditySampleSize;
	}
}
