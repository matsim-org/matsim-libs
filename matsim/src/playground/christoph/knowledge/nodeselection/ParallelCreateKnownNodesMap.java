/* *********************************************************************** *
 * project: org.matsim.*
 * ParallelCreateKnownNodesMap.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package playground.christoph.knowledge.nodeselection;


import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.gbl.Gbl;
import org.matsim.interfaces.basic.v01.Id;
import org.matsim.interfaces.core.v01.Person;
import org.matsim.network.Node;
import org.matsim.population.Population;

import playground.christoph.router.util.DeadEndRemover;


/**
 * A class for running {@link CreateKnownNodesMap} in parallel using threads.
 *
 * @author Christoph Dobler
 */
public class ParallelCreateKnownNodesMap {

	private final static Logger log = Logger.getLogger(ParallelCreateKnownNodesMap.class);
	
	protected static boolean removeDeadEnds = true;
	
	/**
	 * Creates a Map of known Nodes of each person of the given <code>population</code> using up to 
	 * <code>numberOfThreads</code> threads to speed things up. The given <code>nodeSelectors</code> will be
	 * cloned for each thread because the SelectNodes implementations are not thread-safe.
	 * 
	 * @param population
	 * @param nodeSelectors
	 * @param numberOfThreads
	 */
	public static void run(final Population population, final ArrayList<SelectNodes> nodeSelectors, final int numberOfThreads)
	{
		int numOfThreads = Math.max(numberOfThreads, 1); // it should be at least 1 here; we allow 0 in other places for "no threads"
		
		log.info("Using " + numOfThreads + " parallel threads to calculate the maps of known nodes.");
		
		SelectNodesThread[] selectorThreads = new SelectNodesThread[numOfThreads];
		
		Thread[] threads = new Thread[numOfThreads];
		
		// create and fill Array of NodeSelectors used in the threads
		SelectNodes[][] nodeSelectorArray = new SelectNodes[nodeSelectors.size()][numOfThreads];
		
		for(int i = 0; i < nodeSelectors.size(); i++)
		{
			SelectNodes selector = nodeSelectors.get(i);
		
			// fill first row with already defined selectors
			nodeSelectorArray[i][0] = selector;
			
			// fill the other fields in the current row with clones
			for(int j = 1; j < numOfThreads; j++)
			{
				// insert clone
				nodeSelectorArray[i][j] = selector.clone();
			}
			
		}
		
		// setup threads
		for (int i = 0; i < numOfThreads; i++) 
		{

			SelectNodesThread selectionThread = new SelectNodesThread(i, nodeSelectorArray, nodeSelectors);
			selectorThreads[i] = selectionThread;
			
			Thread thread = new Thread(selectionThread, "Thread#" + i);
			threads[i] = thread;
		}
		
		// distribute workload between threads, as long as threads are not yet started, so we don't need synchronized data structures
		int i = 0;
		for (Person person : population) 
		{
			selectorThreads[i % numOfThreads].handlePerson(person);
			i++;
		}
		
		// start the threads
		for (Thread thread : threads) 
		{
			thread.start();
		}
		
		// wait for the threads to finish
		try {
			for (Thread thread : threads) 
			{
				thread.join();
			}
		} 
		catch (InterruptedException e)
		{
			Gbl.errorMsg(e);
		}
	}
	
	public static void setRemoveDeadEnds(boolean value)
	{
		removeDeadEnds = value;
	}
	
	public static boolean getRemoveDeadEnds()
	{
		return removeDeadEnds;
	}
	
	
	/**
	 * The thread class that really handles the persons.
	 */
	private static class SelectNodesThread implements Runnable 
	{
		public final int threadId;
		private final ArrayList<SelectNodes> nodeSelectors;
		private final SelectNodes[][] nodeSelectorArray;
		private final List<Person> persons = new LinkedList<Person>();

		public SelectNodesThread(final int i, final SelectNodes nodeSelectorArray[][], final ArrayList<SelectNodes> nodeSelectors)
		{
			this.threadId = i;
			this.nodeSelectorArray = nodeSelectorArray;
			this.nodeSelectors = nodeSelectors;
		}

		public void handlePerson(final Person person)
		{
			this.persons.add(person);
		}

		public void run()
		{
			int numRuns = 0;
			
			for (Person person : this.persons)
			{	
				/* 
				 * If person has no Knowledge create at least the knowledge structure to
				 * avoid error during the simulation.
				 * No known nodes is handled as if the person would know the entire network.
				 * This should be faster than using a Map that contains all Nodes of the network.
				 */
				if(person.getKnowledge() == null) 
				{
					person.createKnowledge("activityroom");
					
					Map<Id, Node> nodesMap = new TreeMap<Id, Node>();

					// add the new created Nodes to the knowledge of the person
					Map<String,Object> customKnowledgeAttributes = person.getKnowledge().getCustomAttributes();
					customKnowledgeAttributes.put("Nodes", nodesMap);
				}
				
				// all NodeSelectors of the Person
				ArrayList<SelectNodes> personNodeSelectors = (ArrayList<SelectNodes>)person.getCustomAttributes().get("NodeSelectors");

				// for all NodeSelectors of the Person
				for(int i = 0; i < personNodeSelectors.size(); i++)
				{					
					// get the index of the NodeSelector in the nodeSelectors Array
					int index = nodeSelectors.indexOf(personNodeSelectors.get(i));
					
					// if there is a valid index returned -> get Nodes
					if (index >= 0)
					{			
						CreateKnownNodesMap.collectSelectedNodes(person, this.nodeSelectorArray[index][threadId]);
					}
					else
					{
						log.error("NodeSelector not found!");
					}
					
				}	// for all NodeSelectors
				
				// if Flag is set, remove Dead Ends from the Person's Activity Room
				if(removeDeadEnds) DeadEndRemover.removeDeadEnds(person);
				
				numRuns++;
				if (numRuns % 500 == 0) 
				{
					log.info("created Acivityrooms for " + numRuns + " persons in thread " + threadId);
				}
			
			}
		
			log.info("Thread " + threadId + " done.");
			
		}	// run
		
	}	// SelectNodesThread
	
}