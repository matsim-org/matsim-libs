/* *********************************************************************** *
 * project: org.matsim.*
 * Sim2DOTFSnapshotGenerator.java
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
package playground.gregor.snapshots.writers;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.core.utils.geometry.CoordImpl;

import playground.gregor.sim2d.events.XYZEventsFileReader;
import playground.gregor.sim2d.events.XYZEventsManager;
public class Sim2DOTFSnapshotGenerator {
	public static String SHARED_SVN = "../../../../../arbeit/svn/shared-svn/studies";
//	public static String RUNS_SVN = "../../../../../arbeit/svn/runs-svn/run1020/output";
	public static String RUNS_SVN = "/home/laemmel/devel/sim2d/output";
//public static String RUNS_SVN = "../../../matsim/test/output/org/matsim/evacuation/run/ShelterEvacuationControllerTest/testShelterEvacuationController";

public static String MVI_FILE;
private static boolean firstIteration = true;



private static String LABEL = null;

private final ScenarioImpl scenario;
private final String eventsFile;

private double startTime;
//	private final String txtSnapshotFile = "../../../outputs/output/snapshots.txt.gz";


public Sim2DOTFSnapshotGenerator() {

	ScenarioLoaderImpl sl = new ScenarioLoaderImpl(RUNS_SVN + "/output_config.xml.gz");

	this.scenario = sl.getScenario();
	this.scenario.getConfig().network().setInputFile(RUNS_SVN + "/output_network.xml.gz");
	this.scenario.getConfig().network().setChangeEventInputFile(RUNS_SVN + "/output_change_events.xml.gz");
	
	this.scenario.getConfig().simulation().setSnapshotFormat("otfvis");
	this.scenario.getConfig().simulation().setSnapshotPeriod(1);
	//		this.scenario.getConfig().simulation().setEndTime(4*3600+30*60);
//	if (this.scenario.getConfig().evacuation().getEvacuationScanrio() == EvacuationScenario.night || this.scenario.getConfig().evacuation().getEvacuationScanrio() == EvacuationScenario.from_file) {
//		this.startTime = 3 * 3600.;
//		this.scenario.getConfig().simulation().setEndTime(5*3600);	
//	} else if (this.scenario.getConfig().evacuation().getEvacuationScanrio() == EvacuationScenario.day) {
//		this.startTime = 12 * 3600.;
//		this.scenario.getConfig().simulation().setEndTime(14*3600);
//	} else if (this.scenario.getConfig().evacuation().getEvacuationScanrio() == EvacuationScenario.afternoon) {
//		this.startTime = 16 * 3600.;
//		this.scenario.getConfig().simulation().setEndTime(18*3600);
//	} 
	
	this.startTime = 9 * 3600;
	
//	this.scenario.getConfig().simulation().set
//	this.scenario.getConfig().setQSimConfigGroup(new QSimConfigGroup());
	this.scenario.getConfig().setQSimConfigGroup(new QSimConfigGroup());

//	this.scenario.getConfig().evacuation().setBuildingsFile(SHARED_SVN + "/countries/id/padang/gis/buildings_v20100315/evac_zone_buildings_v20100315.shp");
//			this.scenario.getConfig().evacuation().setBuildingsFile("/home/laemmel/devel/workspace/matsim/test/input/org/matsim/evacuation/data/buildings.shp");
			
	//		this.scenario.getConfig().evacuation().setSampleSize("0.1");
	//		this.scenario.getConfig().controler().setLastIteration(0);
	int it = this.scenario.getConfig().controler().getLastIteration();
	if (firstIteration) {
		it = 0;
	}
//	it = 250;
	MVI_FILE = RUNS_SVN + "/movie.it" + it + ".mvi";
//	MVI_FILE = "/home/laemmel/tmp/flooding.mvi";
	sl.loadNetwork();
	this.eventsFile = RUNS_SVN + "/ITERS/it." + it + "/" + it + ".events.txt.gz";

	//		this.txtSnapshotFile = "../../outputs/output/snapshots.txt.gz";
}

public void run() {

	
//
//	this.scenario.getNetwork().addNode(
//			this.scenario.getNetwork().getFactory().createNode(new IdImpl("minXY"), new CoordImpl(643000,9870000)));//HACK to get the bounding box big enough; 
	//otherwise we could get negative openGL coords since we calculating offsetEast, offsetNorth based on this bounding box

	MVISnapshotWriter writer = new MVISnapshotWriter(this.scenario,MVI_FILE);
	writer.setStartTime(this.startTime);
	writer.setLabel(LABEL);

	XYZAzimuthSnapshotGenerator sg = new XYZAzimuthSnapshotGenerator(writer);
	XYZEventsManager ev = new XYZEventsManager();
	ev.addHandler(sg);
	writer.open();
	new XYZEventsFileReader(ev).readFile("/home/laemmel/devel/sim2d/output/ITERS/it.0/0.xyzAzimuthEvents.xml.gz");
	writer.finish();
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
	new Sim2DOTFSnapshotGenerator().run();
}

}
