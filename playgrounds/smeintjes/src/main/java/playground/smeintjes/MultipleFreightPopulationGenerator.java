package playground.smeintjes;

import playground.southafrica.population.freight.FreightChainGenerator;
import playground.southafrica.utilities.Header;

/**
 * This class generates multiple freight populations using
 * playground.southafrica.population.freight.FreightChainGenerator. All 
 * populations will use the same input parameters, and are therefore all based
 * on the same complex network, and will have the same number of plans. The output 
 * file names will have the same basic description given in the parameters, with 
 * an index number appended to prevent overwriting of files. 
 * 
 * @author sumarie
 *
 */
public class MultipleFreightPopulationGenerator {
	
	/**
	 * 
	 * @param the path to the path dependent complex network file, ending in ".xml.gz"
	 * @param the number of plans to generate per population
	 * @param the sub-population description, in this case it's "commercial"
	 * @param the path to where the plans file should be written, NOT including the file 
	 * 		  extension .xml.gz.
	 * @param the path to where the attribute file should be written, not including file extension
	 * @param the number of threads to use for the multi-threaded part
	 * @param the number of freight populations to generate
	 */
	public static void main(String args[]){
		Header.printHeader(MultipleFreightPopulationGenerator.class.toString(), args);
		
		String complexNetworkFile = args[0];
		int numberOfPlans = Integer.parseInt(args[1]);
		String populationPrefix = args[2];
		String outputPlansFile = args[3];
		String attributeFile = args[4];
		int numberOfThreads = Integer.parseInt(args[5]);
		int numberOfPopulations = Integer.parseInt(args[6]);
		
		generatePopulations(args, numberOfPopulations);
		
	}
	
	public static void generatePopulations(String[] arguments, int numberOfPopulations){
		
		
		
		for(int i = 0; i < numberOfPopulations; i++){
			String output = String.format("%s_%d.xml.gz", arguments[3], i);
			String attributeFile = String.format("%s_%d.xml.gz", arguments[4], i);
			
			String[] appendedArguments = new String[]{arguments[0], arguments[1], 
					arguments[2], output, attributeFile, arguments[5], arguments[6]};
			
			
			FreightChainGenerator.main(appendedArguments);
			
		}
		
	}

}
