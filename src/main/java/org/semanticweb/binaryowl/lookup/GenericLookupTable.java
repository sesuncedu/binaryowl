/*
 * This file is part of the OWL API.
 *
 * The contents of this file are subject to the LGPL License, Version 3.0.
 *
 * Copyright (C) 2011, The University of Manchester
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 *
 * Alternatively, the contents of this file may be used under the terms of the Apache License, Version 2.0
 * in which case, the provisions of the Apache License Version 2.0 are applicable instead of those above.
 *
 * Copyright 2011, The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.semanticweb.binaryowl.lookup;

import org.semanticweb.binaryowl.doc.OWLOntologyDocument;
import org.semanticweb.binaryowl.stream.BinaryOWLInputStream;
import org.semanticweb.binaryowl.stream.BinaryOWLOutputStream;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLObjectVisitor;
import org.semanticweb.owlapi.util.OWLObjectVisitorAdapter;
import org.semanticweb.owlapi.util.OWLObjectWalker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import static org.semanticweb.owlapi.util.StructureWalker.AnnotationWalkingControl.DONT_WALK_ANNOTATIONS;
import static org.semanticweb.owlapi.util.StructureWalker.AnnotationWalkingControl.WALK_ANNOTATIONS;

/**
 * Author: Matthew Horridge<br>
 * Stanford University<br>
 * Bio-Medical Informatics Research Group<br>
 * Date: 06/04/2012
 */
public class GenericLookupTable<E extends OWLObject> implements Comparator<E> {
    private static Logger logger = LoggerFactory.getLogger(GenericLookupTable.class);


    protected LinkedHashMap<E, Integer> indexMap = new LinkedHashMap<>();

    protected List<E> tableList = new ArrayList<E>();

    private IRILookupTable iriLookupTable;
    private OWLObjectVisitor interner;

    public GenericLookupTable(IRILookupTable iriLookupTable, OWLObjectVisitor interner) {
        this.iriLookupTable = iriLookupTable;
        this.interner = interner;

    }

    public static class InterningVisitor extends OWLObjectVisitorAdapter {
        public GenericLookupTable lookupTable;

        protected void setLookupTable(GenericLookupTable lookupTable) {
            this.lookupTable = lookupTable;
        }


    }

    public GenericLookupTable(OWLObjectVisitor interner) {
        this(new IRILookupTable(), interner);

    }


    public GenericLookupTable(IRILookupTable iriLookupTable, BinaryOWLInputStream dis, OWLDataFactory df, OWLObjectVisitor interner) throws IOException {
        this(iriLookupTable, interner);
        readTable(dis, df);
    }


    public GenericLookupTable(OWLOntologyDocument ontology, IRILookupTable lookupTable, InterningVisitor interner) {
        this.iriLookupTable = lookupTable;
        this.interner = interner;
        interner.setLookupTable(this);
        intern(ontology);
        deltaHistoryTable = new DeltaHistoryTable(6, indexMap.size(), 0, 2);

    }

    @Override
    public int compare(E o1, E o2) {
        int i1 = getIndex(o1);
        int i2 = getIndex(o2);
        if (i1 < 0 || i2 < 0) {
            return o1.compareTo(o2);
        } else {
            return i1 - i2;
        }
    }

    public void intern(OWLOntologyDocument ontology) {

        OWLObjectWalker<OWLAnnotation> annoWalker = new OWLObjectWalker<OWLAnnotation>(ontology.getAnnotations(), WALK_ANNOTATIONS);
        annoWalker.walkStructure(interner);
        Comparator<OWLAxiom> subCompare = new AxiomSorter(new Comparator<IRI>() {
            @Override
            public int compare(IRI o1, IRI o2) {
                return iriLookupTable.compare(o1, o2);
            }
        });

        for (AxiomType<?> axiomType : AxiomType.AXIOM_TYPES) {
            Set<? extends OWLAxiom> axioms = ontology.getAxioms(axiomType);
            ArrayList<? extends OWLAxiom> orderedAxioms = new ArrayList<>(axioms);
            Collections.sort(orderedAxioms, subCompare);
            OWLObjectWalker<? extends OWLAxiom> walker = new OWLObjectWalker<>(orderedAxioms,DONT_WALK_ANNOTATIONS);
            walker.walkStructure(interner);
        }

    }

    private OWLObjectVisitor getInterner() {
        return interner;
    }


    public void internEntry(E value) {
        if (!indexMap.containsKey(value)) {
            indexMap.put(value, indexMap.size());
        }
    }


    public E get(int index) {
        return tableList.get(index);
    }

    public int getIndex(E key) {

        Integer i = indexMap.get(key);
        if (i != null) {
            return i;
        } else {
            return -1;
        }
    }

    public void write(BinaryOWLOutputStream os) throws IOException {

        os.writeCollectionSize(indexMap.size());
        logger.info("interning Table starting at {}", os.size());

        for (E value : indexMap.keySet()) {
            os.writeOWLObject(value);

        }


    }

    private void readTable(BinaryOWLInputStream is, OWLDataFactory df) throws IOException {
        int size = is.readVariableLengthUnsignedInt();
        tableList = new ArrayList<E>(size + 2);
        for (int i = 0; i < size; i++) {
            E literal = is.readOWLObject();
            tableList.add(literal);
        }
    }


    public E readEntry(BinaryOWLInputStream is, OWLDataFactory df) throws IOException {
        int index = is.readInt();
        return tableList.get(index);

    }


    private DeltaHistoryTable deltaHistoryTable;

    private int indexCount = 0;
    private int bcdCount = 0;
    private int scdCount = 0;
    private int icdCount = 0;

    public void logDeltaCounts() {
        logger.info("{}",
                String.format("index count = %,d, bcd = %,d, scd = %,d, icd = %,d",
                        indexCount, bcdCount, scdCount, icdCount));
    }

    public void write(BinaryOWLOutputStream dos, E value) throws IOException {
        {
            indexCount++;
            int index = getIndex(value);
            if (index <0) {
                logger.error("missed interned annotation");
            };

            long codedDelta = deltaHistoryTable.getCodedDelta(index);
            long delta = deltaHistoryTable.decodeDelta(codedDelta);
            long deltaBaseId = deltaHistoryTable.decodeDeltaBase(codedDelta);
            byte flagByte = (byte) deltaBaseId;

            byte bcd = (byte) (delta);
            if (bcd == delta) {
                int v = flagByte | 0x00;
                dos.writeByte(v);
                dos.writeByte(bcd);
                bcdCount++;
                return;
            }

            short scd = (short) delta;
            if (scd == delta) {
                int v = flagByte | 0x40;
                dos.writeByte(v);
                dos.writeShort(scd);
                scdCount++;
                return;
            }

            int icd = (int) delta;
            if (icd == delta) {
                int v = flagByte | 0x80;
                dos.writeByte(v);
                dos.writeInt(icd);
                icdCount++;
                return;
            }

        }


    }


    public DeltaHistoryTable getDeltaHistoryTable() {
        return deltaHistoryTable;
    }

}
