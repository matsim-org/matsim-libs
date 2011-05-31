package utils;

import org.matsim.api.core.v01.network.Link;

public class MergingConstraint {
	
	public boolean judge(Link link1, Link link2){
		if(link1.getCapacity() == link2.getCapacity()){
			if(link1.getFreespeed() == link2.getFreespeed()){
				if(link1.getNumberOfLanes() == link2.getNumberOfLanes()){
						return true;
				}
			}
		}
		return false;
	}

}
