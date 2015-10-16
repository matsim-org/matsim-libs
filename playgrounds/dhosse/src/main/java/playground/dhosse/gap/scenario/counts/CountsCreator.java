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
		Count count7 = counts.createAndAddCount(Id.createLinkId("13027"), "Alleestraße Richtung Osten");
		count7.setCoord(network.getLinks().get(Id.createLinkId("13027")).getCoord());
		count7.createVolume(7, 30+16+20+33);
		count7.createVolume(8, 41+56+90+96);
		count7.createVolume(9, 80+93+89+104);
		count7.createVolume(10, 77+77+87+98);
		count7.createVolume(16, 110+115+97+100);
		count7.createVolume(17, 121+87+96+101);
		count7.createVolume(18, 84+101+85+70);
		count7.createVolume(19, 89+67+62+55);
		//Richtung Westen
		Count count8 = counts.createAndAddCount(Id.createLinkId("13026"), "Alleestraße Richtung Westen");
		count8.setCoord(network.getLinks().get(Id.createLinkId("13026")).getCoord());
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
		
		Count count13 = counts.createAndAddCount(Id.createLinkId("3599"), "Hauptstraße Nord Ri Norden");
		count13.setCoord(network.getLinks().get(Id.createLinkId("3599")).getCoord());
		count13.createVolume(9, 629);
		count13.createVolume(17, 1308);
		Count count14 = counts.createAndAddCount(Id.createLinkId("3598"), "Hauptstraße Nord Ri Süden");
		count14.setCoord(network.getLinks().get(Id.createLinkId("3598")).getCoord());
		count14.createVolume(9, 896);
		count14.createVolume(17, 912);
		Count count15 = counts.createAndAddCount(Id.createLinkId("15493"), "Hauptstraße Süd Ri Norden");
		count15.setCoord(network.getLinks().get(Id.createLinkId("15493")).getCoord());
		count15.createVolume(9, 615);
		count15.createVolume(17, 1253);
		Count count16 = counts.createAndAddCount(Id.createLinkId("15492"), "Hauptstraße Süd Ri Süden");
		count16.setCoord(network.getLinks().get(Id.createLinkId("15492")).getCoord());
		count16.createVolume(9, 832);
		count16.createVolume(17, 863);
		Count count17 = counts.createAndAddCount(Id.createLinkId("7893"), "Unterfeldstraße Ri Osten");
		count17.setCoord(network.getLinks().get(Id.createLinkId("7893")).getCoord());
		count17.createVolume(9, 131);
		count17.createVolume(17, 207);
		Count count18 = counts.createAndAddCount(Id.createLinkId("7894"), "Unterfeldstraße Ri Westen");
		count18.setCoord(network.getLinks().get(Id.createLinkId("7894")).getCoord());
		count18.createVolume(9, 176);
		count18.createVolume(17, 184);
		Count count19 = counts.createAndAddCount(Id.createLinkId("8750"), "Ferdinand-Barth-Straße Ri Osten");
		count19.setCoord(network.getLinks().get(Id.createLinkId("8750")).getCoord());
		count19.createVolume(9, 46);
		count19.createVolume(17, 64);
		Count count20 = counts.createAndAddCount(Id.createLinkId("8749"), "Ferdinand-Barth-Straße Ri Westen");
		count20.setCoord(network.getLinks().get(Id.createLinkId("8749")).getCoord());
		count20.createVolume(9, 51);
		count20.createVolume(17, 82);
		
		Count count21 = counts.createAndAddCount(Id.createLinkId("3832"), "Alpspitzstraße Ri Süden");
		count21.setCoord(network.getLinks().get(Id.createLinkId("3832")).getCoord());
		count21.createVolume(7, 71);
		count21.createVolume(8, 135);
		count21.createVolume(9, 155);
		count21.createVolume(10, 198);
		count21.createVolume(11, 238);
		count21.createVolume(12, 259);
		count21.createVolume(13, 259);
		count21.createVolume(14, 219);
		count21.createVolume(15, 195);
		count21.createVolume(16, 209);
		count21.createVolume(17, 273);
		count21.createVolume(18, 264);
		count21.createVolume(19, 223);
		count21.createVolume(20, 148);
		
		Count count22 = counts.createAndAddCount(Id.createLinkId("3833"), "Alpspitzstraße Ri Norden");
		count22.setCoord(network.getLinks().get(Id.createLinkId("3833")).getCoord());
		count22.createVolume(7, 68);
		count22.createVolume(8, 153);
		count22.createVolume(9, 217);
		count22.createVolume(10, 250);
		count22.createVolume(11, 289);
		count22.createVolume(12, 322);
		count22.createVolume(13, 250);
		count22.createVolume(14, 244);
		count22.createVolume(15, 289);
		count22.createVolume(16, 205);
		count22.createVolume(17, 277);
		count22.createVolume(18, 288);
		count22.createVolume(19, 243);
		count22.createVolume(20, 177);
		
		Count count23 = counts.createAndAddCount(Id.createLinkId("22360"), "Krottenkopfstraße Ri Norden");
		count23.setCoord(network.getLinks().get(Id.createLinkId("22360")).getCoord());
		count23.createVolume(9, 213);
		count23.createVolume(17, 428);
		Count count24 = counts.createAndAddCount(Id.createLinkId("22359"), "Krottenkopfstraße Ri Süden");
		count24.setCoord(network.getLinks().get(Id.createLinkId("22359")).getCoord());
		count24.createVolume(9, 171);
		count24.createVolume(17, 210);
		Count count25 = counts.createAndAddCount(Id.createLinkId("746"), "Hindenburgstraße West Ri Osten");
		count25.setCoord(network.getLinks().get(Id.createLinkId("746")).getCoord());
		count25.createVolume(9, 434);
		count25.createVolume(17, 724);
		Count count26 = counts.createAndAddCount(Id.createLinkId("745"), "Hindenburgstraße West Ri Westen");
		count26.setCoord(network.getLinks().get(Id.createLinkId("745")).getCoord());
		count26.createVolume(9, 356);
		count26.createVolume(17, 395);
		Count count27 = counts.createAndAddCount(Id.createLinkId("727"), "Hindenburgstraße Ost Ri Osten");
		count27.setCoord(network.getLinks().get(Id.createLinkId("727")).getCoord());
		count27.createVolume(9, 133);
		count27.createVolume(17, 226);
		Count count28 = counts.createAndAddCount(Id.createLinkId("728"), "Hindenburgstraße Ost Ri Westen");
		count28.setCoord(network.getLinks().get(Id.createLinkId("728")).getCoord());
		count28.createVolume(9, 135);
		count28.createVolume(17, 157);
		Count count29 = counts.createAndAddCount(Id.createLinkId("761"), "Prof-Carl-Reisser-Straße Ri Norden");
		count29.setCoord(network.getLinks().get(Id.createLinkId("761")).getCoord());
		count29.createVolume(9, 166);
		count29.createVolume(17, 215);
		Count count30 = counts.createAndAddCount(Id.createLinkId("762"), "Prof-Carl-Reisser-Straße Ri Süden");
		count30.setCoord(network.getLinks().get(Id.createLinkId("762")).getCoord());
		count30.createVolume(9, 128);
		count30.createVolume(17, 173);
		
		Count count31 = counts.createAndAddCount(Id.createLinkId("16017"), "St.-Martin-Straße Ost Richtung Osten");
		count31.setCoord(network.getLinks().get(Id.createLinkId("16017")).getCoord());
		count31.createVolume(9, 502);
		count31.createVolume(17, 778);
		Count count32 = counts.createAndAddCount(Id.createLinkId("2035"), "St.-Martin-Straße Ost Richtung Westen");
		count32.setCoord(network.getLinks().get(Id.createLinkId("2035")).getCoord());
		count32.createVolume(9, 601);
		count32.createVolume(17, 1022);
		Count count33 = counts.createAndAddCount(Id.createLinkId("25274"), "St.-Martin-Straße West Richtung Osten");
		count33.setCoord(network.getLinks().get(Id.createLinkId("25274")).getCoord());
		count33.createVolume(9, 645);
		count33.createVolume(17, 897);
		Count count34 = counts.createAndAddCount(Id.createLinkId("20925"), "St.-Martin-Straße West Richtung Westen");
		count34.setCoord(network.getLinks().get(Id.createLinkId("20925")).getCoord());
		count34.createVolume(9, 535);
		count34.createVolume(17, 1194);
		Count count35 = counts.createAndAddCount(Id.createLinkId("2032"), "Olympiastraße Nord Richtung Norden");
		count35.setCoord(network.getLinks().get(Id.createLinkId("2032")).getCoord());
		count35.createVolume(9, 265);
		count35.createVolume(17, 371);
		Count count36 = counts.createAndAddCount(Id.createLinkId("2031"), "Olympiastraße Nord Richtung Süden");
		count36.setCoord(network.getLinks().get(Id.createLinkId("2031")).getCoord());
		count36.createVolume(9, 264);
		count36.createVolume(17, 424);
		Count count37 = counts.createAndAddCount(Id.createLinkId("18761"), "Olympiastraße Süd Richtung Norden");
		count37.setCoord(network.getLinks().get(Id.createLinkId("18761")).getCoord());
		count37.createVolume(9, 131);
		count37.createVolume(17, 286);
		Count count38 = counts.createAndAddCount(Id.createLinkId("18760"), "Olympiastraße Süd Richtung Süden");
		count38.setCoord(network.getLinks().get(Id.createLinkId("18760")).getCoord());
		count38.createVolume(9, 180);
		count38.createVolume(17, 286);
		
		Count count39 = counts.createAndAddCount(Id.createLinkId("20150"), "St.-Martin-Straße 2 Ost Richtung Osten");
		count39.setCoord(network.getLinks().get(Id.createLinkId("20150")).getCoord());
		count39.createVolume(9, 409);
		count39.createVolume(17, 554);
		Count count40 = counts.createAndAddCount(Id.createLinkId("20151"), "St.-Martin-Straße 2 Ost Richtung Westen");
		count40.setCoord(network.getLinks().get(Id.createLinkId("20151")).getCoord());
		count40.createVolume(9, 332);
		count40.createVolume(17, 650);
		Count count41 = counts.createAndAddCount(Id.createLinkId("20148"), "St.-Martin-Straße 2 West Richtung Osten");
		count41.setCoord(network.getLinks().get(Id.createLinkId("20148")).getCoord());
		count41.createVolume(9, 381);
		count41.createVolume(17, 471);
		Count count42 = counts.createAndAddCount(Id.createLinkId("20149"), "St.-Martin-Straße 2 West Richtung Westen");
		count42.setCoord(network.getLinks().get(Id.createLinkId("20149")).getCoord());
		count42.createVolume(9, 277);
		count42.createVolume(17, 542);
		Count count43 = counts.createAndAddCount(Id.createLinkId("1730"), "Reißerkopfstraße Nord Richtung Norden");
		count43.setCoord(network.getLinks().get(Id.createLinkId("1730")).getCoord());
		count43.createVolume(9, 57);
		count43.createVolume(17, 87);
		Count count44 = counts.createAndAddCount(Id.createLinkId("1731"), "Reißerkopfstraße Nord Richtung Süden");
		count44.setCoord(network.getLinks().get(Id.createLinkId("1731")).getCoord());
		count44.createVolume(9, 44);
		count44.createVolume(17, 84);
		Count count45 = counts.createAndAddCount(Id.createLinkId("10285"), "Reißerkopfstraße Süd Richtung Norden");
		count45.setCoord(network.getLinks().get(Id.createLinkId("10285")).getCoord());
		count45.createVolume(9, 31);
		count45.createVolume(17, 29);
		Count count46 = counts.createAndAddCount(Id.createLinkId("10286"), "Reißerkopfstraße Süd Richtung Süden");
		count46.setCoord(network.getLinks().get(Id.createLinkId("10286")).getCoord());
		count46.createVolume(9, 62);
		count46.createVolume(17, 55);
		
		return counts;
		
	}
	
}
