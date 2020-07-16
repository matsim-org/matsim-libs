package example.lsp.simulationTrackers;

import java.util.ArrayList;
import java.util.Collection;

import lsp.functions.LSPInfoFunction;
import lsp.functions.LSPInfoFunctionValue;



/*package-private*/ class CostInfoFunction implements LSPInfoFunction {

	private FixedCostFunctionValue fixedValue;
	private LinearCostFunctionValue linearValue;
	private Collection<LSPInfoFunctionValue<?>> values;
	
	public CostInfoFunction() {
		values = new ArrayList<LSPInfoFunctionValue<?>>();
		fixedValue = new FixedCostFunctionValue();
		linearValue = new LinearCostFunctionValue();
		values.add(fixedValue);
		values.add(linearValue);
		
	}
	
	@Override
	public Collection<LSPInfoFunctionValue<?>> getValues() {
		return values;
	}

}
