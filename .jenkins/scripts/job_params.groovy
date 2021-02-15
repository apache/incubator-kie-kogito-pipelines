// Map <paramName, jenkinsparam>
Map parameters = [:]

void addStringParam(String key, String value) {
  parameters.put(key, string(name: key, value: value))
}

void addBooleanParam(String key, boolean value) {
  parameters.put(key, booleanParam(name: key, value: value))
}

// Do not remove this below line as fundamental to reuse func in this script
return this