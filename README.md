# RFID

## Project setup

* Open project in eclipse: https://eclipse.org/
* Add jar libraries to lib/

## Build jar

* File > Export
* Java > Runnable jar
  - Launch configuration: Driver - RFID
  - Export destination: RFID/export/rfid.jar
  - Library handling: Package required libraries into generated JAR
  - > Finish

## Running on Windows

* Put native files in folder with rfid.jar and config.properties.
* java -jar rfid.jar

## Running on Linux