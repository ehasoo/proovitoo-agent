# Koodi Ülevaatuse (Code Review) Standardid ja Protsess

## Ülevaade
Koodi ülevaatus (Code Review ehk CR) on kohustuslik kvaliteedi tagamise etapp enne iga koodimuudatuse liitmist (Merge Request / MR) `develop` või `main` harusse.

## Kuidas koodi üle vaadata enne merge'i?
1. Ava GitLabis vastava projekti sektsioon "Merge Requests" ja vali ülevaatamiseks määratud MR.
2. Mine vahekaardile "Changes" (muudatused failides).
3. Kontrolli koodi vastavust nõuetele:
   - **Funktsionaalsus**: Kas lahendus vastab seotud Jira/taski nõuetele?
   - **Arhitektuur ja puhtus**: Kas kood järgib SOLID põhimõtteid, ei sisalda dubleerimist ega tarbetut keerukust?
   - **Testid**: Kas uuele või muudetud funktsionaalsusele on lisatud piisavad ühik- ja integratsioonitestid?
   - **Turvalisus**: Veendu, et koodis pole kõvakodeeritud paroole, API võtmeid, SQL-injection ega XSS riske.
4. Jäta tagasiside ja kommentaarid:
   - Küsimuste ja ettepanekute korral kommenteeri otse vastaval koodireal.
   - Vali "Start a review" mitme kommentaari korraga saatmiseks.
5. Heakskiitmine ja liitmine:
   - Kui kõik parandused on sisse viidud ja lahendus sobib, vajuta nuppu "Approve".
   - Merge'i lubamiseks peab olema vähemalt 2 tiimiliikme (peer review) heakskiit ning CI/CD pipeline peab olema edukalt läbitud (roheline).
