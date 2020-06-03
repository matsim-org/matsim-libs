# MATSim Distribution

To add a contrib to the MATSim distribution:

1. make sure your contrib creates a `*-release.zip` when calling `mvn clean package`
2. add your contrib as a dependency to this project's `pom.xml` (please in alphabetical order, it helps to keep a good overview)
3. list your contrib as a file in `src/main/assembly/assembly-release.xml` (again, please maintain the alphabetical order) 

(Step 2 is necessary to ensure that this module's packaging phase runs after all the other modules that provide a `release.zip`.)