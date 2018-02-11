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

/**
 * Classe principale du client. Celui-ci, par défaut, stocke les fichiers locaux dans le répertoire "clientfiles".
 * Il enregistre également l'identifiant du client dans un fichier caché ".client.id".
 */
public class FileClient {

    private static final String DISTANT_HOSTNAME = "132.207.12.231";
    private final String FSROOT = "clientfiles/";
    private final String CLIENTID_FILE = ".client.id";
    private FileServerInterface distantServerStub;
    private String clientID;

    /**
     * Constructeur public du client, se chargeant de configurer les mécanismes de sécurité RMI et d'obtenir le
     * "stub" du serveur de fichiers pour les appels RMI.
     */
    public FileClient() {

        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new SecurityManager());
        }

        distantServerStub = loadServerStub(DISTANT_HOSTNAME);

    }

    /**
     * Fonction main du client. Est exécutée lors de l'appel à ./client.
     * @param args Les paramètres de la ligne de commande.
     *             Certaines commandes (list, syncLocalDirectory) ne prennent pas d'argument supplémentaire,
     *             alors que toutes les autres (create, get, lock, push) prennent également le nom du fichier comme
     *             second argument.
     */
    public static void main(String[] args) {

        FileClient fc = new FileClient();

        if (args.length > 0) {
            fc.initializeFileSystem();
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
                    default:
                        System.out.println("Opération '" + command + "' invalide.\n" +
                                "Les opérations valides sont: create, list, syncLocalDirectory, get, lock, push.");
                        break;
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                System.out.println("La commande '" + command + "' nécessite un nom de fichier comme second argument!");
            }
        } else {
            System.out.println("Vous devez ajouter un argument pour spécifier la commande à utiliser.\n" +
                    "Les opérations valides sont: create, list, syncLocalDirectory, get, lock, push.");
        }
    }

    /**
     * Fonction pour obtenir le "stub" RMI du serveur de fichiers.
     * @param hostname L'adresse IP du serveur distant.
     * @return Le "stub" RMI du serveur de fichiers.
     */
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

    /**
     * Fonction permettant de générer la somme de hachage MD5 d'un fichier local.
     * @param filename Le nom du fichier dont on veut obtenir la somme de hachage.
     * @return La somme de hachage MD5 du contenu du fichier, sous forme de String.
     */
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

    /**
     * Fonction d'initialisation du système de fichiers du client. Se charge de créer le dossier "clientfiles"
     * si celui-ci n'existe pas déjà.
     */
    private void initializeFileSystem() {
        File dir = new File(FSROOT);
        if (!dir.exists()) {
            dir.mkdir();
        }
    }

    /**
     * Méthode permettant d'obtenir un identifiant pour le client, si celui-ci n'en a pas déjà un stocké dans le
     * fichier ".client.id". Fait appel à la méthode RMI du même nom.
     */
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

    /**
     * Méthode faisant appel à la méthode RMI du même nom, permettant de créer un fichier sur le serveur de fichiers.
     * @param filename Le nom du fichier à créer.
     */
    private void create(String filename) {
        try {
            System.out.println(distantServerStub.create(filename));
        } catch (RemoteException e) {
            System.out.println("Erreur: " + e.getMessage());
        }
    }

    /**
     * Méthode faisant appel à la méthode RMI du même nom, permettant de lister les fichiers sur le serveur de fichiers,
     * ainsi que leur état de verrouillage.
     */
    private void list() {
        try {
            for (String file : distantServerStub.list()) {
                System.out.println(file);
            }
        } catch (RemoteException e) {
            System.out.println("Erreur: " + e.getMessage());
        }
    }

    /**
     * Méthode faisant appel à la méthode RMI du même nom, permettant de synchroniser les fichiers locaux avec ceux
     * du serveur de fichiers. Cette méthode se charge de créer les fichiers manquants, et écrasera les fichiers
     * existants avec les versions obtenues du serveur, qu'elles soient nouvelles ou non.
     */
    private void syncLocalDirectory() {
        try {
            HashMap<String, byte[]> files = distantServerStub.syncLocalDirectory();
            if (files != null) {
                files.forEach((name, contents) -> {
                    File f = new File(FSROOT + name);
                    try {
                        if(f.createNewFile()) {
                            System.out.println(f.getName() + " ajouté.");
                        } else {
                            System.out.println(f.getName() + " synchronisé.");
                        }
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

    /**
     * Méthode faisant appel à la méthode RMI du même nom, permettant d'obtenir un fichier à partir du serveur
     * de fichiers. Cette méthode génère implicitement une somme de hachage MD5 de la version locale du fichier,
     * utilisée comme somme de contrôle, afin de permettre au serveur de déterminer s'il est nécessaire d'envoyer
     * une nouvelle version. À noter qu'une somme de hachage nulle (fichier inexistant par exemple) entraînera
     * un téléchargement forcé du fichier distant.
     * @param filename Le nom du fichier à obtenir.
     */
    private void get(String filename) {
        File f = new File(FSROOT + filename);
        byte[] result = null;
        try {
            result = distantServerStub.get(filename, this.generateFileChecksum(filename));
            if (result != null) {
                f.createNewFile();
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

    /**
     * Méthode faisant appel à la méthode RMI du même nom, permettant de verrouiller un fichier pour modification
     * sur le serveur de fichiers. Cette méthode fait un certain traitement local pour déterminer si la réponse
     * du serveur est un fichier ou un identifiant de verrou, dans le cas où le fichier serait déjà verrouillé.
     * @param filename Le nom du fichier à verrouiller.
     */
    private void lock(String filename) {
        try {
            byte[] result = distantServerStub.lock(filename, clientID, this.generateFileChecksum(filename));
            if (result != null) {
                if (result.length == 32) {
                    String lockID = new String(result);
                    if (lockID.equals(clientID)) {
                        System.out.println(filename + " est déjà verrouillé par vous!");
                    } else {
                        System.out.println(filename + " est déjà verrouillé par " + new String(result));
                    }
                } else {
                    try {
                        File f = new File(FSROOT + filename);
                        f.createNewFile();
                        FileOutputStream fos = new FileOutputStream(f);
                        fos.write(result);
                        fos.close();
                        System.out.println(filename + " synchronisé et verrouillé.");
                    } catch (IOException e) {
                        System.out.println("Erreur: " + e.getMessage());
                    }
                }
            } else {
                System.out.println(filename + " verrouillé. Il est identique à votre version locale.");
            }

        } catch (RemoteException e) {
            System.out.println("Erreur: " + e.getMessage());
        }
    }

    /**
     * Méthode faisant appel à la méthode RMI du même nom, permettant de mettre à jour un fichier sur le serveur
     * avec la version locale du client. La méthode échouera si un verrou n'est pas obtenu au préalable sur le fichier
     * en effectuant un appel à lock(), de même que si le fichier n'existe pas localement.
     * @param filename Le nom du fichier à mettre à jour.
     */
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
