package teamConnect.transmission;

import java.util.Arrays;

/**
 * A static class of constants and helper methods for creating and reading Messages.
 */
public class MessageHelper
{
 /**
  * Messages larger than {@value} bytes will be fragmented into appropriately
  * sized segments
  */
 public static final int MAX_SEGMENT_SIZE = 1 << 10;

 /**
  * Team names, usernames, and statuses may be at most {@value} bytes
  */
 public static final int MAX_IDENTIFIER_SIZE = 16;

 /**
  * A sigil to be used in the team number byte of a header when the message does
  * not relate to a specific team
  */
 public static final int TEAM_WILDCARD = 0xff;

 /**
  * A sigil to communicate a user does not currently have a status set, either
  * because they have gone inactive or because they haven't chosen a status yet.
  */
 public static final int INACTIVE_STATUS = 0xff;

 /**
  * A sigil to represent net ID not set yet.
  */
 public final static int NETWORK_ID_LIMIT = 0xffffff;

 /**
  * Because message bodies need to be at least one byte, this sigil exists to
  * represent an empty body
  */
 public static final byte[] EMPTY_BODY =
 { '&' };

 /**
  * Helper method to concatenate 2 byte arrays
  * 
  * @param arr1
  * @param arr2
  * @return Concatenation of arr1 and arr2
  */
 public static byte[] concat(byte[] arr1, byte[] arr2)
 {
  byte[] arr = new byte[arr1.length + arr2.length];

  for(int i = 0; i < arr1.length; i++)
   arr[i] = arr1[i];
  for(int i = 0; i < arr2.length; i++)
   arr[i + arr1.length] = arr2[i];

  return arr;
 }

 /**
  * Takes a nested array of Strings and formats them into a byte array of entries
  * of a fixed size {@value #MAX_IDENTIFIER_SIZE}.
  * 
  * @param allEntries
  * @return A list of ServerMessages
  */
 private static byte[] generatePaddedDescription(String[][] allEntries, int entryCount)
 {
  /*
   * Each entry will take up MAX_IDENTIFIER_SIZE bytes, and 1 byte is reserved for an ending delimiter
   */
  byte[] description = new byte[entryCount * MAX_IDENTIFIER_SIZE + 1];

  /*
   * Add an ending delimiter
   */
  description[description.length - 1] = '#';

  /*
   * Iterate over each string and add its bytes to a fixed-length entry in the
   * description
   */
  int descriptionIndex = 0;
  for(String[] entryList : allEntries)
   for(String entry : entryList)
   {
    /*
     * Create a fixed-length entry in the description containing the bytes of this
     * entry and right-padded with zero bytes.
     */
    byte[] entryBytes = entry.getBytes();
    for(int i = 0; i < MAX_IDENTIFIER_SIZE; i++)
     description[descriptionIndex++] = i < entryBytes.length ? entryBytes[i] : 0;
   }

  return description;
 }

 /**
  *
  * @param userNames
  * @param teamNumber
  * @return A series of ServerMessages that can be sent to a client to tell it
  *         the user-names in a team.
  */
 public static ServerMessage generateTeamStatusMessage(String[] userNames, byte[] statusVector)
 {
  /* Compile all the strings that will be added to the description */
  String[][] allEntries =
  {
    { "#USERS" }, userNames, };

  /*
   * The team description will contain entries for each user-name, plus a
   * delimiter
   * 
   */
  int entryCount = userNames.length + 1;
  byte[] description = concat(generatePaddedDescription(allEntries, entryCount), statusVector);
  return new ServerMessage(ServerMessage.TEAM_STATUS, description);
 }

 /**
  *
  * @param teamNames
  * @param statusNames
  * @return A series of ServerMessages that can be sent to a client to tell it
  *         the teamNames and statusNames supported by a server.
  */
 public static ServerMessage generateServerDescription(String[] teamNames, String[] statusNames)
 {
  /* Compile all the strings that will be added to the description */
  String[][] allEntries =
  {
    { "#TEAMS" }, teamNames,
    { "#STATUSES" }, statusNames, };

  /*
   * The server description will contain entries for each team name and status
   * name, plus one delimiter for each of those two categories.
   */
  int entryCount = teamNames.length + statusNames.length + 2;
  byte[] description = generatePaddedDescription(allEntries, entryCount);

  return new ServerMessage(ServerMessage.SERVER_DESCRIPTION, description);
 }

 /**
  * 
  * @param identifier
  * @return Whether an identifier is invalid for a team name, username, or
  *         status. To be valid, it must contain 1 to
  *         {@value MAX_IDENTIFIER_SIZE} letters, numbers, and spaces.
  */
 public static boolean isValidIdentifier(String identifier)
 {
  return identifier.matches("[a-zA-Z0-9 ]{1," + MAX_IDENTIFIER_SIZE + "}");
 }

 /**
  * 
  * @param arr
  * @return arr with 0 or more trailing zero bytes removed
  */
 public static byte[] trimTrailingNull(byte[] arr)
 {
  for(int lastIndex = arr.length - 1; lastIndex >= 0; lastIndex--)
   if(arr[lastIndex] != 0)
    return Arrays.copyOf(arr, lastIndex + 1);

  /* If the entire array is null bytes, return an empty array */
  return new byte[0];
 }
}
