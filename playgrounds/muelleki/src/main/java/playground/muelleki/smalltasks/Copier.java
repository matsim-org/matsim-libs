package playground.muelleki.smalltasks;

import java.util.ArrayList;
import java.util.Collections;

public class Copier extends RunnableCallable {
	private final int BUFFER_SIZE;
	private final int BUFFER_COUNT;
	protected static final int DATA_SIZE = 1 << 28;

	static final byte[] data = new byte[DATA_SIZE];

	int[] from;
	int[] to;
	int i = 0;

	public Copier(final int nSubTasks) {
		this.BUFFER_SIZE = nSubTasks * 100;
		this.BUFFER_COUNT = DATA_SIZE / BUFFER_SIZE;
		
		from = createShuffledList();
		to = createShuffledList();
	}

	private int[] createShuffledList() {
		ArrayList<Integer> arrayList = new ArrayList<Integer>(BUFFER_COUNT);
		for (int i = 0; i < BUFFER_COUNT; i++)
			arrayList.add(i);
		Collections.shuffle(arrayList);
		int[] array = new int[BUFFER_COUNT];
		for (int i = 0; i < BUFFER_COUNT; i++)
			array[i] = arrayList.get(i);
		return array;
	}

	@Override
	public void run() {
		System.arraycopy(data, from[i] * BUFFER_SIZE, data, to[i] * BUFFER_SIZE, BUFFER_SIZE);
		i = (i + 1) % from.length;
	}
}

