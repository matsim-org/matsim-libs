/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.agarwalamit.spatial;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import org.apache.log4j.Logger;
import org.jzy3d.analysis.AbstractAnalysis;
import org.jzy3d.analysis.AnalysisLauncher;
import org.jzy3d.chart.factories.AWTChartComponentFactory;
import org.jzy3d.colors.Color;
import org.jzy3d.colors.ColorMapper;
import org.jzy3d.colors.colormaps.ColorMapRainbow;
import org.jzy3d.maths.Coord3d;
import org.jzy3d.plot3d.primitives.Shape;
import org.jzy3d.plot3d.rendering.canvas.Quality;
import org.jzy3d.plot3d.rendering.view.modes.ViewPositionMode;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.emissions.types.WarmPollutant;
import org.matsim.core.utils.io.IOUtils;

import playground.agarwalamit.analysis.LoadMyScenarios;
import playground.agarwalamit.analysis.emission.EmissionLinkAnalyzer;
import playground.southafrica.utilities.gis.MyMultiFeatureReader;
import playground.southafrica.utilities.grid.GeneralGrid;
import playground.southafrica.utilities.grid.GeneralGrid.GridType;
import playground.southafrica.utilities.grid.KernelDensityEstimator;
import playground.southafrica.utilities.grid.KernelDensityEstimator.KdeType;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;

/**
 * @author amit
 */
public class KernalDensityPlot extends AbstractAnalysis {

	private static final Logger log = Logger.getLogger(KernalDensityPlot.class);

	private final GridType type = GridType.SQUARE;
	private final double width = 170;
	private final KdeType kdeType = KdeType.GAUSSIAN;
	private final double smoothingRadius = 500;
	private final String lineOrPointFeature = "point"; //alternative is "line"
	//	if emissions is needed as z cordinate to get peaks in 3d plot, set following to false
	private final boolean polygonSurfaceChart = true;
	private final boolean printPlotToScreen = false;

	private final String runDir = "/Users/aagarwal/Desktop/ils4/agarwal/munich/output/1pct/ei/";
	private final String outputFolder = runDir+"/analysis/spatialPlots/";
	private final String shapefile ="/Users/aagarwal/workspace/shared-svn/projects/detailedEval/Net/shapeFromVISUM/urbanSuburban/cityArea.shp";//args[0];

	private final String configFile = runDir+"/output_config.xml";
	private final String networkFile = runDir+"/output_network.xml.gz";
	private final double simulationEndTime = LoadMyScenarios.getSimulationEndTime(configFile);
	private final int lastIteration = LoadMyScenarios.getLastIteration(configFile);
	private final Scenario sc = LoadMyScenarios.loadScenarioFromNetwork(networkFile);
	private final String emissionEventFile =runDir+"/ITERS/it."+lastIteration+"/"+lastIteration+".emission.events.xml.gz";

	private final GeometryFactory gf = new GeometryFactory();
	private GeneralGrid grid;
	private KernelDensityEstimator kde;
	private Map<Double,Map<Id,SortedMap<String,Double>>> linkEmissions;

	public static void main(String[] args) {
		KernalDensityPlot munichEmissionPlot = new KernalDensityPlot();
		munichEmissionPlot.run();
	}

	public void run (){
		new File(outputFolder).mkdirs(); 

		MyMultiFeatureReader mmfr = new MyMultiFeatureReader();
		try {
			mmfr.readMultizoneShapefile(shapefile, 1);
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot read shapefile from " + shapefile);
		}

		log.info("Read " + mmfr.getAllZones().size() + " zone(s).");
		grid = new GeneralGrid(width, type);

		Geometry zone = mmfr.getAllZones().get(0);
		grid.generateGrid(zone);
		grid.writeGrid(outputFolder, "EPSG:20004");//"WGS84_SA_Albers");

		kde = new KernelDensityEstimator(grid, kdeType, smoothingRadius);

		//==== reading emission data
		EmissionLinkAnalyzer emsLnkAna = new EmissionLinkAnalyzer(simulationEndTime, emissionEventFile, 1);
		emsLnkAna.init();
		emsLnkAna.preProcessData();
		emsLnkAna.postProcessData();
		linkEmissions = emsLnkAna.getLink2TotalEmissions();

		//processing kernal density
		switch(lineOrPointFeature){
		case "line" :
			log.info("======================================\n");
			log.info("==========Using Line Features=========\n");
			log.info("======================================");
			Map<LineString, Double> lineToEmissions = getLineToEmissions();
			for(LineString ls : lineToEmissions.keySet()){
				kde.processLine(ls, lineToEmissions.get(ls));
			}
			break;
		case "point" :
			log.info("=======================================\n");
			log.info("==========Using Point Features=========\n");
			log.info("=======================================");
			Map<Point, Double> pointToEmissions = getPointToEmissions();
			for(Point p :pointToEmissions.keySet()){
				kde.processPoint(p, pointToEmissions.get(p));
			}
			break;
		}
		writeRData();
//		writeRContourPlotData(); // take too much time; matrix of 17000 x 17000 and double (= may end up in approx 1 gb file)
		// instead use R function acast(data, data$centroidX ~ data$centroidY), it will give warning but will work.
		if(printPlotToScreen) printGridToScreen();
	}

	private Map<Point, Double> getPointToEmissions(){
		Map<Point, Double> pointToNO2Emissions = new HashMap<Point, Double>();
		if(linkEmissions.keySet().size()!=1) log.warn("Currently, this method will produce correct result only if noOfTimeBin is 1,"
				+ "therefore, modify method or use one timebin only.");
		for(double time :linkEmissions.keySet()){
			for(Id id : linkEmissions.get(time).keySet()){
				Link l = sc.getNetwork().getLinks().get(id);
				double x = l.getCoord().getX();
				double y = l.getCoord().getY();
				Point p = gf.createPoint(new Coordinate(x, y));
				pointToNO2Emissions.put(p, linkEmissions.get(time).get(id).get(WarmPollutant.NO2.toString()));
				// noOfTimeBin is 1, so directly can be inserted into this map else need to add linkEmissions for different time bins.
			}
		}
		return pointToNO2Emissions;
	}

	private Map<LineString, Double> getLineToEmissions(){
		Map<LineString, Double> lineToNO2Emissions = new HashMap<LineString, Double>();
		if(linkEmissions.keySet().size()!=1) log.warn("Currently, this method will produce correct result only if noOfTimeBin is 1,"
				+ "therefore, modify method or use one timebin only.");
		for(double time :linkEmissions.keySet()){
			for(Id id : linkEmissions.get(time).keySet()){
				Link l = sc.getNetwork().getLinks().get(id);
				Coordinate fromCoord = new Coordinate(l.getFromNode().getCoord().getX(), l.getFromNode().getCoord().getY());
				Coordinate toCoord = new Coordinate(l.getToNode().getCoord().getX(),l.getToNode().getCoord().getY());
				Coordinate[] cal = {fromCoord, toCoord};
				LineString ls = gf.createLineString(cal);
				lineToNO2Emissions.put(ls, linkEmissions.get(time).get(id).get(WarmPollutant.NO2.toString()));
			}
		}
		return lineToNO2Emissions;
	}

	private void writeRData(){
		log.info("====Writing data to plot polygon surface in R.====");
		String fileName = outputFolder+"rDataKernalDensitySurface"+"_"+type+"_"+lineOrPointFeature+".txt";
		BufferedWriter writer = IOUtils.getBufferedWriter(fileName);
		int noOfSidesOfPolygon = 0;
		if(this.type.equals(GridType.SQUARE)) noOfSidesOfPolygon = 4;
		else if(this.type.equals(GridType.HEX)) noOfSidesOfPolygon = 6;
		else throw new RuntimeException(this.type +" is not a valid grid type.");
		try {
			for(int i=0;i<noOfSidesOfPolygon;i++){
				writer.write("polyX"+i+"\t"+"polyY"+i+"\t");
			}
			writer.write("centroidX \t centroidY \t   weight \n ");

			for(Point p : this.grid.getGrid().values()){
				org.jzy3d.plot3d.primitives.Polygon cell = new org.jzy3d.plot3d.primitives.Polygon();
				Geometry g = this.grid.getCellGeometry(p);
				Coordinate[] ca = g.getCoordinates();
				for(int i = 0; i < ca.length-1; i++){
					cell.add(new org.jzy3d.plot3d.primitives.Point(new Coord3d(ca[i].x, ca[i].y, 0.0)));
					writer.write(ca[i].x+"\t"+ca[i].y+"\t");
				}
				double thisWeight = this.kde.getWeight(p);
				writer.write(p.getX()+"\t"+p.getY()+"\t"+thisWeight+"\n");
			}
			writer.close();
			log.info("Data is written to file "+fileName);
		} catch (Exception e) {
			throw new RuntimeException("Data is not written to file. Reason "+e);
		}
	}

	private void writeRContourPlotData(){
		log.info("====Writing data to plot filled contour in R.====");
		String fileName = outputFolder+"rDataKernalDensityContour"+"_"+type+"_"+lineOrPointFeature+".txt";
		try {
			BufferedWriter writer = IOUtils.getBufferedWriter(fileName);
			List<Double> xCoords = new ArrayList<Double>();
			List<Double> yCoords = new ArrayList<Double>();

			for(Point p : this.grid.getGrid().values()){
				if(kde.getWeight(p)>0.){
					xCoords.add(p.getX());
					yCoords.add(p.getY());
				}
			}
			Collections.sort(xCoords);
			Collections.sort(yCoords);

			writer.write("\t");
			//x-coordinates as first row
			for(int xIndex = 0; xIndex < xCoords.size(); xIndex++){
				writer.write(xCoords.get(xIndex).toString() + "\t");
			}
			writer.newLine();
			for(int yIndex = 0; yIndex < yCoords.size(); yIndex++){
				//y-coordinates as first column
				writer.write(yCoords.get(yIndex) + "\t");
				for(int xIndex =0; xIndex < xCoords.size(); xIndex++){
					Point p = gf.createPoint(new Coordinate(xCoords.get(xIndex), yCoords.get(yIndex)));
					double emissionWeight = kde.getWeight(p);
					writer.write(Double.toString(emissionWeight)+"\t");
				}
				writer.newLine();
				log.info("Writing line "+yIndex);
			}
			writer.close();
		} catch (Exception e) {
			throw new RuntimeException("Data is not written to file. Reason "+e);
		}
		log.info("Data is written to file "+fileName);
	}

	private void printGridToScreen(){
		try {
			AnalysisLauncher.open(this);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot visualise.");
		}
	}

	@Override
	public void init() throws Exception {
		chart = AWTChartComponentFactory.chart(Quality.Advanced, "awt");
		Coord3d newViewPoint = new Coord3d(3*Math.PI/2, Math.PI/2, 5000);
		chart.getView().setViewPoint(newViewPoint, true);
		chart.getView().setViewPositionMode(ViewPositionMode.FREE);

		/* First draw the original geometry. */
		org.jzy3d.plot3d.primitives.Polygon polygon = new org.jzy3d.plot3d.primitives.Polygon();
		for(Coordinate c : this.grid.getGeometry().getCoordinates()){
			polygon.add(new org.jzy3d.plot3d.primitives.Point(new Coord3d(c.x, c.y, 1.0)));
		}
		polygon.setFaceDisplayed(false);
		polygon.setWireframeDisplayed(true);
		polygon.setWireframeColor(Color.RED);

		/* Determine the alpha extent. */
		double max = 0.0;
		double sum = 0.0;
		for(Point p : this.grid.getGrid().values()){
			max = Math.max(max, this.kde.getWeight(p));
			sum += this.kde.getWeight(p);
		}
		chart.getScene().add(polygon);

		if(polygonSurfaceChart){
			polygonSurfaceChart(max);
		} else 
			weightAsZCoordChart();

		log.info("Total weight: " + sum);
		chart.getAxeLayout().setZAxeLabel("NO2 [gm]");
		chart.getView().shoot();
	}
	private void polygonSurfaceChart(double maxOfWeight){
		for(Point p : this.grid.getGrid().values()){
			org.jzy3d.plot3d.primitives.Polygon cell = new org.jzy3d.plot3d.primitives.Polygon();
			Geometry g = this.grid.getCellGeometry(p);
			Coordinate[] ca = g.getCoordinates();
			for(int i = 0; i < ca.length-1; i++){
				cell.add(new org.jzy3d.plot3d.primitives.Point(new Coord3d(ca[i].x, ca[i].y, 0.0)));
			}
			double thisWeight = this.kde.getWeight(p);

			int alpha = (int) Math.round((thisWeight / maxOfWeight)*255.0);
			Color thisColor = new Color(0,0,0, alpha);

			cell.setFaceDisplayed(true);
			cell.setColor(thisColor);
			cell.setWireframeDisplayed(true);
			cell.setWireframeColor(thisColor);
			chart.getScene().add(cell);
		}
	}
	private void weightAsZCoordChart() {
		List<org.jzy3d.plot3d.primitives.Polygon> polygons = new ArrayList<org.jzy3d.plot3d.primitives.Polygon>();
		for(Point p: this.grid.getGrid().values()){
			org.jzy3d.plot3d.primitives.Polygon cell = new org.jzy3d.plot3d.primitives.Polygon();
			Geometry g = this.grid.getCellGeometry(p);
			double thisWeight = this.kde.getWeight(p);
			Coordinate[] ca = g.getCoordinates();
			for(int i = 0; i < ca.length-1; i++){
				org.jzy3d.plot3d.primitives.Point point = new org.jzy3d.plot3d.primitives.Point(new Coord3d(ca[i].x, ca[i].y, thisWeight));
				cell.add(point);
			}
			polygons.add(cell);
		}
		Shape surface = new Shape(polygons);
		surface.setColorMapper(new ColorMapper(new ColorMapRainbow(), surface.getBounds().getZmin(), surface.getBounds().getZmax(), new org.jzy3d.colors.Color(1,1,1,1f)));
		surface.setWireframeDisplayed(false);// no wireframe.
		surface.setFaceDisplayed(true);
		chart.getScene().getGraph().add(surface);
	}
}
