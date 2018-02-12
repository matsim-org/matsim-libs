package lspScoringTests;

import java.util.ArrayList;
import java.util.Collection;

import lsp.functions.InfoFunction;
import lsp.functions.InfoFunctionValue;

public class TrinkgeldInfoFunction  implements InfoFunction{

	private Collection <InfoFunctionValue> values;
	
	public TrinkgeldInfoFunction() {
		this.values = new ArrayList<InfoFunctionValue>();
	}
	
	
	@Override
	public Collection<InfoFunctionValue> getValues() {
		return values;
	}

}
