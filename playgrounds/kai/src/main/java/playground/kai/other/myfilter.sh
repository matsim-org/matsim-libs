#!/bin/bash
sed 's/<[a-zA-Z0-9,? ]*>//g' $* | sed 's/, Serializable//' | sed 's/final //g'