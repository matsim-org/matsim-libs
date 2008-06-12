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
import java.util.GregorianCalendar;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.gbl.MatsimResource;
import org.matsim.utils.geometry.CoordI;
import org.matsim.utils.geometry.CoordinateTransformationI;
import org.matsim.utils.geometry.shared.Coord;
import org.matsim.utils.misc.Time;
import org.matsim.utils.vis.kml.ColorStyle;
import org.matsim.utils.vis.kml.Document;
import org.matsim.utils.vis.kml.Folder;
import org.matsim.utils.vis.kml.Icon;
import org.matsim.utils.vis.kml.IconStyle;
import org.matsim.utils.vis.kml.KML;
import org.matsim.utils.vis.kml.KMLWriter;
import org.matsim.utils.vis.kml.KMZWriter;
import org.matsim.utils.vis.kml.Link;
import org.matsim.utils.vis.kml.MultiGeometry;
import org.matsim.utils.vis.kml.NetworkLink;
import org.matsim.utils.vis.kml.Placemark;
import org.matsim.utils.vis.kml.Point;
import org.matsim.utils.vis.kml.Style;
import org.matsim.utils.vis.kml.TimeSpan;
import org.matsim.utils.vis.kml.TimeStamp;
import org.matsim.utils.vis.matsimkml.MatsimKMLLogo;
import org.matsim.utils.vis.matsimkml.MatsimKmlStyleFactory;

public class KmlSnapshotWriter implements SnapshotWriterI {

	private KML mainKml = null;
	private Document mainDoc = null;
	private Folder mainFolder = null;

	private Style carStyle = null;

	private KML timeKml = null;
	private Document timeDoc = null;
	private Placemark timePlacemark = null;
	private MultiGeometry timeGeometry = null;

	private KMZWriter writer = null;

	private CoordinateTransformationI coordTransform = null;

	private final TreeMap<Double, NetworkLink> timeLinks = new TreeMap<Double, NetworkLink>();

	private double time = Time.UNDEFINED_TIME;
	
	private final static Logger log = Logger.getLogger(KmlSnapshotWriter.class);

	public KmlSnapshotWriter(final String filename, final CoordinateTransformationI coordTransform) {
		this.coordTransform = coordTransform;
		//the kmz writer
		this.writer = new KMZWriter(filename, KMLWriter.DEFAULT_XMLNS);
		//the main kml document
		this.mainKml = new KML();
		this.mainDoc = new Document(filename);
		this.mainKml.setFeature(this.mainDoc);

		//set car style
		Icon icon;
		try {
			this.writer.addNonKMLFile(MatsimResource.getAsInputStream("car.png"), "data/car.png");
			icon = new Icon("./car.png");
		} catch (IOException e1) {
			log.warn("Cannot write car icon to kmz, trying to use icon from http://maps.google.com/mapfiles/kml/pal4/icon15.png");
			icon = new Icon("http://maps.google.com/mapfiles/kml/pal4/icon15.png");
			e1.printStackTrace();
		}
		this.carStyle = new Style("redCarStyle");
		this.carStyle.setIconStyle(new IconStyle(icon, MatsimKmlStyleFactory.MATSIMRED, ColorStyle.DEFAULT_COLOR_MODE, 0.5));
		this.mainDoc.addStyle(this.carStyle);

		this.mainFolder = new Folder("networklinksfolder");
		this.mainDoc.addFeature(this.mainFolder);
		//set logo
		try {
			MatsimKMLLogo logo;
			logo = new MatsimKMLLogo(this.writer);
			this.mainFolder.addFeature(logo);
		} catch (IOException e) {
			log.warn("Cannot read matsim logo file! The logo will not be added to the kmz");
			e.printStackTrace();
		}

	}

	public void beginSnapshot(final double time) {
		this.time = time;
		String timeStr = Time.writeTime(time, Time.TIMEFORMAT_HHMMSS, ':');
		this.timeKml = new KML();
		this.timeDoc = new Document(timeStr);
		this.timeKml.setFeature(this.timeDoc);
		this.timeDoc.addStyle(this.carStyle);
		this.timePlacemark = new Placemark(timeStr);
		this.timeDoc.addFeature(this.timePlacemark);
		this.timeGeometry = new MultiGeometry();
		this.timePlacemark.setGeometry(this.timeGeometry);
		this.timePlacemark.setStyleUrl(this.carStyle.getStyleUrl());
	}

	public void endSnapshot() {
		String filename = "data/time_" + this.time + ".kml";
		this.writer.writeLinkedKml(filename, this.timeKml);

		NetworkLink nl = new NetworkLink("link for time" + this.time, new Link(filename));
		nl.setTimePrimitive(new TimeStamp(
				new GregorianCalendar(1970, 0, 1, (int) (this.time / 3600), (int) ((this.time / 60) % 60), (int) (this.time % 60))));
		this.mainFolder.addFeature(nl);
		this.timeLinks.put(Double.valueOf(this.time), nl);

		this.timeKml = null;
		this.timeDoc = null;
	}

	public void addAgent(final PositionInfo position) {

		//drop all parking vehicles
		if (position.getVehicleState() == PositionInfo.VehicleState.Parking) {
			return;
		}

		CoordI coord = this.coordTransform.transform(new Coord(position.getEasting(), position.getNorthing()));
		Point point = new Point(coord.getX(), coord.getY(), 0.0);
		this.timeGeometry.addGeometry(point);
	}

	public void finish() {
		// change timestamps to timespans
		Double lt = null;
		double lasttime = Double.NEGATIVE_INFINITY;
		for (Double t : this.timeLinks.keySet()) {
			double time = t.doubleValue();
			if (lt != null) {
				NetworkLink nl = this.timeLinks.get(lt);
				nl.setTimePrimitive(new TimeSpan(
						new GregorianCalendar(1970, 0, 1, (int) (lasttime / 3600), (int) ((lasttime / 60) % 60), (int) (lasttime % 60)),
						new GregorianCalendar(1970, 0, 1, (int) (time / 3600), (int) ((time / 60) % 60), (int) (time % 60))));
			}
			lt = t;
			lasttime = time;
		}
		// write main kml
		this.writer.writeMainKml(this.mainKml);
		this.writer.close();
	}
}
