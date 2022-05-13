package lsp.scoring;

import lsp.LSP;

public interface LSPScorer {

	double scoreCurrentPlan(LSP lsp);
	void setLSP(LSP lsp);
}
