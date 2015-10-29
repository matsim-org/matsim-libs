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

import java.util.Arrays;

import org.matsim.contrib.otfvis.OTFVis;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.QSimConfigGroup.SnapshotStyle;
import org.matsim.contrib.signals.SignalSystemsConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;

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
    
    MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
    Config conf = scenario.getConfig();
    conf.network().setInputFile(netFile);
    conf.plans().setInputFile(popFile);
    ActivityParams a = new ActivityParams("h");
    a.setTypicalDuration(8.0 * 3600.0);
    conf.planCalcScore().addActivityParams(a);
    
    conf.controler().setSnapshotFormat(Arrays.asList("otfvis"));
    conf.qsim().setSnapshotPeriod(15.0);
    conf.qsim().setSnapshotStyle( SnapshotStyle.queue ) ;;
    conf.qsim().setStuckTime(100.0);
    
    conf.network().setLaneDefinitionsFile(lanesFile);
    conf.qsim().setUseLanes(true);

    ConfigUtils.addOrGetModule(scenario.getConfig(), SignalSystemsConfigGroup.GROUPNAME, SignalSystemsConfigGroup.class).setSignalSystemFile(signalFile);
    ConfigUtils.addOrGetModule(scenario.getConfig(), SignalSystemsConfigGroup.GROUPNAME, SignalSystemsConfigGroup.class).setSignalGroupsFile(signalGroupsFile);
    ConfigUtils.addOrGetModule(scenario.getConfig(), SignalSystemsConfigGroup.GROUPNAME, SignalSystemsConfigGroup.class).setSignalControlFile(signalControlFile);
    ConfigUtils.addOrGetModule(scenario.getConfig(), SignalSystemsConfigGroup.GROUPNAME, SignalSystemsConfigGroup.class).setUseSignalSystems(true);

    conf.controler().setFirstIteration(0);
    conf.controler().setLastIteration(0);
    conf.controler().setOutputDirectory(DgPaths.OUTDIR + "fourwaysmvitest");
    
    
    Controler controler = new Controler(conf);
    controler.getConfig().controler().setOverwriteFileSetting(
			true ?
					OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles :
					OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists );
    controler.run();
    
    //open vis
    String filename = controler.getConfig().controler().getOutputDirectory();
    System.err.println(filename);
    filename += "/ITERS/it."+ Integer.toString(iteration)+"/" + iteration + ".otfvis.mvi";
    System.out.println("filename is: " + filename);
    OTFVis.main(new String[] {filename});

    
  }
}
