package playground.artemc.utils;

import java.io.File;

/**
 * Created by artemc on 7/3/15.
 */
public class OutputToSQLReader {

	public void main(String[] args)
	{
		String path = "/Volumes/DATA 1 (WD 2 TB)/SimMethanaOutput_120215/";
		File directory = new File(path);

		//get all the files from a directory
		File[] fList = directory.listFiles();

		for (File file : fList) {
			if (file.isFile()) {
				System.out.println(file.getAbsolutePath());


			} else if (file.isDirectory()) {

			}
		}
	}
}
