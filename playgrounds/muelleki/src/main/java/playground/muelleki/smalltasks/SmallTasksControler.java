package playground.muelleki.smalltasks;

import org.matsim.core.controler.Controler;
import org.matsim.core.mobsim.qsim.qnetsimengine.MyQSimFactory;

public class SmallTasksControler extends Controler {
	public SmallTasksControler(String[] args) {
		super(args);
		super.setMobsimFactory(new MyQSimFactory());
	}
	
	public static void main(String[] args) {
		final SmallTasksControler controler = new SmallTasksControler(args);
		controler.run();
		System.exit(0);
	}
}
