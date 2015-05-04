package others.sergioo.util.text;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

public class ConsecutiveDays {

	public static void main(String[] args) throws IOException, ParseException {
		Map<String, SortedSet<Date>> users = new HashMap<String, SortedSet<Date>>();
		SimpleDateFormat format = new SimpleDateFormat("\"yyyy-mm-dd\"");
		BufferedReader reader = new BufferedReader(new FileReader(args[0]));
		reader.readLine();
		String line = reader.readLine();
		while(line!=null) {
			String[] parts = line.split(",");
			SortedSet<Date> dates = users.get(parts[1]);
			if(dates==null) {
				dates = new TreeSet<>();
				users.put(parts[1], dates);
			}
			dates.add(format.parse(parts[0]));
			line = reader.readLine();
		}
		reader.close();
		Collection<Integer> cons = new ArrayList<>();
		for(SortedSet<Date> dates:users.values()) {
			Date prevDate = null;
			int numCons = 0;
			for(Date date:dates) {
				if(prevDate!=null) {
					int days = (int)((date.getTime()-prevDate.getTime())/(1000*60*60*24));
					if(days==1)
						numCons++;
				}
				prevDate = date;
			}
			cons.add(numCons);
		}
		PrintWriter writer = new PrintWriter(args[1]);
		for(Integer num:cons) {
			writer.println(num);
			System.out.println(num);
		}
		writer.close();
	}

}
