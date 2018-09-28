package com.hpe.autoframework;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;

import org.apache.commons.net.imap.IMAPClient;
import org.apache.commons.net.io.CRLFLineReader;
import org.testng.TestException;

/**
 * 
 * Class extends IMAPClient to support other character set and automatic IMAP command ID generation.
 *
 */
class ExIMAPClient extends IMAPClient {
	
	/**
	 * Current command ID
	 * in format of '999'
	 */
	protected char[] commandId_ = "001".toCharArray();
	
	/**
	 * Default character set
	 */
    protected static final String DEFAULT_ENCODING = "UTF-8";
    
    /**
     * Character set
     */
    protected Charset cs_;
	
    /**
     * Generate next command ID
     * Increment current command ID to generate it.
     * @return  next command ID
     */
	protected String generateCommandID() {
		String res = new String(commandId_);
		// "increase" the ID for the next call
		boolean carry = true; // want to increment initially
		for (int i = commandId_.length - 1; carry && i >= 0; i--) {
			if (commandId_[i] == '9') {
				commandId_[i] = '0';
			} else {
				commandId_[i]++;
				carry = false; // did not wrap round
			}
		}
		return res;
	}

	/**
	 * Override _reader and __writer to support specified character set
	 */
    @Override
    protected void _connectAction_() throws IOException
    {
        super._connectAction_();
        if (cs_ == null)
        	cs_ = Charset.forName(DEFAULT_ENCODING);
        _reader = new CRLFLineReader(new InputStreamReader(_input_,  cs_));
        __writer = new BufferedWriter(new OutputStreamWriter(_output_, cs_));
    }
    
    /**
     * Set character set
     * @param charset character set name
     */
    public void setCharset(String charset) {
    	try {
    		cs_ = Charset.forName(charset);
    	} catch (UnsupportedCharsetException exp) {
			throw new TestException("Invalid character set", exp);
    	} catch (IllegalCharsetNameException exp) {
			throw new TestException("Invalid character set", exp);
    	}
    }
    
    /**
     * Get the byte length of the data in the specified character set
     * @param data the data
     * @return byte length of the data in the specified character set
     */
	public int getDataLengh(String data) {
		ByteBuffer bf = cs_.encode(data);
		return bf.limit();		
	}
}
