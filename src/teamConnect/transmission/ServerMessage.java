package teamConnect.transmission;

import java.net.DatagramPacket;
import java.util.Arrays;

/**
 * <p>
 * A ServerMessage is sent by a server in response to a ClientMessage. It may
 * contain acknowledgement of the client's message or information requested by
 * the client. Its header consists of the following:
 * </p>
 * <l>
 * <li>One byte that carries the message's type</li> </l>
 * 
 * <p>
 * Allowed message types are:
 * </p>
 * <l>
 * <li><b>Error:</b> Indicates something went wrong in handling the client's
 * request</li>
 * <li><b>ACK New User:</b> Indicates that a new user profile was succesfully
 * created for the client. The body of the message contains the networkID
 * associated with the new profile.</li>
 * <li><b>Server Description</b> The body of the message contains a list of all
 * the teams and all the status options the server supports.</li>
 * <li><b>Team Status:</b> The body of the message contains the usernames and
 * statuses of each member of a team requested by the client.</li>
 * <li><b>ACK Status Update:</b> Indicates that a client's request to update
 * their status of change teams was handled succesfully.</li></l>
 */
public class ServerMessage extends Message
{
 /* CLASS CONSTANTS */

 /**
  * The header of a ServerMessage is {@value} bytes long. See the specification
  * in the class definition.
  */
 public static final int HEADER_SIZE = 1;

 /**
  * The max amount of data that can be transmitted in the body of a message after
  * leaving {@value #HEADER_SIZE} bytes for the header is {@value}
  */
 public static final int MAX_BODY_SIZE = MessageHelper.MAX_SEGMENT_SIZE - HEADER_SIZE;

 /* Server message types */
 public static final int ERROR = 1;
 public static final int ACK_NEW_USER = 2;
 public static final int SERVER_DESCRIPTION = 3;
 public static final int TEAM_STATUS = 4;
 public static final int ACK_STATUS_UPDATE = 5;

 /* CONSTRUCTORS */

 public ServerMessage(DatagramPacket packet)
 {
  super(packet);
 }

 /**
  * Creates a ServerMessage from header values and a body. This constructor is
  * for use by the server to create a message to send to a client
  * 
  * @param messageType A message type according to the class specification.
  * @param body        A message with 1 <= size <= {@value #MAX_BODY_SIZE}
  */
 public ServerMessage(int messageType, byte[] body)
 {
  /* Guard case for invalid body */
  if(body == null || body.length < 1 || body.length > MAX_BODY_SIZE)
   throw new IllegalArgumentException("Cannot read malformed message body");

  /* Guard case for invalid header values which do not fit into a byte */
  if(messageType != (messageType & 0xff))
   throw new IllegalArgumentException("Invalid header values");

  /*
   * Populate the header with appropriate data according to ServerMessage
   * specification
   */

  header = new byte[HEADER_SIZE];

  header[0] = (byte) messageType;

  /*
   * Populate other variables
   */

  this.body = body;
 }

 public ServerMessage(int messageType, String body)
 {
  this(messageType, body.getBytes());
 }

 /* GETTERS */

 @Override
 public int getMessageType()
 {
  /*
   * Extracts the message type stored in byte 0 of the header. See specification
   * in class documentation.
   */
  return this.header[0] & 0xff;
 }

 public int getNetID()
 {
  assertMessageType(ACK_NEW_USER);

  /*
   * For a message containing a user-name, the user-name should make up the entire
   * body
   */
  return Integer.parseInt(getBodyText());
 }

 public String[] getTeamNames()
 {
  assertMessageType(SERVER_DESCRIPTION);

  return getIdentifiersByDelimiter("#TEAMS");
 }

 public String[] getStatuses()
 {
  assertMessageType(SERVER_DESCRIPTION);

  return getIdentifiersByDelimiter("#STATUSES");
 }

 public String[] getUsernames()
 {
  assertMessageType(TEAM_STATUS);

  return getIdentifiersByDelimiter("#USERS");
 }

 public int[] getStatusVector()
 {
  assertMessageType(TEAM_STATUS);

  int[] statusVec = new int[getBody().length];

  /*
   * A TEAM_STATUS message contains usernames of all its team members followed by
   * the delimiter '#', and then the statuses of each member. Nagivate past that
   * delimiter to extract statuses.
   */
  int messageIndex = 1;
  while (getBody()[messageIndex] != '#')
   messageIndex++;
  messageIndex++;

  /* Extract integer status codes from each byte */
  for(int i = 0; messageIndex < getBody().length; i++)
   statusVec[i] = getBody()[messageIndex++] & 0xff;

  return statusVec;
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
  case ERROR:
   return "ERROR";
  case ACK_NEW_USER:
   return "ACK_NEW_USER";
  case SERVER_DESCRIPTION:
   return "SERVER_DESCRIPTION";
  case TEAM_STATUS:
   return "TEAM_STATUS";
  case ACK_STATUS_UPDATE:
   return "ACK_STATUS_UPDATE";
  default:
   return "UNKNOWN";
  }
 }

 /**
  * Extracts all of the identifiers from the body of a message between the first
  * instance of a specified delimiter and the next demiliter. Note that all
  * delimiters start with '#', and no identifiers may start with '#'.
  *
  * @param delimiter
  * @return The delimited identifiers.
  */
 private String[] getIdentifiersByDelimiter(String delimiter)
 {
  /* A large enough array to store as many identifiers as could fit in a body */
  String[] identifiers = new String[getBody().length / MessageHelper.MAX_IDENTIFIER_SIZE];

  int messageIdx = 0;

  /* Navigate through the past the delimiter */
  while (!getStringAtByteRange(messageIdx, messageIdx + MessageHelper.MAX_IDENTIFIER_SIZE).equals(delimiter))
   messageIdx += MessageHelper.MAX_IDENTIFIER_SIZE;
  messageIdx += MessageHelper.MAX_IDENTIFIER_SIZE;

  /* Collect identifiers until the next delimiter is reached */
  int identIdx = 0;
  while (!getStringAtByteRange(messageIdx, messageIdx + MessageHelper.MAX_IDENTIFIER_SIZE).startsWith("#"))
  {
   identifiers[identIdx] = getStringAtByteRange(messageIdx, messageIdx + MessageHelper.MAX_IDENTIFIER_SIZE);

   identIdx++;
   messageIdx += MessageHelper.MAX_IDENTIFIER_SIZE;
  }

  /* Trim empty spaces in identifier array */
  return Arrays.copyOf(identifiers, identIdx);
 }
}
