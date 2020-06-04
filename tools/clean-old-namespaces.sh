#!/bin/bash

IN=$(kubectl get namespace -o jsonpath='{range .items[*].metadata}{.name}{";"}{.creationTimestamp}{"\n"}{end}' | awk '/cucumber-/')
projects=$(echo $IN | tr "\n" "\n")
date_cleanup=$(date -u +"%Y-%m-%dT%H:%M:%SZ" -d "1 day ago")
for values in $projects
do
    name_project=${values%;*}
    date_project=${values#*;}
    if [[ "$date_project" < "$date_cleanup"  ]] ; then
	oc delete project $name_project
	echo "delete project ${name_project}"
    fi
done    
