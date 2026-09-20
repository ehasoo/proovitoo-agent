# Kaugtöö ja VPN Ühenduse Seadistamine

## Ülevaade
Asutuse sisevõrgu ressurssidele (nt siseveeb, GitLab, arendus- ja testkeskkonnad, andmebaasid) turvaliseks ligipääsemiseks väljastpoolt kontorit on kohustuslik kasutada asutuse hallatavat VPN-ühendust.

## VPN kliendi paigaldus ja seadistus
1. **Tarkvara**:
   - Asutuse standardne VPN klient on **FortiClient** (või OpenVPN alternatiivina vastavalt IT osakonna juhisele).
   - Tööarvutitele paigaldatakse FortiClient automaatselt läbi Company Portali / tarkvarakeskuse.
2. **Ühenduse parameetrid**:
   - Ühenduse nimi: `Sisevork-VPN`
   - VPN Server / Gateway: `vpn.siseveeb.ee`
   - Port: `443` (või `10443`)
   - Autentimistüüp: SSO / SAML autentimine koos 2FA-ga.

## Kuidas luua VPN ühendus?
1. Ava FortiClient rakendus oma arvutis.
2. Vali ühenduseks `Sisevork-VPN` ja vajuta "Connect".
3. Brauseriaknas soorita sisselogimine oma töökonto ja Mobiil-ID / Smart-ID või autentimisäpiga.
4. Kui ühendus on aktiivne, muutub olekuroheliseks ja kuvatakse teade "Status: Connected".

## Levinumad probleemid ja abi
- **Ühendus katkeb kohe pärast loomist**: Veendu, et sinu internetiühendus on stabiilne ja kohalik tulemüür ei blokeeri VPN porti.
- **Autentimise viga**: Kontrolli, kas sinu parool pole aegunud ja 2FA teavitusele reageeriti õigeaegselt.
- **Täiendav IT tugi**: Probleemide jätkumisel ava pilet kasutajatoe portaalis või kirjuta `abi@siseveeb.ee`.
