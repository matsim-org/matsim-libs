package playground.muelleki.smalltasks;

import java.util.Random;

public class RandomGenerating extends RunnableCallable {
	static final ThreadLocal<Random> R = new ThreadLocal<Random>() {
		@Override
		protected synchronized Random initialValue() {
			return new Random();
		}
	};
	
	private final int nSubTasks;

	public RandomGenerating(int nSubTasks) {
		this.nSubTasks = nSubTasks;
	}

	@Override
	public void run() {
		Random r = R.get();
		for (int j = 0; j < nSubTasks; j++)
			r.nextDouble();
	}
}

