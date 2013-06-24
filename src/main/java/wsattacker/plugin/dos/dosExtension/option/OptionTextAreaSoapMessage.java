/**
 * WS-Attacker - A Modular Web Services Penetration Testing Framework Copyright
 * (C) 2012 Andreas Falkenberg
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package wsattacker.plugin.dos.dosExtension.option;

import java.util.ArrayList;
import java.util.List;

import wsattacker.main.composition.plugin.option.AbstractOptionChoice;
import wsattacker.main.composition.testsuite.CurrentOperationObserver;
import wsattacker.main.testsuite.TestSuite;

import com.eviware.soapui.impl.wsdl.WsdlOperation;
import com.eviware.soapui.impl.wsdl.WsdlRequest;
import com.eviware.soapui.model.iface.Operation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;
import wsattacker.main.composition.plugin.option.AbstractOptionString;
import wsattacker.main.composition.testsuite.CurrentRequestContentChangeObserver;
import wsattacker.main.composition.testsuite.CurrentRequestObserver;
import wsattacker.plugin.dos.dosExtension.util.UtilDos;
import wsattacker.util.SoapUtilities;

/**
 * Adds textarea with original SoapRequest and payload placeholder! - everytime
 * Operation Changes -> has to implement: currentOperationChanged +
 * noCurrentOperation -
 */
public class OptionTextAreaSoapMessage extends AbstractOptionString implements CurrentRequestContentChangeObserver, CurrentRequestObserver {

    /*
     * Where should we place the payload placeholder?
     * NONE = no payload placeholder
     */
    public enum PayloadPosition {
	NONE,
	BODYLASTCHILDELEMENT,
	BODYLASTCHILDELEMENTATTRIBUTES,
	BODYELEMENTATTRIBUTES,
	HEADERLASTCHILDELEMENT,
	HEADERLASTCHILDELEMENTATTRIBUTES,
	ENVELOPEELEMENTATTRIBUTES,
    }


    private static final long serialVersionUID = 1L;
    private String defaultSoapMessage = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?><Envelope><Header/><Body></Body></Envelope>";
    private String currentMessage;
    private PayloadPosition payloadPosition;

    public OptionTextAreaSoapMessage(String name, String description, PayloadPosition payloadPosition) {
	super(name, description);
	this.payloadPosition = payloadPosition;
	//TestSuite.getInstance().getCurrentRequest().addCurrentRequestObserver(this);
	TestSuite.getInstance().getCurrentRequest().addCurrentRequestContentObserver(this);
	TestSuite.getInstance().getCurrentRequest().addCurrentRequestObserver(this);
    }

    @Override
    public boolean isValid(String value) {
	if (value != null && value.length() > 0) {
	    return true;
	} else {
	    return false;
	}
    }

    /*
     * if any payload placeholder is present it will get replaced by supplied payload string
     * @param soapMessage
     * @param payloadString
     * @return
     */
    public String replacePlaceholderWithPayload(String soapMessage, String payloadString){

	switch (this.payloadPosition) {
	   case NONE:
	       break;
	   case BODYLASTCHILDELEMENT:
	       return soapMessage.replace("$$PAYLOADELEMENT$$", payloadString);
	   case BODYLASTCHILDELEMENTATTRIBUTES:
	       return soapMessage.replace("$$PAYLOADATTR$$", payloadString);
	   case BODYELEMENTATTRIBUTES:
	       return soapMessage.replace("$$PAYLOADATTR$$", payloadString);
	   case HEADERLASTCHILDELEMENT:
	       return soapMessage.replace("$$PAYLOADELEMENT$$", payloadString);
	   case HEADERLASTCHILDELEMENTATTRIBUTES:
	       return soapMessage.replace("$$PAYLOADATTR$$", payloadString);
	   case ENVELOPEELEMENTATTRIBUTES:
	       return soapMessage.replace("$$PAYLOADATTR$$", payloadString);
	   default:
	       break;
	}
	return soapMessage;
    }

    /*
     * Insert payload placeholder in current request and write to currentMessage String
     * all depending on payloadPosition
     */
    public void insertPayloadPlaceholder(String inMessage) {
	String preSoapMessage;
	Document doc = null;
	Element soapBodyElement = null;
	Element soapHeaderElement = null;
	Element soapEnvelopeElement = null;
	Element newElementAttributes = null;
	Element newElement = null;

	try {
	    doc = SoapUtilities.stringToDom(inMessage);
	} catch (SAXException e1) {
	    e1.printStackTrace();
	}

        switch (payloadPosition) {
            case NONE:
                break;
            case BODYLASTCHILDELEMENT:
		newElement = doc.createElement("PAYLOADELEMENT");
		soapBodyElement = (Element) UtilDos.getSoapBody(doc);
		soapBodyElement.appendChild(newElement);
                break;
            case BODYLASTCHILDELEMENTATTRIBUTES:
		newElementAttributes = doc.createElement("attackElement");
		soapBodyElement = (Element) UtilDos.getSoapBody(doc);
		soapBodyElement.appendChild(newElementAttributes);
		newElementAttributes.setAttribute("PAYLOAD", "PAYLOAD");
		newElementAttributes.setTextContent("test");
                break;
            case BODYELEMENTATTRIBUTES:
		soapBodyElement = (Element) UtilDos.getSoapBody(doc);
		soapBodyElement.setAttribute("PAYLOAD", "PAYLOAD");
                break;
            case HEADERLASTCHILDELEMENT:
		newElement = doc.createElement("PAYLOADELEMENT");
		soapHeaderElement = (Element) UtilDos.getSoapHeader(doc);
		soapHeaderElement.appendChild(newElement);
                break;
            case HEADERLASTCHILDELEMENTATTRIBUTES:
		newElementAttributes = doc.createElement("attackElement");
		soapHeaderElement = (Element) UtilDos.getSoapHeader(doc);
		soapHeaderElement.appendChild(newElementAttributes);
		newElementAttributes.setAttribute("PAYLOAD", "PAYLOAD");
		newElementAttributes.setTextContent("test");
                break;
            case ENVELOPEELEMENTATTRIBUTES:
		soapEnvelopeElement = (Element) UtilDos.getSoapEnvelope(doc);
		soapEnvelopeElement.setAttribute("PAYLOAD", "PAYLOAD");
                break;
            default:
                break;
        }

	// replace valid placeholder Nodes with placeholder strings
	preSoapMessage = SoapUtilities.domToString(doc);
	preSoapMessage = preSoapMessage.replace("PAYLOAD=\"PAYLOAD\"", "$$PAYLOADATTR$$");
	preSoapMessage = preSoapMessage.replace("<PAYLOADELEMENT/>", "$$PAYLOADELEMENT$$");

	currentMessage = preSoapMessage;
    }

    @Override
    public void currentRequestContentChanged(String newContent, String oldContent) {
	System.out.println("requestChanged");
	this.insertPayloadPlaceholder(newContent);
	this.setValue(currentMessage);
    }

    @Override
    public void noCurrentRequestcontent() {
	this.setValue(defaultSoapMessage);
    }

    @Override
    public void currentRequestChanged(WsdlRequest newRequest, WsdlRequest oldRequest) {
	this.insertPayloadPlaceholder(newRequest.getRequestContent());
	this.setValue(currentMessage);
    }

    @Override
    public void noCurrentRequest() {
	this.insertPayloadPlaceholder(defaultSoapMessage);
	this.setValue(currentMessage);
    }
}



