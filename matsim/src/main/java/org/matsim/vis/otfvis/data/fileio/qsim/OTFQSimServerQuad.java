/* *********************************************************************** *
 * project: org.matsim.*
 * OTFQSimServerQuad
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
package org.matsim.vis.otfvis.data.fileio.qsim;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.ptproject.qsim.QLink;
import org.matsim.ptproject.qsim.QNetwork;
import org.matsim.ptproject.qsim.QNode;
import org.matsim.vis.otfvis.data.OTFConnectionManager;
import org.matsim.vis.otfvis.data.OTFDataWriter;
import org.matsim.vis.otfvis.data.OTFServerQuad2;
import org.matsim.vis.otfvis.data.OTFWriterFactory;

/**
 * @author dgrether
 * 
 */
public class OTFQSimServerQuad extends OTFServerQuad2 {

	private static final long serialVersionUID = 23L;

	private static final Logger log = Logger.getLogger(OTFQSimServerQuad.class);

	transient private QNetwork net;

	public OTFQSimServerQuad(QNetwork net) {
		super(net.getNetwork());
		this.net = net;
	}

	@Override
	public void initQuadTree(OTFConnectionManager connect) {
		createFactoriesAndFillQuadTree(connect);
	}

	private void createFactoriesAndFillQuadTree(OTFConnectionManager connect) {
		Collection<Class<OTFWriterFactory<QNode>>> nodeFactories = connect.getQNodeEntries();
		List<OTFWriterFactory<QNode>> nodeWriterFractoryObjects = instanciateFactories(nodeFactories);
		installNodeWriterFactories(nodeWriterFractoryObjects);

		Collection<Class<OTFWriterFactory<QLink>>> linkFactories = connect.getQLinkEntries();
		List<OTFWriterFactory<QLink>> linkWriterFactoryObjects = instanciateFactories(linkFactories);
		installLinkWriterFactories(linkWriterFactoryObjects);
	}

	private static <T> List<OTFWriterFactory<T>> instanciateFactories(Collection<Class<OTFWriterFactory<T>>> nodeFactories) {
		List<OTFWriterFactory<T>> writerFactoryObjects = new ArrayList<OTFWriterFactory<T>>();
		OTFWriterFactory<T> writerFactory = null;
		for (Class<? extends OTFWriterFactory<T>> writerFactoryClass : nodeFactories) {
			try {
				writerFactory = writerFactoryClass.newInstance();
			} catch (InstantiationException e) {
				throw new RuntimeException(e);
			} catch (IllegalAccessException e) {
				throw new RuntimeException(e);
			}
			writerFactoryObjects.add(writerFactory);
		}
		return writerFactoryObjects;
	}

	private void installLinkWriterFactories(
			List<OTFWriterFactory<QLink>> linkWriterFactoryObjects) {
		boolean first = true;
		for (QLink link : this.net.getLinks().values()) {
			double middleEast = (link.getLink().getToNode().getCoord().getX() + link
					.getLink().getFromNode().getCoord().getX())
					* 0.5 - this.minEasting;
			double middleNorth = (link.getLink().getToNode().getCoord().getY() + link
					.getLink().getFromNode().getCoord().getY())
					* 0.5 - this.minNorthing;
			for (OTFWriterFactory<QLink> fac : linkWriterFactoryObjects) {
				OTFDataWriter<QLink> writer = fac.getWriter();
				// null means take the default handler
				if (writer != null) {
					writer.setSrc(link);
					if (first) {
						log.info("Connecting Source QLink with "
								+ writer.getClass().getName());
						first = false;
					}
				}
				this.put(middleEast, middleNorth, writer);
			}
		}
	}

	private void installNodeWriterFactories(
			List<OTFWriterFactory<QNode>> nodeWriterFractoryObjects) {
		boolean first = true;
		for (QNode node : this.net.getNodes().values()) {
			for (OTFWriterFactory<QNode> fac : nodeWriterFractoryObjects) {
				OTFDataWriter<QNode> writer = fac.getWriter();
				if (writer != null) {
					writer.setSrc(node);
					if (first) {
						log.info("Connecting Source QNode with "
								+ writer.getClass().getName());
						first = false;
					}
				}
				this.put(node.getNode().getCoord().getX() - this.minEasting,
						node.getNode().getCoord().getY() - this.minNorthing,
						writer);
			}
		}
	}

}
