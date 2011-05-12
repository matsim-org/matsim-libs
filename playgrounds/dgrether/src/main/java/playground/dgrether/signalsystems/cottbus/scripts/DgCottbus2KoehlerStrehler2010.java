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

import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.network.NetworkWriter;
import org.matsim.core.config.Config;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;

import playground.dgrether.DgPaths;
import playground.dgrether.koehlerstrehlersignal.DgKoehlerStrehler2010ModelWriter;
import playground.dgrether.koehlerstrehlersignal.DgMatsim2KoehlerStrehler2010DemandConverter;
import playground.dgrether.koehlerstrehlersignal.DgMatsim2KoehlerStrehler2010NetworkConverter;
import playground.dgrether.koehlerstrehlersignal.DgMatsim2KoehlerStrehler2010Zones2Commodities;
import playground.dgrether.koehlerstrehlersignal.data.DgCommodities;
import playground.dgrether.koehlerstrehlersignal.data.DgNetwork;
import playground.dgrether.signalsystems.cottbus.DgCottbusScenarioPaths;
import playground.dgrether.utils.DgGrid;
import playground.dgrether.utils.DgGridUtils;
import playground.dgrether.utils.DgMatsimPopulation2Zones;
import playground.dgrether.utils.DgZone;
import playground.dgrether.utils.DgZonesUtils;

import com.vividsolutions.jts.geom.Envelope;

/**
 * @author dgrether
 */
public class DgCottbus2KoehlerStrehler2010 {
	
	public static final Logger log = Logger.getLogger(DgCottbus2KoehlerStrehler2010.class);
	
	private static String smallNet = DgPaths.REPOS + "shared-svn/studies/dgrether/cottbus/cottbus_feb_fix/network_small/network_small.xml.gz";
//	private static final String populationFile = DgPaths.REPOS + "runs-svn/run1223/ITERS/it.1000/1223.1000.plans.xml.gz";
	private static final String populationFile = DgPaths.REPOS + "runs-svn/run1223/1223.output_plans_sample.xml";
	private static String modelOutfile = DgPaths.REPOS + "shared-svn/studies/dgrether/cottbus/cottbus_feb_fix/network_small/koehler_strehler_model.xml";
	
	public static void main(String[] args) {
		int cellsX = 10;
		int cellsY = 10;

		DgCottbusSmallNetworkGenerator netShrinker = new DgCottbusSmallNetworkGenerator();
		Network smallNetwork = netShrinker.createSmallNetwork();
		NetworkWriter netWriter = new NetworkWriter(smallNetwork);
		netWriter.write(smallNet);
		Envelope netBoundingBox = netShrinker.getBoundingBox();
		Envelope gridBoundingBox = new Envelope(netBoundingBox);
		//expand the grid size to avoid rounding errors 
		gridBoundingBox.expandBy(0.1);
		DgGrid grid = new DgGrid(cellsX, cellsY, gridBoundingBox);
		DgGridUtils.writeGrid2Shapefile(grid, netShrinker.getCrs(), DgPaths.REPOS + "shared-svn/studies/dgrether/cottbus/cottbus_feb_fix/network_small/grid.shp");

		Config config = ConfigUtils.createConfig();
		config.network().setInputFile(DgCottbusScenarioPaths.NETWORK_FILENAME);
		config.plans().setInputFile(populationFile);
		Scenario scenario = ScenarioUtils.loadScenario(config);

		List<DgZone> cells = DgZonesUtils.createZonesFromGrid(grid);
		DgMatsimPopulation2Zones scenarioConverter = new DgMatsimPopulation2Zones();
		cells = scenarioConverter.convert2Zones(scenario, cells, netBoundingBox);

		DgZonesUtils.writePolygonZones2Shapefile(cells, netShrinker.getCrs(), DgPaths.REPOS + "shared-svn/studies/dgrether/cottbus/cottbus_feb_fix/network_small/grid_cells.shp");
		//next steps:
		Map<DgZone, Link> zones2LinkMap = DgZonesUtils.createZoneCenter2LinkMapping(cells, (NetworkImpl) smallNetwork);
		
		Config c2 = ConfigUtils.createConfig();
		c2.scenario().setUseLanes(true);
		c2.scenario().setUseSignalSystems(true);
		c2.network().setInputFile(smallNet);
		c2.network().setLaneDefinitionsFile(DgCottbusScenarioPaths.LANES_FILENAME);
		c2.signalSystems().setSignalSystemFile(DgCottbusScenarioPaths.SIGNALS_FILENAME);
		c2.signalSystems().setSignalGroupsFile(DgCottbusScenarioPaths.SIGNAL_GROUPS_FILENAME);
		c2.signalSystems().setSignalControlFile(DgCottbusScenarioPaths.SIGNAL_CONTROL_FIXEDTIME_FILENAME);
		ScenarioImpl sc = (ScenarioImpl) ScenarioUtils.loadScenario(c2);
	//create koehler strehler network
		DgMatsim2KoehlerStrehler2010NetworkConverter netConverter = new DgMatsim2KoehlerStrehler2010NetworkConverter();
		DgNetwork dgNet = netConverter.convertNetworkLanesAndSignals(sc);
		DgMatsim2KoehlerStrehler2010DemandConverter demandConverter = new DgMatsim2KoehlerStrehler2010Zones2Commodities(zones2LinkMap);
		DgCommodities commodities = demandConverter.convert(sc, dgNet);
		new DgKoehlerStrehler2010ModelWriter().write(sc, dgNet, commodities, modelOutfile);
	}



}
