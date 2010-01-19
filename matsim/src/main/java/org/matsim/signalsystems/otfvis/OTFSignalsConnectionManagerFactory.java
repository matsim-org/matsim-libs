/* *********************************************************************** *
 * project: org.matsim.*
 * OTFLanesConnectionManagerFactory
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
package org.matsim.signalsystems.otfvis;

import org.matsim.lanes.otfvis.drawer.OTFLaneSignalDrawer;
import org.matsim.ptproject.qsim.QueueLink;
import org.matsim.signalsystems.otfvis.io.OTFSignalReader;
import org.matsim.signalsystems.otfvis.io.OTFSignalWriter;
import org.matsim.signalsystems.otfvis.layer.OTFSignalLayer;
import org.matsim.vis.otfvis.data.OTFConnectionManager;
import org.matsim.vis.otfvis.data.OTFConnectionManagerFactory;


/**
 * @author dgrether
 *
 */
public class OTFSignalsConnectionManagerFactory implements OTFConnectionManagerFactory {

  private OTFConnectionManagerFactory delegate;

  public OTFSignalsConnectionManagerFactory(OTFConnectionManagerFactory delegate) {
    this.delegate = delegate;
  }
  
  @Override
  public OTFConnectionManager createConnectionManager() {
    OTFConnectionManager connect = this.delegate.createConnectionManager();
    // data source to writer
    connect.add(QueueLink.class, OTFSignalWriter.class);
    // writer -> reader: from server to client
    connect
    .add(OTFSignalWriter.class, OTFSignalReader.class);
    // reader to drawer (or provider to receiver)
    connect.add(OTFSignalReader.class, OTFLaneSignalDrawer.class);
    // drawer -> layer
    connect.add(OTFLaneSignalDrawer.class, OTFSignalLayer.class);
    return connect;
  }

}
