package org.semanticweb.binaryowl.doc;

import org.semanticweb.owlapi.model.OWLOntology;

/**
 * Author: Matthew Horridge<br>
 * Stanford University<br>
 * Bio-Medical Informatics Research Group<br>
 * Date: 23/07/2013
 */
public interface OWLOntologyDocument extends HasOntologyID, HasImportsDeclarations, HasAnnotations, HasAxioms, HasSignature {
    public OWLOntology getOntology();
}
