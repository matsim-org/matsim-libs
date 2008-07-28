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

import java.io.IOException;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.matsim.basic.v01.Id;
import org.matsim.counts.CountSimComparison;
import org.matsim.counts.algorithms.graphs.BiasErrorGraph;
import org.matsim.counts.algorithms.graphs.BoxPlotErrorGraph;
import org.matsim.counts.algorithms.graphs.CountsGraph;
import org.matsim.counts.algorithms.graphs.CountsLoadCurveGraph;
import org.matsim.counts.algorithms.graphs.CountsLoadCurveGraphCreator;
import org.matsim.counts.algorithms.graphs.CountsSimReal24Graph;
import org.matsim.counts.algorithms.graphs.CountsSimRealPerHourGraph;
import org.matsim.gbl.MatsimResource;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.utils.geometry.CoordI;
import org.matsim.utils.geometry.CoordinateTransformationI;
import org.matsim.utils.geometry.shared.Coord;
import org.matsim.utils.vis.kml.ColorStyle;
import org.matsim.utils.vis.kml.Document;
import org.matsim.utils.vis.kml.Folder;
import org.matsim.utils.vis.kml.Icon;
import org.matsim.utils.vis.kml.IconStyle;
import org.matsim.utils.vis.kml.KML;
import org.matsim.utils.vis.kml.KMLWriter;
import org.matsim.utils.vis.kml.KMZWriter;
import org.matsim.utils.vis.kml.Placemark;
import org.matsim.utils.vis.kml.Point;
import org.matsim.utils.vis.kml.ScreenOverlay;
import org.matsim.utils.vis.kml.Style;
import org.matsim.utils.vis.kml.TimeSpan;
import org.matsim.utils.vis.kml.fields.Color;
import org.matsim.utils.vis.kml.fields.Vec2Type;
import org.matsim.utils.vis.matsimkml.MatsimKMLLogo;
import org.matsim.utils.vis.matsimkml.NetworkFeatureFactory;

/**
 * @author dgrether
 */
public class CountSimComparisonKMLWriter extends CountSimComparisonWriter {
	/**
	 * constant for the name of the links
	 */
	private static final String LINK = "Link: ";
	/**
	 * constant for the link description
	 */
	private static final String COUNTVALUE = "Count Value: ";
	/**
	 * constant for the link description
	 */
	private static final String MATSIMVALUE = "MATSim Value: ";
	/**
	 * constant for the link description
	 */
	private static final String RELERROR = "Relative Error: ";
	/**
	 * constant for the link description
	 */
	private static final String IMG = "<img src=\"./";
	/**
	 * constant for the link description
	 */
	private static final String IMGEND = "\">";
	/**
	 * constant for the link description
	 */
	private static final String H24OVERVIEW = "24 h overview";
	/**
	 * constant for the link description
	 */
	private static final String DETAILSFROM = "Details from ";
	/**
	 * constant for the link description
	 */
	private static final String OCLOCKTO = " o'clock to ";
	/**
	 * constant for the link description
	 */
	private static final String OCLOCK = " o'clock";
	/**
	 * constant for the link description
	 */
	private static final String ZERO = "0";
	/**
	 * the icons
	 */
	private static final String CROSSICON = "icons/plus.png";
	/**
	 * the icons
	 */
	private static final String MINUSICON = "icons/minus.png";
	/**
	 * the scale for the icons
	 */
	private static final double ICONSCALE = 0.5;
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
	/**
	 * constant for the file name of the CountsSimRealPerHourGraphs
	 */
	private static final String SIMREALGRAPHNAME = "countsSimRealPerHour_";
	/**
	 * the network
	 */
	private final NetworkLayer network;
	/**
	 * the srs transformation used
	 */
	private CoordinateTransformationI coordTransform = null;
	/**
	 * main kml, doc and folder
	 */
	private KML mainKml = null;
	/**
	 * the kml main document
	 */
	private Document mainDoc = null;
	/**
	 * The kml main folder
	 */
	private Folder mainFolder = null;
	/**
	 * the kmz writer
	 */
	private KMZWriter writer = null;
	/**
	 * style for one of the icons
	 */
	private Style redCrossStyle;
	/**
	 * style for one of the icons
	 */
	private Style redMinusStyle;
	/**
	 * style for one of the icons
	 */
	private Style yellowCrossStyle;
	/**
	 * style for one of the icons
	 */
	private Style yellowMinusStyle;
	/**
	 * style for one of the icons
	 */
	private Style greenMinusStyle;
	/**
	 * style for one of the icons
	 */
	private Style greenCrossStyle;
	/**
	 * style for one of the icons
	 */
	private Style greyCrossStyle;
	/**
	 * style for one of the icons
	 */
	private Style greyMinusStyle;
	/**
	 * maps linkids to filenames in the kmz
	 */
	private Map<String, String> countsLoadCurveGraphMap;

	/** The logging object for this class. */
	private static final Logger log = Logger.getLogger(CountSimComparisonKMLWriter.class);

	/**
	 * Sets the data to the fields of this class
	 * @param countSimCompList
	 * @param network
	 * @param coordTransform
	 */
	public CountSimComparisonKMLWriter(final List<CountSimComparison> countSimCompList, final NetworkLayer network, final CoordinateTransformationI coordTransform) {
		super(countSimCompList);
		this.network = network;
		this.coordTransform = coordTransform;
	}

	/**
	 * This method initializes the styles for the different icons used.
	 */
	private void createStyles() {
		this.redCrossStyle = new Style("redCrossStyle");
		this.redMinusStyle = new Style("redMinusStyle");
		this.yellowCrossStyle = new Style("yellowCrossStyle");
		this.yellowMinusStyle = new Style("yellowMinusStyle");
		this.greenCrossStyle = new Style("greenCrossStyle");
		this.greenMinusStyle = new Style("greenMinusStyle");
		this.greyCrossStyle = new Style("greyCrossStyle");
		this.greyMinusStyle = new Style("greyMinusStyle");

		Color red = new Color("ff", "0f", "0f", "be");
		Color green = new Color("ff", "14", "dc", "0a");
		Color yellow = new Color("ff", "14", "e6", "e6");
		Color grey = new Color("ff", "42", "42", "42");

		this.redCrossStyle.setIconStyle(new IconStyle(new Icon(CROSSICON), red, ColorStyle.DEFAULT_COLOR_MODE, ICONSCALE));
		this.redMinusStyle.setIconStyle(new IconStyle(new Icon(MINUSICON), red, ColorStyle.DEFAULT_COLOR_MODE, ICONSCALE));
		this.yellowCrossStyle.setIconStyle(new IconStyle(new Icon(CROSSICON), yellow, ColorStyle.DEFAULT_COLOR_MODE, ICONSCALE));
		this.yellowMinusStyle.setIconStyle(new IconStyle(new Icon(MINUSICON), yellow, ColorStyle.DEFAULT_COLOR_MODE, ICONSCALE));
		this.greenCrossStyle.setIconStyle(new IconStyle(new Icon(CROSSICON), green, ColorStyle.DEFAULT_COLOR_MODE, ICONSCALE));
		this.greenMinusStyle.setIconStyle(new IconStyle(new Icon(MINUSICON), green, ColorStyle.DEFAULT_COLOR_MODE, ICONSCALE));
		this.greyCrossStyle.setIconStyle(new IconStyle(new Icon(CROSSICON), grey, ColorStyle.DEFAULT_COLOR_MODE, ICONSCALE));
		this.greyMinusStyle.setIconStyle(new IconStyle(new Icon(MINUSICON), grey, ColorStyle.DEFAULT_COLOR_MODE, ICONSCALE));

		this.mainDoc.addStyle(this.redCrossStyle);
		this.mainDoc.addStyle(this.redMinusStyle);
		this.mainDoc.addStyle(this.yellowCrossStyle);
		this.mainDoc.addStyle(this.yellowMinusStyle);
		this.mainDoc.addStyle(this.greenCrossStyle);
		this.mainDoc.addStyle(this.greenMinusStyle);
		this.mainDoc.addStyle(this.greyCrossStyle);
		this.mainDoc.addStyle(this.greyMinusStyle);
	}

	/**
	 * Writes the data to the file at the path given as String
	 *
	 * @param filename
	 */
	public void writeFile(final String filename) {
		Folder subfolder;
		// init kml
		this.mainKml = new KML();
		this.mainDoc = new Document(filename);
		this.mainKml.setFeature(this.mainDoc);
		// create the styles and the folders
		createStyles();
		// create a folder
		this.mainFolder = new Folder("2dnetworklinksfolder");
		this.mainFolder.setName("Comparison, Iteration " + this.iterationNumber);
		this.mainDoc.addFeature(this.mainFolder);
		// the writer
		this.writer = new KMZWriter(filename, KMLWriter.DEFAULT_XMLNS);

		try {
			//try to create the legend
			this.mainFolder.addFeature(createLegend());
		} catch (IOException e) {
			log.error("Cannot add legend to the KMZ file.", e);
		}
		try {
			//add the matsim logo to the kml
			this.mainFolder.addFeature(new MatsimKMLLogo(this.writer));
		} catch (IOException e) {
			log.error("Cannot add logo to the KMZ file.", e);
		}

		try {
			// copy required icons to the kmz
			this.writer.addNonKMLFile(MatsimResource.getAsInputStream("icons/plus.png"), CROSSICON);
			this.writer.addNonKMLFile(MatsimResource.getAsInputStream("icons/minus.png"), MINUSICON);
		} catch (IOException e) {
			log.error("Could not copy copy plus-/minus-icons to the KMZ.", e);
		}

		// prepare folders for simRealPerHour-Graphs (top-left, xy-plots)
		Folder simRealFolder = new Folder("simRealPerHour");
		simRealFolder.setName("XY Comparison Plots");
		this.mainFolder.addFeature(simRealFolder);

		// error graphs and awtv graph
		ScreenOverlay errorGraph = createBiasErrorGraph();
		errorGraph.setVisibility(true);
		this.mainFolder.addFeature(errorGraph);
		errorGraph = createBoxPlotErrorGraph();
		if (errorGraph != null) {
			errorGraph.setVisibility(false);
			this.mainFolder.addFeature(errorGraph);
		}
		ScreenOverlay awtv=this.createAWTVGraph();
		if (awtv != null) {
			awtv.setVisibility(false);
			this.mainFolder.addFeature(awtv);
		}

		// link graphs
		this.createCountsLoadCurveGraphs();

		// hourly data...
		int minHour = 1;
		int maxHour = 25;
		if (this.timeFilter != null) {
			minHour = this.timeFilter.intValue();
			maxHour = minHour;
		}
		for (int h = minHour; h < maxHour; h++) {
			// the timespan for this hour
			TimeSpan timespan = new TimeSpan(new GregorianCalendar(1999, 0, 1, h - 1, 0, 0), new GregorianCalendar(1999, 0, 1, h, 0, 0));

			// first add the xyplot ("SimRealPerHourGraph") as overlay
			this.addCountsSimRealPerHourGraphs(simRealFolder, h, timespan);

			// add the placemarks for the links in this hour
			subfolder = new Folder("timestep".concat(Integer.toString(h)));
			subfolder.setName(createFolderName(h));
			subfolder.setTimePrimitive(timespan);
			this.mainFolder.addFeature(subfolder);

			writeLinkData(this.countComparisonFilter.getCountsForHour(Integer.valueOf(h)), subfolder);
		}
		finish();
	}

	/**
	 * Creates the string for the foldername
	 * @param timestep
	 * @return a timestep specific standard string
	 */
	private String createFolderName(final int timestep) {
		StringBuffer buffer = new StringBuffer();
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
	private ScreenOverlay createLegend() throws IOException {
		this.writer.addNonKMLFile(MatsimResource.getAsInputStream("countsKml/countsLegend240x300.png"), "countsLegend.png");
		ScreenOverlay overlay = new ScreenOverlay("Legend");
		Icon icon = new Icon("./countsLegend.png");
    overlay.setIcon(icon);
    overlay.setName("Legend");
    // place the image bottom left
    Vec2Type overlayXY = new Vec2Type(0.0d, 0.0d, Vec2Type.Units.fraction, Vec2Type.Units.fraction);
    Vec2Type screenXY = new Vec2Type(0.02d, 0.07d, Vec2Type.Units.fraction, Vec2Type.Units.fraction);
    overlay.setOverlayXY(overlayXY);
    overlay.setScreenXY(screenXY);
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
	private Placemark createPlacemark(final String linkid, final CountSimComparison csc, final double relativeError, final int timestep) {
		StringBuffer stringBuffer = new StringBuffer();
		Placemark placemark = new Placemark(linkid);
		stringBuffer.delete(0, stringBuffer.length());
		stringBuffer.append(LINK);
		stringBuffer.append(linkid);
		placemark.setDescription(createPlacemarkDescription(linkid, csc, relativeError, timestep));
		return placemark;
	}

	/**
	 * This method writes all the data for each of the links/counts to the kml
	 * document.
	 *
	 * @param countSimComparisonList provides "the data"
	 * @param folder The folder to which to add the data in the kml-file.
	 */
	private void writeLinkData(final List<CountSimComparison> countSimComparisonList, final Folder folder) {
		Id linkid;
		Link link;
		Placemark placemark;
		double relativeError;
		CoordI coord;
		Point point;
		for (CountSimComparison csc : countSimComparisonList) {
			linkid = csc.getId();
			link = this.network.getLink(linkid);

			coord = this.coordTransform.transform(calculatePlacemarkPosition(link));
			relativeError = csc.calculateRelativeError();
			// build placemark
			placemark = createPlacemark(linkid.toString(), csc, relativeError, csc.getHour());
		  point = new Point(coord.getX(), coord.getY(), 0.0);
		  placemark.setGeometry(point);
			// cross
		  if (csc.getSimulationValue() > csc.getCountValue()) {
		  	if (csc.getSimulationValue() < csc.getCountValue() * 1.5) {
					placemark.setStyleUrl(this.greenCrossStyle.getStyleUrl());
			  }
			  else if (csc.getSimulationValue() < csc.getCountValue() * 2) {
			  	placemark.setStyleUrl(this.yellowCrossStyle.getStyleUrl());
			  }
			  else {
			  	placemark.setStyleUrl(this.redCrossStyle.getStyleUrl());
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
			folder.addFeature(placemark);
		}
	}
	/**
	 * Calculates the position of a placemark in a way that it is 40 % of the link
	 * length away from the node where the link starts.
	 *
	 * @param l
	 * @return the CoordI instance
	 */
	private CoordI calculatePlacemarkPosition(final Link l) {
		CoordI coordFrom = l.getFromNode().getCoord();
		CoordI coordTo = l.getToNode().getCoord();
		double xDiff = coordTo.getX() - coordFrom.getX();
		double yDiff = coordTo.getY() - coordFrom.getY();
		double length = Math.sqrt((xDiff*xDiff) + (yDiff*yDiff));
		double scale = 0.4;
		scale = l.getLength() * scale;
		CoordI vec = new Coord(coordFrom.getX() + (xDiff * scale/length), coordFrom.getY() + (yDiff * scale/length));
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
	private String createPlacemarkDescription(final String linkid, final CountSimComparison csc, final double relativeError, final int timestep) {
		StringBuffer buffer = new StringBuffer();
		buffer.append(NetworkFeatureFactory.STARTCDATA);
// buffer.append(STARTH1);
// buffer.append(LINK);
// buffer.append(linkid);
// buffer.append(ENDH1);
		buffer.append(NetworkFeatureFactory.STARTH2);
		buffer.append(LINK);
		buffer.append(linkid);
		buffer.append(NetworkFeatureFactory.ENDH2);
		buffer.append(NetworkFeatureFactory.STARTH3);
		buffer.append(H24OVERVIEW);
		buffer.append(NetworkFeatureFactory.ENDH3);
		buffer.append(NetworkFeatureFactory.STARTP);
		buffer.append(IMG);
		buffer.append(this.countsLoadCurveGraphMap.get(linkid));
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
		buffer.append(relativeError);
		buffer.append(NetworkFeatureFactory.ENDP);
		buffer.append(NetworkFeatureFactory.ENDCDATA);
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
	 * Creates CountsSimRealPerHourGraphs and adds them to the kmz in the given folder. The creation of the graphs is
	 * only done if the map attribute of this class for CountsSimRealPerHourGraphs is null.
	 *
	 * @param folder
	 * @param timestep
	 * @param timespan
	 */
	private void addCountsSimRealPerHourGraphs(final Folder folder, final int timestep, final TimeSpan timespan) {
		StringBuffer filename;
		ScreenOverlay overlay;

		try {
			//add the file to the kmz
			filename = new StringBuffer(SIMREALGRAPHNAME);
			filename.append(Integer.toString(timestep));
			filename.append(PNG);

			CountsSimRealPerHourGraph graph = new CountsSimRealPerHourGraph(this.countComparisonFilter.getCountsForHour(null), this.iterationNumber, filename.toString());
			graph.createChart(timestep);

			this.writeChartToKmz(filename.toString(), graph.getChart());
			//and link with the overlay
			overlay = new ScreenOverlay(filename.toString());
			Icon icon = new Icon("./" + filename.toString());
	    overlay.setIcon(icon);
	    overlay.setName(graph.getChartTitle());
	    // place the image top left
	    Vec2Type overlayXY = new Vec2Type(0.0d, 1.0d, Vec2Type.Units.fraction, Vec2Type.Units.fraction);
	    Vec2Type screenXY = new Vec2Type(0.02d, 0.98d, Vec2Type.Units.fraction, Vec2Type.Units.fraction);
	    overlay.setOverlayXY(overlayXY);
	    overlay.setScreenXY(screenXY);
	    overlay.setTimePrimitive(timespan);
	   	//add the overlay to the folder
	   	folder.addFeature(overlay);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Creates CountsLoadCurveGraphs for each link and puts them in the kmz as pngs
	 */
	private void createCountsLoadCurveGraphs() {
		CountsLoadCurveGraphCreator cgc = new CountsLoadCurveGraphCreator("");
		List<CountsGraph> graphs= cgc.createGraphs(this.countComparisonFilter.getCountsForHour(this.timeFilter), this.iterationNumber);

		this.countsLoadCurveGraphMap = new HashMap<String, String>(graphs.size());
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
	 * Writes the given JFreeChart to the kmz file specified for the kmz writer attribute of this class.
	 * @param filename the filename to use in the kmz
	 * @param chart
	 * @throws IOException
	 */
	private void writeChartToKmz(final String filename, final JFreeChart chart) throws IOException {
		byte [] img;
		img = ChartUtilities.encodeAsPNG(chart.createBufferedImage(CHARTWIDTH, CHARTHEIGHT));
		this.writer.addNonKMLFile(img, filename);
	}

	/**
	 * Creates the CountsErrorGraph for all the data
	 * @param visible true if initially visible
	 * @return the ScreenOverlay Feature
	 */
	private ScreenOverlay createBoxPlotErrorGraph() {

		CountsGraph ep;
		try {
			ep = new BoxPlotErrorGraph(this.countComparisonFilter.getCountsForHour(this.timeFilter), this.iterationNumber, null, "error graph");
			ep.createChart(0);
		} catch (IllegalArgumentException e) {
			log.error("Could not create BoxPlot-ErrorGraph.", e);
			return null;
		}

		String filename = "errorGraphBoxPlot.png";
		try {
		  writeChartToKmz(filename, ep.getChart());
		  ScreenOverlay overlay = new ScreenOverlay("ErrorGraph BoxPlot");
		  Icon icon1 = new Icon("./" + filename);
			overlay.setIcon(icon1);
			overlay.setName("Error Graph [Box-Plot]");
	    // place the image bottom right
	    Vec2Type overlayXY = new Vec2Type(1.0d, 0.0d, Vec2Type.Units.fraction, Vec2Type.Units.fraction);
	    Vec2Type screenXY = new Vec2Type(0.98d, 0.1d, Vec2Type.Units.fraction, Vec2Type.Units.fraction);

	    overlay.setOverlayXY(overlayXY);
	    overlay.setScreenXY(screenXY);

	    return overlay;
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
	private ScreenOverlay createBiasErrorGraph() {
		CountsGraph ep = new BiasErrorGraph(this.countComparisonFilter.getCountsForHour(this.timeFilter), this.iterationNumber, null, "error graph");
		ep.createChart(0);

		String filename = "errorGraphErrorBias.png";
		try {
			writeChartToKmz(filename, ep.getChart());
			ScreenOverlay overlay = new ScreenOverlay("ErrorGraph Error/Bias");
			Icon icon1 = new Icon("./" + filename);
			overlay.setIcon(icon1);
			overlay.setName("Error Graph [Error/Bias]");
			// place the image bottom right
			Vec2Type overlayXY = new Vec2Type(1.0d, 0.0d, Vec2Type.Units.fraction, Vec2Type.Units.fraction);
			Vec2Type screenXY = new Vec2Type(0.98d, 0.1d, Vec2Type.Units.fraction, Vec2Type.Units.fraction);

			overlay.setOverlayXY(overlayXY);
			overlay.setScreenXY(screenXY);
			return overlay;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Creates the CountsSimReal24Graph for all the data
	 * @param visible true if initially visible
	 * @return the ScreenOverlay Feature
	 */
	private ScreenOverlay createAWTVGraph() {
		CountsGraph awtv = new CountsSimReal24Graph(this.countComparisonFilter.getCountsForHour(null), this.iterationNumber, "awtv graph");
		awtv.createChart(0);

		String filename = "awtv.png";
		try {
			writeChartToKmz(filename, awtv.getChart());
			ScreenOverlay overlay = new ScreenOverlay("AWTV");
			Icon icon1 = new Icon("./" + filename);
			overlay.setIcon(icon1);
			overlay.setName("AWTV");
			// place the image bottom right
			Vec2Type overlayXY = new Vec2Type(1.0d, 0.0d, Vec2Type.Units.fraction, Vec2Type.Units.fraction);
			Vec2Type screenXY = new Vec2Type(0.98d, 0.1d, Vec2Type.Units.fraction, Vec2Type.Units.fraction);

			overlay.setOverlayXY(overlayXY);
			overlay.setScreenXY(screenXY);
			return overlay;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * to be called when the kml stream shall be closed.
	 *
	 */
	private void finish() {
		this.writer.writeMainKml(this.mainKml);
		this.writer.close();
	}

}
