package lsp;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;

public class LSPs {
	
	private Map<Id<LSP>, LSP> lsps = new HashMap<>();

	public LSPs(Collection<LSP> lsps) {
		makeMap(lsps);
	}

	private void makeMap(Collection<LSP> lsps) {
		for (LSP c : lsps) {
			this.lsps.put(c.getId(), c);
		}
	}

	public LSPs() {

	}

	public Map<Id<LSP>, LSP> getLSPs() {
		return lsps;
	}

	public void addLSP(LSP lsp) {
		if(!lsps.containsKey(lsp.getId())){
			lsps.put(lsp.getId(), lsp);
		}
	}
}
