package org.matsim.contrib.transEnergySim.events;

import java.util.LinkedList;

public class EventManager<T> {
	
	protected LinkedList<T> handlers=new LinkedList<>();
	
	public void addEventHandler(T handler){
		handlers.add(handler);
	}

}
