package org.matsim.core.population.io;

import org.matsim.core.utils.misc.Counter;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class ParallelPopulationWriterV6 implements Runnable {
	private final Counter counter = new Counter("[" + this.getClass().getSimpleName() + "] dumped person # ");
	private BlockingQueue<CompletableFuture<String>> outputQueue;
	private BufferedWriter out;

	private boolean finish = false;


	ParallelPopulationWriterV6(BlockingQueue<CompletableFuture<String>> outputQueue, BufferedWriter out) {
		this.outputQueue = outputQueue;
		this.out = out;
	}

	@Override
	public void run() {
		do {
			try {
				CompletableFuture<String> f = outputQueue.poll();
				if(f!=null)
				{
					out.write(f.get());
					counter.incCounter();
				}
			} catch (InterruptedException | ExecutionException | IOException e) {
				throw new RuntimeException(e);
			}
		} while (!(this.outputQueue.isEmpty() && finish));
	}

	public void finish() {
		this.finish = true;
	}
}
