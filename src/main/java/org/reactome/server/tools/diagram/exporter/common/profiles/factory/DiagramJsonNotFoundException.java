package org.reactome.server.tools.diagram.exporter.common.profiles.factory;

/**
 * When the source file of a diagram is not found.
 *
 * @author Antonio Fabregat (fabregat@ebi.ac.uk)
 */
public class DiagramJsonNotFoundException extends Exception {
	public DiagramJsonNotFoundException(String message) {
		super(message);
	}
}
