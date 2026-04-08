Voici le guide complet.

Guide de Présentation — Architecture Reporting E-Cash
Contexte de l’audience
Direction technique et architectes = audience Cartésienne. Ils attendent :
	∙	Une justification rigoureuse de chaque choix
	∙	Qu’on anticipe les objections avant qu’ils les posent
	∙	De la précision sur les compromis — pas de vente, pas d’enthousiasme artificiel
	∙	Que tu défendes tes positions avec des faits, pas de l’autorité

Structure de la Session (1h30)

00:00 - 10:00   Constat & problématique         (contexte partagé)
10:00 - 25:00   Architecture proposée            (vue d'ensemble)
25:00 - 50:00   Décisions techniques clés        (le coeur du débat)
50:00 - 65:00   Pros, cons & risques             (honnêteté totale)
65:00 - 90:00   Questions & objections           (laisser du temps)


Bloc 1 — Constat & Problématique (10 min)
Ce que tu dis
“Avant de présenter la solution, je veux m’assurer qu’on partage le même constat. On a aujourd’hui un système de paiement orienté microservices — chaque service possède sa base de données, son schéma, sa logique métier. C’est une bonne architecture pour l’opérationnel. Le problème émerge dès qu’on veut une vision transverse : combien de paiements ont été traités ce mois-ci en EUR ? Quel est le taux de règlement par type de message ? Ces questions simples en apparence traversent trois ou quatre bases de données sans clé commune garantie.”
“On a également une contrainte forte : les APIs opérationnelles qui alimentent l’interface live ne doivent pas être surchargées par des requêtes de reporting. Ce sont deux cas d’usage fondamentalement différents — l’un est transactionnel, l’autre est analytique.”
Pourquoi commencer ainsi
Les architectes français valident d’abord le diagnostic avant d’écouter la solution. Si ton constat est juste, ta crédibilité monte immédiatement. Si tu sautes au solution directement, tu perds leur attention.
Objection probable à anticiper
“On a déjà des APIs qui exposent les données, pourquoi ne pas les utiliser ?”
“C’est la première question qu’on s’est posée. Le problème est double : d’abord la pagination — extraire 500 000 transactions via une API REST paginée est structurellement inefficace par rapport à un accès direct base de données. Ensuite le couplage — si le reporting consomme les APIs opérationnelles, un pic de requêtes analytiques impacte directement la disponibilité des APIs pour les vrais utilisateurs métier. On sépare les préoccupations.”

Bloc 2 — Architecture Proposée (15 min)
Ce que tu dis
“La solution s’articule autour d’un principe simple : séparer physiquement les données opérationnelles des données de reporting. On introduit une base PostgreSQL dédiée au reporting, alimentée par un pipeline ETL nocturne. Cette base est la seule source de vérité pour Power BI et pour le GUI de reporting.”
“L’architecture est en quatre couches. D’abord les réplicas en lecture des bases sources — on ne touche jamais la production. Ensuite le pipeline ETL en Spring Batch qui extrait chaque nuit de manière incrémentale. Puis la base de reporting avec deux schémas : un schéma de staging en JSONB brut, et un schéma reporting avec des tables typées et des vues matérialisées. Enfin une API REST Spring Boot en lecture seule exposée au GUI et à Power BI.”
Schéma à dessiner au tableau

[Replica Creditor DB]──┐
[Replica Payment DB] ──┤──► [ETL Spring Batch] ──► [Reporting DB]
[Replica Flow DB]   ──┘              ↑                    │
                               nuit, 2h00          ┌──────┴──────┐
                                                   │  GUI / API  │
                                                   │  Power BI   │
                                                   └─────────────┘


Point clé à insister
“Le pipeline ETL lit directement les réplicas base de données — pas via les APIs. C’est un choix délibéré de performance et d’isolation. Les APIs restent dédiées à l’opérationnel.”

Bloc 3 — Décisions Techniques Clés (25 min)
C’est le coeur de la session. Présente chaque décision comme un choix raisonné avec ses alternatives rejetées.

Décision 1 — JDBC plutôt que JPA
Ce que tu dis :
“Pour le pipeline ETL et l’API de reporting, on utilise JDBC directement — pas JPA. Je veux être explicite sur ce choix parce qu’il va à l’encontre des réflexes habituels en Spring Boot.”
“JPA est conçu pour mapper un modèle objet à un schéma stable que l’équipe contrôle. Ici on a trois problèmes fondamentaux. Premier problème : on lit des bases qu’on ne possède pas. Si l’équipe payment-message ajoute une colonne demain, une entité JPA casse à la compilation. Deuxième problème : JPA charge les données dans un graphe d’objets avec cache de premier niveau — sur des extractions de 100 000 lignes, c’est une catastrophe mémoire. Troisième problème : avec plusieurs DataSources, la configuration JPA multi-EntityManager devient un problème en soi.”
“Avec JDBC et row_to_json() côté PostgreSQL, on sérialise chaque ligne source en JSONB. Le schéma source peut évoluer librement — le staging l’absorbe sans modification de code.”
Objection probable :
“row_to_json() charge toutes les colonnes, y compris celles qu’on n’utilise pas.”
“Exact. C’est un compromis assumé. On préfère un staging légèrement surdimensionné mais résilient aux changements de schéma plutôt qu’une extraction précise mais fragile. La transformation qui suit dans le schéma reporting applique la projection. Le volume supplémentaire est négligeable à l’échelle d’un cycle nocturne.”

Décision 2 — ETL piloté par configuration
Ce que tu dis :
“Le pipeline ETL est entièrement piloté par configuration. Ajouter une nouvelle source de données ne nécessite aucune modification Java — uniquement une nouvelle entrée dans le fichier YAML ou dans la table de configuration base de données.”
“Concrètement, chaque source est décrite par cinq paramètres : l’identifiant du service, la référence du DataSource, la table à extraire, la colonne d’identifiant métier, et la colonne de watermark. Le job Spring Batch construit dynamiquement ses Steps au démarrage à partir de cette configuration.”
“Ça a une conséquence directe sur la maintenabilité : l’ajout du service de règlement au périmètre de reporting est une opération de dix secondes, pas une modification de code avec revue, build et déploiement.”
Objection probable :
“Un job qui se construit dynamiquement est plus difficile à déboguer qu’un job statique.”
“C’est vrai. La contrepartie est dans la table d’audit ETL. Chaque exécution écrit son propre log structuré en base — service par service, avec le nombre de lignes extraites, insérées, et le message d’erreur si applicable. Le débogage se fait par requête SQL, pas par parsing de logs. Et l’isolation des sources garantit qu’une source en échec ne bloque pas les autres.”

Décision 3 — Schéma staging JSONB + schéma reporting typé
Ce que tu dis :
“La base de reporting est organisée en deux schémas avec des responsabilités distinctes. Le schéma staging est une zone d’atterrissage brute — chaque ligne source y arrive intacte sous forme JSONB avec ses métadonnées d’origine. Aucune transformation n’y est appliquée. C’est le filet de sécurité : si la transformation downstream a un bug, on peut la rejouer sans ré-extraire les sources.”
“Le schéma reporting contient les tables typées et aplaties que Power BI et l’API consomment. C’est là qu’on applique la logique métier : jointures entre services via la clé de corrélation, cast des types, calcul des indicateurs dérivés. Les vues matérialisées précalculent les agrégats KPI pour des requêtes sub-seconde.”
Point à souligner :
“La frontière entre ces deux schémas est aussi la frontière de responsabilité. Le staging appartient au pipeline ETL. Le schéma reporting appartient à la couche métier. Si les règles de transformation évoluent, on ne touche pas à l’ETL.”

Décision 4 — La clé de corrélation
Ce que tu dis — et c’est le point le plus risqué à présenter :
“Je vais être direct sur le prérequis le plus critique de toute cette architecture : l’existence d’un identifiant métier commun qui traverse tous les microservices. Sans ça, on peut faire du reporting par service — volume de paiements ici, statut des flux là — mais pas de reporting de bout en bout.”
“Dans un système de paiement SWIFT, les candidats naturels sont l’EndToEndId présent dans PAIN001 et PACS008, ou le UETR pour les flux SWIFT. La question concrète est : est-ce que chaque service reçoit cet identifiant dans son message d’entrée et le persiste en base ? Si la réponse est oui pour tous les services, l’architecture tient. Sinon, c’est un prérequis à traiter avant de construire quoi que ce soit.”
“J’ai inclus un audit d’identifiants comme Phase 0 de la roadmap précisément pour ça. Ce n’est pas optionnel.”
Pourquoi cette honnêteté est importante :
Les architectes détestent les solutions qui cachent leurs prérequis. En nommant le risque clairement et en proposant une Phase 0 dédiée, tu montres de la maturité — pas de la faiblesse.

Bloc 4 — Pros, Cons & Risques (15 min)
Présente ce tableau explicitement. Ne laisse pas l’audience le découvrir seule.
Les avantages réels



|Avantage                                |Explication à donner                                                   |
|----------------------------------------|-----------------------------------------------------------------------|
|**Isolation totale**                    |Le reporting n’impacte jamais la production. Zéro risque opérationnel. |
|**Résilience aux changements de schéma**|Le JSONB staging absorbe les évolutions des services sans intervention.|
|**Performance Power BI**                |Import mode sur vues matérialisées — dashboards sub-seconde.           |
|**Maintenabilité**                      |Nouvelle source = une ligne de config. Pas de code, pas de déploiement.|
|**Observabilité intégrée**              |La table d’audit ETL est elle-même requêtable depuis Power BI.         |

Les contraintes réelles — à assumer sans honte



|Contrainte                    |Ce que tu dis                                                                                                                                             |
|------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------|
|**Latence D-1**               |*“Les données sont à J-1. Si le besoin évolue vers du temps réel, l’architecture devra être revue — c’est un choix de conception assumé pour la phase 1.”*|
|**Accès réplicas**            |*“On a besoin d’un accès en lecture aux réplicas de chaque base source. C’est une demande infra à cadrer en amont.”*                                      |
|**Clé de corrélation**        |*“Si l’EndToEndId ne propage pas aujourd’hui, c’est une dette technique à résoudre. L’architecture de reporting en dépend.”*                              |
|**Instance PostgreSQL dédiée**|*“Une base supplémentaire à maintenir, monitorer, sauvegarder. Le coût opérationnel est réel.”*                                                           |
|**Vues matérialisées**        |*“Elles doivent être rafraîchies après chaque ETL. C’est un step supplémentaire dans le pipeline nocturne — pas complexe, mais à ne pas oublier.”*        |

Bloc 5 — Gérer les Objections Difficiles
“C’est trop complexe pour ce qu’on veut faire.”
“La complexité apparente vient de la séparation en couches. Mais chaque couche a une responsabilité unique et peut évoluer indépendamment. L’alternative — requêter directement les bases opérationnelles depuis Power BI — est plus simple à court terme mais crée un couplage fort entre le reporting et la production. On a déjà vu ce scénario générer des incidents en production lors de pics de requêtes analytiques. La complexité ici est un investissement en résilience.”
“Pourquoi pas un data lake ou un outil ETL du marché ?”
“C’est une question légitime. La réponse est contextuelle. Notre équipe maîtrise Java et Spring Boot. Notre infrastructure est hybride et incertaine. Introduire Kafka, Spark ou Airflow aujourd’hui, c’est ajouter une courbe d’apprentissage et une dépendance opérationnelle pour un besoin qui se satisfait d’un refresh quotidien. On part du principe qu’on ne sur-ingénierie pas la phase 1. Si le volume ou la fraîcheur des données évolue, l’architecture est modulaire — on peut remplacer le pipeline ETL sans toucher la base de reporting ni l’API.”
“Comment on gère les pannes du pipeline ETL ?”
“Trois mécanismes. Premier : l’isolation par source — si le service creditor est indisponible cette nuit, les autres sources continuent. Deuxième : le watermark — au prochain run, on reprend exactement où on s’est arrêté, pas depuis le début. Troisième : la table d’audit — on détecte immédiatement quelle source a échoué et pourquoi, sans éplucher des logs.”
“Spring Batch, c’est du batch des années 2000. Pourquoi pas quelque chose de plus moderne ?”
“Spring Batch 5 est activement maintenu, supporte nativement les chunks, la reprise sur erreur, le monitoring, et s’intègre parfaitement avec l’écosystème Spring Boot qu’on utilise déjà. ‘Moderne’ n’est pas un critère de choix en soi — la fiabilité et la maîtrise par l’équipe le sont. Si demain on a besoin d’Airflow pour l’orchestration ou de Spark pour le volume, le pipeline ETL est le seul composant à remplacer — la base de reporting et l’API ne bougent pas.”

Conseils pour la Présentation
Sur le style :
Ne commence jamais par les avantages. Commence par le problème et laisse l’audience arriver elle-même à la conclusion que la solution est logique. Les architectes français apprécient de participer au raisonnement, pas d’être convaincus.
Sur les questions :
Quand une objection est bonne, dis-le explicitement : “C’est effectivement le point de fragilité de cette approche…”. La crédibilité technique se gagne en reconnaissant les limites, pas en les esquivant.
Sur les silences :
Laisse les silences après les points importants. Ne comble pas le vide. Si quelqu’un réfléchit, c’est bon signe.
Sur les schémas :
Dessine l’architecture en direct si tu peux — ça montre que tu la maîtrises sans support. Le document est pour l’après-réunion.
Phrase d’ouverture recommandée :
“Je vais vous présenter une architecture de reporting pour le système E-Cash. Mon objectif n’est pas de vous convaincre que c’est la seule solution possible — c’est de vous présenter les choix qu’on a faits, pourquoi, et quels sont les compromis. J’attends vos objections.”
Cette phrase installe immédiatement le bon registre : débat technique rigoureux, pas présentation commerciale.​​​​​​​​​​​​​​​​
