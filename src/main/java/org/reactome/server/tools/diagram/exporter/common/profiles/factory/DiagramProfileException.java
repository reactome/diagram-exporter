package org.reactome.server.tools.diagram.exporter.common.profiles.factory;

/**
 * @author Antonio Fabregat (fabregat@ebi.ac.uk)
 */
public class DiagramProfileException extends Exception {

    @SuppressWarnings("UnusedDeclaration")
    public DiagramProfileException() {
    }

    public DiagramProfileException(String message, Throwable cause) {
        super(message, cause);
    }
    public DiagramProfileException(String message) {
        super(message);
    }
}
