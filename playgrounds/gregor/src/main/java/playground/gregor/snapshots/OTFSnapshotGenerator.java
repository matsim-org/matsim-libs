/* *********************************************************************** *
 * project: org.matsim.*
 * OTFSnapshotGenerator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.gregor.snapshots;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import javax.xml.parsers.ParserConfigurationException;

import org.geotools.data.FeatureSource;
import org.geotools.feature.Feature;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.EvacuationConfigGroup.EvacuationScenario;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.EventsReaderTXTv1;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.evacuation.otfvis.drawer.OTFBackgroundTexturesDrawer;
import org.xml.sax.SAXException;

import playground.gregor.snapshots.postprocessors.ConfluenceArrowsFromEvents;
import playground.gregor.snapshots.postprocessors.DestinationDependentColorizer;
import playground.gregor.snapshots.postprocessors.EvacuationLinksTeleporter;
import playground.gregor.snapshots.postprocessors.SheltersColorizer;
import playground.gregor.snapshots.postprocessors.TimeDependentColorizer;
import playground.gregor.snapshots.writers.LineStringTree;
import playground.gregor.snapshots.writers.MVISnapshotWriter;
import playground.gregor.snapshots.writers.PositionInfo;
import playground.gregor.snapshots.writers.SnapshotGenerator;

public class OTFSnapshotGenerator {

	public static String SHARED_SVN = "../../../../../arbeit/svn/shared-svn/studies";
		public static String RUNS_SVN = "../../../../../arbeit/svn/runs-svn/run1020/output";
//		public static String RUNS_SVN = "/home/laemmel/devel/EAF/output";
//	public static String RUNS_SVN = "../../../matsim/test/output/org/matsim/evacuation/run/ShelterEvacuationControllerTest/testShelterEvacuationController";
	
	public static String MVI_FILE;
	private static boolean firstIteration = true;


	private final String lsFile;

	private final static double VIS_OUTPUT_SAMPLE = 1.;
	private static String LABEL = null;

	private final ScenarioImpl scenario;
	private final String eventsFile;

	private final String txtSnapshotFile = null;
	private double startTime;
	//	private final String txtSnapshotFile = "../../../outputs/output/snapshots.txt.gz";


	public OTFSnapshotGenerator() {

		this.lsFile =  SHARED_SVN + "/countries/id/padang/gis/network_v20080618/d_ls.shp";
		ScenarioLoaderImpl sl = new ScenarioLoaderImpl(RUNS_SVN + "/output_config.xml.gz");

		this.scenario = sl.getScenario();
		this.scenario.getConfig().network().setInputFile(RUNS_SVN + "/output_network.xml.gz");
		this.scenario.getConfig().network().setChangeEventInputFile(RUNS_SVN + "/output_change_events.xml.gz");
		
		this.scenario.getConfig().simulation().setSnapshotFormat("otfvis");
		this.scenario.getConfig().simulation().setSnapshotPeriod(60);
		//		this.scenario.getConfig().simulation().setEndTime(4*3600+30*60);
		if (this.scenario.getConfig().evacuation().getEvacuationScanrio() == EvacuationScenario.night || this.scenario.getConfig().evacuation().getEvacuationScanrio() == EvacuationScenario.from_file) {
			this.startTime = 3 * 3600.;
			this.scenario.getConfig().simulation().setEndTime(5*3600);	
		} else if (this.scenario.getConfig().evacuation().getEvacuationScanrio() == EvacuationScenario.day) {
			this.startTime = 12 * 3600.;
			this.scenario.getConfig().simulation().setEndTime(14*3600);
		} else if (this.scenario.getConfig().evacuation().getEvacuationScanrio() == EvacuationScenario.afternoon) {
			this.startTime = 16 * 3600.;
			this.scenario.getConfig().simulation().setEndTime(18*3600);
		} 
		
//		this.scenario.getConfig().simulation().set
		this.scenario.getConfig().setQSimConfigGroup(new QSimConfigGroup());

		this.scenario.getConfig().evacuation().setBuildingsFile(SHARED_SVN + "/countries/id/padang/gis/buildings_v20100315/evac_zone_buildings_v20100315.shp");
//				this.scenario.getConfig().evacuation().setBuildingsFile("/home/laemmel/devel/workspace/matsim/test/input/org/matsim/evacuation/data/buildings.shp");
				
		//		this.scenario.getConfig().evacuation().setSampleSize("0.1");
		//		this.scenario.getConfig().controler().setLastIteration(0);
		int it = this.scenario.getConfig().controler().getLastIteration();
		if (firstIteration) {
			it = 0;
		}
//		it = 250;
//		MVI_FILE = RUNS_SVN + "/movie.it" + it + ".mvi";
		MVI_FILE = "/home/laemmel/tmp/flooding.mvi";
		sl.loadNetwork();
		this.eventsFile = RUNS_SVN + "/ITERS/it." + it + "/" + it + ".events.txt.gz";

		//		this.txtSnapshotFile = "../../outputs/output/snapshots.txt.gz";
	}

	public void run() {

		PositionInfo.setLANE_WIDTH(this.scenario.getNetwork().getEffectiveLaneWidth());

		EventsManagerImpl ev = new EventsManagerImpl();
		DestinationDependentColorizer d = new DestinationDependentColorizer();
		ev.addHandler(d);
		EvacuationLinksTeleporter e = new EvacuationLinksTeleporter();
		//		AllAgentsTeleporter aat = new AllAgentsTeleporter();
		
		double startTime = 0;
		if (this.scenario.getConfig().evacuation().getEvacuationScanrio() == EvacuationScenario.night) {
			startTime = 3* 3600;
		} else if (this.scenario.getConfig().evacuation().getEvacuationScanrio() == EvacuationScenario.day) {
			startTime = 12 * 3600;
		} else if (this.scenario.getConfig().evacuation().getEvacuationScanrio() == EvacuationScenario.afternoon) {
			startTime = 16 * 3600;
		} 
		TimeDependentColorizer t = new TimeDependentColorizer(startTime);
		ev.addHandler(t);
		
//		EventsManagerImpl evII = new EventsManagerImpl();
//		evII.addHandler(t);
//		new EventsReaderTXTv1(evII).readFile("/home/laemmel/devel/EAF/output/ITERS/it.0/0.events.txt.gz");

		SheltersColorizer s = new SheltersColorizer(this.scenario.getConfig().evacuation().getBuildingsFile(),this.scenario.getConfig().simulation().getSnapshotPeriod(), this.scenario.getConfig().evacuation().getSampleSize());
		ev.addHandler(s);

		OTFBackgroundTexturesDrawer sbg = new OTFBackgroundTexturesDrawer("arrow.png");
		ConfluenceArrowsFromEvents c = new ConfluenceArrowsFromEvents(sbg,this.scenario.getNetwork());

		//		SimpleBackgroundTextureDrawer sbgII = new SimpleBackgroundTextureDrawer("./res/arrow.png");
		//		SimpleBackgroundTextureDrawer sbgIII = new SimpleBackgroundTextureDrawer("./res/blocked.png");
		//		WrongDirectionArrowsFromEvents w = new WrongDirectionArrowsFromEvents(sbgII,sbgIII,this.scenario.getNetwork());

		ev.addHandler(c);
		//		ev.addHandler(w);

		triggerEventsReader(ev);

		ev.removeHandler(d);
		ev.removeHandler(t);
		ev.removeHandler(c);
		//		ev.removeHandler(w);
		//		c.createArrows();
		//		w.createArrows();
		ev.removeHandler(s);


		PositionInfo.lsTree = new LineStringTree(getFeatures(),this.scenario.getNetwork());

		this.scenario.getNetwork().addNode(
				this.scenario.getNetwork().getFactory().createNode(new IdImpl("minXY"), new CoordImpl(643000,9870000)));//HACK to get the bounding box big enough; 
		//otherwise we could get negative openGL coords since we calculating offsetEast, offsetNorth based on this bounding box

		MVISnapshotWriter writer = new MVISnapshotWriter(this.scenario);
		writer.setStartTime(this.startTime);
		writer.setLabel(LABEL);
		writer.addSimpleBackgroundTextureDrawer(sbg);
		writer.setSheltersOccupancyMap(s.getOccMap());
		//		writer.addSimpleBackgroundTextureDrawer(sbgII);
		//		writer.addSimpleBackgroundTextureDrawer(sbgIII);

		SnapshotGenerator sg = new SnapshotGenerator(this.scenario,writer);
		if (this.txtSnapshotFile != null) {
			sg.enableTableWriter(this.txtSnapshotFile);
		}

		sg.setVisOutputSample(VIS_OUTPUT_SAMPLE);
		ev.addHandler(sg);
		sg.addColorizer(d);
		sg.addColorizer(e);
		sg.addColorizer(t);

		//		sg.addColorizer(aat);

		triggerEventsReader(ev);
		sg.finish();
	}

	private void triggerEventsReader(EventsManagerImpl ev) {
		if (this.eventsFile.endsWith("txt.gz")) {
			new EventsReaderTXTv1(ev).readFile(this.eventsFile);
		} else if (this.eventsFile.endsWith("xml.gz")){
			try {
				new EventsReaderXMLv1(ev).parse(this.eventsFile);
			} catch (SAXException e1) {
				e1.printStackTrace();
			} catch (ParserConfigurationException e1) {
				e1.printStackTrace();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		} else {
			throw new RuntimeException("unrecognized file format:" + this.eventsFile);
		}

	}

	private Collection<Feature> getFeatures() {

		FeatureSource fts = null;
		try {
			fts = ShapeFileReader.readDataFile(this.lsFile);
		} catch (final Exception e) {
			// TODO Auto-generated catch block/run1006
			e.printStackTrace();
		}
		final Collection<Feature> fa = new ArrayList<Feature>();
		Iterator it = null;
		try {
			it = fts.getFeatures().iterator();
		} catch (final IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		while (it.hasNext()) {
			fa.add((Feature) it.next());
		}

		return fa;
	}

	public static void main(String [] args) {
		LABEL = "Run 4.1 (SP)";
		if (args.length == 3) {
			String outputDir = args[0];
			String svnRoot = args[1];
			LABEL = args[2];
			RUNS_SVN=outputDir;
			SHARED_SVN = svnRoot;
		}
		new OTFSnapshotGenerator().run();
	}
}

