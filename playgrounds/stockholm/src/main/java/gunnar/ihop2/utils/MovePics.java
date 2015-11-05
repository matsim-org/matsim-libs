package gunnar.ihop2.utils;
import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

public class MovePics {

	public MovePics() {
	}

	public static void main(String[] args) throws IOException {

		for (int i = 0; i <= 200; i++) {
			System.out.println(i);
			final File from = new File(
					"./test/regentmatsim/matsim-output/ITERS/it." + i + "/" + i
							+ ".legHistogram_car.png");
			final File to = new File("./test/regentmatsim/hist" + i + ".png");

			FileUtils.copyFile(from, to);
			
		}
	}

}
