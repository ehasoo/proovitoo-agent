# CI/CD Töövoog ja Automaatkontrollid

## Ülevaade
Kõik projektid asutuse GitLabis kasutavad GitLab CI/CD torujuhtmeid (`.gitlab-ci.yml`), et tagada pidev koodikvaliteet, turvakontrollid ja automaatne testimine enne koodi ühendamist peaharudesse.

## Torujuhtme standardfaasid (Pipeline Stages)
1. **Lint & Format**:
   - Kontrollib koodistiili ja vormindusreegleid (Spotless, Checkstyle, ESLint).
2. **Test**:
   - Käivitab automaatsed ühik- ja integratsioonitestid (`./gradlew test`, `npm test`). Testikatvus peab olema vähemalt 80%.
3. **Security Scan**:
   - SonarQube staatiline koodianalüüs.
   - Trivy konteineripildi ja sõltuvuste haavatavuste skaneering (kriitilised CVE vead peatavad ehituse).
   - GitGuardian / Gitleaks salajaste võtmete (secrets) lekke kontroll.
4. **Build & Package**:
   - Kompileerib lõpliku binaari ja loob Docker pildi, mis laetakse Harbor registrisse unikaalse commit-räsiga.
5. **Deploy**:
   - Paigaldab pildi sihtkeskkonda vastavalt harule (DEV/TEST/PROD).

## Ebaõnnestunud pipeline'i tõrkeotsing
- Kui pipeline katkeb etapis **Security Scan**: ava töö logi ja tuvasta leitud CVE või reeglirikkumine. Uuenda haavatav sõltuvus `build.gradle` failis.
- Kui pipeline katkeb etapis **Lint**: käivita lokaalselt `./gradlew spotlessApply` või `npm run lint:fix` ja commiti muudatused uuesti.
- Kui pipeline'i jooksutaja (Runner) ei käivitu: veendu, et projektis on lubatud jagatud runnerid (`Settings` -> `CI/CD` -> `Runners`).
