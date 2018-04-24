package cascadingInfoTest;

import lsp.functions.Info;
import lsp.functions.InfoFunction;
import lsp.functions.InfoFunctionImpl;
import lsp.functions.InfoFunctionValue;
import lsp.functions.InfoFunctionValueImpl;

public class AverageTimeInfo extends Info{

	private InfoFunction function;
	private String name = "averageTime";
		
	public AverageTimeInfo() {
		function = new AverageTimeInfoFunction();
	}	
	
	@Override
	public String getName() {
		return name;
	}

	@Override
	public InfoFunction getFunction() {
		return function;
	}

	@Override
	public double getFromTime() {
		return 0;
	}

	@Override
	public double getToTime() {
		return Double.MAX_VALUE;
	}

	@Override
	public void update() {
		Info  preInfo = predecessorInfos.iterator().next();
		AverageTimeInfo avgInfo = (AverageTimeInfo)preInfo;
		InfoFunctionValue<?> infoVal  = avgInfo.getFunction().getValues().iterator().next();
		if( infoVal.getValue() instanceof Double) {
			if(function.getValues().iterator().next() instanceof AverageTimeInfoFunctionValue) {
				AverageTimeInfoFunctionValue avgVal = (AverageTimeInfoFunctionValue) function.getValues().iterator().next();
				avgVal.setValue((Double)infoVal.getValue());
			}
		}	
	}

	@Override
	public void setName(String name) {
		this.name = name;
	}
}
