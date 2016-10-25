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

* Put native files in the folder with rfid.jar and config.properties. 
  Alternative is to put the path to the .dll files into the windows environment path. 
* java -jar rfid.jar

## Running on Linux

* Install the .so files. Use the install-libs.sh script.
