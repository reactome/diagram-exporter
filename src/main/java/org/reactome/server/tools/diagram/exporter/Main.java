package org.reactome.server.tools.diagram.exporter;

import com.martiansoftware.jsap.*;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.commons.io.IOUtils;
import org.reactome.server.graph.domain.model.Pathway;
import org.reactome.server.graph.exception.CustomQueryException;
import org.reactome.server.graph.service.AdvancedDatabaseObjectService;
import org.reactome.server.graph.service.util.DatabaseObjectUtils;
import org.reactome.server.graph.utils.ReactomeGraphCore;
import org.reactome.server.tools.diagram.data.DiagramFactory;
import org.reactome.server.tools.diagram.data.exception.DeserializationException;
import org.reactome.server.tools.diagram.data.layout.Diagram;
import org.reactome.server.tools.diagram.exporter.common.Decorator;
import org.reactome.server.tools.diagram.exporter.common.analysis.AnalysisException;
import org.reactome.server.tools.diagram.exporter.common.profiles.factory.DiagramJsonDeserializationException;
import org.reactome.server.tools.diagram.exporter.common.profiles.factory.DiagramJsonNotFoundException;
import org.reactome.server.tools.diagram.exporter.common.profiles.factory.DiagramProfileException;
import org.reactome.server.tools.diagram.exporter.pptx.PowerPointExporter;
import org.reactome.server.tools.diagram.exporter.raster.RasterExporter;
import org.reactome.server.tools.diagram.exporter.raster.api.RasterArgs;
import org.reactome.server.tools.diagram.exporter.raster.ehld.exception.EhldException;
import org.reactome.server.tools.diagram.exporter.raster.profiles.ColorProfiles;
import org.reactome.server.tools.diagram.exporter.sbgn.SbgnConverter;
import org.reactome.server.tools.diagram.exporter.utils.ProgressBar;
import org.sbgn.SbgnUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Main class for the diagram exporter project
 */
//@SpringBootApplication
public class Main {

    private static final Logger logger = LoggerFactory.getLogger("diagram-exporter");

    private enum Format {PPTX, SVG, PNG, JPEG, JPG, GIF, SBGN, PDF}

    public static boolean verbose;

    public static void main(String[] args) throws JSAPException, DiagramProfileException, DiagramJsonNotFoundException, DiagramJsonDeserializationException {
        // Program Arguments -i, -p, -o, -j, -f and -s
        SimpleJSAP jsap = new SimpleJSAP(Main.class.getName(), "Exports the requested pathway diagrams to different formats (svg, png, sbgn, pptx, gif, jpeg)",
                new Parameter[]{
                        new FlaggedOption("target",   JSAP.STRING_PARSER, null,        JSAP.REQUIRED,    't', "target",  "Target pathways to convert. Use either comma separated IDs, pathways for a given species (e.g. 'Homo sapiens') or 'all' for every pathway").setList(true).setListSeparator(','),
                        new FlaggedOption(  "format",   JSAP.STRING_PARSER, null,        JSAP.REQUIRED,    'f', "format",  "Format of the output files (svg, png, sbgn, pptx, gif, jpeg, pdf)"),
                        new FlaggedOption(  "output",   JSAP.STRING_PARSER, null,        JSAP.REQUIRED,    'o', "output",  "The output folder"),
                        new FlaggedOption(  "input",    JSAP.STRING_PARSER, null,        JSAP.REQUIRED,    'i', "input",   "The input folder containing the diagram json files"),

                        //EHLD options
                        new FlaggedOption(  "ehlds",    JSAP.STRING_PARSER, null,        JSAP.NOT_REQUIRED,'e', "ehld",    "The folder containing the EHLD svg files"),
                        new FlaggedOption(  "summary",  JSAP.STRING_PARSER, null,        JSAP.NOT_REQUIRED,'s', "summary", "The file containing the summary of pathways with EHLD assigned"),

                        //GRAPH-DB options
                        new FlaggedOption(  "host",     JSAP.STRING_PARSER, "bolt://localhost:7687", JSAP.NOT_REQUIRED,'h', "host",    "The neo4j host"),
                        new FlaggedOption(  "name",     JSAP.STRING_PARSER, "graph.db",  JSAP.NOT_REQUIRED,'n', "name",    "The neo4j database name"),
                        new FlaggedOption(  "user",     JSAP.STRING_PARSER, "neo4j",     JSAP.NOT_REQUIRED,'u', "user",    "The neo4j user"),
                        new FlaggedOption(  "password", JSAP.STRING_PARSER, "neo4j",     JSAP.REQUIRED,    'w', "password","The neo4j password"),

                        new FlaggedOption(  "profile",  JSAP.STRING_PARSER, "Modern",    JSAP.NOT_REQUIRED,'c', "profile", "The colour diagram [Modern or Standard]"),
                        new FlaggedOption(  "license",  JSAP.STRING_PARSER, null,        JSAP.NOT_REQUIRED,'l', "license", "Software License file"),

                        new QualifiedSwitch("verbose",  JSAP.BOOLEAN_PARSER,null,        JSAP.NOT_REQUIRED,'v', "verbose", "Requests verbose output.")
                }
        );

        JSAPResult config = jsap.parse(args);
        if (jsap.messagePrinted()) System.exit(1);

        verbose = config.getBoolean("verbose");

        //Initialising ReactomeCore Neo4j configuration
        ReactomeGraphCore.initialise(config.getString("host"), config.getString("user"), config.getString("password"), config.getString("name"));

        String profile = config.getString("profile");

        //Checking for the format
        String f = config.getString("format").toUpperCase();
        Format format = null;
        try {
            format = Format.valueOf(f);
        } catch (IllegalArgumentException e) {
            System.err.printf("'%s' is not a valid format. Please specify one of the supported formats %s%n", f, Arrays.toString(Format.values()));
            System.exit(1);
        }

        //Checking for the specified input/output folders
        File input = getFinalInputFolder(config.getString("input"));
        File output = getFinalOutputFolder(config.getString("output"), format, profile);

        long start = System.currentTimeMillis();
        print("· Retrieving target pathways...");
        Collection<Pathway> targets = getTargets(config.getStringArray("target"));
        final int total = targets.size();
        if (total > 0) {
            println("\r· Targeting %,d pathway%s to be converted to '%s':\n", total, total > 1 ? "s" : "", format.name());
            int counter = 0;
            switch (format) {
                case PPTX:
                    String lic = config.getString("license");
                    counter = generatePPTX(targets, input, output, profile, lic);
                    break;
                case SVG:
                case PNG:
                case PDF:
                case JPEG:
                case JPG:
                case GIF:
                    File ehlds = getEhldsFolder(config.getString("ehlds"));
                    File ehldSummary = getEhldSummaryFile(config.getString("summary"));
                    counter = generateImage(targets, format, profile, input, output, ehlds, ehldSummary);
                    break;
                case SBGN:
                    counter = generateSBGN(targets, input, output);
                    break;
            }
            Long time = System.currentTimeMillis() - start;
            println("Finished: %,d pathway%s have been exported to '%s' in %s", counter, counter > 1 ? "s" : "", f, getTimeFormatted(time));
            if (counter != total) {
                int wrong = total - counter;
                println("%,d pathway%s could not be converted. Please check log file for more info.", wrong, wrong > 1 ? "s" : "");
                System.exit(1);
            }
        } else {
            println("· No target pathways found based on '%s'", String.join(",", config.getStringArray("target")));
        }

        System.exit(0);
    }

    private static int generatePPTX(Collection<Pathway> target, File input, File output, String colourProfile, String lic) throws DiagramProfileException, DiagramJsonDeserializationException, DiagramJsonNotFoundException {
        int total = target.size();
        int counter = 0;
        for (Pathway pathway : target) {
            String name = pathway.getStId() + ".pptx";
            ProgressBar.updateProgressBar(name, counter, total);
            File finalPptx = PowerPointExporter.export(pathway.getStId(), input.getAbsolutePath(), colourProfile, output.getPath(), new Decorator(), lic);
            if (finalPptx.exists()) {
                counter++;
            } else {
                logger.error("Cannot generate the PPTX file for " + pathway.getStId());
            }
        }
        ProgressBar.done(total);
        return counter;
    }

    private static int generateImage(Collection<Pathway> target, Format format, String colourProfile, File input, File output, File ehlds, File ehldSummary) {
        RasterExporter rasterExporter = new RasterExporter(input.getAbsolutePath(), ehlds.getAbsolutePath(), null, ehldSummary.getAbsolutePath());

        int total = target.size();
        int counter = 0;
        for (Pathway pathway : target) {
            String name = pathway.getStId() + "." + format.name().toLowerCase();
            ProgressBar.updateProgressBar(name, counter, total);
            final RasterArgs args = new RasterArgs(pathway.getStId(), format.name().toLowerCase());
            args.setProfiles(new ColorProfiles(colourProfile, null, null));
            args.setWriteTitle(true);

            File file = new File(output.getAbsolutePath() + "/" + name);
            try {
                rasterExporter.export(args, new FileOutputStream(file));
                if (file.exists()) counter++;
            } catch (EhldException | AnalysisException | DiagramJsonDeserializationException | DiagramJsonNotFoundException | IOException | TranscoderException e) {
                logger.error("Cannot generate the " + format + " file for " + pathway.getStId(), e);
            }
        }
        ProgressBar.done(total);
        return counter;
    }

    private static int generateSBGN(Collection<Pathway> target, File input, File output){
        int total = target.size();
        int counter = 0;
        for (Pathway pathway : target) {
            String name = pathway.getStId() + ".sbgn";
            ProgressBar.updateProgressBar(name, counter, total);
            try {
                //noinspection ConstantConditions
                SbgnConverter converter = new SbgnConverter(getDiagram(pathway, input));
                File sbgn = new File(output.getAbsolutePath() + "/" + name);
                SbgnUtil.writeToFile(converter.getSbgn(), sbgn);
                counter++;
            } catch (JAXBException | NullPointerException e) {
                logger.error("Cannot generate the SBGN file for " + pathway.getStId(), e);
            }
        }
        ProgressBar.done(total);
        return counter;
    }

    private static Collection<Pathway> getTargets(String[] target) {
        AdvancedDatabaseObjectService advancedDatabaseObjectService = ReactomeGraphCore.getService(AdvancedDatabaseObjectService.class);
        String query;
        Map<String, Object> parametersMap = new HashMap<>();
        if (target.length > 1) {
            query = "MATCH (p:Pathway{hasDiagram:True}) " +
                    "WHERE p.dbId IN $dbIds OR p.stId IN $stIds " +
                    "WITH DISTINCT p " +
                    "RETURN p " +
                    "ORDER BY p.dbId";
            List<Long> dbIds = new ArrayList<>();
            List<String> stIds = new ArrayList<>();
            for (String identifier : target) {
                String id = DatabaseObjectUtils.getIdentifier(identifier);
                if (DatabaseObjectUtils.isStId(id)) {
                    stIds.add(id);
                } else if (DatabaseObjectUtils.isDbId(id)) {
                    dbIds.add(Long.parseLong(id));
                }
            }
            parametersMap.put("dbIds", dbIds);
            parametersMap.put("stIds", stIds);
        } else {
            String aux = target[0];
            if (aux.toLowerCase().equals("all")) {
                query = "MATCH (p:Pathway{hasDiagram:True})-[:species]->(s:Species) " +
                        "WITH DISTINCT p, s " +
                        "RETURN p " +
                        "ORDER BY s.dbId, p.dbId";
            } else if (DatabaseObjectUtils.isStId(aux)) {
                query = "MATCH (p:Pathway{hasDiagram:True, stId:$stId}) RETURN DISTINCT p";
                parametersMap.put("stId", DatabaseObjectUtils.getIdentifier(aux));
            } else if (DatabaseObjectUtils.isDbId(aux)) {
                query = "MATCH (p:Pathway{hasDiagram:True, dbId:$dbId}) RETURN DISTINCT p";
                parametersMap.put("dbId", DatabaseObjectUtils.getIdentifier(aux));
            } else {
                query = "MATCH (p:Pathway{hasDiagram:True, speciesName:$speciesName}) " +
                        "WITH DISTINCT p " +
                        "RETURN p " +
                        "ORDER BY p.dbId";
                parametersMap.put("speciesName", aux);
            }
        }

        Collection<Pathway> pathways = new ArrayList<>();
        try {
            pathways = advancedDatabaseObjectService.getCustomQueryResults(Pathway.class, query, parametersMap);
        } catch (CustomQueryException e) {
            logger.error("Problem retrieving the target pathways", e);
        }
        return pathways;
    }

    private static Diagram getDiagram(Pathway pathway, File input) {
        try {
            File aux = new File(input.getAbsolutePath() + "/" + pathway.getStId()+ ".json");
            String json = IOUtils.toString(new FileInputStream(aux), Charset.defaultCharset());
            return DiagramFactory.getDiagram(json);
        } catch (IOException | DeserializationException e) {
            logger.error("Problem retrieving the diagram object for pathway " + pathway.getStId(), e);
            return null;
        }
    }

    private static File getFinalInputFolder(String inputFolder) {
        //Checking for the specified input/output folders
        File input = new File(inputFolder);
        if (!input.exists() || !input.isDirectory()) {
            System.err.printf("Cannot find the input folder '%s'%n", input.getAbsolutePath());
            System.exit(1);
        }
        return input;
    }

    private static File getFinalOutputFolder(String outputFolder, Format format, String profile) {
        File output = new File(outputFolder);
        if (!output.exists() || !output.isDirectory()) {
            System.err.printf("Cannot find the output folder '%s'%n", output.getAbsolutePath());
            System.exit(1);
        } else {
            output = new File(output.getAbsolutePath() + "/" + format.name().toLowerCase());
            if (!output.exists() && !output.mkdir()) {
                System.err.printf("Cannot create '%s'%n", output.getAbsolutePath());
                System.exit(1);
            }
            if(!format.equals(Format.SBGN)) {
                output = new File(output.getAbsolutePath() + "/" + profile);
                if (!output.exists()) {
                    //noinspection ResultOfMethodCallIgnored
                    output.mkdir();
                }
            }
        }
        return output;
    }

    private static File getEhldsFolder(String ehlds){
        File rtn = new File(ehlds);
        if(!rtn.exists() || !rtn.isDirectory()) {
            System.err.printf("Cannot find the ehlds folder '%s'%n", rtn.getAbsolutePath());
            System.exit(1);
        }
        return rtn;
    }

    private static File getEhldSummaryFile(String fileName){
        File file = new File(fileName);
        if(!file.exists()) {
            System.err.printf("Cannot find the ehld summary file '%s'%n", file.getAbsolutePath());
            System.exit(1);
        }
        return file;
    }

    private static String getTimeFormatted(Long millis) {
        return String.format("%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(millis),
                TimeUnit.MILLISECONDS.toMinutes(millis) % TimeUnit.HOURS.toMinutes(1),
                TimeUnit.MILLISECONDS.toSeconds(millis) % TimeUnit.MINUTES.toSeconds(1));
    }

    @SuppressWarnings("SameParameterValue")
    private static void print(String msg, Object... params){
        if(verbose) System.out.printf(msg, params);
    }

    private static void println(String msg, Object... params){
        if(verbose) System.out.printf((msg) + "%n", params);
    }
}
