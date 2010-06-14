/**
 * 
 */
package playground.benjamin.dataprepare;

import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;

abstract class CheckingTabularFileHandler implements TabularFileHandler {
	
	boolean first = true;
	
	int numColumns = -1;
	
	protected void check(String[] row) {
		if (first) {
			numColumns = row.length;
			System.out.println("Header: ");
			for (String entry : row) {
				System.out.print(entry);
				System.out.print(" ");
			}
			System.out.println();
		}
		if (numColumns != row.length) {
			throw new RuntimeException("Parse error. Expected: "+numColumns+" Got: "+row.length);
		}
	}
	
}