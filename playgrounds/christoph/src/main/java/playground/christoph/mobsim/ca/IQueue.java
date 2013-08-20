/* *********************************************************************** *
 * project: org.matsim.*
 * IQueue.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.christoph.mobsim.ca;

import java.util.LinkedList;

// Implements basic queue functionality for link and nodes waiting queues in CAServer
public class IQueue {
//properties (Access = private)
//    queue;
//    currentIndex;
//end

	private final LinkedList<CAAgent> queue;
//	private int currentIndex;
	
//methods
//    function this = IQueue(queue)
//        this.queue = queue;
//        this.currentIndex = 0;
//    end
	public IQueue(LinkedList<CAAgent> queue) {
		this.queue = queue;
//		this.currentIndex = 0;
	}
	
//    function prepareForIteration(this)
//       this.currentIndex = length(this.queue); 
//    end
	public void prepareForIteration() {
//		this.currentIndex = this.queue.size();
		this.queue.clear();
	}
	
//    function [hasMoreElements] = hasMoreElements(this)
//        hasMoreElements = false;
//        if (this.currentIndex >= 1)
//           hasMoreElements = true; 
//        end
//    end
	public boolean isEmpty() {
//		if (this.currentIndex >= 1) {
//			return true;
//		} else return false;
		return this.queue.isEmpty();
	}
	
//    function [index] = getCurrentIndex(this)
//        index = this.currentIndex;
//    end
//	@Deprecated
//	public int getCurrentIndex() {
//		return this.currentIndex;
//	}
	
//    function [lastIndex] = getLastIndex(this)
//        lastIndex = length(this.queue);
//    end
//	@Deprecated
//	public int getLastIndex() {
//		return this.queue.size();
//	}
	public int getSize() {
		return this.queue.size();
	}
	
//    function [agent] = getElement(this, index)
//        agent = this.queue(index);
//    end
//	@Deprecated
//	public CAAgent getElement(int index) {
//		return this.queue.get(index);
//	}
	
//    function [agent] = getNextElement(this)
//        agent = this.queue(this.currentIndex);
//        this.currentIndex = this.currentIndex - 1;
//    end 
	public CAAgent getFirstElement() {
		CAAgent agent = this.queue.getFirst();
		return agent;
	}
	
//    function addElement(this, el)
//        this.queue = [el this.queue];
//        this.currentIndex = this.currentIndex + 1;
//    end
	public void addElement(CAAgent agent) {
		this.queue.addLast(agent);
//		this.currentIndex++;
	}
	
//    function [lastElement] = getLastElement(this)
//        lastElement = this.queue(length(this.queue));
//    end
	public CAAgent getLastElement() {
		return this.queue.getLast();
	}
	
//    function [hasElements] = hasElements(this)
//        hasElements = (~isempty(this.queue));
//    end
	public boolean hasElements() {
		return !this.queue.isEmpty();
	}
	
//    function removeElements(this, indices)
//        this.queue(indices) = [];
//    end
	
//    function removeLastElement(this)          
//        this.queue(length(this.queue)) = [];
//        this.currentIndex = this.currentIndex - 1;
//    end
	
	public CAAgent removeFirstElement() {
		return this.queue.removeFirst();
	}
	
	public boolean removeElement (CAAgent agent) {
		return this.queue.remove(agent);
	}
	
	public CAAgent removeLastElement() {
		return this.queue.removeLast();
//		this.currentIndex--;
	}
	
//    function removeElement(this, el)
//        for i=1:length(this.queue)
//            if (strcmp(el.getId(), this.queue(i).getId()))
//                this.queue(i) = [];
//                this.currentIndex = this.currentIndex - 1;
//            end
//        end
//    end
//	public void removeElement(CAAgent agent) {
//		if (this.queue.remove(agent)) this.currentIndex--;
//	}
}
