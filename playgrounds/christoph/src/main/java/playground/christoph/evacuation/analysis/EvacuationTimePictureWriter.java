/* *********************************************************************** *
 * project: org.matsim.*
 * EvacuationTimePictureWriter.java
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

package playground.christoph.evacuation.analysis;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Map.Entry;

import javax.imageio.ImageIO;

import net.opengis.kml._2.DocumentType;
import net.opengis.kml._2.FolderType;
import net.opengis.kml._2.IconStyleType;
import net.opengis.kml._2.LinkType;
import net.opengis.kml._2.ObjectFactory;
import net.opengis.kml._2.PlacemarkType;
import net.opengis.kml._2.PointType;
import net.opengis.kml._2.ScreenOverlayType;
import net.opengis.kml._2.StyleType;
import net.opengis.kml._2.UnitsEnumType;
import net.opengis.kml._2.Vec2Type;

import org.apache.log4j.Logger;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.statistics.DefaultBoxAndWhiskerCategoryDataset;
import org.jfree.data.statistics.HistogramDataset;
import org.jfree.data.statistics.HistogramType;
import org.matsim.api.core.v01.BasicLocation;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.gbl.MatsimResource;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.facilities.Facility;
import org.matsim.vis.kml.KMZWriter;
import org.matsim.vis.kml.MatsimKMLLogo;
import org.matsim.vis.kml.NetworkFeatureFactory;

import playground.christoph.evacuation.analysis.EvacuationTimePicture.AgentInfo;
import playground.christoph.evacuation.config.EvacuationConfig;

public class EvacuationTimePictureWriter {
	
	private static final Logger log = Logger.getLogger(EvacuationTimePictureWriter.class);

	/*
	 * Strings
	 */
	private static final String TITLE = "Evacation Travel Times";
	private static final String MEANEVACUATIONTIME = "mean evacuation time: ";
	private static final String TRIPS = "trips (valid/invalid): ";
	private static final String SECONDS = "s";

	private static final String IMG = "<img src=\"./";
	private static final String IMGEND = "\">";
	
	private static final String LINKLOCATION = "Link_";
	private static final String FACILITYLOCATION = "Facility_";
	private static final String HISTOGRAM = "_Histogram";
	private static final String BOXPLOT = "_Boxplot";
	private static final String LEGENDHEADER = "mean evacuation time\nfrom location by ";
	
	/*
	 * Icons
	 */
	private static final String SPACER ="spacer.png";
	private static final String LEGEND ="_legend.png";
	private static final String OVERALLHISTROGRAM ="_overallhistogram.png";
	private static final String DEFAULTNODEICON ="node.png";
	private static final String DEFAULTNODEICONRESOURCE = "icon18.png";
	private static final Double ICONSCALE = Double.valueOf(0.5);
	
	/*
	 * height of the charts
	 */
	private static final int OVERALLHISTOGRAMHEIGHT = 200;
	private static final int HISTOGRAMHEIGHT = 250;
	private static final int BOXPLOTHEIGHT = 250;
	/*
	 * width of the  charts
	 */
	private static final int OVERALLHISTOGRAMWIDTH = 300;
	private static final int HISTOGRAMWIDTH = 400;
	private static final int BOXPLOTWIDTH = 100;
	/*
	 * constant for the file suffix of graphs
	 */
	private static final String PNG = ".png";
	
	/*
	 * Color Scale:
	 * RGB
	 * 255	0	0
	 * 255 127 0
	 * 255 205 0
	 * 255 255 0
	 * 205 230 0
	 * 127 230 0
	 * 0 255 0
	 * 0 255 153
	 * 0 255 255
	 * 0 205 255
	 * 0 127 255
	 * 0 0 255
	 */
	
	// ALPHA - B - G - R
	/*package*/ static final byte[][] colorScale = new byte[][]{{(byte) 255, (byte) 255, (byte) 0, (byte) 0},
															{(byte) 255, (byte) 255, (byte) 127, (byte) 0},
															{(byte) 255, (byte) 255, (byte) 205, (byte) 0},
															{(byte) 255, (byte) 255, (byte) 255, (byte) 0},
															{(byte) 255, (byte) 205, (byte) 230, (byte) 0},
															{(byte) 255, (byte) 127, (byte) 230, (byte) 0},
															{(byte) 255, (byte) 0, (byte) 255, (byte) 0},
															{(byte) 255, (byte) 0, (byte) 255, (byte) 153},
															{(byte) 255, (byte) 0, (byte) 255, (byte) 255},
															{(byte) 255, (byte) 0, (byte) 205, (byte) 255},
															{(byte) 255, (byte) 0, (byte) 127, (byte) 255},
															{(byte) 255, (byte) 0, (byte) 0, (byte) 255}};
	
	private static final byte[] colorBlack = new byte[]{(byte) 255, (byte) 0, (byte) 0, (byte) 0};
	
//	private static final byte[] MATSIMRED = new byte[]{(byte) 255, (byte) 15, (byte) 15, (byte) 190};
//	private static final byte[] MATSIMGREEN = new byte[]{(byte) 255, (byte) 15, (byte) 190, (byte) 15};
//	private static final byte[] MATSIMWHITE = new byte[]{(byte) 230, (byte) 230, (byte) 230, (byte) 230};

	private Scenario scenario;
	private KMZWriter kmzWriter;
	private DocumentType document;

	private ObjectFactory kmlObjectFactory = new ObjectFactory();
	
	private StyleType[] colorBarStyles;
	private StyleType blackStyle;

	private CoordinateTransformation coordTransform;

	private double minEvacuationTime = 0.0;
	private double maxEvacuationTime = Double.MIN_VALUE;
	private double meanEvacuationTime = 0.0;
	private double standardDeviation = 0.0;
	private boolean limitMaxEvacuationTime = true;
	private double evacuationTimeCutOffFactor = 3;	// cut off values > mean + 3 * standard deviation
	
	private boolean doClustering = true;
	private double clusterFactor = 5.0;
	private int clusterIterations = 100;
	
	public EvacuationTimePictureWriter(Scenario scenario, CoordinateTransformation coordTransform, KMZWriter kmzWriter, DocumentType document) throws IOException {
		this.scenario = scenario;
		this.coordTransform = coordTransform;
		this.kmzWriter = kmzWriter;
		this.document = document;
		
		createDefaultStyles();
	}

	private void calcMaxEvacuationTime(Map<Id, Double> evacuationTime) {
		double sum = 0.0;
		for (double d : evacuationTime.values()) {
			if (d > maxEvacuationTime) maxEvacuationTime = d;
			
			sum = sum + d;
		}
		
		int count = evacuationTime.size();
		if (count == 0) return;
		
		meanEvacuationTime = sum / count;
		double sumSquares = 0.0;
		for (double d : evacuationTime.values()) {
			sumSquares = sumSquares + Math.pow(meanEvacuationTime - d, 2);
		}
		double variance = (1.0 / (count - 1)) * sumSquares;		
		standardDeviation = Math.sqrt(variance);
		
		log.info("Mean evacuation time: " + (int)meanEvacuationTime);
		log.info("Standard deviation: " + (int)standardDeviation);
		
		int cuttedValues = 0;
		double cutOffValue = meanEvacuationTime + standardDeviation * evacuationTimeCutOffFactor;
		for (double d : evacuationTime.values()) {
			if (d > cutOffValue) cuttedValues++;
		}
		log.info("Persons using transport mode: " + evacuationTime.values().size());
		log.info("Travel times above cut off travel time: " + cuttedValues);
	}
	
	private int getColorIndex(double value) {
		double step;
		if (limitMaxEvacuationTime) {
			double cutOffValue = meanEvacuationTime + standardDeviation * evacuationTimeCutOffFactor;
			step = (cutOffValue - minEvacuationTime) / colorScale.length;
			// if the value is > than the cut off Value -> return the highest index
			if (value > cutOffValue) return colorScale.length - 1;
		} 
		else {
			step = (maxEvacuationTime - minEvacuationTime) / colorScale.length;
		}	
		return (int)Math.floor((value - minEvacuationTime) / step);
	}
	
	// Ids are personIds!
	public FolderType getLinkFolder(Map<Id, BasicLocation> locations, Map<Id, BasicLocation> positionAtEvacuationStart, Map<Id, AgentInfo> agentInfos) throws IOException {
	
		/*
		 * Create basic structures
		 */
		
		// create main folder
		FolderType mainFolder = this.kmlObjectFactory.createFolderType();
		mainFolder.setName("Evacuation Times");
		
		// add the MATSim logo to the kml
		mainFolder.getAbstractFeatureGroup().add(kmlObjectFactory.createScreenOverlay(MatsimKMLLogo.writeMatsimKMLLogo(kmzWriter)));
				
		/*
		 * Identify all utilized modes
		 */
		Set<String> modes = new HashSet<String>();
		for (AgentInfo agentInfo : agentInfos.values()) modes.addAll(agentInfo.transportModes);
		Set<String> orderedModes = new TreeSet<String>(modes);
		
		/*
		 * for every transportMode
		 */

		for (String transportMode : orderedModes) {
			Map<Id, Double> times = new HashMap<Id, Double>();

			for (Entry<Id, BasicLocation> entry : positionAtEvacuationStart.entrySet()) {
				AgentInfo agentInfo = agentInfos.get(entry.getKey());
				if (agentInfo.transportModes.size() == 1 && agentInfo.transportModes.contains(transportMode)) {
					times.put(entry.getKey(), agentInfo.leftArea - EvacuationConfig.evacuationTime);
				}
			}
			
			/*
			 * If no agents uses the current transport mode we can skip it. 
			 */
			if (times.size() == 0) continue;
			
			/*
			 * Filter locations - only those are needed, which contain agents of the
			 * current transport mode.
			 */
			Map<Id, BasicLocation> transportModeLocations = new HashMap<Id, BasicLocation>();
			for (Id id : times.keySet()) {
				transportModeLocations.put(id, locations.get(id));
			}
			FolderType transportModeFolder = getLinkFolder(transportMode, transportModeLocations, times);
//			FolderType transportModeFolder = getLinkFolder(transportMode, locations, times);
			mainFolder.getAbstractFeatureGroup().add(kmlObjectFactory.createFolder(transportModeFolder));
		}
		
		return mainFolder;
	}
	
	// Ids are personIds and not Link/Facility Ids!
	public FolderType getLinkFolder(String transportMode, Map<Id, BasicLocation> locations, Map<Id, Double> evacuationTimes) throws IOException {

		calcMaxEvacuationTime(evacuationTimes);
		
		/*
		 * create Folders and connect them
		 */
		FolderType mainFolder = this.kmlObjectFactory.createFolderType();
		FolderType linkFolder = this.kmlObjectFactory.createFolderType();
		FolderType facilityFolder = this.kmlObjectFactory.createFolderType();
		FolderType linkFolderA = this.kmlObjectFactory.createFolderType();	// 0 .. 10 valid Trips
		FolderType linkFolderB = this.kmlObjectFactory.createFolderType();	// 10 .. 100 valid Trips
		FolderType linkFolderC = this.kmlObjectFactory.createFolderType();	// 100 .. 1000 valid Trips
		FolderType linkFolderD = this.kmlObjectFactory.createFolderType();	// 1000 and more valid Trips
		FolderType facilityFolderA = this.kmlObjectFactory.createFolderType();	// 0 .. 10 valid Trips
		FolderType facilityFolderB = this.kmlObjectFactory.createFolderType();	// 10 .. 100 valid Trips
		FolderType facilityFolderC = this.kmlObjectFactory.createFolderType();	// 100 .. 1000 valid Trips
		FolderType facilityFolderD = this.kmlObjectFactory.createFolderType();	// 1000 and more valid Trips
		
		mainFolder.setName("Evacuation Times " + transportMode);
		linkFolder.setName("Links");
		facilityFolder.setName("Facilities");
		linkFolderA.setName("Links with 0..9 valid Trips");
		linkFolderB.setName("Links with 10..99 valid Trips");
		linkFolderC.setName("Links with 100..9 valid Trips");
		linkFolderD.setName("Links with 1000 and more valid Trips");
		facilityFolderA.setName("Facilities with 0..9 valid Trips");
		facilityFolderB.setName("Facilities with 10..99 valid Trips");
		facilityFolderC.setName("Facilities with 100..9 valid Trips");
		facilityFolderD.setName("Facilities with 1000 and more valid Trips");
		
		mainFolder.getAbstractFeatureGroup().add(this.kmlObjectFactory.createFolder(linkFolder));
		mainFolder.getAbstractFeatureGroup().add(this.kmlObjectFactory.createFolder(facilityFolder));
		linkFolder.getAbstractFeatureGroup().add(this.kmlObjectFactory.createFolder(linkFolderA));
		linkFolder.getAbstractFeatureGroup().add(this.kmlObjectFactory.createFolder(linkFolderB));
		linkFolder.getAbstractFeatureGroup().add(this.kmlObjectFactory.createFolder(linkFolderC));
		linkFolder.getAbstractFeatureGroup().add(this.kmlObjectFactory.createFolder(linkFolderD));
		facilityFolder.getAbstractFeatureGroup().add(this.kmlObjectFactory.createFolder(facilityFolderA));
		facilityFolder.getAbstractFeatureGroup().add(this.kmlObjectFactory.createFolder(facilityFolderB));
		facilityFolder.getAbstractFeatureGroup().add(this.kmlObjectFactory.createFolder(facilityFolderC));
		facilityFolder.getAbstractFeatureGroup().add(this.kmlObjectFactory.createFolder(facilityFolderD));
		
		/*
		 * create overall histogram and add it to the kmz file
		 */
		ScreenOverlayType histogram = createHistogram(transportMode, evacuationTimes);
		mainFolder.getAbstractFeatureGroup().add(kmlObjectFactory.createScreenOverlay(histogram));
		
		/*
		 * create legend and add it to the kmz file
		 */
		ScreenOverlayType legend = createLegend(transportMode);
		mainFolder.getAbstractFeatureGroup().add(kmlObjectFactory.createScreenOverlay(legend));

		Map<BasicLocation, List<Double>> locationMap = new HashMap<BasicLocation, List<Double>>();
		
		for (Entry<Id, BasicLocation> entry : locations.entrySet()) {
			Id id = entry.getKey();
			BasicLocation location = entry.getValue();
			
			List<Double> list = locationMap.get(location);
			if (list == null) {
				list = new ArrayList<Double>();
				locationMap.put(location, list);
			}
			
			Double value = evacuationTimes.get(id);
			if (value == null) value = Double.NaN;
			list.add(value);
		}
		
		log.info("Number of different start locations found: " + locationMap.size());

		if (doClustering) {
			EvacuationTimeClusterer clusterer = new EvacuationTimeClusterer(scenario.getNetwork(), locationMap, scenario.getConfig().global().getNumberOfThreads());
			int numClusters = (int) Math.ceil(locationMap.size() / clusterFactor);
			locationMap = clusterer.buildCluster(numClusters, clusterIterations);
		}
		
		for (Entry<BasicLocation, List<Double>> entry : locationMap.entrySet()) {
			BasicLocation location = entry.getKey();
			List<Double> list = entry.getValue();

			int valid = 0;
			int invalid = 0;
			
			/*
			 * Remove NaN entries from the List
			 */
			List<Double> listWithoutNaN = new ArrayList<Double>();
			for (Double d : list) {
				if (d.isNaN()) {
					invalid++;
				} else listWithoutNaN.add(d);
			}

			/*
			 * If trip with significant to high evacuation times should be cut off
			 */
			if (limitMaxEvacuationTime) {
				double cutOffValue = meanEvacuationTime + standardDeviation * evacuationTimeCutOffFactor;
				ListIterator<Double> iter = listWithoutNaN.listIterator();
				while (iter.hasNext()) {
					double value = iter.next();
					if (value > cutOffValue) {
						iter.remove();
						invalid++;
					}
				}
			}
			valid = list.size() - invalid;
			
			double mean = 0.0;
			for (Double d : list) {
				mean = mean + d;
			}
			mean = mean / list.size();
			
			// if at least one valid entry found - otherwise it would result in a divide by zero error
			if (listWithoutNaN.size() == 0) continue;
//			if (invalid < list.size()) mean = mean / (list.size() - invalid);
//			else continue;
			
			int index = getColorIndex(mean);
			
			StyleType styleType = colorBarStyles[index];
			
			PlacemarkType placemark = createPlacemark(transportMode, location, mean, valid, invalid);		
			placemark.setStyleUrl(styleType.getId());
			if (location instanceof Facility) {
				if (valid < 10) facilityFolderA.getAbstractFeatureGroup().add(this.kmlObjectFactory.createPlacemark(placemark));
				else if (valid < 100) facilityFolderB.getAbstractFeatureGroup().add(this.kmlObjectFactory.createPlacemark(placemark));
				else if (valid < 1000) facilityFolderC.getAbstractFeatureGroup().add(this.kmlObjectFactory.createPlacemark(placemark));
				else facilityFolderD.getAbstractFeatureGroup().add(this.kmlObjectFactory.createPlacemark(placemark));
								
			}
			else if (location instanceof Link) {
				if (valid < 10) linkFolderA.getAbstractFeatureGroup().add(this.kmlObjectFactory.createPlacemark(placemark));
				else if (valid < 100) linkFolderB.getAbstractFeatureGroup().add(this.kmlObjectFactory.createPlacemark(placemark));
				else if (valid < 1000) linkFolderC.getAbstractFeatureGroup().add(this.kmlObjectFactory.createPlacemark(placemark));
				else linkFolderD.getAbstractFeatureGroup().add(this.kmlObjectFactory.createPlacemark(placemark));
			}
			else {
				mainFolder.getAbstractFeatureGroup().add(this.kmlObjectFactory.createPlacemark(placemark));
			}
					
			histogramToKMZ(transportMode, location, listWithoutNaN);
			boxplotToKMZ(transportMode, location, listWithoutNaN);
		}
		
		return mainFolder;
	}
	
	private void histogramToKMZ(String transportMode, BasicLocation location, List<Double> listWithoutNaN) throws IOException {		
		String filename = createFilenameFromLocation(location, "_" + transportMode + HISTOGRAM);
		if (filename == null) return;
				
		double[] array = new double[listWithoutNaN.size()];
		int i = 0;
		ListIterator<Double> iter = listWithoutNaN.listIterator();
		while (iter.hasNext()) {
			array[i] = iter.next();
			i++;
		}
		
		// if no valid travel times exist -> create an empty histogram
		if (array.length == 0) {
			array = new double[1];
			array[0] = 0.0;
		}
		
		writeChartToKmz(filename, createHistogramChart(transportMode, array), HISTOGRAMWIDTH, HISTOGRAMHEIGHT);
	}
	
	private void boxplotToKMZ(String transportMode, BasicLocation location, List<Double> listWithoutNaN) throws IOException {		
		String filename = createFilenameFromLocation(location, "_" + transportMode + BOXPLOT);
		if (filename == null) return;
		
//		List<Double> listWithoutNaN = new ArrayList<Double>();
//		for (Double d : list) if (!d.isNaN()) listWithoutNaN.add(d);
		
		// if no valid travel times exist -> create an empty histogram
		if (listWithoutNaN.size() == 0) listWithoutNaN.add(0.0);
		
		writeChartToKmz(filename, createBoxplotChart(listWithoutNaN), BOXPLOTWIDTH, BOXPLOTHEIGHT);
	}
	
	private String createFilenameFromLocation(BasicLocation location, String suffix) {
		String prefix;
		Id id;
		
		if (location instanceof Link) {
			prefix = LINKLOCATION;
			id = ((Link) location).getId();
		}
		else if (location instanceof Facility) {
			prefix = FACILITYLOCATION;
			id = ((Facility) location).getId();
		}
		else {
			log.warn("Unkwown location type: " + location.getClass().toString() + ". Skipping it!");
			return null;
		}
		
		StringBuffer filename = new StringBuffer();
		filename.append(prefix);
		filename.append(id.toString());
		filename.append(suffix);
		filename.append(PNG);
		return filename.toString();
	}
	
	private PlacemarkType createPlacemark(String transportMode, BasicLocation location, double mean, int valid, int invalid) {
	
		Coord coord = null;
		if (location instanceof Link) {
			coord = getShiftedLinkCoord((Link) location);
		} else coord = location.getCoord();
		
		Coord transformedCoord = this.coordTransform.transform(coord);
		String histogram = createFilenameFromLocation(location, "_" + transportMode + HISTOGRAM);
		String boxplot = createFilenameFromLocation(location, "_" + transportMode + BOXPLOT);

		PlacemarkType placemark = kmlObjectFactory.createPlacemarkType();
		placemark.setDescription(createPlacemarkDescription(mean, valid, invalid, histogram, boxplot));
		
		PointType point;
		point = kmlObjectFactory.createPointType();
		point.getCoordinates().add(Double.toString(transformedCoord.getX()) + "," + Double.toString(transformedCoord.getY()) + ",0.0");
		placemark.setAbstractGeometryGroup(kmlObjectFactory.createPoint(point));
		return placemark;
	}
		
	private String createPlacemarkDescription(double mean, int valid, int invalid, String histogram, String boxplot) {
		StringBuffer buffer = new StringBuffer(100);
		buffer.append(NetworkFeatureFactory.STARTH2);
		buffer.append(TITLE);
		buffer.append(NetworkFeatureFactory.ENDH2);
		
		buffer.append(NetworkFeatureFactory.STARTH3);
		buffer.append(MEANEVACUATIONTIME);
		buffer.append((int)mean);
		buffer.append(SECONDS);
		buffer.append(NetworkFeatureFactory.ENDH3);
		
		buffer.append(NetworkFeatureFactory.STARTH3);
		buffer.append(TRIPS);
		buffer.append(valid);
		buffer.append(" / ");
		buffer.append(invalid);
		buffer.append(NetworkFeatureFactory.ENDH3);
				
		buffer.append(IMG);
		buffer.append(histogram);
		buffer.append(IMGEND);
		
		buffer.append(IMG);
		buffer.append(boxplot);
		buffer.append(IMGEND);
		
		/*
		 * Add a spacer image which has the same width as all other
		 * images together - doing so should ensure that they are
		 * displayed side by side.
		 */
		buffer.append(IMG);
		buffer.append(SPACER);
		buffer.append(IMGEND);
		
		return buffer.toString();
		
//		NumberFormat formatter = new DecimalFormat("0.0");
//		formatter.format(mean);
	}
	
	
    private JFreeChart createHistogramChart(String transportMode, double[] travelTimes) {    	

    	HistogramDataset dataset = new HistogramDataset();
    	dataset.setType(HistogramType.RELATIVE_FREQUENCY);
    	dataset.addSeries("evacuation travel time " + transportMode, travelTimes, 20);
    	    	
    	JFreeChart chart = ChartFactory.createHistogram(
    			null, 
    			null, 
    			"frequency", 
    			dataset, 
    			PlotOrientation.VERTICAL, 
    			true, 
    			false, 
    			false);
    	chart.getXYPlot().setForegroundAlpha(0.75f);
    	
    	/*
    	 * set x-axis range 
    	 */
    	double min = 0.0;
    	double max = maxEvacuationTime;
    	if (limitMaxEvacuationTime) {
			double cutOffValue = meanEvacuationTime + standardDeviation * evacuationTimeCutOffFactor;
			max = cutOffValue;
		} 
    	chart.getXYPlot().getDomainAxis().setLowerBound(min);
    	chart.getXYPlot().getDomainAxis().setUpperBound(max);
			
    	return chart;
    }
    
    private JFreeChart createBoxplotChart(List<Double> travelTimes) {    	

    	DefaultBoxAndWhiskerCategoryDataset dataset = new DefaultBoxAndWhiskerCategoryDataset();
    	dataset.add(travelTimes, "Series", "");
    	
    	JFreeChart chart = ChartFactory.createBoxAndWhiskerChart(
    			null, 
    			null, 
    			"evacuation travel time", 
    			dataset, 
    			false);
    	chart.getCategoryPlot().setForegroundAlpha(0.75f);
    	return chart;
    }
    	
	private void createDefaultStyles() throws IOException {

		LinkType iconLink = kmlObjectFactory.createLinkType();
		iconLink.setHref(DEFAULTNODEICON);
		this.kmzWriter.addNonKMLFile(MatsimResource.getAsInputStream(DEFAULTNODEICONRESOURCE), DEFAULTNODEICON);
		
		colorBarStyles = new StyleType[colorScale.length];
		for (int i = 0; i < colorScale.length; i++) {
			byte[] color = colorScale[i];
			this.colorBarStyles[i] = kmlObjectFactory.createStyleType();
			this.colorBarStyles[i].setId("Color" + i);
			IconStyleType iStyle = kmlObjectFactory.createIconStyleType();
			iStyle.setIcon(iconLink);
			iStyle.setColor(color);
			iStyle.setScale(EvacuationTimePictureWriter.ICONSCALE);
			this.colorBarStyles[i].setIconStyle(iStyle);
			this.document.getAbstractStyleSelectorGroup().add(kmlObjectFactory.createStyle(colorBarStyles[i]));
		}
		
		this.blackStyle = kmlObjectFactory.createStyleType();
		this.blackStyle.setId("blackStyle");
		IconStyleType iStyle = kmlObjectFactory.createIconStyleType();
		iStyle.setIcon(iconLink);
		iStyle.setColor(EvacuationTimePictureWriter.colorBlack);
		iStyle.setScale(EvacuationTimePictureWriter.ICONSCALE);
		this.blackStyle.setIconStyle(iStyle);
		this.document.getAbstractStyleSelectorGroup().add(kmlObjectFactory.createStyle(this.blackStyle));
		
		createSpacer(HISTOGRAMWIDTH + BOXPLOTWIDTH, 1);
	}
	
	private void createSpacer(int width, int height) throws IOException {
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);
		byte[] imageBytes = bufferedImageToByteArray(image);
		this.kmzWriter.addNonKMLFile(imageBytes, SPACER);
	}
	
	private ScreenOverlayType createHistogram(String transportMode, Map<Id, Double> evacuationTimes) throws IOException {
		
		/*
		 * Remove NaN entries from the List
		 */
		List<Double> listWithoutNaN = new ArrayList<Double>();
		for (Double d : evacuationTimes.values()) if (!d.isNaN()) listWithoutNaN.add(d);
		
		/*
		 * If trip with significant to high evacuation times should be cut off
		 */
		if (limitMaxEvacuationTime) {
			double cutOffValue = meanEvacuationTime + standardDeviation * evacuationTimeCutOffFactor;
			ListIterator<Double> iter = listWithoutNaN.listIterator();
			while (iter.hasNext()) {
				double value = iter.next();
				if (value > cutOffValue) iter.remove();
			}
		}
		
		double[] array = new double[listWithoutNaN.size()];
		int i = 0;
		for (double d : listWithoutNaN) array[i++] = d;
		
		JFreeChart chart = createHistogramChart(transportMode, array);
		BufferedImage chartImage = chart.createBufferedImage(OVERALLHISTOGRAMWIDTH, OVERALLHISTOGRAMHEIGHT);
		BufferedImage image = new BufferedImage(OVERALLHISTOGRAMWIDTH, OVERALLHISTOGRAMHEIGHT, BufferedImage.TYPE_4BYTE_ABGR);
		
		// clone image and set alpha value
		for (int x = 0; x < OVERALLHISTOGRAMWIDTH; x++) {
			for (int y = 0; y < OVERALLHISTOGRAMHEIGHT; y++) {
				int rgb = chartImage.getRGB(x, y);
				Color c = new Color(rgb);
				int r = c.getRed();
				int b = c.getBlue();
				int g = c.getGreen();
				int argb = 225<<24 | r<<16 | g<<8 | b;	// 225 as transparency value
				image.setRGB(x, y, argb);
			}
		}
		
		byte[] imageBytes = bufferedImageToByteArray(image);
		this.kmzWriter.addNonKMLFile(imageBytes, transportMode + OVERALLHISTROGRAM);

		ScreenOverlayType overlay = kmlObjectFactory.createScreenOverlayType();
		LinkType icon = kmlObjectFactory.createLinkType();
		icon.setHref(transportMode + OVERALLHISTROGRAM);
		overlay.setIcon(icon);
		overlay.setName("Histogram " + transportMode);
		// place the image top right
		Vec2Type overlayXY = kmlObjectFactory.createVec2Type();
		overlayXY.setX(0.0);
		overlayXY.setY(1.0);
		overlayXY.setXunits(UnitsEnumType.FRACTION);
		overlayXY.setYunits(UnitsEnumType.FRACTION);
		overlay.setOverlayXY(overlayXY);
		Vec2Type screenXY = kmlObjectFactory.createVec2Type();
		screenXY.setX(0.02);
		screenXY.setY(0.98);
		screenXY.setXunits(UnitsEnumType.FRACTION);
		screenXY.setYunits(UnitsEnumType.FRACTION);
		overlay.setScreenXY(screenXY);
		return overlay;
	}
	
	private ScreenOverlayType createLegend(String transportMode) throws IOException {
		double step = 0.0;
		
		if (limitMaxEvacuationTime) {
			double cutOffValue = meanEvacuationTime + standardDeviation * evacuationTimeCutOffFactor;
			step = (cutOffValue - minEvacuationTime) / colorScale.length;
		} 
		else step = (maxEvacuationTime - minEvacuationTime) / colorScale.length;
	
		String[] legendTexts = new String[colorScale.length];
		
		double from = minEvacuationTime;
		double to = step;
		
		for (int i = 0; i < legendTexts.length; i++) {
			StringBuffer sb = new StringBuffer();
			sb.append((int) from);
			sb.append(SECONDS);
			sb.append(" - ");
			sb.append((int) to);
			sb.append(SECONDS);
			legendTexts[i] = sb.toString();
			
			from = to;
			to = to + step;
		}
		
		/*
		 * If we limit the max evacuation time, then the last legend entry is not
		 * "from - to" but "from"
		 */
		if (limitMaxEvacuationTime) {
			StringBuffer sb = new StringBuffer();
			sb.append((int) from);
			sb.append(SECONDS);
			sb.append(" - ...");
			legendTexts[legendTexts.length - 1] = sb.toString();
		}
		
		BufferedImage image = new EvacuationTimeLegend().createLegend(LEGENDHEADER + transportMode, legendTexts);
		byte[] imageBytes = bufferedImageToByteArray(image);
		this.kmzWriter.addNonKMLFile(imageBytes, transportMode + LEGEND);
		
		ScreenOverlayType overlay = kmlObjectFactory.createScreenOverlayType();
		LinkType icon = kmlObjectFactory.createLinkType();
		icon.setHref(transportMode + LEGEND);
		overlay.setIcon(icon);
		overlay.setName("Legend " + transportMode);
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
	 * Writes the given JFreeChart to the kmz file specified for the kmz writer attribute of this class.
	 * @param filename the filename to use in the kmz
	 * @param chart
	 * @throws IOException
	 */
	private void writeChartToKmz(final String filename, final JFreeChart chart, int height, int width) throws IOException {
		byte[] img;
		img = ChartUtilities.encodeAsPNG(chart.createBufferedImage(height, width));
		this.kmzWriter.addNonKMLFile(img, filename);
	}
	
	private byte[] bufferedImageToByteArray(BufferedImage image) throws IOException  {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ImageIO.write(image, "PNG", out);
		byte[] imageBytes = out.toByteArray();
		return imageBytes;
	}

	/**
	 * Calculates the position of a placemark in a way that it is 40 % of the link
	 * length away from the node where the link starts. Requires that the coordinates
	 * as well as the link length use the same SI units!
	 *
	 * @param l
	 * @return the Coord instance
	 */
	private Coord getShiftedLinkCoord(final Link l) {
		
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
}
