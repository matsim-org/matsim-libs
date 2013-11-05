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
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;
import org.matsim.matrices.Entry;
import org.matsim.matrices.Matrix;

import playground.southafrica.utilities.Header;

public class SAString {
	final private static Logger LOG = Logger.getLogger(SAString.class);
	final private Random random;
	private Matrix distanceMatrix;
	private List<Id> sites;
	private List<Id> demandPoints;
	private List<Id> fixedSites;
	private int numberOfThreads;

	private Map<Id, Double> demandPointWeights;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(SAString.class.toString(), args);

		/* Required arguments. */
		String distanceFilename = args[0];
		String distanceMatrixDescription = args[1];
		String outputFolder = args[2];
		int numberOfThreads = Integer.parseInt(args[3]);

		/*
		 * Optional arguments. If used, BOTH MUST be given, even if it is an
		 * empty string.
		 */
		String weightsFilename = null;
		String fixedSitesFilename = null;
		if (args.length > 4) {
			weightsFilename = args[4];
			fixedSitesFilename = args[5];
		}

		/*
		 * Implementing the Simulated Annealing algorithm using a single string
		 * representation.
		 */
		SAString sas = new SAString(numberOfThreads, false);
		sas.readDistanceMatrix(distanceFilename);
		sas.readDemandWeights(weightsFilename);
		sas.readFixedSites(fixedSitesFilename);
		
		/* Initialise the output list. */
		List<String> outputList = new ArrayList<String>();
		
		int[] sitesInSolution = {5, 10, 15, 20, 25, 30, 35, 40, 45, 50};
		String prefix;
		for(int n : sitesInSolution){
			for(int run = 1; run <= 200; run++){
				LOG.info("====> Number of sites: " + n + "; Run " + run + " <====");
				/* Execute for full distance matrix. */
				prefix = n +"_" + distanceMatrixDescription + "_" + String.format("%03d", run);
				String outputString = sas.executeSA(n, outputFolder, prefix);
				
				outputList.add(outputString);
			}
		}
		
		/* Write out the overall multi-run results. */
		BufferedWriter bw = IOUtils.getBufferedWriter(outputFolder + "multiRunOutput_" + distanceMatrixDescription + ".csv");
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

	
	public SAString(int numberOfThreads, long seed, boolean useDemandPoints) {
		this.distanceMatrix = new Matrix("Distance",
				"Distance matrix from demand point to site.");
		this.sites = new ArrayList<Id>();
		this.demandPoints = new ArrayList<Id>();
		this.fixedSites = new ArrayList<Id>();
		
		this.random = new Random(seed);
		this.numberOfThreads = numberOfThreads;
	}

	
	public SAString(int numberOfThreads, boolean useDemandPoints) {
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

		Map<Id, Integer> siteMap;
		Map<Integer, Id> siteIndexMap;

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

			siteMap = new HashMap<Id, Integer>(numberOfSites);
			siteIndexMap = new HashMap<Integer, Id>();

			for (int i = 1; i < sa.length; i++) {
				Id siteId = new IdImpl(sa[i]);
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
				Id demandPointId = new IdImpl(sa[0]);
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
		this.demandPointWeights = new HashMap<Id, Double>(
				this.demandPoints.size());
		if (filename != null) {
			File f = new File(filename);
			if (f.exists() && f.isFile() && f.canRead()) {
				BufferedReader br = IOUtils.getBufferedReader(filename);
				try {
					String line = null;
					while ((line = br.readLine()) != null) {
						String[] sa = line.split(",");
						Id id = new IdImpl(sa[0]);
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
		for (Id demandPointId : this.demandPoints) {
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
						Id id = new IdImpl(line);
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
	 * Generate a random permutation of `n' integers in what I believe is O(n)
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
		List<Solution> solutionList = new ArrayList<SAString.Solution>();
		List<Solution> incumbentList = new ArrayList<SAString.Solution>();
		
		/* Initialise the algorithm. 
		 * 
		 * TODO Perform parameter analysis/tweaking */
		int iteration = 0;
		int iterationMax = 500;
		double temp = 1000;
		int tempChangeFrequency = 20;
		double tempChangeFraction = 0.75;
		int nonImprovingIterations = 0;
		
		/* Get initial solution. */
		Solution initialSolution = generateInitialSolution(numberOfSites);
		Solution currentSolution = initialSolution;
		Solution incumbent = initialSolution;
		
		solutionList.add(currentSolution.copy());
		incumbentList.add(incumbent.copy());
		
		/* Repeat until termination criteria is met. */
		LOG.info("Executing the simulated annealing algorithm...");
		Counter counter = new Counter("  iteration # ");
		boolean terminate = false;
		while(!terminate){
			counter.incCounter();
			iteration++;
			
			/* Get the best neighborhood solution, for current temperature. */
			Solution newCurrent = getNeighbour(currentSolution, temp);
//			Solution newCurrent = getNearestNeighbour(currentSolution, temp);
			if(newCurrent == null){
				LOG.warn("Cannot make a move...");
				terminate = true;
			} else{
				/* Test incumbent. */
				if(newCurrent.getObjective() < incumbent.getObjective()){
					incumbent = newCurrent;
				} else{
					nonImprovingIterations++;
				}
				
				/* Update the solution progress. */
				incumbentList.add(incumbent.copy());
				currentSolution = newCurrent;
				solutionList.add(currentSolution.copy());

				/* Update temperature, if necessary. */
				if(nonImprovingIterations % tempChangeFrequency == 0){
					temp *= tempChangeFraction;
				}

				/* Update termination criteria. */
				if(iteration >= iterationMax){
					terminate = true;
				}
			}
		}
		counter.printCounter();
		
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
	
	
	public Solution getNearestNeighbour(Solution current, double temperature){
		Solution newCurrent = null;
		
		Map<Tuple<Id, Id>, Double> map = new HashMap<Tuple<Id, Id>, Double>();
		
		/*TODO Fix to increasing again. */
		ValueComparatorDecreasing vc = new ValueComparatorDecreasing(map);
		Map<Tuple<Id, Id>, Double> sortedMap = new TreeMap<Tuple<Id, Id>, Double>(vc); 
		
		/* Pick a random current site. */
		Id selectedSite = current.getRepresentation().get(getRandomPermutation(current.getRepresentation().size())[0]);
		
		/* Find it's closest neighbour. */
		for(Id nextSite : this.sites){
			if(!selectedSite.toString().equalsIgnoreCase(nextSite.toString())){
				/* Consider this possible move. */
				map.put(new Tuple<Id, Id>(selectedSite, nextSite), this.distanceMatrix.getEntry(selectedSite, nextSite).getValue());
			}
		}
		
		sortedMap.putAll(map);
		
		/* Get the first (best) accepted move */
		boolean found = false;
		for(java.util.Map.Entry<Tuple<Id, Id>, Double> entry : sortedMap.entrySet()){
			Tuple<Id, Id> thisMove = entry.getKey();
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
		
		Map<Tuple<Id, Id>, Double> map = new HashMap<Tuple<Id, Id>, Double>(); 
		ValueComparator vc = new ValueComparator(map);
		Map<Tuple<Id, Id>, Double> sortedMap = new TreeMap<Tuple<Id, Id>, Double>(vc); 
		
		/* Check for each current site in the solution. */
//		for(Id currentSite : current.getRepresentation()){
//			for(Id nextSite : this.sites){
//				if(!currentSite.toString().equalsIgnoreCase(nextSite.toString())){
//					/* Consider this possible move. */
//					Solution possibleMove = current.copy();
//					map.put(new Tuple<Id, Id>(currentSite, nextSite), possibleMove.evaluateObjectiveFunctionDifference(currentSite, nextSite));
//				}
//			}
//		}
		
		/* Select a random current site to remove, but only if it is NOT a fixed 
		 * site. This is achieved by only considering sites that appear AFTER the
		 * fixed sites in the representation. */
		List<Id> currentRepresentation = current.getRepresentation();
		/* Generate the random permutation. */
		int[] randomPermutation = getRandomPermutation(currentRepresentation.size()-fixedSites.size());
		/* Adapt random permutation to only start searching after fixed sites. */
		for(int i = 0; i < randomPermutation.length; i++){
			randomPermutation[i] = randomPermutation[i] + fixedSites.size();
		}
		Id selectedSite = currentRepresentation.get(randomPermutation[0]);
		if(fixedSites.contains(selectedSite)){
			throw new RuntimeException("Found a fixed site to remove. Shouldn't happen!!");
		}
		
		/* Consider all possible moves for this site, but again ignore fixed sites. */
		for(Id nextSite : this.sites){
			if(!selectedSite.toString().equalsIgnoreCase(nextSite.toString()) &&
			   !this.fixedSites.contains(nextSite)){
				/* Consider this possible move. */
				Solution possibleMove = current.copy();
				
				/*TODO Parallelise the objective function evaluation. */ 
				map.put(new Tuple<Id, Id>(selectedSite, nextSite), possibleMove.evaluateObjectiveFunctionDifference(selectedSite, nextSite));
			}
		}

		sortedMap.putAll(map);
		
		/* Get the first (best) accepted move */
		boolean found = false;
		for(java.util.Map.Entry<Tuple<Id, Id>, Double> entry : sortedMap.entrySet()){
			Tuple<Id, Id> thisMove = entry.getKey();
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
	
	
	
	private Solution generateInitialSolution(int numberOfSites){
		/* Throw an error if the maximum number of required sites are less than 
		 * the number of fixed sites that MUST be included in the solution. */
		if(numberOfSites < this.fixedSites.size()){
			LOG.error("The number of fixed sites exceeds the required number of sites. Aborting algorithm.");
			throw new IllegalArgumentException();
		}
		
		/* Start by populating the fixed sites. */
		List<Id> chosenSites = new ArrayList<Id>(numberOfSites);
		chosenSites.addAll(fixedSites);
		
		/* Then add randomly sampled sites until the required number is reached. */
		int[] randomPerm = getRandomPermutation(this.sites.size());
		int nextInt = 0;
		while(chosenSites.size() < numberOfSites){
			Id nextId = this.sites.get(randomPerm[nextInt]);
			
			/* Ensure that there are no duplicate sites. */
			if(!chosenSites.contains(nextId)){
				chosenSites.add(nextId);
			}
			nextInt++;
		}
		
		return new Solution(chosenSites);
	}
	
	
	private String getOutputString(){
		String string = "";
		
		
		return string;
	}
	
	
	
	
	
	
	private class Solution {
		public List<Id> representation;
		public Map<Id, Id> allocation;
		public double objective;

		public Solution(List<Id> representation) {
			this(representation, 0.0);
			this.objective = calculateObjective();
		}
		
		public Solution(List<Id> representation, double objective){
			this.representation = representation;
			this.objective = objective;
		}
		
		public List<Id> getRepresentation() {
			return this.representation;
		}
		
		public double getObjective() {
			return this.objective;
		}

		public double calculateObjective() {
			this.allocation = new HashMap<Id, Id>(this.representation.size());
			double obj = 0.0;

			/* Set up multi-threaded objective function evaluator. */
			ExecutorService threadExecutor = Executors.newFixedThreadPool(numberOfThreads);
			List<Future<Tuple<Tuple<Id,Id>, Double>>> listOfJobs = new ArrayList<Future<Tuple<Tuple<Id,Id>, Double>>>();
			
			/* Assign to each demand point its closest site. */
			for(Id demandPointId : demandPoints){
				Callable<Tuple<Tuple<Id,Id>, Double>> job = new EvaluateClosestSiteCallable(demandPointId, this.representation);
				Future<Tuple<Tuple<Id,Id>, Double>> result = threadExecutor.submit(job);
				listOfJobs.add(result);
			}
			
			threadExecutor.shutdown();
			while(!threadExecutor.isTerminated()){
			}
			
			/* Consolidate the output. */
			try {
				for(Future<Tuple<Tuple<Id,Id>, Double>> job : listOfJobs){
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
			List<Id> newRepresentation = new ArrayList<Id>(this.representation.size());
			for(Id id : this.representation){
				newRepresentation.add(new IdImpl(id.toString()));
			}
			
			/* Copy the objective. */
			Solution ss = new Solution(newRepresentation, this.getObjective());

			/* Copy the allocation. */
			Map<Id, Id> newAllocation = new HashMap<Id, Id>();
			for(Id id : this.allocation.keySet()){
				newAllocation.put(new IdImpl(id.toString()), new IdImpl(this.allocation.get(id).toString()));
			}
			ss.allocation = newAllocation;
			
			return ss;
		}
		
		
		public double evaluateObjectiveFunctionDifference(Id oldId, Id newId){
			double difference = 0.0;
			
			/* Only consider a move if the new site is not already included in 
			 * the current solution. Otherwise, just return a zero difference.*/
			if(!this.representation.contains(newId)){
				for(Id id : this.allocation.keySet()){
					Id thisAllocationId = this.allocation.get(id);
					/* Check if the current allocation is affected by the move. */
					if(thisAllocationId.toString().equalsIgnoreCase(oldId.toString())){
						/* Calculate the difference. */
						difference -= distanceMatrix.getEntry(id, oldId).getValue();
						difference += distanceMatrix.getEntry(id, newId).getValue();
					}
				}
			}
			
			return difference;
		}
		
		
		public void makeMove(Id oldId, Id newId){
			/* Remove the old site, and add the new. */
			this.representation.remove(oldId);
			this.representation.add(newId);

			/* Re-calculate the objective function. The allocation will also be 
			 * updated. */
			this.objective = calculateObjective();
		}
		
		
		public String toString(){
			String string = "";
			/* Add all but the last element. */
			for(int i=0; i < this.representation.size()-1; i++){
				string += this.representation.get(i).toString() + ";";
			}
			string += this.representation.get(this.representation.size()-1);
			
			return string;
		}
		
	}
	
	
	class ValueComparator implements Comparator<Tuple<Id, Id>>{
		Map<Tuple<Id, Id>, Double> map;
		
		public ValueComparator(Map<Tuple<Id, Id>, Double> map) {
			this.map = map;
		}

		@Override
		public int compare(Tuple<Id, Id> arg0, Tuple<Id, Id> arg1) {
			if(map.get(arg0) <= map.get(arg1)){
				return -1;
			} else{
				return 1;
			}
		}
	}

	class ValueComparatorDecreasing implements Comparator<Tuple<Id, Id>>{
		Map<Tuple<Id, Id>, Double> map;
		
		public ValueComparatorDecreasing(Map<Tuple<Id, Id>, Double> map) {
			this.map = map;
		}
		
		@Override
		public int compare(Tuple<Id, Id> arg0, Tuple<Id, Id> arg1) {
			if(map.get(arg0) >= map.get(arg1)){
				return -1;
			} else{
				return 1;
			}
		}
	}
	
	
	class EvaluateClosestSiteCallable implements Callable<Tuple<Tuple<Id,Id>, Double>> {
		private Id id;
		private List<Id> currentSites;
		
		public EvaluateClosestSiteCallable(Id id, List<Id> currentSites) {
			this.id = id;
			this.currentSites = currentSites;
		}

		@Override
		public Tuple<Tuple<Id,Id>, Double> call() throws Exception {
			Id closest = null;
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
				Id thisSite = siteIterator.next().getToLocation();
				if(currentSites.contains(thisSite)){
					foundClosest = true;
					closest = thisSite;
				}
			}
			
			Tuple<Id, Id> idPair = new Tuple<Id, Id>(this.id, closest);
			Double distance = distanceMatrix.getEntry(id, closest).getValue();
			return new Tuple<Tuple<Id,Id>, Double>(idPair, distance);
		}
	}

	
}
