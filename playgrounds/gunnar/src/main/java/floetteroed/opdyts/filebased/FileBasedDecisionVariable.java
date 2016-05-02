package floetteroed.opdyts.filebased;

import java.io.FileNotFoundException;
import java.io.PrintWriter;

import floetteroed.opdyts.DecisionVariable;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class FileBasedDecisionVariable implements DecisionVariable {

	// -------------------- CONSTANTS --------------------

	private final String decisionVariableId;

	private final String newDecisionVariableFileName;

	// -------------------- CONSTRUCTION --------------------

	public FileBasedDecisionVariable(final String decisionVariableId,
			final String decisionVariableFileName) {
		this.decisionVariableId = decisionVariableId;
		this.newDecisionVariableFileName = decisionVariableFileName;
	}

	// --------------- IMPLEMENTATION OF DecisionVariable ---------------

	@Override
	public void implementInSimulation() {
		try {
			final PrintWriter writer = new PrintWriter(
					this.newDecisionVariableFileName);
			writer.println(this.decisionVariableId);
			writer.flush();
			writer.close();
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
	}
}
