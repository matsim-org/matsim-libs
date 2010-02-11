/* *********************************************************************** *
 * project: org.matsim.*
 * GershensonControllerTest
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
package java.playground.droeder;

import static org.junit.Assert.assertNotNull;

import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;
import org.matsim.core.api.experimental.events.EventsFactory;
import org.matsim.core.api.experimental.events.EventsManager;

import playground.droeder.gershensonSignals.GershensonAdaptiveTrafficLightController;

public class GershensonControllerTest {
  
  private static final Logger log = Logger
      .getLogger(GershensonControllerTest.class);

  private GershensonAdaptiveTrafficLightController controller;
  
  @Before 
  public void init() {
    
    controller = new GershensonAdaptiveTrafficLightController(null);
    
  }
  
  
  @Test
  public void testControler(){
    assertNotNull(this.controller);
//    boolean green = controller.givenSignalGroupIsGreen(0, signalGroup1);
//    assertTrue(green);
    
    EventsManager events = null;
    EventsFactory fac = events.getFactory();
    
//    events.processEvent(fac.createLinkEnterEvent(0, id1, id1));
    
    
  }
  
}
