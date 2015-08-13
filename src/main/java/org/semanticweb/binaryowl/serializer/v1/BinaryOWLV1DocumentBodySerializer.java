package org.semanticweb.binaryowl.serializer.v1;

import org.semanticweb.binaryowl.BinaryOWLMetadata;
import org.semanticweb.binaryowl.BinaryOWLOntologyDocumentAppendedChangeHandler;
import org.semanticweb.binaryowl.BinaryOWLOntologyDocumentHandler;
import org.semanticweb.binaryowl.BinaryOWLParseException;
import org.semanticweb.binaryowl.BinaryOWLVersion;
import org.semanticweb.binaryowl.change.OntologyChangeDataList;
import org.semanticweb.binaryowl.chunk.BinaryOWLMetadataChunk;
import org.semanticweb.binaryowl.doc.OWLOntologyDocument;
import org.semanticweb.binaryowl.lookup.AnonymousIndividualLookupTable;
import org.semanticweb.binaryowl.lookup.IRILookupTable;
import org.semanticweb.binaryowl.lookup.LiteralLookupTable;
import org.semanticweb.binaryowl.lookup.LookupTable;
import org.semanticweb.binaryowl.owlobject.serializer.BinaryOWLImportsDeclarationSet;
import org.semanticweb.binaryowl.owlobject.serializer.BinaryOWLOntologyID;
import org.semanticweb.binaryowl.serializer.BinaryOWLDocumentBodySerializer;
import org.semanticweb.binaryowl.stream.BinaryOWLInputStream;
import org.semanticweb.binaryowl.stream.BinaryOWLOutputStream;
import org.semanticweb.owlapi.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;

/**
 * Author: Matthew Horridge<br>
 * Stanford University<br>
 * Bio-Medical Informatics Research Group<br>
 * Date: 23/07/2013
 * <p>
 *     A serializer for legacy v1 format
 * </p>
 */
public class BinaryOWLV1DocumentBodySerializer implements BinaryOWLDocumentBodySerializer {
    private static Logger logger = LoggerFactory.getLogger(BinaryOWLV1DocumentBodySerializer.class);
    /**
     * The version written by this serializer - always 1.
     */
    private static final BinaryOWLVersion VERSION = BinaryOWLVersion.getVersion(1);

    public <E extends Throwable> void read(DataInputStream dis, BinaryOWLOntologyDocumentHandler<E> handler, OWLDataFactory df) throws IOException, BinaryOWLParseException, UnloadableImportException, E {

        BinaryOWLInputStream inputStream = new BinaryOWLInputStream(dis, df, VERSION);

        // Metadata
        BinaryOWLMetadataChunk chunk = new BinaryOWLMetadataChunk(inputStream);
        BinaryOWLMetadata metadata = chunk.getMetadata();
        handler.handleDocumentMetaData(metadata);

        handler.handleBeginInitialDocumentBlock();

        // Ontology ID
        BinaryOWLOntologyID serializer = new BinaryOWLOntologyID(inputStream);
        OWLOntologyID ontologyID = serializer.getOntologyID();
        handler.handleOntologyID(ontologyID);

        // Imported ontologies
        BinaryOWLImportsDeclarationSet importsDeclarationSet = new BinaryOWLImportsDeclarationSet(inputStream);
        Set<OWLImportsDeclaration> importsDeclarations = importsDeclarationSet.getImportsDeclarations();
        handler.handleImportsDeclarations(importsDeclarations);

        // IRI Table
        IRILookupTable iriLookupTable = new IRILookupTable(dis);

        // Used to be literal table
        // Skip 1 byte - the interning marker
        inputStream.skip(1);


        LookupTable lookupTable = new LookupTable(iriLookupTable);


        BinaryOWLInputStream lookupTableStream = new BinaryOWLInputStream(dis, lookupTable, df, VERSION);

        // Ontology Annotations
        Set<OWLAnnotation> annotations = lookupTableStream.readOWLObjects();
        handler.handleOntologyAnnotations(annotations);

        // Axiom tables - axioms by type
        for (int i = 0; i < AxiomType.AXIOM_TYPES.size(); i++) {
            Set<OWLAxiom> axiomsOfType = lookupTableStream.readOWLObjects();
            handler.handleAxioms(axiomsOfType);
        }

        handler.handleEndInitialDocumentBlock();
        handler.handleBeginDocumentChangesBlock();
        BinaryOWLInputStream changesInputStream = new BinaryOWLInputStream(dis, df, VERSION);
        // Read any changes that have been appended to the end of the file - no look up table for this
        readOntologyChanges(changesInputStream, handler);
        handler.handleEndDocumentChangesBlock();
        handler.handleEndDocument();
    }

    private void readOntologyChanges(BinaryOWLInputStream inputStream, BinaryOWLOntologyDocumentAppendedChangeHandler changeHandler) throws IOException, BinaryOWLParseException {
        byte chunkFollowsMarker = (byte) inputStream.read();
        while (chunkFollowsMarker != -1) {
            OntologyChangeDataList list = new OntologyChangeDataList(inputStream);
            changeHandler.handleChanges(list);
            chunkFollowsMarker = (byte) inputStream.read();
        }
    }




    public void write(OWLOntologyDocument doc, DataOutputStream dos, BinaryOWLMetadata documentMetadata) throws IOException {

        BinaryOWLOutputStream nonLookupTableOutputStream = new BinaryOWLOutputStream(dos, VERSION);

        // Metadata
        BinaryOWLMetadataChunk metadataChunk = new BinaryOWLMetadataChunk(documentMetadata);
        metadataChunk.write(nonLookupTableOutputStream);

        // Ontology ID
        BinaryOWLOntologyID serializer = new BinaryOWLOntologyID(doc.getOntologyID());
        serializer.write(nonLookupTableOutputStream);

        // Imports
        BinaryOWLImportsDeclarationSet importsDeclarationSet = new BinaryOWLImportsDeclarationSet(doc.getImportsDeclarations());
        importsDeclarationSet.write(nonLookupTableOutputStream);

        // IRI Table
        IRILookupTable iriLookupTable = new IRILookupTable(doc);
        iriLookupTable.write(dos);

        // Literal Table
        final LiteralLookupTable literalLookupTable = new LiteralLookupTable(doc, iriLookupTable, true);
        literalLookupTable.write(dos);

        LookupTable lookupTable = new LookupTable(iriLookupTable,new AnonymousIndividualLookupTable(), literalLookupTable);

        BinaryOWLOutputStream lookupTableOutputStream = new BinaryOWLOutputStream(dos, lookupTable);

        // Ontology Annotations
        lookupTableOutputStream.writeOWLObjects(doc.getAnnotations());

        Comparator<OWLAxiom> subCompare = new Comparator<OWLAxiom>() {
            @Override
            public int compare(OWLAxiom o1, OWLAxiom o2) {
                if (o1 instanceof OWLSubClassOfAxiom && o2 instanceof OWLSubClassOfAxiom) {
                    return compareSubClassAxioms((OWLSubClassOfAxiom) o1, (OWLSubClassOfAxiom) o2);

                } else if (o1 instanceof OWLAnnotationAssertionAxiom && o2 instanceof OWLAnnotationAssertionAxiom) {
                    return compareAnnotationAssertionAxioms((OWLAnnotationAssertionAxiom) o1, (OWLAnnotationAssertionAxiom) o2);

                } else {
                    return o1.compareTo(o2);
                }
            }

            private int compareAnnotationAssertionAxioms(OWLAnnotationAssertionAxiom a1, OWLAnnotationAssertionAxiom a2) {
                int cmp;
                if (a1.getValue() instanceof OWLLiteral && a2.getValue() instanceof OWLLiteral) {
                    cmp = literalLookupTable.getIndex((OWLLiteral) a1.getValue()) - literalLookupTable.getIndex((OWLLiteral) a2.getValue());
                } else {
                    cmp = a1.getValue().compareTo(a2.getValue());
                }
                if (cmp != 0) {
                    return cmp;
                }

                cmp = a1.getProperty().compareTo(a2.getProperty());
                if (cmp != 0) {
                    return cmp;
                }


                cmp = a1.getSubject().compareTo(a2.getSubject());
                if (cmp != 0) {
                    return cmp;
                }


                cmp = compareIterators(
                        new TreeSet<>(a1.getAnnotations()).iterator(),
                        new TreeSet<>(a2.getAnnotations()).iterator());
                if (cmp != 0) {
                    return cmp;
                }
                return cmp;

            }

            private int compareSubClassAxioms(OWLSubClassOfAxiom sc1, OWLSubClassOfAxiom sc2) {
                int result;
                result = sc1.getSuperClass().compareTo(sc2.getSuperClass());
                if (result != 0) {
                    return result;
                }
                result = sc1.getSubClass().compareTo(sc2.getSubClass());
                if (result != 0) {
                    return result;
                }

                return compareIterators(
                        new TreeSet<>(sc1.getAnnotations()).iterator(),
                        new TreeSet<>(sc2.getAnnotations()).iterator());
            }

            private int compareIterators(Iterator<OWLAnnotation> it1, Iterator<OWLAnnotation> it2) {
                while (it1.hasNext() && it2.hasNext()) {
                    int cmp = it1.next().compareTo(it2.next());
                    if (cmp != 0) {
                        return cmp;
                    }
                }
                if (it1.hasNext()) return -1;
                if (it2.hasNext()) return +1;
                return 0;
            }
        };

        // Axiom tables - axioms by type
        for (AxiomType<?> axiomType : AxiomType.AXIOM_TYPES) {
            Set<? extends OWLAxiom> axioms = doc.getAxioms(axiomType);
            ArrayList<? extends OWLAxiom> orderedAxioms = new ArrayList<>(axioms);
            Collections.sort(orderedAxioms, subCompare);
            LinkedHashSet<? extends OWLAxiom> tmp = new LinkedHashSet<>(orderedAxioms);
            lookupTableOutputStream.writeOWLObjects(tmp);
        }

        iriLookupTable.logDeltaCounts();
        literalLookupTable.logDeltaCounts();
        dos.flush();
    }

}
