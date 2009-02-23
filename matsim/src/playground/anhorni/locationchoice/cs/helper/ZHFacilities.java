package playground.anhorni.locationchoice.cs.helper;

import java.util.ArrayList;
import java.util.TreeMap;

import org.matsim.interfaces.basic.v01.Id;

public class ZHFacilities {
	
	private TreeMap<Id, ZHFacility> zhFacilities = new TreeMap<Id, ZHFacility>();	
	private TreeMap<Id, ArrayList<Id>> zhFacilitiesByLink = new TreeMap<Id, ArrayList<Id>>();
		

}
