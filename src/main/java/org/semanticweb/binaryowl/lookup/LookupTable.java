/*
 * This file is part of the OWL API.
 *  
 * The contents of this file are subject to the LGPL License, Version 3.0.
 *
 * Copyright (C) 2011, The University of Manchester
 *  
 * This program is free software: you can redataInputtribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *  
 * This program is dataInputtributed in the hope that it will be useful,
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
 * dataInputtributed under the License is dataInputtributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.semanticweb.binaryowl.lookup;

import org.semanticweb.binaryowl.stream.BinaryOWLOutputStream;
import org.semanticweb.owlapi.model.*;

import java.io.DataInput;
import java.io.IOException;

/**
 * Author: Matthew Horridge<br>
 * Stanford University<br>
 * Bio-Medical Informatics Research Group<br>
 * Date: 25/04/2012
 */
public class LookupTable {

    private IRILookupTable iriLookupTable;

    private AnonymousIndividualLookupTable anonymousIndividualLookupTable;

    private LiteralLookupTable literalLookupTable;
    private GenericLookupTable<OWLAnnotation> annotationLookupTable;
    private static LookupTable EMPTY_LOOKUP_TABLE = new LookupTable(new IRILookupTable(), new AnonymousIndividualLookupTable() {
        @Override
        public int getIndex(OWLAnonymousIndividual ind) {
            return System.identityHashCode(ind);
        }
    }, new LiteralLookupTable() {
        @Override
        public int getIndex(OWLLiteral literal) {
            return -1;
        }
    }
    );

    public LookupTable(IRILookupTable iriLookupTable, AnonymousIndividualLookupTable anonymousIndividualLookupTable, LiteralLookupTable literalLookupTable) {
        this.iriLookupTable = iriLookupTable;
        this.anonymousIndividualLookupTable = anonymousIndividualLookupTable;
        this.literalLookupTable = literalLookupTable;
    }

    public LookupTable(IRILookupTable iriLookupTable) {
        this(iriLookupTable, new AnonymousIndividualLookupTable(), null);
    }

    public LookupTable() {
        this(new IRILookupTable(), new AnonymousIndividualLookupTable(), new LiteralLookupTable());
    }


    public static LookupTable emptyLookupTable() {
        return EMPTY_LOOKUP_TABLE;
    }

    public IRILookupTable getIRILookupTable() {
        return iriLookupTable;
    }

    public AnonymousIndividualLookupTable getAnonymousIndividualLookupTable() {
        return anonymousIndividualLookupTable;
    }

    public LiteralLookupTable getLiteralLookupTable() {
        return literalLookupTable;
    }


    //////////////////////////////////////////////////////////////////


    public void writeIRI(IRI iri, BinaryOWLOutputStream dataOutput) throws IOException {
        iriLookupTable.writeIRI(iri, dataOutput);
    }

    public void writeIRI(IRI iri, BinaryOWLOutputStream binaryOWLOutputStream, DeltaHistoryTable table) throws IOException {
        iriLookupTable.writeIRI(iri, binaryOWLOutputStream, table);
    }

    public IRI readIRI(DataInput dataInput) throws IOException {
        return iriLookupTable.readIRI(dataInput);
    }
    
    public OWLClass readClassIRI(DataInput dataInput) throws IOException {
        return iriLookupTable.readClassIRI(dataInput);
    }


    public OWLObjectProperty readObjectPropertyIRI(DataInput dataInput) throws IOException {
        return iriLookupTable.readObjectPropertyIRI(dataInput);
    }
    
    public OWLDataProperty readDataPropertyIRI(DataInput dataInput) throws IOException {
        return iriLookupTable.readDataPropertyIRI(dataInput);
    }
    
    public OWLAnnotationProperty readAnnotationPropertyIRI(DataInput dataInput) throws IOException {
        return iriLookupTable.readAnnotationPropertyIRI(dataInput);
    }

    public OWLNamedIndividual readIndividualIRI(DataInput dataInput) throws IOException {
        return iriLookupTable.readIndividualIRI(dataInput);
    }

    public OWLDatatype readDatatypeIRI(DataInput dataInput) throws IOException {
        return iriLookupTable.readDataypeIRI(dataInput);
    }

    public void write(OWLAnnotation annotation, BinaryOWLOutputStream bos) throws IOException {
        annotationLookupTable.write(bos, annotation);
    }
    public void writeLiteral(OWLLiteral literal, BinaryOWLOutputStream dos) throws IOException {
        literalLookupTable.writeLiteral(dos, literal);
    }

   public OWLLiteral readLiteral(DataInput dataInput, OWLDataFactory dataFactory) throws IOException {
       // TODO: implement
       throw new NoSuchMethodError("read Literal method not implemented yet");
    }

    public GenericLookupTable<OWLAnnotation> getAnnotationLookupTable() {
        return annotationLookupTable;
    }

    public void setAnnotationLookupTable(GenericLookupTable<OWLAnnotation> annotationLookupTable) {
        this.annotationLookupTable = annotationLookupTable;
    }

    public int compareIRI(IRI o1, IRI o2) {
        return iriLookupTable.compare(o1, o2);
    }

    public int compareLiteral(OWLLiteral o1, OWLLiteral o2) {
        return literalLookupTable.compare(o1, o2);
    }

    public int compareAnnotationValue(OWLAnnotationValue v1, OWLAnnotationValue v2) {
        if (v1 instanceof IRI) {
            if (v2 instanceof IRI) {
                return compareIRI((IRI) v1, (IRI) v2);
            } else {
                return -1;
            }
        } else {
            if (v2 instanceof OWLLiteral) {
                return compareLiteral((OWLLiteral) v1, (OWLLiteral) v2);
            } else {
                return +1;
            }
        }
    }
}
