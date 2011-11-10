package org.matsim.locationchoice.utils;

import java.util.HashSet;
import java.util.List;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.groups.LocationChoiceConfigGroup;
import org.matsim.locationchoice.bestresponse.scoring.ScaleEpsilon;

public class ActivitiesHandler {
	private HashSet<String> flexibleTypes = new HashSet<String>();
	private static final Logger log = Logger.getLogger(ActivitiesHandler.class);
	private LocationChoiceConfigGroup dcconfig;

	public ActivitiesHandler(final LocationChoiceConfigGroup dcconfig) {
		this.dcconfig = dcconfig;
		this.initFlexibleTypes(dcconfig);
	}
	
	public ScaleEpsilon createScaleEpsilon() {
		ScaleEpsilon scaleEpsilon = new ScaleEpsilon();
		String factors = this.dcconfig.getEpsilonScaleFactors();
		String types = this.dcconfig.getFlexibleTypes();
		
		if (!types.equals("null")) {			
			String[] fentries = factors.split(",", -1);
			String[] tentries = types.split(",", -1);
			
			// check if demand is v1 with types = s0.5, ...
			if (tentries[0].length() == 1) {
				scaleEpsilon.setUseSimpleTypes(true);
			}
			
			int fentriesLength = fentries.length;
			if (fentries[0].equals("null")) fentriesLength = 0;
			
			int tentriesLength = tentries.length;
			if (tentries[0].equals("null")) tentriesLength = 0;
			
			if (fentriesLength != tentriesLength || factors.equals("null")) {
				log.error("Specify an epsilon (now: " + fentriesLength + " specified) " +
						"for every flexible activity type (now: " + tentriesLength + " specified)!");
				System.exit(1);
			}			
			for (int i = 0; i < fentries.length; i++) {
					scaleEpsilon.setEpsilonFactor(tentries[i].trim(), Double.parseDouble(fentries[i].trim()));
			}
		}	
		return scaleEpsilon;
	}
	
	public ActTypeConverter createActivityTypeConverter() {
		String types = this.dcconfig.getFlexibleTypes();
		String[] tentries = types.split(",", -1);
		
		// check if demand = v1
		if (tentries[0].length() == 1) {
			return new ActTypeConverter(true);
		}
		else {
			return new ActTypeConverter(false);
		}
	}

	// only used by TGSimple
	public List<Activity> getFlexibleActivities(final Plan plan) {
		List<Activity> flexibleActivities = new Vector<Activity>();
		this.getFlexibleActs(plan, flexibleActivities);
		return flexibleActivities;
	}

	private void getFlexibleActs(Plan plan, List<Activity> flexibleActivities) {
		for (int i = 0; i < plan.getPlanElements().size(); i = i + 2) {
			Activity act = (Activity)plan.getPlanElements().get(i);
			if (this.flexibleTypes.contains(act.getType())) {
				flexibleActivities.add(act);
			}
		}
	}

	private void initFlexibleTypes(LocationChoiceConfigGroup config) {
		String types = config.getFlexibleTypes();
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
