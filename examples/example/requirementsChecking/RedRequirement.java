package example.requirementsChecking;

import lsp.functions.Info;
import lsp.LogisticsSolution;
import lsp.shipment.Requirement;

public class RedRequirement implements Requirement{

	@Override
	public boolean checkRequirement(LogisticsSolution solution) {
		for(Info info : solution.getInfos()) {
			if(info instanceof RedInfo) {
				return true;
			}
		}	
		return false;
	}

}
