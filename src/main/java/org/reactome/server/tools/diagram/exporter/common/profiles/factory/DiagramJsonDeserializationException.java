package org.reactome.server.tools.diagram.exporter.common.profiles.factory;

/**
 * If the source file of a diagram is malformed or cannot be read.
 *
 * @author Antonio Fabregat (fabregat@ebi.ac.uk)
 */
public class DiagramJsonDeserializationException extends Exception {
	public DiagramJsonDeserializationException(String message) {
		super(message);
	}
}
