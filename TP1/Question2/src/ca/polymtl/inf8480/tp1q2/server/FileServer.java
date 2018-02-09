package ca.polymtl.inf8480.tp1q2.server;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.attribute.UserDefinedFileAttributeView;
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
    private final String METADATA = "user:fslock";
    private HashMap<String, String> fileLocks;
    private Object fileLocksLock;
    private Object fileSystemLock;


    public static void main(String[] args) {
        FileServer fs = new FileServer();
        //fs.run();
        fs.initializeFileSystem();
        try {
            String id = fs.createClientID();
            System.out.println(id);
            System.out.println(fs.create("poopy.txt"));
            for (String file : fs.list()){
                System.out.println(file);
            }
            String test = "blabla";
            System.out.println(fs.push("poopy.txt", test.getBytes(), id));
            fs.lock("poopy.txt", id, "");
            for (String file : fs.list()){
                System.out.println(file);
            }
            File f = new File("filesystem/poopy.txt");
            System.out.println(fs.push("poopy.txt", test.getBytes(), id));
            System.out.println("GET:" + new String(fs.get("poopy.txt", null)));


        } catch (RemoteException e) {
            e.printStackTrace();
        }

    }

    public FileServer() {
        super();
        fileLocks = new HashMap<>();
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

    private void initializeFileSystem() {
        synchronized (fileSystemLock) {
            File dir = new File(FSROOT);
            if (!dir.exists()){
                dir.mkdir();
            /*} else {
                for (File f : dir.listFiles()) {
                    // Check file metadata to get back locks here, inspired by : https://docs.oracle.com/javase/tutorial/essential/io/fileAttr.html
                    String metadata = this.readMetadata(f);
                    System.out.println("METADATA : " + metadata);
                    if (metadata != null){
                        synchronized (fileLocksLock) {
                            fileLocks.putIfAbsent(f.getName(), metadata);
                        }
                    }
                }*/
            }
        }
    }

    private String readMetadata(File f) {
        UserDefinedFileAttributeView view = Files.getFileAttributeView(f.toPath(), UserDefinedFileAttributeView.class);
        try {
            //ByteBuffer buf = ByteBuffer.allocate(view.size(METADATA));
            //view.read(METADATA, buf);
            //buf.flip();
            return new String((byte[]) Files.getAttribute(f.toPath(), METADATA));//Charset.defaultCharset().decode(buf).toString();
        } catch (IOException e) {
            //e.printStackTrace();
            System.out.println("Error : Could not read metadata from file : " + f.getName());
            return null;
        }
    }

    private void addMetadata(File f, String metadata) {
        //UserDefinedFileAttributeView view = Files.getFileAttributeView(f.toPath(), UserDefinedFileAttributeView.class);
        try {
            Files.setAttribute(f.toPath(), METADATA, metadata.getBytes());
            //view.write(METADATA, Charset.defaultCharset().encode(metadata));
        } catch (IOException e) {
            System.out.println("Error : Could not add metadata to file : " + f.getName());
        }
    }

    private void removeMetadata(File f) {
        UserDefinedFileAttributeView view = Files.getFileAttributeView(f.toPath(), UserDefinedFileAttributeView.class);
        try {
            view.delete(METADATA);
        } catch (IOException e) {
            System.out.println("Error : Could not remove metadata from file : " + f.getName());
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
                File f = new File(FSROOT + nom);
                //this.addMetadata(f, clientid);
                //System.out.println("METADATA_LOCK : " + this.readMetadata(f));
                return get(nom, checksum);
            }
            return id.getBytes();
        }
    }

    @Override
    public String push(String nom, byte[] contenu, String clientid) throws RemoteException {
        synchronized (fileLocksLock) {
            String proprietaire = fileLocks.get(nom);
            if (proprietaire == null) {
                return "Opération refusée. Vous devez d'abord verrouiller le fichier.";
            }
            else if (proprietaire != clientid) {
                return "Ce fichier est verrouillé par un autre utilisateur : " + proprietaire;
            }
            else {
                File f = new File(FSROOT + nom);
                synchronized (fileSystemLock) {

                    try {
                        FileOutputStream fos = new FileOutputStream(f);
                        fos.write(contenu);
                        fos.close();
                    } catch (IOException e) {
                        return "Une erreur est survenue lors de l'écriture dans le fichier. Celui-ci est toujours verrouillé.";
                    }
                }
                fileLocks.remove(nom);
                //System.out.println("METADATA_PUSH: " + this.readMetadata(f));
                //this.removeMetadata(f);
                return nom + " a été envoyé au serveur.";
            }
        }
    }
}

