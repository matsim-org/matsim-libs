package demand.decoratedLSP;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;

import lsp.LSP;



public class LSPDecorators {

	private Map<Id<LSP>, LSPDecorator> lsps = new HashMap<>();

	public LSPDecorators(Collection<LSPDecorator> lsps) {
		makeMap(lsps);
	}

	private void makeMap(Collection<LSPDecorator> lsps) {
		for (LSPDecorator c : lsps) {
			this.lsps.put(c.getId(), c);
		}
	}

	public LSPDecorators() {

	}

	public Map<Id<LSP>, LSPDecorator> getLSPs() {
		return lsps;
	}

	public void addLSP(LSPDecorator lsp) {
		if(!lsps.containsKey(lsp.getId())){
			lsps.put(lsp.getId(), lsp);
		}
	}
}
