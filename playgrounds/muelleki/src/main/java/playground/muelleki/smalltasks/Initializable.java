package playground.muelleki.smalltasks;

public interface Initializable extends Runnable {

	public abstract void run();

	public abstract void init();

	public abstract void shutdown();
}
