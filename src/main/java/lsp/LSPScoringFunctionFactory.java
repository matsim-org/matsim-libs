package lsp;

import lsp.LSP;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.core.scoring.ScoringFunction;

public interface LSPScoringFunctionFactory{
	LSPScorer createScoringFunction( LSP lsp );
}
