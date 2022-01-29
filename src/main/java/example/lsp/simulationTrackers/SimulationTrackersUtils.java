package example.lsp.simulationTrackers;

import lsp.functions.LSPInfoFunction;

public class SimulationTrackersUtils {

	// Mache da shier erstmal aus den Beispielen heraus damit ich überhaupt mal eine CostInfo woanders einbinden kann.
	// Muss dann vermutlich entweder auf eine andere Ebene (raus aus den examples verschoben werden.
	public static CostInfo createDefaultCostInfo(){
		return new CostInfo();
	}

	public static FixedCostFunctionValue getFixedCostFunctionValue (LSPInfoFunction lspInfoFunction){
		throw new RuntimeException("not implemented");
		//		return lspInfoFunction.getValues().;  //TODOSchauen, dass man hier die richtige ewischt
	}
}
