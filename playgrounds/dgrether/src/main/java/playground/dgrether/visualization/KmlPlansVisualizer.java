/* *********************************************************************** *
 * project: org.matsim.*
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.analysis.filters.population.RouteLinkFilter;
import org.matsim.contrib.analysis.filters.population.SelectedPlanFilter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigReader;
import org.matsim.core.population.algorithms.PlanCollectFromAlgorithm;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.vis.kml.KMZWriter;
import org.matsim.vis.kml.MatsimKMLLogo;

import net.opengis.kml._2.DocumentType;
import net.opengis.kml._2.FolderType;
import net.opengis.kml._2.KmlType;
import net.opengis.kml._2.ObjectFactory;
import net.opengis.kml._2.ScreenOverlayType;
import playground.dgrether.matsimkml.KmlPlansWriter;


/**
 * @author dgrether
 *
 */
public class KmlPlansVisualizer {

	private static final Logger log = Logger.getLogger(KmlPlansVisualizer.class);

	private ObjectFactory kmlObjectFactory = new ObjectFactory();
	
	private KmlType mainKml;

	private DocumentType mainDoc;

	private FolderType mainFolder;

	private KMZWriter writer;

	private List<Tuple<String, String>> linkTuples;

	private MutableScenario scenario;


	public KmlPlansVisualizer(final String config, final List<Tuple<String, String>> linkTuples) {
		Config conf = new Config();
		ConfigReader reader = new ConfigReader(conf);
		reader.readFile(config);
		scenario = (MutableScenario) ScenarioUtils.loadScenario(conf);
		this.linkTuples = linkTuples;
	}

	private void write(final String filename) {
		// init kml
		this.mainKml = this.kmlObjectFactory.createKmlType();
		this.mainDoc = this.kmlObjectFactory.createDocumentType();
		this.mainKml.setAbstractFeatureGroup(this.kmlObjectFactory.createDocument(mainDoc));
		// create a folder
		this.mainFolder = this.kmlObjectFactory.createFolderType();
		this.mainFolder.setName("Matsim Data");
		this.mainDoc.getAbstractFeatureGroup().add(this.kmlObjectFactory.createFolder(this.mainFolder));
		// the writer
		this.writer = new KMZWriter(filename);
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
		try {
			// add the matsim logo to the kml
			ScreenOverlayType logo = MatsimKMLLogo.writeMatsimKMLLogo(writer);
			this.mainFolder.getAbstractFeatureGroup().add(this.kmlObjectFactory.createScreenOverlay(logo));
			KmlPlansWriter plansWriter = new KmlPlansWriter(this.scenario.getNetwork(),
					TransformationFactory.getCoordinateTransformation(this.scenario.getConfig().global().getCoordinateSystem(), TransformationFactory.WGS84), this.writer, this.mainDoc);
			FolderType plansFolder = plansWriter.getPlansFolder(planSet);
			this.mainFolder.getAbstractFeatureGroup().add(this.kmlObjectFactory.createFolder(plansFolder));
		} catch (IOException e) {
			log.error("Cannot create kmz or logo.", e);
		}
		this.writer.writeMainKml(this.mainKml);
		this.writer.close();
		log.info("Plans written to kmz: " + filename);
	}

	private Set<Plan> filterPlans() {
		PlanCollectFromAlgorithm collector = new PlanCollectFromAlgorithm();

		RouteLinkFilter linkFilter = new RouteLinkFilter(collector);
		for (Tuple<String, String> t : this.linkTuples) {
			linkFilter.addLink(Id.create(t.getFirst(), Link.class));
			linkFilter.addLink(Id.create(t.getSecond(), Link.class));
		}

		SelectedPlanFilter selectedPlanFilter = new SelectedPlanFilter(linkFilter);
		selectedPlanFilter.run(this.scenario.getPopulation());
		return collector.getPlans();
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
