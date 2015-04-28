package playground.muelleki.smalltasks;

import com.google.inject.Provider;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.qsim.qnetsimengine.MyQSimFactory;

public class SmallTasksControler extends Controler {
	public SmallTasksControler(String[] args) {
		super(args);
		this.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bindMobsim().toProvider(MyQSimFactory.class);
			}
		});
	}
	
	public static void main(String[] args) {
		final SmallTasksControler controler = new SmallTasksControler(args);
		controler.run();
		System.exit(0);
	}
}
