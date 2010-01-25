/* *********************************************************************** *
 * project: org.matsim.*
 * DgSatellicMviGenerator
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
package playground.dgrether.tests.satellic;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.api.experimental.ScenarioLoader;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.run.OTFVis;

import playground.dgrether.DgPaths;


/**
 * @author dgrether
 *
 */
public class DgSatellicMviGenerator {
  
  public static void main(String[] args){
    
    Scenario sc = new ScenarioImpl();
    Config c = sc.getConfig();
    c.network().setInputFile(DgSatellicData.NETWORK);
    c.plans().setInputFile(DgSatellicData.EMPTY_POPULATION);
    c.controler().setLastIteration(0);
    c.setQSimConfigGroup(new QSimConfigGroup());
    c.getQSimConfigGroup().setSnapshotFormat("otfvis");
    c.getQSimConfigGroup().setSnapshotPeriod(60.0);
    
    c.controler().setOutputDirectory(DgPaths.OUTDIR + "satellic");
    
    ScenarioLoader scl = new ScenarioLoaderImpl(sc);
    scl.loadScenario();
    
    Controler controler = new Controler((ScenarioImpl)sc);
    controler.setOverwriteFiles(true);
    controler.run();
    
    String[] a = {controler.getControlerIO().getIterationFilename(0, "otfvis.mvi")};
    
    OTFVis.playMVI(a);
  }

}
