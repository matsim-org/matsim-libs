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
package playground.dgrether.koehlerstrehlersignal.conversion;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.lanes.data.v20.LaneDefinitions20;
import org.matsim.contrib.signals.data.SignalsData;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import playground.dgrether.koehlerstrehlersignal.data.DgCommodities;
import playground.dgrether.koehlerstrehlersignal.data.DgCommodity;
import playground.dgrether.koehlerstrehlersignal.data.DgCommodityUtils;
import playground.dgrether.koehlerstrehlersignal.data.DgCrossing;
import playground.dgrether.koehlerstrehlersignal.data.DgKSNetwork;
import playground.dgrether.koehlerstrehlersignal.data.KS2010ModelWriter;
import playground.dgrether.koehlerstrehlersignal.demand.M2KS2010Zones2Commodities;
import playground.dgrether.koehlerstrehlersignal.gexf.DgKSNetwork2Gexf;
import playground.dgrether.koehlerstrehlersignal.ids.DgIdConverter;
import playground.dgrether.koehlerstrehlersignal.ids.DgIdPool;
import playground.dgrether.koehlerstrehlersignal.network.DgNetworkUtils;
import playground.dgrether.signalsystems.utils.DgSignalsBoundingBox;
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
public class M2KS2010Converter {
	
	public static final Logger log = Logger.getLogger(M2KS2010Converter.class);
	
	private double ksModelCommoditySampleSize = 1.0;

	private Network network;

	private LaneDefinitions20 lanes;

	private SignalsData signals;

	private double minCommodityFlow;

	private CoordinateReferenceSystem crs;
	
	private DgSignalsBoundingBox signalsBoundingBox;

	
	public M2KS2010Converter(Network network, LaneDefinitions20 lanes,
			SignalsData signals, double signalsBoundingBoxOffset, CoordinateReferenceSystem crs) {
		this.network = network;
		this.lanes = lanes;
		this.signals = signals;
		this.crs = crs;
		this.createSignalsEnvelope(signalsBoundingBoxOffset);
	}
	
	private void createSignalsEnvelope(double signalsBoundingBoxOffset) {
		this.signalsBoundingBox = new DgSignalsBoundingBox(crs);
		signalsBoundingBox.calculateBoundingBoxForSignals(this.network, 
				this.signals.getSignalSystemsData(), signalsBoundingBoxOffset);
	}

	private void scaleCommodities(DgCommodities commodities){
		if (ksModelCommoditySampleSize != 1.0){
			for (DgCommodity com : commodities.getCommodities().values()) {
				double flow = com.getFlow() * ksModelCommoditySampleSize;
				com.setFlow(flow);
			}
		}
	}


	public void convertAndWrite(String outputDirectory, String shapeFileDirectory, String filename, 
			String name, String description, DgZones zones, double startTimeSec, double endTimeSec) throws IOException{
		//create koehler strehler network
		DgIdPool idPool = new DgIdPool();
		DgIdConverter idConverter = new DgIdConverter(idPool);
		
		M2KS2010NetworkConverter netConverter = new M2KS2010NetworkConverter(idConverter);
		DgKSNetwork ksNet = netConverter.convertNetworkLanesAndSignals(this.network, this.lanes, this.signals, 
				this.signalsBoundingBox.getBoundingBox(), startTimeSec, endTimeSec);
		
		//gexf output for visualization
		DgKSNetwork2Gexf converter = new DgKSNetwork2Gexf();
		converter.convertAndWrite(ksNet, outputDirectory + "network_small_simplified_ks2010_model.gexf"); //the former name was network_small_simplified.gexf which is misleading
		
		M2KS2010Zones2Commodities demandConverter = new M2KS2010Zones2Commodities(zones, idConverter);
		DgCommodities commodities = demandConverter.convert(ksNet);

		this.scaleCommodities(commodities);
		
		Set<DgCommodity> removedCommodities = new HashSet<DgCommodity>();
		Set<Id<DgCommodity>> commoditiesToRemove = new HashSet<>();
		double totalFlow = 0.0;
		for (DgCommodity com : commodities.getCommodities().values()){
			if (com.getSourceNodeId().equals(com.getDrainNodeId())) {
				log.warn("commodity : " + com.getId() + " flow: " + com.getFlow() + " has same start and drain node: " + com.getSourceNodeId());
			}
		}
		
		for (DgCommodity com : commodities.getCommodities().values()){
			totalFlow += com.getFlow();
			if (com.getFlow() < minCommodityFlow){
				commoditiesToRemove.add(com.getId());
			}
		}
		for (Id<DgCommodity> id : commoditiesToRemove){
			removedCommodities.add(commodities.getCommodities().remove(id));
			log.info("Removed commodity id " + id + " because flow is less than " + minCommodityFlow);
		}
		
		
		// convert the KS2010 network back to the matsim format for debugging and visualization
		Network newMatsimNetwork = new DgKSNet2MatsimNet().convertNetwork(ksNet);
		DgKS2010Router router = new DgKS2010Router();
		List<Id<DgCommodity>> invalidCommodities = router.routeCommodities(newMatsimNetwork, commodities);
		for (Id<DgCommodity> id : invalidCommodities) {
			removedCommodities.add(commodities.getCommodities().remove(id));
			log.warn("removed commodity id : " + id + " because it can not be routed on the network.");
		}
		log.info("testing routing again...");
		invalidCommodities = router.routeCommodities(newMatsimNetwork, commodities);
		if (! invalidCommodities.isEmpty()){
			throw new IllegalStateException("Commodities that can not be routed still exist");
		}
		
		log.warn("To use the information of ks model link costs (i.e. link travel time) in the matsim network shapefile (with freespeed and link length format), we use a default freespeed of 1 m/s and set the link length to the link cost.");
		DgNetworkUtils.writeNetwork(newMatsimNetwork, outputDirectory + "matsim_network_ks_model.xml.gz");
		DgNetworkUtils.writeNetwork2Shape(newMatsimNetwork, crs,  shapeFileDirectory + "matsim_network_ks_model.shp");
		
		DgCommodityUtils.write2Shapefile(commodities, newMatsimNetwork, crs,  shapeFileDirectory + "commodities.shp");

		// write ks-model 
		new KS2010ModelWriter().write(ksNet, commodities, name, description, outputDirectory + filename);
		
		// write commodities from the ks-model as matsim population in the small network
		new TtMorningCommodityAsMatsimPopWriter().writeTripPlansFile(this.network, commodities, outputDirectory, filename, startTimeSec, endTimeSec);
		
		writeStats(ksNet, commodities, totalFlow, removedCommodities);
		
		signalsBoundingBox.writeBoundingBox(shapeFileDirectory + "signals_");
		
		idPool.writeToFile(outputDirectory + "id_conversions.txt");
	}



	private void writeStats( DgKSNetwork ksNet, DgCommodities commodities, double totalFlow, Set<DgCommodity> removedCommodities){
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
		log.info("  # Commodities removed: " + removedCommodities.size());
		log.info("Overall flow: " + totalFlow);
		double removedFlow = 0.0;
		String info = "";
		for (DgCommodity com : removedCommodities){
			removedFlow += com.getFlow();
			info += comToString(com) + " ; ";
		}
		log.info("Removed flow: " + removedFlow + " %: "+ Double.toString(removedFlow/totalFlow * 100.0));
		log.info("Removed Commodities : " );
		log.info(info);
		
	}

	public String comToString(DgCommodity com) {
		return com.getId().toString() + " " + com.getFlow();
	}
	
	public double getKsModelCommoditySampleSize() {
		return ksModelCommoditySampleSize;
	}

	
	public void setKsModelCommoditySampleSize(double ksModelCommoditySampleSize) {
		this.ksModelCommoditySampleSize = ksModelCommoditySampleSize;
	}


	public void setMinCommodityFlow(double minCommodityFlow) {
		this.minCommodityFlow = minCommodityFlow;
	}


}
