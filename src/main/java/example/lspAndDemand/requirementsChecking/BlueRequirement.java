package example.lspAndDemand.requirementsChecking;

import lsp.LSPInfo;
import lsp.LogisticsSolution;
import lsp.shipment.Requirement;

/*package-private*/ class BlueRequirement implements Requirement{

	@Override
	public boolean checkRequirement(LogisticsSolution solution) {
		for(LSPInfo info : solution.getInfos()) {
			if(info instanceof BlueInfo) {
				return true;
			}
		}	
		return false;
	}

}
