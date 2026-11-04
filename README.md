# La masa de joc

Zece jocuri de cărți în limba română, realizate cu Kotlin Multiplatform și Compose
Multiplatform. Logica și interfața sunt în `commonMain`, iar aplicația iOS
folosește o gazdă SwiftUI foarte subțire.

La pornire ajungi pe pagina principală, de unde poți începe un joc, continua
partida activă, redeschide jocurile recente sau marca un joc preferat. Catalogul
disponibil acum include Șeptică și are căutare, inclusiv fără diacritice.
Macao, Tabinet, Război, Popa prostu’, Blackjack și Șaizeci și șase
au un adversar automat. La Șeptică alegi 2, 3 sau 4 jucători (tu și 1–3 roboți).
Whist și Rentz au trei adversari automați, iar Solitaire
se joacă individual. Toate funcționează offline, fără bani reali.

Partida poate fi continuată cât timp aplicația rămâne deschisă; repornirea
aplicației o încheie. Pagina Setări permite alegerea uneia dintre cele cinci
teme, iar alegerea este memorată între porniri. Codul folosește identificatori
și comentarii în engleză.
Jocurile noi folosesc un model comun pentru pachetul standard și componente
vizuale comune. Variantele regionale pot avea alte reguli; fiecare joc afișează
regulile variantei implementate.

## Grafică și asistent tactic

Catalogul are ilustrații diferite pentru fiecare joc, fundal de masă texturat
și o paletă verde închis, bleumarin și auriu. Cărțile au simboluri vectoriale,
dispuneri de simboluri pentru valorile numerice, ornamente pentru figuri și
animație discretă la selectare. Nu se descarcă imagini sau fonturi externe.

**Sfat tactic · local** oferă o recomandare la cerere, fără a juca în locul tău.
Asistentul folosește algoritmi locali de decizie și căutare, nu un model LLM
sau un serviciu de chat. Nu necesită cheie API și nu trimite starea jocului
pe internet. Recomandările sunt estimări, nu garanții de câștig.

Analiza folosește mâna ta și informațiile publice, nu identitatea cărților
ascunse ale adversarilor sau ordinea necunoscută a pachetului. La Război și
Popa prostu’, asistentul explică limitele controlului și ale informației
disponibile, fără să pretindă că poate ghici o carte ascunsă.

## Reguli Șeptică

- Pachet de 32 de cărți: 7, 8, 9, 10, J, Q, K și A (As) din fiecare culoare.
- La intrare și la joc nou alegi 2–4 jucători; fiecare joacă pentru el, fără echipe.
- Numărul ales te include: tu și Ion în 2, plus Costică în 3, plus Vasile în 4.
- Numai în 3 jucători se scot 8 de cupă și 8 de caro (30 de cărți).
  Cei doi optari rămași taie orice valoare, exact ca șeptarii, inclusiv la continuare.
- Fiecare jucător începe cu patru cărți.
- Un 7 sau o carte de aceeași valoare cu prima carte a mâinii „taie”.
- Toți ceilalți răspund în ordine. Dacă ultima tăiere aparține altcuiva,
  jucătorul care a deschis poate continua doar cu un 7 (sau 8 în 3 jucători) ori cu valoarea de
  pornire, ori poate renunța la mână. La continuare, ceilalți răspund din nou.
- Ultimul jucător care a tăiat ia toate cărțile și deschide mâna următoare.
- După fiecare mână se împarte câte o carte fiecăruia, pe rând, începând cu
  câștigătorul. Se repetă până când fiecare are patru cărți sau se termină
  pachetul; jucătorii care au deja patru cărți nu mai primesc.
- După epuizarea pachetului, jucătorii fără cărți sunt săriți. Cel care a deschis
  poate ceda dacă nu mai poate continua. Dacă învingătorul nu mai are cărți,
  următorul jucător cu cărți deschide.
- Așii și decarii valorează câte un punct. Sunt opt puncte în total; câștigă
  punctajul maxim, iar un maxim comun înseamnă egalitate.
- Dacă iei toate cele 8 puncte, apare „l-ai batut la fundu gol”; dacă un robot
  ia toate cele 8 puncte, apare „ai fost batut la fundu gol”.

## Reguli Macao

- Pachet de 54 de cărți (2–10, J, Q, K, A și doi jokeri); cinci cărți de jucător.
- Se potrivește culoarea sau valoarea; Asul și jokerii sunt universali și cer
  alegerea unei culori noi.
- 2 dă +2, 3 dă +3, jokerul negru +5, cel roșu +10. Oricare dintre aceste
  cărți poate cumula penalizarea, indiferent de culoare.
- Sub penalizare, joci o carte de atac sau tragi totalul și închei tura.
  Asul și 4 nu anulează penalizarea.
- 4 sare adversarul; în doi jucători, cel care l-a pus joacă din nou.
- Tragerea normală este de o carte și încheie tura, chiar dacă acea carte poate
  fi jucată. Tragerea este permisă și când ai o carte jucabilă.
- Macao se anunță automat la o carte rămasă. Ultima carte câștigă imediat,
  inclusiv dacă este specială.
- Pachetul se reface din cărțile aruncate, mai puțin ultima. La epuizare
  completă, se trag numai cărțile disponibile și se afișează numărul efectiv.

## Reguli Tabinet

- Varianta în doi, cu 52 de cărți fără jokeri: șase de jucător, patru pe masă
  și 36 în pachet. După golirea ambelor mâini se împart câte șase, fără cărți
  noi pe masă.
- Se joacă o carte pe tură. Selectezi o carte din mână, apoi cărțile de capturat
  de pe masă. Butonul **Sugestie** propune o selecție validă.
- Fiecare grup capturat trebuie să însumeze valoarea cărții jucate:
  2–10, J=12, Q=13, K=14, As=1 sau 11. Grupurile nu pot folosi aceeași carte.
  Cu un As pot fi luate simultan grupuri de 1 și de 11.
- Captura este opțională: fără selecție, cartea jucată rămâne pe masă.
- A/K/Q/J/10 valorează câte un punct; 10 de caro valorează două puncte
  în total, iar 2 de treflă un punct. Totalul cărților este de 22 de puncte.
- Golirea mesei aduce o tablă (+1), exceptând ultima mutare a partidei.
  Tablele sunt cumulative.
- La final, masa rămasă merge la ultimul jucător care a capturat, fără tablă.
  Dacă nimeni nu a capturat, merge la împărțitor (calculatorul).
- Majoritatea cărților capturate aduce +3 la final; egalitatea nu aduce bonus.
  Se joacă o singură partidă cu toate cele 52 de cărți, nu un meci până la 101.

Valorile și punctajul urmează [Pagat](https://www.pagat.com/fishing/tablic.html).
Curățarea finală de către ultimul care a capturat urmează varianta românească
descrisă pe [Wikipedia](https://ro.wikipedia.org/wiki/Tabinet). Există variante
regionale; regulile de mai sus sunt cele aplicate în această aplicație.

## Structură

```text
composeApp/
  src/commonMain/   motorul jocului și interfața Compose
  src/commonTest/   testele regulilor și ale unei partide complete
  src/iosMain/      UIViewController-ul exportat către Swift
  src/desktopMain/  lansator local pentru dezvoltare
iosApp/
  iosApp.xcodeproj  aplicația iPhone și integrarea framework-ului KMP
```

## Celelalte jocuri

| Joc | Varianta disponibilă |
| --- | --- |
| Război | 52 de cărți, Asul cel mai mare, războaie la egalitate; rezultatul fiecărei bătălii rămâne vizibil până la următoarea apăsare. |
| Popa prostu’ | Doi jucători, 51 de cărți după eliminarea regelui de treflă. Perechile de aceeași valoare se elimină automat; alegi o carte ascunsă de la adversar. |
| Blackjack | Un jucător contra dealerului. Hit/Stand, dealerul se oprește inclusiv la 17 cu As flexibil; fără split, double sau asigurare. Jetoane exclusiv virtuale. |
| Whist | Patru jucători, 32 de cărți, program de 24 de runde de la 1 la 8 cărți și înapoi. Licitație și punctaj cumulativ. |
| Rentz | Meci scurt cu patru jucători și opt contracte jucate o singură dată fiecare, inclusiv contractul de așezare a suitelor. |
| Solitaire | Klondike, o carte trasă, reciclări nelimitate; mutări prin atingerea sursei și a destinației. Nu toate amestecările sunt garantat rezolvabile. |
| Șaizeci și șase | 24 de cărți, atu, căsătorii, schimbarea nouarului de atu și închiderea pachetului. Meci până la 7 puncte de joc. |

### Whist și Rentz

Whist folosește rangurile 7–A. Se joacă patru runde cu o carte, 2–7 cărți,
patru runde cu opt cărți, 7–2 cărți și încă patru runde cu o carte.
Ultimul licitant nu poate face suma licitațiilor egală cu numărul de cărți.
Se respectă culoarea; în lipsă, se joacă atu dacă există. Nu se impune
supratăierea. La opt cărți nu există atu. Licitația exactă aduce `5 + licitația`,
altfel se scade diferența absolută dintre licitație și mâinile câștigate.

Rentz are opt contracte: fără mâini, fără cupe, fără dame, fără regele de cupă,
fără ultimele două mâini, totaluri, whist și așezarea suitelor. Nu există atu
în contractele de levate și nu există restricții suplimentare la deschidere,
în afară de respectarea culorii la răspuns. La așezare, suitele încep de la J;
se adaugă vecinul imediat mai mic sau mai mare. Pasul este permis numai când
nu există o carte legală.

### Blackjack și Șaizeci și șase

Blackjack începe cu 100 de jetoane virtuale și o miză fixă de 10 pe rundă.
Victoria obișnuită întoarce 20, egalitatea 10, iar un blackjack natural 25
(incluzând miza). Nu există cumpărături, depuneri, retrageri sau recompense
cu valoare reală.

La Șaizeci și șase, valorile de punctaj sunt A=11, 10=10, K=4, Q=3, J=2,
9=0. Căsătoriile K–Q aduc 20, respectiv 40 la atu, numai după câștigarea
unei mâini. La epuizarea sau închiderea pachetului, răspunsul trebuie să
respecte culoarea și să câștige dacă este posibil, altfel să folosească atu
când lipsește culoarea. Atingerea a 66 de puncte se recunoaște automat.
Regulile de închidere și punctele de meci sunt explicate în ecranul jocului.

## Rulare în IntelliJ IDEA

Folosește JDK 17 și pluginul **Kotlin Multiplatform**. Pe Windows poți dezvolta
și testa interfața cu:

```powershell
.\gradlew.bat :composeApp:run
```

Țintele iOS sunt configurate numai pe macOS. Pe Windows și Linux, importul
Gradle include doar ținta desktop și nu pregătește distribuția Kotlin/Native
pentru iOS. După modificarea configurației, anulează orice import în curs și
folosește **Reload All Gradle Projects** în IntelliJ.

Testele motorului:

```powershell
.\gradlew.bat :composeApp:desktopTest
```

## Rulare pe iPhone sau simulator

Compilarea iOS necesită macOS și Xcode. Deschide rădăcina proiectului în
IntelliJ IDEA, activează funcționalitățile Kotlin Multiplatform experimentale
dacă IDE-ul le solicită, apoi selectează configurația `iosApp` și un simulator.

Alternativ, deschide `iosApp/iosApp.xcodeproj` în Xcode și rulează schema
`iosApp`. Faza de build a proiectului Xcode execută automat
`:composeApp:embedAndSignAppleFrameworkForXcode`. Pentru un dispozitiv real,
alege echipa Apple în **Signing & Capabilities** și schimbă identificatorul
`ro.septica.game` dacă este necesar.
