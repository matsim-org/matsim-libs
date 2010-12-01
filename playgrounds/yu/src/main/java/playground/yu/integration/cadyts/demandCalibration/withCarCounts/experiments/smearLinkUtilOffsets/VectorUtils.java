/**
 * 
 */
package playground.yu.integration.cadyts.demandCalibration.withCarCounts.experiments.smearLinkUtilOffsets;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import no.uib.cipr.matrix.Vector;
import no.uib.cipr.matrix.VectorEntry;

import org.apache.log4j.Logger;

import playground.yu.utils.io.SimpleReader;
import playground.yu.utils.io.SimpleWriter;

/**
 * @author yu
 * 
 */
public class VectorUtils {
	private static String VECTOR_UTILS = VectorUtils.class.getName();

	public static void writeVector(Vector vector, String outputFilename) {
		Logger.getLogger(VECTOR_UTILS).info(
				"write Vector into\t" + outputFilename + "\tBEGAN!");
		SimpleWriter writer = new SimpleWriter(outputFilename);
		writer.writeln("Index\tvalue");
		for (Iterator<VectorEntry> it = vector.iterator(); it.hasNext();) {
			writer.writeln(VectorEntryToString(it.next()));
			writer.flush();
		}
		writer.close();
		Logger.getLogger(VECTOR_UTILS).info(
				"write Vector into\t" + outputFilename + "\tENDED!");
	}

	public static String VectorEntryToString(VectorEntry entry) {
		StringBuffer sb = new StringBuffer(Integer.toString(entry.index()));
		sb.append('\t');
		sb.append(entry.get());
		return sb.toString();
	}

	public static Map<Integer, Double> readVector(String outputFilename) {
		Logger.getLogger(VECTOR_UTILS).info(
				"read Vector into\t" + outputFilename + "\tBEGAN!");

		Map<Integer/* index */, Double/* value */> result = new HashMap<Integer, Double>();

		SimpleReader reader = new SimpleReader(outputFilename);
		String line = reader.readLine();// Index\tvalue
		line = reader.readLine();

		while (line != null) {
			String[] strs = line.split("\t");
			if (strs.length == 2) {
				result.put(Integer.parseInt(strs[0]), Double
						.parseDouble(strs[1]));
			}
			line = reader.readLine();
		}

		reader.close();

		Logger.getLogger(VECTOR_UTILS).info(
				"read Vector into\t" + outputFilename + "\tENDED!");
		return result;
	}
}
