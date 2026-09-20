# Kubernetesi paigaldusprotsess ja keskkonnad

## Ülevaade
Asutuse mikroteenused ja rakendused jooksevad konteineriseeritud kujul Kubernetes (K8s) klastrites. Paigaldusi (deploy) hallatakse läbi GitOps ja Helm väärtuste failide.

## Paigalduskeskkonnad ja nimeruumid
1. **Arenduskeskkond (DEV)**:
   - Nimeruum: `<teenus>-dev`
   - Paigaldus toimub automaatselt pärast igat `develop` haru edukat CI/CD ehitust.
2. **Testkeskkond (TEST/STAGING)**:
   - Nimeruum: `<teenus>-test`
   - Paigaldus käivitatakse automaatselt või päästikuga, kui muudatused liidetakse `release/*` harusse.
3. **Toodangukeskkond (PROD)**:
   - Nimeruum: `<teenus>-prod`
   - Paigaldus nõuab ametlikku väljalaske (Release Tag) märget ning DevOpsi ja tooteomaniku heakskiitu GitLab CI pipeline'is.

## Paigalduse sammud ja protsess
1. Veendu, et rakenduse konteineripilt (Docker image) on ehitatud ja edukalt valideeritud asutuse Nexus/Harbor registris.
2. Uuenda teenuse Helmi väärtuste failis (`values.yaml`) pildi versiooniviidet (image tag).
3. Käivita paigaldus pipeline läbi GitLab CI/CD `deploy_to_k8s` töölõigu.
4. Jälgi paigalduse staatust:
   - Kontrolli podide käivitumist käsuga: `kubectl get pods -n <nimeruum> -l app=<teenuse-nimi>`
   - Kontrolli rollout'i lõpetamist: `kubectl rollout status deployment/<teenuse-nimi> -n <nimeruum>`

## Levinumad tõrked ja tõrkeotsing
- `CrashLoopBackOff`: Rakendus sulgub kohe pärast käivitumist. Kontrolli rakenduse logisid käsuga `kubectl logs <pod-nimi> -n <nimeruum>`. Tavaliselt on põhjuseks puuduv keskkonnamuutuja või andmebaasi ühenduvusviga.
- `ImagePullBackOff`: Klastril puudub ligipääs konteineriregistrile või määratud pilditagi ei eksisteeri. Kontrolli pildi nime ja `imagePullSecrets` seadistust.
