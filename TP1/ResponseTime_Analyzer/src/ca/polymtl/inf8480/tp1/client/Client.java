package ca.polymtl.inf8480.tp1.client;

import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import ca.polymtl.inf8480.tp1.shared.ServerInterface;

import java.lang.Math;

public class Client {
	public static void main(String[] args) {
		String distantHostname = null;
		int argsize = 0;

		if (args.length > 0) {
			distantHostname = args[0];
			try {
				argsize = Integer.parseInt(args[1]);
			} catch (NumberFormatException e) {
				System.out.println("Erreur: " + e.getMessage());
			}
		}

		Client client = new Client(distantHostname);
		client.run(argsize);
	}

	FakeServer localServer = null; // Pour tester la latence d'un appel de
									// fonction normal.
	private ServerInterface localServerStub = null;
	private ServerInterface distantServerStub = null;

	public Client(String distantServerHostname) {
		super();

		if (System.getSecurityManager() == null) {
			System.setSecurityManager(new SecurityManager());
		}

		localServer = new FakeServer();
		localServerStub = loadServerStub("127.0.0.1");

		if (distantServerHostname != null) {
			distantServerStub = loadServerStub(distantServerHostname);
			//distantServerStub = loadServerStub("132.207.12.231");

		}
	}

	private void run(int argsize) {
		char[] fakearg = null;
		try {
			fakearg = new char[(int)Math.pow(10, argsize)];
		} catch (ArrayIndexOutOfBoundsException e) {
			System.out.println("Erreur: " + e.getMessage());
		}
		appelNormal(fakearg);

		if (localServerStub != null) {
			appelRMILocal(fakearg);
		}

		if (distantServerStub != null) {
			appelRMIDistant(fakearg);
		}
	}

	private ServerInterface loadServerStub(String hostname) {
		ServerInterface stub = null;

		try {
			Registry registry = LocateRegistry.getRegistry(hostname);
			stub = (ServerInterface) registry.lookup("server");
		} catch (NotBoundException e) {
			System.out.println("Erreur: Le nom '" + e.getMessage()
					+ "' n'est pas défini dans le registre.");
		} catch (AccessException e) {
			System.out.println("Erreur: " + e.getMessage());
		} catch (RemoteException e) {
			System.out.println("Erreur: " + e.getMessage());
		}

		return stub;
	}

	private void appelNormal(char[] fakearg) {
		long start = System.nanoTime();
		//int result = localServer.execute(4, 7);
		localServer.execute(fakearg);
		long end = System.nanoTime();

		System.out.println("Temps écoulé appel normal: " + (end - start)
				+ " ns");
		//System.out.println("Résultat appel normal: " + result);
	}

	private void appelRMILocal(char[] fakearg) {
		try {
			long start = System.nanoTime();
			//int result = localServerStub.execute(4, 7);
			localServerStub.execute(fakearg);
			long end = System.nanoTime();

			System.out.println("Temps écoulé appel RMI local: " + (end - start)
					+ " ns");
			//System.out.println("Résultat appel RMI local: " + result);
		} catch (RemoteException e) {
			System.out.println("Erreur: " + e.getMessage());
		}
	}

	private void appelRMIDistant(char[] fakearg) {
		try {
			long start = System.nanoTime();
			//int result = distantServerStub.execute(4, 7);
			distantServerStub.execute(fakearg);
			long end = System.nanoTime();

			System.out.println("Temps écoulé appel RMI distant: "
					+ (end - start) + " ns");
			//System.out.println("Résultat appel RMI distant: " + result);
		} catch (RemoteException e) {
			System.out.println("Erreur: " + e.getMessage());
		}
	}
}
