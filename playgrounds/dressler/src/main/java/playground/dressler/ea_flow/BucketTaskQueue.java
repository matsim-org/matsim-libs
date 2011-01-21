/* *********************************************************************** *
 * project: org.matsim.*
 * BucketTaskQueue.java
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

package playground.dressler.ea_flow;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;

import org.matsim.api.core.v01.Id;

public class BucketTaskQueue implements TaskQueue {
	private int depth = 0;
	private Id origin = null;
	
	final int INITIAL_INDEX_SIZE = 10000;
	final int INITIAL_QUEUE_SIZE = 10000;
	final int TOO_BIG_INDEX = 1000000; // 1 million 
	
	ArrayList<Integer> accessMin;
	ArrayList<Integer> accessTail;
	ArrayList<Integer> growingPointer;
	ArrayList<BFTask> growingData;
	
	private int minIndex;
	private int debugCount = 0;
	
	PriorityQueue<BFTask> _backupQueue;
	private TaskComparatorI taskcomp;
		
	public BucketTaskQueue(TaskComparatorI taskcomp){
		
		accessMin = new ArrayList<Integer>(INITIAL_INDEX_SIZE);
		accessTail = new ArrayList<Integer>(INITIAL_INDEX_SIZE);
		growingPointer = new ArrayList<Integer>(INITIAL_QUEUE_SIZE);
		growingData = new ArrayList<BFTask>(INITIAL_QUEUE_SIZE);
		
		minIndex = TOO_BIG_INDEX;
		
		_backupQueue = new PriorityQueue<BFTask>((1), taskcomp);
		this.taskcomp = taskcomp;
	}	
	
	
	
	private void reallyAdd(BFTask task) {
		/*if (debugCount != dumbCount()) {
			System.out.println("Error! Counts do not agree.");
			debug();
		}
		
		debugCount++;*/
		
		int val = taskcomp.getValue(task);
		
		if (val < 0) {
			System.out.println("negative value in task");
			throw new IllegalArgumentException("Negative value in task!");
		}
		
		if (val < TOO_BIG_INDEX) {
			
			// increase ArrayList as necessary
			if (val >= accessMin.size()) {
				
				accessMin.ensureCapacity(val);
				accessTail.ensureCapacity(val);
				
				for (int i = accessMin.size(); i <= val; i++) {
					accessMin.add(-1); // empty
					accessTail.add(-1); // empty					
				}
			}
			
			// insert the element
			
			int j = growingData.size() ; // the position of the new element
			growingData.add(task);
			growingPointer.add(-1); // no next
								
			int i = accessTail.get(val); // old last element
			
			if (i == -1) { // empty list
				
				accessMin.set(val, j);
				accessTail.set(val, j);
				
				// update the minimum
				if (val < minIndex) {
					minIndex = val;
				}
			} else { // append
				
				growingPointer.set(i, j);
				accessTail.set(val, j);
			}
			
		} else {
			System.out.println("too large index");
			throw new RuntimeException("Too large index from TaskComp!");
			// FIXME
			// add to backup list ...
		}
		
		/*if (debugCount != dumbCount()) {			
			System.out.println("Error! Counts do not agree.");
			System.out.println("tried to add: ");
			System.out.println(taskcomp.getValue(task));
			debug();
		}*/
	}

	@Override
	public boolean addAll(Collection<? extends BFTask> c) {
		Boolean result = false;
		for(BFTask task: c){
			task.depth = this.depth;
			if (task.origin == null) task.origin = this.origin;
			
			reallyAdd(task);
			result = true; // we added something		
		}
		return result;
	}

	@Override
	public boolean add(BFTask task) {
		task.depth = this.depth;
		if (task.origin == null) task.origin = this.origin;
		
		reallyAdd(task);
		
		return true; // we added something
	}
	
	@Override
	public boolean addAll(TaskQueue tasks) {		
		boolean result = false;
		
		for(BFTask task: tasks){
			task.depth = this.depth;
			if (task.origin == null) task.origin = this.origin;
			
			reallyAdd(task);
			result = true; // we added something		
		}
		return result;
	}

	@Override
	public BFTask poll() {
		/*if (debugCount != dumbCount()) {
			System.out.println("Error! Counts do not agree.");
			debug();
		}*/
		
		//debug();
		//debugCount--;
		
		
		int i = -1;
		
		while (minIndex < accessMin.size()) {		
			i = accessMin.get(minIndex);
			if (i == -1) { 
				minIndex++;
			} else { 
				break;
			}
		};
		
		if (minIndex >= accessMin.size()) {			
			//System.out.println(debugCount);
			
			// FIXME
			// should check backup queue
			return null;
		}
		
		BFTask temp = growingData.get(i);
		int j = growingPointer.get(i);
		accessMin.set(minIndex, j);
		if (j == -1) {
			accessTail.set(minIndex, j);
		}
		
		this.depth = temp.depth + 1;
		this.origin = temp.origin;
		
		return temp;
	}

	@Override
	public Iterator<BFTask> iterator() {
		System.out.println("Iterator not implemented");
		throw new RuntimeException("Iterator not implementend!"); 
	}

	public int dumbCount() {
		int c = 0;
		for (int i = 0; i < accessMin.size(); i++) {			
			int j = accessMin.get(i);
			if (j != -1) {				
				do {
					c++;
					j = growingPointer.get(j);
				} while (j != -1);				
			}			
		}
		return c;
	}
	
	public void debug() {
		System.out.println("DebugCount: " + debugCount);
		System.out.println("DumbCount: " + dumbCount());
		System.out.println("minIndex: " + minIndex);
		for (int i = 0; i < accessMin.size(); i++) {
			
			int j = accessMin.get(i);
			if (j != -1) {
				System.out.print(i + " --> ");
				do {
					System.out.print(j + " : " + taskcomp.getValue(growingData.get(j)) + ", ");
					j = growingPointer.get(j);
				} while (j != -1);
				System.out.println();
			}
			
		}
		throw new RuntimeException("Debug ...");
	}

}
