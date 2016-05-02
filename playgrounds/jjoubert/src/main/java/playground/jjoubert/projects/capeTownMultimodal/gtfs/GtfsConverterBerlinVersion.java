/* *********************************************************************** *
 * project: org.matsim.*
 * GtfsConverterBerlinVersion.java
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

/**
 * 
 */
package playground.jjoubert.projects.capeTownMultimodal.gtfs;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.gtfs.GtfsConverter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup.SnapshotStyle;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.pt.utils.CreatePseudoNetwork;
import org.matsim.vehicles.VehicleWriterV1;
import org.matsim.vis.otfvis.OTFVisConfigGroup;
import org.matsim.vis.otfvis.OTFVisConfigGroup.ColoringScheme;

import com.conveyal.gtfs.GTFSFeed;

import playground.mrieser.pt.utils.MergeNetworks;
import playground.southafrica.utilities.Header;

/**
 *
 * @author jwjoubert
 */
public class GtfsConverterBerlinVersion {
	final private static Logger LOG = Logger.getLogger(GtfsConverterBerlinVersion.class);
	final private static String CRS_CT = "WGS84_SA_Albers";

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(GtfsConverterBerlinVersion.class.toString(), args);
		String gtfsZipFile = args[0];
		String transitFolder = args[1];
		transitFolder += transitFolder.endsWith("/") ? "" : "/";
		String inputNetworkFile = args[2];
		String outputNetworkFile = args[3];
		
		GtfsConverterBerlinVersion.convert(gtfsZipFile, transitFolder);
		GtfsConverterBerlinVersion.mergeNetworks(inputNetworkFile, transitFolder, outputNetworkFile);
		
		Header.printFooter();
	}
	
	
	private static void mergeNetworks(String inputNetwork, String transitFolder, String outputNetwork){
		LOG.trace("Merging two network:");
		LOG.trace("    base: " + inputNetwork);
		LOG.trace("  myCiTi: " + transitFolder + "transitNetwork.xml.gz");
		Network baseNetwork = NetworkUtils.createNetwork();
		new MatsimNetworkReader(baseNetwork ).parse(inputNetwork);
		String prefix = "";
		Network myCiTiNetwork = NetworkUtils.createNetwork();
		new MatsimNetworkReader(myCiTiNetwork).parse(transitFolder + "transitNetwork.xml.gz");
		
		MergeNetworks.merge(baseNetwork, prefix, myCiTiNetwork);
		new NetworkWriter(baseNetwork).write(outputNetwork);
		LOG.trace("Done merging networks.");
	}
	
	
	private static void convert(String gtfsFile, String outputFolder){
		Scenario sc = parseScenario(gtfsFile);

		/* Convert the GTFS feed. */
		System.out.println("Scenario has " + sc.getNetwork().getLinks().size() + " links.");
		sc.getConfig().controler().setMobsim("qsim");
		sc.getConfig().qsim().setSnapshotStyle( SnapshotStyle.queue );
		sc.getConfig().qsim().setSnapshotPeriod(1);
		sc.getConfig().qsim().setRemoveStuckVehicles(false);
		ConfigUtils.addOrGetModule(sc.getConfig(), OTFVisConfigGroup.GROUP_NAME, OTFVisConfigGroup.class).setColoringScheme(ColoringScheme.gtfs);
		ConfigUtils.addOrGetModule(sc.getConfig(), OTFVisConfigGroup.GROUP_NAME, OTFVisConfigGroup.class).setDrawTransitFacilities(false);
		sc.getConfig().transitRouter().setMaxBeelineWalkConnectionDistance(1.0);
		
		/* Build a pseudo-network. */
		CreatePseudoNetwork cpn = new CreatePseudoNetwork(
				sc.getTransitSchedule(),
				sc.getNetwork(),
				"MyCiTi_");
		cpn.createNetwork();
		
		/* Set up the transit vehicles. */
//		CreateVehiclesForSchedule cvs = new CreateVehiclesForSchedule(sc.getTransitSchedule(), sc.getTransitVehicles());
//		cvs.run();
		
		new NetworkWriter(sc.getNetwork()).write(outputFolder + "transitNetwork.xml.gz");
		new TransitScheduleWriter(sc.getTransitSchedule()).writeFile(outputFolder + "transitSchedule.xml.gz");
		new VehicleWriterV1(((MutableScenario)sc).getTransitVehicles()).writeFile(outputFolder + "transitVehicles.xml.gz");
	}

	
	private static Scenario parseScenario(String gtfsFile){
		Config config = ConfigUtils.createConfig();
		config.global().setCoordinateSystem(CRS_CT);
		config.controler().setLastIteration(0);
		config.transit().setUseTransit(true);
		
		Scenario sc = ScenarioUtils.createScenario(config);
		
		GTFSFeed feed = GTFSFeed.fromFile(gtfsFile);
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation("WGS84", CRS_CT);
		GtfsConverter gtfs = new GtfsConverter(feed, sc, ct);
		gtfs.convert();
		LOG.info("Number of transit lines: " + sc.getTransitSchedule().getTransitLines().size());
		
		return sc;
	}
}
