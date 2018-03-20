# INF8480 TP2

Voici, en quelques lignes, comment construire et exécuter le TP.

## Construire (build)

### Ant

Il est nécessaire d'utiliser **Ant en version 1.10.x** et le **JDK en version 8** pour construire le TP. Il suffira ainsi de se trouver dans le répertoire `TP2`, où se trouve `build.xml`, puis d'appeler `ant` à partir de la ligne de commandes. Si le tout réussit, on aura un affichage similaire à ceci :

```
Felix@Typhoon-PC MINGW64 ~/Documents/INF8480/INF8480/TP2 (master)
$ ant
Buildfile: C:\Users\Felix\Documents\INF8480\INF8480\TP2\build.xml

init:
    [mkdir] Created dir: C:\Users\Felix\Documents\INF8480\INF8480\TP2\bin
     [copy] Copying 5 files to C:\Users\Felix\Documents\INF8480\INF8480\TP2\bin

build-class:
    [javac] Compiling 10 source files to C:\Users\Felix\Documents\INF8480\INF8480\TP2\bin

build-jar:
      [jar] Building jar: C:\Users\Felix\Documents\INF8480\INF8480\TP2\shared.jar
      [jar] Building jar: C:\Users\Felix\Documents\INF8480\INF8480\TP2\computeserver.jar
      [jar] Building jar: C:\Users\Felix\Documents\INF8480\INF8480\TP2\nameserver.jar
      [jar] Building jar: C:\Users\Felix\Documents\INF8480\INF8480\TP2\client.jar

BUILD SUCCESSFUL
Total time: 1 second
```

Il faudra ensuite vérifier que les fichiers `nameserver`, `computeserver`, `client` et `policy` sont bien présents à la racine du répertoire.

## Exécution

Voici comment exécuter le TP :

### Configuration

Un fichier de configuration se trouve dans le répertoire `config`, nommé `hosts.conf`. Ce fichier de configuration sert à indiquer les noms d'hôtes que les deux types de serveur utiliseront pour leurs appels RMI. On peut y mettre par exemple tous les postes d'un local, car les serveurs gèrent automatiquement les postes qui ne répondent pas. Il est à noter qu'il est préférable de mettre des postes qui sont sur le même réseau local; cela accélère grandement la découverte des registres RMI.

### Création d'utilisateurs

Le serveur de noms se sert d'un fichier nommé `users.dat`, présent à la racine du répertoire, pour conserver sa base de données d'utilisateurs. Ainsi, lorsqu'on exécute le serveur de noms, on a l'option d'ajouter des utilisateurs (et il faudra le faire une première fois avant d'exécuter le "vrai" serveur de noms, pour créer un utilisateur). On peut le faire de la façon suivante :

```
$./nameserver -a -u <username> -p <password>
```

Lorsqu'on utilise ce mode, le serveur de noms n'a pas besoin que le registre RMI soit démarré pour fonctionner; il ne fait qu'ajouter un utilisateur à la base de données. Il est à noter que les trois arguments (-a, -u et -p) sont obligatoires dans ce mode. Ici, *username* et *password* doivent être des chaînes de caractères.

### Serveur de noms (NameServer)

Lors d'une exécution normale du serveur de noms, on doit auparavant démarrer le registre RMI afin d'assurer son bon fonctionnement. On peut le faire de la façon suivante :

```
Felix@Typhoon-PC MINGW64 ~/Documents/INF8480/INF8480/TP2 (master)
$ cd bin
$ rmiregistry 5050 &
$ cd ..
```

Il est important de spécifier le port **5050** lorsqu'on lance le registre RMI, et ce, pour les deux types de serveurs. On peut ensuite démarrer le serveur de noms simplement en appelant le script `nameserver` :

```
Felix@Typhoon-PC MINGW64 ~/Documents/INF8480/INF8480/TP2 (master)
$ ./nameserver
Name server ready.
```

Il est possible que des messages concernant un nom "cs" non trouvé dans le registre soient affichés. C'est tout à fait normal, car le serveur de noms s'attend à trouver des ComputeServer sur les machines qu'il "scanne" pour établir sa liste de serveurs. Comme ceux-ci ne sont pas encore démarrés, le message s'affiche; pas d'inquiétude, le serveur de noms effectue un scan à chaque fois que le client lance un calcul. On peut maintenant lancer nos serveurs de calcul.

### Serveur de calcul (ComputeServer)

Pour le serveur de calcul, qu'on assume être exécuté sur une machine différente, on doit également démarrer le registre RMI (s'il est sur la même machine, pas nécessaire si déjà fait auparavant).

```
Felix@Typhoon-PC MINGW64 ~/Documents/INF8480/INF8480/TP2 (master)
$ cd bin
$ rmiregistry 5050 &
$ cd ..
```

On peut ensuite démarrer le serveur de calcul :

```
Felix@Typhoon-PC MINGW64 ~/Documents/INF8480/INF8480/TP2 (master)
$ ./computeserver -q <capacité> -m <maliciosité>
Compute server ready.
NameServer found on host '127.0.0.1'.
```
Idem, il est possible qu'un message concernant un nom "ns" non trouvé s'affiche (surtout si on a démarré le serveur de calcul avant le serveur de noms). Encore une fois, pas de problème, car les serveurs de calcul tentent de retrouver un serveur de noms s'ils reçoivent un calcul alors qu'ils n'ont pas de serveur de noms enregistré.

L'option -q est obligatoire et sert à spécifier la *capacité* du serveur, qui doit être un nombre entier supérieur à 0. L'option -m est facultative et sert à spécifier la *maliciosité* du serveur (à quel point le serveur est malicieux). Ce nombre doit être un nombre à virgule flottante compris dans [0.0, 1.0]. Par défaut, lorsque l'option -m n'est pas spécifié, le serveur de calcul n'est pas malicieux (m = 0.0).

### Client

Une fois des serveurs de nom et de calcul démarrés sur différentes machines (ou la même, avec un maximum de un de chaque dans ce cas), on peut démarrer le client. Il n'est pas nécessaire de démarrer un registre RMI pour exécuter le client. En fait, il suffit d'exécuter le script `client` de la façon suivante :

```
Felix@Typhoon-PC MINGW64 ~/Documents/INF8480/INF8480/TP2 (master)
$ ./client -i <fichier> -u <username> -p <password> -m
Démarrage du répartiteur en mode non-sécurisé.
...
```

Ici, on spécifie un fichier d'opérations d'un format correct (un nombre arbitraire de `prime x` ou `pell x` sur des lignes distinctes) pour l'option -i, qui est obligatoire, un nom d'utilisateur et mot de passe déjà enregistré auparavant avec le serveur de noms (options -u et -p, obligatoires), et une option -m facultative qui indique qu'on s'attend à avoir des serveurs malicieux (donc mode non-sécurisé). Ainsi, si l'option n'est pas spécifiée, le client s'exécute en mode sécurisé.

Si au moins un serveur de noms et au moins un serveur de calculs sont disponibles, le client devrait s'exécuter et retourner le résultat attendu, en plus du temps d'exécution en millisecondes.

```
Résultat total : 705
Temps d'exécution : 12345 millisecondes.
```

Lors d'une exécution en mode non-sécurisé, des messages indiquant les validations entre les différents serveurs seront affichés à des fins d'information :

```
Tâche #0 transférée du serveur 'l4712-01.info.polymtl.ca' au serveur 'l4712-02.info.polymtl.ca' pour validation.
```

Dans les deux types d'exécution, des messages concernant les tâches refusées seront affichés. Cela permet d'évaluer si une exécution est anormale, pour la prise de données :

```
Le serveur 'l4712-01.info.polymtl.ca' a refusé la tâche. Renvoi.
```

C'est tout !
