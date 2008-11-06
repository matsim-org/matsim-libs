package org.matsim.basic.v01;

import java.util.ArrayList;
import java.util.List;


public class BasicKnowledgeImpl implements BasicKnowledge {

	private List<BasicActivity> activities;
	private String description;
	
	public void addActivity(BasicActivity activity) {
		if (this.activities == null) {
			this.activities = new ArrayList<BasicActivity>();
		}
		this.activities.add(activity);
	}

	public List<BasicActivity> getActivities() {
		return this.activities;
	}

	public String getDescription(String desc) {
		return this.description;
	}

	public void setDescription(String desc) {
		this.description = desc;
	}

}
