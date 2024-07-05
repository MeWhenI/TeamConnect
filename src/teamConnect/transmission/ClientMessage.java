package teamConnect.transmission;

import java.net.*;

/**
 * <p>
 * A ClientMessage holds all relevant information that can be in a message sent
 * by a TeamConnectClient to a TeamConnectServer. Its header consists of the following:
 * </p>
 * <l>
 * <li>One byte that carries the message's type</li>
 * <li>Three bytes that store the client's network ID in big-endian form</li>
 * <li>One byte that indicates which team this message relates to</li>
 * <li>One byte that contains the client's status</li> </l>
 * 
 * <p>
 * Allowed message types are:</p< <l>
 * <li><b>Network ID Request:</b> A request for a unique network ID. This message's body contains the displayName the client wishes to use.</li>
 * <li><b>Status Update:</b> A request for the server database to update the
 * status entry for this user:</li>
 * <li><b>Team Status Request:</b> A request for the names of all members of a
 * team and their statuses</li>
 * <li><b>Server Description Request:</b> A request for the teams and status
 * options supported by a server</li></l>
 */
public class ClientMessage extends Message
{
 /* CLASS CONSTANTS */

 /**
  * The header of a ClientMessage is {@value} bytes long. See the specification
  * in the class definition.
  */
 public static final int HEADER_SIZE = 6;

 /**
  * The max amount of data that can be transmitted in the body of a message after
  * leaving {@value #HEADER_SIZE} bytes for the header is {@value}
  */
 public static final int MAX_BODY_SIZE = MessageHelper.MAX_SEGMENT_SIZE - HEADER_SIZE;

 /* All allowed client message types */
 public static final int NET_ID_REQUEST = 1;
 public static final int STATUS_UPDATE = 2;
 public static final int TEAM_STATUS_REQUEST = 3;
 public static final int SERVER_DESCRIPTION_REQUEST = 4;

 /* CONSTRUCTORS */

 /**
  * Create a client message from its component parts. This constructor is
  * primarily for use by clients to construct and send a message.
  * 
  * @param messageType           A message type according to the class
  *                              specification.
  * @param networkID
  * @param acknowledgementNumber
  * @param teamID
  * @param body                  A message with 1 <= size <=
  *                              {@value #MAX_BODY_SIZE}
  */
 public ClientMessage(int messageType, int networkID, int teamID, int status, byte[] body)
 {

  /* Guard case for invalid body */
  if(body == null || body.length < 1 || body.length > MAX_BODY_SIZE)
   throw new IllegalArgumentException("Cannot read malformed message body");

  /* Guard cases for invalid header values that should fit into bytes */
  if(messageType != (messageType & 0xff) || teamID != (teamID & 0xff))
   throw new IllegalArgumentException("Invalid header values");

  /* Guard case for network ID */
  if(networkID < 0 || networkID > MessageHelper.NETWORK_ID_LIMIT)
   throw new IllegalArgumentException("Invalid network ID");

  /*
   * Populate the header with appropriate data according to ClientMessage
   * specification
   */

  header = new byte[HEADER_SIZE];
  header[0] = (byte) messageType;
  header[1] = (byte) (networkID >> 16 & 0xff); // High byte
  header[2] = (byte) (networkID >> 8 & 0xff); // Middle byte
  header[3] = (byte) (networkID & 0xff); // Low byte
  header[4] = (byte) teamID;
  header[5] = (byte) status;

  this.body = body;
 }

 public ClientMessage(int messageType, byte[] body)
 {
  this(messageType, MessageHelper.NETWORK_ID_LIMIT, MessageHelper.TEAM_WILDCARD, MessageHelper.INACTIVE_STATUS, body);
 }

 /**
  * Create a client message from a DatagramPacket sent by a client. This
  * constructor is primarily for use by a server to decode a packet sent by a
  * client.
  * 
  * @param packet
  */
 public ClientMessage(DatagramPacket packet)
 {
  super(packet);
 }

 /* GETTERS */

 @Override
 public int getMessageType()
 {
  /*
   * Extracts a message type stored in byte 0 of the header. See specification in
   * class documentation.
   */
  return this.header[0] & 0xff;
 }

 public int getNetworkID()
 {
  /*
   * Extracts a networkID stored in big-endian form in bytes 1, 2, and 3 of the
   * header. See specification in class documentation. AND with 0xffffff reverts
   * any negative bit values.
   */
  return (header[1] << 16 | header[2] << 8 | header[3]) & 0xffffff;
 }

 public int getTeamID()
 {
  /*
   * Extracts the team number stored in byte 5 of the header. See specification in
   * class documentation.
   */
  return header[4] & 0xff;
 }

 public int getStatus()
 {
  /*
   * Extracts the status code stored in byte 6 of the header. See specification in
   * class documentation.
   */
  return header[5] & 0xff;
 }

 public String getUserName()
 {
  assertMessageType(NET_ID_REQUEST);

  /*
   * For a message containing a user-name, the user-name should make up the entire
   * body
   */
  return getBodyText();
 }
 
 @Override
 int getHeaderSize()
 {
  return HEADER_SIZE;
 }
 
 /* HELPERS */
 
 /**
  * @param messageType
  * @return A String representation of messageType
  */
 public static String typeString(int messageType)
 {
  switch (messageType)
  {
  case NET_ID_REQUEST:
   return "NET_ID_REQUEST";
  case STATUS_UPDATE:
   return "STATUS_UPDATE";
  case TEAM_STATUS_REQUEST:
   return "TEAM_STATUS_REQUEST";
  case SERVER_DESCRIPTION_REQUEST:
   return "SERVER_DESCRIPTION_REQUEST";
  default:
   return "UNKNOWN";
  }
 }

}
