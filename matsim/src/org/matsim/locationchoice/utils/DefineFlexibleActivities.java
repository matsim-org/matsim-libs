package org.matsim.locationchoice.utils;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.matsim.api.basic.v01.population.BasicPlanElement;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.knowledges.Knowledges;

public class DefineFlexibleActivities {
	
	private Knowledges knowledges = null;
	HashSet<String> flexibleTypes = new HashSet<String>();
	
	public DefineFlexibleActivities(Knowledges kn) {
		this.knowledges = kn;
		this.initFlexibleTypes();
	}
	
	public List<ActivityImpl> getFlexibleActivities(final PlanImpl plan) {		
		List<ActivityImpl> flexibleActivities = new Vector<ActivityImpl>();
		if (flexibleTypes.size() > 0) {
			this.getFlexibleActs(plan, flexibleActivities);
		}
		else {
			this.getFlexibleActsBasedOnKnowledge(plan, flexibleActivities);
		}
		return flexibleActivities;
	}
	
	private void getFlexibleActs(PlanImpl plan, List<ActivityImpl> flexibleActivities) {				
		for (int i = 0; i < plan.getPlanElements().size(); i = i + 2) {
			ActivityImpl act = (ActivityImpl)plan.getPlanElements().get(i);
			if (this.flexibleTypes.contains(act.getType())) {
				flexibleActivities.add(act);
			}
		}
	}
	
	/*
	 * Get all activities which are allowed to be relocated (incl. "primaries")
	 */
	private void getFlexibleActsBasedOnKnowledge(PlanImpl plan, List<ActivityImpl> flexibleActivities) {				
		for (int i = 0; i < plan.getPlanElements().size(); i = i + 2) {
			ActivityImpl act = (ActivityImpl)plan.getPlanElements().get(i);
			boolean isPrimary = this.knowledges.getKnowledgesByPersonId().get(plan.getPerson().getId()).isPrimary(act.getType(), act.getFacilityId());
			if (!isPrimary && !(act.getType().startsWith("h") || act.getType().startsWith("tta"))) {
				flexibleActivities.add(act);
			}
		}
		flexibleActivities.addAll(this.getMovablePrimaryActivities(plan));
	}
	
	/*
	 * Get only "primary" activities which are allowed to be relocated
	 */	
	public List<ActivityImpl> getMovablePrimaryActivities(final PlanImpl plan) {
		
		List<ActivityImpl> primaryActivities = new Vector<ActivityImpl>();
		
		final List<? extends BasicPlanElement> actslegs = plan.getPlanElements();
		for (int j = 0; j < actslegs.size(); j=j+2) {
			final ActivityImpl act = (ActivityImpl)actslegs.get(j);
			if (act.getType().startsWith("h") || act.getType().startsWith("tta")) continue;
			boolean isPrimary = this.knowledges.getKnowledgesByPersonId().get(plan.getPerson().getId()).isPrimary(act.getType(), act.getFacilityId());
			
			if (isPrimary) {
				primaryActivities.add(act);
			}
		}
		Collections.shuffle(primaryActivities);
		
		List<ActivityImpl> movablePrimaryActivities = new Vector<ActivityImpl>();
		
		// key: activity.type + activity.facility
		HashMap<String, Boolean> fixPrimaries = new HashMap<String, Boolean>();
				
		Iterator<ActivityImpl> it = primaryActivities.iterator();
		while (it.hasNext()) {
			ActivityImpl a = it.next();		
			String key = a.getType()+a.getFacility().getId().toString();
			if (fixPrimaries.containsKey(key)) {
				// there is already one activity performed of the specific type at this location
				movablePrimaryActivities.add(a);
			}
			else {
				fixPrimaries.put(key, true);
			}
		}
		return movablePrimaryActivities;
	}
	
	private void initFlexibleTypes() {
		String types = Gbl.getConfig().locationchoice().getFlexibleTypes();
		if (!types.equals("null")) {
			String[] entries = types.split(",", -1);
			for (int i = 0; i < entries.length; i++) {
				this.flexibleTypes.add(entries[i].trim());
			}
		}
	}

	public HashSet<String> getFlexibleTypes() {
		return flexibleTypes;
	}

	public void setFlexibleTypes(HashSet<String> flexibleTypes) {
		this.flexibleTypes = flexibleTypes;
	}
	
}
