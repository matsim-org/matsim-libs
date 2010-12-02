/* *********************************************************************** *
 * project: org.matsim.*
 * FourWaysVisMviTest
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
package playground.dgrether.signalsystems.otfvis;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.CharyparNagelScoringConfigGroup.ActivityParams;
import org.matsim.core.controler.Controler;
import org.matsim.run.OTFVis;

import playground.dgrether.DgPaths;


/**
 * @author dgrether
 *
 */
public class FourWaysVisMviTest {

  public static final String TESTINPUTDIR = "../../matsim/test/input/org/matsim/signalsystems/TravelTimeFourWaysTest/";
  
  /**
   * @param args
   */
  public static void main(String[] args) {

    final int iteration = 0;
    
		String netFile = TESTINPUTDIR + "network.xml.gz";
		String lanesFile  = TESTINPUTDIR + "testLaneDefinitions_v1.1.xml";
		String popFile = TESTINPUTDIR + "plans.xml.gz";
		String signalFile = TESTINPUTDIR + "testSignalSystems_v2.0.xml";
		String signalGroupsFile = TESTINPUTDIR + "testSignalGroups_v2.0.xml";
		String signalControlFile = TESTINPUTDIR + "testSignalControl_v2.0.xml";
    
    ScenarioImpl scenario = new ScenarioImpl();
    Config conf = scenario.getConfig();
    conf.network().setInputFile(netFile);
    conf.plans().setInputFile(popFile);
    ActivityParams a = new ActivityParams("h");
    a.setTypicalDuration(8.0 * 3600.0);
    conf.charyparNagelScoring().addActivityParams(a);
    
    conf.addQSimConfigGroup(new QSimConfigGroup());
    conf.getQSimConfigGroup().setSnapshotFormat("otfvis");
    conf.getQSimConfigGroup().setSnapshotPeriod(15.0);
    conf.getQSimConfigGroup().setSnapshotStyle("queue");
    conf.getQSimConfigGroup().setStuckTime(100.0);
    
    conf.network().setLaneDefinitionsFile(lanesFile);
    conf.scenario().setUseLanes(true);
    
		scenario.getConfig().signalSystems().setSignalSystemFile(signalFile);
		scenario.getConfig().signalSystems().setSignalGroupsFile(signalGroupsFile);
		scenario.getConfig().signalSystems().setSignalControlFile(signalControlFile);
		scenario.getConfig().scenario().setUseSignalSystems(true);
    
    conf.controler().setFirstIteration(0);
    conf.controler().setLastIteration(0);
    conf.controler().setOutputDirectory(DgPaths.OUTDIR + "fourwaysmvitest");
    
    
    Controler controler = new Controler(conf);
    controler.setOverwriteFiles(true);
    controler.run();
    
    //open vis
    String filename = controler.getConfig().controler().getOutputDirectory();
    System.err.println(filename);
    filename += "/ITERS/it."+ Integer.toString(iteration)+"/" + iteration + ".otfvis.mvi";
    System.out.println("filename is: " + filename);
    OTFVis.main(new String[] {filename});

    
  }
}
