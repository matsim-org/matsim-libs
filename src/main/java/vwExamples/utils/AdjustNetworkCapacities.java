/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package vwExamples.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.ArrayUtils;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author  jbischoff
 *
 */
public class AdjustNetworkCapacities {
	public static void main(String[] args) {
		
	Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
	String basedir = "D:/Axer/MatsimDataStore/BaseCases/VW205/";
	new MatsimNetworkReader(scenario.getNetwork()).readFile(basedir+"vw205.1.0.output_network.xml.gz");
	for (Link link : scenario.getNetwork().getLinks().values()){
		if (link.getId().toString().startsWith("tr")) continue;
			
		Coord coord = link.getCoord();
		
		//If we are in BS, we need to scale down the link capacities by factor 1/4
		//We guarantee a minimum link capacity of 2000 on all other links
		if ((coord.getY()>5785522&&coord.getY()<5800201)&&(coord.getX()>597958&&coord.getX()<610209)){
			//We increase/decrease the capacity by factor
			link.setCapacity(link.getCapacity()*0.25);
			
			//Minimum Capacity per Link
			if (link.getCapacity()<2000) link.setCapacity(2000);
			
			//Critical intersection areas within Braunschweig
			//Links at Hans-Sommer-Straße
			if ((coord.getY()>5792755&&coord.getY()<5792892)&&(coord.getX()>604705&&coord.getX()<604823)){
				link.setCapacity(1100);
				//Hist: 2100,2000,1500
			}
			
			//Links at Lenohardstraße
			if ((coord.getY()>5790948+100&&coord.getY()<5791044+100)&&(coord.getX()>605093+100&&coord.getX()<605230+100)){
				link.setCapacity(1200);
				//Hist: 1600,1500
			}
			
			//Links at HamburgerStraße/Neustadtring: Done
			if ((coord.getY()>5792670&&coord.getY()<5792828)&&(coord.getX()>603743&&coord.getX()<603886)){
				link.setCapacity(1200);
				//Hist: 2100,1600,
			}
			
			//Links at Rudolfsplatz: Done
			if ((coord.getY()>5792060&&coord.getY()<5792298)&&(coord.getX()>602301&&coord.getX()<602501)){
				link.setCapacity(1000);
				//Hist: 1700,1630,1200
			}
			
			//Links at Cellerstraße/Neustadtring
			if ((coord.getY()>5792346&&coord.getY()<5792513)&&(coord.getX()>602773&&coord.getX()<602863)){
				link.setCapacity(1000);
				//Hist: 800,
			}

			//Links at Rebenring/Mühlenpfordstraße/Mittelweg
			if ((coord.getY()>5792683&&coord.getY()<5792844)&&(coord.getX()>603964&&coord.getX()<604169)){
				link.setCapacity(1200);
				//Hist: 2100
			}
			
			//Links at HamburgerStraße/A392
			if ((coord.getY()>5793379+20&&coord.getY()<5793558+20)&&(coord.getX()>603479+20&&coord.getX()<603637+20)){
				link.setCapacity(1000);
				//Hist: 1300
			}
			
			//Links at Hagenring/Jasper
			if ((coord.getY()>5791881&&coord.getY()<5792054)&&(coord.getX()>605021&&coord.getX()<605134)){
				link.setCapacity(1000);
				//Hist: 1600,1500
			}
			
			//Links at Hagenring/Kastanien
			if ((coord.getY()>5791354&&coord.getY()<5791495)&&(coord.getX()>605257&&coord.getX()<605375)){
				link.setCapacity(1100);
				//Hist: 1550,1450,
			}
			
			//Links at Hagenring/Gließmaroderstr
			if ((coord.getY()>5792419&&coord.getY()<5792598)&&(coord.getX()>604817&&coord.getX()<604948)){
				link.setCapacity(1100);
			}
			//Hist: 1300
						
			//Links at Georgwestermann-Allee/Helmstedter
			if ((coord.getY()>5790910&&coord.getY()<5791027)&&(coord.getX()>605291&&coord.getX()<605478)){
				link.setCapacity(750);
				//Hist: 1300,1200
			}
			
			//Hansestraße Südl. A392 inkl. Gifhorner Straße
			if ((coord.getY()>5796246+50&&coord.getY()<5796340+50)&&(coord.getX()>603090+50&&coord.getX()<603402+50)){
				link.setCapacity(850);
				//Hist: 1100,1000,900
			}
			
			//Hansestraße Nörd. A392 
			if ((coord.getY()>5796388&&coord.getY()<5796462)&&(coord.getX()>602855&&coord.getX()<602985)){
				link.setCapacity(850);
				//Hist: 1100,1000,900
			}
			
			//Hagenmarkt
			if ((coord.getY()>5791814&&coord.getY()<5792027)&&(coord.getX()>603957&&coord.getX()<604111)){
				link.setCapacity(1000);
			}
			
			//Radeklint
			if ((coord.getY()>5791753&&coord.getY()<5791878)&&(coord.getX()>603240&&coord.getX()<603497)){
				link.setCapacity(1100);
				//Hist: 1500,1600,1500
			}
			
			//Europaplatz/Frankfurtstraße Süd
			if ((coord.getY()>5790612&&coord.getY()<5790908)&&(coord.getX()>603328&&coord.getX()<603651)){
				link.setCapacity(1500);
				//Hist: 1600
			}
			
			//Europaplatz/Frankfurtstraße Nord 
			if ((coord.getY()>5790861&&coord.getY()<5791016)&&(coord.getX()>603436&&coord.getX()<603620)){
				link.setCapacity(1700);
				//Hist: 1800
			}
			
			//BS Süd, Frankfurter Straße
			if ((coord.getY()>5789404&&coord.getY()<5789556)&&(coord.getX()>603067&&coord.getX()<603294)){
				link.setCapacity(1100);
			}

			//Cyriaksring/Münchenstraße
			if ((coord.getY()>5790375&&coord.getY()<5790627)&&(coord.getX()>602716&&coord.getX()<602999)){
				link.setCapacity(800);
			}
			
			//Donaustraße/Münchenstraße
			if ((coord.getY()>5789585&&coord.getY()<5789919)&&(coord.getX()>601294&&coord.getX()<601899)){
				link.setCapacity(1300);
				//Hist: 1100,
			}
			
			//Bahnhof Süd/B4
			if ((coord.getY()>5789925&&coord.getY()<5790217)&&(coord.getX()>604338&&coord.getX()<604593)){
				link.setCapacity(1350);
				//Hist: 1250,1500
			}
			
						

			 

			//Ciritcal links at Saarstraße/Lehndorf-Autobahn
			//Zufahrt von der B1
			
			//Zufahrt auf B1
			String[] fieldsToInclude = { "43270","58966","15849","69854","4669","4672","69858","48655","48656","43268"};
			if ( Arrays.asList(fieldsToInclude).contains( link.getId().toString()) )  {
				link.setCapacity(1900);
			}
			
			//Zufahrt von B1
			String[] fieldsToInclude2 = { "13111","43269","43271","43273"};
			if ( Arrays.asList(fieldsToInclude2).contains( link.getId().toString()) )  {
				link.setCapacity(1750);
			}
			
			String[] fieldsToInclude91 = { "66904","17587"};
			if ( Arrays.asList(fieldsToInclude91).contains( link.getId().toString()) )  {
				link.setCapacity(1000);
			}
						
			
			
			
			//Zufahrt von der B1
			
//			//Auffahrt auf A391 Richtung Süd
//			if (link.getId().toString().equals("4672")) 
//			{link.setCapacity(8000);}
//			
//			if (link.getId().toString().equals("69858")) 
//			{link.setCapacity(8000);}
//			
//			if (link.getId().toString().equals("15849")) 
//			{link.setCapacity(8000);}
//			
//			if (link.getId().toString().equals("40565")) 
//			{link.setCapacity(8000);}
//			
//			if (link.getId().toString().equals("4661")) 
//			{link.setCapacity(8000);}
//			//Auffahrt auf A391 Richtung Süd

			

			
			
//			//Abfahrt A391 Lehndorf von Nord kommend
//			if (link.getId().toString().equals("13938")) 
//			{link.setCapacity(3600);}
//			if (link.getId().toString().equals("4487")) 
//			{link.setCapacity(3600);}
//			//Abfahrt A391 Lehndorf von Nord kommend
//			
//			
//			//übergangskante Lehndorf
//			if (link.getId().toString().equals("48656")) 
//			{link.setCapacity(8000);}
//			
//			
//			//Abfahrt A391 Lehndorf von Süd kommend
//			if (link.getId().toString().equals("4529")) 
//			{link.setCapacity(1200);}
//			//Abfahrt A391 Lehndorf von Süd kommend
//			
//
//			

			
			
		}else 
		{
//			link.setCapacity(link.getCapacity()*1.1);
		}
		
//		//Intersections in Meine
//		if ((coord.getY()>5804153&&coord.getY()<5804803)&&(coord.getX()>604184&&coord.getX()<604869)){
//			link.setCapacity(1400);
//		}
		
		///Intersections in Meine///
		String[] fieldsToInclude3 = { "49379","22517"};
		if ( Arrays.asList(fieldsToInclude3).contains( link.getId().toString()) )  {
			link.setCapacity(900);
		}
		
		String[] fieldsToInclude4 = { "165"};
		if ( Arrays.asList(fieldsToInclude4).contains( link.getId().toString()) )  {
			link.setCapacity(400);
		}
		
		String[] fieldsToInclude5 = { "70938"};
		if ( Arrays.asList(fieldsToInclude5).contains( link.getId().toString()) )  {
			link.setCapacity(600);
		}
		
		String[] fieldsToInclude6 = { "21201"};
		if ( Arrays.asList(fieldsToInclude6).contains( link.getId().toString()) )  {
			link.setCapacity(500);
		}
		///Intersections in Meine//
		

		//Berliner Heer Straße/Friedrich Voigländer	
		if ((coord.getY()>5793076+100&&coord.getY()<5793456+100)&&(coord.getX()>606820+100&&coord.getX()<607110+100)){
			link.setCapacity(1100);
			//Hist: 1400,1600
		}
		
		//Berliner Heer Straße/Hordorfer
		if ((coord.getY()>5793598&&coord.getY()<5794014)&&(coord.getX()>607881&&coord.getX()<608182)){
			link.setCapacity(1100);
			//Hist: 1200
		}
		
		//Lehre
		if ((coord.getY()>5798644+200&&coord.getY()<5798845+200)&&(coord.getX()>613373+200&&coord.getX()<613849+200)){
			link.setCapacity(700);
			//Hist: 750, 800
		}
		
		
		//A39 Congestion Links
		String[] fieldsToInclude7 = { "10104"};
		if ( Arrays.asList(fieldsToInclude7).contains( link.getId().toString()) )  {
			link.setCapacity(2100);
		}
		
		
		String[] fieldsToInclude8 = { "28521","65600","36133","65588" };
		if ( Arrays.asList(fieldsToInclude8).contains( link.getId().toString()) )  {
			link.setCapacity(1900);
			//Hist: 2100,1900
		}
		
		String[] fieldsToInclude9 = { "63688"};
		if ( Arrays.asList(fieldsToInclude9).contains( link.getId().toString()) )  {
			link.setCapacity(3400);
			//Hist: 4100
		}
		
		//Berliner Ring, Richtung BS
		String[] fieldsToInclude10 = { "366"};
		if ( Arrays.asList(fieldsToInclude10).contains( link.getId().toString()) )  {
			link.setCapacity(2400);
		//Hist: 2500
		}
		
		//Berliner Ring, Richtung Autostadt
			String[] fieldsToInclude11 = { "42600"};
			if ( Arrays.asList(fieldsToInclude11).contains( link.getId().toString()) )  {
				link.setCapacity(1900);
			//Hist: 2500
		}
			

		//Intersections in Wolfsburg	
		//K5 und B188
		if ((coord.getY()>5811070&&coord.getY()<5811248)&&(coord.getX()>622219&&coord.getX()<622481)){
			link.setCapacity(1950);
			//hist: 1850
		}
		
		//Schulenburgallee/B188
		if ((coord.getY()>5811279&&coord.getY()<5811533)&&(coord.getX()>622814&&coord.getX()<623072)){
			link.setCapacity(2150);
			//Hist: 1800,1400,1550,1700,1800,2000
		}
		
		//B188 and K4
		if ((coord.getY()>5811152&&coord.getY()<5811401)&&(coord.getX()>624957&&coord.getX()<625402)){
			link.setCapacity(1400);
			//Hist: 1300,
		}
		
		//B188 and L290
		if ((coord.getY()>5810680&&coord.getY()<5811165)&&(coord.getX()>625501&&coord.getX()<625926)){
			link.setCapacity(780);
			//Hist: 1300
		}

		//Dieselstraße and Zollstraße
		if ((coord.getY()>5809939&&coord.getY()<5810066)&&(coord.getX()>625444&&coord.getX()<625811)){
			link.setCapacity(650);
			//Hist: 1300
		}
		
		//Dieselstraße and Käthe Paulus
		if ((coord.getY()>5809939&&coord.getY()<5810066)&&(coord.getX()>625444&&coord.getX()<625811)){
			link.setCapacity(850);
			
		}
		
		//L290 and K2
		if ((coord.getY()>5809066&&coord.getY()<5809333)&&(coord.getX()>625589&&coord.getX()<625865)){
			link.setCapacity(650);
			//Hist: 1300
		}
		
			
		//Sandkrug and Käthe Paulus
		if ((coord.getY()>5809406&&coord.getY()<5809635)&&(coord.getX()>624319&&coord.getX()<624603)){
			link.setCapacity(800);
		}
		

		//Hafenstraße/Wehyhäuserweg
		if ((coord.getY()>5809503&&coord.getY()<5809721)&&(coord.getX()>616569&&coord.getX()<616845)){
			link.setCapacity(950);
		}
		
		//Siemensstraße/Berlinerrimg
		if ((coord.getY()>5808783&&coord.getY()<5808991)&&(coord.getX()>621567&&coord.getX()<621878)){
			link.setCapacity(1750);
			//Hist: 1500,1650
		}
		
		//K28 and K114
		if ((coord.getY()>5810686&&coord.getY()<5810994)&&(coord.getX()>616468&&coord.getX()<616808)){
			link.setCapacity(900);
		}
		
		
	}
	new NetworkWriter(scenario.getNetwork()).write(basedir+"vw212.1.0.output_network_mod.xml.gz");
	
	}


//	static boolean decideToAdjust(Coord coord){
//		if (coord.getX()<593084) return true;
//		else if (coord.getX()>629810) return true;
//		else if (coord.getY()<5785583) return true;
//		else if (coord.getY()>5817600) return true;
//		else return false;
//	} 
	static boolean decideToAdjust(Coord coord){
		//This is the complete area of Braunschweig
		if ((coord.getY()>5785522&&coord.getY()<5800201)&&(coord.getX()>597958&&coord.getX()<610209)) return true;
		
		else return false;
		
	}
	



}
