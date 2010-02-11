/* *********************************************************************** *
 * project: org.matsim.*
 * OTFConnectionManager.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package org.matsim.vis.otfvis.data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.core.mobsim.queuesim.QueueLink;
import org.matsim.ptproject.qsim.QLink;
import org.matsim.ptproject.qsim.QNode;
import org.matsim.vis.otfvis.caching.SceneGraph;
import org.matsim.vis.otfvis.caching.SceneLayer;
import org.matsim.vis.otfvis.data.fileio.queuesim.OTFQueueSimLinkAgentsWriter;
import org.matsim.vis.otfvis.interfaces.OTFDataReader;

/**
 * The OTFConnectionManager is the most important class when building an OTFVis instance.
 * It holds pairs of classes. Each of this class-pairs yields as a "From" -> "To" connection between classes.
 * The whole from-to conncetions established in a OTFConnectionManager describe the route all data has to 
 * take from the source (normally a QueueLink/Node, etc.) to the actual display on screen.
 * It is the programmer's responsibility to define a complete chain of responsible objects for all data sent.
 * 
 *  A chain of responsibility normally consists of 
 *  a DataSource (e.g. QueueLink), 
 *  a DataWriter (e.g. OTFDefaultLinkHandler.Writer)
 *  a DataReader (e.g. OTFDefaultLinkHandler)
 *  a Visualizer class (e.g. SimpleStaticNetLayer.QuadDrawer)
 *  and possibly a layer this Drawer belongs to (e.g. SimpleStaticNetLayer)
 *  
 * @author dstrippgen
 *
 */
public class OTFConnectionManager implements Cloneable, Serializable {

	private static final long serialVersionUID = 6481835753628883014L;
	
	private static final Logger log = Logger.getLogger(OTFConnectionManager.class);

	private static class Entry implements Serializable {

		private static final long serialVersionUID = -2260651735789627280L;
		
		Class<?> from, to;

		public Entry(Class<?> from, Class<?> to) {
			this.from = from;
			this.to = to;
		}
		
		@Override
		public String toString() {
			return "(" + from.toString() + "," + to.toString() + ") ";
		}

		public Class<?> getTo(){
			return this.to;
		}
		
		public Class<?> getFrom(){
			return this.from;
		}

		@Override
		public boolean equals(Object obj) {
			Entry other = (Entry) obj;
			return from.equals(other.from) && to.equals(other.to);
		}

		@Override
		public int hashCode() {
			return from.hashCode() * to.hashCode();
		}
		
	}
	
	private final List<Entry> connections = new LinkedList<Entry>();
	
	@Override
	public OTFConnectionManager clone() {
		OTFConnectionManager clone = new OTFConnectionManager();
		Iterator<Entry> iter = connections.iterator();
		while(iter.hasNext()) {
			Entry entry = iter.next();
			Entry entry1 = new Entry(entry.from, entry.to);
			clone.connections.add(entry1);
		}
		return clone;
	}
	
	public void connectQNodeToWriter(Class<? extends OTFDataWriter<? extends QNode>> writer) {
		Entry entry = new Entry(QNode.class, writer);
		connections.add(entry);
	}
	
	public void connectQLinkToWriter(Class<? extends OTFDataWriter<? extends QLink>> writer) {
		Entry entry = new Entry(QLink.class, writer);
		connections.add(entry);
	}
	
	public void connectQueueLinkToWriter(Class<OTFQueueSimLinkAgentsWriter> writer) {
		Entry entry = new Entry(QueueLink.class, writer);
		connections.add(entry);
	}

	@SuppressWarnings("unchecked")
	public Collection<Class<OTFWriterFactory<QueueLink>>> getQueueLinkEntries() {
		Collection<Class<OTFWriterFactory<QueueLink>>> result = new ArrayList<Class<OTFWriterFactory<QueueLink>>>();
		for (Class<?> clazz : getToEntries(QueueLink.class)) {
			result.add((Class<OTFWriterFactory<QueueLink>>) clazz);
		}
		return result;
	}

	public void connectWriterToReader(Class<? extends OTFDataWriter> writer, Class<? extends OTFDataReader> reader) {
		Collection<Class<?>> readerClasses = this.getToEntries(writer);
		if (!readerClasses.isEmpty()) {
			throw new RuntimeException("We already have a reader for this writer.");
		}
		Entry entry = new Entry(writer, reader);
		connections.add(entry);
	}
	
	public void connectReaderToReceiver(Class<? extends OTFDataReader> reader, Class<? extends OTFDataReceiver> receiver) {
		Entry entry = new Entry(reader, receiver);
		connections.add(entry);
	}
	
	public void connectReceiverToLayer(Class<? extends OTFDataReceiver> receiver, Class<? extends SceneLayer> layer) {
		Entry entry = new Entry(receiver, layer);
		connections.add(entry);
	}

	public void remove(Class<?> from) {
		Iterator<Entry> iter = connections.iterator();
		while(iter.hasNext()) {
			Entry entry = iter.next();
			if (entry.from == from) iter.remove();
		}
	}

	public Collection<Class<?>> getToEntries(Class<?> srcClass) {
		List<Class<?>> classList = new LinkedList<Class<?>>();
		for(Entry entry : connections) {
			if (entry.from.equals(srcClass)) {
			  classList.add(entry.to);
			}
		}
		return classList;
	}

	@SuppressWarnings("unchecked")
	public Collection<OTFDataReceiver> getReceiversForReader(Class<? extends OTFDataReader> reader, SceneGraph graph) {
		Collection<Class<?>> classList = getToEntries(reader);
		List<OTFDataReceiver> receiverList = new LinkedList<OTFDataReceiver>();
		for(Class<?> entry : classList) {
			try {
				receiverList.add(graph.newInstance((Class<? extends OTFDataReceiver>) entry));
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
		}
		return receiverList;
	}

	@SuppressWarnings("unchecked")
	public Collection<Class<OTFDataReader>> getReadersForWriter(Class<? extends OTFDataWriter> writer) {
		Collection<Class<?>> classList = getToEntries(writer);
		List<Class<OTFDataReader>> readerList = new LinkedList<Class<OTFDataReader>>();
		for(Class<?> entry : classList) {
			readerList.add((Class<OTFDataReader>) entry);
		}
		return readerList;
	}

	public void fillLayerMap(Map<Class<?>, SceneLayer> layers) {
		Iterator<Entry> iter = connections.iterator();
		while(iter.hasNext()) {
			Entry entry = iter.next();
			if (SceneLayer.class.isAssignableFrom(entry.to))
				try {
					layers.put(entry.from, (SceneLayer)(entry.to.newInstance()));
				} catch (InstantiationException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				}
		}
	}

	public void addEntriesFrom(OTFConnectionManager connect2) {
		Iterator<Entry> iter = connect2.connections.iterator();
		while(iter.hasNext()) {
			Entry entry = iter.next();
			log.info("updating entry: " + entry.from.getCanonicalName() + " to " + entry.to.getName());
			this.connections.add(entry);
		}
	}
	
	public void logEntries() {
		for (Entry e : this.connections){
			log.info("writing entry: " + e.getFrom().getCanonicalName() + " to " + e.getTo().getName());
		}
	}

	@SuppressWarnings("unchecked")
	public Collection<Class<OTFWriterFactory<QNode>>> getQNodeEntries() {
		Collection<Class<OTFWriterFactory<QNode>>> result = new ArrayList<Class<OTFWriterFactory<QNode>>>();
		for (Class<?> clazz : getToEntries(QNode.class)) {
			result.add((Class<OTFWriterFactory<QNode>>) clazz);
		}
		return result;
	}
	
	@SuppressWarnings("unchecked")
	public Collection<Class<OTFWriterFactory<QLink>>> getQLinkEntries() {
		Collection<Class<OTFWriterFactory<QLink>>> result = new ArrayList<Class<OTFWriterFactory<QLink>>>();
		for (Class<?> clazz : getToEntries(QLink.class)) {
			result.add((Class<OTFWriterFactory<QLink>>) clazz);
		}
		return result;
	}
	
}
