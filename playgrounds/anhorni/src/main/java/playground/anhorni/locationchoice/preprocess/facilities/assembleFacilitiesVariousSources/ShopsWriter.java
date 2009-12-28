package playground.anhorni.locationchoice.preprocess.facilities.assembleFacilitiesVariousSources;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.matsim.core.gbl.Gbl;
import org.matsim.core.utils.io.IOUtils;

public class ShopsWriter {
	
	private BufferedWriter shops;
	
	public ShopsWriter() {

		try {
			shops =  IOUtils.getBufferedWriter("output/zhshops.txt");
			
		} catch (final IOException e) {
			Gbl.errorMsg(e);
		}
	}
		
	public void write(List<ZHFacilityComposed> zhfacilities)  {
		try {
			
			String header ="id\tx\ty\tRetailer_category\tName\tZIP\tCity\tStreet\tHNR\tSize_category\tSize\t" +
					"Shop_type\tDesc\tHrs_week\tParking_lots\tParking_costs_perhour";
			
			header +=	"MON_start\tMON_break_start\tMON_break_end\tMON_end\t" +
						"TUE_start\tTUE_break_start\tTUE_break_end\tTUE_end\t" +
						"WED_start\tWED_break_start\tWED_break_end\tWED_end\t" +
						"THU_start\tTHU_break_start\tTHU_break_end\tTHU_end\t" +
						"FRI_start\tFRI_break_start\tFRI_break_end\tFRI_end\t" +
						"SAT_start\tSAT_break_start\tSAT_break_end\tSAT_end\t" +
						"SUN_start\tSUN_break_start\tSUN_break_end\tSUN_end\t";
			
			shops.write(header +"\n");
			
			Iterator<ZHFacilityComposed> facilities_it = zhfacilities.iterator();
			while (facilities_it.hasNext()) {
				ZHFacilityComposed facility = facilities_it.next();				
				shops.write(facility.getId() +"\t");
				shops.write(facility.getCoords().getX() +"\t");
				shops.write(facility.getCoords().getY() +"\t");
				shops.write(facility.getRetailerCategory() +"\t");
				shops.write(facility.getName() +"\t");
				shops.write(facility.getPLZ() +"\t");
				shops.write(facility.getCity() +"\t");
				shops.write(facility.getStreet() +"\t");
				shops.write(facility.getHNR() +"\t");
				shops.write(facility.getSizeCategory() +"\t");
				shops.write(facility.getSize() +"\t");
				shops.write(facility.getShopType() +"\t");
				shops.write(facility.getDesc() +"\t");
				
				int whours = (int)(facility.getHrsWeek()) / 3600;
				int wremainder = (int)facility.getHrsWeek() % 3600;
				int wminutes = (int)wremainder / 60;
				shops.write((whours < 10 ? "0" : "") + whours + ":" + (wminutes < 10 ? "0" : "") + wminutes +"\t");
				
				shops.write(facility.getParkingLots() +"\t");
				shops.write(facility.getParkingCostsPerHour() +"\t");
				
				for (int i = 0; i < 7; i++) {
					for (int j = 0; j < 4; j++) {
						
						if (facility.getOpentimes()[i][j] > 0.0) {
							int hours = (int)(facility.getOpentimes()[i][j]) / 3600;
							int remainder = (int)facility.getOpentimes()[i][j] % 3600;
							int minutes = (int)remainder / 60;
							
							shops.write((hours < 10 ? "0" : "") + hours + ":" + (minutes < 10 ? "0" : "") + minutes +"\t");
						}
						else {
							shops.write(facility.getOpentimes()[i][j] +"\t");
						}	                           
					}
				}
				
				shops.newLine();
			}
			shops.flush();
			shops.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
	}
}
