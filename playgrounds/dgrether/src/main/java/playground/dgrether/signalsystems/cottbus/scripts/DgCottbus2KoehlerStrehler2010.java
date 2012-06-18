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
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.network.NetworkWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.OutputDirectoryLogging;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.Time;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import playground.dgrether.DgPaths;
import playground.dgrether.koehlerstrehlersignal.DgKoehlerStrehler2010ModelWriter;
import playground.dgrether.koehlerstrehlersignal.DgMatsim2KoehlerStrehler2010DemandConverter;
import playground.dgrether.koehlerstrehlersignal.DgMatsim2KoehlerStrehler2010NetworkConverter;
import playground.dgrether.koehlerstrehlersignal.DgMatsim2KoehlerStrehler2010Zones2Commodities;
import playground.dgrether.koehlerstrehlersignal.data.DgCommodities;
import playground.dgrether.koehlerstrehlersignal.data.DgCrossing;
import playground.dgrether.koehlerstrehlersignal.data.DgKSNetwork;
import playground.dgrether.signalsystems.cottbus.DgCottbusScenarioPaths;
import playground.dgrether.utils.DgGrid;
import playground.dgrether.utils.DgGridUtils;
import playground.dgrether.utils.zones.DgMatsimPopulation2Zones;
import playground.dgrether.utils.zones.DgZone;
import playground.dgrether.utils.zones.DgZonesUtils;

import com.vividsolutions.jts.geom.Envelope;

/**
 * @author dgrether
 */
public class DgCottbus2KoehlerStrehler2010 {
	
	public static final Logger log = Logger.getLogger(DgCottbus2KoehlerStrehler2010.class);
	public static final String outputDirectory = DgPaths.REPOS + "shared-svn/studies/dgrether/cottbus/cottbus_feb_fix/network_small/";
	private static String smallNet = outputDirectory + "network_small.xml.gz";
	private static final String populationFile = DgPaths.REPOS + "runs-svn/run1292/1292.output_plans.xml.gz";
//	private static final String populationFile = DgPaths.REPOS + "runs-svn/run1292/1292.output_plans_sample.xml";
	private static String modelOutfile = outputDirectory + "koehler_strehler_model.xml";
	
	private static String netFile = "/media/data/work/repos/shared-svn/studies/dgrether/cottbus/cottbus_feb_fix/network_wgs84_utm33n.xml.gz";
	private static String signalSystems = DgPaths.REPOS +  "shared-svn/studies/dgrether/cottbus/cottbus_feb_fix/signal_systems.xml";

	private static String name = "run1292";
	private static String description = "run 1292 output plans between 05:30 and 09:30";
	
	private static int cellsX = 5;
	private static int cellsY = 5;
	private static double boundingBoxOffset = 250.0;
	private static double startTime = 5.5 * 3600.0;
	private static double endTime = 9.5 * 3600.0;
	
	public static void main(String[] args) throws IOException {
		OutputDirectoryLogging.initLoggingWithOutputDirectory(outputDirectory);
		/*TODO add time support in respect to:
		 * network capacity creation: document
		*/
		DgCottbusSmallNetworkGenerator netShrinker = shrinkAndWriteNetwork(netFile, signalSystems, smallNet, boundingBoxOffset);
		DgGrid grid = createAndWriteGrid(netShrinker.getBoundingBox(), netShrinker.getCrs(), cellsX, cellsY,  outputDirectory + "grid.shp");
		
		//create some zones and map demand on the zones
		Scenario scenario = loadScenario(DgCottbusScenarioPaths.NETWORK_FILENAME, populationFile);
		List<DgZone> cells = DgZonesUtils.createZonesFromGrid(grid);
		cells = new DgMatsimPopulation2Zones().convert2Zones(scenario, cells, netShrinker.getBoundingBox(), startTime, endTime);
		DgZonesUtils.writePolygonZones2Shapefile(cells, netShrinker.getCrs(), outputDirectory + "grid_cells.shp");
		DgZonesUtils.writeLineStringOdPairsFromZones2Shapefile(cells, netShrinker.getCrs(), outputDirectory + "grid_od_pairs.shp");
		Map<DgZone, Link> zones2LinkMap = DgZonesUtils.createZoneCenter2LinkMapping(cells, (NetworkImpl) netShrinker.getShrinkedNetwork());


		//create koehler strehler network
		Scenario sc = loadSmallNetworkLanesSignals();
		DgKSNetwork ksNet = new DgMatsim2KoehlerStrehler2010NetworkConverter().convertNetworkLanesAndSignals(sc, startTime, endTime);

		DgMatsim2KoehlerStrehler2010DemandConverter demandConverter = new DgMatsim2KoehlerStrehler2010Zones2Commodities(zones2LinkMap);
		DgCommodities commodities = demandConverter.convert(sc, ksNet);
		
		new DgKoehlerStrehler2010ModelWriter().write(ksNet, commodities, name, description, modelOutfile);
		writeStats(ksNet, commodities);
		OutputDirectoryLogging.closeOutputDirLogging();
	}
	
	public static void writeStats(DgKSNetwork ksNet, DgCommodities commodities){
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
	
	public static Scenario loadSmallNetworkLanesSignals(){
		Config c2 = ConfigUtils.createConfig();
		c2.scenario().setUseLanes(true);
		c2.scenario().setUseSignalSystems(true);
		c2.network().setInputFile(smallNet);
		c2.network().setLaneDefinitionsFile(DgCottbusScenarioPaths.LANES_FILENAME);
		c2.signalSystems().setSignalSystemFile(DgCottbusScenarioPaths.SIGNALS_FILENAME);
		c2.signalSystems().setSignalGroupsFile(DgCottbusScenarioPaths.SIGNAL_GROUPS_FILENAME);
		c2.signalSystems().setSignalControlFile(DgCottbusScenarioPaths.SIGNAL_CONTROL_FIXEDTIME_FILENAME);
		ScenarioImpl sc = (ScenarioImpl) ScenarioUtils.loadScenario(c2);
		return sc;
	}

	
	
	public static Scenario loadScenario(String net, String pop){
		Config config = ConfigUtils.createConfig();
		config.network().setInputFile(DgCottbusScenarioPaths.NETWORK_FILENAME);
		config.plans().setInputFile(populationFile);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		return scenario;
	}
	
	
	public static DgGrid createAndWriteGrid(Envelope boundingBox, CoordinateReferenceSystem crs, int xCells, int yCells, String outputfilename){
		Envelope gridBoundingBox = new Envelope(boundingBox);
		//expand the grid size to avoid rounding errors 
		gridBoundingBox.expandBy(0.1);
		DgGrid grid = new DgGrid(xCells, yCells, gridBoundingBox);
		DgGridUtils.writeGrid2Shapefile(grid, crs, outputfilename);
//		
		return grid;
	}
	
	
	public static DgCottbusSmallNetworkGenerator shrinkAndWriteNetwork(String networkFile, String signalSystemsFile, String shrinkedNetOutfile, double offset){
		DgCottbusSmallNetworkGenerator netShrinker = new DgCottbusSmallNetworkGenerator();
		netShrinker.createSmallNetwork(networkFile, signalSystemsFile, offset);
		Network smallNetwork = netShrinker.getShrinkedNetwork();
		NetworkWriter netWriter = new NetworkWriter(smallNetwork);
		netWriter.write(shrinkedNetOutfile);
		return netShrinker;
	}



}
