package lsp;


/**
 * Was macht das?
 */
public interface SolutionScheduler {

	void scheduleSolutions();

	void setLSP(LSP lsp);
	
	void setBufferTime(int bufferTime);
}
