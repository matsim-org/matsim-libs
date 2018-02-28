package demand.decoratedLSP;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;

import lsp.LSP;



public class LSPsWithOffers {

	private Map<Id<LSP>, LSPWithOffers> lsps = new HashMap<>();

	public LSPsWithOffers(Collection<LSPWithOffers> lsps) {
		makeMap(lsps);
	}

	private void makeMap(Collection<LSPWithOffers> lsps) {
		for (LSPWithOffers c : lsps) {
			this.lsps.put(c.getId(), c);
		}
	}

	public LSPsWithOffers() {

	}

	public Map<Id<LSP>, LSPWithOffers> getLSPs() {
		return lsps;
	}

	public void addLSP(LSPWithOffers lsp) {
		if(!lsps.containsKey(lsp.getId())){
			lsps.put(lsp.getId(), lsp);
		}
	}
}
