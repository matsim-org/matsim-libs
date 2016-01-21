package floetteroed.utilities.tabularfileparser;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public abstract class AbstractTabularFileHandlerWithHeaderLine implements
		TabularFileHandler {

	// TODO make private
	protected final Map<String, Integer> label2index = new LinkedHashMap<String, Integer>();

	private boolean parsedFirstRow = false;

	protected String getStringValue(final String label) {
		return this.currentRow[this.label2index.get(label)];
	}

	protected Integer getIntValue(final String label) {
		try {
			return Integer
					.parseInt(this.currentRow[this.label2index.get(label)]);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	protected Double getDoubleValue(final String label) {
		try {
			return Double.parseDouble(this.currentRow[this.label2index
					.get(label)]);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	// TODO new and not systematically tested
	private String[] currentRow = null;

	@Override
	public final void startRow(String[] row) {
		this.currentRow = row;
		if (!this.parsedFirstRow) {
			for (int i = 0; i < row.length; i++) {
				this.label2index.put(this.preprocessColumnLabel(row[i]), i);
			}
			this.parsedFirstRow = true;
		} else {
			this.startDataRow(row);
			this.startCurrentDataRow();
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

	// TODO NEW
	public void startCurrentDataRow() {
	}

}
