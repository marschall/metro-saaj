/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2017 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * http://glassfish.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

package com.sun.xml.messaging.saaj.util;

import java.util.logging.Logger;

import javax.xml.parsers.SAXParser;
import javax.xml.soap.SOAPException;

import org.xml.sax.*;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.XMLFilterImpl;

import com.sun.xml.messaging.saaj.SOAPExceptionImpl;
import org.xml.sax.helpers.AttributesImpl;

/**
 * Users of this class see a SAX2 XMLReader (via XMLFilterImpl).  This
 * class creates a parent XMLReader via JAXP and installs itself as a SAX2
 * extension LexicalHandler which rejects document type declarations
 * because they are not legal in SOAP.  If the user of this class sets a
 * LexicalHandler, then it forwards events to that handler.
 *
 * 
 * @author Edwin Goei
 */

public class RejectDoctypeSaxFilter extends XMLFilterImpl implements XMLReader, LexicalHandler{
    protected static final Logger log =
    Logger.getLogger(LogDomainConstants.UTIL_DOMAIN,
    "com.sun.xml.messaging.saaj.util.LocalStrings");
    
    /** Standard SAX 2.0 ext property */
    static final String LEXICAL_HANDLER_PROP =
    "http://xml.org/sax/properties/lexical-handler";
    
    static final String WSU_NS = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd".intern();
    static final String SIGNATURE_LNAME = "Signature".intern();
    static final String ENCRYPTED_DATA_LNAME = "EncryptedData".intern();
    static final String DSIG_NS = "http://www.w3.org/2000/09/xmldsig#".intern();
    static final String XENC_NS = "http://www.w3.org/2001/04/xmlenc#".intern();
    static final String ID_NAME = "ID".intern();
    
    /** LexicalHandler to forward events to, if any */
    private LexicalHandler lexicalHandler;
    
    public RejectDoctypeSaxFilter(SAXParser saxParser) throws SOAPException {
        XMLReader xmlReader;
        try {
            xmlReader = saxParser.getXMLReader();
        } catch (Exception e) {
            log.severe("SAAJ0602.util.getXMLReader.exception");
            throw new SOAPExceptionImpl(
            "Couldn't get an XMLReader while constructing a RejectDoctypeSaxFilter",
            e);
        }
        
        // Set ourselves up to be the SAX LexicalHandler
        try {
            xmlReader.setProperty(LEXICAL_HANDLER_PROP, this);
        } catch (Exception e) {
            log.severe("SAAJ0603.util.setProperty.exception");
            throw new SOAPExceptionImpl(
            "Couldn't set the lexical handler property while constructing a RejectDoctypeSaxFilter",
            e);
        }
        
        // Set the parent XMLReader of this SAX filter
        setParent(xmlReader);
    }
    
    /*
     * Override setProperty() to capture any LexicalHandler that is set for
     * forwarding of events.
     */
    @Override
    public void setProperty(String name, Object value)
    throws SAXNotRecognizedException, SAXNotSupportedException {
        if (LEXICAL_HANDLER_PROP.equals(name)) {
            lexicalHandler = (LexicalHandler) value;
        } else {
            super.setProperty(name, value);
        }
    }
    
    //
    // Beginning of SAX LexicalHandler callbacks...
    //
    
    @Override
    public void startDTD(String name, String publicId, String systemId)
    throws SAXException {
        throw new SAXException("Document Type Declaration is not allowed");
    }
    
    @Override
    public void endDTD() throws SAXException {
    }
    
    @Override
    public void startEntity(String name) throws SAXException {
        if (lexicalHandler != null) {
            lexicalHandler.startEntity(name);
        }
    }
    
    @Override
    public void endEntity(String name) throws SAXException {
        if (lexicalHandler != null) {
            lexicalHandler.endEntity(name);
        }
    }
    
    @Override
    public void startCDATA() throws SAXException {
        if (lexicalHandler != null) {
            lexicalHandler.startCDATA();
        }
    }
    
    @Override
    public void endCDATA() throws SAXException {
        if (lexicalHandler != null) {
            lexicalHandler.endCDATA();
        }
    }
    
    @Override
    public void comment(char[] ch, int start, int length) throws SAXException {
        if (lexicalHandler != null) {
            lexicalHandler.comment(ch, start, length);
        }
    }
    
    //
    // End of SAX LexicalHandler callbacks
    //
    
    @Override
    public void startElement(String namespaceURI, String localName,
    String qName, Attributes atts)   throws SAXException{
        if(atts != null ){
            boolean eos = false;
            if(namespaceURI == DSIG_NS || XENC_NS == namespaceURI){
                eos = true;
            }
            int length = atts.getLength();
            AttributesImpl attrImpl = new AttributesImpl();
            for(int i=0; i< length;i++){
                String name = atts.getLocalName(i);
                if(name!=null && (name.equals("Id"))){
                    if(eos || atts.getURI(i) == WSU_NS ){
                        attrImpl.addAttribute(atts.getURI(i), atts.getLocalName(i),
                        atts.getQName(i), ID_NAME, atts.getValue(i));
                    }else{
			 attrImpl.addAttribute(atts.getURI(i), atts.getLocalName(i), atts.getQName(i), atts.getType(i), atts.getValue(i));
                    }
                }else{
                    attrImpl.addAttribute(atts.getURI(i), atts.getLocalName(i),
                    atts.getQName(i), atts.getType(i), atts.getValue(i));
                }
            }
            super.startElement(namespaceURI,localName, qName,attrImpl);
        }else{
            super.startElement(namespaceURI,localName, qName, null);
        }
    }
}
