#!/bin/bash
# script to create jaxb bindings for a given schmema and its bindings
# editor dgrether
# modifier gregor

function print_help()
{
  echo "Wrong number of parameters!"
  echo "Expected parameters:"
  echo "  1.) bindings file for the schema"
  echo "  2.) schema file"
}

if [ "$#" = 2 ]; then
command="xjc -d ../src/main/java/  -b $1 -b gml.jxb xmlschemaBindings.xsd $2"
echo $command
$command
else
  print_help
fi
