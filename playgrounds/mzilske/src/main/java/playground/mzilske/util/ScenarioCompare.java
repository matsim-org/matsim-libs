package playground.mzilske.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.utils.io.UncheckedIOException;

public class ScenarioCompare {

	public static boolean equalPopulation(final Scenario s1, final Scenario s2) {
		try {
			InputStream inputStream1 = null;
			InputStream inputStream2 = null;
			try {
				inputStream1 = openPopulationInputStream(s1);
				inputStream2 = openPopulationInputStream(s2);
				return isEqual(inputStream1, inputStream2);
			} finally {
				if (inputStream1 != null) inputStream1.close();
				if (inputStream2 != null) inputStream2.close();
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/**
	 * The InputStream which comes from this method must be properly
	 * resource-managed, i.e. always be closed.
	 * 
	 * Otherwise, the Thread which is opened here may stay alive.
	 */
	private static InputStream openPopulationInputStream(final Scenario s1) {
		try {
			final PipedInputStream in = new PipedInputStream();
			final PipedOutputStream out = new PipedOutputStream(in);
			new Thread(new Runnable() {
				// Thread will terminate with an IOException when pipe is closed from the other side (like "broken pipe" in UNIX)
				// This is normal.
				public void run() {
					final PopulationWriter writer = new PopulationWriter(s1.getPopulation(), s1.getNetwork());
					writer.write(out);
				}
			}).start();
			return in;
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	// http://stackoverflow.com/questions/4245863/fast-way-to-compare-inputstreams
	private static boolean isEqual(InputStream i1, InputStream i2)
			throws IOException {

		ReadableByteChannel ch1 = Channels.newChannel(i1);
		ReadableByteChannel ch2 = Channels.newChannel(i2);

		ByteBuffer buf1 = ByteBuffer.allocateDirect(1024);
		ByteBuffer buf2 = ByteBuffer.allocateDirect(1024);

		try {
			while (true) {

				int n1 = ch1.read(buf1);
				int n2 = ch2.read(buf2);

				if (n1 == -1 || n2 == -1) return n1 == n2;

				buf1.flip();
				buf2.flip();

				for (int i = 0; i < Math.min(n1, n2); i++)
					if (buf1.get() != buf2.get())
						return false;

				buf1.compact();
				buf2.compact();
			}
		} finally {
			if (ch1 != null) ch1.close();
			if (ch2 != null) ch2.close();
		}
	}


}
