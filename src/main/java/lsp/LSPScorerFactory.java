package lsp;

import org.matsim.core.api.internal.MatsimFactory;

public interface LSPScorerFactory extends MatsimFactory {
	LSPScorer createScoringFunction( LSP lsp );
}
