package playground.dhosse.gap.scenario.counts;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;

public class CountsCreator {

	/**
	 * Creates counting stations for GaPa
	 * 
	 * @param network
	 * @return
	 */
	public static Counts createCountingStations(Network network){

		//create counts container and set the description, the name and the year of the survey
		Counts counts = new Counts();
		counts.setDescription("eGaP");
		counts.setName("Pendler");
		counts.setYear(2005);
		
		//create counting stations one by one and fill the hourly values
		
		//Alpspitzstraße TODO
		
		//Burgstraße
		//Burgstraße Richtung Süden
		Count count5 = counts.createAndAddCount(Id.createLinkId("18003"), "Burgstr Richtung Süden");
		count5.setCoord(network.getLinks().get(Id.createLinkId("18003")).getCoord());
		count5.createVolume(7, 28+43+57+72);
		count5.createVolume(8, 85+119+140+170);
		count5.createVolume(9, 130+118+139+139);
		count5.createVolume(10, 115+123+122+148);
		count5.createVolume(16, 111+139+106+140);
		count5.createVolume(17, 157+130+131+126);
		count5.createVolume(18, 133+132+113+124);
		count5.createVolume(19, 131+115+103+127);
		//Burgstraße Richtung Norden
		Count count6 = counts.createAndAddCount(Id.createLinkId("12997"), "Burgstr Richtung Norden");
		count6.setCoord(network.getLinks().get(Id.createLinkId("12997")).getCoord());
		count6.createVolume(7,77+18+34+59);
		count6.createVolume(8, 69+60+80+97);
		count6.createVolume(9, 79+79+78+93);
		count6.createVolume(10, 89+97+117+104);
		count6.createVolume(16, 141+134+121+118);
		count6.createVolume(17, 173+138+171+169);
		count6.createVolume(18, 183+166+161+169);
		count6.createVolume(19, 162+131+105+108);
		
		//Alleestraße Ost
		//Richtung Osten
		Count count7 = counts.createAndAddCount(Id.createLinkId("13026"), "Alleestraße Richtung Osten");
		count7.setCoord(network.getLinks().get(Id.createLinkId("13026")).getCoord());
		count7.createVolume(7, 30+16+20+33);
		count7.createVolume(8, 41+56+90+96);
		count7.createVolume(9, 80+93+89+104);
		count7.createVolume(10, 77+77+87+98);
		count7.createVolume(16, 110+115+97+100);
		count7.createVolume(17, 121+87+96+101);
		count7.createVolume(18, 84+101+85+70);
		count7.createVolume(19, 89+67+62+55);
		//Richtung Westen
		Count count8 = counts.createAndAddCount(Id.createLinkId("13027"), "Alleestraße Richtung Westen");
		count8.setCoord(network.getLinks().get(Id.createLinkId("13027")).getCoord());
		count8.createVolume(7, 10+13+20+36);
		count8.createVolume(8, 35+42+61+62);
		count8.createVolume(9, 46+59+82+78);
		count8.createVolume(10, 76+75+77+73);
		count8.createVolume(16, 94+94+105+77);
		count8.createVolume(17, 134+111+132+84);
		count8.createVolume(18, 140+86+99+111);
		count8.createVolume(19, 106+82+88+74);
		
		//Promenadenstraße
		//Richtung Süden
		Count count9 = counts.createAndAddCount(Id.createLinkId("-49944_0"), "Promenadenstraße Richtung Süden");
		count9.setCoord(network.getLinks().get(Id.createLinkId("-49944_0")).getCoord());
		count9.createVolume(7, 32+39+54+65);
		count9.createVolume(8, 70+103+117+155);
		count9.createVolume(9, 100+123+143+145);
		count9.createVolume(10, 130+130+137+156);
		count9.createVolume(16, 128+142+118+142);
		count9.createVolume(17, 170+157+159+140);
		count9.createVolume(18, 180+139+127+137);
		count9.createVolume(19, 139+132+115+125);
		//Richtung Norden
		Count count10 = counts.createAndAddCount(Id.createLinkId("574"), "Promenadenstraße Richtung Norden");
		count10.setCoord(network.getLinks().get(Id.createLinkId("574")).getCoord());
		count10.createVolume(7, 97+21+32+62);
		count10.createVolume(8, 74+68+106+117);
		count10.createVolume(9, 87+105+90+121);
		count10.createVolume(10, 113+98+138+120);
		count10.createVolume(16, 179+157+126+122);
		count10.createVolume(17, 188+128+144+179);
		count10.createVolume(18, 145+176+157+148);
		count10.createVolume(19, 160+133+107+122);
		
		//Loisachstraße
		//Richtung Osten
		Count count11 = counts.createAndAddCount(Id.createLinkId("9948"), "Loisachstraße Richtung Osten");
		count11.setCoord(network.getLinks().get(Id.createLinkId("9948")).getCoord());
		count11.createVolume(7, 11+7+12+16);
		count11.createVolume(8, 21+26+45+48);
		count11.createVolume(9, 39+53+45+50);
		count11.createVolume(10, 36+46+35+47);
		count11.createVolume(16, 37+44+47+52);
		count11.createVolume(17, 39+54+70+56);
		count11.createVolume(18, 76+49+35+44);
		count11.createVolume(19, 38+38+32+20);
		//Richtung Westen
		Count count12 = counts.createAndAddCount(Id.createLinkId("9949"), "Loisachstraße Richtung Westen");
		count12.setCoord(network.getLinks().get(Id.createLinkId("9949")).getCoord());
		count12.createVolume(7, 7+11+15+29);
		count12.createVolume(8, 36+35+67+49);
		count12.createVolume(9, 43+40+46+46);
		count12.createVolume(10, 44+38+31+30);
		count12.createVolume(16, 42+43+48+31);
		count12.createVolume(17, 54+42+51+35);
		count12.createVolume(18, 46+38+32+51);
		count12.createVolume(19, 45+38+48+55);
		
		//Haupstraße Nord
		//Richtung Süden
		Count count13 = counts.createAndAddCount(Id.createLinkId("3598"), "Haupstraße Nord Richtung Süden");
		count13.setCoord(network.getLinks().get(Id.createLinkId("3598")).getCoord());
		count13.createVolume(9, 98 + 768 + 30);
		count13.createVolume(17, 99 + 771 + 42);
		//Richtung Norden
		Count count14 = counts.createAndAddCount(Id.createLinkId("3599"), "Hauptstraße Nord Richtung Norden");
		count14.setCoord(network.getLinks().get(Id.createLinkId("3599")).getCoord());
		count14.createVolume(9, 66 + 541 + 22);
		count14.createVolume(17, 101 + 1169 + 38);
		
		//Unterfeldstraße
		//Richtung Osten
		Count count15 = counts.createAndAddCount(Id.createLinkId("7893"), "Unterfeldstraße Richtung Osten");
		count15.setCoord(network.getLinks().get(Id.createLinkId("7893")).getCoord());
		count15.createVolume(9, 66 + 12 + 53);
		count15.createVolume(17, 101 + 26 + 80);
		//Richtung Westen
		Count count16 = counts.createAndAddCount(Id.createLinkId("7894"), "Unterfeldstraße Richtung Westen");
		count16.setCoord(network.getLinks().get(Id.createLinkId("7894")).getCoord());
		count16.createVolume(9, 98 + 65 + 13);
		count16.createVolume(17, 99 + 70 + 15);
		
		//Ferdinand-Barth-Straße
		//Richtung Osten
		Count count17 = counts.createAndAddCount(Id.createLinkId("8750"), "Ferdinand-Barth-Straße Richtung Osten");
		count17.setCoord(network.getLinks().get(Id.createLinkId("8750")).getCoord());
		count17.createVolume(9, 30 + 12 + 9);
		count17.createVolume(17, 42 + 26 + 14);
		//Richtung Westen
		Count count18 = counts.createAndAddCount(Id.createLinkId("8749"), "Ferdinand-Barth-Straße Richtung Westen");
		count18.setCoord(network.getLinks().get(Id.createLinkId("8749")).getCoord());
		count18.createVolume(9, 22 + 13 + 11);
		count18.createVolume(17, 38 + 15 + 11);
		
		//Hindenburgstraße
		//Richtung Osten
		Count count19 = counts.createAndAddCount(Id.createLinkId("727"), "Hindenburgstraße Richtung Osten");
		count19.setCoord(network.getLinks().get(Id.createLinkId("727")).getCoord());
		count19.createVolume(9, 2 + 25 + 106);
		count19.createVolume(17, 6 + 50 + 170);
		//Richtung Westen
		Count count20 = counts.createAndAddCount(Id.createLinkId("728"), "Hindenburgstraße Richtung Westen");
		count20.setCoord(network.getLinks().get(Id.createLinkId("728")).getCoord());
		count20.createVolume(9, 11 + 18 + 106);
		count20.createVolume(17, 28 + 40 + 89);
		
		//Professor-Carl-Reiser-Straße
		//Richtung Norden
		Count count21 = counts.createAndAddCount(Id.createLinkId("761"), "Professor-Carl-Reiser-Straße Richtung Norden");
		count21.setCoord(network.getLinks().get(Id.createLinkId("761")).getCoord());
		count21.createVolume(9, 7 + 141 + 18);
		count21.createVolume(17, 6 + 169 + 40);
		//Richtung Süden
		Count count22 = counts.createAndAddCount(Id.createLinkId("762"), "Professor-Carl-Reiser-Straße Richtung Süden");
		count22.setCoord(network.getLinks().get(Id.createLinkId("762")).getCoord());
		count22.createVolume(9, 88 + 25 + 15);
		count22.createVolume(17, 108 + 50 + 15);
		
		//Hindenburgstraße
		//Richtung Osten
		Count count23 = counts.createAndAddCount(Id.createLinkId("22360"), "Hindenburgstraße Ost Richtung Osten");
		count23.setCoord(network.getLinks().get(Id.createLinkId("22360")).getCoord());
		count23.createVolume(9, 7 + 162 + 2);
		count23.createVolume(17, 6 + 198 + 6);
		//Richtung Westen
		Count count24 = counts.createAndAddCount(Id.createLinkId("22359"), "Hindenburgstraße Ost Richtung Westen");
		count24.setCoord(network.getLinks().get(Id.createLinkId("22359")).getCoord());
		count24.createVolume(9, 15 + 187 + 11);
		count24.createVolume(17, 15 + 385 + 28);
		//Richtung Osten
		Count count25 = counts.createAndAddCount(Id.createLinkId("745"), "Hindenburgstraße West Richtung Osten");
		count25.setCoord(network.getLinks().get(Id.createLinkId("745")).getCoord());
		count25.createVolume(9, 141 + 187 + 106);
		count25.createVolume(17, 169 + 385 + 170);
		//Richtung Westen
		Count count26 = counts.createAndAddCount(Id.createLinkId("746"), "Hindenburgstraße West Richtung Westen");
		count26.setCoord(network.getLinks().get(Id.createLinkId("746")).getCoord());
		count26.createVolume(9, 88 + 106 + 162);
		count26.createVolume(17, 108 + 89 + 198);
		
		//Bahnhofstraße
		//Richtung Norden
		Count count27 = counts.createAndAddCount(Id.createLinkId("1996"), "Bahnhofstraße Nord Richtung Norden");
		count27.setCoord(network.getLinks().get(Id.createLinkId("1996")).getCoord());
		count27.createVolume(9, 78 + 53 + 98);
		count27.createVolume(17, 109 + 99 + 138);
		//Richtung Norden
		Count count28 = counts.createAndAddCount(Id.createLinkId("2940"), "Bahnhofstraße Süd Richtung Norden");
		count28.setCoord(network.getLinks().get(Id.createLinkId("2940")).getCoord());
		count28.createVolume(9, 38 + 98);
		count28.createVolume(17, 48 + 136);
		//Richtung Süden
		Count count28a = counts.createAndAddCount(Id.createLinkId("2941"), "Bahnhofstraße Süd Richtung Süden");
		count28a.setCoord(network.getLinks().get(Id.createLinkId("2941")).getCoord());
		count28a.createVolume(9, 50 + 72);
		count28a.createVolume(17, 51 + 139);
		
		//Chamonixstraße
		//Richtung Osten
		Count count29 = counts.createAndAddCount(Id.createLinkId("29613"), "Chamonixstraße West Richtung Osten");
		count29.setCoord(network.getLinks().get(Id.createLinkId("29613")).getCoord());
		count29.createVolume(9, 53 + 72 + 108);
		count29.createVolume(17, 99 + 42 + 139);
		//Richtung Osten
		Count count30 = counts.createAndAddCount(Id.createLinkId("2959"), "Chamonixstraße Ost Richtung Osten");
		count30.setCoord(network.getLinks().get(Id.createLinkId("2959")).getCoord());
		count30.createVolume(9, 72 + 38);
		count30.createVolume(17, 42 + 48);
		//Richtung Westen
		Count count30a = counts.createAndAddCount(Id.createLinkId("2960"), "Chamonixstraße Ost Richtung Westen");
		count30a.setCoord(network.getLinks().get(Id.createLinkId("2960")).getCoord());
		count30a.createVolume(9, 78 + 50);
		count30a.createVolume(17, 109 + 51);
		
		//St.-Martin-Straße
		//Richtung Osten
		Count count31 = counts.createAndAddCount(Id.createLinkId("16017"), "St.-Martin-Straße Ost Richtung Osten");
		count31.setCoord(network.getLinks().get(Id.createLinkId("16017")).getCoord());
		count31.createVolume(9, 27 + 411 + 64);
		count31.createVolume(17, 49 + 597 + 132);
		//Richtung Westen
		Count count32 = counts.createAndAddCount(Id.createLinkId("2035"), "St.-Martin-Straße Ost Richtung Westen");
		count32.setCoord(network.getLinks().get(Id.createLinkId("2035")).getCoord());
		count32.createVolume(9, 46 + 470 + 85);
		count32.createVolume(17, 63 + 798 + 161);
		//Richtung Osten
		Count count33 = counts.createAndAddCount(Id.createLinkId("25274"), "St.-Martin-Straße West Richtung Osten");
		count33.setCoord(network.getLinks().get(Id.createLinkId("25274")).getCoord());
		count33.createVolume(9, 178 + 411 + 56);
		count33.createVolume(17, 231 + 597 + 69);
		//Richtung Westen
		Count count34 = counts.createAndAddCount(Id.createLinkId("20925"), "St.-Martin-Straße West Richtung Westen");
		count34.setCoord(network.getLinks().get(Id.createLinkId("20925")).getCoord());
		count34.createVolume(9, 198 + 26 + 470);
		count34.createVolume(17, 319 + 77 + 798);
		
		//Olympiastraße
		//Richtung Norden
		Count count35 = counts.createAndAddCount(Id.createLinkId("2032"), "Olympiastraße Nord Richtung Norden");
		count35.setCoord(network.getLinks().get(Id.createLinkId("2032")).getCoord());
		count35.createVolume(9, 46 + 178 + 41);
		count35.createVolume(17, 63 + 231 + 77);
		//Richtung Süden
		Count count36 = counts.createAndAddCount(Id.createLinkId("2031"), "Olympiastraße Nord Richtung Süden");
		count36.setCoord(network.getLinks().get(Id.createLinkId("2031")).getCoord());
		count36.createVolume(9, 198 + 39 + 27);
		count36.createVolume(17, 319 + 56 + 49);
		//Richtung Norden
		Count count37 = counts.createAndAddCount(Id.createLinkId("18761"), "Olympiastraße Süd Richtung Norden");
		count37.setCoord(network.getLinks().get(Id.createLinkId("18761")).getCoord());
		count37.createVolume(9, 26 + 41 + 64);
		count37.createVolume(17, 77 + 77 + 132);
		//Richtung Süden
		Count count38 = counts.createAndAddCount(Id.createLinkId("18760"), "Olympiastraße Süd Richtung Süden");
		count38.setCoord(network.getLinks().get(Id.createLinkId("18760")).getCoord());
		count38.createVolume(9, 39 + 56 + 85);
		count38.createVolume(17, 56 + 69 + 161);
		
		//St.-Martin-Straße
		//Richtung Osten
		Count count39 = counts.createAndAddCount(Id.createLinkId("20150"), "St.-Martin-Straße 2 Ost Richtung Osten");
		count39.setCoord(network.getLinks().get(Id.createLinkId("20150")).getCoord());
		count39.createVolume(9, 35 + 368 + 25);
		count39.createVolume(17, 66 + 461 + 27);
		//Richtung Westen
		Count count40 = counts.createAndAddCount(Id.createLinkId("20151"), "St.-Martin-Straße 2 Ost Richtung Westen");
		count40.setCoord(network.getLinks().get(Id.createLinkId("20151")).getCoord());
		count40.createVolume(9, 44 + 267 + 21);
		count40.createVolume(17, 76 + 531 + 43);
		//Richtung Osten
		Count count41 = counts.createAndAddCount(Id.createLinkId("20148"), "St.-Martin-Straße 2 West Richtung Osten");
		count41.setCoord(network.getLinks().get(Id.createLinkId("20148")).getCoord());
		count41.createVolume(9, 7 + 368 + 6);
		count41.createVolume(17, 5 + 461 + 5);
		//Richtung Westen
		Count count42 = counts.createAndAddCount(Id.createLinkId("20149"), "St.-Martin-Straße 2 West Richtung Westen");
		count42.setCoord(network.getLinks().get(Id.createLinkId("20149")).getCoord());
		count42.createVolume(9, 4 + 6 + 267);
		count42.createVolume(17, 11 + 531);
		
		//Reißerkopfstraße
		//Richtung Norden
		Count count43 = counts.createAndAddCount(Id.createLinkId("1730"), "Reißerkopfstraße Nord Richtung Norden");
		count43.setCoord(network.getLinks().get(Id.createLinkId("1730")).getCoord());
		count43.createVolume(9, 44 + 7 + 6);
		count43.createVolume(17, 76 + 5 + 2);
		//Richtung Süden
		Count count44 = counts.createAndAddCount(Id.createLinkId("1731"), "Reißerkopfstraße Nord Richtung Süden");
		count44.setCoord(network.getLinks().get(Id.createLinkId("1731")).getCoord());
		count44.createVolume(9, 4 + 5 + 35);
		count44.createVolume(17, 11 + 7 + 66);
		//Richtung Norden
		Count count45 = counts.createAndAddCount(Id.createLinkId("10285"), "Reißerkopfstraße Süd Richtung Norden");
		count45.setCoord(network.getLinks().get(Id.createLinkId("10285")).getCoord());
		count45.createVolume(9, 6 + 6 + 25);
		count45.createVolume(17, 2 + 27);
		//Richtung Süden
		Count count46 = counts.createAndAddCount(Id.createLinkId("10286"), "Reißerkopfstraße Süd Richtung Süden");
		count46.setCoord(network.getLinks().get(Id.createLinkId("10286")).getCoord());
		count46.createVolume(9, 5 + 6 + 21);
		count46.createVolume(17, 7 + 5 + 43);
		
		return counts;
		
	}
	
}
