package gunnar.ihop2.roadpricing;

import java.util.List;

import org.matsim.api.core.v01.Scenario;
import org.matsim.roadpricing.RoadPricingScheme;
import org.matsim.roadpricing.RoadPricingSchemeImpl;

import floetteroed.opdyts.DecisionVariable;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
class TollLevels implements DecisionVariable {

	// -------------------- MEMBERS --------------------

	final double level1start_s;
	final double morningLevel2start_s;
	final double morningLevel3start_s;
	final double morningLevel3end_s;
	final double morningLevel2end_s;
	final double eveningLevel2start_s;
	final double eveningLevel3start_s;
	final double eveningLevel3end_s;
	final double eveningLevel2end_s;
	final double level1end_s;
	final double level1cost_money;
	final double level2cost_money;
	final double level3cost_money;

	private final Scenario scenario;

	// -------------------- CONSTRUCTION --------------------

	public TollLevels(final double level1start_s,
			final double morningLevel2start_s,
			final double morningLevel3start_s, final double morningLevel3end_s,
			final double morningLevel2end_s, final double eveningLevel2start_s,
			final double eveningLevel3start_s, final double eveningLevel3end_s,
			final double eveningLevel2end_s, final double level1end_s,
			final double level1cost_sek, final double level2cost_sek,
			final double level3cost_sek, final Scenario scenario) {
		this.level1start_s = level1start_s;
		this.morningLevel2start_s = morningLevel2start_s;
		this.morningLevel3start_s = morningLevel3start_s;
		this.morningLevel3end_s = morningLevel3end_s;
		this.morningLevel2end_s = morningLevel2end_s;
		this.eveningLevel2start_s = eveningLevel2start_s;
		this.eveningLevel3start_s = eveningLevel3start_s;
		this.eveningLevel3end_s = eveningLevel3end_s;
		this.eveningLevel2end_s = eveningLevel2end_s;
		this.level1end_s = level1end_s;
		this.level1cost_money = level1cost_sek;
		this.level2cost_money = level2cost_sek;
		this.level3cost_money = level3cost_sek;

		this.scenario = scenario;
	}

	// --------------- IMPLEMENTATION OF DecisionVariable ---------------

	@Override
	public void implementInSimulation() {
		
		final RoadPricingScheme roadPricingScheme = (RoadPricingScheme) this.scenario
				.getScenarioElement(RoadPricingScheme.ELEMENT_NAME);
		
		for (List<RoadPricingSchemeImpl.Cost> costList : roadPricingScheme
				.getTypicalCostsForLink().values()) {
			costList.clear();
			costList.add(new RoadPricingSchemeImpl.Cost(this.level1start_s,
					this.morningLevel2start_s, this.level1cost_money));
			costList.add(new RoadPricingSchemeImpl.Cost(
					this.morningLevel2start_s, this.morningLevel3start_s,
					this.level2cost_money));
			costList.add(new RoadPricingSchemeImpl.Cost(
					this.morningLevel3start_s, this.morningLevel3end_s,
					this.level3cost_money));
			costList.add(new RoadPricingSchemeImpl.Cost(
					this.morningLevel3end_s, this.morningLevel2end_s,
					this.level2cost_money));
			costList.add(new RoadPricingSchemeImpl.Cost(
					this.morningLevel2end_s, this.eveningLevel2start_s,
					this.level1cost_money));
			costList.add(new RoadPricingSchemeImpl.Cost(
					this.eveningLevel2start_s, this.eveningLevel3start_s,
					this.level2cost_money));
			costList.add(new RoadPricingSchemeImpl.Cost(
					this.eveningLevel3start_s, this.eveningLevel3end_s,
					this.level3cost_money));
			costList.add(new RoadPricingSchemeImpl.Cost(
					this.eveningLevel3end_s, this.eveningLevel2end_s,
					this.level2cost_money));
			costList.add(new RoadPricingSchemeImpl.Cost(
					this.eveningLevel2end_s, this.level1end_s,
					this.level1cost_money));
		}
	}
}
