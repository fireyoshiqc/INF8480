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

/**
 * Classe principale du serveur de fichiers. Celui-ci, par défaut, stocke les fichiers distants dans le répertoire "filesystem".
 * Il enregistre également la table de hachage pour les verrous de fichier dans un fichier de métadonnées caché ".locks.dat".
 */
public class FileServer implements FileServerInterface {

    private final String FSROOT = "filesystem/";
    private final String LOCKFILE = ".locks.dat";
    private final Object fileLocksLock;
    private final Object fileSystemLock;
    private HashMap<String, String> fileLocks;


    /**
     * Constructeur public du serveur. Instancie la table de hachage pour les verrous de fichier, ainsi que
     * des verrous de synchronisation utilisés dans les blocs "synchronized" afin d'éviter des disparités entre
     * les exécutions des clients.
     */
    public FileServer() {
        super();
        fileLocks = new HashMap<>();
        fileLocksLock = new Object();
        fileSystemLock = new Object();
    }

    /**
     * Fonction main du serveur. Est exécutée lors de l'appel à ./server
     * @param args - Arguments de ligne de commande
     */
    public static void main(String[] args) {
        FileServer fs = new FileServer();
        fs.initializeFileSystem();
        fs.run();
    }

    /**
     * Méthode de configuration du serveur. Doit être exécutée pour permettre le RMI.
     */
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

    /**
     * Méthode d'initialisation du système de fichiers associé au serveur.
     */
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

    /**
     * Méthode de sauvegarde de la table de hachage des verrous sur les fichiers.
     * Est appelée lors d'un appel à lock ou push, entre autres.
     */
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

    /**
     * Méthode de lecture de la table de hachage des verrous sur les fichiers.
     * Est appelée lorsque la table doit être vérifiée (presque tous les appels de fonction).
     */
    @SuppressWarnings("unchecked")
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

    /**
     * Fonction de création d'un identifiant client.
     * @return Un hash MD5 du temps système servant d'identifiant au client qui en fait la demande.
     * @throws RemoteException - Peut être exécutée en RMI.
     */
    @Override
    public String createClientID() throws RemoteException {
        String timeStr = Long.toString(System.nanoTime());
        return MD5Hasher.hashMD5(timeStr);
    }

    /**
     * Fonction de création d'un fichier sur le serveur.
     * @param nom - Le nom du fichier à créer.
     * @return Une String décrivant le résultat de l'exécution de la fonction.
     * @throws RemoteException - Peut être exécutée en RMI.
     */
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
                return "Une exception est survenue : " + e.getMessage();
            }
        }
    }

    /**
     * Méthode pour lister les fichiers présents sur le serveur.
     * @return Une liste des fichiers présents sur le serveur, ainsi que leur état de verrouillage.
     * @throws RemoteException - Peut être exécutée en RMI.
     */
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
                        String proprietaire = fileLocks.get(nomFichier);
                        fileList.add("* " + file.getName() + "\t" + (proprietaire == (null) ? "non verrouillé" : "verrouillé par " + proprietaire));
                    }
                }
            }
        }
        return fileList;
    }

    /**
     * Fonction pour synchroniser le client avec les fichiers du serveur.
     * @return Une table de hachage contenant des paires clé-valeur où la clé est le nom du fichier, et la valeur
     * est son contenu. Une table vide est retournée s'il n'y a pas de fichiers sur le serveur.
     * @throws RemoteException - Peut être exécutée en RMI.
     */
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

    /**
     * Fonction pour obtenir un fichier se trouvant sur le serveur. Cette fonction vérifie s'il est nécessaire
     * d'envoyer le fichier en comparant la somme de hachage MD5 de celui-ci à une somme fournie par le client.
     * @param nom Le nom du fichier à obtenir.
     * @param checksum La somme de hachage dudit fichier, provenant de la version du client.
     * @return Le contenu du fichier sous forme de tableau de bytes, si la somme de hachage du fichier sur
     * le serveur est différente de celle fournie à la fonction, ou si cette dernière est null. Sinon, null
     * (cela sera interprété côté client comme quoi le fichier est déjà à jour).
     * @throws RemoteException - Peut être exécutée en RMI.
     */
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

    /**
     * Méthode permettant de verrouiller un fichier sur le serveur pour modification.
     * Le fichier ne sera verrouillé que s'il n'est pas déjà verrouillé par un autre client ou par le client lui-même.
     * @param nom Le nom du fichier à verrouiller.
     * @param clientid L'identifiant du client désirant verrouiller ledit fichier.
     * @param checksum La somme de contrôle de la version du fichier possédée par le client.
     *                 En effet, cette fonction effectue également un get() si nécessaire.
     * @return L'identifiant du client possédant le verrou sur le fichier, s'il y en a un, sinon le contenu du fichier.
     * Tous deux sont sous forme de tableau de bytes, et seront interprétés différemment côté client.
     * @throws RemoteException - Peut être exécutée en RMI.
     */
    @Override
    public byte[] lock(String nom, String clientid, String checksum) throws RemoteException {
        synchronized (fileLocksLock) {
            String id = fileLocks.putIfAbsent(nom, clientid);
            this.saveLocks();
            if (id == null) {
                return get(nom, checksum);
            }
            return id.getBytes();
        }
    }

    /**
     * Méthode permettant de mettre à jour un fichier sur le serveur avec une version obtenue du client.
     * Le client doit auparavant verrouiller le fichier en utilisant la méthode lock().
     * @param nom Le nom du fichier à mettre à jour.
     * @param contenu Le contenu (tableau de bytes) du fichier en question.
     * @param clientid L'identifiant du client désirant procéder à cette mise à jour, afin de vérifier s'il possède
     *                 un verrou sur le fichier en question.
     * @return Une String décrivant le résultat de l'exécution de la fonction.
     * @throws RemoteException - Peut être exécutée en RMI.
     */
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

