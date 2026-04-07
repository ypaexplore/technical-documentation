Voici les **paramètres PostgreSQL optimaux** pour un serveur avec **64 Go de RAM** et **500 Go de disque**, adaptés à une charge OLTP/production classique. Ces valeurs sont basées sur les bonnes pratiques standard (25% RAM pour `shared_buffers`, etc.) et ton contexte de troubleshooting.[1][2][9]

## Configuration recommandée `postgresql.conf`

```ini
# ========================================
# MÉMOIRE PRINCIPALE (64 Go RAM)
# ========================================
shared_buffers = 16GB              # 25% de la RAM (max 40%)
effective_cache_size = 48GB        # 75% de la RAM (indice planner)
work_mem = 64MB                    # Par opération, prudent avec 200 connexions
maintenance_work_mem = 4GB         # VACUUM/CREATE INDEX (plus haut que work_mem)
wal_buffers = -1                   # AUTO (ou 64MB si charge écrite forte)

# ========================================
# CONNEXIONS
# ========================================
max_connections = 200              # Ajuster selon ton pool de connexions
shared_preload_libraries = 'pg_stat_statements'

# ========================================
# WAL & CHECKPOINTS (500 Go disque)
# ========================================
max_wal_size = 8GB                 # Avant checkpoint forcé
min_wal_size = 2GB
checkpoint_timeout = 15min
checkpoint_completion_target = 0.9 # Étaler les checkpoints
wal_keep_size = 2GB                # Pour réplication

# ========================================
# PLANNER (SSD supposé)
# ========================================
random_page_cost = 1.1             # SSD (4.0 = HDD)
effective_io_concurrency = 200     # I/O parallèle SSD
default_statistics_target = 100    # Stats plus précises

# ========================================
# PARALLÉLISME
# ========================================
max_worker_processes = 16
max_parallel_workers = 16
max_parallel_workers_per_gather = 4

# ========================================
# LOGGING (diagnostics)
# ========================================
log_min_duration_statement = 250   # Requêtes > 250ms
log_checkpoints = on
log_lock_waits = on
log_autovacuum_min_duration = 250
```

## Explications détaillées par paramètre

### **Mémoire (64 Go RAM)**

| Paramètre | Valeur | Pourquoi ? |
|---|---|---|
| `shared_buffers` | **16GB** | 25% de 64Go. Cache interne PG. Laisse 75% pour OS cache [9][1] |
| `effective_cache_size` | **48GB** | 75% RAM totale. Indice pour planner (index vs seq scan) [2] |
| `work_mem` | **64MB** | RAM/opération. Avec 200 connexions × 3 ops max = ~38GB max théorique [9] |
| `maintenance_work_mem` | **4GB** | VACUUM/INDEX. Peu concurrent, peut être généreux [1] |

### **Calcul `work_mem` précis**
```
RAM totale = 64GB
Max connexions = 200
Opérations par requête = 3 max
work_mem sûr = 64GB / (200 × 3) = ~106MB → **64MB prudent**
```

### **WAL (500 Go disque)**
- `max_wal_size = 8GB` : Évite checkpoints trop fréquents
- `wal_buffers = AUTO` : PG ajuste à ~1/32 de `shared_buffers` (=512MB)
- `wal_keep_size = 2GB` : Sécurité réplication

## Vérification après redémarrage

```sql
-- Cache effectif
SHOW shared_buffers;
SHOW effective_cache_size;

-- Stats compilation
SELECT name, setting, unit 
FROM pg_settings 
WHERE name IN ('shared_buffers','work_mem','maintenance_work_mem');

-- Cache hit ratio (doit viser >99%)
SELECT 
  'Buffer cache hit ratio' AS name,
  round(100.0*sum(heap_blks_hit)/(sum(heap_blks_hit)+sum(heap_blks_read)),2) AS ratio
FROM pg_statio_user_tables;
```

## Pour tes **read replicas** (bonus)

Avec cette config, tes replicas WAL seront fluides :
```sql
-- Sur primaire : surveiller lag
SELECT client_addr, state, sent_lsn, replay_lsn, 
       pg_wal_lsn_diff(sent_lsn, replay_lsn) AS lag_bytes
FROM pg_stat_replication;
```

## Script de vérification rapide

```bash
# Vérifier conso mémoire après redémarrage
ps -eo pid,ppid,cmd,%mem,%cpu --as rss | grep postgres | head -10

# Cache hit ratio
psql -c "SELECT round(100*sum(heap_blks_hit)/(sum(heap_blks_hit)+sum(heap_blks_read)),2) FROM pg_statio_user_tables;"
```

## Phrase pour ta présentation

« Pour 64 Go de RAM et 500 Go SSD : `shared_buffers=16GB`, `effective_cache_size=48GB`, `work_mem=64MB`, `maintenance_work_mem=4GB`.  
Ces valeurs laissent de la place au cache OS, évitent les temp files, et supportent bien la réplication WAL pour tes read replicas. »[2][9][1]

**Ces valeurs sont prêtes à copier dans `postgresql.conf`** — redémarre PostgreSQL et teste avec ta charge réelle !

Sources
[1] Optimisation avancée de PostgreSQL en production - SHPV France https://www.shpv.fr/blog/postgresql-performance-advanced/
[2] How to Configure PostgreSQL for Production - OneUptime https://oneuptime.com/blog/post/2026-01-21-postgresql-production-configuration/view
[3] Optimisation des ressources système ... - Forums PostgreSQL.fr https://forums.postgresql.fr/viewtopic.php?id=4903
[4] Ajustement des paramètres mémoire de PostgreSQL pour ... https://blog.codeutility.io/fr/database/fine-tuning-postgresql-memory-settings-for-optimal-performance-4dc83420a3
[5] PostgreSQL configuration recommendations https://developer.radiantlogic.com/ia/descartes/best-practice/02-databases/01-postgres-recommendations/
[6] shared_buffers paramètre - PostgreSQL Documentation https://postgresqlco.nf/doc/fr/param/shared_buffers/
[7] PostgreSQL Performances https://public.dalibo.com/exports/formation/manuels/formations/perf1/perf1.handout.html
[8] Fine-Tuning PostgreSQL Memory Settings for Optimal Performance https://blog.codeutility.io/database/fine-tuning-postgresql-memory-settings-for-optimal-performance-4dc83420a3
[9] How to Tune shared_buffers and work_mem in PostgreSQL https://oneuptime.com/blog/post/2026-01-25-postgresql-shared-buffers-work-mem-tuning/view
[10] Paramétrage du serveur PostgreSQL pour l'exploitation ... https://data.sigea.educagri.fr/download/sigea/supports/PostGIS/distance/perfectionnement/M06_Bonnes_pratiques/co/20_Parametrage_spatial.html
