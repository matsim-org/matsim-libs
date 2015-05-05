/**
 * 
 */
package playground.southafrica.projects.digicore.scoring;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;

import playground.southafrica.utilities.Header;
import playground.southafrica.utilities.grid.GeneralGrid;
import playground.southafrica.utilities.grid.GeneralGrid.GridType;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

/**
 * In this class only the x and y acceleration is used to score an individual.
 * The records are aggregated using hexagons.
 * 
 * @author jwjoubert
 */
public class Only2dDigiscorer implements DigiScorer {
	private final static Logger LOG = Logger.getLogger(Only2dDigiscorer.class);
	private int maxLines = Integer.MAX_VALUE;
	private List<Double> riskThresholds;
	private List<Point> sortedPoints = null;
	private Map<Point,Integer> mapRating = null;
	
	private GeneralGrid grid;
	private Map<Point, Double> countMap;
	
	public Only2dDigiscorer(double scale, String filename, final List<Double> riskThresholds) {
		this.grid = new GeneralGrid(scale, GridType.HEX);
		/* Set up the grid */
		setupGrid(filename);
		this.riskThresholds = riskThresholds;
	}
	
	private void setupGrid(String filename){
		/* Find the extent of the data. */
		LOG.info("Finding the extent of the data.");
		double xMin = Double.POSITIVE_INFINITY;
		double xMax = Double.NEGATIVE_INFINITY;
		double yMin = Double.POSITIVE_INFINITY;
		double yMax = Double.NEGATIVE_INFINITY;
		
		BufferedReader br = IOUtils.getBufferedReader(filename);
		Counter counter = new Counter("   lines # ");
		try{
			String line = null;
			while((line = br.readLine()) != null && counter.getCounter() <= this.maxLines){
				String[] sa = line.split(",");
				double x = Double.parseDouble(sa[5]);
				double y = Double.parseDouble(sa[6]);
				xMin = Math.min(xMin, x);
				xMax = Math.max(xMax, x);
				yMin = Math.min(yMin, y);
				yMax = Math.max(yMax, y);
				counter.incCounter();
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot read from " + filename);
		}finally{
			try {
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot close " + filename);
			}
		}
		counter.printCounter();
		
		/* Generating a geometry capturing data extent. */
		GeometryFactory gf = new GeometryFactory();
		Coordinate c1 = new Coordinate(xMin, yMin);
		Coordinate c2 = new Coordinate(xMax, yMin);
		Coordinate c3 = new Coordinate(xMax, yMax);
		Coordinate c4 = new Coordinate(xMin, yMax);
		Coordinate[] ca = {c1, c2, c3, c4, c1};
		Polygon envelope = gf.createPolygon(ca);
		
		/* Build grid space */
		this.grid.generateGrid(envelope);
		
		/* Initialise the grid map with zeros. */
		LOG.info("Initialising the grid's count map.");
		this.countMap = new HashMap<Point, Double>(this.grid.getGrid().size());
		for(Point p: this.grid.getGrid().values()){
			this.countMap.put(p, 0.0);
		}
	}


	/* (non-Javadoc)
	 * @see playground.southafrica.projects.digicore.scoring.DigiScorer#buildScoringModel(java.lang.String)
	 */
	@Override
	public void buildScoringModel(String filename) {
		LOG.info("Populating the dodecahedra with point observations...");
		if(this.maxLines < Integer.MAX_VALUE){
			LOG.warn("A limited number of " + this.maxLines + " is processed (if there are so many)");
		}
		double pointsConsidered = 0.0;

		Counter counter = new Counter("   lines # ");
		BufferedReader br = IOUtils.getBufferedReader(filename);
		try{
			String line = null;
			while( (line = br.readLine()) != null && counter.getCounter() < maxLines){
				String[] sa = line.split(",");
				double x = Double.parseDouble(sa[5]);
				double y = Double.parseDouble(sa[6]);
				
				Point closestCell = this.grid.getGrid().get(x, y);
				double oldValue = countMap.get(closestCell);
				countMap.put(closestCell, oldValue + 1.0);
				pointsConsidered += 1.0; /* We assume the weight of each point is 1.0 */

				counter.incCounter();
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot read from " + filename);
		} finally{
			try {
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot close " + filename);
			}
		}
		counter.printCounter();
		LOG.info("All " + counter.getCounter() + " points processed.");
		
		/* Rank the cells from highest to lowest count. */
		LOG.info("Ranking hexagonal cells based on point-counts only.");
		Comparator<Point> comparator = new Comparator<Point>() {
			@Override
			public int compare(Point o1, Point o2) {
				return -countMap.get(o1).compareTo(countMap.get(o2));
			}
		};
		
		sortedPoints = new ArrayList<Point>(countMap.keySet());
		Collections.sort(sortedPoints, comparator);
		/* Report the top 20 cell values. */
		LOG.info("   20 polyhedra with largest number of observations:");
		for(int i = 0; i < 20; i++){
			LOG.info(String.format("      %d: %.1f observations", i+1, countMap.get(sortedPoints.get(i))));
		}
		
		LOG.info("Done building scoring model.");
		
		/* Rank the grid space. */
		LOG.info("Ranking the grid space...");
		double totalAdded = 0.0;
		double cumulative = 0.0;
		mapRating = new HashMap<Point, Integer>(sortedPoints.size());
		
		List<Point> centroidsToRemove = new ArrayList<Point>();

		double maxValue = 0.0;
		for(int i = 0; i < sortedPoints.size(); i++){
			Point p = sortedPoints.get(i);
			double obs = countMap.get(p);
			if(obs > 0){
				maxValue = Math.max(maxValue, (double)obs);
				totalAdded += (double)obs;
				cumulative = totalAdded / pointsConsidered;
				
				/* Get the rating class for this value. */
				Integer ratingZone = null;
				int zoneIndex = 0;
				while(ratingZone == null && zoneIndex < this.riskThresholds.size()){
					if(cumulative <= this.riskThresholds.get(zoneIndex)){
						ratingZone = new Integer(zoneIndex);
					} else{
						zoneIndex++;
					}
				}
				mapRating.put(p, ratingZone);
			} else{
				centroidsToRemove.add(p);
			}
		}
		
		/* Remove zero-count hexagons. */
		for(Point p : centroidsToRemove){
			countMap.remove(p);
		}
		
		LOG.info("Done ranking hexagonal cells.");
		LOG.info("A total of " + countMap.size() + " hexagons contains points (max value: " + maxValue + ")");
	}

	/**
	 * An accelerometer record is only classified based on the accelerometer 
	 * data, and then also only the x and y-components.
	 */
	public RISK_GROUP getRiskGroup(String record) {
		if(mapRating == null){
			LOG.error("You cannot get a risk group unless the risk evaluation has been done.");
			LOG.error("First call the method 'buildScoringModel(...)");
			throw new RuntimeException();
		}
		
		String[] sa = record.split(",");

		/* Return accelerometer risk class. */
		double x = Double.parseDouble(sa[5]);
		double y = Double.parseDouble(sa[6]);

		/* Get the closest cell to this point. */
		Point cell = grid.getGrid().get(x, y);
		int risk = mapRating.get(cell);
		switch (risk) {
		case 0:
			return RISK_GROUP.NONE;
		case 1:
			return RISK_GROUP.LOW;
		case 2:
			return RISK_GROUP.MEDIUM;
		case 3:
			return RISK_GROUP.HIGH;
		default:
			throw new RuntimeException("Don't know what risk class " + risk + " is!");
		}
	}

	/**
	 * Consider each record, and process them per individual so that the total
	 * number of occurrences in each risk group can be calculated. The output 
	 * file with name <code>riskClassCountsPerPerson.csv</code> will be created 
	 * in the output folder.
	 * 
	 * @param outputFolder
	 */
	@Override
	public void rateIndividuals(String filename, String outputFolder) {
		Map<String, Integer[]> personMap = new TreeMap<String, Integer[]>();

		/* Process all records. */
		LOG.info("Processing records for person-specific scoring.");
		Counter counter = new Counter("   lines # ");
		BufferedReader br = IOUtils.getBufferedReader(filename);
		try{
			String line = null;
			while( (line = br.readLine()) != null && counter.getCounter() < maxLines){
				String[] sa = line.split(",");

				String id = sa[1];
				if(!personMap.containsKey(id)){
					Integer[] ia = {0, 0, 0, 0};
					personMap.put(id, ia);
				}
				Integer[] thisArray = personMap.get(id);

				RISK_GROUP risk = getRiskGroup(line);
				int index;
				switch (risk) {
				case NONE:
					index = 0;
					break;
				case LOW:
					index = 1;
					break;
				case MEDIUM:
					index = 2;
					break;
				case HIGH:
					index = 3;
					break;
				default:
					throw new RuntimeException("Don't know where to get risk values for " + risk.toString());
				}
				int oldCount = thisArray[index];
				thisArray[index] = oldCount+1;

				counter.incCounter();
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot read from " + filename);
		} finally{
			try {
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot close " + filename);
			}
		}
		counter.printCounter();
		LOG.info("Done processing records. Unique persons identified: " + personMap.size());

		/* Write the output to file. */
		String outputFilename = outputFolder + (outputFolder.endsWith("/") ? "" : "/") + "riskClassCountsPerPerson.csv";
		LOG.info("Writing the per-person risk classes counts to " + outputFilename); 

		/* Write the cell values and their risk classes. */
		BufferedWriter bw = IOUtils.getBufferedWriter(outputFilename);
		try{
			/* Header. */
			bw.write("id,none,low,medium,high");
			bw.newLine();

			for(String id : personMap.keySet()){
				Integer[] thisArray = personMap.get(id);
				bw.write(String.format("%s,%d,%d,%d,%d\n", id, thisArray[0], thisArray[1], thisArray[2], thisArray[3]));
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot write to " + outputFilename);
		} finally{
			try {
				bw.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot close " + outputFilename);
			}
		}
		LOG.info("Done writing the per-person risk classes counts.");	}

	
	public void setMaximumLines(int maxLines){
		this.maxLines = maxLines;
	}
	
	private void writeCellCountsAndRiskClasses(String outputFolder){
		if(countMap.size() == 0 || mapRating == null){
			throw new RuntimeException("Insufficient data to write. Either no grids, or no ranking.");
		}
		String filename = outputFolder + (outputFolder.endsWith("/") ? "" : "/") + "cellValuesAndRiskClasses.csv";
		LOG.info("Writing the cell values and risk classes to " + filename); 
		
		/* Report the risk thresholds for which this output holds. */
		LOG.info("  \\_ Accelerometer risk thresholds:");
		for(int i = 0; i < this.riskThresholds.size(); i++){
			LOG.info(String.format("      \\_ Risk %d: %.4f", i, this.riskThresholds.get(i)));
		}
		
		/* Write the cell values and their risk classes. */
		BufferedWriter bw = IOUtils.getBufferedWriter(filename);
		try{
			/* Header. */
			bw.write("x,y,count,class");
			bw.newLine();
			
			for(Point p : this.countMap.keySet()){
				bw.write(String.format("%.4f, %.4f,%.1f,%d\n", p.getX(), p.getY(), this.countMap.get(p), this.mapRating.get(p)));
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot write to " + filename);
		} finally{
			try {
				bw.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot close " + filename);
			}
		}
		LOG.info("Done writing cell values and risk classes.");
	}
	
	
	public static void main(String[] args) {
		Header.printHeader(AccelOnlyDigiScorer.class.toString(), args);
		
		/* Parse the input arguments. */
		String filename = args[0];
		String outputFolder = args[1];
		Double scale = Double.parseDouble(args[2]);
		int maxLines = Integer.parseInt(args[3]);
		
		List<Double> riskThresholds = new ArrayList<Double>();
		int argsIndex = 4;
		while(args.length > argsIndex){
			riskThresholds.add(Double.parseDouble(args[argsIndex++]));
		}

		/* Check that the output folder is empty. */
		File folder = new File(outputFolder);
		if(folder.exists() && folder.isDirectory() && folder.listFiles().length > 0){
			LOG.error("The output folder " + outputFolder + " is not empty.");
			throw new RuntimeException("Output directory will not be overwritten!!");
		}
		folder.mkdirs();

		Only2dDigiscorer o2d = new Only2dDigiscorer(scale, filename, riskThresholds);
		o2d.setMaximumLines(maxLines);
		o2d.buildScoringModel(filename);
		o2d.writeCellCountsAndRiskClasses(outputFolder);
		o2d.rateIndividuals(filename, outputFolder);

		Header.printFooter();
	}

}
