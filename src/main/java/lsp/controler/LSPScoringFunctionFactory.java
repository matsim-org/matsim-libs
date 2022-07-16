package lsp.controler;

import lsp.LSP;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.core.scoring.ScoringFunction;

public interface LSPScoringFunctionFactory{
	ScoringFunction createScoringFunction( LSP lsp );
}
