package playground.muelleki.smalltasks;

public class Copier extends RunnableCallable {
	private final int BUFFER_SIZE;
	private final int BUFFER_COUNT;
	protected static final int DATA_SIZE = 1 << 26;

	static final byte[] data = new byte[DATA_SIZE];

	int[] fromto;
	int i = 0;

	public Copier(final int nSubTasks, final int maxInvocations) {
		this.BUFFER_SIZE = nSubTasks;
		this.BUFFER_COUNT = DATA_SIZE / nSubTasks;
		fromto = new int[maxInvocations * 2];

		for (int i = 0; i < maxInvocations * 2; i++)
			fromto[i] = (int) (Math.random() * BUFFER_COUNT);
	}

	@Override
	public void run() {
		System.arraycopy(data, fromto[i++] * BUFFER_SIZE, data, fromto[i++] * BUFFER_SIZE, BUFFER_SIZE);
	}
}

