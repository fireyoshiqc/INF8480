package ca.polymtl.inf8480.tp1q2.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.rmi.ConnectException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeSet;
import java.security.MessageDigest;

import ca.polymtl.inf8480.tp1q2.shared.FileServerInterface;

public class FileServer implements FileServerInterface {

    private final String FSROOT = "filesystem/";
    private TreeSet<String> clientIDs;
    private HashMap<String, String> fileLocks;
    private Object clientIDLock;
    private Object fileLocksLock;
    private Object fileSystemLock;


    public static void main(String[] args) {
        FileServer fs = new FileServer();
        //fs.run();
        try {
            System.out.println(fs.createClientID());
            System.out.println(fs.create("poopy.txt"));
            for (String file : fs.list()){
                System.out.println(file);
            }
            fs.lock("poopy.txt", "1234", "");
            for (String file : fs.list()){
                System.out.println(file);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }

    }

    public FileServer() {
        super();
        clientIDs = new TreeSet<>();
        fileLocks = new HashMap<>();
        clientIDLock = new Object();
        fileLocksLock = new Object();
        fileSystemLock = new Object();
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

    private String hashMD5(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] md5 = md.digest(s.getBytes());
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < md5.length; ++i) {
                sb.append(Integer.toHexString((md5[i] & 0xFF) | 0x100).substring(1, 3));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return "";
        }
    }

    private String hashMD5(byte[] barr) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] md5 = md.digest(barr);
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < md5.length; ++i) {
                sb.append(Integer.toHexString((md5[i] & 0xFF) | 0x100).substring(1, 3));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return "";
        }
    }

    @Override
    public String createClientID() throws RemoteException {
        String timeStr = Long.toString(System.nanoTime());
        String id = hashMD5(timeStr);
        synchronized (clientIDLock){
            clientIDs.add(id);
        }
        return id;
    }

    @Override
    public String create(String nom) throws RemoteException {
        synchronized(fileSystemLock){
            File f = new File(FSROOT+nom);
            try {
                if(f.createNewFile()) {
                    return nom + " ajouté.";
                }
                else {
                    return nom + " existe déjà!";
                }
            } catch (IOException e) {
                e.printStackTrace();
                return "Une exception est survenue : " + e.getMessage();
            }
        }
    }

    @Override
    public ArrayList<String> list() throws RemoteException {
        ArrayList<String> fileList = new ArrayList<>();
        synchronized (fileSystemLock) {
            File root = new File(FSROOT);
            for (File file : root.listFiles()) {
                String nomFichier = file.getName();
                synchronized (fileLocksLock) {
                    String proprietaire = fileLocks.get(nomFichier);
                    fileList.add("* " + file.getName() + "\t" + (proprietaire == (null) ? "non verrouillé" : "verrouillé par " + proprietaire));
                }
            }
        }
        return fileList;
    }

    @Override
    public ArrayList<byte[]> syncLocalDirectory() throws RemoteException {
        synchronized (fileSystemLock) {
            File f = new File(FSROOT);
            try {
                ArrayList<byte[]> files = new ArrayList<>();
                for (File file : f.listFiles()){
                    FileInputStream fis = new FileInputStream(file);
                    byte[] contents = new byte[(int)f.length()];
                    fis.read(contents);
                    files.add(contents);
                }
                return files;
            } catch (IOException e) {
                return null;
            }
        }
    }

    @Override
    public byte[] get(String nom, String checksum) throws RemoteException {
        synchronized (fileSystemLock) {
            File f = new File(FSROOT + nom);
            try {
                FileInputStream fis = new FileInputStream(f);
                byte[] contents = new byte[(int) f.length()];
                fis.read(contents);
                fis.close();
                return (checksum == hashMD5(contents)) ? null : contents;
            } catch (IOException e) {
                return null;
            }
        }
    }

    @Override
    public byte[] lock(String nom, String clientid, String checksum) throws RemoteException {
        synchronized (fileLocksLock) {
            String id = fileLocks.putIfAbsent(nom, clientid);
            if (id == null) {
                return get(nom, checksum);
            }
            return id.getBytes();
        }
    }

    @Override
    public void push(String nom, String contenu, String clientid) throws RemoteException {

    }

}

