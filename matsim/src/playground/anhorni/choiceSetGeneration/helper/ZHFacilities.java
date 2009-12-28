package playground.anhorni.choiceSetGeneration.helper;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.geometry.CoordUtils;

public class ZHFacilities {
	
	private TreeMap<Id, ZHFacility> zhFacilities = new TreeMap<Id, ZHFacility>();	
	private TreeMap<Id, ArrayList<Id>> zhFacilitiesByLink = new TreeMap<Id, ArrayList<Id>>();
	private int numberOfFacilities;
	
	
	public ZHFacilities() {
		this.zhFacilities = new TreeMap<Id, ZHFacility>();
		this.zhFacilitiesByLink = new TreeMap<Id, ArrayList<Id>>();
	}
	
	public void addFacilityByLink(Id linkId, ZHFacility facility) {
	
		if (this.zhFacilitiesByLink.containsKey(linkId)) {
			this.zhFacilitiesByLink.get(linkId).add(facility.getId());
		}
		else {
			ArrayList<Id> list = new ArrayList<Id>();
			list.add(facility.getId());
			this.zhFacilitiesByLink.put(linkId,list);
		}
		
		if (!this.zhFacilities.containsKey(facility.getId())) {
			this.zhFacilities.put(facility.getId(), facility);
			this.numberOfFacilities += 1;
		}			
	}
	
	public ArrayList<ZHFacility> getFacilitiesByLinkId(Id linkId) {
		ArrayList<Id> idList = this.zhFacilitiesByLink.get(linkId);
		
		ArrayList<ZHFacility> facilitiesList = new ArrayList<ZHFacility>();
		
		Iterator<Id> idList_it = idList.iterator();
		while (idList_it.hasNext()) {
			Id id = idList_it.next();
			facilitiesList.add(this.zhFacilities.get(id));					
		}
		return facilitiesList;
	}
	
	
	public void addFacilitiesByLink(Id linkId, ArrayList<ZHFacility> facilitiesList) {		
		Iterator<ZHFacility> facility_it = facilitiesList.iterator();
		while (facility_it.hasNext()) {
			ZHFacility facility = facility_it.next();
			this.addFacilityByLink(facility.getId(), facility);					
		}
	}

	public TreeMap<Id, ZHFacility> getZhFacilities() {
		return zhFacilities;
	}

	public void setZhFacilities(TreeMap<Id, ZHFacility> zhFacilities) {
		this.zhFacilities = zhFacilities;
	}

	public TreeMap<Id, ArrayList<Id>> getZhFacilitiesByLink() {
		return zhFacilitiesByLink;
	}

	public void setZhFacilitiesByLink(TreeMap<Id, ArrayList<Id>> zhFacilitiesByLink) {
		this.zhFacilitiesByLink = zhFacilitiesByLink;
	}

	public int getNumberOfFacilities() {
		return numberOfFacilities;
	}
	
	private void calculateAccesibilities() {		
		Iterator<ZHFacility> referenceFacility_it = this.zhFacilities.values().iterator();
		while (referenceFacility_it.hasNext()) {
			ZHFacility referenceFacility = referenceFacility_it.next();
			double accessibility02 = 0.0;
			double accessibility10 = 0.0;
			double accessibility20 = 0.0;
		
			Iterator<ZHFacility> facilities_it = this.zhFacilities.values().iterator();
			while (facilities_it.hasNext()) {
				ZHFacility facility = facilities_it.next();
				
				// distance in km
				double distance = 0.001 * CoordUtils.calcDistance(facility.getExactPosition(), referenceFacility.getExactPosition());
				accessibility02 += Math.exp(0.2 * distance * (-1.0));
				accessibility10 += Math.exp(1.0 * distance * (-1.0));
				accessibility20 += Math.exp(2.0 * distance * (-1.0));	
			}
			referenceFacility.setAccessibility02(accessibility02);
			referenceFacility.setAccessibility10(accessibility10);
			referenceFacility.setAccessibility20(accessibility20);
		}
	}	
	
	public void finish() {
		this.calculateAccesibilities();
	}
}
