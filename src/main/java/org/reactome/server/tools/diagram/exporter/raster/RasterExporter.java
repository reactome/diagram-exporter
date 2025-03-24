package org.reactome.server.tools.diagram.exporter.raster;

import com.itextpdf.layout.Document;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.commons.io.IOUtils;
import org.reactome.server.analysis.core.model.AnalysisType;
import org.reactome.server.analysis.core.result.AnalysisStoredResult;
import org.reactome.server.analysis.core.result.exception.ResourceGoneException;
import org.reactome.server.analysis.core.result.exception.ResourceNotFoundException;
import org.reactome.server.analysis.core.result.utils.TokenUtils;
import org.reactome.server.tools.diagram.data.graph.Graph;
import org.reactome.server.tools.diagram.data.layout.Diagram;
import org.reactome.server.tools.diagram.exporter.common.analysis.AnalysisException;
import org.reactome.server.tools.diagram.exporter.common.profiles.factory.DiagramJsonDeserializationException;
import org.reactome.server.tools.diagram.exporter.common.profiles.factory.DiagramJsonNotFoundException;
import org.reactome.server.tools.diagram.exporter.raster.api.RasterArgs;
import org.reactome.server.tools.diagram.exporter.raster.diagram.DiagramRenderer;
import org.reactome.server.tools.diagram.exporter.raster.ehld.EhldRenderer;
import org.reactome.server.tools.diagram.exporter.raster.ehld.exception.EhldException;
import org.springframework.stereotype.Component;
import org.w3c.dom.svg.SVGDocument;

import java.awt.image.BufferedImage;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

/**
 * @author Lorente-Arencibia, Pascual (pasculorente@gmail.com)
 */
// This is the entry point. Methods are intended to be called.
@SuppressWarnings({"unused", "WeakerAccess"})
@Component
public class RasterExporter {

	private final String diagramPath;
	private final String ehldPath;
	private final Set<String> ehld;
	private final TokenUtils tokenUtils;

	/**
	 * Creates an empty RasterExporter. As no paths are configured, only
	 * {@link RasterExporter#export(Diagram, Graph, RasterArgs, AnalysisStoredResult, OutputStream)} will be available.
	 */
	public RasterExporter() {
		diagramPath = null;
		ehldPath = null;
		ehld = null;
		tokenUtils = null;
	}

	/**
	 * Configures a new RasterExporter setting the resources paths.
	 *
	 * @param diagramPath  path to standard diagrams
	 * @param ehldPath     path to EHLDs
	 * @param analysisPath path to analysis results (aka token)
	 * @param svgSummary   path to the the list of stId that have EHLD
	 */
	public RasterExporter(String diagramPath, String ehldPath, String analysisPath, String svgSummary) {
		this.diagramPath = diagramPath;
		this.ehldPath = ehldPath;
		this.tokenUtils = new TokenUtils(analysisPath);
		Set<String> ehld;
		try {
			ehld = new TreeSet<>(IOUtils.readLines(new FileReader(svgSummary)));
		} catch (Exception e) {
			ehld = new HashSet<>();
		}
		this.ehld = ehld;
	}

	/**
	 * Renders args as a BufferedImage. See {@link RasterOutput} for saving
	 * options.
	 *
	 * @param args image arguments
	 */
	public BufferedImage exportToImage(RasterArgs args) throws AnalysisException, EhldException, DiagramJsonNotFoundException, DiagramJsonDeserializationException {
		return exportToImage(args, null);
	}

	/**
	 * Renders args as a BufferedImage. See {@link RasterOutput} for saving
	 * options.
	 *
	 * @param args image arguments
	 */
	public BufferedImage exportToImage(RasterArgs args, AnalysisStoredResult result) throws AnalysisException, EhldException, DiagramJsonNotFoundException, DiagramJsonDeserializationException {
		result = getResult(args.getToken(), result);
		final RasterRenderer renderer = selectRenderer(args, result);
//		final Dimension dimension = renderer.getDimension();
//		double size = dimension.getHeight() * dimension.getWidth();
		return renderer.render();
	}

	/**
	 * Generates an animated GIF with as many frames as columns in the analysis
	 * token. args.getColumn() is ignored. Animated GIFs must be written
	 * directly into an <code>{@link OutputStream}</code>. There is no Java
	 * class that supports storing a GIF in memory.
	 */
	public void exportToGif(RasterArgs args, OutputStream os) throws IOException, AnalysisException, EhldException, DiagramJsonNotFoundException, DiagramJsonDeserializationException {
		exportToGif(args, os, null);
	}

	/**
	 * Generates an animated GIF with as many frames as columns in the analysis
	 * token. args.getColumn() is ignored. Animated GIFs must be written
	 * directly into an <code>{@link OutputStream}</code>. There is no Java
	 * class that supports storing a GIF in memory.
	 */
	public void exportToGif(RasterArgs args, OutputStream os, AnalysisStoredResult result) throws AnalysisException, EhldException, DiagramJsonNotFoundException, DiagramJsonDeserializationException, IOException {
		result = getResult(args.getToken(), result);
		final RasterRenderer renderer = selectRenderer(args, result);
		renderer.renderToAnimatedGif(os);
	}

	/**
	 * Creates a SVG document from the diagram, adding, if asked, selection,
	 * flagging and analysis.See {@link RasterOutput} for saving options.
	 */
	public SVGDocument exportToSvg(RasterArgs args) throws AnalysisException, EhldException, DiagramJsonNotFoundException, DiagramJsonDeserializationException {
		return exportToSvg(args, null);
	}

	/**
	 * Creates a SVG document from the diagram, adding, if asked, selection,
	 * flagging and analysis.See {@link RasterOutput} for saving options.
	 */
	public SVGDocument exportToSvg(RasterArgs args, AnalysisStoredResult result) throws AnalysisException, EhldException, DiagramJsonNotFoundException, DiagramJsonDeserializationException {
		result = getResult(args.getToken(), result);
		final RasterRenderer renderer = selectRenderer(args, result);
		return renderer.renderToSvg();
	}

	/**
	 * Creates an iText7 PDF document
	 */
	public Document exportToPdf(RasterArgs args) throws EhldException, IOException, AnalysisException, DiagramJsonNotFoundException, DiagramJsonDeserializationException {
		return exportToPdf(args, null);
	}

	/**
	 * Creates an iText7 PDF document in reading mode.
	 */
	public Document exportToPdf(RasterArgs args, AnalysisStoredResult result) throws EhldException, AnalysisException, DiagramJsonNotFoundException, DiagramJsonDeserializationException, IOException {
		result = getResult(args.getToken(), result);
		final RasterRenderer renderer = selectRenderer(args, result);
		return renderer.renderToPdf();
	}

	public Document exportToPdf(Diagram diagram, Graph graph, RasterArgs args, AnalysisStoredResult result) throws AnalysisException, IOException {
		result = getResult(args.getToken(), result);
		final DiagramRenderer renderer = new DiagramRenderer(diagram, graph, args, result);
		final AnalysisType type = result == null
				? null
				: AnalysisType.valueOf(result.getSummary().getType());
		return renderer.renderToPdf();
	}


	/**
	 * Exports the diagram defined by args in the most appropriate way,
	 * depending on the args, and using os to export the result. This is a
	 * shortcut for export(args
	 *
	 * @param args what you want
	 * @param os   where you want it
	 */
	public void export(RasterArgs args, OutputStream os) throws EhldException, AnalysisException, DiagramJsonNotFoundException, DiagramJsonDeserializationException, IOException, TranscoderException {
		export(args, os, null);
	}

	/**
	 * Export the diagram defined by args in the most appropriate format.
	 */
	public void export(RasterArgs args, OutputStream os, AnalysisStoredResult result) throws EhldException, AnalysisException, DiagramJsonNotFoundException, DiagramJsonDeserializationException, IOException, TranscoderException {
		result = getResult(args.getToken(), result);
		final RasterRenderer renderer = selectRenderer(args, result);
		final AnalysisType type = result == null
				? null
				: AnalysisType.valueOf(result.getSummary().getType());
		export(args, os, renderer, type);
	}

	/**
	 * Exports diagram in the most appropriate format.
	 *
	 * @param diagram the diagram
	 * @param graph   the associated graph
	 * @param args    params to export
	 * @param result  use a non null value to overlay analysis results
	 * @param os      result will be written through the OutputStream
	 * @throws IOException         for problems wit the outputStream
	 * @throws TranscoderException for problems when rendering to SVG
	 * @throws AnalysisException   for problems with the analysis
	 */
	public void export(Diagram diagram, Graph graph, RasterArgs args, AnalysisStoredResult result, OutputStream os) throws IOException, TranscoderException, AnalysisException {
		result = getResult(args.getToken(), result);
		final DiagramRenderer renderer = new DiagramRenderer(diagram, graph, args, result);
		final AnalysisType type = result == null
				? null
				: AnalysisType.valueOf(result.getSummary().getType());
		export(args, os, renderer, type);
	}

	private void export(RasterArgs args, OutputStream os, RasterRenderer renderer, AnalysisType type) throws IOException, TranscoderException {
		if (args.getFormat().equalsIgnoreCase("gif")
				&& args.getColumn() == null
				&& allowAnimatedGif(type))
			renderer.renderToAnimatedGif(os);
		else if (args.getFormat().equalsIgnoreCase("svg"))
			RasterOutput.save(renderer.renderToSvg(), os);
		else if (args.getFormat().equalsIgnoreCase("pdf"))
			RasterOutput.save(renderer.renderToPdf(), os);
		else RasterOutput.save(renderer.render(), args.getFormat(), os);
	}

	private RasterRenderer selectRenderer(RasterArgs args, AnalysisStoredResult result) throws EhldException, DiagramJsonNotFoundException, DiagramJsonDeserializationException {
		return args.isEhld() && ehld.contains(args.getStId())
				? new EhldRenderer(args, ehldPath, result)
				: new DiagramRenderer(args, diagramPath, result);
	}

	/**
	 * If result is not null, use result. If not and token is not null try to
	 * get the result using it. If token is also null, returns null.
	 */
	private AnalysisStoredResult getResult(String token, AnalysisStoredResult result) throws AnalysisException {
		if (result != null) return result;
		if (token == null) return null;
		try {
			return tokenUtils.getFromToken(token);
		} catch (ResourceGoneException e) {
			throw new AnalysisException("Token has expired: " + token, e);
		} catch (ResourceNotFoundException e) {
			throw new AnalysisException("Token not valid: " + token, e);
		}
	}

	private boolean allowAnimatedGif(AnalysisType type) {
		return type == AnalysisType.EXPRESSION || type == AnalysisType.GSA_REGULATION || type == AnalysisType.GSA_STATISTICS || type == AnalysisType.GSVA;
	}

	public void addEhld(String stId) {
		this.ehld.add(stId);
	}
}
