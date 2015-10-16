package gunnar.ihop2.utils;

import java.util.LinkedHashMap;
import java.util.Map;

import floetteroed.utilities.tabularfileparser.TabularFileHandler;

/**
 * TODO move this to tabular file parser utilities
 * 
 * @author Gunnar Flötteröd
 *
 */
public abstract class AbstractTabularFileHandler implements TabularFileHandler {

	protected final Map<String, Integer> label2index = new LinkedHashMap<String, Integer>();

	private boolean parsedFirstRow = false;

	@Override
	public final void startRow(String[] row) {
		if (!this.parsedFirstRow) {
			for (int i = 0; i < row.length; i++) {
				this.label2index.put(this.preprocessColumnLabel(row[i]), i);
			}
			this.parsedFirstRow = true;
		} else {
			this.startDataRow(row);
		}
	}

	@Override
	public void startDocument() {
	}

	@Override
	public String preprocess(final String line) {
		return line;
	}

	@Override
	public void endDocument() {
	}

	protected String preprocessColumnLabel(final String label) {
		return label;
	}

	protected int index(final String label) {
		return this.label2index.get(label);
	}

	public void startDataRow(final String[] row) {
	}

}
