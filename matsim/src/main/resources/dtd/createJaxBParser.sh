#!/bin/bash
# script to create jaxb bindings for a given schmema and its bindings
# editor dgrether

function print_help()
{
  echo "Wrong number of parameters!"
  echo "Expected parameters:"
  echo "  1.) base dir of jaxb installation, containing the folder bin."
  echo "  2.) bindings file for the schema"
  echo "  3.) schema file"
}

if [ "$#" = 3 ]; then
command="$1/bin/xjc.sh -d ../src/main/java/  -b $2 xmlschemaBindings.xsd $3"
echo $command
$command
else
  print_help
fi
