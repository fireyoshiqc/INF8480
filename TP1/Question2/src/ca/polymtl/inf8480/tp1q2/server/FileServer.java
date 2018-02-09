package ca.polymtl.inf8480.tp1q2.server;

import ca.polymtl.inf8480.tp1q2.shared.FileServerInterface;
import ca.polymtl.inf8480.tp1q2.shared.MD5Hasher;

import java.io.*;
import java.rmi.ConnectException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;

public class FileServer implements FileServerInterface {

    private final String FSROOT = "filesystem/";
    private final String LOCKFILE = ".locks.dat";
    private final Object fileLocksLock;
    private final Object fileSystemLock;
    private HashMap<String, String> fileLocks;


    public FileServer() {
        super();
        fileLocks = new HashMap<>();
        fileLocksLock = new Object();
        fileSystemLock = new Object();
    }

    public static void main(String[] args) {
        FileServer fs = new FileServer();
        fs.initializeFileSystem();
        fs.run();
        /*
        try {
            String id = fs.createClientID();
            System.out.println(id);
            System.out.println(fs.create("poopy.txt"));
            for (String file : fs.list()) {
                System.out.println(file);
            }
            String test = "blabla";
            System.out.println(fs.push("poopy.txt", test.getBytes(), id));
            fs.lock("poopy.txt", id, "");
            for (String file : fs.list()) {
                System.out.println(file);
            }
            System.out.println(fs.push("poopy.txt", test.getBytes(), id));
            System.out.println("GET:" + new String(fs.get("poopy.txt", null)));


        } catch (RemoteException e) {
            e.printStackTrace();
        }*/

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
            if (!dir.exists()) {
                dir.mkdir();
            } else {
                this.readLocks();
            }
        }
    }

    private void saveLocks() {
        synchronized (fileSystemLock) {
            File lockFile = new File(LOCKFILE);
            try {
                lockFile.createNewFile();
                FileOutputStream fos = new FileOutputStream(lockFile);
                ObjectOutputStream oos = new ObjectOutputStream(fos);
                oos.writeObject(fileLocks);
                oos.close();
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void readLocks() {
        synchronized (fileSystemLock) {
            File lockFile = new File(LOCKFILE);
            if (lockFile.exists()) {
                try {
                    FileInputStream fis = new FileInputStream(lockFile);
                    ObjectInputStream ois = new ObjectInputStream(fis);
                    fileLocks = (HashMap<String, String>) ois.readObject();
                    ois.close();
                    fis.close();
                } catch (ClassNotFoundException | IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public String createClientID() throws RemoteException {
        String timeStr = Long.toString(System.nanoTime());
        return MD5Hasher.hashMD5(timeStr);
    }

    @Override
    public String create(String nom) throws RemoteException {
        synchronized (fileSystemLock) {
            File f = new File(FSROOT + nom);
            try {
                if (f.createNewFile()) {
                    return nom + " ajouté.";
                } else {
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
            File[] files = root.listFiles();
            if (files != null && files.length > 0) {
                for (File file : files) {
                    String nomFichier = file.getName();
                    synchronized (fileLocksLock) {
                        this.readLocks();
                        String proprietaire = fileLocks.get(nomFichier);
                        fileList.add("* " + file.getName() + "\t" + (proprietaire == (null) ? "non verrouillé" : "verrouillé par " + proprietaire));
                    }
                }
            }
        }
        return fileList;
    }

    @Override
    public HashMap<String, byte[]> syncLocalDirectory() throws RemoteException {
        synchronized (fileSystemLock) {
            File f = new File(FSROOT);
            try {
                HashMap<String, byte[]> fileList = new HashMap<>();
                File[] files = f.listFiles();
                if (files != null && files.length > 0) {
                    for (File file : files) {
                        if (!file.isDirectory()) {
                            FileInputStream fis = new FileInputStream(file);
                            byte[] contents = new byte[(int) file.length()];
                            fis.read(contents);
                            fileList.putIfAbsent(file.getName(), contents);
                        }
                    }
                    return fileList;
                }
                return fileList;
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
                return (checksum != null && checksum.equals(MD5Hasher.hashMD5(contents))) ? null : contents;
            } catch (IOException e) {
                return null;
            }
        }
    }

    @Override
    public byte[] lock(String nom, String clientid, String checksum) throws RemoteException {
        synchronized (fileLocksLock) {
            this.readLocks();
            String id = fileLocks.putIfAbsent(nom, clientid);
            this.saveLocks();
            if (id == null) {
                return get(nom, checksum);
            }
            return id.getBytes();
        }
    }

    @Override
    public String push(String nom, byte[] contenu, String clientid) throws RemoteException {
        synchronized (fileLocksLock) {
            this.readLocks();
            String proprietaire = fileLocks.get(nom);
            if (proprietaire == null) {
                return "Opération refusée. Vous devez d'abord verrouiller le fichier.";
            } else if (!proprietaire.equals(clientid)) {
                return "Ce fichier est verrouillé par un autre utilisateur : " + proprietaire;
            } else {
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
                this.saveLocks();
                return nom + " a été envoyé au serveur.";
            }
        }
    }
}

