package playground.dziemke.accessibility;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.contrib.accessibility.CSVWriter;
import org.matsim.contrib.accessibility.Labels;
import org.matsim.contrib.accessibility.Modes4Accessibility;
import org.matsim.contrib.accessibility.gis.SpatialGrid;

public class CalculateCompositeAccessibility {
	
	private static final Logger log = Logger.getLogger(CalculateCompositeAccessibility.class);
	

	public static void main(String[] args) {
		String directoryRoot = "../../accessibility-sa/data/01/";
		String[] activityTypes = {"s", "l", "t", "e"};
		boolean includeDensityLayer = true;
		
		// create maps
		Map<String,Map<Modes4Accessibility,SpatialGrid>> accessibilityGrids = 
				new HashMap<String, Map<Modes4Accessibility,SpatialGrid>>() ;
		
		for (String type : activityTypes) {
			Map<Modes4Accessibility,SpatialGrid> typeAccessibilityGrids = new HashMap<Modes4Accessibility,SpatialGrid>();
			accessibilityGrids.put(type, typeAccessibilityGrids);
		}
		

		// check structure of input files
		String[] header = null;
		
		for (String type : activityTypes) {
			String inputFile = directoryRoot + type + "/accessibilities.csv";
			FileReader fileReader;
			BufferedReader bufferedReader;

			try {
				fileReader = new FileReader(inputFile);
				bufferedReader = new BufferedReader(fileReader);

				String line = bufferedReader.readLine();
				header = line.split(",");

				if (!header[0].equals(Labels.X_COORDINATE)) { throw new RuntimeException("Column is not the expected one!"); }
				if (!header[1].equals(Labels.Y_COORDINATE)) { throw new RuntimeException("Column is not the expected one!"); }
				if (!header[2].equals(Labels.ACCESSIBILITY_BY_FREESPEED)) { throw new RuntimeException("Column is not the expected one!"); }
				if (!header[3].equals(Labels.ACCESSIBILITY_BY_CAR)) { throw new RuntimeException("Column is not the expected one!"); }
				if (!header[4].equals(Labels.ACCESSIBILITY_BY_BIKE)) { throw new RuntimeException("Column is not the expected one!"); }
				if (!header[5].equals(Labels.ACCESSIBILITY_BY_WALK)) { throw new RuntimeException("Column is not the expected one!"); }
				if (!header[6].equals(Labels.ACCESSIBILITY_BY_PT)) { throw new RuntimeException("Column is not the expected one!"); }
				if (includeDensityLayer == true) {
					if (!header[7].equals(Labels.POPULATION_DENSITIY)) { throw new RuntimeException("Column is not the expected one!"); }
					if (!header[8].equals(Labels.POPULATION_DENSITIY)) { throw new RuntimeException("Column is not the expected one!"); }
				}
			} catch (FileNotFoundException ex) {
				ex.printStackTrace();
			} catch (IOException ex) {
				ex.printStackTrace();
			} 
		}
		
		
		// now look up coordinates in input files
		// does not change if they are the same in all fiels
		Double minX = (double) Integer.MAX_VALUE;
		Double maxX = (double) Integer.MIN_VALUE;
		Double minY = (double) Integer.MAX_VALUE;
		Double maxY = (double) Integer.MIN_VALUE;

		for (String type : activityTypes) {
			String inputFile = directoryRoot + type + "/accessibilities.csv";
			FileReader fileReader;
			BufferedReader bufferedReader;

			try {
				fileReader = new FileReader(inputFile);
				bufferedReader = new BufferedReader(fileReader);

				String line = null;

				bufferedReader.readLine(); // skip the header

				while ((line = bufferedReader.readLine()) != null) {
					String[] entry = line.split(",");

					// line must not be empty
					if (line != null && !line.equals("")) {
						double x = Double.parseDouble(entry[0]);
						double y = Double.parseDouble(entry[1]);
	
						if (x < minX) { minX = x; }
						if (x > maxX) { maxX = x; }
						if (y < minY) { minY = y; }
						if (y > maxY) { maxY = y; }
					}
				}
			} catch (FileNotFoundException ex) {
				ex.printStackTrace();
			} catch (IOException ex) {
				ex.printStackTrace();
			} 
		}
		System.out.println("minX = " + minX + " -- maxX = " + maxX + " -- minY = " + minY + " -- maxY = " + maxY);
	
		
		double cellSize = 1000.;
		
		
		// create spatial grids
		for (String type : activityTypes) {
			for ( Modes4Accessibility mode : Modes4Accessibility.values() ) {
				accessibilityGrids.get(type).put( mode, new SpatialGrid(minX, minY, maxX, maxY, cellSize, Double.NaN) ) ;
			}
		}
		
		SpatialGrid densityGrid = null;
		if (includeDensityLayer == true) {
			densityGrid = new SpatialGrid(minX, minY, maxX, maxY, cellSize, Double.NaN);
		}
		
		// read in information
		for (String type : activityTypes) {
			
			String inputFile = directoryRoot + type + "/accessibilities.csv";
			FileReader fileReader;
			BufferedReader bufferedReader;

			try {
				fileReader = new FileReader(inputFile);
				bufferedReader = new BufferedReader(fileReader);

				String line = null;

				bufferedReader.readLine(); // skip the header
				
					

				while ((line = bufferedReader.readLine()) != null) {
					String[] entry = line.split(",");
					
					// line must not be empty
					if (line != null && !line.equals("")) {
						
						
						double x = Double.parseDouble(entry[0]);
						double y = Double.parseDouble(entry[1]);
		
						int i = 2;
						
						for (Modes4Accessibility mode : Modes4Accessibility.values()) {
							accessibilityGrids.get(type).get(mode).setValue( Double.parseDouble(entry[i]), x, y ) ;
							i++;
						}
						
						densityGrid.setValue(Double.parseDouble(entry[i]), x, y);
						
					}
				}
			} catch (FileNotFoundException ex) {
				ex.printStackTrace();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
		
		
		// write new output file
		final CSVWriter writer = new CSVWriter(directoryRoot + "composite/" + CSVWriter.FILE_NAME ) ;
		
		// write header
		writer.writeField(Labels.X_COORDINATE);
		writer.writeField(Labels.Y_COORDINATE);
		writer.writeField(Labels.ACCESSIBILITY_BY_FREESPEED);
		writer.writeField(Labels.ACCESSIBILITY_BY_CAR);
		writer.writeField(Labels.ACCESSIBILITY_BY_BIKE);
		writer.writeField(Labels.ACCESSIBILITY_BY_WALK);
		writer.writeField(Labels.ACCESSIBILITY_BY_PT);
		if (includeDensityLayer) {
			// TODO dont really know why this is always twice, but keep it for the moment
			writer.writeField(Labels.POPULATION_DENSITIY);
			writer.writeField(Labels.POPULATION_DENSITIY);
		}
		writer.writeNewLine();
		
		// write data
		double resolution = accessibilityGrids.get(activityTypes[0]).get(Modes4Accessibility.freeSpeed).getResolution();
		
		for(double y = minY; y <= maxY ; y += resolution) {
			for(double x = minX; x <= maxX; x += resolution) {
				
				writer.writeField(x);
				writer.writeField(y);

				for (Modes4Accessibility mode : Modes4Accessibility.values()) {
					double value = 0.;
					for (String type : activityTypes) {
						value = value + accessibilityGrids.get(type).get(mode).getValue(x, y);
					}
					value = value / activityTypes.length;
					
					writer.writeField(value);
				}
				
				if (includeDensityLayer) {
					double densityValue = densityGrid.getValue(x, y);
					// TODO dont really know why this is always twice, but keep it for the moment
					writer.writeField(densityValue);
					writer.writeField(densityValue);
				}
				
				writer.writeNewLine(); 
			}
		}
		writer.close() ;

		log.info("Writing plotting data for other analysis done!");
	}
}