package playground.dgrether;
import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.MatsimConfigReader;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.ControlerIO;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.vis.otfvis.OTFVisQSim;


/* *********************************************************************** *
 * project: org.matsim.*
 * DgOTFVisReplayLastIteration
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

/**
 * @author dgrether
 *
 */
public class DgOTFVisReplayLastIteration {

  private static final Logger log = Logger.getLogger(DgOTFVisReplayLastIteration.class);


  private void playOutputConfig(String configfile) {
    Config config = new Config();
    config.addCoreModules();
    MatsimConfigReader configReader = new MatsimConfigReader(config);
    configReader.readFile(configfile);
    String currentDirectory = configfile.substring(0, configfile.lastIndexOf("/") + 1);
    if (currentDirectory == null ) {
      currentDirectory = configfile.substring(0, configfile.lastIndexOf("\\") + 1);
    }
    log.info("using " + currentDirectory + " as base directory...");
    config.network().setInputFile(currentDirectory + Controler.FILENAME_NETWORK);
    config.plans().setInputFile(currentDirectory + Controler.FILENAME_POPULATION);
    if (config.scenario().isUseLanes()){
      config.network().setLaneDefinitionsFile(currentDirectory + Controler.FILENAME_LANES);
    }
    if (config.scenario().isUseSignalSystems()){
      config.signalSystems().setSignalSystemFile(currentDirectory + Controler.FILENAME_SIGNALSYSTEMS);
      config.signalSystems().setSignalSystemConfigFile(currentDirectory + Controler.FILENAME_SIGNALSYSTEMS_CONFIG);
    }
    log.info("Complete config dump:");
    StringWriter writer = new StringWriter();
    new ConfigWriter(config).writeStream(new PrintWriter(writer));
    log.info("\n\n" + writer.getBuffer().toString());
    log.info("Complete config dump done.");

    //TODO this is a bit risky if other capacity factors have been used in QueueSimulation
    if (config.getQSimConfigGroup() == null) {
      config.setQSimConfigGroup(new QSimConfigGroup());
    }
    
    ScenarioLoaderImpl loader = new ScenarioLoaderImpl(config);
    Scenario sc = loader.loadScenario();
    EventsManagerImpl events = new EventsManagerImpl();
    ControlerIO controlerIO = new ControlerIO(sc.getConfig().controler().getOutputDirectory());
    OTFVisQSim queueSimulation = new OTFVisQSim(sc, events);
    queueSimulation.setControlerIO(controlerIO);
    queueSimulation.setIterationNumber(sc.getConfig().controler().getLastIteration());
    queueSimulation.run();
  }



  public static final String chooseFile() {
    JFileChooser fc = new JFileChooser();

    fc.setFileFilter( new FileFilter() { 
      @Override public boolean accept( File f ) { 
        return f.isDirectory() || f.getName().toLowerCase().endsWith( ".xml" ); 
      }
      @Override public String getDescription() { return "MATSim config file (*.xml)"; } 
    } ); 

    fc.setFileFilter( new FileFilter() { 
      @Override public boolean accept( File f ) { 
        return f.isDirectory() || f.getName().toLowerCase().endsWith( ".xml.gz" ); 
      }
      @Override public String getDescription() { return "MATSim zipped config file (*.xml.gz)"; } 
    } ); 

    
    int state = fc.showOpenDialog( null ); 
    if ( state == JFileChooser.APPROVE_OPTION ) { 
      String args_new = fc.getSelectedFile().getAbsolutePath();
      return args_new;
    }
    System.out.println( "No file selected." );
    return null;
  }

  /**
   * @param args
   */
  public static void main(String[] args) {
//    args = new String[1];
//    args[0] = "/home/dgrether/data/work/matsimOutput/equil/output_config.xml.gz";
    String configfile = null;
    if (args.length == 0){
      configfile = chooseFile();
    }
    else if (args.length == 1){
      configfile = args[0];
    }
    else {
      log.error("not the correct arguments");
    }
    if (configfile != null) {
      new DgOTFVisReplayLastIteration().playOutputConfig(configfile);
    }
  }

}
