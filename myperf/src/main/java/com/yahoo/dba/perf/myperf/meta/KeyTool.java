/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.dba.perf.myperf.meta;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class KeyTool {
	private static Logger logger = Logger.getLogger(KeyTool.class.getName());
	
	private static void maskFileReadonly(String filename)
	{
    	try
    	{
    		if ((new File(filename)).exists())
    			Runtime.getRuntime().exec("chmod 400 " + filename);
    		logger.log(Level.INFO, "Execute shell command to change file permission");
    	}catch(Exception ex)
    	{
    		logger.log(Level.INFO, "Faild to execute shell command", ex );
    	}		
		
	}
	public static class DesEncrypter {
		private static final char[] base64Table =
				   "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz./".toCharArray();
		public static final String BASE64_ENCODING = "BASE64";
		public static final String BASE16_ENCODING = "HEX";


	    Cipher ecipher;
	    Cipher dcipher;

	    //SecretKey key = KeyGenerator.getInstance("DES").generateKey();

	    private static void saveKeyToFile(SecretKey key) throws FileNotFoundException, IOException {
	    	ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(".des"));
	    	oos.writeObject(key);
	    	oos.close();
	    }

	    private static SecretKey getKeyFromFile() throws IOException, ClassNotFoundException {
	    	SecretKey key = null;
	    	ObjectInputStream ois = new ObjectInputStream(new FileInputStream(".des"));
	    	key = (SecretKey) ois.readObject();
	    	ois.close();	    	
	    	return key;
	    }


	    DesEncrypter() {
	    	SecretKey key = null;
	        try {
	        	try {
					key = getKeyFromFile();
				} catch (IOException e) {
					logger.info("Generating new key ...");
				} catch (ClassNotFoundException e) {
					logger.log(Level.SEVERE,"Exception", e);
				}
	        	if(key==null)
	        	{
	        		key = KeyGenerator.getInstance("DES").generateKey();
	        		try {
						saveKeyToFile(key);
					} catch (FileNotFoundException e) {
						logger.log(Level.SEVERE,"Exception", e);
					} catch (IOException e) {
						logger.log(Level.SEVERE,"Exception", e);
					}
	        	}
	            ecipher = Cipher.getInstance("DES");
	            dcipher = Cipher.getInstance("DES");
	            ecipher.init(Cipher.ENCRYPT_MODE, key);
	            dcipher.init(Cipher.DECRYPT_MODE, key);
		    	maskFileReadonly(".des");
	        } catch (javax.crypto.NoSuchPaddingException e) {
	        } catch (java.security.NoSuchAlgorithmException e) {
	        } catch (java.security.InvalidKeyException e) {
	        }
	    }

	    public String encrypt(String str) {
	        try {
	            // Encode the string into bytes using utf-8
	            byte[] utf8 = str.getBytes("UTF8");

	            // Encrypt
	            byte[] enc = ecipher.doFinal(utf8);

	            // Encode bytes to base64 to get a string
	            return tob64(enc);
	        } catch (javax.crypto.BadPaddingException e) {
	        } catch (IllegalBlockSizeException e) {
	        } catch (UnsupportedEncodingException e) {
	        } catch (java.io.IOException e) {
	        }
	        return null;
	    }

	    public String decrypt(String str) {
	        try {
	            // Decode base64 to get bytes
	            byte[] dec = fromb64(str);

	            // Decrypt
	            byte[] utf8 = dcipher.doFinal(dec);

	            // Decode using utf-8
	            return new String(utf8, "UTF8");
	        } catch (javax.crypto.BadPaddingException e) {
	        } catch (IllegalBlockSizeException e) {
	        } catch (UnsupportedEncodingException e) {
	        } catch (java.io.IOException e) {
	        }
	        return null;
	    }
	    
	    public static String tob64(byte[] buffer)
	    {
	       boolean notleading = false;
	       int len = buffer.length, pos = len % 3, c;
	       byte b0 = 0, b1 = 0, b2 = 0;
	       StringBuffer sb = new StringBuffer();

	       switch(pos)
	       {
	          case 1:
	             b2 = buffer[0];
	             break;
	          case 2:
	             b1 = buffer[0];
	             b2 = buffer[1];
	             break;
	       }
	       do
	       {
	          c = (b0 & 0xfc) >>> 2;
	          if(notleading || c != 0)
	          {
	             sb.append(base64Table[c]);
	             notleading = true;
	          }
	          c = ((b0 & 3) << 4) | ((b1 & 0xf0) >>> 4);
	          if(notleading || c != 0)
	          {
	             sb.append(base64Table[c]);
	             notleading = true;
	          }
	          c = ((b1 & 0xf) << 2) | ((b2 & 0xc0) >>> 6);
	          if(notleading || c != 0)
	          {
	             sb.append(base64Table[c]);
	             notleading = true;
	          }
	          c = b2 & 0x3f;
	          if(notleading || c != 0)
	          {
	             sb.append(base64Table[c]);
	             notleading = true;
	          }
	          if(pos >= len)
	             break;
	          else
	          {
	             try
	             {
	                b0 = buffer[pos++];
	                b1 = buffer[pos++];
	                b2 = buffer[pos++];
	             }
	             catch(ArrayIndexOutOfBoundsException e)
	             {
	                break;
	             }
	          }
	       } while(true);

	       if(notleading)
	          return sb.toString();
	       else
	          return "0";
	    }

	    public static byte[] fromb64(String str) throws NumberFormatException
	    {
	       int len = str.length();
	       if(len == 0)
	          throw new NumberFormatException("Empty Base64 string");

	       byte[] a = new byte[len + 1];
	       char c;
	       int i, j;

	       for(i = 0; i < len; ++i)
	       {
	          c = str.charAt(i);
	          try
	          {
	             for(j = 0; c != base64Table[j]; ++j)
	                ;
	          } catch(Exception e)
	          {
	             throw new NumberFormatException("Illegal Base64 character");
	          }
	          a[i] = (byte) j;
	       }

	       i = len - 1;
	       j = len;
	       try
	       {
	          while(true)
	          {
	             a[j] = a[i];
	             if(--i < 0)
	                break;
	             a[j] |= (a[i] & 3) << 6;
	             --j;
	             a[j] = (byte) ((a[i] & 0x3c) >>> 2);
	             if(--i < 0)
	                break;
	             a[j] |= (a[i] & 0xf) << 4;
	             --j;
	             a[j] = (byte) ((a[i] & 0x30) >>> 4);
	             if(--i < 0)
	                break;
	             a[j] |= (a[i] << 2);

	             // Nasty, evil bug in Microsloth's Java interpreter under
	             // Netscape:  The following three lines of code are supposed
	             // to be equivalent, but under the Windows NT VM (Netscape3.0)
	             // using either of the two commented statements would cause
	             // the zero to be placed in a[j] *before* decrementing j.
	             // Weeeeird.
	             a[j-1] = 0; --j;
	             // a[--j] = 0;
	             // --j; a[j] = 0;

	             if(--i < 0)
	                break;
	          }
	       }
	       catch(Exception e)
	       {

	       }

	       try
	       {
	          while(a[j] == 0)
	             ++j;
	       }
	       catch(Exception e)
	       {
	          return new byte[1];
	       }

	       byte[] result = new byte[len - j + 1];
	       System.arraycopy(a, j, result, 0, len - j + 1);
	       return result;
	    }


	}

}
