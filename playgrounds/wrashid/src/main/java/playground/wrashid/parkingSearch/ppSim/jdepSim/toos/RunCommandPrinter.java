package playground.wrashid.parkingSearch.ppSim.jdepSim.toos;

public class RunCommandPrinter {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		for (int i=200;i<400;i++){
			System.out.print(i + "\t");
			System.out.print("cd data/sandbox/sandbox/playgrounds/wrashid; export MAVEN_OPTS=-Xmx15g; mvn -e exec:java -Dexec.mainClass=");
			System.out.print("\"playground.wrashid.parkingSearch.ppSim.jdepSim.MainPPSimZurich30km\" -Dexec.args=\"/Network/Servers/kosrae.ethz.ch/Volumes/ivt-home/wrashid/data/experiments/parkingSearchOct2013/");
			System.out.print("runs/run" + i);
			System.out.print("/config.xml\" 2>&1 | tee /Network/Servers/kosrae.ethz.ch/Volumes/ivt-home/wrashid/data/experiments/parkingSearchOct2013/runs/run");
			System.out.print(i + "/output/log.txt &");
			System.out.println();
		}

	}

}
