/**
 *
 */
package playground.yu.visum.writer;

import java.io.DataOutputStream;
import java.io.IOException;

import playground.yu.visum.filter.finalFilters.FinalEventFilterA;

/**
 * @author ychen
 * 
 */
public abstract class PrintStreamVisum9_3A implements PrintStreamVisum9_3I {
	/**
	 * out - an underly static DataOutputStream
	 */
	protected DataOutputStream out;

	public abstract void output(FinalEventFilterA fef);

	/**
	 * @Specified by: close in interface Closeable
	 * @throws IOException -
	 *             if an I/O error occurs.
	 */
	public void close() throws IOException {
		this.out.close();
	}
}
