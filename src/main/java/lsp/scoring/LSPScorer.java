package lsp.scoring;

import lsp.LSP;

public interface LSPScorer extends Scorer{

	double scoreCurrentPlan(LSP lsp);
	void setLSP(LSP lsp);
}
