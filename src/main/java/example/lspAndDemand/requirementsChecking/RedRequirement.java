package example.lspAndDemand.requirementsChecking;

import lsp.LSPInfo;
import lsp.LogisticsSolution;
import lsp.shipment.Requirement;

/*package-private*/ class RedRequirement implements Requirement{

	@Override
	public boolean checkRequirement(LogisticsSolution solution) {
		for(LSPInfo info : solution.getInfos()) {
			if(info instanceof RedInfo) {
				return true;
			}
		}	
		return false;
	}

}
