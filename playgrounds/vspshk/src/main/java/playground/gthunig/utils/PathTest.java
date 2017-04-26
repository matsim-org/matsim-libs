package playground.gthunig.utils;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

import java.io.File;
import java.net.URL;
import java.util.Arrays;

/**
 * Created by GabrielT on 20.07.2016.
 */
public class PathTest {

	public static void main(String[] args) {
		Config config = ConfigUtils.createConfig();
		String inputDir = "C:/Users/GabrielT/Vsp/matsim/matsim/matsim/matsim/examples/equil/";
		System.out.println(new File(inputDir).exists());

		File file = new File(inputDir);
		try {
			System.out.println(file.getAbsolutePath());
			System.out.println(Arrays.asList(file.list()));
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		String urlString = "../../../matsim/matsim/matsim/matsim/examples/equil/network.xml";
		File urlFile = new File(urlString);
		try {
			URL url = urlFile.toURI().toURL();
			System.out.println(url.toString());
			System.out.println(url.getFile());
		} catch (Exception e) {
			e.printStackTrace();
		}

		config.network().setInputFile(urlString);
		config.plans().setInputFile("../../../matsim/matsim/matsim/matsim/examples/equil/plans2.xml");

//		config.network().setInputFile("file:C:\\Users/GabrielT/Vsp/matsim/matsim/matsim/matsim/examples/equil/network.xml");
//		config.plans().setInputFile("file:C:\\Users/GabrielT/Vsp/matsim/matsim/matsim/matsim/examples/equil/plans2.xml");


		ScenarioUtils.loadScenario(config);
	}
}
