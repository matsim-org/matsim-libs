package usecases.chessboard;

import org.matsim.contrib.freight.replanning.CarrierReplanningStrategy;
import org.matsim.contrib.freight.replanning.selectors.SelectBestPlan;


public class SelectBestPlanStrategyFactory {
	
	public CarrierReplanningStrategy createStrategy(){
		return new CarrierReplanningStrategy(new SelectBestPlan());
	}

}
