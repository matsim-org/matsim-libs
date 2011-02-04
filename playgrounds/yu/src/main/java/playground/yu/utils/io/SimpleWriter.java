/**
 * 
 */
package playground.yu.utils.io;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.Flushable;
import java.io.IOException;

import org.matsim.core.utils.io.IOUtils;

/**
 * a small and simple writer
 * 
 * @author yu
 * 
 */
public class SimpleWriter implements Closeable, Flushable {
	private BufferedWriter writer = null;
	private static String intermission;

	public static void setIntermission(String intermission) {
		SimpleWriter.intermission = intermission;
	}

	public static void appendIntermission(StringBuffer stringBuffer) {
		stringBuffer.append(intermission);
	}

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

	public SimpleWriter(String outputFilename, String contents2write) {
		try {
			writer = IOUtils.getBufferedWriter(outputFilename);
			write(contents2write);
			close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void write(char[] c) {
		if (writer != null) {
			try {
				writer.write(c);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public void write(char c) {
		if (writer != null) {
			try {
				writer.write(c);
			} catch (IOException e) {
				e.printStackTrace();
			}
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

	public void write(Object o) {
		write(o.toString());
	}

	public void writeln(String s) {
		write(s + "\n");
	}

	public void writeln(Object o) {
		write(o + "\n");
	}

	public void writeln() {
		write('\n');
	}

	public void close() {
		try {
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void flush() {
		try {
			writer.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void writeln(StringBuffer line) {
		writeln(line.toString());
	}

}
