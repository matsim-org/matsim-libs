package playground.mzilske.bvg09;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;

public class StreamingVisumNetworkReader {

	private Map<String, VisumNetworkRowHandler> rowHandlers = new HashMap<String, VisumNetworkRowHandler>();

	public void addRowHandler(String string, VisumNetworkRowHandler nodeRowHandler) {
		rowHandlers.put(string, nodeRowHandler);
	}

	public void read(String inVisumNetFile) {
		TabularFileParserConfig tabFileParserConfig = new TabularFileParserConfig();
		tabFileParserConfig.setFileName(inVisumNetFile);
		tabFileParserConfig.setDelimiterRegex("\\Q;\\E");
		try {
			new TabularFileParser().parse(tabFileParserConfig, new TabularFileHandler() {

				int nRow = 1;
				private VisumNetworkRowHandler currentTable = null;
				private Map<String, String> currentMap = new LinkedHashMap<String, String>();
				
				@Override
				public void startRow(String[] row) {
					if (nRow == 1) {
						if (!"$VISION".equals(row[0])) {
							throw new RuntimeException(row[0]);
						}
					} else if (row[0].isEmpty()) {
						// Line feed
					} else if (row[0].charAt(0) == '*') {
						// Comment
					} else if (row[0].charAt(0) == '$') {
						String tableName = row[0].substring(1, row[0].indexOf(':'));
						row[0] = row[0].substring(row[0].indexOf(':') + 1, row[0].length());
						currentTable = rowHandlers.get(tableName);
						currentMap.clear();
						for (String columnKey : row) {
							currentMap.put(columnKey, null);
						}
					} else if (currentTable != null) {
						int col = 0;
						for (Map.Entry<String, String> entry : currentMap.entrySet()) {
							if (col < row.length) {
								entry.setValue(row[col++]);
							} else {
								entry.setValue("");
							}
						}
						currentTable.handleRow(currentMap);
					}
					
					nRow++;
				}
				
			});
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

}
