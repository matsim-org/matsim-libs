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
package playground.dgrether.signalsystems.cottbus.scripts;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.network.NetworkWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.OutputDirectoryLogging;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.signalsystems.data.SignalsData;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import playground.dgrether.koehlerstrehlersignal.DgKoehlerStrehler2010ModelWriter;
import playground.dgrether.koehlerstrehlersignal.DgMatsim2KoehlerStrehler2010DemandConverter;
import playground.dgrether.koehlerstrehlersignal.DgMatsim2KoehlerStrehler2010NetworkConverter;
import playground.dgrether.koehlerstrehlersignal.DgMatsim2KoehlerStrehler2010Zones2Commodities;
import playground.dgrether.koehlerstrehlersignal.data.DgCommodities;
import playground.dgrether.koehlerstrehlersignal.data.DgCommodity;
import playground.dgrether.koehlerstrehlersignal.data.DgCrossing;
import playground.dgrether.koehlerstrehlersignal.data.DgKSNetwork;
import playground.dgrether.koehlerstrehlersignal.ids.DgIdConverter;
import playground.dgrether.koehlerstrehlersignal.ids.DgIdPool;
import playground.dgrether.utils.DgGrid;
import playground.dgrether.utils.DgGridUtils;
import playground.dgrether.utils.DgPopulationSampler;
import playground.dgrether.utils.zones.DgMatsimPopulation2Zones;
import playground.dgrether.utils.zones.DgZone;
import playground.dgrether.utils.zones.DgZonesUtils;

import com.vividsolutions.jts.geom.Envelope;

/**
 * @author dgrether
 */
public class DgMATSimScenario2KoehlerStrehler2010 {
	
	public static final Logger log = Logger.getLogger(DgMATSimScenario2KoehlerStrehler2010.class);
	
	private static final String smallNetworkFilename = "network_small.xml.gz";
	private static final String modelOutfile = "koehler_strehler_model.xml";
	private double matsimPopSampleSize = 1.0;
	private double ksModelCommoditySampleSize = 1.0;

	private Scenario fullScenario;
	
	
	public DgMATSimScenario2KoehlerStrehler2010(Scenario scenario) {
		this.fullScenario = scenario;
	}

	public void convert(String outputDirectory, String name, double boundingBoxOffset, int cellsX, int cellsY, double startTimeSec, double endTimeSec) throws IOException{
		OutputDirectoryLogging.initLoggingWithOutputDirectory(outputDirectory);
		DgIdPool idPool = new DgIdPool();
		DgIdConverter idConverter = new DgIdConverter(idPool);
		String smallNetworkFile = outputDirectory +  smallNetworkFilename;
		
		DgCottbusSmallNetworkGenerator netShrinker = new DgCottbusSmallNetworkGenerator();
		netShrinker.createSmallNetwork(fullScenario.getNetwork(), fullScenario.getScenarioElement(SignalsData.class).getSignalSystemsData(), outputDirectory, boundingBoxOffset);
		Network smallNetwork = netShrinker.getShrinkedNetwork();
		this.writeNetwork(smallNetwork, smallNetworkFile);
		
//		System.exit(0);
		DgGrid grid = createAndWriteGrid(netShrinker.getBoundingBox(), netShrinker.getCrs(), cellsX, cellsY,  outputDirectory + "grid.shp");
		
		//create some zones and map demand on the zones
		if (matsimPopSampleSize != 1.0){
			new DgPopulationSampler().samplePopulation(fullScenario.getPopulation(), matsimPopSampleSize);
		}
		List<DgZone> cells = DgZonesUtils.createZonesFromGrid(grid);
		cells = new DgMatsimPopulation2Zones().convert2Zones(this.fullScenario.getNetwork(), this.fullScenario.getPopulation(), cells,
				netShrinker.getBoundingBox(), startTimeSec, endTimeSec);
		DgZonesUtils.writePolygonZones2Shapefile(cells, netShrinker.getCrs(), outputDirectory + "grid_cells.shp");
		DgZonesUtils.writeLineStringOdPairsFromZones2Shapefile(cells, netShrinker.getCrs(), outputDirectory + "grid_od_pairs.shp");
		Map<DgZone, Link> zones2LinkMap = DgZonesUtils.createZoneCenter2LinkMapping(cells, (NetworkImpl) netShrinker.getShrinkedNetwork());

		//create koehler strehler network
		Config smallNetConfig = ConfigUtils.createConfig();
		smallNetConfig.network().setInputFile(smallNetworkFile);
		smallNetConfig.scenario().setUseLanes(true);
		smallNetConfig.network().setLaneDefinitionsFile(fullScenario.getConfig().network().getLaneDefinitionsFile());
		Scenario smallScenario = ScenarioUtils.loadScenario(smallNetConfig);
		smallScenario.addScenarioElement(this.fullScenario.getScenarioElement(SignalsData.class));
		
		DgKSNetwork ksNet = new DgMatsim2KoehlerStrehler2010NetworkConverter(idConverter).convertNetworkLanesAndSignals(smallScenario, startTimeSec, endTimeSec);

		DgMatsim2KoehlerStrehler2010DemandConverter demandConverter = new DgMatsim2KoehlerStrehler2010Zones2Commodities(zones2LinkMap, idConverter);
		DgCommodities commodities = demandConverter.convert(smallScenario, ksNet);
		
		if (ksModelCommoditySampleSize != 1.0){
			for (DgCommodity com : commodities.getCommodities().values()) {
				List<Id> ids = new ArrayList<Id>();
				ids.addAll(com.getSourceNodesFlowMap().keySet());
				for (Id id : ids){
					Double flow = com.getSourceNodesFlowMap().get(id);
					com.getSourceNodesFlowMap().put(id, flow * ksModelCommoditySampleSize);
				}
			}
		}
		
		String description = "offset: " + boundingBoxOffset + "cellsX: " + cellsX + " cellsY: " + cellsY + " startTimeSec: " + startTimeSec + " endTimeSec: " + endTimeSec;
		description += "matsimPopsampleSize: " + this.matsimPopSampleSize + " ksModelCommoditySampleSize: " + this.ksModelCommoditySampleSize;
		
		new DgKoehlerStrehler2010ModelWriter().write(ksNet, commodities, name, description, outputDirectory + modelOutfile);
		writeStats(cellsX, cellsY, boundingBoxOffset, startTimeSec, endTimeSec, ksNet, commodities);
		
		idPool.writeToFile(outputDirectory + "id_conversions.txt");
		
		OutputDirectoryLogging.closeOutputDirLogging();		
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
	

	
	public static Scenario loadScenario(String net, String pop, String lanesFilename, String signalsFilename,
			String signalGroupsFilename, String signalControlFilename){
		Config c2 = ConfigUtils.createConfig();
		c2.scenario().setUseLanes(true);
		c2.scenario().setUseSignalSystems(true);
		c2.network().setInputFile(net);
		c2.plans().setInputFile(pop);
		c2.network().setLaneDefinitionsFile(lanesFilename);
		c2.signalSystems().setSignalSystemFile(signalsFilename);
		c2.signalSystems().setSignalGroupsFile(signalGroupsFilename);
		c2.signalSystems().setSignalControlFile(signalControlFilename);
		Scenario scenario = ScenarioUtils.loadScenario(c2);
		return scenario;
	}
	
	
	public DgGrid createAndWriteGrid(Envelope boundingBox, CoordinateReferenceSystem crs, int xCells, int yCells, String outputfilename){
		Envelope gridBoundingBox = new Envelope(boundingBox);
		//expand the grid size to avoid rounding errors 
		gridBoundingBox.expandBy(0.1);
		DgGrid grid = new DgGrid(xCells, yCells, gridBoundingBox);
		DgGridUtils.writeGrid2Shapefile(grid, crs, outputfilename);
		return grid;
	}
	
	public void writeNetwork(Network net, String outputFile){
		NetworkWriter netWriter = new NetworkWriter(net);
		netWriter.write(outputFile);
	}
	


	
	
	public DgCottbusSmallNetworkGenerator shrinkAndWriteNetwork(String networkFile, String outputDirectory, String signalSystemsFile, String shrinkedNetOutfile, double offset){
		DgCottbusSmallNetworkGenerator netShrinker = new DgCottbusSmallNetworkGenerator();
		netShrinker.createSmallNetwork(networkFile, outputDirectory, signalSystemsFile, offset);
		Network smallNetwork = netShrinker.getShrinkedNetwork();
		NetworkWriter netWriter = new NetworkWriter(smallNetwork);
		netWriter.write(shrinkedNetOutfile);
		return netShrinker;
	}

	
	public double getMatsimPopSampleSize() {
		return matsimPopSampleSize;
	}

	
	public void setMatsimPopSampleSize(double matsimPopSampleSize) {
		this.matsimPopSampleSize = matsimPopSampleSize;
	}

	
	public double getKsModelCommoditySampleSize() {
		return ksModelCommoditySampleSize;
	}

	
	public void setKsModelCommoditySampleSize(double ksModelCommoditySampleSize) {
		this.ksModelCommoditySampleSize = ksModelCommoditySampleSize;
	}
}
