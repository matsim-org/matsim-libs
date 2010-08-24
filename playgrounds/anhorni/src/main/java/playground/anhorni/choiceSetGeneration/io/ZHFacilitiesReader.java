package playground.anhorni.choiceSetGeneration.io;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.utils.geometry.CoordImpl;

import playground.anhorni.choiceSetGeneration.helper.ZHFacilities;
import playground.anhorni.choiceSetGeneration.helper.ZHFacility;


/*	  0			1		2			3			4			5		6		7			8				9
 * ---------------------------------------------------------------------------------------------------------		
 *0	| ShopID	RetID	Size_descr	dHalt		aAlt02		aAlt10	aAlt20	retailer	Sales_area_m2	classif	
 *1	| Shoptype	PLZ		ORT			STRASSE		HNR			x_CH	y_CH	NAME		HRS_WEEK		TOTAL		
 *2	| mon		V20		V21			V22			V23			V24		tue		V26			V27				V28			
 *3	| V29		V30		wed			V32			V33			V34		V35		V36			thu				V38		
 *4	| V39		V40		V41			V42			fri			V44		V45		V46			V47				V48
 *5	| sat		V50		V51			V52			V53			V54		sun		V56			V57				Tel
 *6	| Email		Web		park_only	Park_joint	Turnover_m2	Hweek	cost_parking_h
 * ---------------------------------------------------------------------------------------------------------
 */


public class ZHFacilitiesReader {
	
	private NetworkLayer network = null;
	private final static Logger log = Logger.getLogger(ZHFacilitiesReader.class);
		
	public ZHFacilitiesReader(NetworkLayer network) {
		this.network = network;
	}
	
	public void readFile(final String file, ZHFacilities facilities)  {
		
		if (file == null) {
			log.error("file is null");
			return;
		}
		try {
			FileReader fileReader = new FileReader(file);
			BufferedReader bufferedReader = new BufferedReader(fileReader);
			String curr_line = bufferedReader.readLine(); // Skip header
			
			while ((curr_line = bufferedReader.readLine()) != null) {	
				String[] entries = curr_line.split("\t", -1);
				
				String shopID = entries[0].trim();
				String retailerID =  entries[1].trim();
				int size_descr = Integer.parseInt(entries[2].trim());
				double dHalt = Double.parseDouble(entries[3].trim());
				double xCH = Double.parseDouble(entries[4].trim());
				double yCH = Double.parseDouble(entries[5].trim());
				double hrs_week = Double.parseDouble(entries[6].trim());
				
				String name = entries[6].trim();
				
				Coord exactPosition = new CoordImpl(xCH, yCH);
				LinkImpl closestLink = network.getNearestLink(exactPosition);
				
				facilities.addFacilityByLink(closestLink.getId(), new ZHFacility(
									new IdImpl(shopID),
									name,
									closestLink.getCoord(),
									exactPosition, 
									closestLink.getId(),
									new IdImpl(retailerID),
									size_descr,
									dHalt,
									hrs_week));	
			}
			bufferedReader.close();
			fileReader.close();
		
		} catch (IOException e) {
			Gbl.errorMsg(e);
		}	
	}





}
