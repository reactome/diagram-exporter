package org.reactome.server.tools.diagram.exporter.svg;

import org.junit.jupiter.api.Test;
import org.reactome.server.tools.diagram.exporter.BaseTest;
import org.reactome.server.tools.diagram.exporter.raster.TestUtils;
import org.reactome.server.tools.diagram.exporter.raster.api.RasterArgs;
import org.reactome.server.tools.diagram.exporter.raster.profiles.ColorProfiles;

import java.util.Collections;

public class SvgRendererTest extends BaseTest {

	@Test
	public void testDiagram() {
		final RasterArgs args = new RasterArgs("R-HSA-109606", "svg");
		args.setToken(TestUtils.TOKEN_OVER_1);
		TestUtils.render(args, null);
	}


	@Test
	public void testDiagramWithGSAAnalysis() {
		final RasterArgs args = new RasterArgs("R-HSA-68877", "svg");
		final ColorProfiles profiles = new ColorProfiles("standard", "standard", "teal");
		args.setProfiles(profiles);
		args.setSelected(Collections.singletonList("R-HSA-1638802"));
		args.setToken(TestUtils.TOKEN_GSA);
		TestUtils.render(args, null);
	}

	@Test
	public void testDiagramWithGSVAAnalysis() {
		final RasterArgs args = new RasterArgs("R-HSA-68877", "svg");
		final ColorProfiles profiles = new ColorProfiles("standard", "standard", "teal");
		args.setProfiles(profiles);
		args.setSelected(Collections.singletonList("R-HSA-1638802"));
		args.setToken(TestUtils.TOKEN_GSVA);
		TestUtils.render(args, null);
	}


	@Test
	public void testEhld() {
		final RasterArgs args = new RasterArgs("R-HSA-74160", "svg");
		args.setToken(TestUtils.TOKEN_EXPRESSION_1);
		TestUtils.render(args, null);
	}

	@Test
	public void testEhldFont() {
		TestUtils.render(new RasterArgs("R-HSA-69278", "svg"), null);
		// REPORT: partial fix. EHLDs don't follow SVG standards (Illustrator)
		TestUtils.render(new RasterArgs("R-HSA-69278", "png"), null);
		TestUtils.render(new RasterArgs("R-HSA-69620", "svg"), null);
		TestUtils.render(new RasterArgs("R-HSA-69620", "png"), null);
	}

	@Test
	public void testLegend() {
		final ColorProfiles profiles = new ColorProfiles("modern", "copper plus", "teal");
		final RasterArgs args = new RasterArgs("R-HSA-432047", "png");
		args.setProfiles(profiles);
		args.setSelected(Collections.singleton("R-ALL-879874"));
		args.setToken(TestUtils.TOKEN_OVER_1);
		TestUtils.render(args, null);
	}
}