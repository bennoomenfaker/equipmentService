Création de l’équipement par le Ministère de la Santé

Champs obligatoires :
nom, riskClass, emdnCode, lifespan, hospitalId
Génération automatique :
serialNumber (unique),
maintenancePlans (maintenance préventive automatique)
Statut initial : Non réceptionné (reception = false)
2️⃣ Réception de l’équipement par l’hôpital

L’Admin de l’hôpital confirme la réception en mettant reception = true et status = "En service".
L’Admin peut enrichir les données :
brandName, amount, warranty, slaId, sparePartIds, serviceId