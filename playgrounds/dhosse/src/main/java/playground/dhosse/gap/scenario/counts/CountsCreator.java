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
		//Olympiastraße
		//Richtung Süden
		Count count = counts.createAndAddCount(Id.createLinkId("2669"), "Olympiastr Richtung St-Martin-Str");
		count.createVolume(7, 7+8+9+13);
		count.createVolume(8, 16+22+25+40);
		count.createVolume(9, 18+30+36+40);
		count.createVolume(10, 37+41+41+41);
		count.createVolume(16, 45+49+45+70);
		count.createVolume(17, 66+65+66+56);
		count.createVolume(18, 48+69+63+50);
		count.createVolume(19, 48+34+31+24);
		//Richtung Norden
		Count count2 = counts.createAndAddCount(Id.createLinkId("2670"), "Olympiastr Richtung Landratsamt");
		count2.createVolume(7, 12+5+17+24);
		count2.createVolume(8, 26+45+66+62);
		count2.createVolume(9, 48+69+76+92);
		count2.createVolume(10, 79+77+76+72);
		count2.createVolume(16, 65+63+72+72);
		count2.createVolume(17, 81+77+71+79);
		count2.createVolume(18, 84+61+70+62);
		count2.createVolume(19, 58+70+30+53);
		
		//Alpspitzstraße
		//Richtung Süden
		Count count3 = counts.createAndAddCount(Id.createLinkId("5129"), "Alpspitzstr Richtung Süden");
		count3.createVolume(7, 15+10+23+21);
		count3.createVolume(8, 28+30+48+49);
		count3.createVolume(9, 45+45+61+69);
		count3.createVolume(10, 51+65+70+67);
		count3.createVolume(11, 91+51+72+78);
		count3.createVolume(12, 84+84+79+79);
		count3.createVolume(13, 66+67+65+55);
		count3.createVolume(14, 73+52+68+54);
		count3.createVolume(15, 59+75+72+86);
		count3.createVolume(16, 62+34+39+73);
		count3.createVolume(17, 62+71+76+71);
		count3.createVolume(18, 81+61+65+84);
		count3.createVolume(19, 61+57+68+60);
		count3.createVolume(20, 50+45+43+41);
		//Richtung Norden
		Count count4 = counts.createAndAddCount(Id.createLinkId("5128"), "Alpspitzstr Richtung Norden");
		count4.createVolume(7, 11+16+22+23);
		count4.createVolume(8, 14+24+37+62);
		count4.createVolume(9, 45+41+37+34);
		count4.createVolume(10, 52+51+50+47);
		count4.createVolume(11, 69+58+51+63);
		count4.createVolume(12, 64+77+65+56);
		count4.createVolume(13, 85+60+48+69);
		count4.createVolume(14, 71+45+50+56);
		count4.createVolume(15, 47+45+61+44);
		count4.createVolume(16, 55+53+54+49);
		count4.createVolume(17, 70+51+78+76);
		count4.createVolume(18, 66+71+71+59);
		count4.createVolume(19, 74+59+50+43);
		count4.createVolume(20, 45+41+31+33);
		
		//Burgstraße
		//Burgstraße Richtung Süden
		Count count5 = counts.createAndAddCount(Id.createLinkId("22050"), "Burgstr Richtung Süden");
		count5.createVolume(7, 28+43+57+72);
		count5.createVolume(8, 85+119+140+170);
		count5.createVolume(9, 130+118+139+139);
		count5.createVolume(10, 115+123+122+148);
		count5.createVolume(16, 111+139+106+140);
		count5.createVolume(17, 157+130+131+126);
		count5.createVolume(18, 133+132+113+124);
		count5.createVolume(19, 131+115+103+127);
		//Burgstraße Richtung Norden
		Count count6 = counts.createAndAddCount(Id.createLinkId("16043"), "Burgstr Richtung Norden");
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
		Count count7 = counts.createAndAddCount(Id.createLinkId("16083"), "Alleestraße Richtung Osten");
		count7.createVolume(7, 30+16+20+33);
		count7.createVolume(8, 41+56+90+96);
		count7.createVolume(9, 80+93+89+104);
		count7.createVolume(10, 77+77+87+98);
		count7.createVolume(16, 110+115+97+100);
		count7.createVolume(17, 121+87+96+101);
		count7.createVolume(18, 84+101+85+70);
		count7.createVolume(19, 89+67+62+55);
		//Richtung Westen
		Count count8 = counts.createAndAddCount(Id.createLinkId("16082"), "Alleestraße Richtung Westen");
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
		Count count9 = counts.createAndAddCount(Id.createLinkId("27194"), "Promenadenstraße Richtung Süden");
		count9.createVolume(7, 32+39+54+65);
		count9.createVolume(8, 70+103+117+155);
		count9.createVolume(9, 100+123+143+145);
		count9.createVolume(10, 130+130+137+156);
		count9.createVolume(16, 128+142+118+142);
		count9.createVolume(17, 170+157+159+140);
		count9.createVolume(18, 180+139+127+137);
		count9.createVolume(19, 139+132+115+125);
		//Richtung Norden
		Count count10 = counts.createAndAddCount(Id.createLinkId("798"), "Promenadenstraße Richtung Norden");
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
		Count count11 = counts.createAndAddCount(Id.createLinkId("12348"), "Loisachstraße Richtung Osten");
		count11.createVolume(7, 11+7+12+16);
		count11.createVolume(8, 21+26+45+48);
		count11.createVolume(9, 39+53+45+50);
		count11.createVolume(10, 36+46+35+47);
		count11.createVolume(16, 37+44+47+52);
		count11.createVolume(17, 39+54+70+56);
		count11.createVolume(18, 76+49+35+44);
		count11.createVolume(19, 38+38+32+20);
		//Richtung Westen
		Count count12 = counts.createAndAddCount(Id.createLinkId("12347"), "Loisachstraße Richtung Westen");
		count12.createVolume(7, 7+11+15+29);
		count12.createVolume(8, 36+35+67+49);
		count12.createVolume(9, 43+40+46+46);
		count12.createVolume(10, 44+38+31+30);
		count12.createVolume(16, 42+43+48+31);
		count12.createVolume(17, 54+42+51+35);
		count12.createVolume(18, 46+38+32+51);
		count12.createVolume(19, 45+38+48+55);
		
		return counts;
		
	}
	
}
