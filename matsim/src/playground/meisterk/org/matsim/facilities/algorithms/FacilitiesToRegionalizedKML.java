/* *********************************************************************** *
 * project: org.matsim.*
 * FacilitiesToRegionalizedKML.java
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

package playground.meisterk.org.matsim.facilities.algorithms;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import net.opengis.kml._2.BasicLinkType;
import net.opengis.kml._2.DocumentType;
import net.opengis.kml._2.FolderType;
import net.opengis.kml._2.IconStyleType;
import net.opengis.kml._2.KmlType;
import net.opengis.kml._2.ObjectFactory;
import net.opengis.kml._2.PlacemarkType;
import net.opengis.kml._2.PointType;
import net.opengis.kml._2.StyleType;
import net.opengis.kml._2.TimeSpanType;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.facilities.OpeningTime;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.transformations.CH1903LV03toWGS84;
import org.matsim.core.utils.misc.Time;
import org.matsim.facilities.algorithms.AbstractFacilityAlgorithm;

import playground.meisterk.org.matsim.run.facilities.ShopsOf2005ToFacilities.Day;

//import com.google.earth.kml._2.BasicLinkType;
//import com.google.earth.kml._2.DocumentType;
//import com.google.earth.kml._2.FolderType;
//import com.google.earth.kml._2.IconStyleType;
//import com.google.earth.kml._2.KmlType;
//import com.google.earth.kml._2.ObjectFactory;
//import com.google.earth.kml._2.PlacemarkType;
//import com.google.earth.kml._2.PointType;
//import com.google.earth.kml._2.StyleType;
//import com.google.earth.kml._2.TimeSpanType;

/**
 * Transforms a MATSim-T facilities.xml file into a Google Earth KML file.
 * The class provides the opportunity to split the KML into regions
 * whose contents are loaded asynchronously. So large amounts of facilites
 * data can be processed and displayed. Furthermore, opening times of
 * facilities are directly converted to TimeSpan attributes of the placemarks.
 * Multiple activities in a facility result in multiple placemarks
 * at the same location.
 *
 * Don't forget to call init() and finish().
 *
 * @author meisterk
 *
 */
public class FacilitiesToRegionalizedKML extends AbstractFacilityAlgorithm {

	public static final String ACTIVITY_TYPE_SHOP = "shop";
	public static final String SHOP_STYLE = ACTIVITY_TYPE_SHOP + "Style";

	// let's use the week April, 21 to April 27, 2008 as THE week
	// to display open times in Google Earth TimeSpan objects
	final int MONDAY_DAY = 21;

	private static final Logger log = Logger.getLogger(FacilitiesToRegionalizedKML.class);
	private static final Day[] days = Day.values();

	// documentName should become the facilities::name
	private String documentName = null;
	private double iconScale = 1.0;
	private String outputFilename = null;

	private JAXBContext jaxbContext = null;
	private ObjectFactory kmlJAXBFactory = new ObjectFactory();
	private KmlType kml = null;
	private DocumentType document = null;

	public FacilitiesToRegionalizedKML(final String documentName, final double iconScale,
			final String outputFilename) {
		super();
		this.documentName = documentName;
		this.iconScale = iconScale;
		this.outputFilename = outputFilename;
	}

	public FacilitiesToRegionalizedKML() {
		this("A facilities KML", 1.0, "output/output_facilitiesKML.kml");
	}

	public void init() {

		log.info("Initializing KML... ");

		// kml and document
		try {
			this.jaxbContext = JAXBContext.newInstance("com.google.earth.kml._2");
		} catch (JAXBException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		this.kml = this.kmlJAXBFactory.createKmlType();
		this.document = this.kmlJAXBFactory.createDocumentType();
		this.document.setName(this.documentName);
		this.kml.setAbstractFeatureGroup(this.kmlJAXBFactory.createDocument(this.document));

		// styles
		StyleType shopStyle = this.kmlJAXBFactory.createStyleType();
		this.document.getAbstractStyleSelectorGroup().add(this.kmlJAXBFactory.createStyle(shopStyle));
		shopStyle.setId(FacilitiesToRegionalizedKML.SHOP_STYLE);
		IconStyleType shopIconStyle = this.kmlJAXBFactory.createIconStyleType();
		shopStyle.setIconStyle(shopIconStyle);
		BasicLinkType shopIconLink = this.kmlJAXBFactory.createBasicLinkType();
		shopIconStyle.setIcon(shopIconLink);
		shopIconStyle.setScale(this.iconScale);
		shopIconLink.setHref("http://maps.google.com/mapfiles/kml/paddle/S.png");
		log.info("Initializing KML...done.");

	}

	public void run(final ActivityFacility facility) {

		PlacemarkType aShopOpeningPeriod = null;
		PointType aPointType = null;
		TimeSpanType aTimeSpanType = null;

		String facilityId = facility.getId().toString();

		FolderType aShop = this.kmlJAXBFactory.createFolderType();
		this.document.getAbstractFeatureGroup().add(this.kmlJAXBFactory.createFolder(aShop));
		aShop.setName(facilityId.split("_", 2)[0]);
		aShop.setDescription(facilityId);

		// transform coordinates incl. toggle easting and northing
		CH1903LV03toWGS84 trafo = new CH1903LV03toWGS84();
		Coord northWestCH1903 = new CoordImpl(facility.getCoord().getX(), facility.getCoord().getY());
		Coord northWestWGS84 = trafo.transform(northWestCH1903);

		// have to iterate this over opening times
		int dayCounter = 0;
		for (Day day : days) {
			if (facility.getActivityOptions().get(ACTIVITY_TYPE_SHOP) != null) {
				Set<OpeningTime> dailyOpentimes = facility.getActivityOptions().get(ACTIVITY_TYPE_SHOP).getOpeningTimes(day.getAbbrevEnglish());
				if (dailyOpentimes != null) {
					for (OpeningTime opentime : dailyOpentimes) {

						// build up placemark structure
						aShopOpeningPeriod = this.kmlJAXBFactory.createPlacemarkType();
						aShop.getAbstractFeatureGroup().add(this.kmlJAXBFactory.createPlacemark(aShopOpeningPeriod));
						aShopOpeningPeriod.setStyleUrl(SHOP_STYLE);
						aShopOpeningPeriod.setName(facilityId.split("_", 2)[0]);
						aShopOpeningPeriod.setDescription(facilityId);

						aPointType = this.kmlJAXBFactory.createPointType();
						aShopOpeningPeriod.setAbstractGeometryGroup(this.kmlJAXBFactory.createPoint(aPointType));
						aPointType.getCoordinates().add(northWestWGS84.getX() + "," + northWestWGS84.getY() + ",0.0");

						// transform opening times to GE time primitives
						aTimeSpanType = this.kmlJAXBFactory.createTimeSpanType();
						aShopOpeningPeriod.setAbstractTimePrimitiveGroup(this.kmlJAXBFactory.createAbstractTimePrimitiveGroup(aTimeSpanType));
						aTimeSpanType.setBegin("2008-04-" + Integer.toString(this.MONDAY_DAY + dayCounter) + "T" + Time.writeTime(opentime.getStartTime()) + "+01:00");
						aTimeSpanType.setEnd("2008-04-" + Integer.toString(this.MONDAY_DAY + dayCounter) + "T" + Time.writeTime(opentime.getEndTime()) + "+01:00");
					}
				}
			}
			dayCounter++;
		}
	}

	public void finish() {

		log.info("Writing out KML...");
		try {
			Marshaller marshaller = this.jaxbContext.createMarshaller();
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
			marshaller.marshal(this.kmlJAXBFactory.createKml(this.kml), new FileOutputStream(this.outputFilename));
		} catch (JAXBException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		log.info("Writing out KML...done.");

	}

}
