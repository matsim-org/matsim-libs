This contribution was created to contain integration tests.
Integration tests are tests that verify that the tested functionality is 
working correctly on a high (complex) level. This often involves running
full scenarios and checking the output of the scenarios. As such tests 
usually run for a while, they are executed less often than other, regular
unit tests.

This contrib-project supports running integration tests either once per day
("daily") or once a week ("weekly") on the MATSim build server.


To add an integration test:

- add a new class into either the "daily" or the "weekly" package. 
  Sub-packages (e.g. org.matsim.integration.daily.mycode.MyTest.java) are supported.
- make sure the class name ends in "Test".
- annotate your test methods with "@Test" (import org.junit.Test).
- write your test functionality, including assert statements from JUnit (import org.junit.jupiter.api.assertions).


To run the daily/weekly tests locally:

- You can run specific test classes from within Eclipse as you always would do.
- To run all daily/weekly tests, similar to what the build server does:
  - in Eclipse:
    - Create a new Run Configuration of type "Maven Build"
    - set the Base directory to the directory of the integration contrib
    - set Goals to "test" (without quotes)
    - set Profiles to "daily" or "weekly"
    - run it! The output will appear in the Console, not in the JUnit-View of Eclipse.
  - on the command line:
    - make sure all dependencies are up-to-date and locally installed:
      - cd matsim
      - mvn install -DskipTests=true
      - cd ../contrib
      - mvn install -DskipTests=true
    - then run the test with the corresponding maven profile:
      - cd integration
      - mvn test -Pdaily
      - mvn test -Pweekly



mrieser/nov-2013