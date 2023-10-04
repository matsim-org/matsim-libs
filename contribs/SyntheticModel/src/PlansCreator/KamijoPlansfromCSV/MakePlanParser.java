package PlansCreator.KamijoPlansfromCSV;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;


public class MakePlanParser {
	private String separator = ",";
	private Charset charset = Charset.forName("UTF-8");

	public List<MakePlanEntry> readFile(String inFile)
	{
		List<MakePlanEntry> entries = new ArrayList<MakePlanEntry>();

		FileInputStream fis = null;
		InputStreamReader isr = null;
	    BufferedReader br = null;

    	try
    	{
    		fis = new FileInputStream(inFile);
    		isr = new InputStreamReader(fis, charset);
			br = new BufferedReader(isr);

			// skip first Line
			br.readLine();

			String line;
			while((line = br.readLine()) != null)
			{
				MakePlanEntry censusEntry = new MakePlanEntry();

				String[] cols = line.split(separator);

				censusEntry.id_person = parseInteger(cols[0]);
				censusEntry.starttime = (int) parseDouble(cols[1]);
				censusEntry.s_x = parseDouble(cols[2]);
				censusEntry.s_y = parseDouble(cols[3]);
				censusEntry.d_x = parseDouble(cols[4]);
				censusEntry.d_y = parseDouble(cols[5]);
				entries.add(censusEntry);
			}

			br.close();
			isr.close();
			fis.close();
    	}
    	catch (FileNotFoundException e)
    	{
			e.printStackTrace();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}

		return entries;
	}

	private int parseInteger(String string)
	{
		if (string == null) return 0;
		else if (string.trim().isEmpty()) return 0;
		else return Integer.valueOf(string);
	}

	private double parseDouble(String string)
	{
		if (string == null) return 0.0;
		else if (string.trim().isEmpty()) return 0.0;
		else return Double.valueOf(string);
	}
}
