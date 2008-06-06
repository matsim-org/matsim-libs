package playground.david.otfivs.executables;

import org.matsim.gbl.Gbl;
import org.matsim.utils.vis.otfvis.opengl.OnTheFlyClientQuad;





public class OnTheFlyClientFileTveh {

	public static void main(String[] args) {
		String netFileName = "../studies/schweiz/2network/ch.xml"; 
//		String vehFileName = "../runs/run168/run168.it210.T.veh.gz"; 
		String vehFileName = "../runs/run301/output/100.T.veh.gz"; 

		if (Gbl.getConfig() == null) Gbl.createConfig(null);

		String localDtdBase = "../matsimJ/dtd/";
		Gbl.getConfig().global().setLocalDtdBase(localDtdBase);
		
		OnTheFlyClientQuad client = new OnTheFlyClientQuad("tveh:"+vehFileName + "@" + netFileName);
		client.run();
	}

 
}
