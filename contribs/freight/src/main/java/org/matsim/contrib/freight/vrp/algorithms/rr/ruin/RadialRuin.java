package org.matsim.contrib.freight.vrp.algorithms.rr.ruin;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.matsim.contrib.freight.vrp.algorithms.rr.RRSolution;
import org.matsim.contrib.freight.vrp.algorithms.rr.tourAgents.TourAgent;
import org.matsim.contrib.freight.vrp.basics.Job;
import org.matsim.contrib.freight.vrp.basics.RandomNumberGeneration;
import org.matsim.contrib.freight.vrp.basics.VehicleRoutingProblem;

public class RadialRuin implements RuinStrategy{
	
	static class ReferencedJob {
		private Job job;
		private double distance;
		
		public ReferencedJob(Job job, double distance) {
			super();
			this.job = job;
			this.distance = distance;
		}

		public Job getJob(){
			return job;
		}

		public double getDistance() {
			return distance;
		}
	}
	
	private Logger logger = Logger.getLogger(RadialRuin.class);
	
	private VehicleRoutingProblem vrp;
	
	private double fractionOfAllNodes2beRuined;
	
	private Map<String,TreeSet<ReferencedJob>> distanceNodeTree = new HashMap<String,TreeSet<ReferencedJob>>();
	
	private List<Job> unassignedJobs = new ArrayList<Job>();
	
	private Random random = RandomNumberGeneration.getRandom();

	private JobDistance jobDistance;
	
	public void setRandom(Random random) {
		this.random = random;
	}

	public RadialRuin(VehicleRoutingProblem vrp, JobDistance jobDistance) {
		super();
		this.vrp = vrp;
		this.jobDistance = jobDistance;
		logger.info("intialise radial ruin");
		calculateDistancesFromJob2Job();
		logger.info("done");
	}

	public void setFractionOfAllNodes(double fractionOfAllNodes) {
		this.fractionOfAllNodes2beRuined = fractionOfAllNodes;
	}

	
	private void calculateDistancesFromJob2Job() {
		for(Job i : vrp.getJobs().values()){
			TreeSet<ReferencedJob> treeSet = new TreeSet<ReferencedJob>(new Comparator<ReferencedJob>() {
				@Override
				public int compare(ReferencedJob o1, ReferencedJob o2) {
					if(o1.getDistance() <= o2.getDistance()){
						return 1;
					}
					else{
						return -1;
					}
				}
			});
			distanceNodeTree.put(i.getId(), treeSet);
			for(Job j : vrp.getJobs().values()){
				double distance = jobDistance.calculateDistance(i,j);
				logger.debug("distMatrix: " + i.getId() + " " + j.getId() + " " + distance);
				ReferencedJob refNode = new ReferencedJob(j, distance);
				treeSet.add(refNode);
			}
		}
	}


	@Override
	public void run(RRSolution initialSolution) {
		clear();
		int nOfJobs2BeRemoved = getNuOfJobs2BeRemoved();
		if(nOfJobs2BeRemoved == 0){
			return;
		}
		Job randomJob = pickRandomJob();
		logger.debug("randomJob: " + randomJob.getId());
		TreeSet<ReferencedJob> tree = distanceNodeTree.get(randomJob.getId());
		Iterator<ReferencedJob> descendingIterator = tree.descendingIterator();
		int counter = 0;
		while(descendingIterator.hasNext() && counter<nOfJobs2BeRemoved){
			ReferencedJob refJob = descendingIterator.next();
			Job job = refJob.getJob();
			logger.debug("removedJob: " + job.getId());
			for(TourAgent agent : initialSolution.getTourAgents()){
				if(agent.hasJob(job)){
					agent.removeJob(job);
					unassignedJobs.add(job);
				}
			}
			counter++;
		}
	}

	private Job pickRandomJob() {
		int totNuOfJobs = vrp.getJobs().values().size();
		int randomIndex = random.nextInt(totNuOfJobs);
		Job job = new ArrayList<Job>(vrp.getJobs().values()).get(randomIndex);
		return job;
	}

	private void clear() {
		unassignedJobs.clear();
	}

	private int getNuOfJobs2BeRemoved(){
		return (int)Math.round(vrp.getJobs().values().size()*fractionOfAllNodes2beRuined);
	}
	
	@Override
	public List<Job> getUnassignedJobs() {
		return unassignedJobs;
	}
}
