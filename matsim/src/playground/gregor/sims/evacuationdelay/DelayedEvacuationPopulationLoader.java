package playground.gregor.sims.evacuationdelay;

import java.util.List;

import org.matsim.api.basic.v01.Coord;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.EvacuationConfigGroup.Scenario;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.evacuation.base.Building;
import org.matsim.evacuation.base.EvacuationPopulationFromShapeFileLoader;
import org.matsim.evacuation.base.EvacuationStartTimeCalculator;


public class DelayedEvacuationPopulationLoader extends EvacuationPopulationFromShapeFileLoader {
	private DelayedEvacuationStartTimeCalculator startTimer;

	public DelayedEvacuationPopulationLoader(List<Building> buildings,
			NetworkLayer network, Config config) {
		super(buildings, network, config);
		double baseTime = config.evacuation().getScanrio() == Scenario.day ? 12 * 3600 : 3 * 3600;
		this.startTimer = new DelayedEvacuationStartTimeCalculator(baseTime,config.evacuation().getShorelineFile());
	}





	@Override
	protected EvacuationStartTimeCalculator getEndCalculatorTime() {
		return this.startTimer; 
	}




}
