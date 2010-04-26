package playground.mfeil.FilesForTests;

import org.matsim.core.controler.Controler;
import org.matsim.core.gbl.Gbl;
import org.matsim.testcases.MatsimTestCase;

public class Initializer {

	private Controler controler;

	public Initializer() {
	}

	public void init(MatsimTestCase testCase) {
		System.out.println(testCase.getPackageInputDirectory());
		String path = testCase.getPackageInputDirectory() + "config.xml";
		testCase.loadConfig(path);
		this.controler = new ControlerForTests(Gbl.getConfig());
		this.controler.setOverwriteFiles(true);
	}

	public void run (){
		this.controler.run();
	}

	public Controler getControler() {
		return controler;
	}
	public void setControler(Controler controler) {
		this.controler = controler;
	}

}
