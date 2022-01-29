package example.lsp.simulationTrackers;

import java.util.ArrayList;
import java.util.Collection;

import lsp.functions.LSPInfoFunction;
import lsp.functions.LSPInfoFunctionValue;



/*package-private*/ class CostInfoFunction implements LSPInfoFunction {

	private final FixedCostFunctionValue fixedValue;
	private final LinearCostFunctionValue linearValue;
	private final Collection<LSPInfoFunctionValue<?>> values;
	
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

	//Introduce getters for fixed and linear Value
	// --> TODO: Macht es nicht Sinn, möglichst auf diese direkt zuzugreifen (und damit deren Werte setzten zu können)
	//  		anstatt diese dann noch erst in den "values" zu "verstecken"?
	//  		Hilft aber auch noch nicht viel, solange CostInfo dann immer noch non-public ist.	KMT, Okt 20
	public FixedCostFunctionValue getFixedValue() {
		return fixedValue;
	}

	public LinearCostFunctionValue getLinearValue() {
		return linearValue;
	}
}
