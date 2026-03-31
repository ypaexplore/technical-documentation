Voici une conversation naturelle et fluide à tenir avec ton équipe, qui couvre tous les sujets :

🗣️ Conversation d’équipe – Bonnes pratiques E-Cash

L’intro – poser le contexte
« Bon les gars, on va prendre un moment ensemble parce que j’ai envie qu’on parle de notre façon de coder collectivement. C’est pas une critique, c’est vraiment une discussion d’équipe. On a une codebase qui a grandi vite, on a des fonctionnalités qui marchent, mais si on regarde honnêtement comment c’est structuré aujourd’hui, on a des choses qui vont nous coûter cher dans six mois si on ne les adresse pas maintenant. L’idée aujourd’hui c’est qu’on aligne tout le monde sur des pratiques communes — pas pour faire du beau code pour le plaisir, mais parce que ça va directement impacter notre vitesse, notre capacité à tester, et notre façon de collaborer. On y va point par point, et je veux vraiment vos retours sur chaque sujet. »

1. Le Formatting
« Premier sujet, et je sais que ça peut paraître trivial — le formatage. Mais c’est vraiment la base. Aujourd’hui quand on fait une code review, on perd du temps à discuter d’indentations, d’imports non triés, de lignes trop longues. Ce temps-là on devrait le passer sur la logique métier, pas sur la forme.
Ce qu’on va mettre en place c’est une config Checkstyle commune, intégrée dans le build Maven — donc bloquante en CI. Tout le monde configure son IDE avec le même fichier, et le formatage devient une non-discussion. Plus personne ne fait de remarque sur un espace de trop en code review.
Est-ce que vous avez des contraintes par rapport à vos configs d’IDE actuelles ? »

2. Les conventions de nommage – camelCase, snake_case, noms de classes
« Dans la même logique de cohérence, parlons du nommage. Parce qu’aujourd’hui on a des incohérences qui créent de vrais bugs silencieux — un champ createdAt en Java, CreatedAt dans le JSON, created_at en base… et le mapping se casse sans qu’on comprenne pourquoi.
La règle c’est simple : en Java tout est camelCase — variables, méthodes, paramètres. Les noms de classes sont en PascalCase, et on adopte des suffixes clairs : *Service, *Repository, *DTO, *Mapper, *Controller, *Config, *Exception. Si une classe ne rentre dans aucune de ces cases, c’est souvent le signe qu’elle fait trop de choses.
Pour le monde extérieur — base de données et JSON — tout est en snake_case. On configure ça une seule fois dans Jackson et dans la PhysicalNamingStrategy de Spring, et la traduction est automatique. On n’annote plus chaque champ à la main.
Le flux devient propre et prévisible :
payment_amount en base → paymentAmount en Java → payment_amount dans le JSON.
Est-ce que vous voyez des endroits dans le code où on a déjà des incohérences sur ça ? »

3. Les DTOs et les Mappers
« Qui dit nommage dit aussi DTOs. Aujourd’hui on a des classes qui s’appellent PaymentData, PaymentInfo, PaymentObject — c’est flou, on ne sait pas si c’est une entité, un DTO entrant ou sortant, un objet interne…
La convention qu’on adopte c’est <Domaine><Direction>DTO — donc PaymentRequestDTO, PaymentResponseDTO, SettlementEventDTO. Clair, explicite, pas d’ambiguïté.
Et pour les mappers — on arrête de faire la conversion à la main dans les services. Un service qui fait du mapping c’est un service qui fait deux choses à la fois. On standardise l’usage de MapStruct avec une interface par domaine : PaymentMapper, SettlementMapper. Le service délègue, point.
Est-ce qu’il y a des cas où vous pensez que le mapping manuel reste justifié ? »

4. Les dépendances et le Dependency Management
« Maintenant un sujet un peu plus structurel. Si on regarde nos pom.xml aujourd’hui, on a des versions de librairies déclarées en dur dans chaque module — et pas forcément les mêmes. On pourrait très bien avoir deux versions de Jackson qui coexistent au runtime sans qu’on s’en rende compte. Ce genre de conflit est extrêmement difficile à débugger.
La solution c’est de tout centraliser dans le POM parent via un dependencyManagement. Les modules enfants déclarent leurs dépendances sans version — c’est le parent qui pilote. On utilise aussi le BOM Spring pour hériter de versions cohérentes et validées ensemble.
Un seul endroit à maintenir, zéro divergence entre modules.
Est-ce que quelqu’un a déjà eu des bugs liés à des conflits de versions chez nous ? »

5. Les plugins Maven
« Dans la continuité du POM parent, on va aussi standardiser les plugins Maven. Aujourd’hui Surefire, Failsafe, Checkstyle, Jacoco — soit ils sont absents, soit configurés différemment selon les modules.
On les déclare tous dans le pluginManagement du parent. Surefire pour les tests unitaires, Failsafe pour les tests d’intégration avec la convention *IT.java, Jacoco pour la couverture avec un seuil minimum qu’on définit ensemble. Chaque module hérite sans redéfinir.
Le build devient identique partout — en local comme en CI. Plus de surprise du type « ça marche sur ma machine ».
Y’a des plugins que vous utilisez en local que vous voudriez standardiser ? »

6. Le whitelisting des URLs sortantes
« Un point plus orienté sécurité maintenant. On a des appels HTTP sortants dans plusieurs services — vers des APIs internes SG, des endpoints de notification. Aujourd’hui ces URLs sont parfois codées en dur, parfois en config, sans vision centralisée de ce qui est autorisé.
Ce qu’on veut mettre en place c’est une whitelist déclarée dans la config Spring, vérifiée par un intercepteur sur nos RestTemplate ou WebClient. Toute URL absente de cette liste déclenche une erreur explicite et loggée.
Ça nous donne trois choses : de la traçabilité, une surface d’attaque réduite, et quelque chose d’auditable facilement si on a une revue sécurité.
Est-ce que vous avez une bonne visibilité aujourd’hui de toutes les URLs qu’on appelle en dehors de notre périmètre ? »

7. La Clean Architecture
« Et pour finir, le sujet de fond qui structure tout le reste. Si on est honnêtes, aujourd’hui on a des services qui font à la fois de la logique métier, de l’accès base de données, et de la transformation de messages SWIFT. Tout est mélangé — et ça veut dire qu’on ne peut pas tester un use case sans démarrer Spring entier. C’est lent, c’est fragile, et ça ralentit tout le monde.
Ce qu’on vise c’est une séparation claire en trois couches :
	∙	domain — les entités métier et les interfaces de ports. Aucune dépendance Spring, aucune dépendance JPA. Testable unitairement en pur Java.
	∙	application — les use cases qui orchestrent le domaine via les ports.
	∙	infrastructure — tout ce qui est technique : JPA, RabbitMQ, RestTemplate, les adapters.
On ne refactorise pas tout d’un coup — on applique le strangler fig pattern. On commence par les nouveaux développements, on isole module par module progressivement. L’objectif à six mois c’est qu’on puisse tester un use case de paiement complet sans contexte Spring.
Qu’est-ce qui vous semble le plus bloquant pour commencer à aller dans cette direction ? »

Le mot de clôture
« Voilà, on a fait le tour. Je sais que c’est beaucoup en une seule session, mais l’idée c’est pas de tout faire demain. C’est de s’aligner sur la direction, et d’avancer point par point de manière incrémentale. Ce que je vous propose c’est qu’on priorise ensemble les deux ou trois sujets les plus urgents, et qu’on définit des actions concrètes pour le prochain sprint. Chaque amélioration qu’on fait là, c’est du temps qu’on récupère plus tard.
Alors, par où vous voulez qu’on commence ? »

Cette trame te donne une session complète, fluide et progressive — chaque sujet s’enchaîne naturellement sur le suivant.​​​​​​​​​​​​​​​​
