/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

package playground.agarwalamit.munich.calibration;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import playground.agarwalamit.analysis.modalShare.ModalShareFromEvents;
import playground.agarwalamit.munich.utils.MunichPersonFilter;
import playground.agarwalamit.utils.FileUtils;

/**
 * Using v3 files, i.e., sub-activities, sub populations and wrapping of sub-activities.
 *
 * The idea is to calibrate ASC for joint PT rather independently.
 *
 *
 * Created by amit on 23/02/2017.
 */


public class MunichJointCalibrationControler {

    private static final String ug = "COMMUTER_REV_COMMUTER";
    private static final String [] availableModes = {"car", "pt", "pt_".concat(ug)};

    public static void main(String[] args) {

        String configFile = FileUtils.SHARED_SVN+"/projects/detailedEval/matsim-input-files/config_1pct_v3.xml";
        String outDir = FileUtils.RUNS_SVN+"/detEval/emissionCongestionInternalization/otherRuns/output/1pct/run11/calibration/trial0/";
        double ascUrban = -0.75;
        double ascCommuters = -0.75;


        if (args.length > 0 ) {
            configFile = args[0];
            outDir = args[1];
            ascUrban = Double.valueOf(args[2]);
            ascCommuters = Double.valueOf(args[3]);
        }

        // calibrating only ASC for urban / commuters

        Scenario sc = ScenarioUtils.loadScenario(ConfigUtils.loadConfig(configFile));

        sc.getConfig().planCalcScore().getModes().get("pt").setConstant(ascUrban);
        sc.getConfig().planCalcScore().getModes().get("pt_"+ug).setConstant(ascUrban);

        sc.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
        sc.getConfig().controler().setOutputDirectory(outDir);

        sc.getConfig().changeMode().setModes(availableModes);

        Controler controler = new Controler(sc);

        // additional things to get the networkRoute for ride mode. For this, ride mode must be assigned in networkModes of the config file.
        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                addTravelTimeBinding("ride").to(networkTravelTime());
                addTravelDisutilityFactoryBinding("ride").to(carTravelDisutilityFactoryKey());
            }
        });

        // allowing car and ride on all links (it is also necessary in order to get network routes for ride mode).
        for (Link l : controler.getScenario().getNetwork().getLinks().values()){
            Set<String> modes = new HashSet<>(Arrays.asList("car","ride"));
            l.setAllowedModes(modes);
        }

        controler.run();

        // delete unnecessary iterations folder here.
        int firstIt = controler.getConfig().controler().getFirstIteration();
        int lastIt = controler.getConfig().controler().getLastIteration();
        String OUTPUT_DIR = controler.getConfig().controler().getOutputDirectory();
        for (int index =firstIt+1; index <lastIt; index ++){
            String dirToDel = OUTPUT_DIR+"/ITERS/it."+index;
            IOUtils.deleteDirectoryRecursively(new File(dirToDel).toPath());
        }

        String outputEventsFile = OUTPUT_DIR+"/output_events.xml.gz";
        if(new File(outputEventsFile).exists()) {
            new File(OUTPUT_DIR + "/analysis/").mkdir();

            {
                String userGroup = MunichPersonFilter.MunichUserGroup.Urban.toString();
                ModalShareFromEvents msc = new ModalShareFromEvents(outputEventsFile, userGroup, new MunichPersonFilter());
                msc.run();
                msc.writeResults(OUTPUT_DIR + "/analysis/modalShareFromEvents_" + userGroup + ".txt");
            }
            {
                String userGroup = MunichPersonFilter.MunichUserGroup.Rev_Commuter.toString();
                ModalShareFromEvents msc = new ModalShareFromEvents(outputEventsFile, userGroup, new MunichPersonFilter());
                msc.run();
                msc.writeResults(OUTPUT_DIR + "/analysis/modalShareFromEvents_" + userGroup + ".txt");
            }
        }
    }
}
