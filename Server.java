// Server Side
import java.net.*;
import java.io.*;

public class Server 
{
	public static void main(String[] args)
	{
		try 
		{
			// server socket is created
			int serverPort = 4020;
			ServerSocket serverSocket = new ServerSocket(serverPort);
			serverSocket.setSoTimeout(10000);

			while (true)
			{
				// waits for client on port
				System.out.println("Waiting for client on port " + serverSocket.getLocalPort() + "...");
				// server is listening to incoming connection requests from clients
				Socket clientSocket = serverSocket.accept();
				transferFileServer t = new transferFileServer(clientSocket);
				t.run();
			}
		} catch (IOException e) {e.printStackTrace();}
	}	
}

class transferFileServer extends Thread
{
	Socket server;
	// server creates output stream to send data to the client socket
	DataOutputStream toClient;
	// server creates input stream to receive data from the client socket
	DataInputStream fromClient;

	transferFileServer(Socket server)
	{
		try
		{
			this.server = server;
			this.toClient = new DataOutputStream(server.getOutputStream());
			this.fromClient = new DataInputStream(server.getInputStream());
			System.out.println("Just connected to " + server.getRemoteSocketAddress()); 
		} 
		catch(UnknownHostException ex) {ex.printStackTrace();}
		catch(IOException e){e.printStackTrace();}
	}

	// code for ls
	public void ls()
	{
		try
		{
			// gets current directory
			File dir = new File(System.getProperty("user.dir"));
			// stores all the files in current directory in an array of strings
			String childs[] = dir.list();
			// stores all elements of array of strings into a single string
			String lsDirectory = childs[0];
			for (int i = 1; i < childs.length; i++) {
				lsDirectory = lsDirectory.concat("  " + childs[i]);
			}
			// sends single string (containing all files in directory) to client
			toClient.writeUTF(lsDirectory);
		} catch (IOException e) {e.printStackTrace();}
	}

	// code for pwd
	public void pwd()
	{
		try
		{
			// sends name of current directory to server
			toClient.writeUTF(System.getProperty("user.dir"));
		} catch (IOException e) {e.printStackTrace();}
	}

	// sends remote-file to client
	public void sendFile(String fileName)
	{
		try
		{
			// new file is created
			File f = new File(fileName);

			// if file does not exist
			if(!f.exists())
		    {
		        toClient.writeUTF("File Not Found");
		        return;
		    }
			// if file exits
		    else
		    {
				// sends message to allow client to receive file
		        toClient.writeUTF("READY");
				// new file input stream is created 
		        FileInputStream fin = new FileInputStream(f);
		        int ch;

				// sends file to client
		        do
		        {
		            ch=fin.read();
		            toClient.writeUTF(String.valueOf(ch));
		        } while(ch!=-1);  
	  
		        fin.close();    
		        toClient.writeUTF("File Received Successfully");                            
		    }
		} catch (IOException e) {e.printStackTrace();}
	}

	// receives remote-file from client
	public void receiveFile(String fileName)
	{
		try
		{
			// stops if file is not found
			if(fileName.equals("File not found")) {return;}
			// new file is created
		    File f = new File(fileName);
		    String option;
		    
		    if(f.exists())
		    {
		        toClient.writeUTF("File Already Exists");
				// reads option fro client
		        option = fromClient.readUTF();
		    }
		    else
		    {
		        toClient.writeUTF("SendFile");
				// gives permission to client to send file
		        option="Y";
		    }
            
            if(option.equals("Y"))
            {
				// new FileOutputStream is created
                FileOutputStream fout = new FileOutputStream(f);
                int ch;
                String temp;

				// server receives client from file
                do
                {
                    temp=fromClient.readUTF();
                    ch=Integer.parseInt(temp);
                    if(ch!=-1)
                    {
                        fout.write(ch);                    
                    }
                } while(ch!=-1);

                fout.close();
                toClient.writeUTF("File Send Successfully");
            }
            else
            {
				// function doesn't run
                return;
            }
		} catch (IOException e) {e.printStackTrace();}
	}

	public void run()
	{
		try
		{
			while(true)
			{
				System.out.println("Waiting for Command ...");
				String command = null;
				// reads command sent from client
				command = fromClient.readUTF();

				if (command.contains(" "))
				{
					// Splitting command
					String[] split = command.split("\\s+");
					// Storing command
					String request = split[0];
					// Storing filename
					String fileName = split[1];
					if (split.length > 2)
					{
						for (int i = 2; i < split.length; i++)
						{
							fileName = fileName.concat(" " + split[i]);
						}
					}
					
					// handles send remote-file to client
					if (request.equals("get"))
					{
						System.out.println("\t get Command Received");
						sendFile(fileName);
						continue;
					}

					// handles get remote-file to client
					if (request.equals("send"))
					{
						System.out.println("\t send Command Received");
						receiveFile(fileName);
						continue;
					}
				}

				// handles pwd command
				if (command.equals("pwd"))
				{
					System.out.println("\t pwd Command Received");
					pwd();
					continue;
				}

				// handles ls command
				if (command.equals("ls"))
				{
					System.out.println("\t ls Command Received");
					ls();
					continue;
				}
				
				// program is terminated
				if (command.equals("exit"))
				{
					System.out.println("\t Exiting Program");
					System.exit(1);
				}
			
			}
		} 
		catch (Exception e)  {e.printStackTrace();}
		finally
		{
			try 
			{
				//System.out.println("Sockets closing");
				server.close();
			} catch (Exception e) {e.printStackTrace();}
		}
	
	}
}
