/* *********************************************************************** *
 * project: org.matsim.*
 * CountSimComparisonKMLWriter.java
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

package org.matsim.counts.algorithms;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.gbl.MatsimResource;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.counts.CountSimComparison;
import org.matsim.counts.Counts;
import org.matsim.counts.algorithms.graphs.BiasErrorGraph;
import org.matsim.counts.algorithms.graphs.BiasNormalizedErrorGraph;
import org.matsim.counts.algorithms.graphs.BoxPlotErrorGraph;
import org.matsim.counts.algorithms.graphs.BoxPlotNormalizedErrorGraph;
import org.matsim.counts.algorithms.graphs.CountsGEHCurveGraph;
import org.matsim.counts.algorithms.graphs.CountsGEHCurveGraphCreator;
import org.matsim.counts.algorithms.graphs.CountsGraph;
import org.matsim.counts.algorithms.graphs.CountsLoadCurveGraph;
import org.matsim.counts.algorithms.graphs.CountsLoadCurveGraphCreator;
import org.matsim.counts.algorithms.graphs.CountsSimReal24Graph;
import org.matsim.counts.algorithms.graphs.CountsSimRealPerHourGraph;
import org.matsim.vis.kml.KMZWriter;
import org.matsim.vis.kml.MatsimKMLLogo;
import org.matsim.vis.kml.NetworkFeatureFactory;

import net.opengis.kml.v_2_2_0.DocumentType;
import net.opengis.kml.v_2_2_0.FolderType;
import net.opengis.kml.v_2_2_0.IconStyleType;
import net.opengis.kml.v_2_2_0.KmlType;
import net.opengis.kml.v_2_2_0.LinkType;
import net.opengis.kml.v_2_2_0.ObjectFactory;
import net.opengis.kml.v_2_2_0.PlacemarkType;
import net.opengis.kml.v_2_2_0.PointType;
import net.opengis.kml.v_2_2_0.ScreenOverlayType;
import net.opengis.kml.v_2_2_0.StyleType;
import net.opengis.kml.v_2_2_0.TimeSpanType;
import net.opengis.kml.v_2_2_0.UnitsEnumType;
import net.opengis.kml.v_2_2_0.Vec2Type;

/**
 * @author dgrether
 */
public class CountSimComparisonKMLWriter<T> extends CountSimComparisonWriter {
	/**
	 * constant for the name of the links
	 */
	private static final String LINK = "Link: ";
	private static final String CSID = "Count Station: ";

	private static final String COUNTVALUE = "Count Value: ";
	private static final String MATSIMVALUE = "MATSim Value: ";
	private static final String RELERROR = "Relative Error: ";
	private static final String NORMRELERROR = "Normalized Relative Error: ";
	private static final String GEH = "GEH: ";
	private static final String IMG = "<img src=\"../";
	private static final String IMGEND = "\">";
	private static final String H24OVERVIEW = "24 h overview";
	private static final String DETAILSFROM = "Details from ";
	private static final String OCLOCKTO = " o'clock to ";
	private static final String OCLOCK = " o'clock";
	private static final String ZERO = "0";

	private static final NumberFormat nf = new DecimalFormat("#.#");
	
	/**
	 * the icons
	 */
	private static final String CIRCLEICON = "icons/circle.png";
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
	 * width of the  charts
	 */
	private static final int CHARTWIDTH = 400;
	/**
	 * constant for the file suffix of graphs
	 */
	private static final String PNG = ".png";
	private final String graphname;

	private final Network network;
	private final Counts<T> counts;
	
	private CoordinateTransformation coordTransform = null;
	private ObjectFactory kmlObjectFactory = new ObjectFactory();
	/**
	 * main kml, doc and folder
	 */
	private KmlType mainKml = null;

	private DocumentType mainDoc = null;
	private FolderType mainFolder = null;
	private KMZWriter writer = null;

	private StyleType redCircleStyle;
	private StyleType redCrossStyle;
	private StyleType redMinusStyle;
	private StyleType yellowCircleStyle;
	private StyleType yellowCrossStyle;
	private StyleType yellowMinusStyle;
	private StyleType greenMinusStyle;
	private StyleType greenCircleStyle;
	private StyleType greenCrossStyle;
	private StyleType greyCircleStyle;
	private StyleType greyCrossStyle;
	private StyleType greyMinusStyle;
	
	/**
	 * maps linkIds to filenames in the kmz
	 */
	private Map<String, String> countsLoadCurveGraphMap;
	private Map<String, String> countsGEHCurveGraphMap;

	/** The logging object for this class. */
	private static final Logger log = Logger.getLogger(CountSimComparisonKMLWriter.class);

	/**
	 * Sets the data to the fields of this class.  We either accept "network" or "counts" to localize the counting stations.
	 */
	public CountSimComparisonKMLWriter(final List<CountSimComparison> countSimCompList, final Network network, final CoordinateTransformation coordTransform) {
		super(countSimCompList);
		this.network = network;
		this.counts = null;
		this.coordTransform = coordTransform;
		this.graphname = "countsSimRealPerHour_";
	}
	/**
	 * Sets the data to the fields of this class.  We either accept "network" or "counts" to localize the counting stations.
	 * @param graphname TODO
	 */
	public CountSimComparisonKMLWriter(final List<CountSimComparison> countSimCompList, final Counts<T> counts, final CoordinateTransformation coordTransform, String graphname) {
		super(countSimCompList);
		this.network = null;
		this.counts = counts;
		this.coordTransform = coordTransform;
		this.graphname = graphname;
	}

	/**
	 * This method initializes the styles for the different icons used.
	 */
	private void createStyles() {

		this.redCircleStyle = this.kmlObjectFactory.createStyleType(); this.redCircleStyle.setId("redCircleStyle");
		this.redCrossStyle = this.kmlObjectFactory.createStyleType(); this.redCrossStyle.setId("redCrossStyle");
		this.redMinusStyle = this.kmlObjectFactory.createStyleType(); this.redMinusStyle.setId("redMinusStyle");
		this.yellowCircleStyle = this.kmlObjectFactory.createStyleType(); this.yellowCircleStyle.setId("yellowCircleStyle");
		this.yellowCrossStyle = this.kmlObjectFactory.createStyleType(); this.yellowCrossStyle.setId("yellowCrossStyle");
		this.yellowMinusStyle = this.kmlObjectFactory.createStyleType(); this.yellowMinusStyle.setId("yellowMinusStyle");
		this.greenCircleStyle = this.kmlObjectFactory.createStyleType(); this.greenCircleStyle.setId("greenCircleStyle");
		this.greenCrossStyle = this.kmlObjectFactory.createStyleType(); this.greenCrossStyle.setId("greenCrossStyle");
		this.greenMinusStyle = this.kmlObjectFactory.createStyleType(); this.greenMinusStyle.setId("greenMinusStyle");
		this.greyCircleStyle = this.kmlObjectFactory.createStyleType(); this.greyCircleStyle.setId("greyCircleStyle");
		this.greyCrossStyle = this.kmlObjectFactory.createStyleType(); this.greyCrossStyle.setId("greyCrossStyle");
		this.greyMinusStyle = this.kmlObjectFactory.createStyleType(); this.greyMinusStyle.setId("greyMinusStyle");

		byte[] red = new byte[]{(byte) 0xFF, (byte) 0x0F, (byte) 0x0F, (byte) 0xBE};
		byte[] green = new byte[]{(byte) 0xFF, (byte) 0x14, (byte) 0xDC, (byte) 0x0A};
		byte[] yellow = new byte[]{(byte) 0xFF, (byte) 0x14, (byte) 0xE6, (byte) 0xE6};
		byte[] grey = new byte[]{(byte) 0xFF, (byte) 0x42, (byte) 0x42, (byte) 0x42};

		HashMap<StyleType, byte[]> colors = new HashMap<StyleType, byte[]>();
		colors.put(this.redCircleStyle, red);
		colors.put(this.redCrossStyle, red);
		colors.put(this.redMinusStyle, red);
		colors.put(this.yellowCircleStyle, yellow);
		colors.put(this.yellowCrossStyle, yellow);
		colors.put(this.yellowMinusStyle, yellow);
		colors.put(this.greenCircleStyle, green);
		colors.put(this.greenCrossStyle, green);
		colors.put(this.greenMinusStyle, green);
		colors.put(this.greyCircleStyle, grey);
		colors.put(this.greyCrossStyle, grey);
		colors.put(this.greyMinusStyle, grey);

		HashMap<StyleType, String> hrefs = new HashMap<StyleType, String>();
		hrefs.put(this.redCircleStyle, CIRCLEICON);
		hrefs.put(this.redCrossStyle, CROSSICON);
		hrefs.put(this.redMinusStyle, MINUSICON);
		hrefs.put(this.yellowCircleStyle, CIRCLEICON);
		hrefs.put(this.yellowCrossStyle, CROSSICON);
		hrefs.put(this.yellowMinusStyle, MINUSICON);
		hrefs.put(this.greenCircleStyle, CIRCLEICON);
		hrefs.put(this.greenCrossStyle, CROSSICON);
		hrefs.put(this.greenMinusStyle, MINUSICON);
		hrefs.put(this.greyCircleStyle, CIRCLEICON);
		hrefs.put(this.greyCrossStyle, CROSSICON);
		hrefs.put(this.greyMinusStyle, MINUSICON);

		for (StyleType styleType : new StyleType[]{
				this.redCircleStyle, this.redCrossStyle, this.redMinusStyle, this.yellowCircleStyle, this.yellowCrossStyle, this.yellowMinusStyle,
				this.greenCircleStyle, this.greenCrossStyle, this.greenMinusStyle, this.greyCircleStyle, this.greyCrossStyle, this.greyMinusStyle}) {

			IconStyleType icon = this.kmlObjectFactory.createIconStyleType();
			icon.setColor(
					new byte[]{
							colors.get(styleType)[0],
							colors.get(styleType)[1],
							colors.get(styleType)[2],
							colors.get(styleType)[3]});
			icon.setScale(ICONSCALE);

			LinkType link = this.kmlObjectFactory.createLinkType();
			link.setHref(hrefs.get(styleType));
			icon.setIcon(link);

			styleType.setIconStyle(icon);

			this.mainDoc.getAbstractStyleSelectorGroup().add(this.kmlObjectFactory.createStyle(styleType));
		}
	}

	/**
	 * Writes the data to the file at the path given as String
	 *
	 * @param filename
	 */
	@Override
	public void writeFile(final String filename) {
		log.info("Writing google earth file to " + filename);
		// init kml
		this.mainKml = this.kmlObjectFactory.createKmlType();
		this.mainDoc = this.kmlObjectFactory.createDocumentType();
		this.mainKml.setAbstractFeatureGroup(this.kmlObjectFactory.createDocument(mainDoc));

		// create the styles and the folders
		createStyles();
		// create a folder
		this.mainFolder = this.kmlObjectFactory.createFolderType();
		this.mainFolder.setName("Comparison, Iteration " + this.iterationNumber);
		this.mainDoc.getAbstractFeatureGroup().add(this.kmlObjectFactory.createFolder(this.mainFolder));
		// the writer
		this.writer = new KMZWriter(filename);

		try {
			//try to create the legend
			this.mainFolder.getAbstractFeatureGroup().add(this.kmlObjectFactory.createScreenOverlay(createLegend()));
			this.mainFolder.getAbstractFeatureGroup().add(this.kmlObjectFactory.createScreenOverlay(createLegendNormalized()));
			this.mainFolder.getAbstractFeatureGroup().add(this.kmlObjectFactory.createScreenOverlay(createLegendGEH()));
		} catch (IOException e) {
			log.error("Cannot add legend to the KMZ file.", e);
		}
		try {
			//add the matsim logo to the kml
			this.mainFolder.getAbstractFeatureGroup().add(this.kmlObjectFactory.createScreenOverlay(MatsimKMLLogo.writeMatsimKMLLogo(writer)));
		} catch (IOException e) {
			log.error("Cannot add logo to the KMZ file.", e);
		}

		try {
			// copy required icons to the kmz
			this.writer.addNonKMLFile(MatsimResource.getAsInputStream("icons/circle.png"), CIRCLEICON);
			this.writer.addNonKMLFile(MatsimResource.getAsInputStream("icons/plus.png"), CROSSICON);
			this.writer.addNonKMLFile(MatsimResource.getAsInputStream("icons/minus.png"), MINUSICON);
		} catch (IOException e) {
			log.error("Could not copy copy plus-/minus-icons to the KMZ.", e);
		}

		// prepare folders for simRealPerHour-Graphs (top-left, xy-plots)
		FolderType simRealFolder = this.kmlObjectFactory.createFolderType();
		simRealFolder.setName("XY Comparison Plots");
		this.mainFolder.getAbstractFeatureGroup().add(this.kmlObjectFactory.createFolder(simRealFolder));

		// error graphs and awtv graph
		{
			ScreenOverlayType errorGraph = createBiasErrorGraph(filename);
			errorGraph.setVisibility(Boolean.TRUE);
			this.mainFolder.getAbstractFeatureGroup().add(this.kmlObjectFactory.createScreenOverlay(errorGraph));
		}
		{
			ScreenOverlayType errorGraph = createBiasNormalizedErrorGraph(filename);
			errorGraph.setVisibility(Boolean.FALSE);
			this.mainFolder.getAbstractFeatureGroup().add(this.kmlObjectFactory.createScreenOverlay(errorGraph));
		}
		{
			ScreenOverlayType errorGraph = createBoxPlotErrorGraph();
			if (errorGraph != null) {
				errorGraph.setVisibility(Boolean.FALSE);
				this.mainFolder.getAbstractFeatureGroup().add(this.kmlObjectFactory.createScreenOverlay(errorGraph));
			}
		}
		{
			ScreenOverlayType errorGraph = createBoxPlotNormalizedErrorGraph();
			if (errorGraph != null) {
				errorGraph.setVisibility(Boolean.FALSE);
				this.mainFolder.getAbstractFeatureGroup().add(this.kmlObjectFactory.createScreenOverlay(errorGraph));
			}
		}
		{
			ScreenOverlayType awtv = null;
			try {
				awtv=this.createAWTVGraph();
			} catch (Exception ee) {
				log.warn("generating awtv (average weekday traffic volumes) graph failed; printing stacktrace but continuing anyways ...");
				for (int ii = 0; ii < ee.getStackTrace().length; ii++) {
					log.info(ee.getStackTrace()[ii].toString());
				}
			}
			if (awtv != null) {
				awtv.setVisibility(Boolean.FALSE);
				this.mainFolder.getAbstractFeatureGroup().add(this.kmlObjectFactory.createScreenOverlay(awtv));
			}
		}

		// link graphs
		this.createCountsLoadCurveGraphs();
		this.createCountsGEHCurveGraphs();

		// hourly data...
		FolderType folderRelative = this.kmlObjectFactory.createFolderType();
		folderRelative.setName("Relative Values");
		this.mainFolder.getAbstractFeatureGroup().add(this.kmlObjectFactory.createFolder(folderRelative));
		
		FolderType folderNormalizedRelative = this.kmlObjectFactory.createFolderType();
		folderNormalizedRelative.setName("Normalized Relative Values");
		folderNormalizedRelative.setVisibility(Boolean.FALSE);
		this.mainFolder.getAbstractFeatureGroup().add(this.kmlObjectFactory.createFolder(folderNormalizedRelative));
		
		FolderType folderGEH = kmlObjectFactory.createFolderType();
		folderGEH.setName("GEH Values");
		folderGEH.setVisibility(Boolean.FALSE);
		this.mainFolder.getAbstractFeatureGroup().add(this.kmlObjectFactory.createFolder(folderGEH));
		
		for (int h = 1; h < 25; h++) {
			// the timespan for this hour
			TimeSpanType timespan = this.kmlObjectFactory.createTimeSpanType();
			timespan.setBegin("1999-01-01T" + Time.writeTime(((h - 1) * 3600)));
			timespan.setEnd("1999-01-01T" + Time.writeTime((h * 3600)));

			// first add the xyplot ("SimRealPerHourGraph") as overlay
			this.addCountsSimRealPerHourGraphs(simRealFolder, h, timespan);

			// add the placemarks for the links in this hour
			FolderType subfolderRelative = this.kmlObjectFactory.createFolderType();
			subfolderRelative.setName(createFolderName(h));
			subfolderRelative.setAbstractTimePrimitiveGroup(this.kmlObjectFactory.createTimeSpan(timespan));
			folderRelative.getAbstractFeatureGroup().add(this.kmlObjectFactory.createFolder(subfolderRelative));

			FolderType subfolderNormalizedRelative = this.kmlObjectFactory.createFolderType();
			subfolderNormalizedRelative.setName(createFolderName(h));
			subfolderNormalizedRelative.setAbstractTimePrimitiveGroup(this.kmlObjectFactory.createTimeSpan(timespan));
			folderNormalizedRelative.getAbstractFeatureGroup().add(this.kmlObjectFactory.createFolder(subfolderNormalizedRelative));
			
			FolderType subfolderGEH = this.kmlObjectFactory.createFolderType();
			subfolderGEH.setName(createFolderName(h));
			subfolderGEH.setAbstractTimePrimitiveGroup(this.kmlObjectFactory.createTimeSpan(timespan));
			subfolderGEH.setVisibility(Boolean.FALSE);
			folderGEH.getAbstractFeatureGroup().add(this.kmlObjectFactory.createFolder(subfolderGEH));
			
			writeLinkData(this.countComparisonFilter.getCountsForHour(Integer.valueOf(h)), subfolderRelative, subfolderNormalizedRelative, subfolderGEH);
		}
		
		finish();
	}

	/**
	 * Creates the string for the foldername
	 * @param timestep
	 * @return a timestep specific standard string
	 */
	private String createFolderName(final int timestep) {
		StringBuilder buffer = new StringBuilder(30);
		buffer.append("Traffic from ");
		buffer.append(this.timestepToString(timestep - 1));
		buffer.append(" to ");
		buffer.append(this.timestepToString(timestep));
		buffer.append(" o'clock");
		return buffer.toString();
	}

	/**
	 * Creates a legend
	 * @return a ScreenOverlay read from a file
	 * @throws IOException
	 */
	private ScreenOverlayType createLegend() throws IOException {

		this.writer.addNonKMLFile(MatsimResource.getAsInputStream("countsKml/countsLegend240x300.png"), "countsLegend.png");
		ScreenOverlayType overlay = this.kmlObjectFactory.createScreenOverlayType();
		LinkType icon = this.kmlObjectFactory.createLinkType();
		icon.setHref("./countsLegend.png");
		overlay.setIcon(icon);
		overlay.setName("Legend");
		// place the image bottom left
		Vec2Type overlayXY = this.kmlObjectFactory.createVec2Type();
		overlayXY.setX(0.0);
		overlayXY.setY(0.0);
		overlayXY.setXunits(UnitsEnumType.FRACTION);
		overlayXY.setYunits(UnitsEnumType.FRACTION);
		overlay.setOverlayXY(overlayXY);
		Vec2Type screenXY = this.kmlObjectFactory.createVec2Type();
		screenXY.setX(0.02);
		screenXY.setY(0.07);
		screenXY.setXunits(UnitsEnumType.FRACTION);
		screenXY.setYunits(UnitsEnumType.FRACTION);
		overlay.setScreenXY(screenXY);
		return overlay;
	}
	
	/**
	 * Creates a legend
	 * @return a ScreenOverlay read from a file
	 * @throws IOException
	 */
	private ScreenOverlayType createLegendNormalized() throws IOException {

		this.writer.addNonKMLFile(MatsimResource.getAsInputStream("countsKml/countsLegendNormalized.png"), "countsLegendNormalized.png");
		ScreenOverlayType overlay = this.kmlObjectFactory.createScreenOverlayType();
		LinkType icon = this.kmlObjectFactory.createLinkType();
		icon.setHref("./countsLegendNormalized.png");
		overlay.setIcon(icon);
		overlay.setName("Legend Normalized");
		// place the image bottom left
		Vec2Type overlayXY = this.kmlObjectFactory.createVec2Type();
		overlayXY.setX(0.0);
		overlayXY.setY(0.0);
		overlayXY.setXunits(UnitsEnumType.FRACTION);
		overlayXY.setYunits(UnitsEnumType.FRACTION);
		overlay.setOverlayXY(overlayXY);
		Vec2Type screenXY = this.kmlObjectFactory.createVec2Type();
		screenXY.setX(0.02);
		screenXY.setY(0.07);
		screenXY.setXunits(UnitsEnumType.FRACTION);
		screenXY.setYunits(UnitsEnumType.FRACTION);
		overlay.setScreenXY(screenXY);
		overlay.setVisibility(Boolean.FALSE);
		return overlay;
	}
	
	/**
	 * Creates a legend
	 * @return a ScreenOverlay read from a file
	 * @throws IOException
	 */
	private ScreenOverlayType createLegendGEH() throws IOException {

		this.writer.addNonKMLFile(MatsimResource.getAsInputStream("countsKml/countsLegendGEH.png"), "countsLegendGEH.png");
		ScreenOverlayType overlay = this.kmlObjectFactory.createScreenOverlayType();
		LinkType icon = this.kmlObjectFactory.createLinkType();
		icon.setHref("./countsLegendGEH.png");
		overlay.setIcon(icon);
		overlay.setName("Legend GEH");
		// place the image bottom left
		Vec2Type overlayXY = this.kmlObjectFactory.createVec2Type();
		overlayXY.setX(0.0);
		overlayXY.setY(0.0);
		overlayXY.setXunits(UnitsEnumType.FRACTION);
		overlayXY.setYunits(UnitsEnumType.FRACTION);
		overlay.setOverlayXY(overlayXY);
		Vec2Type screenXY = this.kmlObjectFactory.createVec2Type();
		screenXY.setX(0.02);
		screenXY.setY(0.07);
		screenXY.setXunits(UnitsEnumType.FRACTION);
		screenXY.setYunits(UnitsEnumType.FRACTION);
		overlay.setScreenXY(screenXY);
		overlay.setVisibility(Boolean.FALSE);
		return overlay;
	}
	
	/**
	 * Creates a placemark
	 *
	 * @param linkid
	 * @param csc
	 * @param relativeError
	 * @param timestep
	 * @return the Placemark instance with description and name set
	 */
	private PlacemarkType createPlacemark(final String linkid, final CountSimComparison csc, final double relativeError, final double normalizedRelativeError, 
			final double gehValue, final int timestep, final String imagePath) {
		PlacemarkType placemark = this.kmlObjectFactory.createPlacemarkType();
		placemark.setDescription(createPlacemarkDescription(linkid, csc, relativeError, normalizedRelativeError, gehValue, timestep, imagePath));
		return placemark;
	}

	/**
	 * This method writes all the data for each of the links/counts to the kml
	 * document.
	 *
	 * @param countSimComparisonList provides "the data"
	 * @param folder The folder to which to add the data in the kml-file.
	 */
	private void writeLinkData(final List<CountSimComparison> countSimComparisonList, final FolderType folder, final FolderType folderNormalized, final FolderType folderGEH) {
		PlacemarkType placemark;
		double relativeError;
		double normalizedRelativeError;
		double gehValue;
		PointType point;
		for (CountSimComparison csc : countSimComparisonList) {
			Id<T> itemId = csc.getId();
			Coord coord = null;
//			String description = csc.getCsId();
			
			if (this.counts == null) {			
				Link link = this.network.getLinks().get(itemId);
				coord = this.coordTransform.transform(calculatePlacemarkPosition(link));
			} else {
				coord = this.coordTransform.transform(this.counts.getCount(itemId).getCoord());
			}
			relativeError = csc.calculateRelativeError();
			normalizedRelativeError = csc.calculateNormalizedRelativeError();
			gehValue = csc.calculateGEHValue();
			
			// +/- placemark for relative values
			{
				// build placemark
				placemark = createPlacemark(itemId.toString(), csc, relativeError, normalizedRelativeError, gehValue, csc.getHour(), this.countsLoadCurveGraphMap.get(itemId.toString()));
//				if (description != null) placemark.setName(description);
				point = this.kmlObjectFactory.createPointType();
				point.getCoordinates().add(Double.toString(coord.getX()) + "," + Double.toString(coord.getY()) + ",0.0");
				placemark.setAbstractGeometryGroup(this.kmlObjectFactory.createPoint(point));
				// cross
				if (csc.getSimulationValue() > csc.getCountValue()) {
					if (csc.getSimulationValue() < csc.getCountValue() * 1.5) {
						placemark.setStyleUrl(this.greenCrossStyle.getId());
					}
					else if (csc.getSimulationValue() < csc.getCountValue() * 2) {
						placemark.setStyleUrl(this.yellowCrossStyle.getId());
					}
					else {
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
				folder.getAbstractFeatureGroup().add(this.kmlObjectFactory.createPlacemark(placemark));
			}
			
			// +/- placemark for normalized relative values
			{
				// build placemark
				placemark = createPlacemark(itemId.toString(), csc, relativeError, normalizedRelativeError, gehValue, csc.getHour(), this.countsLoadCurveGraphMap.get(itemId.toString()));
//				if (description != null) placemark.setName(description);
				placemark.setVisibility(Boolean.FALSE);
				point = this.kmlObjectFactory.createPointType();
				point.getCoordinates().add(Double.toString(coord.getX()) + "," + Double.toString(coord.getY()) + ",0.0");
				placemark.setAbstractGeometryGroup(this.kmlObjectFactory.createPoint(point));
				
				// circle
				if (normalizedRelativeError <= 0.10) {
					placemark.setStyleUrl(this.greenCircleStyle.getId());
				} else if (normalizedRelativeError <= 0.25) {
					if (csc.getSimulationValue() > csc.getCountValue()) placemark.setStyleUrl(this.greenCrossStyle.getId());
					else placemark.setStyleUrl(this.greenMinusStyle.getId());
				} else if (normalizedRelativeError <= 0.50) {
					if (csc.getSimulationValue() > csc.getCountValue()) placemark.setStyleUrl(this.yellowCrossStyle.getId());
					else placemark.setStyleUrl(this.yellowMinusStyle.getId());
				} else if (normalizedRelativeError <= 1.00) {
					if (csc.getSimulationValue() > csc.getCountValue()) placemark.setStyleUrl(this.redCrossStyle.getId());
					else placemark.setStyleUrl(this.redMinusStyle.getId());
				} else {
					placemark.setStyleUrl(this.greyCircleStyle.getId());
				}

				folderNormalized.getAbstractFeatureGroup().add(this.kmlObjectFactory.createPlacemark(placemark));
			}
			
			// circle placemark for geh
			{
				// build placemark
				placemark = createPlacemark(itemId.toString(), csc, relativeError, normalizedRelativeError, gehValue, csc.getHour(), this.countsGEHCurveGraphMap.get(itemId.toString()));
//				if (description != null) placemark.setName(description);
				placemark.setVisibility(Boolean.FALSE);
				point = this.kmlObjectFactory.createPointType();
				point.getCoordinates().add(Double.toString(coord.getX()) + "," + Double.toString(coord.getY()) + ",0.0");
				placemark.setAbstractGeometryGroup(this.kmlObjectFactory.createPoint(point));
				
				if (gehValue < 5) placemark.setStyleUrl(this.greenCircleStyle.getId());
				else if (gehValue < 10) placemark.setStyleUrl(this.yellowCircleStyle.getId());
				else if (gehValue > 10) placemark.setStyleUrl(this.redCircleStyle.getId());
				else placemark.setStyleUrl(this.greyCircleStyle.getId());
				
				folderGEH.getAbstractFeatureGroup().add(this.kmlObjectFactory.createPlacemark(placemark));				
			}
		}
	}
	/**
	 * Calculates the position of a placemark in a way that it is 40 % of the link
	 * length away from the node where the link starts.
	 *
	 * @param l
	 * @return the CoordI instance
	 */
	private Coord calculatePlacemarkPosition(final Link l) {
		Coord coordFrom = l.getFromNode().getCoord();
		Coord coordTo = l.getToNode().getCoord();
		double xDiff = coordTo.getX() - coordFrom.getX();
		double yDiff = coordTo.getY() - coordFrom.getY();
		double length = Math.sqrt((xDiff*xDiff) + (yDiff*yDiff));
		double scale = 0.4;
		scale = l.getLength() * scale;
		Coord vec = new Coord(coordFrom.getX() + (xDiff * scale / length), coordFrom.getY() + (yDiff * scale / length));
		return vec;
	}
	/**
	 *
	 * @param linkid
	 * @param csc
	 * @param relativeError
	 * @param timestep
	 * @return A String containing the description for each placemark
	 */
	private String createPlacemarkDescription(final String linkid, final CountSimComparison csc, final double relativeError, final double normalizedRelativeError, 
			final double gehValue, final int timestep, final String imagePath) {
		StringBuilder buffer = new StringBuilder(100);
//		buffer.append(NetworkFeatureFactory.STARTCDATA);
//		buffer.append(STARTH1);
//		buffer.append(LINK);
//		buffer.append(linkid);
//		buffer.append(ENDH1);
		if (csc.getCsId() != null) {
			buffer.append(NetworkFeatureFactory.STARTH2);
			buffer.append(CSID);
			buffer.append(csc.getCsId());
			buffer.append(NetworkFeatureFactory.ENDH2);			
		}
		buffer.append(NetworkFeatureFactory.STARTH2);
		buffer.append(LINK);
		buffer.append(linkid);
		buffer.append(NetworkFeatureFactory.ENDH2);
		buffer.append(NetworkFeatureFactory.STARTH3);
		buffer.append(H24OVERVIEW);
		buffer.append(NetworkFeatureFactory.ENDH3);
		buffer.append(NetworkFeatureFactory.STARTP);
		buffer.append(IMG);
		buffer.append(imagePath);
		buffer.append(IMGEND);
		buffer.append(NetworkFeatureFactory.ENDP);
		buffer.append(NetworkFeatureFactory.STARTH3);
		buffer.append(DETAILSFROM);
		buffer.append((this.timestepToString(timestep -1)));
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
		buffer.append(nf.format(relativeError * 100) +  "%");
		buffer.append(NetworkFeatureFactory.ENDP);
		buffer.append(NetworkFeatureFactory.STARTP);
		buffer.append(NORMRELERROR);
		buffer.append(nf.format(normalizedRelativeError * 100) +  "%");
		buffer.append(NetworkFeatureFactory.ENDP);
		buffer.append(NetworkFeatureFactory.STARTP);
		buffer.append(GEH);
		buffer.append(gehValue);
		buffer.append(NetworkFeatureFactory.ENDP);
//		buffer.append(NetworkFeatureFactory.ENDCDATA);
		return buffer.toString();
	}

	/**
	 * @param timestep
	 * @return A two digits string containing the given timestep
	 */
	private String timestepToString(final int timestep) {
		if (timestep < 10) {
			StringBuilder buffer = new StringBuilder();
			buffer.append(ZERO);
			buffer.append(Integer.toString(timestep));
			return buffer.toString();
		}
		return Integer.toString(timestep);
	}

	/**
	 * Creates CountsSimRealPerHourGraphs and adds them to the kmz in the given folder. The creation of the graphs is
	 * only done if the map attribute of this class for CountsSimRealPerHourGraphs is null.
	 *
	 * @param folder
	 * @param timestep
	 * @param timespan
	 */
	private void addCountsSimRealPerHourGraphs(final FolderType folder, final int timestep, final TimeSpanType timespan) {
		StringBuffer filename;
		ScreenOverlayType overlay;

		try {
			//add the file to the kmz
			filename = new StringBuffer(graphname);
			filename.append(Integer.toString(timestep));
			filename.append(PNG);

			CountsSimRealPerHourGraph graph = new CountsSimRealPerHourGraph(this.countComparisonFilter.getCountsForHour(null), this.iterationNumber, filename.toString());
			graph.createChart(timestep);

			this.writeChartToKmz(filename.toString(), graph.getChart());
			//and link with the overlay
			overlay = this.kmlObjectFactory.createScreenOverlayType();
			LinkType icon = this.kmlObjectFactory.createLinkType();
			icon.setHref("./" + filename.toString());
			overlay.setIcon(icon);
			overlay.setName(graph.getChartTitle());
			// place the image top left
			Vec2Type overlayXY = this.kmlObjectFactory.createVec2Type();
			overlayXY.setX(1.0);
			overlayXY.setY(1.0);
			overlayXY.setXunits(UnitsEnumType.FRACTION);
			overlayXY.setYunits(UnitsEnumType.FRACTION);
			overlay.setOverlayXY(overlayXY);
			Vec2Type screenXY = this.kmlObjectFactory.createVec2Type();
			screenXY.setX(0.98);
			screenXY.setY(0.98);
			screenXY.setXunits(UnitsEnumType.FRACTION);
			screenXY.setYunits(UnitsEnumType.FRACTION);
			overlay.setScreenXY(screenXY);
			overlay.setAbstractTimePrimitiveGroup(this.kmlObjectFactory.createTimeSpan(timespan));
			//add the overlay to the folder
			folder.getAbstractFeatureGroup().add(this.kmlObjectFactory.createScreenOverlay(overlay));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Creates CountsLoadCurveGraphs for each link and puts them in the kmz as pngs
	 */
	private void createCountsLoadCurveGraphs() {
		CountsLoadCurveGraphCreator cgc = new CountsLoadCurveGraphCreator("");
		List<CountsGraph> graphs = cgc.createGraphs(this.countComparisonFilter.getCountsForHour(null), this.iterationNumber);

		this.countsLoadCurveGraphMap = new HashMap<>(graphs.size());
		String linkid;
		StringBuffer filename;
		for (CountsGraph cg : graphs) {
			try {
				filename = new StringBuffer();
				linkid = ((CountsLoadCurveGraph) cg).getLinkId();
				filename.append(linkid);
				filename.append(PNG);
				writeChartToKmz(filename.toString(), cg.getChart());
				this.countsLoadCurveGraphMap.put(linkid, filename.toString());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Creates CountsGEHCurveGraphs for each link and puts them in the kmz as pngs
	 */
	private void createCountsGEHCurveGraphs() {
		CountsGEHCurveGraphCreator cgc = new CountsGEHCurveGraphCreator("");
		List<CountsGraph> graphs = cgc.createGraphs(this.countComparisonFilter.getCountsForHour(null), this.iterationNumber);

		this.countsGEHCurveGraphMap = new HashMap<>(graphs.size());
		String linkId;
		StringBuffer filename;
		for (CountsGraph cg : graphs) {
			try {
				filename = new StringBuffer();
				linkId = ((CountsGEHCurveGraph) cg).getLinkId();
				filename.append(linkId);
				filename.append("_GEH");
				filename.append(PNG);
				writeChartToKmz(filename.toString(), cg.getChart());
				this.countsGEHCurveGraphMap.put(linkId, filename.toString());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Writes the given JFreeChart to the kmz file specified for the kmz writer attribute of this class.
	 * @param filename the filename to use in the kmz
	 * @param chart
	 * @throws IOException
	 */
	private void writeChartToKmz(final String filename, final JFreeChart chart) throws IOException {
		byte [] img;
		img = ChartUtils.encodeAsPNG(chart.createBufferedImage(CHARTWIDTH, CHARTHEIGHT));
		this.writer.addNonKMLFile(img, filename);
	}

	/**
	 * Creates the CountsErrorGraph for all the data
	 * @param visible true if initially visible
	 * @return the ScreenOverlay Feature
	 */
	private ScreenOverlayType createBoxPlotErrorGraph() {

		CountsGraph ep;
		try {
			ep = new BoxPlotErrorGraph(this.countComparisonFilter.getCountsForHour(null), this.iterationNumber, null, "error graph");
			ep.createChart(0);
		} catch (IllegalArgumentException e) {
			log.error("Could not create BoxPlot-ErrorGraph.", e);
			return null;
		}

		String filename = "errorGraphBoxPlot.png";
		try {
			writeChartToKmz(filename, ep.getChart());
			return createOverlayBottomRight(filename, "Error Graph [Box-Plot]");
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Creates the CountsErrorGraph for all the data
	 * @param visible true if initially visible
	 * @return the ScreenOverlay Feature
	 */
	private ScreenOverlayType createBoxPlotNormalizedErrorGraph() {

		CountsGraph ep;
		try {
			ep = new BoxPlotNormalizedErrorGraph(this.countComparisonFilter.getCountsForHour(null), this.iterationNumber, null, "error graph");
			ep.createChart(0);
		} catch (IllegalArgumentException e) {
			log.error("Could not create BoxPlot-NormalizedErrorGraph.", e);
			return null;
		}

		String filename = "errorGraphNormalizedBoxPlot.png";
		try {
			writeChartToKmz(filename, ep.getChart());
			return createOverlayBottomRight(filename, "Normalized Error Graph [Box-Plot]");
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * Creates the CountsErrorGraph for all the data
	 * @param kmlFilename the filename of the kml file
	 * @param visible true if initially visible
	 * @return the ScreenOverlay Feature
	 */
	private ScreenOverlayType createBiasErrorGraph(String kmlFilename) {
		BiasErrorGraph ep = new BiasErrorGraph(this.countComparisonFilter.getCountsForHour(null), this.iterationNumber, null, "error graph");
		ep.createChart(0);

		double[] meanError = ep.getMeanRelError();
		double[] meanBias = ep.getMeanBias();
		int index = kmlFilename.lastIndexOf(System.getProperty("file.separator"));
		if (index == -1) {
			index = kmlFilename.lastIndexOf('/');
		}
		String outdir;
		if (index == -1) {
			outdir = "";
		}
		else {
			outdir = kmlFilename.substring(0, index) + System.getProperty("file.separator");
		}
		String file = outdir + "biasErrorGraphData.txt";
		log.info("writing chart data to " + new File(file).getAbsolutePath());
		try {
			BufferedWriter bwriter = IOUtils.getBufferedWriter(file);
			StringBuilder buffer = new StringBuilder(200);
			buffer.append("hour \t mean relative error \t mean bias");
			bwriter.write(buffer.toString());
			bwriter.newLine();
			for (int i = 0; i < meanError.length; i++) {
				buffer.delete(0, buffer.length());
				buffer.append(i + 1);
				buffer.append('\t');
				buffer.append(meanError[i]);
				buffer.append('\t');
				buffer.append(meanBias[i]);
				bwriter.write(buffer.toString());
				bwriter.newLine();
			}
			bwriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		String filename = "errorGraphErrorBias.png";
		try {
			writeChartToKmz(filename, ep.getChart());
			return createOverlayBottomRight(filename, "Error Graph [Error/Bias]");
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * Creates the CountsErrorGraph for all the data
	 * @param kmlFilename the filename of the kml file
	 * @param visible true if initially visible
	 * @return the ScreenOverlay Feature
	 */
	private ScreenOverlayType createBiasNormalizedErrorGraph(String kmlFilename) {
		BiasNormalizedErrorGraph ep = new BiasNormalizedErrorGraph(this.countComparisonFilter.getCountsForHour(null), this.iterationNumber, null, "error graph");
		ep.createChart(0);

		double[] meanError = ep.getMeanNormRelError();
		double[] meanBias = ep.getMeanBias();
		int index = kmlFilename.lastIndexOf(System.getProperty("file.separator"));
		if (index == -1) {
			index = kmlFilename.lastIndexOf('/');
		}
		String outdir;
		if (index == -1) {
			outdir = "";
		}
		else {
			outdir = kmlFilename.substring(0, index) + System.getProperty("file.separator");
		}
		String file = outdir + "biasNormalizedErrorGraphData.txt";
		log.info("writing chart data to " + new File(file).getAbsolutePath());
		try {
			BufferedWriter bwriter = IOUtils.getBufferedWriter(file);
			StringBuilder buffer = new StringBuilder(200);
			buffer.append("hour \t mean normalized relative error \t mean bias");
			bwriter.write(buffer.toString());
			bwriter.newLine();
			for (int i = 0; i < meanError.length; i++) {
				buffer.delete(0, buffer.length());
				buffer.append(i + 1);
				buffer.append('\t');
				buffer.append(meanError[i]);
				buffer.append('\t');
				buffer.append(meanBias[i]);
				bwriter.write(buffer.toString());
				bwriter.newLine();
			}
			bwriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		String filename = "errorGraphNormalizedErrorBias.png";
		try {
			writeChartToKmz(filename, ep.getChart());
			return createOverlayBottomRight(filename, "Normalized Error Graph [Error/Bias]");
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Creates the CountsSimReal24Graph for all the data. AWTV = average weekday traffic volumes.
	 * <br><br>
	 * Notes:<ul>
	 * <li> I think that "weekday" means "day-of-week", i.e. the method does not care when it is sunday. kai, sep'16
	 * </ul>
	 * @param visible true if initially visible
	 * @return the ScreenOverlay Feature
	 */
	private ScreenOverlayType createAWTVGraph() {
		CountsGraph awtv = new CountsSimReal24Graph(this.countComparisonFilter.getCountsForHour(null), this.iterationNumber, "awtv graph");
		awtv.createChart(0);

		String filename = "awtv.png";
		try {
			writeChartToKmz(filename, awtv.getChart());
			return createOverlayBottomRight("./" + filename, "AWTV");
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	private ScreenOverlayType createOverlayBottomRight(final String fileName, final String overlayName) {
		ScreenOverlayType overlay = this.kmlObjectFactory.createScreenOverlayType();
		LinkType icon1 = this.kmlObjectFactory.createLinkType();
		icon1.setHref("./" + fileName);
		overlay.setIcon(icon1);
		overlay.setName(overlayName);
		// place the image bottom right
		Vec2Type overlayXY = this.kmlObjectFactory.createVec2Type();
		overlayXY.setX(1.0);
		overlayXY.setY(0.0);
		overlayXY.setXunits(UnitsEnumType.FRACTION);
		overlayXY.setYunits(UnitsEnumType.FRACTION);
		overlay.setOverlayXY(overlayXY);
		Vec2Type screenXY = this.kmlObjectFactory.createVec2Type();
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
		log.info("DONE with writing kml file.");
	}
}