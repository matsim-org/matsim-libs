package playground.gregor.scenariogen.hhw3hybrid;

import java.io.IOException;
import java.util.Collection;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;

import playground.gregor.grpc.dummyjupedsim.JuPedSimServer;
import playground.gregor.sim2d_v4.scenario.Sim2DConfig;
import playground.gregor.sim2d_v4.scenario.Sim2DConfigUtils;
import playground.gregor.sim2d_v4.scenario.Sim2DEnvironment;
import playground.gregor.sim2d_v4.scenario.Sim2DScenario;
import playground.gregor.sim2d_v4.scenario.Sim2DScenarioUtils;

public class JuPedSimGeomtryConverter {

	private Sim2DScenario sim2dsc;

	public JuPedSimGeomtryConverter(Sim2DScenario sim2dsc) {
		this.sim2dsc = sim2dsc;
	}

	
	private JuPedSimGeomtry buildSaveAndReturnJuPedSimGeomtry() {
		Collection<Sim2DEnvironment> e = this.sim2dsc.getSim2DEnvironments();
		for (Sim2DEnvironment env : e) {
			JuPedSimGeomtry geo = new JuPedSimGeomtry();
			geo.buildFrom2DEnv(env);
			String jupedsimGeoFile = "/Users/laemmel/arbeit/papers/2015/trgindia2015/hhwsim/input/jps_geo.xml";
			String goalFile = "/Users/laemmel/arbeit/papers/2015/trgindia2015/hhwsim/input/goals.xml";
			try {
				new JuPedSimGeomtrySerializer(jupedsimGeoFile, geo,goalFile).serialize();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			return geo;
		}
		return null;
	
	}
	
	public static void main(String [] args) {
		String inputDir = "/Users/laemmel/devel/hhw_hybrid/input";
		String s2dConfigFile = inputDir + "/s2d_config_v0.3.xml";
		String shapeFile = "/Users/laemmel/arbeit/papers/2015/trgindia2015/envplot/jupedsim.shp";

		Sim2DConfig sim2dc = Sim2DConfigUtils.loadConfig(s2dConfigFile);
		Sim2DScenario sim2dsc = Sim2DScenarioUtils.loadSim2DScenario(sim2dc);
		JuPedSimGeomtry geo =  new JuPedSimGeomtryConverter(sim2dsc).buildSaveAndReturnJuPedSimGeomtry();
		new ConvertToEsriShape(sim2dsc.getSim2DEnvironments().iterator().next().getCRS(), geo, shapeFile).run();;
		
		new MATSimScenarioCreator(geo).run();
		
		
	}


}
