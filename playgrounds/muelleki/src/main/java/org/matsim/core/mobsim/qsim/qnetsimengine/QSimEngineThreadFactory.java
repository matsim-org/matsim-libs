package org.matsim.core.mobsim.qsim.qnetsimengine;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

final class QSimEngineThreadFactory implements ThreadFactory {
	static final AtomicInteger i = new AtomicInteger();

	@Override
	public Thread newThread(Runnable r) {
		Thread t = new Thread(r);
		t.setName("QSimEngineThread" + i.incrementAndGet());
		return t;
	}
}