package example.lsp.multipleChains;

import lsp.*;
import lsp.usecase.TransshipmentHub;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A scorer for the LSP.
 * It uses the scores of the
 * - carriers: Take the carrier's score and add it to the LSP's score
 * - hubs: currently a very simple fixed costs scoring (see below {@link #scoreHub()})
 *
 * @author Kai Martins-Turner (kturner)
 */
/*package-private*/ class MyLSPScorer implements LSPScorer {
	private double score = 0;

	final Logger logger = LogManager.getLogger(MyLSPScorer.class);
	private LSP lsp;

	@Override
	public void reset(int iteration) {
		score = 0.;
	}

	@Override
	public double getScoreForCurrentPlan() {
		scoreLspCarriers();
		scoreHub();
		scoreMissingShipments();
		return score;
	}

	private void scoreLspCarriers() {
		var lspPlan = lsp.getSelectedPlan();
		for (LogisticChain logisticChain : lspPlan.getLogisticChains()) {
			for (LogisticChainElement logisticChainElement : logisticChain.getLogisticChainElements()) {
				if (logisticChainElement.getResource() instanceof LSPCarrierResource carrierResource) {
					var carriersScore = carrierResource.getCarrier().getSelectedPlan().getScore();
					if (carriersScore != null) {
						score = score + carriersScore;
					}
				}
			}
		}
	}

	/**
	 * If a hub resource is in the selected plan of the LSP, it will get scored.
	 * <p>
	 * This is somehow a quickfix, because the hubs do **not** have any own events yet.
	 * This needs to be implemented later
	 * KMT oct'22
	 */
	private void scoreHub() {
		var lspPlan = lsp.getSelectedPlan();
		for (LogisticChain logisticChain : lspPlan.getLogisticChains()) {
			for (LogisticChainElement logisticChainElement : logisticChain.getLogisticChainElements()) {
				if (logisticChainElement.getResource() instanceof TransshipmentHub hub){
					score = score - LSPUtils.getFixedCost(hub);
				}
			}
		}
	}

	private void scoreMissingShipments() {
		LSPPlan lspPlan = lsp.getSelectedPlan();
		int lspPlanShipmentCount = lspPlan.getLogisticChains()
				.stream()
				.mapToInt(logisticChain -> logisticChain.getShipmentIds().size())
				.sum();
		if (lspPlanShipmentCount !=  lsp.getShipments().size()) {
			logger.error("LspPlan doesn't contain the same number of shipments as LSP, " +
					"shipments probably lost during replanning.");
			score -= 10000;
		}
	}

	@Override
	public void setEmbeddingContainer(LSP pointer) {
		this.lsp = pointer;
	}

}
