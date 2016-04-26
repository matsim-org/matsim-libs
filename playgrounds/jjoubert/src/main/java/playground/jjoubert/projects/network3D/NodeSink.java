/* *********************************************************************** *
 * project: org.matsim.*
 * NodeSink.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
 * 
 */
package playground.jjoubert.projects.network3D;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.core.utils.io.IOUtils;
import org.openstreetmap.osmosis.core.container.v0_6.BoundContainer;
import org.openstreetmap.osmosis.core.container.v0_6.EntityContainer;
import org.openstreetmap.osmosis.core.container.v0_6.EntityProcessor;
import org.openstreetmap.osmosis.core.container.v0_6.NodeContainer;
import org.openstreetmap.osmosis.core.container.v0_6.RelationContainer;
import org.openstreetmap.osmosis.core.container.v0_6.WayContainer;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.task.v0_6.Sink;

/**
 * @author jwjoubert
 *
 */
public class NodeSink implements Sink {
	private final static Logger LOG = Logger.getLogger(NodeSink.class);
	private final String outputfile;
	private BufferedWriter bw;
	
	public NodeSink(String file) {
		this.outputfile = file;
	}

	/* (non-Javadoc)
	 * @see org.openstreetmap.osmosis.core.task.v0_6.Initializable#initialize(java.util.Map)
	 */
	@Override
	public void initialize(Map<String, Object> arg0) {
		LOG.info("Initialising...");
		this.bw = IOUtils.getBufferedWriter(outputfile);
		try {
			bw.write("osmId,lon,lat,elevation");
			bw.newLine();
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Could not write the header.");
		}
		
		/* TODO Set up your 3D infrastructure here. */

	}

	/* (non-Javadoc)
	 * @see org.openstreetmap.osmosis.core.lifecycle.Completable#complete()
	 */
	@Override
	public void complete() {
		LOG.info("Completing...");
		try {
			this.bw.close();
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Could not close the file.");
		}
	}

	/* (non-Javadoc)
	 * @see org.openstreetmap.osmosis.core.lifecycle.Releasable#release()
	 */
	@Override
	public void release() {
		LOG.info("Releasing...");
	}

	/* (non-Javadoc)
	 * @see org.openstreetmap.osmosis.core.task.v0_6.Sink#process(org.openstreetmap.osmosis.core.container.v0_6.EntityContainer)
	 */
	@Override
	public void process(EntityContainer entityContainer) {
		EntityProcessor entityProcessor = new EntityProcessor() {
			
			@Override
			public void process(RelationContainer arg0) {
			}
			
			@Override
			public void process(WayContainer arg0) {
			}
			
			@Override
			public void process(NodeContainer nodeContainer) {
				Node node = nodeContainer.getEntity();
				long id = node.getId();
				double lon = node.getLongitude();
				double lat = node.getLatitude();
				
				/* TODO Do some elevation estimation. */
				double elev = 123.456;
				try {
					bw.write(String.format("%d,%.4f,%.4f,%.1f\n", id, lon, lat,elev));
				} catch (IOException e) {
					e.printStackTrace();
					throw new RuntimeException("Could not write OSM node " + id);
				}
			}
			
			@Override
			public void process(BoundContainer arg0) {
			}
		};
		entityContainer.process(entityProcessor );
		

	}

}
