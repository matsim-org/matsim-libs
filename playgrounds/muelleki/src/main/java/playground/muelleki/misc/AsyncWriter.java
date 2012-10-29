package playground.muelleki.misc;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author muelleki
 * A writer that forwards its input to another writer
 * which processes the data in a worker thread.
 * All worker threads live in a dedicated cached thread pool,
 * and are named accordingly.
 * 
 * This class attemts to be thread-safe, but no tests
 * have been created to support this.
 */
/**
 * @author muelleki
 *
 */
public class AsyncWriter extends Writer {
	/**
	 * @author muelleki
	 * Thread factory that assigns a nice name to all worker threads
	 */
	private static final class AsyncWriterThreadFactory implements
	ThreadFactory {
		/**
		 * Counter of all worker threads 
		 */
		private static AtomicInteger n = new AtomicInteger(1);

		@Override
		public Thread newThread(Runnable r) {
			Thread t = new Thread(r, "AsyncWriter" + n.getAndIncrement());
			assert(!t.isDaemon());
			return t;
		}
	}

	/**
	 * @author muelleki
	 * Runnable for the worker threads. Essentially copies
	 * contents from the blocking queue to the target
	 * writer. 
	 */
	private final class AsyncWriterRunnable implements Runnable {
		private final Writer out;

		private AsyncWriterRunnable(Writer out) {
			this.out = out;
		}

		@Override
		public void run() {
			try {
				doProcessAll();
			} catch(IOException ioe) {
				ioe.printStackTrace();
			}
		}

		private void doProcessAll() throws IOException {
			for (;;) {
				try {
					if (!processOne(q.take()))
						return;
				} catch (InterruptedException e) {
					throw new IOException(e.getMessage());
				}
				ArrayList<char[]> sink = new ArrayList<char[]>(64);
				q.drainTo(sink);
				for (char[] t : sink) {
					if (!processOne(t))
						return;
				}
			}
		}

		private boolean processOne(char[] buf) throws IOException {
			// Check for object equivalence here!
			if (buf == TERMINATOR)
				return false;
			out.write(buf);
			return true;
		}
	}


	// Thread pool that creates and reuses threads on demand
	static ExecutorService e = Executors.newCachedThreadPool(new AsyncWriterThreadFactory());

	// If the encapsulating writer writes in 8kb chunks (default
	// for BufferedWriter), this equals roughly a cache capacity
	// of 256 MB.
	private static final int QUEUE_CAPACITY = 32000;

	// Blocking queue for exchanging data between threads
	private BlockingQueue<char[]> q = new LinkedBlockingQueue<char[]>(QUEUE_CAPACITY);

	// Special value that will be added to the queue to indicate
	// that the worker thread should terminate
	private static final char[] TERMINATOR = new char[0];

	// Becomes available when the worker thread finishes
	private Future<Writer> f;

	/**
	 * @param out The writer to which all input data is copied.
	 *            The processing will occur in another thread.
	 * @throws IOException
	 */
	public AsyncWriter(final Writer out) {
		startWorker(out);
	}

	/* (non-Javadoc)
	 * @see java.io.Writer#write(char[], int, int)
	 */
	@Override
	public void write(char[] cbuf, int off, int len) throws IOException {
		char[] buf = new char[len];
		System.arraycopy(cbuf, off, buf, 0, len);
		try {
			q.put(buf);
		} catch (InterruptedException e) {
			throw new IOException(e.getMessage());
		}
	}

	/* (non-Javadoc)
	 * @see java.io.Writer#flush()
	 */
	@Override
	public void flush() throws IOException {
	}

	/* (non-Javadoc)
	 * @see java.io.Writer#close()
	 * Waits for the worker thread to finish
	 */
	@Override
	public void close() throws IOException {
		Writer out = finishWorker();
		out.close();
	}

	/**
	 * Starts a new worker thread
	 * @param out The writer the worker thread will write to
	 */
	synchronized private void startWorker(final Writer out) {
		if (f != null && !f.isDone())
			return;
		Runnable r = new AsyncWriterRunnable(out);
		f = e.submit(r, out);
	}

	/**
	 * Asks the worker thread to terminate (by adding a special
	 * entry to the queue) and awaits termination. 
	 * @return The target writer that was used by the
	 *         worker thread
	 * @throws IOException
	 */
	synchronized private Writer finishWorker() throws IOException {
		try {
			if (!f.isDone())
				q.put(TERMINATOR);
			return f.get();
		} catch (InterruptedException e) {
			throw new IOException(e.getMessage());
		} catch (ExecutionException e) {
			throw new IOException(e.getMessage());
		}
	}
}
