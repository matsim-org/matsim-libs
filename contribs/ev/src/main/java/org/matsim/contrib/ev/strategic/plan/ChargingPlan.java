package org.matsim.contrib.ev.strategic.plan;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * This class represents a charging plan. It allows to track the score of a
 * charging plan and indicates the individual charging activities that are to be
 * implemented throghout the day.
 * 
 * @author Sebastian Hörl (sebhoerl), IRT SystemX
 */
public class ChargingPlan {
	@JsonProperty("activities")
	private final List<ChargingPlanActivity> activities = new ArrayList<>();

	@JsonProperty("score")
	private double score = Double.NaN;

	public void setScore(double score) {
		this.score = score;
	}

	public double getScore() {
		return score;
	}

	@JsonIgnore
	public List<ChargingPlanActivity> getChargingActivities() {
		return Collections.unmodifiableList(activities);
	}

	public void addChargingActivity(ChargingPlanActivity activity) {
		activities.add(activity);
	}

	ChargingPlan createCopy() {
		ChargingPlan copyPlan = new ChargingPlan();
		copyPlan.setScore(score);

		for (ChargingPlanActivity activity : activities) {
			ChargingPlanActivity copyActivity = activity.createCopy();
			copyPlan.addChargingActivity(copyActivity);
		}

		return copyPlan;
	}
}
