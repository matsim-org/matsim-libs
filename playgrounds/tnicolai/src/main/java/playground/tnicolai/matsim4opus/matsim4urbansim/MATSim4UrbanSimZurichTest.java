package playground.tnicolai.matsim4opus.matsim4urbansim;

import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.NetworkImpl;

import playground.tnicolai.matsim4opus.constants.Constants;

public class MATSim4UrbanSimZurichTest extends MATSim4Urbansim{
	
	// logger
	private static final Logger log = Logger.getLogger(MATSim4UrbanSimZurichTest.class);
	// test case identifier
	private static String uetlibergtunnel = "uetlibergtunnel";
	private static String birmsdorferstrasse = "birmsdorferstrasse";
	private static String schwamendingertunnel = "schwamendingertunnel";
	
	/**
	 * constructor
	 * @param args
	 */
	public MATSim4UrbanSimZurichTest(String args[]){
		super(args);
	}
	
	/**
	 * This modifies the MATSim network according to the given
	 * test parameter in the MATSim config file (from UrbanSim)
	 */
	@Override
	void modifyNetwork(NetworkImpl network){
		log.info("");
		log.info("Checking for network modifications ...");
		// check given test parameter for desired modifications
		String testParameter = scenario.getConfig().getParam(Constants.MATSIM_4_URBANSIM_PARAM, Constants.TEST_PARAMETER_PARAM);
		if(testParameter.equals("")){
			log.info("No modifications to perform.");
			log.info("");
			return;
		}
		else{
			String parameterArray[] = testParameter.split(",");
			
			for(int i = 0; i < parameterArray.length; i++){
				
				if (parameterArray[i].equalsIgnoreCase(uetlibergtunnel))
					removeUetliBergTunnel(network);
				else if (parameterArray[i].equalsIgnoreCase(schwamendingertunnel))
					removeSchwamendingerTunnel(network);
				else if (parameterArray[i].equalsIgnoreCase(birmsdorferstrasse))
					removeBirmensdorferstrasse(network);
			}
		}
		
		log.info("Done modifying network.");
		log.info("");
	}
	
	/**
	 * removes the Uetlibergtunnel links from MATSim network
	 * @param network
	 */
	private void removeUetliBergTunnel(NetworkImpl network){
		
		log.info("Removing Uetlibertunnel from network ...");
		
		network.removeLink(new IdImpl(108150));
		log.info("Removing link 108150");
		network.removeLink(new IdImpl(121962));
		log.info("Removing link 121962");
		
		log.info("Done removing Uetlibertunnel!");
	}
	
	/**
	 * removes the Birmensdorferstrasse links from MATSim network
	 * @param network
	 */
	private void removeBirmensdorferstrasse(NetworkImpl network){
		
		log.info("Removing Birmensdorferstrasse from network ...");
		
		network.removeLink(new IdImpl(125464));
		log.info("Removing link 125464");
		network.removeLink(new IdImpl(125460));
		log.info("Removing link 125460");
		
		log.info("Done removing Birmensdorferstrasse!");
	}
	
	private void removeSchwamendingerTunnel(NetworkImpl network){
		log.info("Removing Schwamendingertunnel from network ...");
		
		ArrayList<IdImpl> linkIds = new ArrayList<IdImpl>(){
			private static final long serialVersionUID = 1L;
			{
				add(new IdImpl(109024));
				add(new IdImpl(65583));
				add(new IdImpl(65582));
				add(new IdImpl(17692));
				add(new IdImpl(128604));
				add(new IdImpl(113201));
				add(new IdImpl(109059));
				add(new IdImpl(109060));
				add(new IdImpl(109061));
				add(new IdImpl(109021));
			}
		};
		
		for(int i = 0; i < linkIds.size(); i++){
			IdImpl id = linkIds.get(i);
			
			network.removeLink( id );
			log.info("Removing link " + id.toString());
		}
		
		log.info("Done removing Schwamendingertunnel!");
	}

	
	/**
	 * Entry point
	 * @param args urbansim command prompt
	 */
	public static void main(String args[]){
		MATSim4UrbanSimZurichTest zurichTest = new MATSim4UrbanSimZurichTest(args);
		zurichTest.runMATSim();
		MATSim4UrbanSimZurichTest.isSuccessfulMATSimRun = Boolean.TRUE;
	}
}
