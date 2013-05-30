/**
 * 
 */
package playground.qiuhan.sa;

import java.util.Map;

import playground.yu.utils.io.SimpleWriter;

/**
 * extracts the "bezirk" information from VISUM network, e.g. coordinates, type
 * number
 * 
 * @author Q. SUN
 * 
 */
public class VisumZonesRowHandler implements VisumNetworkRowHandler {
	// private NetworkImpl network;
	private SimpleWriter writer;

	public VisumZonesRowHandler(
	// NetworkImpl network,
			String outputFilename) {
		// this.network = network;
		this.writer = new SimpleWriter(outputFilename);
		this.writer.writeln("BezirkID\tx\ty\ttype_number");
	}

	@Override
	public void handleRow(Map<String, String> row) {
		StringBuffer sb = new StringBuffer();
		// bezirk ID
		sb.append(row.get("NR"));
		sb.append("\t");

		// bezirk beziehungspunkt coordinates
		String xStr = row.get("XKOORD"), yStr = row.get("YKOORD");
		if (xStr == null || yStr == null) {
			return;
		}
		double x = Double.parseDouble(xStr.replace(',', '.')), y = Double
				.parseDouble(yStr.replace(',', '.'));
		sb.append(x);
		sb.append("\t");
		sb.append(y);
		sb.append("\t");

		// type number
		sb.append(row.get("TYPNR"));
		writer.writeln(sb);
		writer.flush();
	}

	public void finish() {
		this.writer.close();
	}
}
