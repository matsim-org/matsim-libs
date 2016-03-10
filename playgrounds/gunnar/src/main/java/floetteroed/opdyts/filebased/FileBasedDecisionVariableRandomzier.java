package floetteroed.opdyts.filebased;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import floetteroed.opdyts.DecisionVariableRandomizer;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class FileBasedDecisionVariableRandomzier implements
		DecisionVariableRandomizer<FileBasedDecisionVariable> {

	// -------------------- CONSTANTS --------------------

	private final String createNewDecisionVariablesCommand;

	private final String originalDecisionVariableFileName;

	private final String newDecisionVariablesFileName;

	// -------------------- CONSTRUCTION --------------------

	public FileBasedDecisionVariableRandomzier(
			final String createNewDecisionVariablesCommand,
			final String originalDecisionVariableFileName,
			final String newDecisionVariablesFileName) {
		this.createNewDecisionVariablesCommand = createNewDecisionVariablesCommand;
		this.originalDecisionVariableFileName = originalDecisionVariableFileName;
		this.newDecisionVariablesFileName = newDecisionVariablesFileName;
	}

	// ---------- IMPLEMENTATION OF DecisionVariableRandomizer ----------

	@Override
	public Collection<FileBasedDecisionVariable> newRandomVariations(
			FileBasedDecisionVariable originalDecisionVariable) {

		if (originalDecisionVariable != null) {
			originalDecisionVariable.implementInSimulation();
		} else {
			final File file = new File(this.originalDecisionVariableFileName);
			if (file.exists()) {
				file.delete();
			}
		}

		final Process proc;
		final int exitVal;
		try {
			proc = Runtime.getRuntime().exec(
					this.createNewDecisionVariablesCommand);
			exitVal = proc.waitFor();
			if (exitVal != 0) {
				throw new RuntimeException(
						"Decision variable generation terminated with exit code "
								+ exitVal + ".");
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		final List<FileBasedDecisionVariable> result = new LinkedList<>();
		try {
			String line;
			final BufferedReader reader = new BufferedReader(new FileReader(
					this.newDecisionVariablesFileName));
			while ((line = reader.readLine()) != null) {
				result.add(new FileBasedDecisionVariable(line.trim(),
						this.originalDecisionVariableFileName));
			}
			reader.close();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return result;
	}
}
