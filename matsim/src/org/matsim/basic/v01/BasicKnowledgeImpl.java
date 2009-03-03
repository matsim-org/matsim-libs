package org.matsim.basic.v01;

import java.util.ArrayList;
import java.util.List;


public class BasicKnowledgeImpl implements BasicKnowledge<BasicActivityOption> {

	private List<BasicActivityOption> activities;
	private String description;
	
	public void addActivity(BasicActivityOption activity) {
		if (this.activities == null) {
			this.activities = new ArrayList<BasicActivityOption>();
		}
		this.activities.add(activity);
	}

	public List<BasicActivityOption> getActivities() {
		return this.activities;
	}

	public String getDescription() {
		return this.description;
	}

	public void setDescription(String desc) {
		this.description = desc;
	}

}
