package lsp;

import java.util.Collection;

import org.matsim.api.core.v01.population.BasicPlan;

public interface LSPPlan extends BasicPlan{

	LSPPlan addSolution( LogisticsSolution solution );
	
	Collection<LogisticsSolution> getSolutions();

	ShipmentAssigner getAssigner();

	LSPPlan setAssigner( ShipmentAssigner assigner );

	LSPPlan setLSP( LSP lsp );
	
	LSP getLsp();

}
