package org.matsim.contrib.ev.strategic.plan;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.ev.withinday.WithinDayEvEngine;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Preconditions;
import com.google.common.base.Verify;

/**
 * This class is a container for the charging plans that an agent may accumulate
 * during charging replaninng. Each regular MATSim plan contains one of these
 * objects indicating the underlying charging plans.
 * 
 * @author Sebastian Hörl (sebhoerl), IRT SystemX
 */
public class ChargingPlans {
	static public final String ATTRIBUTE = "charging";

	@JsonProperty("plans")
	private final List<ChargingPlan> chargingPlans = new LinkedList<>();

	@JsonProperty("selected")
	private int selectedIndex = -1;

	@JsonIgnore
	private Plan ownerPlan = null;

	public List<ChargingPlan> getChargingPlans() {
		return Collections.unmodifiableList(chargingPlans);
	}

	public void addChargingPlan(ChargingPlan chargingPlan) {
		if (selectedIndex == -1) {
			selectedIndex = 0;
		}

		chargingPlans.add(chargingPlan);
	}

	public void removeChargingPlan(ChargingPlan chargingPlan) {
		int removeIndex = chargingPlans.indexOf(chargingPlan);

		if (removeIndex >= 0) {
			if (removeIndex == selectedIndex) {
				selectedIndex = -1;
			} else if (removeIndex < selectedIndex) {
				selectedIndex--;
			}

			chargingPlans.remove(removeIndex);
		}
	}

	public void setSelectedPlan(ChargingPlan chargingPlan) {
		selectedIndex = chargingPlans.indexOf(chargingPlan);
		Verify.verify(selectedIndex >= 0, "Plan does not exist");
	}

	@JsonIgnore
	public ChargingPlan getSelectedPlan() {
		return selectedIndex >= 0 ? chargingPlans.get(selectedIndex) : null;
	}

	private ChargingPlans createCopy() {
		ChargingPlans copyPlans = new ChargingPlans();

		for (ChargingPlan plan : chargingPlans) {
			ChargingPlan copyPlan = plan.createCopy();
			copyPlans.addChargingPlan(copyPlan);

			if (getSelectedPlan() == plan) {
				copyPlans.setSelectedPlan(copyPlan);
			}
		}

		return copyPlans;
	}

	static public ChargingPlans get(Plan plan) {
		/*
		 * The regular replanning proces copies plans, including their attributes.
		 * However, this means that the reference to the ChargingPlans object is copied
		 * when a new regular plan is created. So the ChargingPlans of the new regular
		 * plan will point to the same object as for the initial plan. However, we want
		 * that a completely new object is used. Therefore, plans should always be
		 * obtained using the present function. Besides retrieving the attribute, it
		 * checks the ownerPlan variable that indicates the regular plan to which a
		 * ChargingPlans belongs. If a ChargingPlans object is retrieved from a regular
		 * plan, but it doesn't indicate that regular plan as its "ownerPlan", a deep
		 * copy of the ChargingPlans object is created and assigned to the regular plan
		 * in question.
		 */
		Preconditions.checkState(WithinDayEvEngine.isActive(plan.getPerson()),
				"Attempting to obtain charging plans for an agent that is not enabled.");

		ChargingPlans chargingPlans = (ChargingPlans) plan.getAttributes().getAttribute(ChargingPlans.ATTRIBUTE);

		if (chargingPlans == null) {
			chargingPlans = new ChargingPlans();
			plan.getAttributes().putAttribute(ChargingPlans.ATTRIBUTE, chargingPlans);
			chargingPlans.ownerPlan = plan;
		}

		// manage deep copy of charging plans after creating new regular plans
		if (chargingPlans.ownerPlan == null) {
			chargingPlans.ownerPlan = plan;
		} else if (chargingPlans.ownerPlan != plan) {
			chargingPlans = chargingPlans.createCopy();
			plan.getAttributes().putAttribute(ChargingPlans.ATTRIBUTE, chargingPlans);
			chargingPlans.ownerPlan = plan;
		}

		return chargingPlans;
	}
}
