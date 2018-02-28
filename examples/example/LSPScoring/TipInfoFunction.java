package example.LSPScoring;

import java.util.ArrayList;
import java.util.Collection;

import lsp.functions.InfoFunction;
import lsp.functions.InfoFunctionValue;

public class TipInfoFunction  implements InfoFunction{

	private Collection <InfoFunctionValue> values;
	
	public TipInfoFunction() {
		this.values = new ArrayList<InfoFunctionValue>();
	}
	
	
	@Override
	public Collection<InfoFunctionValue> getValues() {
		return values;
	}

}
