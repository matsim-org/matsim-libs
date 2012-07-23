package playground.mkillat.staedtebau;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Time;
import java.util.ArrayList;
import java.util.List;

import playground.mkillat.tmc.StringTimeToDouble;


public class KordonElementReader {
	public static List <List <KordonElement>>  read(String filename){

		List <KordonElement> kd1_rein = new ArrayList <KordonElement>();
		List <KordonElement> kd1_raus= new ArrayList <KordonElement>();
		List <KordonElement> kd2_rein = new ArrayList <KordonElement>();
		List <KordonElement> kd2_raus = new ArrayList <KordonElement>();
		List <KordonElement> kd3_rein = new ArrayList <KordonElement>();
		List <KordonElement> kd3_raus = new ArrayList <KordonElement>();
		List <KordonElement> kd4_rein = new ArrayList <KordonElement>();
		List <KordonElement> kd4_raus = new ArrayList <KordonElement>();
		List <KordonElement> kd5_rein = new ArrayList <KordonElement>();
		List <KordonElement> kd5_raus = new ArrayList <KordonElement>();
		List <KordonElement> kd6_rein = new ArrayList <KordonElement>();
		List <KordonElement> kd6_raus = new ArrayList <KordonElement>();
		List <KordonElement> kd7_rein = new ArrayList <KordonElement>();
		List <KordonElement> kd7_raus = new ArrayList <KordonElement>();
		List <KordonElement> kd8_rein = new ArrayList <KordonElement>();
		List <KordonElement> kd8_raus = new ArrayList <KordonElement>();
		
		FileReader fr;
		BufferedReader br;
		try {
			fr = new FileReader(new File (filename));
			br = new BufferedReader(fr);
			String line = null;
			br.readLine(); //Erste Zeile (Kopfzeile) wird �bersprungen.
			br.readLine(); //Erste Zeile (Kopfzeile) wird �bersprungen.
			while ((line = br.readLine()) != null) {
				String[] result = line.split(";");

				String timeS = result[0];
				StringTimeToDouble aa = new StringTimeToDouble();
				double time = aa.transformer(timeS);
				
				KordonElement kd1_1K = new KordonElement(result[1], time);
				kd1_rein.add(kd1_1K);
				KordonElement kd1_2K = new KordonElement(result[2], time);
				kd1_rein.add(kd1_2K);
				KordonElement kd1_3K = new KordonElement(result[3], time);
				kd1_raus.add(kd1_3K);
				KordonElement kd2_123K = new KordonElement(result[4], time);
				kd2_raus.add(kd2_123K);
				KordonElement kd2_4K = new KordonElement(result[5], time);
				kd2_rein.add(kd2_4K);
				KordonElement kd2_5K = new KordonElement(result[6], time);
				kd2_rein.add(kd2_5K);
				KordonElement kd2_6K = new KordonElement(result[7], time);
				kd2_rein.add(kd2_6K);
				KordonElement kd3_1K = new KordonElement(result[8], time);
				kd3_raus.add(kd3_1K);
				KordonElement kd3_3K = new KordonElement(result[9], time);
				kd3_rein.add(kd3_3K);
				KordonElement kd3_4K = new KordonElement(result[10], time);
				kd3_rein.add(kd3_4K);
				KordonElement kd4_1K = new KordonElement(result[11], time);
				kd4_raus.add(kd4_1K);
				KordonElement kd4_2K = new KordonElement(result[12], time);
				kd4_raus.add(kd4_2K);
				KordonElement kd4_3K = new KordonElement(result[13], time);
				kd4_raus.add(kd4_3K);
				KordonElement kd4_4K = new KordonElement(result[14], time);
				kd4_rein.add(kd4_4K);
				KordonElement kd4_5K = new KordonElement(result[15], time);
				kd4_rein.add(kd4_5K);
				KordonElement kd4_6K = new KordonElement(result[16], time);
				kd4_rein.add(kd4_6K);
				KordonElement kd5_1K = new KordonElement(result[17], time);
				kd5_rein.add(kd5_1K);
				KordonElement kd5_2K = new KordonElement(result[18], time);
				kd5_rein.add(kd5_2K);
				KordonElement kd5_3K = new KordonElement(result[19], time);
				kd5_rein.add(kd5_3K);
				KordonElement kd5_4K = new KordonElement(result[20], time);
				kd5_raus.add(kd5_4K);
				KordonElement kd5_5K = new KordonElement(result[21], time);
				kd5_raus.add(kd5_5K);
				KordonElement kd5_6K = new KordonElement(result[22], time);
				kd5_raus.add(kd5_6K);
				KordonElement kd6_1K = new KordonElement(result[23], time);
				kd6_rein.add(kd6_1K);
				KordonElement kd6_2K = new KordonElement(result[24], time);
				kd6_rein.add(kd6_2K);
				KordonElement kd6_3K = new KordonElement(result[25], time);
				kd6_rein.add(kd6_3K);
				KordonElement kd6_4K = new KordonElement(result[26], time);
				kd6_raus.add(kd6_4K);
				KordonElement kd6_5K = new KordonElement(result[27], time);
				kd6_raus.add(kd6_5K);
				KordonElement kd6_6K = new KordonElement(result[28], time);
				kd6_raus.add(kd6_6K);
				KordonElement kd7_1K = new KordonElement(result[29], time);
				kd7_rein.add(kd7_1K);
				KordonElement kd7_2K = new KordonElement(result[30], time);
				kd7_rein.add(kd7_2K);
				KordonElement kd7_3K = new KordonElement(result[31], time);
				kd7_rein.add(kd7_3K);
				KordonElement kd7_4K = new KordonElement(result[32], time);
				kd7_raus.add(kd7_4K);
				KordonElement kd7_5K = new KordonElement(result[33], time);
				kd7_raus.add(kd7_5K);
				KordonElement kd7_6K = new KordonElement(result[34], time);
				kd7_raus.add(kd7_6K);
				KordonElement kd8_1K = new KordonElement(result[35], time);
				kd8_rein.add(kd8_1K);
				KordonElement kd8_2K = new KordonElement(result[36], time);
				kd8_rein.add(kd8_2K);
				KordonElement kd8_3K = new KordonElement(result[37], time);
				kd8_raus.add(kd8_3K);
				KordonElement kd8_4K = new KordonElement(result[38], time);
				kd8_raus.add(kd8_4K);
				
				
				
		
		}
		
	} catch (FileNotFoundException e) {
		System.err.println("File not found...");
			e.printStackTrace();
	} catch (NumberFormatException e) {
		System.err.println("Wrong No. format...");
		e.printStackTrace();
	} catch (IOException e) {
		System.err.println("I/O error...");
		e.printStackTrace();
		}
		return null;
	}
	

	
}
	
