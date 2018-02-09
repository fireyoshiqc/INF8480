package ca.polymtl.inf8480.tp1q2.client;

import ca.polymtl.inf8480.tp1q2.shared.FileServerInterface;
import ca.polymtl.inf8480.tp1q2.shared.MD5Hasher;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;

public class FileClient {

    private static final String DISTANT_HOSTNAME = "127.0.0.1";//"132.207.12.231";
    private final String FSROOT = "clientfiles/";
    private final String CLIENTID_FILE = ".client.id";
    private FileServerInterface distantServerStub;
    private String clientID;

    public FileClient() {

        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new SecurityManager());
        }

        distantServerStub = loadServerStub(DISTANT_HOSTNAME);

    }

    public static void main(String[] args) {

        FileClient fc = new FileClient();

        if (args.length > 0) {
            fc.createClientID();
            String command = args[0].toLowerCase();
            try {
                switch (command) {
                    case "create":
                        fc.create(args[1]);
                        break;
                    case "list":
                        fc.list();
                        break;
                    case "synclocaldirectory":
                        fc.syncLocalDirectory();
                        break;
                    case "get":
                        fc.get(args[1]);
                        break;
                    case "lock":
                        fc.lock(args[1]);
                        break;
                    case "push":
                        fc.push(args[1]);
                        break;
                    case "default":
                        System.out.println("Opération '" + command + "' invalide.\n" +
                                "Les opérations valides sont: create, list, syncLocalDirectory, get, lock, push.");
                        break;
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                System.out.println("La commande '" + command + "' nécessite un nom de fichier comme second argument!");
            }
        } else {

        }
    }

    private FileServerInterface loadServerStub(String hostname) {
        FileServerInterface stub = null;

        try {
            Registry registry = LocateRegistry.getRegistry(hostname);
            stub = (FileServerInterface) registry.lookup("fs");
        } catch (NotBoundException e) {
            System.out.println("Erreur: Le nom '" + e.getMessage()
                    + "' n'est pas défini dans le registre.");
        } catch (RemoteException e) {
            System.out.println("Erreur: " + e.getMessage());
        }

        return stub;
    }

    private String generateFileChecksum(String filename) {
        File f = new File(FSROOT + filename);
        try {
            if (f.exists()) {
                FileInputStream fis = new FileInputStream(f);
                byte[] contents = new byte[(int) f.length()];
                fis.read(contents);
                fis.close();
                return MD5Hasher.hashMD5(contents);
            } else {
                return null;
            }
        } catch (IOException e) {
            System.out.println("Erreur: " + e.getMessage());
        }
        return null;
    }

    private void createClientID() {
        File f = new File(CLIENTID_FILE);
        try {
            if (f.createNewFile()) {
                clientID = distantServerStub.createClientID();
                FileOutputStream fos = new FileOutputStream(f);
                fos.write(clientID.getBytes());
                fos.close();
            } else {
                FileInputStream fis = new FileInputStream(f);
                byte[] contents = new byte[(int) f.length()];
                fis.read(contents);
                clientID = new String(contents);
            }
        } catch (IOException | NoSuchAlgorithmException e) {
            System.out.println("Erreur: " + e.getMessage());
        }
    }

    private void create(String filename) {
        try {
            System.out.println(distantServerStub.create(filename));
        } catch (RemoteException e) {
            System.out.println("Erreur: " + e.getMessage());
        }
    }

    private void list() {
        try {
            for (String file : distantServerStub.list()) {
                System.out.println(file);
            }
        } catch (RemoteException e) {
            System.out.println("Erreur: " + e.getMessage());
        }
    }

    private void syncLocalDirectory() {
        try {
            HashMap<String, byte[]> files = distantServerStub.syncLocalDirectory();
            if (files != null) {
                files.forEach((name, contents) -> {
                    File f = new File(FSROOT + name);
                    try {
                        f.createNewFile();
                        FileOutputStream fos = new FileOutputStream(f);
                        fos.write(contents);
                        fos.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            }
        } catch (RemoteException e) {
            System.out.println("Erreur: " + e.getMessage());
        }
    }

    private void get(String filename) {
        File f = new File(FSROOT + filename);
        byte[] result = null;
        try {
            result = distantServerStub.get(filename, this.generateFileChecksum(filename));
            if (result != null) {
                FileOutputStream fos = new FileOutputStream(f);
                fos.write(result);
                fos.close();
                System.out.println(filename + " synchronisé.");
            } else {
                System.out.println(filename + " est déjà à jour.");
            }
        } catch (IOException e) {
            System.out.println("Erreur: " + e.getMessage());
        }
    }

    private void lock(String filename) {
        try {
            byte[] result = distantServerStub.lock(filename, clientID, this.generateFileChecksum(filename));
            if (result.length == 32) {
                System.out.println(filename + " est déjà verrouillé par " + new String(result));
            } else {
                try {
                    File f = new File(FSROOT + filename);
                    FileOutputStream fos = new FileOutputStream(f);
                    fos.write(result);
                    fos.close();
                    System.out.println(filename + " verrouillé");
                } catch (IOException e) {
                    System.out.println("Erreur: " + e.getMessage());
                }
            }
        } catch (RemoteException e) {
            System.out.println("Erreur: " + e.getMessage());
        }
    }

    private void push(String filename) {
        File f = new File(FSROOT + filename);
        try {
            if (f.exists()) {
                FileInputStream fis = new FileInputStream(f);
                byte[] contents = new byte[(int) f.length()];
                fis.read(contents);
                fis.close();
                System.out.println(distantServerStub.push(filename, contents, clientID));
            } else {
                System.out.println(filename + " n'existe pas localement!");
            }
        } catch (IOException e) {
            System.out.println("Erreur: " + e.getMessage());
        }
    }

}
