/* *********************************************************************** *
 * project: org.matsim.*
 * OTFClientControl
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
package org.matsim.vis.otfvis;

import org.matsim.vis.otfvis.opengl.drawer.OTFOGLDrawer;


/**
 * This class must not contain server side code. It is responsible 
 * to provide access to the TOP LEVEL control objects of OTFVis, namely
 * the config, the server connection manager, the client data control, e.g. data chaching,
 * the query enging control and the access to the visualization layer.
 * This TOP LEVEL control objects may not exist yet, however it is planned to
 * create them when needed.
 * 
 * @author dgrether
 *
 */
public final class OTFClientControl {
  
  private static final OTFClientControl instance = new OTFClientControl();
  private OTFVisConfigGroup config;
  private OTFOGLDrawer mainDrawer;
  
  private OTFClientControl() {}
  
  public static synchronized OTFClientControl getInstance() {
    return instance;
  }
  
  public void setOTFVisConfig(OTFVisConfigGroup conf) {
    this.config = conf;
    if (this.mainDrawer != null) {
      this.mainDrawer.redraw();
    }
  }
  
  public OTFVisConfigGroup getOTFVisConfig() {
    return this.config;
  }

  public void setMainOTFDrawer(OTFOGLDrawer mainDrawer) {
    this.mainDrawer = mainDrawer;
  }
  
  public OTFOGLDrawer getMainOTFDrawer(){
    return this.mainDrawer;
  }

}
