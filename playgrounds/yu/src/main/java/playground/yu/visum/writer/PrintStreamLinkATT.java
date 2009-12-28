/**
 * 
 */
package playground.yu.visum.writer;

import java.io.IOException;
import java.util.ConcurrentModificationException;
import java.util.List;

import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkLayer;

import playground.yu.visum.filter.finalFilters.FinalEventFilterA;

/**
 * @author ychen
 * 
 */
public class PrintStreamLinkATT extends PrintStreamATTA {
	/*------------------------MEMBER VARIABLE-----------------*/
	private final static String tableName = "Strecken";

	/*------------------------CONSTRUCTOR------------------*/
	public PrintStreamLinkATT(String fileName, NetworkLayer network) {
		super(fileName, network);
		try {
			out.writeBytes(tableName + "\n$LINK:NO;FROMNODENO;TONODENO");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void printRow(String linkID) throws IOException {
		try {
			LinkImpl link = network.getLink(linkID);
			if (link == null)
				return;
			out.writeBytes(link.getOrigId() + SPRT + link.getFromNode().getId()
					+ SPRT + link.getToNode().getId());
			int i = 0;
			List<Double> udawList = udaws.get(linkID);
			for (Double udaw : udawList) {
				DoubleDF.applyPattern(udas.get(i).getPattern());
				if (udaw == null)
					udaw = 0.0;
				out.writeBytes(SPRT + DoubleDF.format(udaw));
				i++;
			}
			out.writeBytes("\n");
		} catch (ConcurrentModificationException cme) {
			System.err.println(cme);
			System.err.println("in printRow(int)");
		}
	}

	@Override
	public void output(FinalEventFilterA fef) {
		udaws = fef.UDAWexport();
		udas = fef.UDAexport();
		try {
			printTable();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}