/* *********************************************************************** *
 * project: org.matsim.*
 * DenverControllerStarter
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

import org.apache.log4j.Logger;
import org.matsim.core.api.experimental.events.AgentStuckEvent;
import org.matsim.core.api.experimental.events.handler.AgentStuckEventHandler;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;

import playground.dgrether.DgPaths;


/**
 * @author dgrether
 *
 */
public class DenverControllerStarter implements AgentStuckEventHandler {

  private static final Logger log = Logger
      .getLogger(DenverControllerStarter.class);
  
  private String configFile;
  public DenverControllerStarter(String configFile) {
    this.configFile = configFile;
  }

  private void runDenver() {
    Controler c = new Controler(this.configFile);
    c.setOverwriteFiles(true);
  
    this.addListener(c);
    
    c.run();
    
    
    
  }
  
  @Override
  public void handleEvent(AgentStuckEvent event) {
    log.error("got stuck event for agent: " + event.getPersonId() + " on Link " + event.getLinkId());
  }

  @Override
  public void reset(int iteration) {
    
  }
  
  
  private void addListener(Controler c) {
    c.addControlerListener(new StartupListener() {
      @Override
      public void notifyStartup(StartupEvent event) {
        
        //enable live-visualization
//        event.getControler().setMobsimFactory(new OTFVisMobsimFactoryImpl());
        
        //output of stucked vehicles
        event.getControler().getEvents().addHandler(DenverControllerStarter.this); 
      }
      
    }
    );
    
    
  }

  public static void main(String[] args) {
    String configFile = null; 
    if (args.length == 0 ) {
      configFile = DgPaths.STUDIESDG + "denver/dgConfig.xml";
    }
    else {
      configFile = args[0];
    }
    new DenverControllerStarter(configFile).runDenver();
    
  }



}
