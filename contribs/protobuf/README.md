## Protobuf

The protobuf contrib provides a protocol buffer implementation and converter for the MATSim event infrastructure.
 
## Usage

Add `pb` to the `eventsFileFormat` in your controller config.

	<module name="controler">
		<param name="outputDirectory" value="./output/example" />
		<param name="firstIteration" value="0" />
		<param name="lastIteration" value="1" />
		<param name="eventsFileFormat" value="xml,pb"/>
	</module>


The resulting files can be read with the MATSim python package and offer better
performance than the xml variant.