package lsp;

import org.matsim.contrib.freight.carrier.Carrier;

public interface LSPCarrierResource extends LSPResource {

	Carrier getCarrier();
	
}
