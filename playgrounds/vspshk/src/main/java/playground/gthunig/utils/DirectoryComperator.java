package playground.gthunig.utils;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author gthunig on 25.04.2017.
 */
public class DirectoryComperator {
    private static final Logger log = Logger.getLogger(DirectoryComperator.class);

    public static void main(String[] args) throws IOException {
        File expected = new File("C:\\Users\\gthunig\\VSP\\runs-svn\\berlin_scenario_2016\\be_117j\\compare\\analysis_300_car");
        File generated = new File("C:\\Users\\gthunig\\VSP\\runs-svn\\berlin_scenario_2016\\be_117j\\compare\\analysis2_300_car");

        verifyDirsAreEqual(expected, generated);
    }

    public static void verifyDirsAreEqual(File expected, File generated)
            throws IOException {

        // Checks parameters
        assertTrue("Generated Folder doesn't exist: " + generated,generated.exists());
        assertTrue("Generated is not a folder?!?!: " + generated,generated.isDirectory());

        assertTrue("Expected Folder doesn't exist: " + expected,expected.exists());
        assertTrue("Expected is not a folder?!?!: " + expected,expected.isDirectory());

        Files.walkFileTree(expected.toPath(), new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir,
                                                     BasicFileAttributes attrs)
                    throws IOException {
                FileVisitResult result = super.preVisitDirectory(dir, attrs);

                // get the relative file name from path "expected"
                Path relativize = expected.toPath().relativize(dir);
                // construct the path for the counterpart file in "generated"
                File otherDir = generated.toPath().resolve(relativize).toFile();
                log.debug("=== preVisitDirectory === compare " + dir + " to " + otherDir);
                assertEquals("Folders doesn't contain same file!?!?",
                        Arrays.toString(dir.toFile().list()),
                        Arrays.toString(otherDir.list()));
                return result;
            }
            @Override
            public FileVisitResult visitFile(Path file,
                                             BasicFileAttributes attrs)
                    throws IOException {
                FileVisitResult result = super.visitFile(file, attrs);

                // get the relative file name from path "expected"
                Path relativize = expected.toPath().relativize(file);
                // construct the path for the counterpart file in "generated"
                File fileInOther = generated.toPath().resolve(relativize).toFile();
                log.debug("=== comparing: " + file + " to " + fileInOther);
                String expectedContents = FileUtils.readFileToString(file.toFile());
                String generatedContents = FileUtils.readFileToString(fileInOther);
                assertEquals("("+fileInOther+")  csv standard doesn't match expected ("+file+")!", expectedContents, generatedContents);
                return result;
            }
        });
    }
}
