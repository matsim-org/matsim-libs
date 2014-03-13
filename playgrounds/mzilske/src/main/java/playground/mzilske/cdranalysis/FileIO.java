package playground.mzilske.cdranalysis;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PipedReader;
import java.io.PipedWriter;
import java.io.PrintWriter;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.matsim.core.utils.io.UncheckedIOException;

public class FileIO {

	public static void readFromFile(String string, Reading si) {
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(string));
			si.read(br);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		} finally {
			try {
				if (br != null) br.close();
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}
	}

	public static void readFromInput(BufferedReader br, Reading si) {
		try {
			si.read(br);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		} finally {
			try {
				if (br != null) br.close();
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}
	}

	public static void writeToFile(String string, StreamingOutput so) {
		PrintWriter pw = null;
		try {
			pw = new PrintWriter(new BufferedWriter(new FileWriter(string)));
			so.write(pw);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		} finally {
			pw.close();
		}
	}

	public static void readFromResponse(final StreamingOutput response, Reading si) {
		BufferedReader br = null;
		PrintWriter bw = null;
		final ExecutorService executor = Executors.newSingleThreadExecutor();
		try {
			final PipedReader in = new PipedReader();
			final PipedWriter out = new PipedWriter(in);
			bw = new PrintWriter(out);
			final Future<Void> future = executor.submit(writeResponseTo(response, bw));
			br = new BufferedReader(in);
			si.read(br);
			br.close();
			future.get();
			executor.shutdown();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		} catch (ExecutionException e) {
			throw new RuntimeException(e);
		} finally {
			try {
				if (br != null) br.close();
				if (bw != null) bw.close();
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}
	}

	private static Callable<Void> writeResponseTo(final StreamingOutput response, final PrintWriter bw) {
		return new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				response.write(bw);
				bw.close();
				return null;
			}
		};
	}

}
