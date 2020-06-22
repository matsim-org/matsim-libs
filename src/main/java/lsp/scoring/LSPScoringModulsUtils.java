package lsp.scoring;

import lsp.LSPs;

public class LSPScoringModulsUtils {
	public static LSPScoringModuleImpl createDefaultLSPScoringModule(LSPs lsps) {
		return new LSPScoringModuleImpl(lsps);
	}
}
