/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.ssix;

import java.awt.Rectangle;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.jzy3d.chart.Chart;
import org.jzy3d.chart.ChartLauncher;
import org.jzy3d.colors.Color;
import org.jzy3d.colors.ColorMapper;
import org.jzy3d.colors.colormaps.ColorMapRainbow;
import org.jzy3d.global.Settings;
import org.jzy3d.maths.Coord3d;
import org.jzy3d.plot3d.primitives.Point;
import org.jzy3d.plot3d.primitives.Polygon;
import org.jzy3d.plot3d.primitives.Shape;

/* 
 * Here a means to visualize in 3D the flow or speed of mixed traffic.
 * 
 * The input file is expected to be a .txt with each line corresponding to a different modal split.
 * The points need to be on a regular grid, and flow or speed values are to be indicated by their column number
 * (see static variables used as local configuration).
 * To identify the different modal splits, the column numbers of the vehicle counts must also be given.
 * 
 * If the imports don't work, try adding the free org.jzy3d package to build path
 * (currently to be found @ http://www.jzy3d.org/release/0.9/org.jzy3d-0.9.jar [june'13])
 * 
 * @author: ssix
 */

public class Plot3DFromPolygons {

	static private String INPUT_FILE_DIR = "Z:\\WinHome\\workspace\\playgrounds-newRepository\\ssix\\output\\data_Patna_MZ_corrected.txt";
	static private int INPUT_FILE_SIZE = 133; 	  //number of interesting lines in the .txt
	static private int INPUT_FILE_MODE1_VEHICLE_COUNT_COLUMN = 0;
	static private int INPUT_FILE_MODE2_VEHICLE_COUNT_COLUMN = 1;
	static private int INPUT_FILE_FLOW_COLUMN = 7;
	static private int INPUT_FILE_SPEED_COLUMN = 11;
	static private String READING_MODE = "speed"; //other alternative is "flow". Anything else will lead to empty graph.
	
	protected Chart chart;
	protected static Rectangle DEFAULT_WINDOW = new Rectangle(0,0,600,600);
    protected String canvasType="awt";
	
	private Coord3d[] points;
	private double[][] z_values_matrix;
	private Color[] colors;
	
	
	public static void main(String[] args) throws Exception {
		Plot3DFromPolygons plot = new Plot3DFromPolygons();
		plot.openDisplayWindow();
		plot.displayMatrix();
	}
	
	public Plot3DFromPolygons() {
		z_values_matrix = new double[321][81];
		for (int i=0; i<321; i++){
			for (int j=0; j<81; j++){
				z_values_matrix[i][j] = 0;
			}
		}
	}

	public void init() throws Exception {
		//getData from .txt and save it locally
        getPointsFromData(INPUT_FILE_DIR, READING_MODE);
        //getPointsFromData("Z:\\WinHome\\workspace\\patnaIndia\\fundamentalDiagrammeMixedTrafic_ssix\\simulation_outputs\\data_Patna_all.txt",mode);
        
		//Create a surface from polygons
        List<Polygon> polygons = new ArrayList<Polygon>();
        for (int i=0; i<320; i+=20){
        	for (int j=0; j<80; j+=10){
        		Polygon polygon = new Polygon();
        		polygon.add(new Point( new Coord3d( i , j ,z_values_matrix[ i ][ j ])));
        		polygon.add(new Point( new Coord3d( i ,j+10,z_values_matrix[ i ][j+10])));
        		polygon.add(new Point( new Coord3d(i+20,j+10,z_values_matrix[i+20][j+10])));
        		polygon.add(new Point( new Coord3d(i+20, j ,z_values_matrix[i+20][ j ])));
        		polygons.add(polygon);
        	}
        }
        //Create the 3d object
        Shape surface = new Shape(polygons);
        surface.setColorMapper(new ColorMapper(new ColorMapRainbow(), surface.getBounds().getZmin(), surface.getBounds().getZmax(), new org.jzy3d.colors.Color(1,1,1,1f)));
        surface.setWireframeDisplayed(true);
        surface.setWireframeColor(org.jzy3d.colors.Color.BLACK);
        
        //Create a chart and add the surface
        this.chart = new Chart();
        this.chart.getScene().getGraph().add(surface);
		
	}
	
	private void getPointsFromData(String dir, String mode){
		this.points = new Coord3d[INPUT_FILE_SIZE];
        this.colors = new Color[INPUT_FILE_SIZE];
        
        try{
        	FileInputStream fstream = new FileInputStream(dir);
        	DataInputStream in = new DataInputStream(fstream);
        	BufferedReader br = new BufferedReader(new InputStreamReader(in));
        	String strLine;
        	br.readLine();
        	
        	int i=0;
        	while ((strLine= br.readLine()) != null){
        		//read data
        		String str[] = strLine.split("\t");
        		float bikes = (float) Integer.parseInt(str[INPUT_FILE_MODE1_VEHICLE_COUNT_COLUMN]);
        		float cars = (float) Integer.parseInt(str[INPUT_FILE_MODE2_VEHICLE_COUNT_COLUMN]);
        		String z_value_split[];
        		if (READING_MODE.equals("speed")){
        			z_value_split = str[INPUT_FILE_SPEED_COLUMN].split(",");
        		} else {
        			z_value_split = str[INPUT_FILE_FLOW_COLUMN].split(",");
        		}
        		float z_value = (float) Integer.parseInt(z_value_split[0]) + (float) 0.01 * Integer.parseInt(z_value_split[1]);
        		
        		//System.out.println("bikes: "+bikes+" cars: "+cars+" z_value: "+z_value);
        		
        		this.points[i] = new Coord3d(bikes,cars,z_value);
        		float a = 0.25f;
        		this.colors[i] = new Color(bikes, cars, z_value, a);
        		
        		this.z_values_matrix[(int)bikes][(int)cars] = z_value;
        		
        		i++;
        	}
        	in.close();
        } catch (Exception e) {
        	System.err.println("Error: " + e.getMessage());
        }
	}
	
	private void openDisplayWindow(){
		Settings.getInstance().setHardwareAccelerated(true);
		
		try {   
			this.init(); 
		} catch(Exception e) {    System.err.println("Error: " + e.getMessage());   }
		
		Chart chart = this.getChart();
		
		ChartLauncher.instructions();
		ChartLauncher.openChart(chart, DEFAULT_WINDOW, this.getName());
		//ChartLauncher.screenshot(demo.getChart(), "./data/screenshots/"+demo.getName()+".png");
	}
	
	private void displayMatrix() throws Exception{
		this.init();
		for (int i=0; i<319; i+=10){
        	for (int j=0; j<79; j+=5){
        		System.out.print(z_values_matrix[i][j]+"\t");
        	}
        	System.out.println("\n");
		}
	}
	
	public String getName() {
		return this.getClass().getSimpleName();
	}
	
	public String getPitch(){
		return "";
	}
	
	public boolean isInitialized(){
	    return chart!=null;
	}
	
	public Chart getChart(){
        return chart;
    }
	
	public String getCanvasType(){
	    return canvasType;
	}
	
    public boolean hasOwnChartControllers(){
	    return false;
	}
	
}
