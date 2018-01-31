package ca.polymtl.inf8480.tp1q2.server;

import java.rmi.ConnectException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.security.MessageDigest;

import ca.polymtl.inf8480.tp1q2.shared.FileServerInterface;

public class FileServer implements FileServerInterface {

    ArrayList<String> clientIDs;

    public static void main(String[] args) {
        FileServer fs = new FileServer();
        fs.run();
    }

    public FileServer() {
        super();
        clientIDs = new ArrayList<String>();
    }

    private void run() {
        if (System.getSecurityManager() == null) {
			System.setSecurityManager(new SecurityManager());
        }
        
        try {
            FileServerInterface stub = (FileServerInterface) UnicastRemoteObject.exportObject(this, 0);
            Registry registry = LocateRegistry.getRegistry();
            registry.rebind("fs", stub);
            System.out.println("File server ready.");
        } catch (ConnectException e) {
            System.err.println("Impossible de se connecter au registre RMI. Est-ce que rmiregistry est lancé?\n");
            System.err.println("Erreur" + e.getMessage());
        } catch (Exception e) {
            System.err.println("Erreur: " + e.getMessage());
        }
    }

    String createClientID() throws RemoteException {
        clientIDs.add();
        MessageDigest md = MessageDigest.getInstance("MD5");
        
    }

}

