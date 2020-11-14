## Description

`denelezh-import` is a Java project that handles the first step of import from Wikidata JSON dumps to Denelezh, generating CSV files using Wikidata Toolkit.

## Installation
Ensure that java is installed.
    
    sudo apt install default-jdk

Use Maven to generate the binary `target/denelezh-import.jar`:

    mvn clean install

## Configuration

There is no configuration file. If you want to change the directories used by this software, you will need to modify the lines at the beginning of `src/main/java/net/lehir/denelezh/Main.java` and then regenerate a binary (see § Installation):

    private static final String DUMP_DIRECTORY = "/home/wikidata/";
    private static final String TMP_DIRECTORY = "/tmp/";

## Copyright

This project is under AGPLv3 license. See LICENSE and NOTICE files.

## See also

* [denelezh-core](https://framagit.org/wikimedia-france/denelezh-core)
