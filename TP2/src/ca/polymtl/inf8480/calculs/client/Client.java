package ca.polymtl.inf8480.calculs.client;

import ca.polymtl.inf8480.calculs.nameserver.NameServer;
import ca.polymtl.inf8480.calculs.shared.ComputeServerInterface;
import ca.polymtl.inf8480.calculs.shared.NameServerInterface;
import ca.polymtl.inf8480.calculs.shared.OperationPair;
import ca.polymtl.inf8480.calculs.shared.Utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static java.lang.System.exit;

public class Client {

    private NameServerInterface nsStub;
    private HashMap<String, ComputeServerInterface> csStubs;
    private HashMap<String, Integer> capacities;
    private String username;
    private String password;

    public Client(String inputFile, String username, String password) {

        this.username = username;
        this.password = password;
        List<String> lines = Utils.readFile(inputFile);
        List<OperationPair> ops = new ArrayList<>();
        if (lines.isEmpty()) {
            System.out.println("Aucune opération spécifiée. Arrêt du répartiteur.");
            exit(1);
        }
        for (String line : lines) {
            String[] lineOps = line.split(" ");
            if (!lineOps[0].toLowerCase().equals("pell") && !lineOps[0].toLowerCase().equals("prime")) {
                System.out.println("Le fichier d'entrée contient une opération invalide ("+ lineOps[0] + "). Arrêt du répartiteur.");
                exit(1);
            }
            try {
                ops.add(new OperationPair(lineOps[0], Integer.parseInt(lineOps[1])));
            } catch (NumberFormatException e) {
                System.out.println("Le fichier d'entrée contient un argument numérique invalide ("+ lineOps[1] + "). Arrêt du répartiteur.");
                exit(1);
            } catch (ArrayIndexOutOfBoundsException e) {
                System.out.println("Le fichier d'entrée contient une opération sans argument numérique. Arrêt du répartiteur.");
                exit(1);
            }
        }

        // Trouver un serveur de noms, puis obtenir la liste des serveurs de calcul.
        this.queryNameServer();
        this.queryComputeServers();

        // Obtenir la capacité de chacun des serveurs.
        capacities = new HashMap<>();
        List<String> toRemove = new ArrayList<>();
        csStubs.forEach((name, stub) -> {
            try {
                capacities.put(name, stub.getCapacity());
            } catch (RemoteException e) {
                toRemove.add(name);
            }
        });

        // Enlever les serveurs problématiques qui n'ont pas pu retourner leur capacité.
        toRemove.forEach((name) -> csStubs.remove(name));

        // Vérifier s'il reste des serveurs qui fonctionnent.
        if (csStubs.isEmpty()) {
            System.out.println("Aucun serveur de calcul n'est présentement disponible. Arrêt du répartiteur.");
            exit(1);
        }

        // Calculer la capacité totale des serveurs.
        int totalCapacity = 0;
        for (int capacity : capacities.values()) {
            totalCapacity += capacity;
        }
    }

    public static void main(String args[]) {

        ArrayList<String> arguments = new ArrayList<>(Arrays.asList(args));
        int posI = arguments.lastIndexOf("-i");
        int posU = arguments.lastIndexOf("-u");
        int posP = arguments.lastIndexOf("-p");
        if (posI == -1 || posU == -1 || posP == -1 || arguments.size() < 6) {
            System.out.println("Les options -i <fichier>, -u <username> et -p <password> sont obligatoires.");
            System.out.println("Usage :\n-i <String>\t: Fichier de calculs en entrée.\n-u <String>\t: Nom de l'utilisateur." +
                    "\n-p <String>\t: Mot de passe.");
            exit(1);
        }
        String inputFile = arguments.get(posI + 1);
        String username = arguments.get(posU + 1);
        String password = arguments.get(posP + 1);

        // Empêcher l'utilisation de deux options consécutives
        if (inputFile.equals("-u") || inputFile.equals("-p") || username.equals("-i") || username.equals("-p")
                || password.equals("-i") || password.equals("-u")) {
            System.out.println("Les options -i <fichier>, -u <username> et -p <password> sont obligatoires.");
            System.out.println("Usage :\n-i <String>\t: Fichier de calculs en entrée.\n-u <String>\t: Nom de l'utilisateur." +
                    "\n-p <String>\t: Mot de passe.");
            exit(1);
        }

        Client client = new Client(inputFile, username, password);

    }

    private void queryNameServer() {
        if ((nsStub = Utils.findNameServer()) == null) {
            System.out.println("Aucun serveur de noms n'est disponible. Arrêt du répartiteur.");
            exit(1);
        }
    }

    private void queryComputeServers() {
        try {
            csStubs = nsStub.getServers();
            if (csStubs != null && !csStubs.isEmpty()) {
                for (String server : csStubs.keySet()) {
                    System.out.println(server);
                }
            } else {
                System.out.println("Aucun serveur de calcul n'est présentement disponible. Arrêt du répartiteur.");
                exit(1);
            }

        } catch (RemoteException e) {
            System.out.println("Une erreur RMI est survenue : " + e.getMessage());
            exit(1);
        }
    }

}
