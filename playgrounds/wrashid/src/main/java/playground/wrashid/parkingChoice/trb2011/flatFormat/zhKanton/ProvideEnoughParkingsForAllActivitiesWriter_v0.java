package playground.wrashid.parkingChoice.trb2011.flatFormat.zhKanton;

import java.io.BufferedWriter;
import java.io.IOException;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.contrib.parking.lib.obj.Matrix;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.io.MatsimXmlWriter;

import playground.wrashid.lib.tools.kml.BasicPointVisualizer;
import playground.wrashid.lib.tools.kml.Color;
import playground.wrashid.parkingChoice.infrastructure.PublicParking;
import playground.wrashid.parkingChoice.infrastructure.api.PParking;
import playground.wrashid.parkingChoice.trb2011.flatFormat.zhCity.DrawAllActivitiesWithParkingsCloseBy;

/*
 * 
 * Goal: provide enough parkings for the region outside the city of zürich.
 * 
 * 
 */
public class ProvideEnoughParkingsForAllActivitiesWriter_v0 extends MatsimXmlWriter {

	private static BasicPointVisualizer basicPointVisualizer = new BasicPointVisualizer();

	private static QuadTree<PParking> parkingsQuadTreeOutsideCityZH=null;
	
	public static void main(String[] args) {
		//String inputPlansFile = "K:/Projekte/herbie/output/demandCreation/plans.xml.gz";
		//String inputFacilities = "K:/Projekte/herbie/output/demandCreation/facilitiesWFreight.xml.gz";
		//String inputNetworkFile = "K:/Projekte/matsim/data/switzerland/networks/ivtch-multimodal/zh/network.multimodal-wu.xml.gz";

		String inputPlansFile = "H:/data/cvs/ivt/studies/switzerland/plans/teleatlas-ivtcheu/census2000v2_dilZh30km_10pct/plans.xml.gz";
		String inputFacilities = "H:/data/cvs/ivt/studies/switzerland/facilities/facilities.xml.gz";
		String inputNetworkFile = "H:/data/cvs/ivt/studies/switzerland/networks/teleatlas-ivtcheu/network.xml.gz";
		
		String outputKmlFile = "C:/data/My Dropbox/ETH/Projekte/TRB Aug 2011/parkings/kmls/parkingsOutsideZHCity.kml";
		String outputParkingFileName = "C:/data/My Dropbox/ETH/static data/parking/zürich city/flat/publicParkingsOutsideZHCity_v0_kti_dilZh30km_10pct.xml";

		Scenario scenario = GeneralLib.readScenario(inputPlansFile, inputNetworkFile, inputFacilities);

		QuadTree<PParking> parkingsQuadTreeZHCity = DrawAllActivitiesWithParkingsCloseBy.getParkingsQuadTreeZHCity();

		parkingsQuadTreeOutsideCityZH = initializeQuadTreeForOutsideZHCity(scenario.getPopulation());

		for (Person person : scenario.getPopulation().getPersons().values()) {
			for (PlanElement pe : person.getSelectedPlan().getPlanElements()) {
				if (pe instanceof Activity) {
					Activity activity = (Activity) pe;
					Coord actCoord = activity.getCoord();
					PParking closestParkingZHCity = parkingsQuadTreeZHCity.getClosest(actCoord.getX(), actCoord.getY());

					if (isActivityOutsideOfCity(actCoord, closestParkingZHCity)) {
						PParking closestParkingOutsideZHCity = parkingsQuadTreeOutsideCityZH.getClosest(actCoord.getX(), actCoord.getY());
						if (parkingsQuadTreeOutsideCityZH.size() < 1) {
							createPublicParkingAtActivityLocation(parkingsQuadTreeOutsideCityZH, activity);
						} else if (GeneralLib.getDistance(actCoord, closestParkingOutsideZHCity.getCoord()) > 200) {
							createPublicParkingAtActivityLocation(parkingsQuadTreeOutsideCityZH, activity);
						}
					}
				}
			}
		}

		System.out.println("writing kml file...");
		basicPointVisualizer.write(outputKmlFile);
		
		String sourcePath = "ETH/static data/parking/zürich city/Parkhaeuser/parkhäuser.txt";

		Matrix garageParkingData = GeneralLib.readStringMatrix("c:/data/My Dropbox/" + sourcePath);
		
		ProvideEnoughParkingsForAllActivitiesWriter_v0 garageParkingsOutsideZHCityWriter=new ProvideEnoughParkingsForAllActivitiesWriter_v0();
		garageParkingsOutsideZHCityWriter.writeFile(outputParkingFileName, sourcePath,garageParkingData);

	}

	private static QuadTree<PParking> initializeQuadTreeForOutsideZHCity(Population population) {
		double minX = Double.MAX_VALUE;
		double minY = Double.MAX_VALUE;
		double maxX = Double.MIN_VALUE;
		double maxY = Double.MIN_VALUE;

		for (Person person : population.getPersons().values()) {
			for (PlanElement pe : person.getSelectedPlan().getPlanElements()) {
				if (pe instanceof Activity) {
					Activity activity = (Activity) pe;

					if (activity.getCoord().getX() < minX) {
						minX = activity.getCoord().getX();
					}

					if (activity.getCoord().getY() < minY) {
						minY = activity.getCoord().getY();
					}

					if (activity.getCoord().getX() > maxX) {
						maxX = activity.getCoord().getX();
					}

					if (activity.getCoord().getY() > maxY) {
						maxY = activity.getCoord().getY();
					}
				}


			}
		}
		QuadTree<PParking> quadTree = new QuadTree<PParking>(minX - 1.0, minY - 1.0, maxX + 1.0, maxY + 1.0);

		return quadTree;
	}

	private static void createPublicParkingAtActivityLocation(QuadTree<PParking> parkingsQuadTreeOutsideCityZH, Activity activity) {
		PublicParking publicParking = new PublicParking(activity.getCoord());
		publicParking.setCapacity(5000);
		parkingsQuadTreeOutsideCityZH.put(activity.getCoord().getX(), activity.getCoord().getY(), publicParking);

		basicPointVisualizer.addPointCoordinate(activity.getCoord(), activity.getType(), Color.GREEN);
	}

	private static boolean isActivityOutsideOfCity(Coord actCoord, PParking closestParkingZHCity) {
		return GeneralLib.getDistance(actCoord, closestParkingZHCity.getCoord()) > 300;
	}
	
	public void writeFile(final String filename, String source, Matrix garageParkingData) {
		String dtd = "./test/input/playground/wrashid/parkingChoice/infrastructure/flatParkingFormat_v1.dtd";

		try {
			openFile(filename);
			writeXmlHead();
			writeDoctype("flatParkings", dtd);

			this.writer.write("<!-- data source: "+ source +" -->\n\n");
			
			this.writer.write("<flatParkings>\n");
			
			writeFacilities(garageParkingData, this.writer);
			
			this.writer.write("</flatParkings>\n");
			
			this.writer.close();
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	private void writeFacilities(Matrix garageParkingData, BufferedWriter writer) throws IOException {
		int i=1;
		for (PParking parking:parkingsQuadTreeOutsideCityZH.values()){
			writer.write("\t<parking type=\"public\"");
			writer.write(" id=\"publicPOutsideCityZH-" + i++ +"\"");
			writer.write(" x=\""+ parking.getCoord().getX() +"\"");
			writer.write(" y=\""+ parking.getCoord().getY() +"\"");
			writer.write(" capacity=\""+ parking.getCapacity() +"\"");
			writer.write("/>\n");
		}
	}

}
