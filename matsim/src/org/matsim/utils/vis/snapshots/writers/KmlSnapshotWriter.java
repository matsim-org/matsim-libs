/* *********************************************************************** *
 * project: org.matsim.*
 * KmlSnapshotWriter.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.utils.vis.snapshots.writers;

import java.io.IOException;
import java.util.TreeMap;

import net.opengis.kml._2.DocumentType;
import net.opengis.kml._2.FolderType;
import net.opengis.kml._2.IconStyleType;
import net.opengis.kml._2.KmlType;
import net.opengis.kml._2.LinkType;
import net.opengis.kml._2.MultiGeometryType;
import net.opengis.kml._2.NetworkLinkType;
import net.opengis.kml._2.ObjectFactory;
import net.opengis.kml._2.PlacemarkType;
import net.opengis.kml._2.PointType;
import net.opengis.kml._2.StyleType;
import net.opengis.kml._2.TimeSpanType;
import net.opengis.kml._2.TimeStampType;

import org.apache.log4j.Logger;
import org.matsim.gbl.MatsimResource;
import org.matsim.utils.geometry.Coord;
import org.matsim.utils.geometry.CoordImpl;
import org.matsim.utils.geometry.CoordinateTransformation;
import org.matsim.utils.misc.Time;
import org.matsim.utils.vis.kml.KMZWriter;
import org.matsim.utils.vis.matsimkml.MatsimKMLLogo;
import org.matsim.utils.vis.matsimkml.MatsimKmlStyleFactory;

public class KmlSnapshotWriter implements SnapshotWriter {

	private ObjectFactory kmlObjectFactory = null;
	
	private KmlType mainKml = null;
	private DocumentType mainDoc = null;
	private FolderType mainFolder = null;
	
	private StyleType carStyle = null;
	
	private KmlType timeKml = null;
	private DocumentType timeDoc = null;
	private PlacemarkType timePlacemark = null;
	private MultiGeometryType timeGeometry = null;
	
	private KMZWriter writer = null;

	private CoordinateTransformation coordTransform = null;

	private final TreeMap<Double, NetworkLinkType> timeLinks = new TreeMap<Double, NetworkLinkType>();

	private double time = Time.UNDEFINED_TIME;
	
	private final static Logger log = Logger.getLogger(KmlSnapshotWriter.class);

	public KmlSnapshotWriter(final String filename, final CoordinateTransformation coordTransform) {
		this.coordTransform = coordTransform;
		//the kmz writer
		this.writer = new KMZWriter(filename);
		//the main kml document
		
		this.mainKml = kmlObjectFactory.createKmlType();
		this.mainDoc = kmlObjectFactory.createDocumentType();
		this.mainKml.setAbstractFeatureGroup(kmlObjectFactory.createDocument(this.mainDoc));

		//set car style
		LinkType iconLink = kmlObjectFactory.createLinkType();
		try {
			this.writer.addNonKMLFile(MatsimResource.getAsInputStream("car.png"), "data/car.png");
			iconLink.setHref("./car.png");
		} catch (IOException e1) {
			log.warn("Cannot write car icon to kmz, trying to use icon from http://maps.google.com/mapfiles/kml/pal4/icon15.png");
			iconLink.setHref("http://maps.google.com/mapfiles/kml/pal4/icon15.png");
			e1.printStackTrace();
		}
		this.carStyle = kmlObjectFactory.createStyleType();
		this.carStyle.setId("redCarStyle");
		IconStyleType carIconStyle = kmlObjectFactory.createIconStyleType();
		carIconStyle.setIcon(iconLink);
		carIconStyle.setColor(MatsimKmlStyleFactory.MATSIMRED);
		carIconStyle.setScale(0.5);
		this.mainDoc.getAbstractStyleSelectorGroup().add(kmlObjectFactory.createStyle(this.carStyle));

		this.mainFolder = kmlObjectFactory.createFolderType();
		this.mainDoc.getAbstractFeatureGroup().add(kmlObjectFactory.createFolder(this.mainFolder));
		
		//set logo
		try {
//			ScreenOverlayType logo;
			MatsimKMLLogo.writeMatsimKMLLogo(this.writer);
		} catch (IOException e) {
			log.warn("Cannot read matsim logo file! The logo will not be added to the kmz");
			e.printStackTrace();
		}

	}

	public void beginSnapshot(final double time) {
		this.time = time;
		String timeStr = Time.writeTime(time, Time.TIMEFORMAT_HHMMSS, ':');
		
		this.timeKml = kmlObjectFactory.createKmlType();

		this.timeGeometry = kmlObjectFactory.createMultiGeometryType();
		
		this.timePlacemark = kmlObjectFactory.createPlacemarkType();
		this.timePlacemark.setAbstractGeometryGroup(kmlObjectFactory.createMultiGeometry(this.timeGeometry));
		this.timePlacemark.setStyleUrl(this.carStyle.getId());
		
		this.timeDoc = kmlObjectFactory.createDocumentType();
		this.timeDoc.getAbstractStyleSelectorGroup().add(kmlObjectFactory.createStyle(this.carStyle));
		this.timeDoc.getAbstractFeatureGroup().add(kmlObjectFactory.createPlacemark(this.timePlacemark));
		
		this.timeKml.setAbstractFeatureGroup(kmlObjectFactory.createDocument(this.timeDoc));
	}

	public void endSnapshot() {
		String filename = "data/time_" + this.time + ".kml";
		this.writer.writeLinkedKml(filename, this.timeKml);

		NetworkLinkType nl = kmlObjectFactory.createNetworkLinkType();
		
		LinkType link = kmlObjectFactory.createLinkType();
		link.setHref(filename);
		
		nl.setLink(link);
	
		TimeStampType timeStamp = kmlObjectFactory.createTimeStampType();
		// TODO is that time correct?
		timeStamp.setWhen("1970-01-01T" + Time.writeTime(this.time));
		
		nl.setAbstractTimePrimitiveGroup(kmlObjectFactory.createTimeStamp(timeStamp));
		
//		NetworkLink nl = new NetworkLink("link for time" + this.time, new Link(filename));
//		nl.setTimePrimitive(new TimeStamp(
//				new GregorianCalendar(1970, 0, 1, (int) (this.time / 3600), (int) ((this.time / 60) % 60), (int) (this.time % 60))));
		this.mainFolder.getAbstractFeatureGroup().add(kmlObjectFactory.createNetworkLink(nl));
		this.timeLinks.put(Double.valueOf(this.time), nl);

		this.timeKml = null;
		this.timeDoc = null;
	}

	public void addAgent(final PositionInfo position) {

		//drop all parking vehicles
		if (position.getVehicleState() == PositionInfo.VehicleState.Parking) {
			return;
		}

		Coord coord = this.coordTransform.transform(new CoordImpl(position.getEasting(), position.getNorthing()));
		PointType point = kmlObjectFactory.createPointType();
		point.getCoordinates().add(Double.toString(coord.getX()) + "," + Double.toString(coord.getY()) + ",0.0");
		this.timeGeometry.getAbstractGeometryGroup().add(kmlObjectFactory.createPoint(point));
	}

	public void finish() {
		// change timestamps to timespans
		Double lt = null;
		double lasttime = Double.NEGATIVE_INFINITY;
		for (Double t : this.timeLinks.keySet()) {
			double time = t.doubleValue();
			if (lt != null) {
				NetworkLinkType nl = this.timeLinks.get(lt);
				TimeSpanType timeSpan = kmlObjectFactory.createTimeSpanType();
				timeSpan.setBegin("1970-01-01T" + Time.writeTime(lasttime));
				timeSpan.setEnd("1970-01-01T" + Time.writeTime(time));
				nl.setAbstractTimePrimitiveGroup(kmlObjectFactory.createTimeSpan(timeSpan));
//				nl.setTimePrimitive(new TimeSpan(
//						new GregorianCalendar(1970, 0, 1, (int) (lasttime / 3600), (int) ((lasttime / 60) % 60), (int) (lasttime % 60)),
//						new GregorianCalendar(1970, 0, 1, (int) (time / 3600), (int) ((time / 60) % 60), (int) (time % 60))));
			}
			lt = t;
			lasttime = time;
		}
		// write main kml
		this.writer.writeMainKml(this.mainKml);
		this.writer.close();
	}
}
