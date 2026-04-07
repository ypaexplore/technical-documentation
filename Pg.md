Yes — the best way is to combine **the query-level view** and **the parameter-level view** in one story. In French, you can present it as: *« On ne peut pas diagnostiquer correctement les lenteurs PostgreSQL avec un seul outil ; il faut croiser les requêtes, les plans d’exécution et les paramètres de configuration comme `shared_buffers`, `work_mem` et `effective_cache_size`. »*[1][2][3]

## Structure complète de la présentation

### 1. Introduction
« Aujourd’hui, je vais expliquer comment on analyse un problème de performance PostgreSQL de bout en bout.  
L’idée est de partir des requêtes les plus coûteuses, de comprendre leur plan d’exécution, puis de vérifier si la configuration PostgreSQL soutient bien la charge. »[2][3][4]

### 2. Visibilité sur les requêtes
« La première étape consiste à identifier les requêtes les plus lourdes avec `pg_stat_statements`.  
Cet outil montre le nombre d’exécutions, le temps total, le temps moyen, les lignes traitées et l’activité I/O, ce qui permet de prioriser les vraies sources de coût. »[5][6][7]

#### Explication détaillée
- **calls** : nombre d’exécutions, utile pour repérer les requêtes très fréquentes.
- **total_exec_time** : impact global, souvent plus important que la vitesse d’une exécution seule.
- **mean_exec_time** : vitesse moyenne, utile pour repérer les requêtes intrinsèquement lentes.
- **temp_blks_written / read** : indique que la requête déborde potentiellement sur disque à cause d’un manque de mémoire de travail.[8][5]

### 3. Comprendre le plan
« Une fois la requête identifiée, on l’analyse avec `EXPLAIN` ou `EXPLAIN ANALYZE` pour voir comment PostgreSQL exécute réellement la requête.  
Le but est de détecter les scans séquentiels, les jointures coûteuses, les tris lourds, ou les mauvaises estimations du planner. »[3][9][10]

#### Explication détaillée
- **Seq Scan** : peut être normal sur une petite table, mais coûteux sur une grande.
- **Index Scan** : efficace si l’index est pertinent.
- **Nested Loop** : très bien pour peu de lignes, mais peut devenir catastrophique sur un grand volume.
- **Hash Join** : souvent meilleur pour gros jeux de données.
- **Sort / Aggregate** : peuvent consommer beaucoup de mémoire si `work_mem` est trop faible.[2][3]

### 4. Les paramètres à connaître
« Ensuite, on regarde si les paramètres PostgreSQL sont alignés avec la charge réelle.  
Les plus importants pour une analyse de performance sont `shared_buffers`, `work_mem`, `effective_cache_size`, `maintenance_work_mem`, `random_page_cost` et `max_connections`. »[11][12][13][1][8]

#### a) `shared_buffers`
« C’est le cache interne principal de PostgreSQL.  
S’il est trop bas, la base lit plus souvent sur disque ; s’il est trop haut, on peut gaspiller de la RAM ou fragiliser le serveur. »[12][14][11]

#### b) `work_mem`
« C’est la mémoire utilisée par les tris, les hash joins et les agrégations.  
Si elle est trop faible, PostgreSQL crée des fichiers temporaires sur disque, ce qui ralentit les requêtes. »[1][8]

#### c) `effective_cache_size`
« Ce paramètre n’alloue pas de mémoire, mais il aide le planner à estimer le cache disponible.  
Il influence le choix entre lecture séquentielle et accès par index. »[8][11]

#### d) `maintenance_work_mem`
« Il sert surtout pour `VACUUM`, `CREATE INDEX` et certaines opérations de maintenance.  
Il est important pour accélérer l’administration sans dégrader les requêtes normales. »[2][8]

#### e) `random_page_cost`
« Il influence la décision du planner entre scan séquentiel et index scan.  
Sur SSD, il est souvent plus faible que sur disque classique, car les accès aléatoires coûtent moins cher. »[9][3]

#### f) `max_connections`
« Plus il y a de connexions, plus la pression mémoire augmente.  
Trop de connexions simultanées peuvent dégrader les performances, même si les requêtes elles-mêmes sont correctes. »[11][1]

## Comment relier requêtes et paramètres

« Le bon diagnostic consiste à faire le lien entre ce que l’on voit dans `pg_stat_statements` et ce que le serveur est capable d’exécuter en pratique.  
Par exemple, si une requête génère beaucoup de temp files, cela peut indiquer que `work_mem` est trop faible ; si le planner choisit un mauvais plan, il faut regarder les statistiques et `effective_cache_size` ; si les lectures disque sont trop élevées, `shared_buffers` et la stratégie d’indexation doivent être étudiés. »[4][15][1][8]

## Formulation orale naturelle

« En résumé, `pg_stat_statements` me dit quelles requêtes coûtent cher, `EXPLAIN ANALYZE` m’explique pourquoi elles coûtent cher, et les paramètres PostgreSQL me disent si la base est configurée pour supporter correctement cette charge. »[6][3][12][8]

## Slide finale possible

**Titre :** « Diagnostic complet des performances »  
**Message :**  
- Identifier les requêtes coûteuses.  
- Comprendre leur plan d’exécution.  
- Vérifier les statistiques du planner.  
- Contrôler les paramètres mémoire et I/O.  
- Vérifier les verrous, les logs et la concurrence.[16][17][4]

Je peux maintenant te faire la **version finale complète en français, comme un script de présentation de 3 à 5 minutes**, avec une **slide par slide** et les **phrases exactes à dire à l’oral**.

Sources
[1] Understanding the importance of shared_buffers, work_mem, and ... https://www.postgresql.fastware.com/pzone/2024-06-understanding-shared-buffers-work-mem-and-wal-buffers-in-postgresql
[2] Chapitre 14. Conseils sur les performances https://docs.postgresql.fr/13/performance-tips.html
[3] Documentation: 18: EXPLAIN - PostgreSQL https://www.postgresql.org/docs/current/sql-explain.html
[4] 18: 27.2. The Cumulative Statistics System - PostgreSQL https://www.postgresql.org/docs/current/monitoring-stats.html
[5] Enabling pg_stat_statements - pganalyze https://pganalyze.com/docs/install/01_enabling_pg_stat_statements
[6] F.30. pg_stat_statements - Documentation PostgreSQL https://docs.postgresql.fr/10/pgstatstatements.html
[7] Surveiller avec pg_stat_statements - Azure Databricks https://learn.microsoft.com/fr-fr/azure/databricks/oltp/projects/pg-stat-statements
[8] How to Tune shared_buffers and work_mem in PostgreSQL https://oneuptime.com/blog/post/2026-01-25-postgresql-shared-buffers-work-mem-tuning/view
[9] EXPLAIN - Documentation PostgreSQL https://docs.postgresql.fr/15/sql-explain.html
[10] Le plan de requête EXPLAIN - AWS Conseils prescriptifs https://docs.aws.amazon.com/fr_fr/prescriptive-guidance/latest/postgresql-query-tuning/explain-query-plan.html
[11] PostgreSQL Configuration Parameters: Essential Settings Guide https://dbadataverse.com/tech/postgresql/2025/02/postgresql-configuration-parameters-best-practices-for-performance-tuning
[12] shared_buffers parameter - PostgreSQL Documentation https://postgresqlco.nf/doc/en/param/shared_buffers/
[13] shared_buffers paramètre - PostgreSQL Documentation https://postgresqlco.nf/doc/fr/param/shared_buffers/
[14] PostgreSQL Performance Tuning: Key Parameters - Tiger Data https://www.tigerdata.com/learn/postgresql-performance-tuning-key-parameters
[15] Hacking the Postgres Statistics Tables for Faster Queries https://www.crunchydata.com/blog/hacking-the-postgres-statistics-tables-for-faster-queries
[16] Troubleshooting Render Postgres Performance https://render.com/docs/postgresql-performance-troubleshooting
[17] 20.8. Remonter et tracer les erreurs - Documentation PostgreSQL https://docs.postgresql.fr/16/runtime-config-logging.html
[18] EXPLAIN that parameterized statement in PostgreSQL! - Cybertec https://www.cybertec-postgresql.com/en/explain-that-parameterized-statement/
[19] Optimisation des requêtes PostgreSQL : Parlons Performance ! https://www.youtube.com/watch?v=A2s6NGk5iT4
[20] PostgreSQL Query Plan Explainer | Sk... - LobeHub https://lobehub.com/skills/agentskillexchange-skills-postgresql-query-plan-explainer
[21] Your SQL queries up to 10000 times faster, sustainably. - YouTube https://www.youtube.com/watch?v=TAJKNBPv4Wc
Voici une version claire et détaillée en français sur **`wal_buffers`** et **`maintenance_work_mem`**, avec les bonnes pratiques et le contexte d’utilisation. Ces deux paramètres ne jouent pas le même rôle : `wal_buffers` aide surtout l’écriture du journal WAL, tandis que `maintenance_work_mem` accélère les opérations de maintenance comme `VACUUM` et `CREATE INDEX`.[1][2][3]

## `wal_buffers`

`wal_buffers` définit la mémoire utilisée pour tamponner les écritures WAL avant qu’elles soient poussées vers le stockage. Par défaut, PostgreSQL l’ajuste automatiquement avec une valeur liée à `shared_buffers`, avec un plafond courant de 16 MB selon les notes techniques consultées.[3][1]

### Bonnes pratiques
- Laisser l’auto-réglage dans un premier temps si la charge est classique.
- Tester une valeur plus élevée si la base est très **écrivante** ou si beaucoup de sessions concurrentes produisent du WAL.[7][1]
- Surveiller si les checkpoints fréquents ou les écritures WAL deviennent un goulot d’étranglement.[9][7]

### Quand l’augmenter
- Beaucoup d’écritures simultanées.
- Charge OLTP soutenue.
- Pics de concurrence.
- Cas où les écritures WAL saturent vite les petits buffers.[7][9]

### Ce qu’il faut éviter
- L’augmenter au hasard sans mesurer.
- Le considérer comme un remède à des problèmes de requêtes lentes.
- Le confondre avec `shared_buffers`, qui sert au cache de données, pas au WAL.[10][7]

## `maintenance_work_mem`

`maintenance_work_mem` est la mémoire maximale utilisée par les opérations de maintenance comme `VACUUM`, `CREATE INDEX`, `REINDEX` et certaines opérations de clés étrangères.[2][6][3]

### Bonnes pratiques
- Il peut être configuré plus haut que `work_mem`, car les opérations de maintenance sont moins nombreuses et moins concurrentes.[1][2]
- Le régler plus haut peut accélérer les index builds et les vacuum sur de grosses tables.[5][2]
- Garder en tête qu’avec autovacuum, cette mémoire peut être consommée par plusieurs workers selon la configuration.[8][3]

### Quand l’augmenter
- VACUUM sur grandes tables.
- Création ou reconstruction d’index lente.
- Import massif ou restauration.
- Maintenance régulière sur volumétrie importante.[6][2]

### Ce qu’il faut éviter
- Le fixer trop haut sans tenir compte du nombre de workers d’autovacuum.
- Penser qu’une valeur très élevée est toujours meilleure.
- Oublier que les processus de maintenance peuvent être parallèles selon la configuration et le contexte.[3][8]

## Différence essentielle

- `wal_buffers` concerne le **flux d’écriture WAL** et la capacité à absorber les écritures journalières de façon fluide.[1][7]
- `maintenance_work_mem` concerne la **mémoire des opérations de maintenance** et n’influence pas directement les requêtes applicatives normales.[2][3]

## Valeurs de départ raisonnables

En pratique, les repères souvent cités sont :
- `wal_buffers` : laisser l’auto-ajustement ou tester une valeur autour de 16 MB sur des serveurs actifs.[6][7]
- `maintenance_work_mem` : une valeur plus élevée que `work_mem`, souvent dans une logique de quelques centaines de MB à plusieurs GB selon la RAM et la fréquence de maintenance.[5][2][3]

## Pour ta présentation

Tu peux le dire comme ça :

« `wal_buffers` aide PostgreSQL à absorber les écritures du journal WAL, surtout en forte concurrence ou en charge écrite importante.  
`maintenance_work_mem`, lui, sert aux opérations de maintenance comme `VACUUM` et `CREATE INDEX`, donc on peut souvent lui donner plus de mémoire que `work_mem`, tant qu’on tient compte de l’autovacuum et de la RAM disponible. »[2][3][1]

## Phrase simple et naturelle

« En résumé, `wal_buffers` aide à mieux gérer les écritures, tandis que `maintenance_work_mem` accélère les tâches de maintenance.  
On les ajuste selon la charge réelle, pas à l’aveugle. »[7][1][2]

Si tu veux, je peux maintenant te faire un **tableau complet des paramètres PostgreSQL importants** avec :
- rôle,
- impact,
- cas d’usage,
- valeurs de départ,
- et pièges à éviter.

Sources
[1] Notes de cours sur PostgreSQL - Grand Dub https://grand-dub.github.io/blog/postgresql/notes.html
[2] Wal_sync_method Wal_buffers https://wiki.postgresql.org/wiki/Tuning_Your_PostgreSQL_Server
[3] 19.4. Consommation des ressources # https://docs.postgresql.fr/current/runtime-config-resource.html
[4] Rapide configuration de PostgreSQL https://connect.ed-diamond.com/GNU-Linux-Magazine/glmfhs-044/rapide-configuration-de-postgresql
[5] How to Tune PostgreSQL Performance for Production - OneUptime https://oneuptime.com/blog/post/2026-02-20-postgresql-performance-tuning/view
[6] Chapitre 7 Tuning PostgreSQL https://docs.anakeen.com/dynacase/stable/manex-ref/manex-ref-4de10551-45b5-4796-964a-55b3187b5942.html
[7] Tuning shared_buffers and wal_buffers http://rhaas.blogspot.com/2012/03/tuning-sharedbuffers-and-walbuffers.html
[8] 18.4. Consommation des ressources https://webusers.i3s.unice.fr/~rueher/Cours/BD/DocPostgresSQL9-5_HTML/runtime-config-resource.html
[9] PostgreSQL Performance Tuning - pgEdge https://www.pgedge.com/blog/postgresql-performance-tuning
[10] Documentation: 18: 19.4. Resource Consumption https://www.postgresql.org/docs/current/runtime-config-resource.html
