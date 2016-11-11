package playground.paschke.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.matsim.contrib.carsharing.manager.supply.costs.CompanyCosts;
import org.matsim.contrib.carsharing.manager.supply.costs.CostCalculation;
import org.matsim.contrib.carsharing.manager.supply.costs.CostsCalculatorContainer;
import org.matsim.contrib.carsharing.runExample.CarsharingUtils;

public class ExampleCarsharingUtils extends CarsharingUtils {
	public static CostsCalculatorContainer createCompanyCostsStructure(Set<String> companies) {
		
		CostsCalculatorContainer companyCostsContainer = new CostsCalculatorContainer();
		
		for (String s : companies) {
			
			Map<String, CostCalculation> costCalculations = new HashMap<String, CostCalculation>();
			
			//=== here customizable cost structures come in ===
			//===what follows is just an example!! and should be modified according to the study at hand===
			costCalculations.put("freefloating", new CostCalculationExample());
			costCalculations.put("twoway", new CostCalculationExample());
			costCalculations.put("oneway", new CostCalculationExample());
			CompanyCosts companyCosts = new CompanyCosts(costCalculations);
			
			companyCostsContainer.getCompanyCostsMap().put(s, companyCosts);
		}
		
		return companyCostsContainer;
		
	}
}
