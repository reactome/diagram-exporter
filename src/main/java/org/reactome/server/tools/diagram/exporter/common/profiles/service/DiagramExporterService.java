package org.reactome.server.tools.diagram.exporter.common.profiles.service;

import org.reactome.server.tools.diagram.data.layout.Diagram;
import org.reactome.server.tools.diagram.exporter.common.Decorator;
import org.reactome.server.tools.diagram.exporter.common.profiles.factory.DiagramJsonDeserializationException;
import org.reactome.server.tools.diagram.exporter.common.profiles.factory.DiagramJsonNotFoundException;
import org.reactome.server.tools.diagram.exporter.common.profiles.factory.DiagramProfileException;
import org.reactome.server.tools.diagram.exporter.pptx.PowerPointExporter;
import org.reactome.server.tools.diagram.exporter.raster.ehld.EhldRenderer;

import java.io.File;

/**
 * @author Guilherme S Viteri (gviteri@ebi.ac.uk
 * @author Antonio Fabregt (fabregat@ebi.ac.uk)
 */

@SuppressWarnings("unused")
public class DiagramExporterService {
	/**
	 * Configure the EHLD renderer to inform it of where the fonts files are located.
	 * Not required when running from a maven environment as it default to src/main/resource/fonts, but necessary to set in war environment
	 * @param fontPath Path to folder containing fonts used in EHLD
	 */
	public static void configureFontPath(String fontPath) {
		EhldRenderer.setFontFolder(fontPath);
	}

	/**
	 * Service layer that provides access to the pptx exporter This method
	 * assumes a software is embedded in the project, otherwise evaluation will
	 * be generated
	 *
	 * @param stId              stable identifier of the diagram
	 * @param diagramJsonFolder static for the diagram json
	 * @param profileName       Color diagram getName
	 * @param outputFolder      output folder, the given folder + the color
	 *                          diagram as a folder.
	 * @param decorator         class that holds the flagged and the selected
	 *                          elements into two different lists.
	 *
	 * @return the pptx file
	 *
	 * @throws DiagramJsonDeserializationException
	 * @throws DiagramProfileException
	 * @throws DiagramJsonNotFoundException
	 */
	public File exportToPPTX(String stId, String diagramJsonFolder, String profileName, String outputFolder, Decorator decorator) throws DiagramJsonDeserializationException, DiagramProfileException, DiagramJsonNotFoundException {
		return PowerPointExporter.export(stId, diagramJsonFolder, profileName.toLowerCase(), outputFolder, decorator, "");
	}

	/**
	 * Service layer that provides access to the pptx exporter This method
	 * assumes a valid software is provided, otherwise evaluation will be
	 * generated
	 *
	 * @param stId              stable identifier of the diagram
	 * @param diagramJsonFolder static for the diagram json
	 * @param profileName       Color diagram getName
	 * @param outputFolder      output folder, the given folder + the color
	 *                          diagram as a folder.
	 * @param decorator         class that holds the flagged and the selected
	 *                          elements into two different lists.
	 * @param licenseFilePath   a valid Aspose Software License.
	 *
	 * @return the pptx file
	 *
	 * @throws DiagramJsonDeserializationException
	 * @throws DiagramProfileException
	 * @throws DiagramJsonNotFoundException
	 */
	public File exportToPPTX(String stId, String diagramJsonFolder, String profileName, String outputFolder, Decorator decorator, String licenseFilePath) throws DiagramJsonDeserializationException, DiagramProfileException, DiagramJsonNotFoundException {
		return PowerPointExporter.export(stId, diagramJsonFolder, profileName.toLowerCase(), outputFolder, decorator, licenseFilePath);
	}

	/**
	 * Service layer that provides access to the pptx exporter This method
	 * assumes a software is embedded in the project, otherwise evaluation will
	 * be generated
	 *
	 * @param diagram			an existing diagram object to be exported
	 * @param profileName       Color diagram getName
	 * @param outputFolder      output folder, the given folder + the color
	 *                          diagram as a folder.
	 * @param decorator         class that holds the flagged and the selected
	 *                          elements into two different lists.
	 *
	 * @return the pptx file
	 *
	 * @throws DiagramJsonDeserializationException
	 * @throws DiagramProfileException
	 */
	public File exportToPPTX(Diagram diagram, String profileName, String outputFolder, Decorator decorator) throws DiagramJsonDeserializationException, DiagramProfileException {
		return PowerPointExporter.export(diagram, profileName.toLowerCase(), outputFolder, decorator, "");
	}

	/**
	 * Service layer that provides access to the pptx exporter This method
	 * assumes a valid software is provided, otherwise evaluation will be
	 * generated
	 *
	 * @param diagram			an existing diagram object to be exported
	 * @param profileName       Color diagram getName
	 * @param outputFolder      output folder, the given folder + the color
	 *                          diagram as a folder.
	 * @param decorator         class that holds the flagged and the selected
	 *                          elements into two different lists.
	 * @param licenseFilePath   a valid Aspose Software License.
	 *
	 * @return the pptx file
	 *
	 * @throws DiagramJsonDeserializationException
	 * @throws DiagramProfileException
	 */
	public File exportToPPTX(Diagram diagram, String profileName, String outputFolder, Decorator decorator, String licenseFilePath) throws DiagramJsonDeserializationException, DiagramProfileException {
		return PowerPointExporter.export(diagram, profileName.toLowerCase(), outputFolder, decorator, licenseFilePath);
	}

	/**
	 * Service layer that provides access to the pptx exporter This method
	 * assumes a software is embedded in the project, otherwise evaluation will
	 * be generated
	 *
	 * @param stId              stable identifier of the diagram
	 * @param diagram			an existing diagram object to be exported
	 * @param profileName       Color diagram getName
	 * @param outputFolder      output folder, the given folder + the color
	 *                          diagram as a folder.
	 * @param decorator         class that holds the flagged and the selected
	 *                          elements into two different lists.
	 *
	 * @return the pptx file
	 *
	 * @throws DiagramJsonDeserializationException
	 * @throws DiagramProfileException
	 */
	public File exportToPPTX(String stId, Diagram diagram, String profileName, String outputFolder, Decorator decorator) throws DiagramJsonDeserializationException, DiagramProfileException {
		return PowerPointExporter.export(stId, diagram, profileName.toLowerCase(), outputFolder, decorator, "");
	}


	/**
	 * Service layer that provides access to the pptx exporter This method
	 * assumes a valid software is provided, otherwise evaluation will be
	 * generated
	 *
	 * @param stId              stable identifier of the diagram
	 * @param diagram			an existing diagram object to be exported
	 * @param profileName       Color diagram getName
	 * @param outputFolder      output folder, the given folder + the color
	 *                          diagram as a folder.
	 * @param decorator         class that holds the flagged and the selected
	 *                          elements into two different lists.
	 * @param licenseFilePath   a valid Aspose Software License.
	 *
	 * @return the pptx file
	 *
	 * @throws DiagramJsonDeserializationException
	 * @throws DiagramProfileException
	 */
	public File exportToPPTX(String stId, Diagram diagram, String profileName, String outputFolder, Decorator decorator, String licenseFilePath) throws DiagramJsonDeserializationException, DiagramProfileException {
		return PowerPointExporter.export(stId, diagram, profileName.toLowerCase(), outputFolder, decorator, licenseFilePath);
	}

}
