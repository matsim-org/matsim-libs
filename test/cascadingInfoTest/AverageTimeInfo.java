package cascadingInfoTest;

import lsp.functions.Info;
import lsp.functions.InfoFunction;

public class AverageTimeInfo extends Info{

	private AverageTimeInfoFunction function;
	
	public AverageTimeInfo() {
		function = new AverageTimeInfoFunction();
	}
	
	
	@Override
	public String getName() {
		return "averageTime";
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
		AverageTimeInfoFunctionValue avgVal  = (AverageTimeInfoFunctionValue) avgInfo.getFunction().getValues().iterator().next();
		function.getValues().iterator().next().setValue(avgVal.getValue()); 
	}

}
