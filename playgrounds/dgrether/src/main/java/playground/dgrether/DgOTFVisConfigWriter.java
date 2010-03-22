/* *********************************************************************** *
 * project: org.matsim.*
 * DgOTFVisConfigWriter
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
package playground.dgrether;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.matsim.core.config.Config;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.ShutdownListener;


/**
 * @author dgrether
 *
 */
public class DgOTFVisConfigWriter implements ShutdownListener {
  
  private static final Logger log = Logger.getLogger(DgOTFVisConfigWriter.class);
  
  public static final String OTFVIS_LAST_ITERATION_CONFIG = "otfvis_last_iteration_properties.xml";

  public static final String NETWORK_PROPERTY = "relativeNetworkFile";
  
  public static final String POPULATION_PROPERTY = "relativePopulationFile";
  
  @Override
  public void notifyShutdown(ShutdownEvent event) {
    Config originalConfig = event.getControler().getConfig();
    String currentDir = new File("tmp").getAbsolutePath();
    currentDir = currentDir.substring(0, currentDir.length() - 3);
    String relativeOutputDir = originalConfig.controler().getOutputDirectory();
    log.info("Current directory: " + currentDir);
    log.info("Output directory: "+ relativeOutputDir);
    
    Properties props = new Properties();
    props.put(NETWORK_PROPERTY, "output_network.xml.gz");
    props.put(POPULATION_PROPERTY, "output_plans.xml.gz");
    FileOutputStream outstream;
    try {
      outstream = new FileOutputStream(event.getControler().getControlerIO().getOutputFilename(OTFVIS_LAST_ITERATION_CONFIG));
      props.storeToXML(outstream, "");
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
    
  }

}
