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
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.lanes.data.v20.LaneDefinitions20;
import org.matsim.signalsystems.data.SignalsData;
import org.matsim.signalsystems.data.signalsystems.v20.SignalSystemsData;

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
	
	private double ksModelCommoditySampleSize = 1.0;

	private Network network;

	private LaneDefinitions20 lanes;

	private SignalsData signals;

	
	public Scenario2KoehlerStrehler2010(Network network, LaneDefinitions20 lanes,
			SignalsData signals) {
		this.network = network;
		this.lanes = lanes;
		this.signals = signals;
	}


	public void convert(String outputDirectory, String shapeFileDirectory, String name, String description, Map<DgZone, Link> zones2LinkMap, double startTimeSec, double endTimeSec) throws IOException{
		//create koehler strehler network
		DgIdPool idPool = new DgIdPool();
		DgIdConverter idConverter = new DgIdConverter(idPool);
		
		Set<Id> signalizedLinks = this.getSignalizedLinkIds(this.signals.getSignalSystemsData());
		DgMatsim2KoehlerStrehler2010NetworkConverter netConverter = new DgMatsim2KoehlerStrehler2010NetworkConverter(idConverter);
		netConverter.setSignalizedLinks(signalizedLinks);
		DgKSNetwork ksNet = netConverter.convertNetworkLanesAndSignals(this.network, this.lanes, this.signals, startTimeSec, endTimeSec);
		DgKSNetwork2Gexf converter = new DgKSNetwork2Gexf();
		converter.convertAndWrite(ksNet, outputDirectory + "network_small_simplified.gexf");
		
		DgMatsim2KoehlerStrehler2010Zones2Commodities demandConverter = new DgMatsim2KoehlerStrehler2010Zones2Commodities(zones2LinkMap, idConverter);
		DgCommodities commodities = demandConverter.convert(ksNet);
		
		if (ksModelCommoditySampleSize != 1.0){
			for (DgCommodity com : commodities.getCommodities().values()) {
				double flow = com.getFlow() * ksModelCommoditySampleSize;
				com.setSourceNode(com.getSourceNode(), flow);
			}
		}
		
		Network newMatsimNetwork = new DgKSNet2MatsimNet().convertNetwork(ksNet);
		DgKoehlerStrehler2010Router router = new DgKoehlerStrehler2010Router();
		List<Id> invalidCommodities = router.routeCommodities(newMatsimNetwork, commodities);
		for (Id id : invalidCommodities) {
			commodities.getCommodities().remove(id);
		}
		log.info("testing routing again...");
		router.routeCommodities(newMatsimNetwork, commodities);
		
		DgNetworkUtils.writeNetwork(newMatsimNetwork, outputDirectory + "matsim_network_ks_model.xml.gz");
		DgNetworkUtils.writeNetwork2Shape(newMatsimNetwork, shapeFileDirectory + "matsim_network_ks_model.shp");
		
		new DgKoehlerStrehler2010ModelWriter().write(ksNet, commodities, name, description, outputDirectory + modelOutfile);
		writeStats(ksNet, commodities);
		
		idPool.writeToFile(outputDirectory + "id_conversions.txt");
	}

	private Set<Id> getSignalizedLinkIds(SignalSystemsData signals){
		Map<Id, Set<Id>> signalizedLinksPerSystem = DgSignalsUtils.calculateSignalizedLinksPerSystem(signals);
		Set<Id> signalizedLinks = new HashSet<Id>();
		for (Set<Id> signalizedLinksOfSystem : signalizedLinksPerSystem.values()){
			signalizedLinks.addAll(signalizedLinksOfSystem);
		}
		return signalizedLinks;
	}

	private void writeStats( DgKSNetwork ksNet, DgCommodities commodities){
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
