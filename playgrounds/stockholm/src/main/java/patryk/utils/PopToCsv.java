package patryk.utils;


public class PopToCsv {
//	final static String inputFile= "agentdata.txt";
//	final static String outputFile = "agentData.csv";
//	static int numOfCars = 0;
//	static int numOfCarPass = 0;
//	static int numOfPT = 0;
//	static int numOfBicycle = 0;
//	static int numOfWalk = 0;
//	static int numOfUndef = 0;
//
//	public static void main(String[] args) throws IOException {
//		Scanner scanner = new Scanner(new FileReader(inputFile));
//		CSVWriter writer = new CSVWriter(new FileWriter(outputFile), ';', CSVWriter.NO_QUOTE_CHARACTER);
//		
//		String firstLine = scanner.nextLine();
//		System.out.println(firstLine);
//		int count = 0;
//		
//		while (scanner.hasNextLine()) {
//			scanner.nextLine();
//			if (scanner.hasNextLine()) {
//				processAgent(scanner, writer);
//				count++;
//				if (count % 10000 == 0 ) {
//					System.out.println("Processed agents: " + count);
//				}
//			}
//		}
//	
//		scanner.close();
//		writer.close();
//		System.out.println("Processed agents: " + count);
//		System.out.println("# car driver: " + numOfCars);
//		System.out.println("# car passenger: " + numOfCarPass);
//		System.out.println("# PT: " + numOfPT);
//		System.out.println("# bicycle: " + numOfBicycle);
//		System.out.println("# walk: " + numOfWalk);
//		System.out.println("# undef: " + numOfUndef);
//	}
//	
//	private static void processAgent(Scanner scanner, CSVWriter writer) {
//		String[] agentData = new String[8];
//		String[] agentDataFormatted = new String[9];
//
//		for (int i=0; i<8; i++) {
//			if (!scanner.hasNextLine()) {
//				return;
//			}
//			String line = scanner.nextLine();
//			Scanner sc = new Scanner(line);
//			sc.useDelimiter(":");
//			sc.next(); agentData[i] = sc.next().trim();
//			sc.close();
//		}
//		
//		agentDataFormatted[0] = agentData[0];		// ID
//		agentDataFormatted[1] = agentData[2];		// Birthyear
//		
//		if (agentData[1].equals("man")) {			// Sex
//			agentDataFormatted[2] = "0";			
//		}
//		else if (agentData[1].equals("woman")) {
//			agentDataFormatted[2] = "1";
//		}
//		else {
//			System.out.println("Undefined sex");
//		}
//		
//		agentDataFormatted[3] = agentData[3];		// Income
//		agentDataFormatted[4] = agentData[5];		// Home zone
//		agentDataFormatted[5] = agentData[7];		// Work zone
//		agentDataFormatted[6] = agentData[6];		// Mode
//		
//		if (agentData[6].equals("car")) {
//			numOfCars++;
//		}
//		else if (agentData[6].equals("carpass")){
//			numOfCarPass++;
//		}
//		else if (agentData[6].equals("pt")){
//			numOfPT++;
//		}
//		else if (agentData[6].equals("bicycle")){
//			numOfBicycle++;
//		}
//		else if (agentData[6].equals("walk")){
//			numOfWalk++;
//		}
//		else {
//			numOfUndef++;
//		}
//		
//		agentDataFormatted[7] = "0";				// Unused
//		
//		if (agentData[4].equals("villa")) {			// Housingtype
//			agentDataFormatted[8] = "0";	
//		}
//		else if (agentData[4].equals("apartment")) {
//			agentDataFormatted[8] = "1";
//		}
//		else {
//			System.out.println("Undefined housingtype");
//		}
//		
//		writer.writeNext(agentDataFormatted);	
//	}
}
