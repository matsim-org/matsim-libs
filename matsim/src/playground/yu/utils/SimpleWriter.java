/**
 * 
 */
package playground.yu.utils;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.Flushable;
import java.io.IOException;

import org.matsim.utils.io.IOUtils;

/**
 * a small and simple writer
 * 
 * @author yu
 * 
 */
public class SimpleWriter implements Closeable, Flushable {
	private BufferedWriter writer = null;

	/**
	 * 
	 */
	public SimpleWriter(final String outputFilename) {
		try {
			writer = IOUtils.getBufferedWriter(outputFilename);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void write(String s) {
		if (writer != null) {
			try {
				writer.write(s);
			} catch (IOException e) {
				System.err.println("writer was not initialized yet!");
				e.printStackTrace();
			}
		}
	}

	public void writeln(String s) {
		write(s + "\n");
	}

	public void close() throws IOException {
		writer.close();
	}

	public void flush() throws IOException {
		writer.flush();
	}
}
