// Client Side
import java.io.*;
import java.net.*;
import java.util.*;

public class Client {

	public static void main(String[] args)
	{
		try
		{
			// client assumes that server is listening for connection request
			// on port serverPort via TCP
			int serverPort = 4020;
			String host = args[0];
			//InetAddress host = InetAddress.getByName("localhost"); 
			System.out.println("Connecting to server on port " + serverPort); 

			// client's request is either accepted or denied	
			Socket socket = new Socket(host,serverPort); 
			transferFileClient t = new transferFileClient(socket);
			t.run();
		} catch (IOException e) {e.printStackTrace();}
	}
}

class transferFileClient	
{
	Socket socket;
	// client creates output stream to send data to the server socket
	DataOutputStream toServer;
	// client creates input stream to receive data from server socket
	DataInputStream fromServer;
	// reads input
	BufferedReader br;

	transferFileClient(Socket socket)
	{
		try
		{
			this.socket = socket;
			this.toServer = new DataOutputStream(socket.getOutputStream());
			this.fromServer = new DataInputStream(socket.getInputStream());
			this.br = new BufferedReader(new InputStreamReader(System.in));
			System.out.println("Just connected to " + socket.getRemoteSocketAddress());
		}
		catch (UnknownHostException ex) {ex.printStackTrace();}
		catch (IOException e) 
		{
			// If connection cannot be made
			e.printStackTrace();
		}
	}

	// code for ls command
	public void ls()
	{
		try
		{
			// string of files in server
			String lsDirectory = fromServer.readUTF();
			// split string of files in server
			String split[] = lsDirectory.split("  ");
			// print all files in server
			for (int i = 0; i < split.length; i++)
			{
				System.out.println(split[i]);
			}
		}
		catch (IOException e) {e.printStackTrace();}
	}

	// code for pwd command
	public void pwd() 
	{
		try
		{
			// gets current directory from server
			String currentDirectory = fromServer.readUTF();
			// current directory is printed
			System.out.println(currentDirectory);
		}
		catch (IOException e) {e.printStackTrace();}
	}

	// code for get remote-file from server
	public void receiveFile(String fileName)
	{
		try 
		{
			// send name of file to server
			toServer.writeUTF(fileName);
			// receive message from server
			String msgFromServer = fromServer.readUTF();

			if (msgFromServer.equals("File Not Found"))
			{
				System.out.println("File not found on Server");
            	return;
			}
			else if (msgFromServer.equals("READY"))
			{
				System.out.println("Receiving File ...");
				// new file is created
            	File f = new File(fileName);

				// if file exists
				if (f.exists())
				{
					String Option;
					// checks if user wants to overwrite current file
                	System.out.println("File Already Exists. Want to OverWrite (Y/N) ?");
                	Option=br.readLine();            
		            if(Option=="N")    
		            {
		                toServer.flush();
		                return;    
		            }    
				}

				// file output stream for client is created
				FileOutputStream fout = new FileOutputStream(f);
		        int ch;
		        String temp;

				// transfer file from server to client
		        do
		        {
		            temp=fromServer.readUTF();
		            ch=Integer.parseInt(temp);
		            if (ch!=-1) {fout.write(ch);}
		        } while(ch!=-1);
		        fout.close();
		        System.out.println(fromServer.readUTF());
			}
		} catch (IOException e) {e.printStackTrace();}
	}

	// code for send remote-file to server
	public void sendFile(String fileName)
	{
		try 
		{
			// new file is declared    
		    File f = new File(fileName);

			// if file does not exists
		    if(!f.exists())
		    {
		        System.out.println("File not Exists...");
		        toServer.writeUTF("File not found");
		        return;
		    }
		    
			// sends name of file to server
		    toServer.writeUTF(fileName);
		    // receives a message from server
		    String msgFromServer=fromServer.readUTF();

			// checks if file exists in server
		    if(msgFromServer.equals("File Already Exists"))
		    {
		        String Option;
				// checks if client wants to overwrite file in server
		        System.out.println("File Already Exists. Want to OverWrite (Y/N) ?");
				// sends message to server
		        Option=br.readLine();            
		        if(Option=="Y")    
		        {
		            toServer.writeUTF("Y");
		        }
		        else
		        {
		            toServer.writeUTF("N");
		            return;
		        }
		    }
		    
		    System.out.println("Sending File ...");
			// new FileInputStream is created
		    FileInputStream fin = new FileInputStream(f);

			// sends file to server
		    int ch;
		    do
		    {
		        ch=fin.read();
		        toServer.writeUTF(String.valueOf(ch));
		    }
		    while(ch!=-1);
		    fin.close();
		    System.out.println(fromServer.readUTF());
		} catch (IOException e) {e.printStackTrace();}
	}

	public void run()
	{
		try
		{
			while(true)
			{
				System.out.print("\n> ");

				// reads command
				String command = br.readLine();

				// handles get, send, and cd (commands that involce files and directories)
				if (command.contains(" "))
				{
					// splits command
					String[] split = command.split("\\s+");
					// stores command
					String request = split[0];
					// stores file or directory name
					String fileName = split[1];
					if (split.length > 2)
					{
						for (int i = 2; i < split.length; i++)
						{
							fileName = fileName.concat(" " + split[i]);
						}
					}

					// complete command is restored in string
					String requestServer = request;
					requestServer = requestServer.concat(" " + fileName);

					// handles get remote-file
					if (request.equals("get"))
					{
						// get remote-file is sent to server
						toServer.writeUTF(requestServer);
						receiveFile(fileName);
					}

					// handles send remote-file
					if (request.equals("send"))
					{
						// send remote-file is sent to server
						toServer.writeUTF(requestServer);
						sendFile(fileName);
					}

					/*/if (request.equals("cd"))
					{
						toServer.writeUTF(requestServer);
						receiveFile();
					}/*/
				}

				// handles pwd command
				if (command.equals("pwd"))
				{
					// send pwd to server
					toServer.writeUTF("pwd");
					pwd();
				}

				// handles ls command
				if (command.equals("ls"))
				{
					toServer.writeUTF("ls");
					// send ls to server
					ls();
				}
				// handles exit command
				else if (command.equals("exit"))
				{
					// tells server to terminate program
					toServer.writeUTF("exit");
					System.exit(1);
				}
			}
		} 
		catch (IOException e) {e.printStackTrace();}
		finally
		{
			try
			{
				// sockets and output streams are closed
				toServer.close();
				fromServer.close();
				socket.close();
			}
			catch (IOException e) {e.printStackTrace();}
		}
	}
}
