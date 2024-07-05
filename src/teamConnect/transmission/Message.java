package teamConnect.transmission;

import java.net.DatagramPacket;
import java.util.Arrays;

/**
 * A message with a header, a body, and helper functions to access information
 * about its data. Implemented by ClientMessage and ServerMessage.
 */
public abstract class Message
{
 
 /* INSTANCE VARIABLES */
 
 protected byte[] header;
 protected byte[] body;

 /* CONSTRUCTORS */
 
 /**
  * Empty constructor is needed for proper inheritance
  */
 public Message()
 {
 }

 /**
  * Extract a ServerMessage or ClientMessage that was encapsulated in a
  * DatagramPacket.
  * 
  * @param packet
  */
 public Message(DatagramPacket packet)
 {
  byte[] messageBytes = packet.getData();

  /*
   * Guard case for invalid message size. If a message size is less than the
   * header size, it cannot contain a valid header, and the body must contain at
   * least 1 byte.
   */
  if(messageBytes == null || messageBytes.length < getHeaderSize() + 1
    || messageBytes.length > MessageHelper.MAX_SEGMENT_SIZE)
   throw new IllegalArgumentException("Cannot read malformed client packet");

  /*
   * Extracting the header and body can be accomplished by splitting the message
   * body at the appropriate index
   */
  this.header = Arrays.copyOfRange(messageBytes, 0, getHeaderSize());
  this.body = Arrays.copyOfRange(messageBytes, getHeaderSize(), messageBytes.length);

  /* Create contactInfo from ip and port of sender packet */
  // this.contactInfo = new ContactInfo(packet.getAddress(), packet.getPort());
 }
 
 /* GETTERS */

 abstract int getMessageType();

 abstract int getHeaderSize();
 
 protected byte[] getBody()
 {
  return this.body;
 }
 
 protected String getStringAtByteRange(int start, int end)
 {
  return new String(MessageHelper.trimTrailingNull(Arrays.copyOfRange(getBody(), start, end)));
 }
 
 public String getBodyText()
 {
  return getStringAtByteRange(0, getBody().length);
 }

 public byte[] getFullMessage()
 {
  return MessageHelper.concat(this.header, this.body);
 }
 
 /* HELPERS */
  
 /**
  * Helper method to assure that a message is not being misread as a type that it is not
  * @param type
  */
 protected void assertMessageType(int type)
 {
  if(getMessageType() != type)
   throw new IllegalStateException(
     "Attempted to read type " + getMessageType() + " message as type " + type + " message.");
 }

}
