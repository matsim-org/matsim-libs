package cascadingInfoTest;

import java.util.ArrayList;
import java.util.Collection;

import lsp.functions.InfoFunction;
import lsp.functions.InfoFunctionValue;



public class AverageTimeInfoFunction implements InfoFunction{

	private Collection<InfoFunctionValue<?>> values;
	
	public AverageTimeInfoFunction() {
		values = new ArrayList<InfoFunctionValue<?>>();
		values.add(new AverageTimeInfoFunctionValue());
	}
		
	@Override
	public Collection<InfoFunctionValue<?>> getValues() {
		return values;
	}

}
