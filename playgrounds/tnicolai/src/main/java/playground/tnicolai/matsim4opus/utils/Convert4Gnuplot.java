/* *********************************************************************** *
 * project: org.matsim.*
 * Convert4Gnuplot.java
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

/**
 * 
 */
package playground.tnicolai.matsim4opus.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

import org.matsim.core.utils.charts.XYLineChart;
import org.matsim.core.utils.io.IOUtils;

import playground.tnicolai.matsim4opus.constants.Constants;
import playground.tnicolai.matsim4opus.utils.io.filter.TabFilter;


/**
 * @author thomas
 *
 */
public class Convert4Gnuplot {
	// sample arguments
//	/Users/thomas/Documents/SVN_Studies/tnicolai/cupum/Data/Ferry_Scenario/run_33.2010_12_26_09_30/indicators/ /Users/thomas/Documents/SVN_Studies/tnicolai/cupum/Data/Highway_Scenario/run_32.2010_12_25_13_45/indicators/ 908

	
	private static String source1;
	private static File[] fileList1;
	private static String source2;
	private static File[] fileList2;
	
	private static int zone_id;
	private static int zone_id_tmp;
	
	private static boolean isSingleDataSet = true;
	
	private static HeaderObject ho1 = null;
	private static HeaderObject ho2 = null;
	
	/**
	 * starting point
	 * @param args
	 */
	public static void main(String args[]){
		try {
			System.out.println("Starting Convert4Gnuplot");
			
//			String[] myArgs = new String[]{"/Users/thomas/Development/opus_home/data/psrc_parcel_cupum_preliminary/runs/run_3.2011_01_17_09_37/indicators", "908"};

			// multi, mit worklogsum, mit ELCM
//			String[] myArgs = new String[]{"/Users/thomas/Documents/SVN_Studies/tnicolai/cupum/Indicators/ferry_mit_worklogsum_mitELCM", "/Users/thomas/Documents/SVN_Studies/tnicolai/cupum/Indicators/highway_mit_worklogsum_mitELCM", "908"};
//			String[] myArgs = new String[]{"/Users/thomas/Documents/SVN_Studies/tnicolai/cupum/Indicators/ferry_accessibility_indicators", "/Users/thomas/Documents/SVN_Studies/tnicolai/cupum/Indicators/highway_accessibility_indicators", "908"};
			
			
			String[] args1 = new String[]{"/Users/thomas/Development/opus_home/vsp_configs/re-estimate_travel_data_related_models/create_plots/highway_low_cap_accessibility_indicators", "908"};		
			String[] args2 = new String[]{"/Users/thomas/Development/opus_home/vsp_configs/re-estimate_travel_data_related_models/create_plots/highway_accessibility_indicators", "908"};		
			String[] args3 = new String[]{"/Users/thomas/Development/opus_home/vsp_configs/re-estimate_travel_data_related_models/create_plots/ferry_accessibility_indicators", "908"};		
			
			ArrayList<String[]>tasks = new ArrayList<String[]>();
			tasks.add(args1);
			tasks.add(args2);	
			tasks.add(args3);
			
			for(int i = 0; i < tasks.size(); i++){
	//			init(args);
				init(tasks.get(i));
				System.out.println("Starting queue process ...");
				if(isSingleDataSet)
					queueSingleDataSet();
				else
					queueMultipleDataSets();
			}
			System.out.println("Finished!");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch(Exception e){
			e.printStackTrace();
		}
	}
	
	/**
	 * init variables
	 * @param args
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	private static void init(String args[]) throws FileNotFoundException, IOException{
		System.out.println("Init program ...");

		source1 = args[0];
		System.out.println("Set working directory to: " + source1);
		File folder = new File(source1);
		fileList1 = folder.listFiles(new TabFilter());

		if (args.length == 3){
			System.out.println("Detected multiple data set");
			isSingleDataSet = false;
			
			source2 = args[1];
			System.out.println("Set second  working directory to: " + source2);
			folder = new File(source2);
			fileList2 = folder.listFiles(new TabFilter());
			
			if(!dataSetsAreOk())
				System.exit(-1);
			
			zone_id = Integer.parseInt(args[2]);
		}		
		else
			zone_id = Integer.parseInt(args[1]);
		
		zone_id_tmp = zone_id;

		System.out.println("... finished init.");
	}
	
	/**
	 * checks the correct number of data sets in each folder (number of data sets should be equal) and
	 * the data set names (every data set needs to be in both folders).
	 * 
	 * @return boolean
	 */
	private static boolean dataSetsAreOk(){
		
		if( fileList1 != null && fileList2 != null && (fileList1.length == fileList2.length) ){
			
			for(File file1 : fileList1){
				String fileName1 = file1.getName();
				boolean foundRelatedFile = false;
				for(File file2 : fileList2){

					String fileName2 = file2.getName();
					
					if(fileName1.equalsIgnoreCase(fileName2)){
						foundRelatedFile = true;
						break;
					}
				}
				if(!foundRelatedFile){
					System.err.println("Didn't found related file for : " + fileName1);
					return false;
				}
			}
			return true;
		}
		System.err.println("Number of datasets not equal or fileList variables is null.");
		return false;		
	}
	
	/**
	 * create JFreeChart
	 * @param filename
	 * @param title
	 * @param xAxisLabel
	 * @param yAxisLable
	 * @param series
	 * @param xAxis
	 * @param yAxis
	 */
	public static void writeChartSingleDataSet(String filename, String title, String xAxisLabel, String yAxisLable, String series, double[] xAxis, double[] yAxis) {
		System.out.println("Writing chart: " + filename);

		XYLineChart chart = new XYLineChart(title, xAxisLabel, yAxisLable);
		chart.addSeries(series, xAxis, yAxis);
//		chart.addMatsimLogo();
		chart.saveAsPng(filename, 1920, 1080);
		
		System.out.println("... finished writing chart.");
	}
	
	public static void writeChartMultipleDataSet(String filename1, String filename2, String title, String xAxisLabel, String yAxisLable, String series1, String series2, double[] xAxis1, double[] xAxis2, double[] yAxis1, double[] yAxis2) {
		System.out.println("Writing chart to: ");
		System.out.println(filename1);
		System.out.println(filename2);

		XYLineChart chart = new XYLineChart(title, xAxisLabel, yAxisLable);
		chart.addSeries(series1, xAxis1, yAxis1);
		chart.addSeries(series2, xAxis2, yAxis2);
//		chart.addMatsimLogo();
		chart.saveAsPng(filename1, 1920, 1080);
		chart.saveAsPng(filename2, 1920, 1080);
		
		System.out.println("... finished writing chart.");
	}
	
	/**
	 * 
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	private static void queueSingleDataSet() throws FileNotFoundException, IOException{
		
		String line;
		int id;
		String[] parts = null;
		
		String title, xAxisLabel, yAxisLable, series;
		
		double xAxis[];
		double yAxis[];
		// tnicolai: sort by years
		for(File file : fileList1){
			
			String source = file.getCanonicalPath();
			String destinationPNG = source.replace(".tab", ".png");
			String destinationDAT = source.replace(".tab", ".dat");
			
			System.out.println("Processing : " + source);
			
			checkAndRestoreZoneID(source);
			
			BufferedReader br = IOUtils.getBufferedReader(source);
			// BufferedWriter and StringBuffer for gnuplot only 
			BufferedWriter bw = IOUtils.getBufferedWriter(destinationDAT);
			StringBuffer content = new StringBuffer("");
			
			// read header
			line = br.readLine();
			parts = line.split(Constants.TAB);
			
			ho1 = initHeaderObject(line);
			
			xAxisLabel = "years";
			yAxisLable = getYAxisLabel(parts);
			series = yAxisLable;
			title = yAxisLable + " in zone " + zone_id;
			
			xAxis = ho1.getSortedHeaderAsDouble();
			yAxis = new double[xAxis.length];
			
			while ( (line = br.readLine()) != null ){

				parts = line.split("\t");
				id = Integer.parseInt(parts[ho1.getZoneId()]);
				
				if( id == zone_id ){
					
					System.out.print("");
					System.out.println("Found zone : " + id);
					
					for(int i = 1; i < parts.length; i++){
						// for JFreeChart
						yAxis[i-1] = Double.parseDouble(parts[ho1.getIndexOf(i)]);
						// for gnuplot
						content.append( ((int)xAxis[i-1]) + Constants.TAB + yAxis[i-1] + Constants.NEW_LINE);
					}
					break;
				}
			}
			// create JFreeChart
			// writeChartSingleDataSet(destinationPNG, title, xAxisLabel, yAxisLable, series, xAxis, yAxis);
			// create gnuplot dat
			System.out.println("Writing gnuplot dat: " + destinationDAT);
			bw.write(content.toString());
			bw.flush();
			bw.close();
			System.out.println("Finished writing gnuplot dat");
		}
	}
	
	/**
	 * 
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	private static void queueMultipleDataSets() throws FileNotFoundException, IOException{
		
		String lineSource1, lineSource2;
		int id1, id2;
		String[] parts1, parts2;
		
		String title, xAxisLabel, yAxisLable, series1, series2;
		double xAxis1[];
		double xAxis2[];
		double yAxis1[];
		double yAxis2[];
		
		for(int i = 0; i < fileList1.length; i++){
			
			String source1 = fileList1[i].getCanonicalPath();
			String source2 = fileList2[i].getCanonicalPath();
			
			String destination1PNG = source1.replace(".tab", ".png");
			String destination1DAT = source1.replace(".tab", ".dat");
			String destination2PNG = source2.replace(".tab", ".png");
			String destination2DAT = source2.replace(".tab", ".dat");
			
			System.out.println("Processing : " + source1 + " and " + source2);
			
			checkAndRestoreZoneID(source1);
			
			BufferedReader br1 = IOUtils.getBufferedReader(source1);
			BufferedReader br2 = IOUtils.getBufferedReader(source2);
			// BufferedWriter and StringBuffer for gnuplot olny 
			BufferedWriter bw1 = IOUtils.getBufferedWriter(destination1DAT);
			BufferedWriter bw2 = IOUtils.getBufferedWriter(destination2DAT);
			StringBuffer content1 = new StringBuffer("");
			StringBuffer content2 = new StringBuffer("");
			
			// read header (should be equal in both data set files)!
			lineSource1 = br1.readLine();
			lineSource2 = br2.readLine();
			if(!lineSource1.equalsIgnoreCase(lineSource2))
				System.err.println("Different headers in " + source1 + " and " + source2);
			
			parts1 = lineSource1.split(Constants.TAB);
			parts2 = lineSource2.split(Constants.TAB);
			
			ho1 = initHeaderObject(lineSource1);
			ho2 = initHeaderObject(lineSource2);
			
			xAxisLabel = "years";
			yAxisLable = getYAxisLabel(parts1);
			series1 = "Ferry Scenario";
			series2 = "Highway Scenario";
			title = yAxisLable + " in zone " + zone_id;
			
			xAxis1 = ho1.getSortedHeaderAsDouble();
			xAxis2 = ho2.getSortedHeaderAsDouble();
			yAxis1 = new double[xAxis1.length];
			yAxis2 = new double[xAxis2.length];
			
			boolean foundZoneID1 = false;
			boolean foundZoneID2 = false;
			
			while( !(foundZoneID1 && foundZoneID2) ){
				
				lineSource1 = br1.readLine();
				lineSource2 = br2.readLine();
				
				if(lineSource1 == null || lineSource2 == null){
					System.err.println("Zone " + zone_id + " not found in " + fileList1[i].getName());
					break;
				}
				
				parts1 = lineSource1.split(Constants.TAB);
				parts2 = lineSource2.split(Constants.TAB);
				
				if(parts1.length != parts2.length)
					System.err.println("Number of columns differ in " + source1 + " and " + source2 + ".");
				
				id1 = Integer.parseInt(parts1[ho1.getZoneId()]);
				id2 = Integer.parseInt(parts2[ho2.getZoneId()]);
				
				if( (id1 == zone_id) ){
					
					System.out.println("Found zone in source dir 1 : " + id1);
					
					for(int j = 1; j < parts1.length; j++){
						// for JFreeChart
						yAxis1[j-1] = Double.parseDouble(parts1[ho1.getIndexOf(j)]);
						// for gnuplot
						content1.append( ((int)xAxis1[j-1]) + Constants.TAB + yAxis1[j-1] + Constants.NEW_LINE);
					}
					foundZoneID1 = true;
				}
				if(id2 == zone_id){
					
					System.out.println("Found zone in source dir 2 : " + id1);
					
					for(int j = 1; j < parts2.length; j++){
						// for JFreeChart
						yAxis2[j-1] = Double.parseDouble(parts2[ho2.getIndexOf(j)]);
						// for gnuplot
						content2.append( ((int)xAxis2[j-1]) + Constants.TAB + yAxis2[j-1] + Constants.NEW_LINE);
					}
					foundZoneID2 = true;
				}
			}
			if(foundZoneID1 && foundZoneID2){
//				writeChartMultipleDataSet(destination1PNG, destination2PNG, title, xAxisLabel, yAxisLable, series1, series2, xAxis1, xAxis2, yAxis1, yAxis2);
				// create gnuplot dat
				System.out.println("Writing gnuplot dat: " + destination1DAT);
				bw1.write(content1.toString());
				bw1.flush();
				
				System.out.println("Writing gnuplot dat: " + destination2DAT);
				bw2.write(content2.toString());
				bw2.flush();
				System.out.println("Finished writing gnuplot dat");
			}
			else
				System.err.println("Didn't found zone id " + zone_id + " in " + fileList1[i].getName());
			bw1.close();
			bw2.close();
		}
	}
	
	private static void checkAndRestoreZoneID(String source){
		
		// restore zone id
		if(zone_id != zone_id_tmp){
			zone_id = zone_id_tmp;
			System.out.println("Restoring zone_id (id = " + zone_id + ")...");
		}
		// check for indicator tables containing only one row
		if( (source != null) && (source.endsWith("total_units__total_units.tab") || source.endsWith("total_office_units__total_office_units.tab"))){
			System.out.println("Switching zone id from " + zone_id + " to 1 for table " + source);
			zone_id = 1;
		}
		
	}
	
	/**
	 * returns x-axis values for JFreeChart
	 * @param header
	 * @return
	 * @throws NumberFormatException
	 */
	@SuppressWarnings("all")
	private static double[] getXAxis(String[] header) throws NumberFormatException{
		
		String[] tmp;
		String tmpName;
		
		double xAxis[] = new double[header.length-1];
		
		for(int i = 1; i < header.length; i++){
			
			tmpName = header[i];
			tmp = tmpName.split(":");
			tmpName = tmp[0];
			tmp = tmpName.split("_");
			tmpName = tmp[tmp.length-1];
			
			xAxis[i-1] = Integer.parseInt(tmpName);
		}
		return xAxis;
	}
	
	/**
	 * return y-axis label for JFreeChart
	 * @param header
	 * @return
	 */
	private static String getYAxisLabel(String[] header){
		
		String[] tmp;
		String tmpName;
		
		String columnName = header[1];
		tmp = columnName.split(":");
		columnName = tmp[0];
		tmp = columnName.split("_");
		
		tmpName = "";
		
		for(int i = 0; i < (tmp.length-1); i++)
			tmpName = tmpName + tmp[i] + " ";
		
		columnName = tmpName.trim();
		
		return columnName;
	}
	
	/**
	 * creates an header object. A header object retuns the indices of columns
	 * in ascending order.
	 * 
	 * @param line
	 */
	private static HeaderObject initHeaderObject(String line){
		
		String parts[] = line.split( Constants.TAB );

		int unsortedHeaderArray[] = new int[parts.length];
		int sortedHeaderArray[] = new int[parts.length];
		
		// puts a header like "zone_id:i8	var_2018:f8	var_2019:f8	var_2012:f8"
		// into two unsorted arrays with the followg structure: [-1, 2018, 2019, 2012]
		for(int i = 0; i < parts.length; i++){
			int index = parts[i].indexOf(":");
			parts[i] = parts[i].substring(0, index);
			
			String key[] = parts[i].split("_");
			
			try{
				unsortedHeaderArray[i] = Integer.parseInt( key[key.length-1]);
			}
			catch(NumberFormatException nfe){
				unsortedHeaderArray[i] = -1; // -> zone id
			}
			// only copy values to other array
			sortedHeaderArray[i] = unsortedHeaderArray[i];
		}
		// stores unsorted array as a map of <YEAR, INDEX>
		Map<Integer,Integer> idxFromKey = UtilityCollection.createIdxFromKey( unsortedHeaderArray );
		// sorted array in chonological order. The zone id is stored as -1 and schould be the first value
		UtilityCollection.ArrayQuicksort( sortedHeaderArray );
		
		return new HeaderObject(idxFromKey, sortedHeaderArray);
	}
	
//	/**
//	 * creates a file with values from the given zone id row
//	 * @throws IOException
//	 */
//	private static void getYAxis(BufferedReader br) throws IOException{
//		
//		StringBuffer content = new StringBuffer("");
//		
//		String line;
//		int id;
//		String[] parts;
//		
//		// header
//		line = br.readLine();
//		parts = line.split(Constants.TAB);
//		
//		while ( (line = br.readLine()) != null ){
//
//			parts = line.split("\t");
//			id = Integer.parseInt(parts[0]);
//			
//			if( id == zone_id){
//				
//				System.out.println("Found zone : " + id);
//				content.append("#item	value");
//				
//				for(int i = 1; i < parts.length; i++)
//					content.append( (i-1) + "\t" + parts[i]+"\n");
//				
//				break;
//			}
//		}
//		System.out.println("Writing data ...");
//		bw.write( content.toString() );
//		bw.flush();
//		bw.close();
//	}
	
//	public static void test() throws IOException{
//	JavaPlot p = new JavaPlot();
//	// set plot title
//	p.setTitle("Test");
//	// set terminal (output type)
//	PostscriptTerminal pst = new PostscriptTerminal("/Users/thomas/Documents/SVN_Studies/tnicolai/cupum/Data/gnuplot/sin.ps");
//	System.out.println(pst.getOutputFile());
//	System.out.println(pst.getTextOutput());
//	System.out.println(pst.getType());
//	p.setTerminal(pst);
//	
//	p.set("datafile commentschars", "#%");
//	// set labels
//	p.set("xlable", "years");
//	p.set("ylable", "population");
//	
//	GNUPlotParameters gp = p.getParameters();
//	
//	PlotStyle plotStyle = new PlotStyle();
//	plotStyle.setStyle(Style.LINESPOINTS);
//	plotStyle.setLineWidth(2);
//	
//	// create test data
//	 int[][] dataset = new int[100][2];
//	 for (int j=0;j<100;j++)
//	 { 
//		 dataset[j][0] = j+1;
//		 dataset[j][1] = (int)Math.log(j);
//	 }
//
//	DataSetPlot datasetplot = new DataSetPlot(dataset);
//	datasetplot.setPlotStyle(plotStyle);
//	datasetplot.setTitle("My Test Plot");
//	//datasetplot.setSmooth(Smooth.UNIQUE);
//	datasetplot.set("xlable", "years");
//	datasetplot.set("ylable", "population");
//
//	Set<String> hp = datasetplot.keySet();
//	
//	p.addPlot( datasetplot );
//	
//	
//	p.plot();
//	
////	p.addPlot("sin(x)");
////	p.plot();
//	
////	ImageIO.write(it.getImage(), "png", new File("/Users/thomas/Documents/SVN_Studies/tnicolai/cupum/Data/gnuplot/sin.png"));
//}

}

class HeaderObject{
	
	private Map<Integer,Integer> idxFromKey = null;
	private int sortedHeaderArray[] = null;
	
	public HeaderObject(Map<Integer,Integer> idxFromKey, int sortedHeaderArray[]){
		this.idxFromKey = idxFromKey;
		this.sortedHeaderArray = sortedHeaderArray;
	}

	/**
	 * returns the index for zone_id
	 * @return index
	 */
	public int getZoneId(){
		int index = idxFromKey.get(-1);
		return index;
	}
	
	public int getIndexOf(int i){
		int year = sortedHeaderArray[i];
		int index = idxFromKey.get(year);
		return index;
	}
	
	public double[] getSortedHeaderAsDouble(){
		double tmp[] = new double[sortedHeaderArray.length-1];
		for (int i = 1; i < sortedHeaderArray.length; i++)
			tmp[i-1] = (double) sortedHeaderArray[i];
		return tmp;
	}
	
}

