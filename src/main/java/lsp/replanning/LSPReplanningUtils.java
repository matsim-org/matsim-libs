package lsp.replanning;

import lsp.LSP;
import lsp.LSPs;

public class LSPReplanningUtils {
	public static LSPReplannerImpl createDefaultLSPReplanner(LSP lsp) {
		return new LSPReplannerImpl(lsp);
	}

	public static LSPReplanningModuleImpl createDefaultLSPReplanningModule(LSPs lsps) {
		return new LSPReplanningModuleImpl(lsps);
	}
}
