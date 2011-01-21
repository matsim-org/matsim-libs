/* *********************************************************************** *
 * project: org.matsim.*
 * PriorityTaskQueue.java
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

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.PriorityQueue;

import org.matsim.api.core.v01.Id;

public class PriorityTaskQueue implements TaskQueue {
	private int depth = 0;
	private Id origin = null;
	
	private PriorityQueue<BFTask> _list;
		
	public PriorityTaskQueue(Comparator<BFTask> taskcomp){			
		_list = new PriorityQueue<BFTask>((1), taskcomp);
	}	
	
	@Override
	public boolean addAll(Collection<? extends BFTask> c) {
		Boolean result = false;
		for(BFTask task: c){
			task.depth = this.depth;
			if (task.origin == null) task.origin = this.origin;
			result = _list.add(task) || result; // never want a shortcut!		
		}
		return result;
	}

	@Override
	public boolean add(BFTask task) {
		task.depth = this.depth;
		if (task.origin == null) task.origin = this.origin;
		return _list.add(task);
	}

	@Override
	public boolean addAll(TaskQueue tasks) {		
		boolean result = false;
		
		for(BFTask task: tasks){
			task.depth = this.depth;
			if (task.origin == null) task.origin = this.origin;
			result = _list.add(task) || result; // never want a shortcut!
		}
		return result;
	}

	@Override
	public BFTask poll() {
		BFTask temp = _list.poll();
		if (temp != null) {
			this.depth = temp.depth + 1;
			this.origin = temp.origin;
		}
		return temp;
	}

	@Override
	public Iterator<BFTask> iterator() {
		return _list.iterator();
	}



}
