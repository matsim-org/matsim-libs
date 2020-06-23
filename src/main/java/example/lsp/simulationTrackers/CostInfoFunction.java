package example.lsp.simulationTrackers;

import java.util.ArrayList;
import java.util.Collection;

import lsp.functions.InfoFunction;
import lsp.functions.InfoFunctionValue;



public class CostInfoFunction implements InfoFunction {

	private example.lsp.simulationTrackers.FixedCostFunctionValue fixedValue;
	private example.lsp.simulationTrackers.LinearCostFunctionValue linearValue;
	private Collection<InfoFunctionValue<?>> values;
	
	public CostInfoFunction() {
		values = new ArrayList<InfoFunctionValue<?>>();
		fixedValue = new example.lsp.simulationTrackers.FixedCostFunctionValue();
		linearValue = new example.lsp.simulationTrackers.LinearCostFunctionValue();
		values.add(fixedValue);
		values.add(linearValue);
		
	}
	
	@Override
	public Collection<InfoFunctionValue<?>> getValues() {
		return values;
	}

}
