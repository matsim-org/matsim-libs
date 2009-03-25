/**
 * 
 */
package playground.yu.utils.io;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.matsim.core.utils.io.IOUtils;

/**
 * @author yu
 * 
 */
public class SimpleReader implements Closeable {
	private BufferedReader reader = null;

	/**
	 * 
	 */
	public SimpleReader(String inputFilename) {
		try {
			reader = IOUtils.getBufferedReader(inputFilename);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void close() {
		try {
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public String readLine() {
		String line = null;
		try {
			line = reader.readLine();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return line;
	}
}
