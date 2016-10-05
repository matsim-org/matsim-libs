package playground.sebhoerl.avtaxi.refactor;

import org.apache.commons.configuration.Configuration;
import org.matsim.contrib.taxi.optimizer.AbstractTaxiOptimizerParams;

public class AVAggregateHeuristicOptimizerParams extends AbstractTaxiOptimizerParams {
	public final long maximumPassengers;
	public final double maximumAggregationDelay;
	public final long grid_x;
	public final long grid_y;
	
	protected AVAggregateHeuristicOptimizerParams(Configuration optimizerConfig) {
		super(optimizerConfig);
		
		maximumPassengers = optimizerConfig.getLong("maximumPassengers", 4);
		maximumAggregationDelay = optimizerConfig.getDouble("maximumAggregationDelay", 60.0);
		grid_x = optimizerConfig.getLong("gridX", 20);
		grid_y = optimizerConfig.getLong("gridY", 20);
	}

}
