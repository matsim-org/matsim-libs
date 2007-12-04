/* *********************************************************************** *
 * project: org.matsim.*
 * KmlNetworkWriter.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
package playground.dgrether.visualization;

import java.io.IOException;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.matsim.basic.v01.Id;
import org.matsim.gbl.Gbl;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.network.NetworkLayerBuilder;
import org.matsim.plans.MatsimPlansReader;
import org.matsim.plans.Plan;
import org.matsim.plans.Plans;
import org.matsim.plans.PlansReaderI;
import org.matsim.plans.algorithms.PlanCollectFromAlgorithm;
import org.matsim.plans.filters.RouteLinkFilter;
import org.matsim.plans.filters.SelectedPlanFilter;
import org.matsim.utils.collections.Tuple;
import org.matsim.utils.geometry.transformations.TransformationFactory;
import org.matsim.utils.vis.kml.Document;
import org.matsim.utils.vis.kml.Folder;
import org.matsim.utils.vis.kml.KML;
import org.matsim.utils.vis.kml.KMLWriter;
import org.matsim.utils.vis.kml.KMZWriter;
import org.matsim.utils.vis.matsimkml.MatsimKMLLogo;
import org.matsim.world.MatsimWorldReader;

import playground.dgrether.matsimkml.KmlPlansWriter;


/**
 * @author dgrether
 *
 */
public class KmlPlansVisualizer {

	private static final Logger log = Logger.getLogger(KmlPlansVisualizer.class);

	private NetworkLayer networkLayer;

	private KML mainKml;

	private Document mainDoc;

	private Folder mainFolder;

	private KMZWriter writer;

	private Plans plans;

	private List<Tuple<String, String>> linkTuples;

	public KmlPlansVisualizer(final String config, final List<Tuple<String, String>> linkTuples) {
		Gbl.createConfig(new String[] {config});
		this.networkLayer = loadNetwork(Gbl.getConfig().network().getInputFile());
		this.loadWorld();
		this.linkTuples = linkTuples;
	}

	private void write(final String filename) {
		// init kml
		this.mainKml = new KML();
		this.mainDoc = new Document(filename);
		this.mainKml.setFeature(this.mainDoc);
		// create a folder
		this.mainFolder = new Folder("matsimdatafolder");
		this.mainFolder.setName("Matsim Data");
		this.mainDoc.addFeature(this.mainFolder);
		// the writer
		this.writer = new KMZWriter(filename, KMLWriter.DEFAULT_XMLNS);
		this.plans = loadPopulation();
		Set<Plan> planSetBig = filterPlans();
		log.info("Found " + planSetBig.size() + " relevant plans");
		int i = 0;
		int max = 50;
		Set<Plan> planSet = new HashSet<Plan>(max);
		for (Plan p : planSetBig) {
			planSet.add(p);
			i++;
			if (i > max)
				break;
		}
		this.plans = null;
		try {
			// add the matsim logo to the kml
			MatsimKMLLogo logo = new MatsimKMLLogo(this.writer);
			this.mainFolder.addFeature(logo);
			KmlPlansWriter plansWriter = new KmlPlansWriter(this.networkLayer,
					TransformationFactory.getCoordinateTransformation(Gbl.getConfig().global().getCoordinateSystem(), TransformationFactory.WGS84), this.writer, this.mainDoc);
			Folder plansFolder = plansWriter.getPlansFolder(planSet);
			this.mainFolder.addFeature(plansFolder);
		} catch (IOException e) {
			Gbl.errorMsg("Cannot create kmz or logo cause: " + e.getMessage());
			e.printStackTrace();
		}
		this.writer.writeMainKml(this.mainKml);
		this.writer.close();
		log.info("Plans written to kmz: " + filename);
	}

	private Set<Plan> filterPlans() {
		PlanCollectFromAlgorithm collector = new PlanCollectFromAlgorithm();

		RouteLinkFilter linkFilter = new RouteLinkFilter(collector);
		for (Tuple<String, String> t : this.linkTuples) {
			linkFilter.addLink(new Id(t.getFirst()));
			linkFilter.addLink(new Id(t.getSecond()));
		}

		SelectedPlanFilter selectedPlanFilter = new SelectedPlanFilter(linkFilter);
		selectedPlanFilter.run(this.plans);
		return collector.getPlans();
	}

	protected Plans loadPopulation() {
		Plans population = new Plans(Plans.NO_STREAMING);
		printNote("", "  reading plans xml file... ");
		PlansReaderI plansReader = new MatsimPlansReader(population);
		plansReader.readFile(Gbl.getConfig().plans().getInputFile());
		population.printPlansCount();
		printNote("", "  done");

		return population;
	}

	/**
	 * load the network
	 *
	 * @return the network layer
	 */
	protected NetworkLayer loadNetwork(final String networkFile) {
		// - read network: which buildertype??
		printNote("", "  creating network layer... ");
		NetworkLayerBuilder
				.setNetworkLayerType(NetworkLayerBuilder.NETWORK_SIMULATION);
		NetworkLayer network = (NetworkLayer) Gbl.getWorld().createLayer(
				NetworkLayer.LAYER_TYPE, null);
		printNote("", "  done");

		printNote("", "  reading network xml file... ");
		new MatsimNetworkReader(network).readFile(networkFile);
		printNote("", "  done");

		return network;
	}

	protected void loadWorld() {
		if (Gbl.getConfig().world().getInputFile() != null) {
			printNote("", "  reading world xml file... ");
			final MatsimWorldReader worldReader = new MatsimWorldReader(Gbl.getWorld());
			worldReader.readFile(Gbl.getConfig().world().getInputFile());
			printNote("", "  done");
		} else {
			printNote("","  No World input file given in config.xml!");
		}
	}


	/**
	 * an internal routine to generated some (nicely?) formatted output. This
	 * helps that status output looks about the same every time output is written.
	 *
	 * @param header
	 *          the header to print, e.g. a module-name or similar. If empty
	 *          <code>""</code>, no header will be printed at all
	 * @param action
	 *          the status message, will be printed together with a timestamp
	 */
	private final void printNote(final String header, final String action) {
		if (header != "") {
			System.out.println();
			System.out
					.println("===============================================================");
			System.out.println("== " + header);
			System.out
					.println("===============================================================");
		}
		if (action != "") {
			System.out.println("== " + action + " at " + (new Date()));
		}
		if (header != "") {
			System.out.println();
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// config with equil net
//		args = new String[] {"./input/plansVisConfig.xml", "2", "20"};
//		config with wip net
				args = new String[] {"./input/plansVisConfig.xml", "7834", "8372"};
		if (args.length < 3) {
			printHelp();
		}
		else if (args.length % 2 == 0) {
			printHelp();
		}
		else {
			List<Tuple<String, String>> tuples = new Vector<Tuple<String, String>>();
			for (int i = 1; i < args.length; i = i + 2) {
				tuples.add(new Tuple<String, String>(args[i], args[i+1]));
			}
			new KmlPlansVisualizer(args[0], tuples).write("./output/plans.kmz");
		}
	}

	public static void printHelp() {
		System.out
				.println("This tool has to be started with the following parameters:");
		System.out.println("  1. a config containing at least a network and population file");
		System.out.println("  2. and 3. til n and n+1: a tuple of two link ids to filter the plans. each plan displayed crosses one tuple of links");

	}
}
