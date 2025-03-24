package org.reactome.server.tools.diagram.exporter.raster;

import com.itextpdf.kernel.pdf.PdfWriter;
import org.apache.batik.transcoder.TranscoderException;
import org.junit.jupiter.api.Assertions;
import org.reactome.server.analysis.core.result.AnalysisStoredResult;
import org.reactome.server.analysis.core.result.utils.TokenUtils;
import org.reactome.server.tools.diagram.exporter.common.Decorator;
import org.reactome.server.tools.diagram.exporter.common.analysis.AnalysisException;
import org.reactome.server.tools.diagram.exporter.common.profiles.factory.DiagramJsonDeserializationException;
import org.reactome.server.tools.diagram.exporter.common.profiles.factory.DiagramJsonNotFoundException;
import org.reactome.server.tools.diagram.exporter.common.profiles.factory.DiagramProfileException;
import org.reactome.server.tools.diagram.exporter.pptx.PowerPointExporter;
import org.reactome.server.tools.diagram.exporter.raster.api.RasterArgs;
import org.reactome.server.tools.diagram.exporter.raster.diagram.DiagramRendererTest;
import org.reactome.server.tools.diagram.exporter.raster.ehld.EhldRendererTest;
import org.reactome.server.tools.diagram.exporter.raster.ehld.exception.EhldException;
import org.reactome.server.tools.diagram.exporter.sbgn.SbgnConverter;
import org.sbgn.SbgnUtil;

import javax.imageio.ImageIO;
import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.reactome.server.tools.diagram.exporter.common.ResourcesFactory.getDiagram;

/**
 * Supporting methods for testing diagram renderering. It is used both in {@link
 * DiagramRendererTest} and {@link EhldRendererTest}. It contains all the common
 * staff, like performing the analysis or creating and deleting the images dir.
 */
public class TestUtils {
    public static final String TOKEN_OVER_1 = "MjAyNTAyMjExNDE4MjBfNA%253D%253D"; // uniprot (GBM Uniprot)
    public static final String TOKEN_EXPRESSION_1 = "MjAyNTAyMjExNzA2MDJfNg%253D%253D";  // microarray (probeset)
    public static final String TOKEN_EXPRESSION_2 = "MjAyNTAzMTMxNDQ2NDhfMTk%253D";  // HPA (GeneName)
    public static final String TOKEN_CELL = "MjAyNTAzMTMxNDQ2NDhfMTk%253D";  // HPA (GeneName)
    public static final String TOKEN_SPECIES = "MjAyNTAzMTMxNDQ3MTRfMjA%253D"; // canis

    public static final String TOKEN_TISSUE = "MjAyNTAzMTMxNDQ3MzdfMjE%253D"; // tissue with 35 columns
    public static final String TOKEN_COSMIC = "MjAyNTAzMTMxNDQ4MDhfMjI%253D"; // COSMIC
    public static final String TOKEN_GSA = "MjAyNTAzMTMxNDUyNDRfMjY%3D";
    public static final String TOKEN_GSVA = "MjAyNTAzMTMxNDUxNDJfMjU%3D";

    private static final String ANALYSIS_PATH = "src/test/resources/org/reactome/server/tools/diagram/exporter/analysis";
    private static final String DIAGRAM_PATH = "src/test/resources/org/reactome/server/tools/diagram/exporter/diagram";
    private static final String EHLD_PATH = "src/test/resources/org/reactome/server/tools/diagram/exporter/ehld";
    private static final String SVG_SUMMARY = "src/test/resources/org/reactome/server/tools/diagram/exporter/svgsummary.txt";
    private static final TokenUtils TOKEN_UTILS = new TokenUtils(ANALYSIS_PATH);

    public static final RasterExporter EXPORTER;

    static {
        EXPORTER = new RasterExporter(DIAGRAM_PATH, EHLD_PATH, ANALYSIS_PATH, SVG_SUMMARY);
    }

    public static AnalysisStoredResult getResult(String token) {
        return TOKEN_UTILS.getFromToken(token);
    }

    public static void render(RasterArgs args) {
        render(args, null);
    }

    public static void render(RasterArgs args, boolean overwriteOutput) {
        render(args, null, overwriteOutput);
    }


    public static void render(RasterArgs args, AnalysisStoredResult result) {
        render(args, result, false);
    }

    public static void render(RasterArgs args, AnalysisStoredResult result, boolean overwriteOutput) {
        try {
            String format = args.getFormat().toLowerCase();
            File folder = Path.of("target/output").toFile();
            if (!folder.exists()) folder.mkdir();
            Path path = Path.of("target/output/" + args.getStId() + "." + format);
            if (!overwriteOutput)
                for (int i = 1; Files.exists(path); i++)
                    path = Path.of("target/output/" + args.getStId() + "-" + i + "." + format);
            File output = path.toFile();

            switch (format) {
                case "pdf":
                    RasterOutput.save(EXPORTER.exportToPdf(args, result), new PdfWriter(output));
                    break;
                case "svg":
                    RasterOutput.save(EXPORTER.exportToSvg(args, result), output);
                    break;
                case "pptx":
                    PowerPointExporter.export(args.getStId(), DIAGRAM_PATH, args.getProfiles().getDiagramSheet().getName(), "target/output/", new Decorator(args), "src/test/resources/license/Aspose.Slides.lic");
                    break;
                case "sbgn":
                    SbgnConverter converter = new SbgnConverter(getDiagram(DIAGRAM_PATH, args.getStId()));
                    SbgnUtil.writeToFile(converter.getSbgn(), output);
                    break;
                default:
                case "png":
                    ImageIO.write(EXPORTER.exportToImage(args, result), format, output);
                    break;
            }
        } catch (EhldException | AnalysisException | DiagramJsonDeserializationException |
                 DiagramJsonNotFoundException | IOException | DiagramProfileException | JAXBException |
                 TranscoderException e) {
            e.printStackTrace();
            Assertions.fail(e.getMessage());
        }
    }
}