/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.andreas.bln.ana.events2counts;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import net.opengis.kml._2.DocumentType;
import net.opengis.kml._2.FolderType;
import net.opengis.kml._2.IconStyleType;
import net.opengis.kml._2.KmlType;
import net.opengis.kml._2.LinkType;
import net.opengis.kml._2.ObjectFactory;
import net.opengis.kml._2.PlacemarkType;
import net.opengis.kml._2.PointType;
import net.opengis.kml._2.ScreenOverlayType;
import net.opengis.kml._2.StyleType;
import net.opengis.kml._2.TimeSpanType;
import net.opengis.kml._2.UnitsEnumType;
import net.opengis.kml._2.Vec2Type;

import org.apache.log4j.Logger;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.gbl.MatsimResource;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.counts.Count;
import org.matsim.counts.CountSimComparison;
import org.matsim.counts.Counts;
import org.matsim.counts.algorithms.graphs.CountsGraph;
import org.matsim.counts.algorithms.graphs.CountsLoadCurveGraph;
import org.matsim.pt.counts.PtBiasErrorGraph;
import org.matsim.pt.counts.PtCountSimComparisonWriter;
import org.matsim.pt.counts.PtCountsLoadCurveGraphCreator;
import org.matsim.pt.counts.PtCountsSimRealPerHourGraph;
import org.matsim.vis.kml.KMZWriter;
import org.matsim.vis.kml.MatsimKMLLogo;
import org.matsim.vis.kml.NetworkFeatureFactory;

/**
 * @author dgrether
 */
public class PtCountCountComparisonKMLWriter extends PtCountSimComparisonWriter {
	/**
	 * constant for the name of the stops
	 */
	private static final String STOP = "Stop: ";
	private static final String COUNTVALUE = "Base Case Value: ";
	private static final String MATSIMVALUE = "Scenario Value: ";
	private static final String RELERROR = "Relative Error: ";
	private static final String IMG = "<img src=\"./";
	private static final String IMGEND = "\">";
	private static final String H24OVERVIEW = "24 h overview";
	private static final String DETAILSFROM = "Details from ";
	private static final String OCLOCKTO = " o'clock to ";
	private static final String OCLOCK = " o'clock";
	private static final String ZERO = "0";
	/*
	 * the icons
	 */
	private static final String CROSSICON = "icons/plus.png";
	private static final String MINUSICON = "icons/minus.png";
	/**
	 * the scale for the icons
	 */
	private static final Double ICONSCALE = Double.valueOf(0.5);
	/**
	 * height of the charts
	 */
	private static final int CHARTHEIGHT = 300;
	/**
	 * width of the charts
	 */
	private static final int CHARTWIDTH = 400;
	/**
	 * constant for the file suffix of graphs
	 */
	private static final String PNG = ".png";
	/**
	 * constant for the file name of the CountsSimRealPerHourGraphs
	 */
	private static final String SIMREALGRAPHNAME = "countsSimRealPerHour_";

	// private final Network network;
	private CoordinateTransformation coordTransform = null;
	private ObjectFactory kmlObjectFactory = new ObjectFactory();
	/**
	 * main kml, doc and folder
	 */
	private KmlType mainKml = null;

	private DocumentType mainDoc = null;
	private FolderType mainFolder = null;
	private KMZWriter writer = null;

	private StyleType redCrossStyle;
	private StyleType redMinusStyle;
	private StyleType yellowCrossStyle;
	private StyleType yellowMinusStyle;
	private StyleType greenMinusStyle;
	private StyleType greenCrossStyle;
	private StyleType greyCrossStyle;
	private StyleType greyMinusStyle;

	/**
	 * maps stopids to filenames in the kmz
	 */
	private Map<String, String> boardCountsLoadCurveGraphMap, alightCountsLoadCurveGraphMap;
	private Counts boardCounts, alightCounts;
	private HashMap<String, String> stopIDMap;
	private Map<String, TreeSet<String>> stopID2lineIdMap;
	private boolean writePlacemarkName;

	/** The logging object for this class. */
	private static final Logger log = Logger
			.getLogger(PtCountCountComparisonKMLWriter.class);

	/**
	 * Sets the data to the fields of this class
	 *
	 * @param countSimCompList
	 * @param network
	 * @param coordTransform
	 * @param stopID2lineIdMap
	 * @param stopIDMap
	 */
	public PtCountCountComparisonKMLWriter(
			final List<CountSimComparison> boardCountSimCompList,
			final List<CountSimComparison> alightCountSimCompList,
			final List<CountSimComparison> occupancyCountSimCompList,
			// final Network network,
			final CoordinateTransformation coordTransform,
			final Counts boradCounts, final Counts alightCounts, HashMap<String,String> stopIDMap, Map<String, TreeSet<String>> stopID2lineIdMap, boolean writePlacemarkName) {
		super(boardCountSimCompList, alightCountSimCompList, occupancyCountSimCompList);
		// this.network = network;
		this.coordTransform = coordTransform;
		this.boardCounts = boradCounts;
		this.alightCounts = alightCounts;
		this.stopIDMap = stopIDMap;
		this.stopID2lineIdMap = stopID2lineIdMap;
		this.writePlacemarkName = writePlacemarkName;
	}

	/**
	 * This method initializes the styles for the different icons used.
	 */
	private void createStyles() {

		this.redCrossStyle = kmlObjectFactory.createStyleType();
		this.redCrossStyle.setId("redCrossStyle");
		this.redMinusStyle = kmlObjectFactory.createStyleType();
		this.redMinusStyle.setId("redMinusStyle");
		this.yellowCrossStyle = kmlObjectFactory.createStyleType();
		this.yellowCrossStyle.setId("yellowCrossStyle");
		this.yellowMinusStyle = kmlObjectFactory.createStyleType();
		this.yellowMinusStyle.setId("yellowMinusStyle");
		this.greenCrossStyle = kmlObjectFactory.createStyleType();
		this.greenCrossStyle.setId("greenCrossStyle");
		this.greenMinusStyle = kmlObjectFactory.createStyleType();
		this.greenMinusStyle.setId("greenMinusStyle");
		this.greyCrossStyle = kmlObjectFactory.createStyleType();
		this.greyCrossStyle.setId("greyCrossStyle");
		this.greyMinusStyle = kmlObjectFactory.createStyleType();
		this.greyMinusStyle.setId("greyMinusStyle");

		byte[] red = new byte[] { (byte) 0xFF, (byte) 0x0F, (byte) 0x0F,
				(byte) 0xBE };
		byte[] green = new byte[] { (byte) 0xFF, (byte) 0x14, (byte) 0xDC,
				(byte) 0x0A };
		byte[] yellow = new byte[] { (byte) 0xFF, (byte) 0x14, (byte) 0xE6,
				(byte) 0xE6 };
		byte[] grey = new byte[] { (byte) 0xFF, (byte) 0x42, (byte) 0x42,
				(byte) 0x42 };

		HashMap<StyleType, byte[]> colors = new HashMap<StyleType, byte[]>();
		colors.put(this.redCrossStyle, red);
		colors.put(this.redMinusStyle, red);
		colors.put(this.yellowCrossStyle, yellow);
		colors.put(this.yellowMinusStyle, yellow);
		colors.put(this.greenCrossStyle, green);
		colors.put(this.greenMinusStyle, green);
		colors.put(this.greyCrossStyle, grey);
		colors.put(this.greyMinusStyle, grey);

		HashMap<StyleType, String> hrefs = new HashMap<StyleType, String>();
		hrefs.put(this.redCrossStyle, CROSSICON);
		hrefs.put(this.redMinusStyle, MINUSICON);
		hrefs.put(this.yellowCrossStyle, CROSSICON);
		hrefs.put(this.yellowMinusStyle, MINUSICON);
		hrefs.put(this.greenCrossStyle, CROSSICON);
		hrefs.put(this.greenMinusStyle, MINUSICON);
		hrefs.put(this.greyCrossStyle, CROSSICON);
		hrefs.put(this.greyMinusStyle, MINUSICON);

		for (StyleType styleType : new StyleType[] { this.redCrossStyle,
				this.redMinusStyle, this.yellowCrossStyle,
				this.yellowMinusStyle, this.greenCrossStyle,
				this.greenMinusStyle, this.greyCrossStyle, this.greyMinusStyle }) {

			IconStyleType icon = kmlObjectFactory.createIconStyleType();
			icon.setColor(new byte[] { colors.get(styleType)[0],
					colors.get(styleType)[1], colors.get(styleType)[2],
					colors.get(styleType)[3] });
			icon.setScale(ICONSCALE);

			LinkType link = kmlObjectFactory.createLinkType();
			link.setHref(hrefs.get(styleType));
			icon.setIcon(link);

			styleType.setIconStyle(icon);

			this.mainDoc.getAbstractStyleSelectorGroup().add(
					kmlObjectFactory.createStyle(styleType));
		}
	}

	/**
	 * Writes the data to the file at the path given as String
	 *
	 * @param filename
	 */
	@Override
	public void writeFile(final String filename) {

		// init kml
		this.mainKml = kmlObjectFactory.createKmlType();
		this.mainDoc = kmlObjectFactory.createDocumentType();
		this.mainKml.setAbstractFeatureGroup(kmlObjectFactory
				.createDocument(mainDoc));

		// create the styles and the folders
		createStyles();
		// create a folder
		this.mainFolder = kmlObjectFactory.createFolderType();
		this.mainFolder.setName("Comparison, Iteration " + this.iter);
		this.mainDoc.getAbstractFeatureGroup().add(
				kmlObjectFactory.createFolder(this.mainFolder));
		// the writer
		this.writer = new KMZWriter(filename);

		try {
			// try to create the legend
			this.mainFolder.getAbstractFeatureGroup().add(
					kmlObjectFactory.createScreenOverlay(createLegend()));
		} catch (IOException e) {
			log.error("Cannot add legend to the KMZ file.", e);
		}
		try {
			// add the matsim logo to the kml
			this.mainFolder.getAbstractFeatureGroup().add(
					kmlObjectFactory.createScreenOverlay(MatsimKMLLogo
							.writeMatsimKMLLogo(writer)));
		} catch (IOException e) {
			log.error("Cannot add logo to the KMZ file.", e);
		}

		try {
			// copy required icons to the kmz
			this.writer.addNonKMLFile(MatsimResource
					.getAsInputStream("icons/plus.png"), CROSSICON);
			this.writer.addNonKMLFile(MatsimResource
					.getAsInputStream("icons/minus.png"), MINUSICON);
		} catch (IOException e) {
			log.error("Could not copy copy plus-/minus-icons to the KMZ.", e);
		}

		// prepare folders for simRealPerHour-Graphs (top-left, xy-plots)
		FolderType simRealFolder = kmlObjectFactory.createFolderType();
		simRealFolder.setName("XY Comparison Plots");
		this.mainFolder.getAbstractFeatureGroup().add(
				kmlObjectFactory.createFolder(simRealFolder));

		// error graphs and awtv graph - board
		ScreenOverlayType errorGraphBoard = createBiasErrorGraphBoard(filename);
		errorGraphBoard.setVisibility(Boolean.TRUE);
		this.mainFolder.getAbstractFeatureGroup().add(
				kmlObjectFactory.createScreenOverlay(errorGraphBoard));
		// error graphs and awtv graph - alight
		ScreenOverlayType errorGraphAlight = createBiasErrorGraphAlight(filename);
		errorGraphAlight.setVisibility(Boolean.TRUE);
		this.mainFolder.getAbstractFeatureGroup().add(
				kmlObjectFactory.createScreenOverlay(errorGraphAlight));
		// link graphs
		this.createCountsLoadCurveGraphs();

		// hourly data...
		for (int h = 1; h < 25; h++) {
			// the timespan for this hour
			TimeSpanType timespan = kmlObjectFactory.createTimeSpanType();
			timespan.setBegin("1999-01-01T" + Time.writeTime(((h - 1) * 3600)));
			timespan.setEnd("1999-01-01T" + Time.writeTime((h * 3600)));

			// first add the xyplot ("SimRealPerHourGraph") as overlay
			this.addCountsSimRealPerHourGraphs(simRealFolder, h, timespan);

			// add the placemarks for the links in this hour
			FolderType subfolder = kmlObjectFactory.createFolderType();
			subfolder.setName(createFolderName(h));
			subfolder.setAbstractTimePrimitiveGroup(kmlObjectFactory
					.createTimeSpan(timespan));
			this.mainFolder.getAbstractFeatureGroup().add(
					kmlObjectFactory.createFolder(subfolder));

			writeStopData(this.boardCountComparisonFilter
					.getCountsForHour(Integer.valueOf(h)), subfolder, "board");
			writeStopData(this.alightCountComparisonFilter
					.getCountsForHour(Integer.valueOf(h)), subfolder, "alight");
		}
		finish();
	}

	/**
	 * Creates the string for the foldername
	 *
	 * @param timestep
	 * @return a timestep specific standard string
	 */
	private String createFolderName(final int timestep) {
		StringBuffer buffer = new StringBuffer(30);
		buffer.append("Traffic from ");
		buffer.append(this.timestepToString(timestep - 1));
		buffer.append(" to ");
		buffer.append(this.timestepToString(timestep));
		buffer.append(" o'clock");
		return buffer.toString();
	}

	/**
	 * Creates a legend
	 *
	 * @return a ScreenOverlay read from a file
	 * @throws IOException
	 */
	private ScreenOverlayType createLegend() throws IOException {

		this.writer.addNonKMLFile(MatsimResource
				.getAsInputStream("countsKml/countsLegend240x300.png"),
				"countsLegend.png");
		ScreenOverlayType overlay = kmlObjectFactory.createScreenOverlayType();
		LinkType icon = kmlObjectFactory.createLinkType();
		icon.setHref("./countsLegend.png");
		overlay.setIcon(icon);
		overlay.setName("Legend");
		// place the image bottom left
		Vec2Type overlayXY = kmlObjectFactory.createVec2Type();
		overlayXY.setX(0.0);
		overlayXY.setY(0.0);
		overlayXY.setXunits(UnitsEnumType.FRACTION);
		overlayXY.setYunits(UnitsEnumType.FRACTION);
		overlay.setOverlayXY(overlayXY);
		Vec2Type screenXY = kmlObjectFactory.createVec2Type();
		screenXY.setX(0.02);
		screenXY.setY(0.07);
		screenXY.setXunits(UnitsEnumType.FRACTION);
		screenXY.setYunits(UnitsEnumType.FRACTION);
		overlay.setScreenXY(screenXY);
		return overlay;

	}

	/**
	 * Creates a placemark
	 *
	 * @param stopid
	 * @param csc
	 * @param relativeError
	 * @param timestep
	 * @return the Placemark instance with description and name set
	 */
	private PlacemarkType createPlacemark(final String stopid,
			final CountSimComparison csc, final double relativeError,
			final int timestep, String countsType) {
		StringBuffer stringBuffer = new StringBuffer();
		PlacemarkType placemark = kmlObjectFactory.createPlacemarkType();
		stringBuffer.delete(0, stringBuffer.length());
		stringBuffer.append(STOP);
		stringBuffer.append(stopid);
		placemark.setDescription(createPlacemarkDescription(stopid, csc,
				relativeError, timestep, countsType));

		//		if(this.stopIDMap.get(stopid) == null){
//			log.warn("Stop " + stopid + " has no name!?");
//		} else {
//			placemark.setName(this.stopIDMap.get(stopid));
//		}

		StringBuffer name = new StringBuffer();
		if (countsType.equals("board")) {
			name.append("B: ");
		} else {
			name.append("A: ");
		}
		if(this.stopID2lineIdMap.get(stopid) != null){
			for (String line : this.stopID2lineIdMap.get(stopid)) {
				name.append(line + " ");
			}
		} else {
			log.warn(stopid + " is not served by any line!?");
		}
		if(this.writePlacemarkName == true){
			placemark.setName(name.toString());
		}

		return placemark;
	}

	/**
	 * This method writes all the data for each of the links/counts to the kml
	 * document.
	 *
	 * @param countSimComparisonList
	 *            provides "the data"
	 * @param folder
	 *            The folder to which to add the data in the kml-file.
	 * @param counts
	 */
	private void writeStopData(
			final List<CountSimComparison> countSimComparisonList,
			final FolderType folder, String type) {
		Id stopid;
		PlacemarkType placemark;
		double relativeError;
		Coord coord;
		PointType point;
		for (CountSimComparison csc : countSimComparisonList) {
			stopid = csc.getId();
			// link = this.network.getLinks().get(stopid);
			Count count;
			if (type.equals("board"))
				count = this.boardCounts.getCount(stopid);
			else
				count = this.alightCounts.getCount(stopid);
			coord = this.coordTransform.transform(count.getCoord());
			relativeError = csc.calculateRelativeError();
			// build placemark
			placemark = createPlacemark(stopid.toString(), csc, relativeError,
					csc.getHour(), type);
			point = kmlObjectFactory.createPointType();
			point.getCoordinates().add(
					Double.toString(coord.getX()) + ","
							+ Double.toString(coord.getY()) + ",0.0");
			placemark.setAbstractGeometryGroup(kmlObjectFactory
					.createPoint(point));

			// first check if we got any counts
			if (csc.getSimulationValue() == 0.0 && csc.getCountValue() == 0.0){
				// no, so place something neutral, e.g. grey dot
				placemark.setStyleUrl(this.greyMinusStyle.getId());
			} else {
				// yes, we have so decide what color and type
				// cross
				if (csc.getSimulationValue() > csc.getCountValue()) {
					if (csc.getSimulationValue() < csc.getCountValue() * 1.5) {
						placemark.setStyleUrl(this.greenCrossStyle.getId());
					} else if (csc.getSimulationValue() < csc.getCountValue() * 2) {
						placemark.setStyleUrl(this.yellowCrossStyle.getId());
					} else {
						placemark.setStyleUrl(this.redCrossStyle.getId());
					}
				}
				// minus
				else {
					if (csc.getSimulationValue() > csc.getCountValue() * 0.75) {
						placemark.setStyleUrl("#greenMinusStyle");
					} else if (csc.getSimulationValue() > csc.getCountValue() * 0.5) {
						placemark.setStyleUrl("#yellowMinusStyle");
					} else {
						placemark.setStyleUrl("#redMinusStyle");
					}
				}
			}
			folder.getAbstractFeatureGroup().add(
					kmlObjectFactory.createPlacemark(placemark));
		}
	}

	// /**
	// * Calculates the position of a placemark in a way that it is 40 % of the
	// * link length away from the node where the link starts.
	// *
	// * @param l
	// * @return the CoordI instance
	// */
	// private Coord calculatePlacemarkPosition(final Link l) {
	// Coord coordFrom = l.getFromNode().getCoord();
	// Coord coordTo = l.getToNode().getCoord();
	// double xDiff = coordTo.getX() - coordFrom.getX();
	// double yDiff = coordTo.getY() - coordFrom.getY();
	// double length = Math.sqrt((xDiff * xDiff) + (yDiff * yDiff));
	// double scale = 0.4;
	// scale = l.getLength() * scale;
	// Coord vec = new CoordImpl(coordFrom.getX() + (xDiff * scale / length),
	// coordFrom.getY() + (yDiff * scale / length));
	// return vec;
	// }

	/**
	 *
	 * @param stopid
	 * @param csc
	 * @param relativeError
	 * @param timestep
	 * @param countsType
	 *            board or alight
	 * @return A String containing the description for each placemark
	 */
	private String createPlacemarkDescription(final String stopid,
			final CountSimComparison csc, final double relativeError,
			final int timestep, String countsType) {
		StringBuffer buffer = new StringBuffer(100);
		// buffer.append(NetworkFeatureFactory.STARTCDATA);
		// buffer.append(STARTH1);
		// buffer.append(LINK);
		// buffer.append(linkid);
		// buffer.append(ENDH1);
		buffer.append(NetworkFeatureFactory.STARTH2);
		buffer.append(STOP);
		buffer.append(stopid + "\t" + countsType + "ing" + "\t" + this.stopIDMap.get(stopid));
		buffer.append(NetworkFeatureFactory.ENDH2);
		buffer.append(NetworkFeatureFactory.STARTH3);
		buffer.append(H24OVERVIEW + " for line(s): ");
		if(this.stopID2lineIdMap.get(stopid) != null){
			for (String line : this.stopID2lineIdMap.get(stopid)) {
				buffer.append(line + " ");
			}
		} else {
			log.warn(stopid + " is not served by any line!?");
		}

		buffer.append(NetworkFeatureFactory.ENDH3);
		buffer.append(NetworkFeatureFactory.STARTP);

		if (countsType.equals("board")) {
			buffer.append(IMG);
			buffer.append(this.boardCountsLoadCurveGraphMap.get(stopid + "b"));
			buffer.append(IMGEND);
		} else {
			buffer.append(IMG);
			buffer.append(this.alightCountsLoadCurveGraphMap.get(stopid + "a"));
			buffer.append(IMGEND);
		}

		buffer.append(NetworkFeatureFactory.ENDP);
		buffer.append(NetworkFeatureFactory.STARTH3);
		buffer.append(DETAILSFROM);
		buffer.append((this.timestepToString(timestep - 1)));
		buffer.append(OCLOCKTO);
		buffer.append(this.timestepToString(timestep));
		buffer.append(OCLOCK);
		buffer.append(NetworkFeatureFactory.ENDH3);
		buffer.append(NetworkFeatureFactory.STARTP);
		buffer.append(COUNTVALUE);
		buffer.append(csc.getCountValue());
		buffer.append(NetworkFeatureFactory.ENDP);
		buffer.append(NetworkFeatureFactory.STARTP);
		buffer.append(MATSIMVALUE);
		buffer.append(csc.getSimulationValue());
		buffer.append(NetworkFeatureFactory.ENDP);
		buffer.append(NetworkFeatureFactory.STARTP);
		buffer.append(RELERROR);
		buffer.append(relativeError);
		buffer.append(NetworkFeatureFactory.ENDP);
		// buffer.append(NetworkFeatureFactory.ENDCDATA);
		return buffer.toString();
	}

	/**
	 * @param timestep
	 * @return A two digits string containing the given timestep
	 */
	private String timestepToString(final int timestep) {
		if (timestep < 10) {
			StringBuffer buffer = new StringBuffer();
			buffer.append(ZERO);
			buffer.append(Integer.toString(timestep));
			return buffer.toString();
		}
		return Integer.toString(timestep);
	}

	/**
	 * Creates CountsSimRealPerHourGraphs and adds them to the kmz in the given
	 * folder. The creation of the graphs is only done if the map attribute of
	 * this class for CountsSimRealPerHourGraphs is null.
	 *
	 * @param folder
	 * @param timestep
	 * @param timespan
	 */
	private void addCountsSimRealPerHourGraphs(final FolderType folder,
			final int timestep, final TimeSpanType timespan) {
		StringBuffer filenameBoard, filenameAlight;
		ScreenOverlayType overlayBoard, overlayAlight;

		try {
			// add the file to the kmz
			filenameBoard = new StringBuffer("board-" + SIMREALGRAPHNAME
					+ Integer.toString(timestep) + PNG);
			filenameAlight = new StringBuffer("alight-" + SIMREALGRAPHNAME
					+ Integer.toString(timestep) + PNG);

			PtCountsSimRealPerHourGraph graphBoard = new PtCountsSimRealPerHourGraph(
					this.boardCountComparisonFilter.getCountsForHour(null),
					this.iter, filenameBoard.toString(), PtCountsType.Boarding);
			PtCountsSimRealPerHourGraph graphAlight = new PtCountsSimRealPerHourGraph(
					this.alightCountComparisonFilter.getCountsForHour(null),
					this.iter, filenameAlight.toString(), PtCountsType.Alighting);

			graphBoard.createChart(timestep);
			graphAlight.createChart(timestep);

			this.writeChartToKmz(filenameBoard.toString(), graphBoard
					.getChart());
			this.writeChartToKmz(filenameAlight.toString(), graphAlight
					.getChart());

			// and link with the overlay
			overlayBoard = kmlObjectFactory.createScreenOverlayType();
			LinkType iconBoard = kmlObjectFactory.createLinkType();
			iconBoard.setHref("./" + filenameBoard.toString());
			overlayBoard.setIcon(iconBoard);
			overlayBoard.setName(graphBoard.getChartTitle());

			// place the image top right
			Vec2Type overlayXYBoard = kmlObjectFactory.createVec2Type();
			overlayXYBoard.setX(1.0);
			overlayXYBoard.setY(1.0);
			overlayXYBoard.setXunits(UnitsEnumType.FRACTION);
			overlayXYBoard.setYunits(UnitsEnumType.FRACTION);
			overlayBoard.setOverlayXY(overlayXYBoard);
			Vec2Type screenXYBoard = kmlObjectFactory.createVec2Type();
			screenXYBoard.setX(0.98);
			screenXYBoard.setY(0.98);
			screenXYBoard.setXunits(UnitsEnumType.FRACTION);
			screenXYBoard.setYunits(UnitsEnumType.FRACTION);
			overlayBoard.setScreenXY(screenXYBoard);
			overlayBoard.setAbstractTimePrimitiveGroup(kmlObjectFactory
					.createTimeSpan(timespan));
			// add the overlay to the folder
			folder.getAbstractFeatureGroup().add(
					kmlObjectFactory.createScreenOverlay(overlayBoard));

			// and link with the overlay
			overlayAlight = kmlObjectFactory.createScreenOverlayType();
			LinkType iconAlight = kmlObjectFactory.createLinkType();
			iconAlight.setHref("./" + filenameAlight.toString());
			overlayAlight.setIcon(iconAlight);
			overlayAlight.setName(graphAlight.getChartTitle());

			// place the image top right
			Vec2Type overlayXYAlight = kmlObjectFactory.createVec2Type();
			overlayXYAlight.setX(1.0);
			overlayXYAlight.setY(0.6);
			overlayXYAlight.setXunits(UnitsEnumType.FRACTION);
			overlayXYAlight.setYunits(UnitsEnumType.FRACTION);
			overlayAlight.setOverlayXY(overlayXYAlight);
			Vec2Type screenXYAlight = kmlObjectFactory.createVec2Type();
			screenXYAlight.setX(0.98);
			screenXYAlight.setY(0.58);
			screenXYAlight.setXunits(UnitsEnumType.FRACTION);
			screenXYAlight.setYunits(UnitsEnumType.FRACTION);
			overlayAlight.setScreenXY(screenXYAlight);
			overlayAlight.setAbstractTimePrimitiveGroup(kmlObjectFactory
					.createTimeSpan(timespan));
			// add the overlay to the folder
			folder.getAbstractFeatureGroup().add(
					kmlObjectFactory.createScreenOverlay(overlayAlight));

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Creates CountsLoadCurveGraphs for each stop and puts them in the kmz as
	 * pngs
	 */
	private void createCountsLoadCurveGraphs() {
		PtCountsLoadCurveGraphCreator cgc = new PtCountsLoadCurveGraphCreator(
				"");
		List<CountsGraph> graphsBoard = cgc.createGraphs(
				this.boardCountComparisonFilter.getCountsForHour(null),
				this.iter);
		List<CountsGraph> graphsAlight = cgc.createGraphs(
				this.alightCountComparisonFilter.getCountsForHour(null),
				this.iter);

		this.boardCountsLoadCurveGraphMap = new HashMap<String, String>(
				graphsBoard.size());
		this.alightCountsLoadCurveGraphMap = new HashMap<String, String>(
				graphsAlight.size());

		String stopId;
		String filename;

		for (CountsGraph cg : graphsBoard) {
			try {
				stopId = ((CountsLoadCurveGraph) cg).getLinkId();
				filename = stopId + "b" + PNG;
				writeChartToKmz(filename, cg.getChart());
				this.boardCountsLoadCurveGraphMap.put(stopId + "b", filename);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		for (CountsGraph cg : graphsAlight) {
			try {
				stopId = ((CountsLoadCurveGraph) cg).getLinkId();
				filename = stopId + "a" + PNG;
				writeChartToKmz(filename, cg.getChart());
				this.alightCountsLoadCurveGraphMap.put(stopId + "a", filename);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Writes the given JFreeChart to the kmz file specified for the kmz writer
	 * attribute of this class.
	 *
	 * @param filename
	 *            the filename to use in the kmz
	 * @param chart
	 * @throws IOException
	 */
	private void writeChartToKmz(final String filename, final JFreeChart chart)
			throws IOException {
		byte[] img;
		img = ChartUtilities.encodeAsPNG(chart.createBufferedImage(CHARTWIDTH,
				CHARTHEIGHT));
		this.writer.addNonKMLFile(img, filename);
	}

	/**
	 * Creates the CountsErrorGraph for all the data
	 *
	 * @param kmlFilename
	 *            the filename of the kml file
	 * @param visible
	 *            true if initially visible
	 * @return the ScreenOverlay Feature
	 */
	private ScreenOverlayType createBiasErrorGraphBoard(String kmlFilename) {
		int index = kmlFilename.lastIndexOf(System
				.getProperty("file.separator"));
		if (index == -1) {
			index = kmlFilename.lastIndexOf('/');
		}
		String outdir;
		if (index == -1) {
			outdir = "";
		} else {
			outdir = kmlFilename.substring(0, index)
					+ System.getProperty("file.separator");
		}
		// ------------------------------------------------------------------------------

		PtBiasErrorGraph epBoard = new PtBiasErrorGraph(
				this.boardCountComparisonFilter.getCountsForHour(null),
				this.iter, null, "error graph - boarding");
		epBoard.createChart(0);

		double[] meanErrorBoard = epBoard.getMeanRelError();
		double[] meanBiasBoard = epBoard.getMeanAbsBias();

		String fileBoard = outdir + "biasErrorGraphDataBoard.txt";
		log.info("writing chart data to "
				+ new File(fileBoard).getAbsolutePath());
		try {
			BufferedWriter bwriter = IOUtils.getBufferedWriter(fileBoard);
			StringBuffer buffer = new StringBuffer(200);
			buffer.append("hour \t mean relative error \t mean absolute bias");
			bwriter.write(buffer.toString());
			bwriter.newLine();
			for (int i = 0; i < meanErrorBoard.length; i++) {
				buffer.delete(0, buffer.length());
				buffer.append(i + 1);
				buffer.append('\t');
				buffer.append(meanErrorBoard[i]);
				buffer.append('\t');
				buffer.append(meanBiasBoard[i]);
				bwriter.write(buffer.toString());
				bwriter.newLine();
			}
			bwriter.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		String chartBoard = "errorGraphErrorBiasBoard.png";
		try {
			writeChartToKmz(chartBoard, epBoard.getChart());
			return createOverlayBottomRight(chartBoard,
					"Error Graph [Error/Bias]");
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	private ScreenOverlayType createBiasErrorGraphAlight(String kmlFilename) {
		int index = kmlFilename.lastIndexOf(System.getProperty("file.separator"));
		if (index == -1) {
			index = kmlFilename.lastIndexOf('/');
		}
		String outdir;
		if (index == -1) {
			outdir = "";
		} else {
			outdir = kmlFilename.substring(0, index)
					+ System.getProperty("file.separator");
		}
		// ------------------------------------------------------------------------------
		PtBiasErrorGraph epAlight = new PtBiasErrorGraph(
				this.alightCountComparisonFilter.getCountsForHour(null),
				this.iter, null, "error graph - Alighting");
		epAlight.createChart(0);

		double[] meanErrorAlight = epAlight.getMeanRelError();
		double[] meanBiasAlight = epAlight.getMeanAbsBias();

		String fileAlight = outdir + "biasErrorGraphDataAlight.txt";
		log.info("writing chart data to "
				+ new File(fileAlight).getAbsolutePath());
		try {
			BufferedWriter bwriter = IOUtils.getBufferedWriter(fileAlight);
			StringBuffer buffer = new StringBuffer(200);
			buffer.append("hour \t mean relative error \t mean absolute bias");
			bwriter.write(buffer.toString());
			bwriter.newLine();
			for (int i = 0; i < meanErrorAlight.length; i++) {
				buffer.delete(0, buffer.length());
				buffer.append(i + 1);
				buffer.append('\t');
				buffer.append(meanErrorAlight[i]);
				buffer.append('\t');
				buffer.append(meanBiasAlight[i]);
				bwriter.write(buffer.toString());
				bwriter.newLine();
			}
			bwriter.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		String chartAlight = "errorGraphErrorBiasAlight.png";
		try {
			writeChartToKmz(chartAlight, epAlight.getChart());
			return createOverlayBottomRight(chartAlight,
					"Error Graph [Error/Bias]");
		} catch (IOException e) {
			e.printStackTrace();
		}

		return null;
	}

	private ScreenOverlayType createOverlayBottomRight(final String fileName,
			final String overlayName) {
		ScreenOverlayType overlay = kmlObjectFactory.createScreenOverlayType();
		LinkType icon1 = kmlObjectFactory.createLinkType();
		icon1.setHref("./" + fileName);
		overlay.setIcon(icon1);
		overlay.setName(overlayName);
		// place the image bottom right
		Vec2Type overlayXY = kmlObjectFactory.createVec2Type();
		overlayXY.setX(1.0);
		overlayXY.setY(0.0);
		overlayXY.setXunits(UnitsEnumType.FRACTION);
		overlayXY.setYunits(UnitsEnumType.FRACTION);
		overlay.setOverlayXY(overlayXY);
		Vec2Type screenXY = kmlObjectFactory.createVec2Type();
		screenXY.setX(0.98);
		screenXY.setY(0.1);
		screenXY.setXunits(UnitsEnumType.FRACTION);
		screenXY.setYunits(UnitsEnumType.FRACTION);
		overlay.setScreenXY(screenXY);
		return overlay;
	}

	/**
	 * to be called when the kml stream shall be closed.
	 */
	private void finish() {
		this.writer.writeMainKml(this.mainKml);
		this.writer.close();
	}
}
