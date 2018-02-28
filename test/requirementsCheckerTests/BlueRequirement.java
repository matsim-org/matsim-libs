package requirementsCheckerTests;

import lsp.functions.Info;
import lsp.LogisticsSolution;
import lsp.shipment.Requirement;

public class BlueRequirement implements Requirement{

	@Override
	public boolean checkRequirement(LogisticsSolution solution) {
		for(Info info : solution.getInfos()) {
			if(info instanceof BlueInfo) {
				return true;
			}
		}	
		return false;
	}

}
