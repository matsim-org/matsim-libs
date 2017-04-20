package saleem.stockholmmodel.transitdataconversion;

/**
 * Execution class to convert Excel reports of transit data into MatSim forms. 
 * VehicleTypes.xls, Stoppstallen.xls and all transit schedule excel reports are required.
 *
 * @author Mohammad Saleem
 */
public class ExceltoMatSimDataConversion {
	public static void main(String[] args){
		//It may take a few minutes, so please be patient...
		/*
		 * Add the following at the very start of transit schedule if there are SAX parsing errors while running the simulation.
		 
		  <?xml version="1.0" encoding="UTF-8"?>
		  <!DOCTYPE transitSchedule SYSTEM "http://www.matsim.org/files/dtd/transitSchedule_v1.dtd">
 
		 */
		ExcelReportsReader ex = new ExcelReportsReader();
		TransitSchedule transit = new TransitSchedule();
		ex.readExcelReportStopStallen(transit, "H:\\Matsim\\Reports\\Stoppstallen.xls" );
		ex.readExcelReports(transit, "H:\\Matsim\\Reports");//Folder containing all transit schedule excel reports from SL.
		ex.readVehicleTypes(transit, "H:\\Matsim\\Reports\\VehicleTypes.xls");
		XMLWriter xmlwriter = new XMLWriter();
		xmlwriter.createTransitSchedule(transit, "./ihop2/TransitSchedule.xml");
		xmlwriter.createVehiclesXML(transit, "./ihop2/vehicles.xml");
	}
}
