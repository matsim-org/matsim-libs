package gunnar.ihop2.scaper;

public class TryPopulationSampler {
	public static void main(String[] args){
		String srcpop = "H:\\Matsim\\Stockholm Scenario\\teleportation\\input\\initial_plans_100%_regent.xml";
		String destpop = "H:\\Matsim\\Stockholm Scenario\\teleportation\\input\\initial_plans_ppp1%_regent.xml";
		String network = "";
		double sample = 0.00001;
		PopulationSampler popsampler = new PopulationSampler();
		//Call this method with null if network file is not required, otherwise pass network file path
		popsampler.createSample(srcpop, network, sample, destpop);
		}

}
