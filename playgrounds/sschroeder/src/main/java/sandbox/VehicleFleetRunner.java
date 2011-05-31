package sandbox;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import kid.KiDDataReader;
import kid.ScheduledVehicles;
import kid.filter.BusinessSectorFilter;

import org.apache.commons.math.stat.Frequency;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;

import sandbox.Classifier.FrequencyClass;
import sandbox.SandBoxTrafficGenerator.Company;
import sandbox.VehicleFleetBuilder.CompanyClass;

public class VehicleFleetRunner {

	/**
	 * @param args
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 */
	public static void main(String[] args) throws FileNotFoundException, IOException {
		ScheduledVehicles vehicles = new ScheduledVehicles();
		KiDDataReader dataReader = new KiDDataReader(vehicles);
		String directory = "/Volumes/projekte/2000-Watt-City/Daten/KiD/";
		dataReader.setVehicleFile(directory + "KiD_2002_Fahrzeug-Datei.txt");
		dataReader.setTransportChainFile(directory + "KiD_2002_Fahrtenketten-Datei.txt");
		dataReader.setTransportLegFile(directory + "KiD_2002_(Einzel)Fahrten-Datei.txt");
		dataReader.setVehicleFilter(new BusinessSectorFilter("D"));
		dataReader.run();
		
		List<CompanyClass> companyClasses = new ArrayList<CompanyClass>();
		companyClasses.add(new CompanyClass(CompanyClass.MICRO, new FrequencyClass(1, 0.0, 20.0)));
		companyClasses.add(new CompanyClass(CompanyClass.SMALL, new FrequencyClass(2, 20.0, 50.0)));
		companyClasses.add(new CompanyClass(CompanyClass.MEDIUM, new FrequencyClass(3, 50.0, 250.0)));
		companyClasses.add(new CompanyClass(CompanyClass.BIG, new FrequencyClass(4, 250.0, 1000.0)));
		companyClasses.add(new CompanyClass(CompanyClass.HUGE, new FrequencyClass(5, 1000.0, null)));
		
		VehicleFleetBuilder fleetBuilder = new VehicleFleetBuilder(vehicles,companyClasses);
		Company c = new Company(makeId("matthias"));
		c.setnOfEmployees(300);
		CompanyClass companyClass = getCompanyClass(c.getnOfEmployees(), companyClasses);
		Plotter plotter = new Plotter();
		Frequency modelledPkwFrequency = new Frequency();
		Frequency modelledLkwFrequency = new Frequency();
		for(int i=0;i<1000;i++){
			double noLkw = fleetBuilder.getNoLkw(c);
			System.out.println("nOfLkwInFleet=" + noLkw);
			modelledLkwFrequency.addValue(noLkw);
			double noPkw = fleetBuilder.getNoPkw(c);
			System.out.println("nOfPkwInFleet=" + noPkw);
			modelledPkwFrequency.addValue(noPkw);
			System.out.println();
		}
		plotter.plotCounts("output/modPkw.png", "output/modPkw_legend.txt", "pkws", "x", "y", modelledPkwFrequency);
		plotter.plotCounts("output/modLkw.png", "output/modLkw_legend.txt", "lkws", "x", "y", modelledLkwFrequency);
		
		plotter.plotCounts("output/actPkw.png", "output/actPkw_legend.txt", "pkws", "x", "y", fleetBuilder.getPkwWalkers().get(companyClass.getCompanySizeType()).getFrequency());
		plotter.plotCounts("output/actLkw.png", "output/actLkw_legend.txt", "lkws", "x", "y", fleetBuilder.getLkwWalkers().get(companyClass.getCompanySizeType()).getFrequency());

	}

	private static Id makeId(String string) {
		return new IdImpl(string);
	}
	
	private static CompanyClass getCompanyClass(int getnOfEmployees, List<CompanyClass> companyClasses) {
		for(CompanyClass companyClass : companyClasses){
			if(companyClass.getFrequencyClass().hasValue(getnOfEmployees)){
				return companyClass;
			}
		}
		return null;
	}

}
