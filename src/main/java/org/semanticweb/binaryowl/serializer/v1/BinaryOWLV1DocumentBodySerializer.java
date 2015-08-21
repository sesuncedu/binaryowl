package org.semanticweb.binaryowl.serializer.v1;

import com.google.common.base.Optional;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import org.semanticweb.binaryowl.BinaryOWLMetadata;
import org.semanticweb.binaryowl.BinaryOWLOntologyDocumentAppendedChangeHandler;
import org.semanticweb.binaryowl.BinaryOWLOntologyDocumentHandler;
import org.semanticweb.binaryowl.BinaryOWLParseException;
import org.semanticweb.binaryowl.BinaryOWLVersion;
import org.semanticweb.binaryowl.change.OntologyChangeDataList;
import org.semanticweb.binaryowl.chunk.BinaryOWLMetadataChunk;
import org.semanticweb.binaryowl.doc.OWLOntologyDocument;
import org.semanticweb.binaryowl.lookup.AnonymousIndividualLookupTable;
import org.semanticweb.binaryowl.lookup.AxiomSorter;
import org.semanticweb.binaryowl.lookup.GenericLookupTable;
import org.semanticweb.binaryowl.lookup.IRILookupTable;
import org.semanticweb.binaryowl.lookup.LiteralLookupTable;
import org.semanticweb.binaryowl.lookup.LookupTable;
import org.semanticweb.binaryowl.lookup.PrimarySortKeyFinder;
import org.semanticweb.binaryowl.owlobject.OWLObjectBinaryTypeSelector;
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
import java.util.List;
import java.util.Set;

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
        final IRILookupTable iriLookupTable = new IRILookupTable(doc);
        logger.info("iri table starts at {}", String.format("%,d", nonLookupTableOutputStream.size()));
        iriLookupTable.write(dos);
        logger.info("iri table done at {}, literal table starts", String.format("%,d", nonLookupTableOutputStream.size()));


        // Literal Table
        final LiteralLookupTable literalLookupTable = new LiteralLookupTable(doc, iriLookupTable, true);
        literalLookupTable.write(nonLookupTableOutputStream);
        logger.info("literal table done at {}, rest of stuff starts", String.format("%,d", nonLookupTableOutputStream.size()));

        final LookupTable lookupTable = new LookupTable(iriLookupTable,new AnonymousIndividualLookupTable(), literalLookupTable);

        BinaryOWLOutputStream lookupTableOutputStream = new BinaryOWLOutputStream(dos, lookupTable);

        Set<OWLAxiom> axiomSet = doc.getOntology().getAxioms();
        Comparator<OWLAxiom> axiomComparator = new AxiomSorter(iriLookupTable, new Comparator<OWLLiteral>() {
            @Override
            public int compare(OWLLiteral o1, OWLLiteral o2) {
                return literalLookupTable.compare(o1,o2);
            }
        });

        final Multimap<IRI,OWLAxiom> axiomsByIRI = MultimapBuilder.treeKeys(iriLookupTable).treeSetValues(axiomComparator).build();

        for (Iterator<OWLAxiom> iterator = axiomSet.iterator(); iterator.hasNext(); ) {
            OWLAxiom axiom = iterator.next();
            Optional<IRI> key = PrimarySortKeyFinder.getPrimarySortKey(axiom);
            if(key.isPresent()) {
                axiomsByIRI.put(key.get(),axiom);
                iterator.remove();
            }
        }
        ArrayList<OWLAxiom> generalAxioms = new ArrayList<>(axiomSet);
        Collections.sort(generalAxioms,axiomComparator);


        GenericLookupTable.InterningVisitor interner = new GenericLookupTable.InterningVisitor() {
            @Override
            public void visit(OWLAnnotation node) {
                lookupTable.internEntry(node);
            }
        };
        final Comparator<OWLAnnotation> annotationComparator = new Comparator<OWLAnnotation>() {
            @Override
            public int compare(OWLAnnotation o1, OWLAnnotation o2) {
                int cmp;
                cmp = lookupTable.compareIRI(o1.getProperty().getIRI(),o2.getProperty().getIRI());
                if(cmp != 0) {
                    return cmp;
                }
                cmp = lookupTable.compareAnnotationValue(o1.getValue(),o2.getValue());
                if(cmp != 0) {
                    return cmp;
                }
                Iterator<OWLAnnotation> i1 = o1.getAnnotations().iterator();
                Iterator<OWLAnnotation> i2 = o2.getAnnotations().iterator();
                while(i1.hasNext() && i2.hasNext()) {
                    cmp = compare(i1.next(),i2.next());
                    if(cmp != 0) {
                        return cmp;
                    }
                }
                if(i1.hasNext()) {
                    return +1;
                }
                if(i2.hasNext()) {
                    return -1;
                }
                return 0;
            }
        };
        GenericLookupTable<OWLAnnotation> annotationLookupTable = new GenericLookupTable<OWLAnnotation>(doc, iriLookupTable, interner) {
            @Override
            public void intern(OWLOntologyDocument doc) {
                internAnnotations(doc.getOntology());
                for (OWLAxiom axiom : axiomsByIRI.values()) {
                    internAnnotations(axiom);
                }
            }

            private void internAnnotations(HasAnnotations hasAnnotations) {
                if (hasAnnotations.getAnnotations().size()>0) {
                    List<OWLAnnotation> list = new ArrayList<>(hasAnnotations.getAnnotations());
                    Collections.sort(list,annotationComparator);
                    for (OWLAnnotation annotation : list) {
                        internEntry(annotation);
                        internAnnotations(annotation);
                    }
                }
            }

            @Override
            public void internEntry(OWLAnnotation value) {
                if (!indexMap.containsKey(value)) {
                    indexMap.put(value, indexMap.size());
                }
            }
        };
        lookupTable.setAnnotationLookupTable(annotationLookupTable);
        annotationLookupTable.write(lookupTableOutputStream);

        //
        //
         lookupTableOutputStream.writeOWLObjects(doc.getAnnotations());


        lookupTableOutputStream.writeOWLObjectList(axiomsByIRI.values());

        lookupTableOutputStream.writeOWLObjectList(generalAxioms);

        /*handleAxiomsOfType(doc, iriLookupTable, literalLookupTable,lookupTableOutputStream, axiomComparator, AxiomType.ANNOTATION_ASSERTION);

        // Axiom tables - axioms by type
        for (AxiomType<?> axiomType : AxiomType.AXIOM_TYPES) {
            if(axiomType != AxiomType.ANNOTATION_ASSERTION) {
                handleAxiomsOfType(doc, iriLookupTable, literalLookupTable,lookupTableOutputStream, axiomComparator, axiomType);
            }
        }
        */
        iriLookupTable.logDeltaCounts();
        iriLookupTable.getDeltaHistoryTable().dumpCounts();
        literalLookupTable.logDeltaCounts();
        literalLookupTable.getDeltaHistoryTable().dumpCounts();
        lookupTable.getAnnotationLookupTable().logDeltaCounts();
        lookupTable.getAnnotationLookupTable().getDeltaHistoryTable().dumpCounts();
        dos.flush();
    }

    private void handleAxiomsOfType(OWLOntologyDocument doc, final IRILookupTable iriLookupTable, final LiteralLookupTable literalLookupTable, BinaryOWLOutputStream lookupTableOutputStream, Comparator<OWLAxiom> subCompare, AxiomType<?> axiomType) throws IOException {
        Set<? extends OWLAxiom> axioms = doc.getAxioms(axiomType);
        if (!axioms.isEmpty()) {
            ArrayList<? extends OWLAxiom> orderedAxioms = new ArrayList<>(axioms);
            if (axiomType == AxiomType.DECLARATION) {
                Comparator<IRI> iriComparator = new Comparator<IRI>() {
                    @Override
                    public int compare(IRI o1, IRI o2) {
                        return iriLookupTable.compare(o1, o2);
                    }
                };

                Comparator<OWLLiteral> literalComparator = new Comparator<OWLLiteral>() {
                    @Override
                    public int compare(OWLLiteral o1, OWLLiteral o2) {
                        return literalLookupTable.compare(o1, o2);
                    }
                };
                AxiomSorter declarationComparator = new AxiomSorter(iriComparator, literalComparator) {
                    OWLObjectBinaryTypeSelector selector = new OWLObjectBinaryTypeSelector();

                    @Override
                    public int compare(OWLAxiom o1, OWLAxiom o2) {
                        if (o1 instanceof OWLDeclarationAxiom && o2 instanceof OWLDeclarationAxiom) {
                            OWLDeclarationAxiom d1 = (OWLDeclarationAxiom) o1;
                            OWLDeclarationAxiom d2 = (OWLDeclarationAxiom) o2;
                            int cmp = compareEntity(d1.getEntity(),d2.getEntity());
                            if (cmp != 0) {
                                return cmp;
                            }
                            cmp = d1.accept(selector).getMarker() - d2.accept(selector).getMarker();
                            if (cmp != 0) {
                                return cmp;
                            }
                            return compareAnnotations(d1.getAnnotations(),d2.getAnnotations());
                        } else {
                            return super.compare(o1, o2);
                        }
                    }
                };
                Collections.sort(orderedAxioms, declarationComparator);
            } else {
                Collections.sort(orderedAxioms, subCompare);
            }
            LinkedHashSet<? extends OWLAxiom> tmp = new LinkedHashSet<>(orderedAxioms);
            iriLookupTable.resetCounts();
            iriLookupTable.getDeltaHistoryTable().resetCounts();
            logger.info("Axiom Type: {}",axiomType.getName());

            lookupTableOutputStream.writeOWLObjects(tmp);
            iriLookupTable.logDeltaCounts();
            iriLookupTable.getDeltaHistoryTable().dumpCounts();

            logger.info("---");
        }
    }

}
