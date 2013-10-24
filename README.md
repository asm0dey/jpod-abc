# JPod: MP3 to M4B audiobook converter

Audiobook converter, written in kotlin. Needs lame, nero aac encoder, mp4box, mp4chaps

It's crossplatform - you can use it everywhere, where binaries of lame and so 
on. 

## 1. Build from source

To build it from source you just need to run `mvn clean package`  and you're done. It's obvious from command, that you need to have Maven installed to build. 

Than to launch binary you need to call `target/m4bconverter-1.0-SNAPSHOT-jar-with-dependencies.jar` with one of two options:

  1. `java -jar m4bconverter-1.0-SNAPSHOT-jar-with-dependencies.jar`
  2. Right click on file and run with java, if it's supported in your OS

## 2. Capabilities

  * Any number of source mp3 files
  * Chaptering
  * Progress indication (only n console for now - will be improved)
  * Automatic selection of output book quality
  * Random names of chapters - defaults to ID3 name, but one can change it
  
## 3. Known issues

  * No settings - one cannot set paths to binaries
  * No support for custom output bitrate
  * No support for chaptering, not binded to mp3 timings
  * No support for tagging output file
  * No warning if file is too long for iPod — border is ≈11h 30m
