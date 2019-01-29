package org.matsim.contrib.carsharing.manager.demand.membership;

import java.util.Map;
import java.util.Set;
/** 
 * @author balac
 */
public class PersonMembership {
	
	private Map<String, Set<String>> membershipsPerCompany;
	private Map<String, Set<String>> membershipsPerCSType;
	
	public PersonMembership(Map<String, Set<String>> membershipsPerCompany, 
			Map<String, Set<String>> membershipsPerCSType) {
				
		this.membershipsPerCompany = membershipsPerCompany;
		this.membershipsPerCSType = membershipsPerCSType;
	}

	public Map<String, Set<String>> getMembershipsPerCSType() {
		return membershipsPerCSType;
	}
	
	public Map<String, Set<String>> getMembershipsPerCompany() {
		return membershipsPerCompany;
	}
}
