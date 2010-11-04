/* *********************************************************************** *
 * project: org.matsim.*
 * Decomposer.java
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

import java.util.LinkedList;

import org.matsim.api.core.v01.network.*;

import playground.dressler.Interval.SourceIntervals;
import playground.dressler.control.FlowCalculationSettings;



public class Decomposer {
	
	private Flow _flow;
	private FlowCalculationSettings _settings;
	private TimeExpandedPath _currentHead;
	private LinkedList<TimeExpandedPath> _paths;

	Decomposer(Flow flow, FlowCalculationSettings settings) {
		this._flow = flow;
		this._settings = settings;
	}
	
	public LinkedList<TimeExpandedPath> decompose() {
		_paths = new LinkedList<TimeExpandedPath>();
		
		for (Node source : _flow.getSources()) {		
			startPath(source);
		}	
		return _paths;
	}
	
	private int startPath(Node source) {
		//System.out.println("Decompose from source " + source.getId());
		int sum = 0;
		SourceIntervals si = _flow.getSourceOutflow(source);
		
		int maxtime; 
		if (si.getLast().getFlow() == 0) {
		  maxtime = si.getLast().getLowBound() - 1;
		} else {
		  maxtime = si.getLast().getHighBound();
		}		
		
		for (int t = 0; t <= maxtime; t++) {			
			_currentHead = new TimeExpandedPath();
			_currentHead.append(new StepSourceFlow(source, t, true));
			int f = si.getFlowAt(t);
			si.augmentUnsafe(t, -f); // set flow to 0		
			
			continuePath(source, t, f);
		}	
		return sum;
	}
	
	private int continuePath(Node node, int t, int targetFlow) {
		int sum = 0;
		
		//System.out.println("Decompose at node " + node.getId() + " @ " + t);

		if (_settings.isSink(node)) {
			int f = finishPath(node, t, targetFlow);
			sum += f;
			targetFlow -= f;
		}
		
		for (Link edge : node.getOutLinks().values()) {
			int f = _flow.getFlow(edge).getFlowAt(t);
			f = Math.min(targetFlow, f);
			if (f > 0) {				
				_flow.getFlow(edge).augment(t, -f); // reduce flow
				_currentHead.append(new StepEdge(edge, t, t + _settings.getLength(edge), true));				
				f = continuePath(edge.getToNode(), t + _settings.getLength(edge), f);
				sum += f;
				targetFlow -= f;
				_currentHead.removeLast();
			}
		}
		
		if (_settings.useHoldover) {
			int f = _flow.getHoldover(node).getFlowAt(t);
			f = Math.min(targetFlow, f);
			if (f > 0) {
				// just do a single step in time and recurse ...
				
				_flow.getHoldover(node).augmentUnsafe(t, t+1, -f);
				
				// try to join with the last step for a tiny bit of efficiency
				if (_currentHead.getPathSteps().getLast() instanceof StepHold) {
					StepHold old = (StepHold) _currentHead.getPathSteps().getLast();
					_currentHead.removeLast();
					_currentHead.append(old.copyShiftedToArrival(t + 1));
					f = continuePath(node, t+1, f);
					sum += f;
					targetFlow -= f;
					_currentHead.removeLast();
					_currentHead.append(old);				
				} else { // start a new step
					_currentHead.append(new StepHold(node, t, t+1, true));
					f = continuePath(node, t+1, f);
					sum += f;
					targetFlow -= f;
					_currentHead.removeLast();
				}
			}
		}
		
		
		return sum;
	}
	
	private int finishPath(Node node, int t, int targetFlow) {
		//System.out.println("Decompose at sink " + node.getId() + " @ " + t);
		
		int f = _flow.getSinkFlow(node).getFlowAt(t);
		
		f = Math.min(targetFlow, f);
		
		if (f > 0) {	
			_flow.getSinkFlow(node).augmentUnsafe(t, -f);
			_currentHead.append(new StepSinkFlow(node, t, true));
			TimeExpandedPath copy = TimeExpandedPath.clone(_currentHead);
			copy.setFlow(f);
			_paths.add(copy);		
			_currentHead.removeLast();
		}
		return f;	
	}
	
}
