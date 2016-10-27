package contrib.baseline.build;

public class CreateDefaultConfig {
	public static void main(String[] argv) {
		if (argv.length < 1) {
			System.out.println("First argument must be target file.");
			System.exit(1);
		}
		
		BuildConfig.saveConfig(argv[0], new BuildConfig());
	}
}
