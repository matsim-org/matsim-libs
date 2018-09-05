package lsp.shipment;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.matsim.api.core.v01.Id;


public class LSPShipments {

	private Map<Id<LSPShipment>, LSPShipment> lspShipments; 

	public LSPShipments(Collection<LSPShipment> lspShipments) {
		makeMap(lspShipments);
	}

	private void makeMap(Collection<LSPShipment> lspShipments) {
		for (LSPShipment l : lspShipments) {
			this.lspShipments.put(l.getId(), l);
		}
	}

	public LSPShipments() {
		this.lspShipments = new HashMap<>();
	}

	public  Map<Id<LSPShipment>, LSPShipment> getShipments() {
		return lspShipments;
	}

	public void addShipment(LSPShipment lspShipment) {
		if(!lspShipments.containsKey(lspShipment.getId())){
			lspShipments.put(lspShipment.getId(), lspShipment);
		}
		else {
			
		}
	}
}
