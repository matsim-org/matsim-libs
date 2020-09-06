package lsp.scoring;

import lsp.LSPs;

public class LSPScoringUtils{
	public static LSPScoringModuleImpl createDefaultLSPScoringModule(LSPs lsps) {
		return new LSPScoringModuleImpl(lsps);
	}
}
