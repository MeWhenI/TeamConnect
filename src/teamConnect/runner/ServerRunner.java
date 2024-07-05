package teamConnect.runner;

import java.net.SocketException;

import teamConnect.server.TeamConnectServer;

/**
 * Test class for running an instance of TeamConnectServer
 */
public class ServerRunner
{
 public static void main(String[] args) throws SocketException
 {
  if(args.length != 3)
  {
   System.out.println(
     "Required arguments: port teamNames statuses\nExample: java teamConnect.runner.ServerRunner 2000 \"The A Team/The B Team\" \"Busy/Asleep/Working Hard/Hardly Working\"");
   return;
  }
  int port = Integer.parseInt(args[0]);
  String[] names = args[1].split("/");
  String[] statuses = args[2].split("/");

  TeamConnectServer ts = new TeamConnectServer(port, names, statuses);

  ts.run();

 }
}
