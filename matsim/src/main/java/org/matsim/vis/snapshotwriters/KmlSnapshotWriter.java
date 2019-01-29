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

package org.matsim.vis.snapshotwriters;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.gbl.MatsimResource;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.misc.Time;
import org.matsim.vis.kml.KMZWriter;
import org.matsim.vis.kml.MatsimKMLLogo;
import org.matsim.vis.kml.MatsimKmlStyleFactory;

import net.opengis.kml.v_2_2_0.DocumentType;
import net.opengis.kml.v_2_2_0.FolderType;
import net.opengis.kml.v_2_2_0.IconStyleType;
import net.opengis.kml.v_2_2_0.KmlType;
import net.opengis.kml.v_2_2_0.LinkType;
import net.opengis.kml.v_2_2_0.MultiGeometryType;
import net.opengis.kml.v_2_2_0.NetworkLinkType;
import net.opengis.kml.v_2_2_0.ObjectFactory;
import net.opengis.kml.v_2_2_0.PlacemarkType;
import net.opengis.kml.v_2_2_0.PointType;
import net.opengis.kml.v_2_2_0.StyleType;
import net.opengis.kml.v_2_2_0.TimeSpanType;
import net.opengis.kml.v_2_2_0.TimeStampType;

public class KmlSnapshotWriter implements SnapshotWriter {

	private final ObjectFactory kmlObjectFactory = new ObjectFactory();
	
	private final KmlType mainKml;
	private final DocumentType mainDoc;
	private FolderType mainFolder = null;
	
	private final Map<String,StyleType> carStyles = new LinkedHashMap<>() ;
	
	private KmlType timeKml = null;
	private DocumentType timeDoc = null;
	
	private PlacemarkType timePlacemarkRed = null;
	private PlacemarkType timePlacemarkYellow = null;
	private PlacemarkType timePlacemarkGreen = null;
	
	private MultiGeometryType timeGeometryRed = null;
	private MultiGeometryType timeGeometryYellow = null;
	private MultiGeometryType timeGeometryGreen = null;
	
	private final KMZWriter writer;

	private final CoordinateTransformation coordTransform;

	private final TreeMap<Double, NetworkLinkType> timeLinks = new TreeMap<>();

	private double time = Time.UNDEFINED_TIME;
	
	private final static Logger log = Logger.getLogger(KmlSnapshotWriter.class);
	
	private boolean writeThisSnapshot ;

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
		{
			StyleType carStyle = createColoredCarStyle(iconLink, MatsimKmlStyleFactory.MATSIMRED, "redCarStyle");
			carStyles.put( carStyle.getId(), carStyle ) ;
		}
		{
			StyleType carStyle = createColoredCarStyle(iconLink, MatsimKmlStyleFactory.MATSIMYELLOW, "yellowCarStyle");
			carStyles.put( carStyle.getId(), carStyle ) ;
		}
		{
			StyleType carStyle = createColoredCarStyle(iconLink, MatsimKmlStyleFactory.MATSIMGREEN, "greenCarStyle");
			carStyles.put( carStyle.getId(), carStyle ) ;
		}
		
		for ( StyleType carStyle : carStyles.values() ) {
			this.mainDoc.getAbstractStyleSelectorGroup().add(kmlObjectFactory.createStyle(carStyle));
			// is this ever used anywhere?  I think that there would have to be a "setStyleUrl" somehow connected to
			// mainDoc in order to be used.  ??  kai, oct'17
		}

		this.mainFolder = kmlObjectFactory.createFolderType();
		this.mainDoc.getAbstractFeatureGroup().add(kmlObjectFactory.createFolder(this.mainFolder));
		
		//set logo
		try {
			MatsimKMLLogo.writeMatsimKMLLogo(this.writer);
		} catch (IOException e) {
			log.warn("Cannot read matsim logo file! The logo will not be added to the kmz");
			e.printStackTrace();
		}

		this.writeThisSnapshot = false ;
	}

	private StyleType createColoredCarStyle(LinkType iconLink, final byte[] color, final String idString) {
		StyleType carStyle = kmlObjectFactory.createStyleType();
		carStyle.setId(idString);
		IconStyleType carIconStyle = kmlObjectFactory.createIconStyleType();
		carIconStyle.setIcon(iconLink);
		carIconStyle.setColor(color);
		carIconStyle.setScale(Double.valueOf(0.5));
		carStyle.setIconStyle(carIconStyle);
		return carStyle;
	}

	@Override
	public void beginSnapshot(final double time) {
		this.time = time;
		
		this.timeKml = kmlObjectFactory.createKmlType();
		
		this.timeGeometryRed = kmlObjectFactory.createMultiGeometryType();
		this.timeGeometryYellow = kmlObjectFactory.createMultiGeometryType();
		this.timeGeometryGreen = kmlObjectFactory.createMultiGeometryType();
		
		this.timePlacemarkRed = kmlObjectFactory.createPlacemarkType();
		this.timePlacemarkYellow = kmlObjectFactory.createPlacemarkType();
		this.timePlacemarkGreen = kmlObjectFactory.createPlacemarkType();
		
		this.timePlacemarkRed.setAbstractGeometryGroup(kmlObjectFactory.createMultiGeometry(this.timeGeometryRed));
		this.timePlacemarkYellow.setAbstractGeometryGroup(kmlObjectFactory.createMultiGeometry(this.timeGeometryYellow));
		this.timePlacemarkGreen.setAbstractGeometryGroup(kmlObjectFactory.createMultiGeometry(this.timeGeometryGreen));
		
		this.timePlacemarkRed.setStyleUrl("redCarStyle");
		this.timePlacemarkYellow.setStyleUrl("yellowCarStyle");
		this.timePlacemarkGreen.setStyleUrl("greenCarStyle");
		
		this.timeDoc = kmlObjectFactory.createDocumentType();
		for ( StyleType carStyle : carStyles.values() ) {
			this.timeDoc.getAbstractStyleSelectorGroup().add(kmlObjectFactory.createStyle(carStyle));
		}
		this.timeDoc.getAbstractFeatureGroup().add(kmlObjectFactory.createPlacemark(this.timePlacemarkRed));
		this.timeDoc.getAbstractFeatureGroup().add(kmlObjectFactory.createPlacemark(this.timePlacemarkYellow));
		this.timeDoc.getAbstractFeatureGroup().add(kmlObjectFactory.createPlacemark(this.timePlacemarkGreen));
		
		this.timeKml.setAbstractFeatureGroup(kmlObjectFactory.createDocument(this.timeDoc));
	}
	
	@Override
	public void addAgent(final AgentSnapshotInfo info) {
		
		//drop all parking vehicles
		if (info.getAgentState() == AgentSnapshotInfo.AgentState.PERSON_AT_ACTIVITY) {
			return;
		}
		
		this.writeThisSnapshot = true ;
		
		Coord coord = this.coordTransform.transform(new Coord(info.getEasting(), info.getNorthing()));
		PointType point = kmlObjectFactory.createPointType();
		point.getCoordinates().add(Double.toString(coord.getX()) + "," + Double.toString(coord.getY()) + ",0.0");

		if ( info.getColorValueBetweenZeroAndOne() < 0.33 ) {
			this.timeGeometryRed.getAbstractGeometryGroup().add(kmlObjectFactory.createPoint(point));
		} else if ( info.getColorValueBetweenZeroAndOne() < 0.66 ) {
			this.timeGeometryYellow.getAbstractGeometryGroup().add(kmlObjectFactory.createPoint(point));
		} else {
			this.timeGeometryGreen.getAbstractGeometryGroup().add(kmlObjectFactory.createPoint(point));
		}
		
	}
	
	@Override
	public void endSnapshot() {
		if ( this.writeThisSnapshot ) {
			String filename = "data/time_" + this.time + ".kml";
			this.writer.writeLinkedKml(filename, this.timeKml);

			NetworkLinkType nl = kmlObjectFactory.createNetworkLinkType();
		
			LinkType link = kmlObjectFactory.createLinkType();
			link.setHref(filename);
		
			nl.setLink(link);
	
			TimeStampType timeStamp = kmlObjectFactory.createTimeStampType();
			timeStamp.setWhen("1970-01-01T" + Time.writeTime(this.time));
		
			nl.setAbstractTimePrimitiveGroup(kmlObjectFactory.createTimeStamp(timeStamp));
		
			this.mainFolder.getAbstractFeatureGroup().add(kmlObjectFactory.createNetworkLink(nl));
			this.timeLinks.put(Double.valueOf(this.time), nl);
		}

		this.timeKml = null;
		this.timeDoc = null;
		
		this.writeThisSnapshot = false ;
	}

	@Override
	public void finish() {
		// change timestamps to timespans
		String lastTimeS = null;
		String timeS;
		for (Entry<Double, NetworkLinkType> e : this.timeLinks.entrySet()) {
			timeS = Time.writeTime(e.getKey().doubleValue());
			if (lastTimeS != null) {
				NetworkLinkType nl = e.getValue();
				TimeSpanType timeSpan = kmlObjectFactory.createTimeSpanType();
				timeSpan.setBegin("1970-01-01T" + lastTimeS);
				timeSpan.setEnd("1970-01-01T" + timeS);
				nl.setAbstractTimePrimitiveGroup(kmlObjectFactory.createTimeSpan(timeSpan));
			}
			lastTimeS = timeS;
		}
		// write main kml
		this.writer.writeMainKml(this.mainKml);
		this.writer.close();
	}
}
