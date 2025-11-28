package org.reactome.server.tools.diagram.exporter.raster.ehld;

import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.*;
import com.itextpdf.layout.Document;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.ImageTranscoder;
import org.apache.fop.activity.ContainerUtil;
import org.apache.fop.configuration.Configuration;
import org.apache.fop.configuration.ConfigurationException;
import org.apache.fop.configuration.DefaultConfigurationBuilder;
import org.apache.fop.svg.PDFTranscoder;
import org.reactome.server.analysis.core.model.AnalysisType;
import org.reactome.server.analysis.core.result.AnalysisStoredResult;
import org.reactome.server.tools.diagram.exporter.common.ResourcesFactory;
import org.reactome.server.tools.diagram.exporter.raster.RasterRenderer;
import org.reactome.server.tools.diagram.exporter.raster.api.RasterArgs;
import org.reactome.server.tools.diagram.exporter.raster.ehld.exception.EhldException;
import org.reactome.server.tools.diagram.exporter.raster.ehld.exception.EhldRuntimeException;
import org.reactome.server.tools.diagram.exporter.raster.gif.AnimatedGifEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.svg.SVGDocument;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.apache.batik.util.SVGConstants.*;

/**
 * Main class to create a render from an EHLD.
 */
public class EhldRenderer implements RasterRenderer {

    private static Logger log = LoggerFactory.getLogger("diagram-exporter");
    private static final Set<String> TRANSPARENT_FORMATS = new HashSet<>(Arrays.asList("png", "pdf"));
    private static final Set<String> NO_TRANSPARENT_FORMATS = new HashSet<>(Arrays.asList("jpg", "jpeg", "gif"));

    private static final float MARGIN = 15;
    private final SVGDocument document;
    private final RasterArgs args;
    private SvgAnalysis svgAnalysis;
    private AnalysisStoredResult result;

    private static Configuration configuration;

    static {
        File file = Path.of("src/main/resources/fonts").toFile();
        if (file.exists() && file.isDirectory())
            setFontFolder("src/main/resources/fonts");
    }

    /**
     * Setting font folder.
     * <p>
     * The folder need to be a proper one, thus when resources are packaged in a jar or a war, we need to provide a stable folder path.<br>
     * For JAR or maven runners, usually the default src/main/resources/fonts is enough, but it needs to be placed on every package that is using Diagram exporter.<br>
     * <p>
     * For war files, we should include the font folder inside webapp so that it is expanded when deployed in Tomcat.<br>
     * To configure inside SpringBoot, use a config class like ExporterConfif in ContentService<br>
     *
     * @param fontFolderPath
     */
    public static void setFontFolder(String fontFolderPath) {
        File file = Path.of(fontFolderPath).toFile();
        if (!file.exists() || !file.isDirectory()) {
            log.error("Configured Font path " + fontFolderPath + " does not exist or is not a directory");
            return;
        }
        log.debug("Configured Font path " + fontFolderPath + " does exist and is a directory");
        try {
            //language=xml
            String confString = String.format(
                    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                            "<fop version=\"1.0\">" +
                            "    <fonts>" +
                            "        <directory>%s</directory>" +
                            "    </fonts>" +
                            "</fop>", fontFolderPath);


            DefaultConfigurationBuilder cfgBuilder = new DefaultConfigurationBuilder();
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(confString.getBytes(StandardCharsets.UTF_8));
            configuration = cfgBuilder.build(byteArrayInputStream);

            // Required so that the font gets correctly loaded and is not bigger than it is supposed when using batik
            File[] fontFiles = file.listFiles((dir, name) -> name.endsWith("ttf") || name.endsWith("otf"));
            if (fontFiles == null) {
                log.warn("Configured Font path " + fontFolderPath + " does not contain any TrueType font file (.ttf or .otf)");
                return;
            }
            for (File fontFile : fontFiles) {
                Font font = Font.createFont(Font.TRUETYPE_FONT, fontFile);
                GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font);
                log.debug("{} font registered", font.getFontName());
            }

        } catch (ConfigurationException | FontFormatException | IOException e) {
            throw new RuntimeException(e);
        }
    }


    public EhldRenderer(RasterArgs args, String ehldPath, AnalysisStoredResult result) throws EhldException {
        this.result = result;
        this.document = ResourcesFactory.getEhld(ehldPath, args.getStId());
        this.args = args;
        layout();
    }

    private void layout() {
        SvgDecoratorRenderer.selectAndFlag(document, args);
        svgAnalysis = new SvgAnalysis(document, args, result);
        svgAnalysis.analysis();
        updateDocumentDimensions();
    }

    @Override
    public Dimension getDimension() {
        final String viewBox = document.getRootElement().getAttribute(SVG_VIEW_BOX_ATTRIBUTE);
        final String[] values = viewBox.split(" ");
        final double width = Double.parseDouble(values[2]);
        final double height = Double.parseDouble(values[3]);
        return new Dimension((int) (width + 0.5), (int) (height + 0.5));
    }

    @Override
    public BufferedImage render() {
//        disableMasks();
        return rasterize();
    }

    private void disableMasks() {
        // Remove each mask from its parent
        final NodeList masks = document.getElementsByTagNameNS(SVG_NAMESPACE_URI, SVG_MASK_TAG);
        final List<Element> maskNodes = IntStream.range(0, masks.getLength())
                .mapToObj(masks::item)
                .map(Element.class::cast)
                .collect(Collectors.toList());
        maskNodes.forEach(mask -> mask.getParentNode().removeChild(mask));
        // Remove from defs/style
        // This is not necessary, as they are not referenced anymore,
        // but will keep the SVG document clear
        final NodeList styleList = document.getRootElement().getElementsByTagNameNS(SVG_NAMESPACE_URI, SVG_STYLE_ATTRIBUTE);
        final Node style = styleList.getLength() > 0 ? styleList.item(0) : null;
        if (style != null)
            maskNodes.forEach(mask -> removeMaskFromStyle(style, mask.getAttribute(SVG_ID_ATTRIBUTE)));
    }

    private void removeMaskFromStyle(Node style, String maskId) {
        final String maskRef = String.format("mask:url(#%s);", maskId);
        style.setTextContent(style.getTextContent().replace(maskRef, ""));
    }

    private void updateDocumentDimensions() {
        final String viewBox = document.getRootElement().getAttribute(SVG_VIEW_BOX_ATTRIBUTE);
        final Scanner scanner = new Scanner(viewBox);
        scanner.useLocale(Locale.UK);
        scanner.nextFloat();  // x
        scanner.nextFloat();  // y
        float width = scanner.nextFloat();
        float height = scanner.nextFloat();

        width += 2 * MARGIN;
        height += 2 * MARGIN;
        // 1 increase image dimensions
        final String newVB = String.format(Locale.UK, "0 0 %.3f %.3f", width, height);
        document.getRootElement().setAttribute(SVG_VIEW_BOX_ATTRIBUTE, newVB);
        // 2 create a g translated (margin, margin)
        final Element group = document.createElementNS(SVG_NAMESPACE_URI, SVG_G_TAG);
        final String translate = String.format(Locale.UK, "%s(%f,%f)", SVG_TRANSLATE_VALUE, MARGIN, MARGIN);
        group.setAttribute(SVG_TRANSFORM_ATTRIBUTE, translate);

        // 3 append to the translated group all the g elements in the root
        final List<Node> children = new LinkedList<>();
        for (int i = 0; i < document.getRootElement().getChildNodes().getLength(); i++)
            children.add(document.getRootElement().getChildNodes().item(i));
        children.stream()
                .filter(node -> node.getNodeName().equals(SVG_G_TAG))
                .forEach(group::appendChild);

        document.getRootElement().appendChild(group);

        // Apply factor/scale
        // NOTE: there are 3 ways of scaling:
        //  * 1: set width and height on svg root
        //    2: apply a transform to the elements transform: scale(factor)
        //    3: set width and height on Transcoder
        document.getRootElement().setAttribute(SVG_WIDTH_ATTRIBUTE, String.format(Locale.UK, "%.3f", width * args.getFactor()));
        document.getRootElement().setAttribute(SVG_HEIGHT_ATTRIBUTE, String.format(Locale.UK, "%.3f", height * args.getFactor()));
    }

    /**
     * Generates a raster from document.
     */
    private BufferedImage rasterize() {
        // TODO: 18/05/18 replace with the svg-renderer project when mature
        try {
            final TranscoderInput input = new TranscoderInput(document);
            final BufferedImageTranscoder transcoder = new BufferedImageTranscoder(args);
            transcoder.transcode(input, null);
            return transcoder.getImage();
        } catch (TranscoderException e) {
            throw new EhldRuntimeException(e.getMessage());
        }
    }

    @Override
    public void renderToAnimatedGif(OutputStream os) {
        if (svgAnalysis.getAnalysisType() != AnalysisType.EXPRESSION
                && svgAnalysis.getAnalysisType() != AnalysisType.GSA_REGULATION
                && svgAnalysis.getAnalysisType() != AnalysisType.GSA_STATISTICS
                && svgAnalysis.getAnalysisType() != AnalysisType.GSVA)
            throw new IllegalStateException("Only EXPRESSION and GENE SET (GSA) analysis can be rendered into animated GIFs");

//        disableMasks();
        final AnimatedGifEncoder encoder = new AnimatedGifEncoder();
        encoder.setDelay(1000);
        encoder.setRepeat(0);
        encoder.start(os);
        for (int expressionColumn = 0; expressionColumn < svgAnalysis.getExpressionSummary().getColumnNames().size(); expressionColumn++) {
            svgAnalysis.setColumn(expressionColumn);
            final BufferedImage image = rasterize();
            encoder.addFrame(image);
        }
        encoder.finish();
    }

    @Override
    public SVGDocument renderToSvg() {
        return document;
    }


    @Override
    public Document renderToPdf() throws IOException {
        final ByteArrayOutputStream os = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(os, new WriterProperties().setFullCompressionMode(true).useSmartMode());
        Document document = new Document(new PdfDocument(writer));

        PageSize pageSize = new PageSize(
                Float.parseFloat(this.document.getRootElement().getAttribute("width")),
                Float.parseFloat(this.document.getRootElement().getAttribute("height"))
        );
        final PdfPage page = document.getPdfDocument().addNewPage(pageSize);
        document.setMargins(0, 0, 0, 0);

        try {
            TranscoderInput input = new TranscoderInput(this.document);
            TranscoderOutput output = new TranscoderOutput(os);
            PDFTranscoder pdfTranscoder = new PDFTranscoder();
            ContainerUtil.configure(pdfTranscoder, configuration);
            pdfTranscoder.transcode(input, output);
        } catch (TranscoderException e) {
            throw new RuntimeException(e);
        }

        try {
            return new Document(new PdfDocument(new PdfReader(new ByteArrayInputStream(os.toByteArray()))));
        } catch (Exception e) { // Avoid PDF cross reference mismatch problem
        }
        return document;
    }

    /**
     * There is no a standard BufferedImageTranscoder, although all Transcorders
     * use BufferedImages as the raster. This class exposes that BufferedImage,
     * so there is no need to store them in a File.
     */
    private static class BufferedImageTranscoder extends ImageTranscoder {

        private BufferedImage image;
        private String format;
        private Color background;

        BufferedImageTranscoder(RasterArgs args) {
            this.background = args.getBackground() == null
                    ? Color.WHITE
                    : args.getBackground();
            this.format = args.getFormat();
        }

        @Override
        public BufferedImage createImage(int w, int h) {
            BufferedImage image;
            Graphics2D graphics;
            if (TRANSPARENT_FORMATS.contains(format)) {
                image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                graphics = image.createGraphics();
            } else if (NO_TRANSPARENT_FORMATS.contains(format)) {
                image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
                graphics = image.createGraphics();
                graphics.setBackground(background);
                graphics.clearRect(0, 0, image.getWidth(), image.getHeight());
            } else
                throw new IllegalArgumentException("Unsupported file extension " + format);

            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_GASP);

            return image;
        }

        @Override
        public void writeImage(BufferedImage image, TranscoderOutput output) {
            this.image = image;
        }

        public BufferedImage getImage() {
            return image;
        }
    }
}
