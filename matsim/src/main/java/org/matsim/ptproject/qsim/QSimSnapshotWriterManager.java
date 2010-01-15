/* *********************************************************************** *
 * project: org.matsim.*
 * QSimSnapshotWriterManager
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
package org.matsim.ptproject.qsim;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.ControlerIO;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.lanes.otfvis.OTFLanesConnectionManagerFactory;
import org.matsim.vis.otfvis.data.OTFConnectionManagerFactory;
import org.matsim.vis.otfvis.data.fileio.OTFFileWriter;
import org.matsim.vis.otfvis.data.fileio.qsim.OTFFileWriterQSimConnectionManagerFactory;
import org.matsim.vis.otfvis.data.fileio.qsim.OTFQSimServerQuadBuilder;
import org.matsim.vis.snapshots.writers.KmlSnapshotWriter;
import org.matsim.vis.snapshots.writers.PlansFileSnapshotWriter;
import org.matsim.vis.snapshots.writers.SnapshotWriter;
import org.matsim.vis.snapshots.writers.TransimsSnapshotWriter;


/**
 * @author dgrether
 *
 */
public class QSimSnapshotWriterManager {
  
  private static final Logger log = Logger
      .getLogger(QSimSnapshotWriterManager.class);
  
  private final List<SnapshotWriter> snapshotWriters = new ArrayList<SnapshotWriter>();
  
  void createSnapshotwriter(QueueNetwork network, Scenario scenario, int snapshotPeriod, 
      Integer iterationNumber, QSimConfigGroup qSimConfigGroup, 
      ControlerIO controlerIO) {
    // A snapshot period of 0 or less indicates that there should be NO snapshot written
    if (snapshotPeriod > 0 ) {
      String snapshotFormat =  scenario.getConfig().getQSimConfigGroup().getSnapshotFormat();
      Integer itNumber = iterationNumber;
      if (controlerIO == null) {
        log.error("Not able to create io path via ControlerIO in mobility simulation, not able to write visualizer output!");
        return;
      }
      else if (itNumber == null) {
        log.warn("No iteration number set in mobility simulation using iteration number 0 for snapshot file...");
        itNumber = 0;
      }
      if (snapshotFormat.contains("plansfile")) {
        String snapshotFilePrefix = controlerIO.getIterationPath(itNumber) + "/positionInfoPlansFile";
        String snapshotFileSuffix = "xml";
        this.snapshotWriters.add(new PlansFileSnapshotWriter(snapshotFilePrefix,snapshotFileSuffix, network.getNetworkLayer()));
      }
      if (snapshotFormat.contains("transims")) {
        String snapshotFile = controlerIO.getIterationFilename(itNumber, "T.veh");
        this.snapshotWriters.add(new TransimsSnapshotWriter(snapshotFile));
      }
      if (snapshotFormat.contains("googleearth")) {
        String snapshotFile = controlerIO.getIterationFilename(itNumber, "googleearth.kmz");
        String coordSystem = scenario.getConfig().global().getCoordinateSystem();
        this.snapshotWriters.add(new KmlSnapshotWriter(snapshotFile,
            TransformationFactory.getCoordinateTransformation(coordSystem, TransformationFactory.WGS84)));
      }
      if (snapshotFormat.contains("netvis")) {
        log.warn("Snapshot format netvis is no longer supported by this simulation");
      }
      if (snapshotFormat.contains("otfvis")) {
        String snapshotFile = controlerIO.getIterationFilename(itNumber, "otfvis.mvi");
        OTFFileWriter writer = null;

        OTFConnectionManagerFactory connectionManagerFactory = new OTFFileWriterQSimConnectionManagerFactory();
        if (scenario.getConfig().scenario().isUseLanes() && ! scenario.getConfig().scenario().isUseSignalSystems()) {
          connectionManagerFactory = new OTFLanesConnectionManagerFactory(connectionManagerFactory);
        }
        writer = new OTFFileWriter(snapshotPeriod, new OTFQSimServerQuadBuilder(network), snapshotFile, connectionManagerFactory);
        this.snapshotWriters.add(writer);
      }
    } else snapshotPeriod = Integer.MAX_VALUE; // make sure snapshot is never called
  }
  
  
  public boolean addSnapshotWriter(final SnapshotWriter writer) {
    return this.snapshotWriters.add(writer);
  }

  public boolean removeSnapshotWriter(final SnapshotWriter writer) {
    return this.snapshotWriters.remove(writer);
  }


  
  public List<SnapshotWriter> getSnapshotWriters() {
    return snapshotWriters;
  }

}
