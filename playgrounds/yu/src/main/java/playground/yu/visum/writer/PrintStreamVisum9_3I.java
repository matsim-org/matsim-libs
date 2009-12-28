/**
 * 
 */
package playground.yu.visum.writer;

import java.io.Closeable;

import playground.yu.visum.filter.finalFilters.FinalEventFilterA;

/**
 * @author ychen A PrintStreamVisum9_3I is a source or destination of data that
 *         can be printed,implements the interface Closeable and offers
 *         output()-function for every writer-class in the package. and
 */
public interface PrintStreamVisum9_3I extends Closeable {
	/**
	 * Extracts the last Filter and save the usefull information to print
	 * 
	 * @param fef -
	 *            the last Filter to extract
	 */
	public void output(FinalEventFilterA fef);
}
