package playground.gregor.sims.evacuationdelay;

import java.util.List;

import org.matsim.core.config.Config;
import org.matsim.core.config.groups.EvacuationConfigGroup.Scenario;
import org.matsim.core.network.NetworkLayer;

import playground.gregor.sims.evacbase.Building;
import playground.gregor.sims.evacbase.EvacuationPopulationFromShapeFileLoader;
import playground.gregor.sims.evacbase.EvacuationStartTimeCalculator;

public class DelayedEvacuationPopulationLoader extends EvacuationPopulationFromShapeFileLoader {

	public DelayedEvacuationPopulationLoader(List<Building> buildings,
			NetworkLayer network, Config config) {
		super(buildings, network, config);
	}

	@Override
	protected EvacuationStartTimeCalculator getEndCalculatorTime(
			Scenario scenario) {
		double earliestEvacTime = Double.NaN;
		if (scenario == Scenario.day) {
			earliestEvacTime = 12 * 3600;
		} else if (scenario == Scenario.night) {
			earliestEvacTime = 3 * 3600;
		}
		return new DelayedEvacuationStartTimeCalculator(earliestEvacTime,this.config.evacuation().getShorelineFile());
	}

	
}
