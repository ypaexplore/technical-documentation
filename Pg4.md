**Shared_buffers**, **effective_cache_size**, **work_mem** et **maintenance_work_mem** sont les 4 paramètres mémoire PostgreSQL les plus critiques. Voici leurs définitions précises et différences.[1][3][6][7]

## 1. `shared_buffers` — Cache interne PostgreSQL

**Définition :**  
C'est la **mémoire partagée dédiée** que PostgreSQL alloue pour **stocker les pages de données les plus utilisées**.  
Chaque connexion PostgreSQL peut y accéder simultanément.

**Rôle concret :**  
- Garde les blocs de tables/index les plus chauds en RAM
- **Réduit les lectures disque** (I/O)
- **Améliore le cache hit ratio** (>99% = excellent)

**Exemple pour 64GB RAM :** `shared_buffers = 16GB` (25%)[1][7]

**Visualisation :**  
```
┌─────────────────────────────────────┐
│ shared_buffers (16GB) ← PG cache    │ ← Pages de données chaudes
│ OS cache (reste RAM) ← Fichiers OS  │ ← Cache système
└─────────────────────────────────────┘
```

## 2. `effective_cache_size` — Indice pour le Planner

**Définition :**  
**Pas une allocation mémoire réelle**, mais un **indice** que PostgreSQL donne à son **planificateur de requêtes** pour estimer **combien de cache disque est disponible au total** (PG + OS).

**Rôle concret :**  
- Influence le choix **index scan vs sequential scan**
- Si trop bas → PG favorise les seq scans
- Si trop haut → PG favorise trop les index

**Exemple pour 64GB RAM :** `effective_cache_size = 48GB` (75%)[8]

**Phrase simple :**  
« C'est comme dire au GPS : 'j'ai 48GB de mémoire totale pour le cache'. »

## 3. `work_mem` — Mémoire par opération de requête

**Définition :**  
Mémoire **par opération** dans une requête pour les **tris**, **hash joins**, **agrégations** et **materializations**.

**Rôle concret :**  
```
SELECT ... ORDER BY ... → utilise work_mem pour trier
SELECT ... FROM a JOIN b → utilise work_mem pour hash join  
SELECT ... GROUP BY ... → utilise work_mem pour agrégation
```

**Si `work_mem` trop faible :** → **temp files sur disque** (très lent)

**Calcul prudent :** `RAM_totale / (max_connections × 3)`  
→ 64GB / (200 × 3) = **~106MB** → utiliser **64MB**[5][11]

## 4. `maintenance_work_mem` — Maintenance

**Définition :**  
Mémoire pour les **opérations de maintenance** : `VACUUM`, `CREATE INDEX`, `REINDEX`, `ALTER TABLE ADD FOREIGN KEY`.

**Rôle concret :**  
```
VACUUM ma_table → utilise maintenance_work_mem
CREATE INDEX CONCURRENTLY → utilise maintenance_work_mem
```

**Peut être élevé car :**  
- **Une seule opération à la fois** par session
- **Peu concurrent** avec les requêtes normales

**Exemple :** `maintenance_work_mem = 4GB`[3][6]

## Différences clés en 1 tableau

| Paramètre | Alloue vraie RAM ? | Utilisé par | Impact principal | Exemple 64GB RAM |
|-----------|--------------------|-------------|------------------|------------------|
| `shared_buffers` | ✅ **OUI** | **TOUT PG** | Cache données/index | **16GB** |
| `effective_cache_size` | ❌ **NON** | **Planner** | Choix index/seq scan | **48GB** |
| `work_mem` | ✅ **OUI** | **Requêtes** | Tri/jointure/agrégat | **64MB** |
| `maintenance_work_mem` | ✅ **OUI** | **Maintenance** | VACUUM/CREATE INDEX | **4GB** |

## Erreurs fréquentes

```
❌ shared_buffers = 60GB  ← Trop ! Laisse rien à l'OS cache
❌ work_mem = 1GB        ← OOM avec 200 connexions  
✅ shared_buffers = 16GB ← Équilibré PG/OS
✅ work_mem = 64MB       ← Sûr avec 200 connexions
```

## Pour ta présentation (phrase naturelle)

« `shared_buffers` c'est le **coffre-fort mémoire** de PostgreSQL pour les données chaudes.  
`effective_cache_size` c'est un **indice** pour le GPS du planner.  
`work_mem` c'est la **papeterie** pour chaque opération de tri.  
`maintenance_work_mem` c'est la **chambre d'hôtel** pour les tâches de ménage. »[1][3][7]

**Ces 4 paramètres forment le cœur du tuning mémoire PostgreSQL.** Ils doivent être cohérents entre eux selon ta RAM et ton nombre de connexions.

Sources
[1] Understanding the importance of shared_buffers, work_mem, and ... https://www.postgresql.fastware.com/pzone/2024-06-understanding-shared-buffers-work-mem-and-wal-buffers-in-postgresql
[2] Optimisation avancée de PostgreSQL en production - SHPV France https://www.shpv.fr/blog/postgresql-performance-advanced/
[3] Resource Usage / Memory server parameters - Azure Database for ... https://learn.microsoft.com/en-us/azure/postgresql/server-parameters/param-resource-usage-memory
[4] shared_buffers paramètre - PostgreSQL Documentation https://postgresqlco.nf/doc/fr/param/shared_buffers/
[5] Ajustement des paramètres mémoire de PostgreSQL pour ... https://blog.codeutility.io/fr/database/fine-tuning-postgresql-memory-settings-for-optimal-performance-4dc83420a3
[6] maintenance_work_mem parameter - PostgreSQL Documentation https://postgresqlco.nf/doc/en/param/maintenance_work_mem/
[7] shared_buffers parameter - PostgreSQL Documentation https://postgresqlco.nf/doc/en/param/shared_buffers/
[8] effective_cache_size https://docs.aws.amazon.com/fr_fr/prescriptive-guidance/latest/tuning-postgresql-parameters/effective-cache-size.html
[9] Documentation: 9.4: Resource Consumption - PostgreSQL https://www.postgresql.org/docs/9.4/runtime-config-resource.html
[10] Les mains dans le cambouis #3 - Les stratégies d'accès aux buffers https://blog.dalibo.com/2024/03/26/strategies-acces.html
[11] How to Tune shared_buffers and work_mem in PostgreSQL https://oneuptime.com/blog/post/2026-01-25-postgresql-shared-buffers-work-mem-tuning/view
