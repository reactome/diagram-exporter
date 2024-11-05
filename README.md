[<img src=https://user-images.githubusercontent.com/6883670/31999264-976dfb86-b98a-11e7-9432-0316345a72ea.png height=75 />](https://reactome.org)

Diagram Exporter
=====================

### Introduction
This project includes methods for exporting a given diagram into an image (png, jpeg or jpeg), a PowerPoint file or to Systems Biology Graphical Notation ([SBGN](http://sbgn.github.io/sbgn/)).

 * [Power point exporter](src/main/java/org/reactome/server/tools/diagram/exporter/pptx/README.md)
 * [Raster exporter](src/main/java/org/reactome/server/tools/diagram/exporter/raster/README.md)
 * [SBGN exporter](src/main/java/org/reactome/server/tools/diagram/exporter/sbgn/README.md)


### Pre-requirements
* Maven 3.x.x
* Java 11

### Nexus / Maven Repo deploy (only when needed)

This particular project has the PowerPoint license ($$$) and it shouldn't be uploaded to Nexus/Maven repo. That changes the way the project is packaged before Maven uploads it.
The pom.xml has a specific profile that tweak few properties and deploy. Note: ```mvn deploy``` simply will be skipped.

```console
mvn -Psafe-deploy deploy
```

### Clone and install

```console
git clone https://github.com/reactome/diagram-exporter.git
cd diagram-exporter
mvn clean package
```

### Usage as command line

This exporter is meant to be executed as a step of the release process to generate the SBGN and SVG files for the pathway diagrams in 'Homo sapiens'.

To find out the parameters, please use the "--help" option:

```console
$java -jar diagram-exporter-exec.jar --help 

Usage:
  org.reactome.server.tools.diagram.exporter.Main [--help]
  (-t|--target)[:target1,target2,...,targetN ] (-f|--format) <format>
  (-o|--output) <output> (-i|--input) <input> (-e|--ehld) <ehlds> (-s|--summary)
  <summary> [(-h|--host) <host>] [(-p|--port) <port>] [(-u|--user) <user>]
  (-w|--password) <password> [(-c|--profile) <profile>] [(-l|--license) <license>]
  [(-v|--verbose)[:<verbose>]]

Exports the requested pathway diagrams to different formats (svg, png, sbgn, pptx, gif, jpeg)


  [--help]
        Prints this help message.

  (-t|--target)[:target1,target2,...,targetN ]
        Target pathways to convert. Use either comma separated IDs, pathways for
        a given species (e.g. 'Homo sapiens') or 'all' for every pathway

  (-f|--format) <format>
        Format of the output files (svg, png, sbgn, pptx, gif, jpeg)

  (-o|--output) <output>
        The output folder

  (-i|--input) <input>
        The input folder containing the diagram json files

  (-e|--ehld) <ehlds>
        The folder containing the EHLD svg files

  (-s|--summary) <summary>
        The file containing the summary of pathways with EHLD assigned

  [(-h|--host) <host>]
        The neo4j host (default: localhost)

  [(-p|--port) <port>]
        The neo4j port (default: 7474)

  [(-u|--user) <user>]
        The neo4j user (default: neo4j)

  (-w|--password) <password>
        The neo4j password (default: neo4j)

  [(-c|--profile) <profile>]
        The colour diagram [Modern or Standard] (default: Modern)

  [(-l|--license) <license>]
        Software License file

  [(-v|--verbose)[:<verbose>]]
        Requests verbose output.

```

To generate the SVG files for 'Homo sapiens' the command line is like this:

```console
java -jar diagram-exporter-exec.jar \
            -t "Homo sapiens" \  
            -f svg \
            -o /path/to/output \
            -i /path/to/folder/with/diagram/json/files \
            -e /path/to/folder/containing/ehld/files \
            -s /path/to/svgsummary.txt \
            -w neo4jPasswrd \
            -v
```

To generate the SBGN files for 'Homo sapiens' the command line is like this:

```console
java -jar diagram-exporter-exec.jar \
            -t "Homo sapiens" \  
            -f sbgn \
            -o /path/to/output \
            -i /path/to/folder/with/diagram/json/files \
            -w neo4jPassword \
            -v
```

### Adding Diagram Exporter in your project

#### Maven Dependency

```html
<dependency>
    <groupId>org.reactome.server.tools</groupId>
    <artifactId>diagram-exporter</artifactId>
    <version>2.0.6</version>
</dependency>
```

```html
<!-- EBI repo -->
<repository>
    <id>pst-release</id>
    <name>EBI Nexus Repository</name>
    <url>https://www.ebi.ac.uk/Tools/maven/repos/content/repositories/pst-release</url>
</repository>
```
