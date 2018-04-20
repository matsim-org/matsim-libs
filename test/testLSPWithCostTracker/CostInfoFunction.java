package testLSPWithCostTracker;

import java.util.ArrayList;
import java.util.Collection;

import lsp.functions.InfoFunction;
import lsp.functions.InfoFunctionValue;



public class CostInfoFunction implements InfoFunction {

	private FixedCostFunctionValue fixedValue;
	private LinearCostFunctionValue linearValue;
	private Collection<InfoFunctionValue<?>> values;
	
	public CostInfoFunction() {
		values = new ArrayList<InfoFunctionValue<?>>();
		fixedValue = new FixedCostFunctionValue();
		linearValue = new LinearCostFunctionValue();
		values.add(fixedValue);
		values.add(linearValue);
		
	}
	
	@Override
	public Collection<InfoFunctionValue<?>> getValues() {
		return values;
	}

}
