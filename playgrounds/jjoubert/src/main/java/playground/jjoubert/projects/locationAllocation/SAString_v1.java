/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,     *
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

package playground.jjoubert.projects.locationAllocation;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.log4j.Logger;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;
import org.matsim.matrices.Entry;
import org.matsim.matrices.Matrix;

import playground.southafrica.utilities.Header;

public class SAString_v1 {
	final private static Logger LOG = Logger.getLogger(SAString_v1.class);
	private static double FRACTION_OF_SOLUTIONS_CHECKED; 
	private static int INITIAL_SOLUTION_STRATEGY;
	private static int NEIGHBOURHOOD_STRATEGY;
	private static int OBJECTIVE_FUNCTION_STRATEGY;
	final private Random random;
	private Matrix distanceMatrix;
	private List<String> sites;
	private List<String> demandPoints;
	private List<String> fixedSites;
	private int numberOfThreads;
	Map<String,Matrix> matrixCache = new TreeMap<String, Matrix>();

	private Map<String, Double> demandPointWeights;
	private final static int[] SAMPLE_SIZE = {
		2, 3, 4, 5, 7, 8, 9, 10, 11, 12, 16, 18, 19, 21, 
		26, 28, 30, 31, 33, 34, 36, 37, 38, 39, 40, 41, 42, 
		43, 44, 48, 50, 52, 53, 57, 58, 59, 63, 64, 65, 66, 
		67, 68, 73, 75, 77, 78, 79, 80, 82, 83, 84, 87, 88, 
		89, 92, 93, 94, 95, 96, 98, 99, 101, 103, 104, 112, 
		113, 114, 120, 123, 124, 125, 126, 135, 136, 138, 143, 
		144, 145, 146, 149, 150, 151, 154, 155, 157, 160, 162, 
		163, 165, 167, 172, 174, 175, 176, 177, 180, 181, 183, 
		184, 190, 193, 195, 200, 201, 202, 203, 205, 206, 207, 
		210, 211, 213, 214, 215, 216, 217, 219, 222, 224, 226, 
		227, 229, 232, 235, 236, 239, 242, 243, 245, 246, 248, 
		249, 251, 254, 258, 259, 272, 273, 274, 284, 287, 288, 
		289, 290, 292, 296, 299, 300, 301, 306, 307, 309, 314, 
		318, 321, 322, 324, 333, 336, 341, 342, 346, 359, 369, 
		373, 376, 378, 385, 388, 392, 400, 411, 418, 421, 423, 
		424, 425, 430, 433, 447, 457, 470, 472, 481, 483, 485, 
		488, 490, 491, 501, 509, 512, 513, 523, 533, 534, 536, 
		539, 547, 556, 600};
//	private final static int[] SAMPLE_SIZE = {10};

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(SAString_v1.class.toString(), args);

		/* Required arguments. */
		String distanceFilename = args[0];
		String distanceMatrixDescription = args[1];
		String outputFolder = args[2];
		int numberOfThreads = Integer.parseInt(args[3]);
//		int numberOfSites = Integer.parseInt(args[4]);
//		int numberOfRuns = Integer.parseInt(args[5]);
		
		FRACTION_OF_SOLUTIONS_CHECKED = Double.parseDouble(args[6]);
		INITIAL_SOLUTION_STRATEGY = Integer.parseInt(args[7]);
		NEIGHBOURHOOD_STRATEGY = Integer.parseInt(args[8]);
		OBJECTIVE_FUNCTION_STRATEGY = Integer.parseInt(args[9]);

		/*
		 * Optional arguments. If used, BOTH MUST be given, even if it is an
		 * empty string.
		 */
		String weightsFilename = null;
		String fixedSitesFilename = null;
		if (args.length > 10) {
			weightsFilename = args[10];
			fixedSitesFilename = args[11];
		}

		/*
		 * Implementing the Simulated Annealing algorithm using a single string
		 * representation.
		 */
		SAString_v1 sas = new SAString_v1(numberOfThreads, false);
		sas.readDistanceMatrix(distanceFilename);
		sas.readDemandWeights(weightsFilename);
		sas.readFixedSites(fixedSitesFilename);
		
		/* Initialise the output list. */
		List<String> outputList = new ArrayList<String>();
		
		String prefix;
		int numberOfRuns = 1;
		for(int numberOfSites : SAMPLE_SIZE){
			int run = numberOfRuns; /* This stays fixed in this version. */
			LOG.info("====> Number of sites: " + numberOfSites + "; Run " + run + " <====");

			/* Execute for full distance matrix. */
			prefix = String.format("%02d_%s_%03d", numberOfSites, distanceMatrixDescription, run);
			String outputString = sas.executeSA(numberOfSites, outputFolder, prefix);

			outputList.add(outputString);
		}
		
		/* Write out the overall multi-run results. */
		BufferedWriter bw = IOUtils.getBufferedWriter(String.format("%smultiRunOutput_%s.csv", outputFolder, distanceMatrixDescription));
		try{
			/* Write header. */
			bw.write("sites,matrix,run,objective,incumbent");
			bw.newLine();
			
			for(String s : outputList){
				bw.write(s);
				bw.newLine();
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot write to BufferedWriter.");
		} finally{
			try {
				bw.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot close BufferedWriter.");
			}
		}
		
		Header.printFooter();
	}

	
	public SAString_v1(int numberOfThreads, long seed, boolean useDemandPoints) {
		this.distanceMatrix = new Matrix("Distance",
				"Distance matrix from demand point to site.");
		this.sites = new ArrayList<>();
		this.demandPoints = new ArrayList<>();
		this.fixedSites = new ArrayList<>();
		
		this.random = new Random(seed);
		this.numberOfThreads = numberOfThreads;
	}

	
	public SAString_v1(int numberOfThreads, boolean useDemandPoints) {
		this(numberOfThreads, new SecureRandom().nextInt(), useDemandPoints);
	}
	

	/**
	 * Reads in a comma-separated value (CSV) file containing the distances
	 * between demand points and sites. The layout is assumed to be as follows:
	 * <ul>
	 * <li>the first row contains the header;
	 * <li>each column heading (starting with second) is the site description;
	 * <li>each subsequent row starts with a demand point description;
	 * <li>the matrix entries are the distance from demand point to the site.
	 * </ul>
	 * 
	 * @param filename
	 */
	private void readDistanceMatrix(String filename) {
		LOG.info("Reading distance matrix from " + filename);

		Map<String, Integer> siteMap;
		Map<Integer, String> siteIndexMap;

		BufferedReader br = IOUtils.getBufferedReader(filename);
		try {
			/* Process the sites from the header line. */
			String line = br.readLine();
			String[] sa;
			if(line != null){
				sa = line.split(",");
			} else{
				throw new NullPointerException("Null line read. Not expected.");
			}
			
			int numberOfSites = sa.length - 1;

			siteMap = new HashMap<>(numberOfSites);
			siteIndexMap = new HashMap<>();

			for (int i = 1; i < sa.length; i++) {
				String siteId = sa[i];
				this.sites.add(siteId);
				siteMap.put(siteId, new Integer(i));
				siteIndexMap.put(new Integer(i), siteId);
			}
			LOG.info("... read " + numberOfSites + " sites.");

			/* Now read the rest of the matrix. */
			while ((line = br.readLine()) != null) {
				sa = line.split(",");
				/* Check the line length. */
				if (sa.length != numberOfSites + 1) {
					LOG.error("Wrong line length read!!");
				}
				/* Add the demand point to the list demand of points. */
				String demandPointId = sa[0];
				//FIXME Remove once sorted out.
				this.demandPoints.add(demandPointId);

				for (int i = 1; i < sa.length; i++) {
					double distance = Double.POSITIVE_INFINITY;
					try {
						distance = Double.parseDouble(sa[i]);
					} catch (Exception e) {
						LOG.error("Cannot convert " + sa[i]
								+ " to distance of type double.");
						e.printStackTrace();
					}
					distanceMatrix.createEntry(demandPointId,
							siteIndexMap.get(i), distance);
				}
			}
			LOG.info("... read " + this.demandPoints.size() + " demand points (rows).");
			LOG.info("... read " + (sa.length-1)+ " sites (columns).");
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Could not read from " + filename);
		} finally {
			try {
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot close " + filename);
			}
		}
	}
	
	
	private void readDemandWeights(String filename) {
		/* Only read a filename if it exists, and is not null. */
		this.demandPointWeights = new HashMap<>(this.demandPoints.size());
		if (filename != null) {
			File f = new File(filename);
			if (f.exists() && f.isFile() && f.canRead()) {
				BufferedReader br = IOUtils.getBufferedReader(filename);
				try {
					String line = null;
					while ((line = br.readLine()) != null) {
						String[] sa = line.split(",");
						String id = sa[0];
						if (!this.demandPoints.contains(id)) {
							LOG.warn("Demand point "
									+ sa[0]
									+ " was indicated as a demand point, but doesn't occur in the distance matrix. It will be ignored.");
						} else {
							this.demandPointWeights.put(id,
									Double.parseDouble(sa[1]));
						}
					}
				} catch (IOException e) {
					e.printStackTrace();
					throw new RuntimeException("Cannot read from " + filename);
				} finally {
					try {
						br.close();
					} catch (IOException e) {
						e.printStackTrace();
						throw new RuntimeException("Cannot close " + filename);
					}
				}
			}
			LOG.info("... read " + this.demandPointWeights.size()
					+ " demand point weights.");
		}

		/*
		 * Check for each demand point that there is a weight, or add a zero
		 * weight if it is not included.
		 */
		for (String demandPointId : this.demandPoints) {
			if (!this.demandPointWeights.containsKey(demandPointId)) {
				this.demandPointWeights.put(demandPointId, 1.0);
			}
		}
		LOG.info("... weights corrected (" + this.demandPointWeights.size()
				+ " with weights)");
	}

	/**
	 * Reads a file with one entry (site description) per line. These sites are
	 * to be considered <i>fixed</i> and must be included in the final solution.
	 * 
	 * @param filename
	 */
	private void readFixedSites(String filename) {
		/* Only read a filename if it exists, and is not null. */
		if (filename != null) {
			File f = new File(filename);
			if (f.exists() && f.isFile() && f.canRead()) {
				BufferedReader br = IOUtils.getBufferedReader(filename);
				try {
					String line = null;
					while ((line = br.readLine()) != null) {
						String id = line;
						if (!this.sites.contains(id)) {
							LOG.warn("Site "
									+ line
									+ " was indicated as a fixed site, but doesn't occur in the distance matrix. It will be ignored.");
						} else {
							this.fixedSites.add(id);
						}
					}
				} catch (IOException e) {
					e.printStackTrace();
					throw new RuntimeException("Cannot read from " + filename);
				} finally {
					try {
						br.close();
					} catch (IOException e) {
						e.printStackTrace();
						throw new RuntimeException("Cannot close " + filename);
					}
				}
			}
		}
		LOG.info("... found " + this.fixedSites.size() + " fixed sites.");
	}

	
	/**
	 * Generate a random permutation of 'n' integers in what I believe is O(n)
	 * complexity.
	 * @param n
	 * @return
	 * @see <a href="http://en.wikipedia.org/wiki/Fisher–Yates_shuffle">Fisher-Yates shuffle</a>
	 */
	private int[] getRandomPermutation(int n){
		/* Initiate the array. */
		int[] array = new int[n];
		for(int i = 0; i < n; i++){
			array[i] = i;
		}
		
		/* Shuffle the array. */
		for(int i = array.length-1; i >= 1; i--){
			int j = this.random.nextInt(i);
			int dummy = array[j];
			array[j] = array[i];
			array[i] = dummy;
		}
		
		return array;
	}
	
	
	public String executeSA(int numberOfSites, String outputFolder, String runPrefix){
		List<Solution> solutionList = new ArrayList<SAString_v1.Solution>();
		List<Solution> incumbentList = new ArrayList<SAString_v1.Solution>();
		
		/* Initialise the algorithm. 
		 * 
		 * TODO Perform parameter analysis/tweaking */
		int iterationMax = 100;
		double temp = 10;
		int tempChangeFrequency = 20;
		double tempChangeFraction = 0.25;
		int nonImprovingIterations = 0;
		int returnToIncumbent = 0;
		int returnToIncumbentThreshold = 10;
		
		/* Get initial solution. */
		Solution initialSolution;
		switch (INITIAL_SOLUTION_STRATEGY) {
		case 1:
			/* Generate a random initial solution. A permutation of 'n' integer
			 * numbers are generated, where 'n' is the total number of sites.
			 * The first 'm' integer values of the permutation is used as
			 * indices, and the associated sites are added to the initial 
			 * solution. */
			initialSolution = generateRandomInitialSolution(numberOfSites);
			break;
		case 2:
			/* Generates a greedy initial solution. A random seed site is selected,
			 * and then, iteratively, each other site is evaluated for insertion.
			 * The site with the biggest improvement (saving in distance) is 
			 * selected next, until the entire initial solution is filled. */
			initialSolution = generateGreedyInitialSolution(numberOfSites);
			break;
		case 3:
			/* Generates a demand-driven initial solution. The demand points are 
			 * randomly selected, and for each selected demand point, its 
			 * closest site is added. But if the closest site is already in the
			 * initial solution, then the demand point is skipped, and we move 
			 * to the next demand point. The process repeats until the initial
			 * solution is complete. */
			initialSolution = generateDemandDrivenInitialSolution(numberOfSites);
			break;
		default:
			throw new IllegalArgumentException("Cannot interpret " + INITIAL_SOLUTION_STRATEGY + " as a valid initial solution strategy.");
		}
		
		Solution currentSolution = initialSolution;
		Solution incumbent = initialSolution;
		
		solutionList.add(currentSolution.copy());
		incumbentList.add(incumbent.copy());
		LOG.info( String.format("    >=> First incumbent: %.2f", incumbent.objective) );
		
		/* Repeat until termination criteria is met. */
		LOG.info("Executing the simulated annealing algorithm...");
		LOG.info(" -> Initial temperature: " + temp);
		Counter counter = new Counter("  iteration # ");
		boolean terminate = false;
		while(!terminate){
			
			/* Get the best neighborhood solution, for current temperature. */
			Solution newCurrent = getNeighbour(currentSolution, temp);
//			Solution newCurrent = getNearestNeighbour(currentSolution, temp);
			
			if(newCurrent == null){
				LOG.warn("Cannot make a move... returning to the incumbent.");
				
				/* Return to the incumbent solution. */
				newCurrent = incumbent;
				returnToIncumbent++;
				if(returnToIncumbent == returnToIncumbentThreshold){
					terminate = true;
					LOG.warn("Tried returning to the incumbent " + returnToIncumbentThreshold + " times. Terminating.");
				}
				
				/*FIXME Consider terminating at this point. */
				//				terminate = true;
			}
			
			/* Test incumbent. */
			if(newCurrent.getObjective() < incumbent.getObjective()){
				incumbent = newCurrent;
				LOG.info( String.format("    >=> New incumbent: %.2f", incumbent.objective) );
			} else{
				nonImprovingIterations++;
			}

			/* Update the solution progress. */
			incumbentList.add(incumbent.copy());
			currentSolution = newCurrent;
			solutionList.add(currentSolution.copy());

			/* Update temperature, if necessary. */
			if(nonImprovingIterations > 0 && nonImprovingIterations % tempChangeFrequency == 0){
				temp *= tempChangeFraction;
			}

			/* Update termination criteria. */
			counter.incCounter();
			if(counter.getCounter() >= iterationMax){
				terminate = true;
			}
		}
		counter.printCounter();
		LOG.info(" -> Final temperature: " + temp);
		
		/* Report the incumbent progress output. */
		String filename = outputFolder + runPrefix + "_progress.csv";
		BufferedWriter bw = IOUtils.getBufferedWriter(filename);
		try{
			bw.write("Iter,Current,Incumbent");
			bw.newLine();
			for(int i = 0; i < solutionList.size(); i++){
				bw.write(String.valueOf(i));
				bw.write(",");
				bw.write(String.valueOf(solutionList.get(i).objective));
				bw.write(",");
				bw.write(String.valueOf(incumbentList.get(i).objective));
				bw.newLine();
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
		
		/* Update the solution string. */
		return String.format("%s,%.4f,%s", runPrefix.replaceAll("_", ","), incumbent.objective, incumbent.toString());
	}
	
	
	@Deprecated
	public Solution getNearestNeighbour(Solution current, double temperature){
		Solution newCurrent = null;
		
		Map<Tuple<String, String>, Double> map = new HashMap<Tuple<String, String>, Double>();
		
		/*TODO Fix to increasing again. */
		ValueComparatorDecreasing vc = new ValueComparatorDecreasing(map);
		Map<Tuple<String, String>, Double> sortedMap = new TreeMap<Tuple<String, String>, Double>(vc); 
		
		/* Pick a random current site. */
		String selectedSite = current.getRepresentation().get(getRandomPermutation(current.getRepresentation().size())[0]);
		
		/* Find it's closest neighbour. */
		for(String nextSite : this.sites){
			if(!selectedSite.toString().equalsIgnoreCase(nextSite.toString())){
				/* Consider this possible move. */
				map.put(new Tuple<String, String>(selectedSite, nextSite), this.distanceMatrix.getEntry(selectedSite, nextSite).getValue());
			}
		}
		
		sortedMap.putAll(map);
		
		/* Get the first (best) accepted move */
		boolean found = false;
		for(java.util.Map.Entry<Tuple<String, String>, Double> entry : sortedMap.entrySet()){
			Tuple<String, String> thisMove = entry.getKey();
			double thisSaving = entry.getValue();
			while(!found){
				if(thisSaving < 0){
					found = true;
					newCurrent = current.copy();
					newCurrent.makeMove(thisMove.getFirst(), thisMove.getSecond());
					break;
				} else{
					/* Check if the deteriorating move will be accepted. */
					double random = Math.random();
					double threshold = Math.exp((- thisSaving) / temperature);
					if(random <= threshold){
						found = true;
						newCurrent = current.copy();
						newCurrent.makeMove(thisMove.getFirst(), thisMove.getSecond());
						break;
					}
				}
			}	
			if(found){
				break;
			}
		}
		
		return newCurrent;
	}
	
	
	public Solution getNeighbour(Solution current, double temperature){
		Solution newCurrent = null;

		/*----------------------------------------------------------------------
		 * The following block of code selects a fraction of the current 
		 * solution's sites, and calculate the neighbourhood for all those 
		 * selected sites.
		 * 
		 * A minimum of one site will be considered, irrespective of how small
		 * the fraction is.
		 * 
		 * TODO - Consider the implication on diversification when using 
		 * different fraction values. 
		 *--------------------------------------------------------------------*/
		Map<Tuple<String, String>, Double> map = new HashMap<Tuple<String, String>, Double>(); 
		
		/* Select, randomly, the sites to consider for a move. */
		int numberOfSitesToConsiderForNeighborhood = Math.max(1, (int) Math.floor(FRACTION_OF_SOLUTIONS_CHECKED*(current.getRepresentation().size())) );
		List<String> sitesToConsiderForMoves = new ArrayList<String>(numberOfSitesToConsiderForNeighborhood);

		List<String> currentRepresentation = current.getRepresentation();
		int[] randomPermutation = getRandomPermutation(currentRepresentation.size());
		while(sitesToConsiderForMoves.size() < numberOfSitesToConsiderForNeighborhood){
			String siteId = currentRepresentation.get( randomPermutation[sitesToConsiderForMoves.size()] );

			/* Only add the site if it is NOT a fixed site. */
			if(!this.fixedSites.contains(siteId)){
				sitesToConsiderForMoves.add( siteId );
			}
		}

		/* Consider the various moves. */
		for(String selectedSite : sitesToConsiderForMoves){
			
			switch (NEIGHBOURHOOD_STRATEGY) {
			case 1:
				/* TODO Consider the twenty sites that are farthest away from the
				 * sites in the current solution. */
				List<String> farthestSites = new ArrayList<>(20);
				for(String site : this.sites){
					if(!current.representation.contains(site)){
						/* The site is not in the current solution. But to check 
						 * how far it is from the OTHER SITES, we need to read 
						 * in the inter-site distance matrix as well... damn. 24*/
					}
				}
				break;
				
			case 2:
				/* Consider all possible moves for this site, but again ignore fixed sites. */
				for(String nextSite : this.sites){
					if(!selectedSite.toString().equalsIgnoreCase(nextSite.toString()) && 	/* Don't replace a site with itself. */ 
							!this.fixedSites.contains(nextSite) && 							/* Don't replace a fixed site. */ 
							!currentRepresentation.contains(nextSite)){						/* Don't replace with a site that is already in the current solution. */
						/* Consider this possible move. */
						Solution possibleMove = current.copy();
						
						switch (OBJECTIVE_FUNCTION_STRATEGY) {
						case 1:
							/* Evaluate the exact objective function difference. 
							 * If it is an improvement, the difference should be
							 * negative. */
							possibleMove.makeMove(selectedSite, nextSite);
							double actualDifference = possibleMove.objective - current.objective;						
							map.put(new Tuple<String, String>(selectedSite, nextSite), actualDifference );
							break;
							
						case 2:
							/* Estimate the objective function difference. */
							double estimatedDifference = possibleMove.evaluateObjectiveFunctionDifference(selectedSite);
							map.put(new Tuple<String, String>(selectedSite, nextSite), estimatedDifference );
							
						default:
							throw new IllegalArgumentException("Cannot interpret " + OBJECTIVE_FUNCTION_STRATEGY + " as a valid objective function strategy.");
						}
					}
				}
				break;
				
			default:
				throw new IllegalArgumentException("Cannot interpret " + NEIGHBOURHOOD_STRATEGY + " as a valid neighbourhood strategy.");
			}
		}

		/* Rank the neighbourhood moves from best to worst. */
		ValueComparatorIncreasing vc = new ValueComparatorIncreasing(map);
		Map<Tuple<String, String>, Double> sortedMap = new TreeMap<Tuple<String, String>, Double>(vc); 
		sortedMap.putAll(map);
		
		/* Get the first (best) accepted move */
		boolean found = false;
		Iterator<java.util.Map.Entry<Tuple<String, String>, Double>> iterator = sortedMap.entrySet().iterator();
		
		while(!found && iterator.hasNext()){
			java.util.Map.Entry<Tuple<String, String>, Double> entry = iterator.next();
			
			Tuple<String, String> thisMove = entry.getKey();
			double thisSaving = entry.getValue();
			
			/* Consider what is considered an "acceptable" move, based on the
			 * objective function evaluation strategy
			 */
			double acceptanceLevel = 0.0;
			switch (OBJECTIVE_FUNCTION_STRATEGY) {
			case 1:
				/* The objective function difference is calculated exactly. The
				 * default acceptance level of zero holds. Only truly improving
				 * moves are accepted. */
				break;
				
			case 2:
				/* Since the objective function difference is a conservative 
				 * estimate, we also accept all moves that are deteriorating the 
				 * current solution by no more than 5%, assuming the benefit 
				 * will be made up when making the move and realising additional 
				 * savings from other allocations. 
				 */
				acceptanceLevel = 0.05*current.objective;
				break;
				
			default:
				throw new IllegalArgumentException("Cannot interpret " + OBJECTIVE_FUNCTION_STRATEGY + " as a valid objective function strategy.");
			}
			
			if(thisSaving < acceptanceLevel){
//				LOG.debug("     ==> Improving move: ");
				found = true;
				newCurrent = current.copy();
				newCurrent.makeMove(thisMove.getFirst(), thisMove.getSecond());
				break;
			} else{
				/* Check if the deteriorating move will be accepted. */
				double random = Math.random();
				double threshold = Math.exp((- thisSaving) / temperature);
//				LOG.debug("     ==> Probability of selecting deteriorating move: " + threshold);
				if(random <= threshold){
					found = true;
					newCurrent = current.copy();
					newCurrent.makeMove(thisMove.getFirst(), thisMove.getSecond());
				}
			}
		}

		if(newCurrent == null){
			LOG.warn("     ==> No new solution found...");
		}
		return newCurrent;
	}
	
	
	
	private Solution generateRandomInitialSolution(int numberOfSites){
		/* Throw an error if the maximum number of required sites are less than 
		 * the number of fixed sites that MUST be included in the solution. */
		if(numberOfSites < this.fixedSites.size()){
			LOG.error("The number of compulsory fixed sites exceeds the required number of sites. Aborting algorithm.");
			throw new IllegalArgumentException();
		}
		
		/* Start by populating the fixed sites. */
		List<String> chosenSites = new ArrayList<>(numberOfSites);
		chosenSites.addAll(fixedSites);
		
		/* Then add randomly sampled sites until the required number is reached. */
		int[] randomPerm = getRandomPermutation(this.sites.size());
		int nextInt = 0;
		while(chosenSites.size() < numberOfSites){
			String nextId = this.sites.get(randomPerm[nextInt]);
			
			/* Ensure that there are no duplicate sites. */
			if(!chosenSites.contains(nextId)){
				chosenSites.add(nextId);
			}
			nextInt++;
		}
		return new Solution(chosenSites);
	}
	
	
	private Solution generateGreedyInitialSolution(int numberOfSites){
		LOG.info("Generating a greedy initial solution");
		Counter counter = new Counter("  sites # ");
		List<String> initial = new ArrayList<>(numberOfSites);
		
		/* Randomly pick the seed. */
		int[] permutation = getRandomPermutation(this.sites.size());
		initial.add(this.sites.get(permutation[0]));
		counter.incCounter();
		
		double partialObjective = evaluatePartialSolution(initial); 

		do {
			double best = Double.NEGATIVE_INFINITY;
			String bestId =  null;
			List<String> evaluateList = new ArrayList<>( initial.size()+1 );
			evaluateList.addAll(initial);
			for(String siteId : this.sites){
				if(!initial.contains(siteId)){
					evaluateList.add(siteId);
					
					/* Evaluate the possible insertion. */
					double thisInsertion = evaluatePartialSolution(evaluateList);
					if(partialObjective - thisInsertion > best){
						bestId = siteId;
						best = partialObjective - thisInsertion;
					}
					evaluateList.remove(siteId);
				}
			}
			initial.add(bestId);
			partialObjective -= best;
			counter.incCounter();
			
		} while (initial.size() < numberOfSites);
		counter.printCounter();
		
		LOG.info("Greedy initial solution generated.");
		return new Solution(initial);
	}
	
	
	private double evaluatePartialSolution(List<String> sites){
		double sum = 0.0;
		for(String demandPoint : this.demandPoints){
			double closestDistance = Double.POSITIVE_INFINITY;
			for(String siteId : sites){
				double thisDistance = this.distanceMatrix.getEntry(demandPoint, siteId).getValue();
				if(thisDistance < closestDistance){
					closestDistance = thisDistance;
				}
			}
			sum += closestDistance;
		}
		return sum;
	}
	
	
	private Solution generateDemandDrivenInitialSolution(int numberOfSites){
		LOG.info("Generating demand-driven initial solution.");
		List<String> initial = new ArrayList<String>(numberOfSites);
		
		int[] randomDemandSites = getRandomPermutation(this.demandPoints.size());
		
		int demandIndex = 0;
		do {
			String demandId = this.demandPoints.get( randomDemandSites[demandIndex] );
			
			List<Entry> thisDemandPointSites =  distanceMatrix.getFromLocEntries(demandId);
			Comparator<Entry> entryComparator = new Comparator<Entry>() {
				@Override
				public int compare(Entry e1, Entry e2) {
					return new Double(e1.getValue()).compareTo(new Double(e2.getValue()));
				}
			};
			Collections.sort(thisDemandPointSites, entryComparator);
			String closestSite = thisDemandPointSites.get(0).getToLocation();

			if(initial.contains(closestSite)){
				demandIndex++;
			} else{
				initial.add(closestSite);
				demandIndex++;
			}
		} while (initial.size() < numberOfSites);
		
		Solution s = new Solution(initial);
		LOG.info("Demand-driven initial solution generated.");
		return s;
	}
	
	
	private class Solution {
		public List<String> representation;
		public Map<String, String> allocation;
		private Matrix solutionMatrix;
		public double objective;

		public Solution(List<String> representation) {
			this(representation, 0.0, null);
			
			this.objective = calculateObjective();
		}
		
		public Solution(List<String> representation, double objective, Matrix matrix){
			this.representation = representation;
			this.objective = objective;
			if(matrix == null){
//				/* Build the current distance matrix. */
//				this.solutionMatrix = new Matrix("current", "Current solution's distance matrix");
//				for(String s : representation){
//					List<Entry> entries = distanceMatrix.getToLocEntries(s);
//					for(Entry e : entries){
//						solutionMatrix.createEntry(e.getFromLocation(), e.getToLocation(), e.getValue());
//					}
//				}
			} else{
				this.solutionMatrix = matrix;
			}
		}
		
		public double calculateObjective() {
			this.allocation = new HashMap<String, String>(this.representation.size());
			double obj = 0.0;
		
			/* Set up multi-threaded objective function evaluator. */
			ExecutorService threadExecutor = Executors.newFixedThreadPool(numberOfThreads);
			List<Future<Tuple<Tuple<String, String>, Double>>> listOfJobs = new ArrayList<Future<Tuple<Tuple<String, String>, Double>>>();
			
			
//			String s = this.toString();
			Matrix matrix;
//			if(matrixCache.containsKey(s)){
//				matrix = matrixCache.get(s);
//			} else{
				/* Calculate the local matrix. */
				matrix = new Matrix("tmp", "tmp");
				for(String rep : this.representation){
					List<Entry> entries = distanceMatrix.getToLocEntries(rep);
					for(Entry e : entries){
						matrix.createEntry(e.getFromLocation(), e.getToLocation(), e.getValue());
					}
				}
//				matrixCache.put(s, matrix);
//			}
			

			/* Assign to each demand point its closest site. */
			for(String demandPointId : demandPoints){
				Callable<Tuple<Tuple<String, String>, Double>> job = new EvaluateClosestSiteCallable(demandPointId, this.representation, matrix);
				Future<Tuple<Tuple<String, String>, Double>> result = threadExecutor.submit(job);
				listOfJobs.add(result);
			}
			
			threadExecutor.shutdown();
			while(!threadExecutor.isTerminated()){
			}
			
			/* Consolidate the output. */
			try {
				for(Future<Tuple<Tuple<String, String>, Double>> job : listOfJobs){
					this.allocation.put(job.get().getFirst().getFirst(), job.get().getFirst().getSecond());
					obj += job.get().getSecond().doubleValue();
				}
			} catch (InterruptedException e) {
				throw new RuntimeException("Could not retrieve multithreaded result.");
			} catch (ExecutionException e) {
				throw new RuntimeException("Could not retrieve multithreaded result.");
			}
			return obj;
		}

		/**
		 * Make an exact copy of the solution. The objective is not re-evaluated
		 * but rather we use, for computational reasons, the current objective.
		 * @return exact copy of the solution, but (hopefully) with no references
		 * 		   to the existing solution's elements.
		 */
		public Solution copy(){
			/* Copy the representation. */
			List<String> newRepresentation = new ArrayList<String>(this.representation.size());
			for(String id : this.representation){
				newRepresentation.add(id);
			}
			
			/* Copy the objective. */
			Solution ss = new Solution(newRepresentation, this.getObjective(), this.solutionMatrix);
		
			/* Copy the allocation. */
			Map<String, String> newAllocation = new HashMap<String, String>();
			for(String id : this.allocation.keySet()){
				newAllocation.put(id, this.allocation.get(id));
			}
			ss.allocation = newAllocation;
			
			return ss;
		}

		public double evaluateObjectiveFunctionDifference(String oldId){
			double difference = 0.0;
		
			/* Check each demand point's allocated site. */
			for (String id : this.allocation.keySet()){
				String thisAllocationId = this.allocation.get(id);
				
				/* Check if the current allocation is affected by the move. */
				if(thisAllocationId.toString().equalsIgnoreCase(oldId.toString())){
					/* Calculate the difference. */
					difference -= distanceMatrix.getEntry(id, oldId).getValue();
					
					/* Now get the affected site's new closest distance, BUT don't
					 * consider the site will be removed. */
					String newAllocationId = null;					
					List<Entry> thisDemandPointSites =  distanceMatrix.getFromLocEntries(id);
					Comparator<Entry> entryComparator = new Comparator<Entry>() {
						@Override
						public int compare(Entry e1, Entry e2) {
							return new Double(e1.getValue()).compareTo(new Double(e2.getValue()));
						}
					};
					Collections.sort(thisDemandPointSites, entryComparator);
					boolean foundClosest = false;
					Iterator<Entry> siteIterator = thisDemandPointSites.iterator();
					while(!foundClosest && siteIterator.hasNext()){
						String thisSite = siteIterator.next().getToLocation();
						if(this.representation.contains(thisSite) && !thisSite.toString().equalsIgnoreCase(oldId.toString()) ){
							foundClosest = true;
							newAllocationId = thisSite;
						}
					}
					
					difference += distanceMatrix.getEntry(id, newAllocationId).getValue();
				}
			}
			
			return difference;
		}

		public double getObjective() {
			return this.objective;
		}

		public List<String> getRepresentation() {
			return this.representation;
		}
		
		public void makeMove(String oldId, String newId){
			/* Remove the old site, and add the new. */
			this.representation.remove(oldId);
			this.representation.add(newId);
			
			/*TODO Check if this works. */
//			updateSolutionMatrix(oldId, newId);

			/* Re-calculate the objective function. The allocation will also be 
			 * updated. */
			this.objective = calculateObjective();
		}
		
		
		@Override
		public String toString(){
			String string = "";
			/* Add all but the last element. */
			for(int i=0; i < this.representation.size()-1; i++){
				string += this.representation.get(i).toString() + ";";
			}
			string += this.representation.get(this.representation.size()-1);
			
			return string;
		}
		
		
		private void updateSolutionMatrix(String outId, String inId){
			/* Remove all the entries from the site that is taken out. */
			List<Entry> remove = solutionMatrix.getToLocEntries(outId);
			List<String> removeS = new ArrayList<String>();
			for(Entry e : remove){
				removeS.add(e.getFromLocation() + "," + e.getToLocation() + "," + String.valueOf(e.getValue()));
			}
			remove = null;
			for(String s : removeS){
				String[] sa = s.split(",");
				solutionMatrix.removeEntry(sa[0], sa[1]);
			}
			
			/* Add all the distance entries of the incoming site. */
			List<Entry> add = distanceMatrix.getToLocEntries(inId);
			for(Entry e : add){
				solutionMatrix.createEntry(e.getFromLocation(), e.getToLocation(), e.getValue());
			}
		}
	}
	
	
	class ValueComparatorIncreasing implements Comparator<Tuple<String, String>>{
		Map<Tuple<String, String>, Double> map;
		
		public ValueComparatorIncreasing(Map<Tuple<String, String>, Double> map) {
			this.map = map;
		}

		@Override
		public int compare(Tuple<String, String> arg0, Tuple<String, String> arg1) {
			if(map.get(arg0) <= map.get(arg1)){
				return -1;
			} else{
				return 1;
			}
		}
	}

	class ValueComparatorDecreasing implements Comparator<Tuple<String, String>>{
		Map<Tuple<String, String>, Double> map;
		
		public ValueComparatorDecreasing(Map<Tuple<String, String>, Double> map) {
			this.map = map;
		}
		
		@Override
		public int compare(Tuple<String, String> arg0, Tuple<String, String> arg1) {
			if(map.get(arg0) >= map.get(arg1)){
				return -1;
			} else{
				return 1;
			}
		}
	}
	
	
	class EvaluateClosestSiteCallable implements Callable<Tuple<Tuple<String,String>, Double>> {
		private String id;
		private List<String> currentSites;
		private final Matrix matrix;
		
		public EvaluateClosestSiteCallable(String id, List<String> currentSites, final Matrix matrix) {
			this.id = id;
			this.currentSites = currentSites;
			this.matrix = matrix;
		}

		@Override
		public Tuple<Tuple<String, String>, Double> call() throws Exception {
			String closest = getClosestSite();
			
			Tuple<String, String> idPair = new Tuple<String, String>(this.id, closest);
			
			/*TODO Old way */
//			Double distance = distanceMatrix.getEntry(id, closest).getValue();
			/*TODO New way way */
			Double distance = matrix.getEntry(id, closest).getValue();
			return new Tuple<Tuple<String, String>, Double>(idPair, distance);
		}
		
		public String getClosestSite(){
			String closest = null;
			/*TODO Old way */
//			List<Entry> thisDemandPointSites =  distanceMatrix.getFromLocEntries(id);
			/*TODO New way */
			List<Entry> thisDemandPointSites =  matrix.getFromLocEntries(id);
			Comparator<Entry> entryComparator = new Comparator<Entry>() {
				@Override
				public int compare(Entry e1, Entry e2) {
					return new Double(e1.getValue()).compareTo(new Double(e2.getValue()));
				}
			};
			Collections.sort(thisDemandPointSites, entryComparator);
			boolean foundClosest = false;
			Iterator<Entry> siteIterator = thisDemandPointSites.iterator();
			while(!foundClosest && siteIterator.hasNext()){
				String thisSite = siteIterator.next().getToLocation();
				if(currentSites.contains(thisSite)){
					foundClosest = true;
					closest = thisSite;
				}
			}
			return closest;
		}
		
		public String getClosestSite2(){
			String closest = null;
			double min = Double.MAX_VALUE;

			List<Entry> thisDemandPointSites =  distanceMatrix.getFromLocEntries(id);
			
			Iterator<Entry> iterator = thisDemandPointSites.iterator();
			while(iterator.hasNext()){
				Entry e = iterator.next();
				String s = e.getToLocation();
				if(currentSites.contains(s)){
					if(e.getValue() < min){
						min = e.getValue();
						closest = s;
					}
				}
			}
			
			return closest;
		}
		
	}

	
}
