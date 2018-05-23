package lsp.scoring;

import lsp.LSP;

public interface LSPScorer extends Scorer{

	public double scoreCurrentPlan(LSP lsp);
	public void setLSP (LSP lsp);
}
