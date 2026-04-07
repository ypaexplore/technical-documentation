Voici la **version complète en français**, qui combine **toutes les parties précédentes** (troubleshooting avec `pg_stat_statements`, `EXPLAIN ANALYZE`, paramètres PostgreSQL, `wal_buffers`, `maintenance_work_mem`) **+ `pg_stat_activity`** **+ réplication WAL et avantages des read replicas**. Tout est structuré pour une présentation d’équipe fluide et professionnelle.[1][2][3][4]

## Script de présentation complet (3-5 minutes)

### Slide 1 : Introduction
**À dire :**  
« Bonjour à tous. Aujourd’hui, je vais vous expliquer comment diagnostiquer et corriger les problèmes de performance PostgreSQL de façon systématique.  
On va partir des requêtes lentes, passer par les plans d’exécution, vérifier les paramètres de configuration, regarder l’activité live, et enfin voir comment la réplication peut aider à scaler les lectures. »[5][6][7]

### Slide 2 : Identifier les requêtes coûteuses
**À dire :**  
« Première étape : `pg_stat_statements`. Cet outil agrège les requêtes et montre celles qui coûtent le plus en temps total, en nombre d’exécutions, ou en I/O.  
On regarde `total_exec_time` pour l’impact global et `temp_blks_written` pour détecter les débordements mémoire. »[8][9][10]

**Exemple SQL :**  
```sql
SELECT query, calls, total_exec_time, temp_blks_written
FROM pg_stat_statements ORDER BY total_exec_time DESC LIMIT 5;
```

### Slide 3 : Comprendre le plan d’exécution
**À dire :**  
« Une fois la requête trouvée, on fait `EXPLAIN ANALYZE` pour voir si PostgreSQL fait un scan séquentiel, une jointure coûteuse, ou un tri sur disque.  
Si les estimations sont fausses, on vérifie les statistiques avec `ANALYZE`. »[6][11][12]

**Exemple SQL :**  
```sql
EXPLAIN (ANALYZE, BUFFERS) SELECT * FROM ma_table WHERE colonne = 'valeur';
```

### Slide 4 : Paramètres PostgreSQL clés
**À dire :**  
« Les paramètres influencent directement les performances. Voici les plus importants : »[13][14][15][16]

| Paramètre | Rôle | Bonne pratique |
|---|---|---|
| `shared_buffers` | Cache interne | 25% de la RAM (serveur dédié) [13][17] |
| `work_mem` | Tri/jointure/agrégat | Éviter les temp files, mais attention aux connexions [14][18] |
| `effective_cache_size` | Indice pour le planner | ~75% de la RAM totale [14][19] |
| `maintenance_work_mem` | VACUUM, CREATE INDEX | Plus haut que `work_mem` (centaines de MB) [20][16] |
| `wal_buffers` | Tampon WAL | Auto ou 16 MB+ si charge écrite forte [15][21] |
| `random_page_cost` | Index vs seq scan | 1.1-1.5 sur SSD [6][11] |

### Slide 5 : `pg_stat_activity` — Activité live
**À dire :**  
« Pour les problèmes ponctuels, `pg_stat_activity` montre ce qui se passe en temps réel : requêtes longues, sessions bloquées, idle in transaction.  
C’est parfait pour diagnostiquer les verrous et la contention. »[4][22][1]

**Exemple SQL :**  
```sql
SELECT pid, state, query_start, query, wait_event_type, wait_event
FROM pg_stat_activity 
WHERE state = 'active' AND now() - query_start > interval '5 minutes';
```

**Explication :**  
- **Long-running queries** : impactent tout le système.
- **Blocking sessions** : créent des cascades de lenteur.
- **Idle in transaction** : garde des verrous trop longtemps.[22][1]

### Slide 6 : Réplication WAL et read replicas
**À dire :**  
« PostgreSQL utilise le **WAL (Write-Ahead Log)** pour la réplication physique : les changements sont d’abord écrits dans le WAL, puis appliqués sur les **read replicas**.  
Les read replicas sont toujours une bonne idée car ils **déchargent la primaire des lectures**, améliorent la **haute disponibilité**, et permettent de **scaler horizontalement** les requêtes de lecture. »[2][3][23]

#### Pourquoi les read replicas sont toujours bons
- **Séparation des charges** : lectures sur replicas, écritures sur primaire.
- **HA** : failover rapide.
- **Scalabilité** : ajouter des replicas selon le besoin de lecture.
- **Backups** : moins d’impact sur la primaire.
- **Analytics** : exécuter des requêtes lourdes sur un replica dédié.[3][23][24][25]

**Rôle de `wal_buffers` :**  
« `wal_buffers` tamponne le WAL avant envoi aux replicas. En charge écrite forte, l’augmenter réduit les checkpoints et le lag de réplication. »[15][21][25]

**Surveiller le lag :**  
```sql
SELECT * FROM pg_stat_replication;
```

### Slide 7 : Démarche complète
**À dire :**  
« Le diagnostic suit cette logique :  
1. `pg_stat_statements` → requêtes coûteuses.  
2. `EXPLAIN ANALYZE` → plan d’exécution.  
3. `pg_stat_activity` → activité live et verrous.  
4. Paramètres → `shared_buffers`, `work_mem`, `wal_buffers`, etc.  
5. Réplication → décharger les lectures via read replicas. »[1][3][5]

### Slide 8 : Conclusion
**À dire :**  
« PostgreSQL est très performant quand on croise les bons outils et les bons paramètres.  
`pg_stat_statements` + `EXPLAIN` + `pg_stat_activity` + tuning mémoire + réplication WAL forment une approche complète et fiable pour résoudre 90% des problèmes de lenteur.  
Des questions ? »[25][3][6][1]

## Avantages clés des read replicas (rappel)
Les read replicas ne sont **pas toujours** parfaits (lag possible en charge écrite forte), mais ils sont **toujours recommandés** car :  
- Ils isolent les lectures des écritures.  
- Ils offrent de la résilience.  
- Ils permettent de scaler sans tout reconfigurer.[24][2][3]

## Phrase naturelle finale
« En résumé, pour un diagnostic fiable : commencez par `pg_stat_statements` et `pg_stat_activity`, analysez les plans avec `EXPLAIN`, vérifiez les paramètres comme `shared_buffers` et `wal_buffers`, et déchargez les lectures sur des read replicas via le WAL. »[3][13][15][1]

Cette présentation est **prête à utiliser** — tu peux la copier dans PowerPoint ou Google Slides en 8 slides maximum. Si tu veux des **captures d’écran** ou un **fichier Markdown exportable**, dis-le-moi !

Sources
[1] Mastering pg_stat_activity for real-time monitoring in PostgreSQL https://www.instaclustr.com/blog/mastering-pg-stat-activity-for-real-time-monitoring-in-postgresql/
[2] Réplication bi-directionnelle dans PostgreSQL 16.0 - Blog Ippon https://blog.ippon.fr/2025/02/21/replication-bi-directionnelle-dans-postgresql-16/
[3] Understanding and Reducing PostgreSQL Replication Lag https://www.pgedge.com/blog/understanding-and-reducing-postgresql-replication-lag
[4] Mastering pg_stat_activity: Your PostgreSQL Swiss Army Knife https://inspector.azimutt.app/blog/postgresql-pg-stat-activity-guide/
[5] 18: 27.2. The Cumulative Statistics System - PostgreSQL https://www.postgresql.org/docs/current/monitoring-stats.html
[6] Documentation: 18: EXPLAIN - PostgreSQL https://www.postgresql.org/docs/current/sql-explain.html
[7] Chapitre 14. Conseils sur les performances https://docs.postgresql.fr/13/performance-tips.html
[8] Query observability and performance tuning with ... https://severalnines.com/blog/query-observability-and-performance-tuning-with-pg_stat_monitor-and-pg_stat_statements/
[9] F.30. pg_stat_statements - Documentation PostgreSQL https://docs.postgresql.fr/10/pgstatstatements.html
[10] Surveiller avec pg_stat_statements - Azure Databricks https://learn.microsoft.com/fr-fr/azure/databricks/oltp/projects/pg-stat-statements
[11] EXPLAIN - Documentation PostgreSQL https://docs.postgresql.fr/15/sql-explain.html
[12] [PDF] Understanding PostgreSQL statistics to optimize performance https://www.postgresql.eu/events/pgconfde2024/sessions/session/5369/slides/523/Understanding%20PostgreSQL%20statistics%20to%20optimize%20performance%20-%20pgconfDE2024.pdf
[13] shared_buffers parameter - PostgreSQL Documentation https://postgresqlco.nf/doc/en/param/shared_buffers/
[14] How to Tune shared_buffers and work_mem in PostgreSQL https://oneuptime.com/blog/post/2026-01-25-postgresql-shared-buffers-work-mem-tuning/view
[15] Notes de cours sur PostgreSQL - Grand Dub https://grand-dub.github.io/blog/postgresql/notes.html
[16] 19.4. Consommation des ressources # https://docs.postgresql.fr/current/runtime-config-resource.html
[17] shared_buffers paramètre - PostgreSQL Documentation https://postgresqlco.nf/doc/fr/param/shared_buffers/
[18] Understanding the importance of shared_buffers, work_mem, and ... https://www.postgresql.fastware.com/pzone/2024-06-understanding-shared-buffers-work-mem-and-wal-buffers-in-postgresql
[19] PostgreSQL Configuration Parameters: Essential Settings Guide https://dbadataverse.com/tech/postgresql/2025/02/postgresql-configuration-parameters-best-practices-for-performance-tuning
[20] Wal_sync_method Wal_buffers https://wiki.postgresql.org/wiki/Tuning_Your_PostgreSQL_Server
[21] Tuning shared_buffers and wal_buffers http://rhaas.blogspot.com/2012/03/tuning-sharedbuffers-and-walbuffers.html
[22] debugging - How to use pg_stat_activity? - Stack Overflow https://stackoverflow.com/questions/17654033/how-to-use-pg-stat-activity
[23] Utilisation de réplicas en lecture pour Amazon RDS pour PostgreSQL https://docs.aws.amazon.com/fr_fr/AmazonRDS/latest/UserGuide/USER_PostgreSQL.Replication.ReadReplicas.html
[24] Read replicas in Azure Database for PostgreSQL https://learn.microsoft.com/en-us/azure/postgresql/read-replica/concepts-read-replicas
[25] Everything You Need to Know About PostgreSQL Replication Lag https://www.percona.com/blog/replication-lag-in-postgresql/
[26] Replication lag in PostgreSQL Replay Lag is extremely ... https://learn.microsoft.com/en-ie/answers/questions/5608783/replication-lag-in-postgresql-replay-lag-is-extrem
