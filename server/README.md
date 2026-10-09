# carTInder szerver

Ktor + Exposed backend a carTInder apphoz. Alapból SQLite-ot használ (`data/cartinder.db`), PostgreSQL-re a `DATABASE_URL` átírásával állítható át.

## Futtatás

A projekt gyökeréből:

```sh
./gradlew -p server run            # fejlesztéshez
./gradlew -p server installDist    # futtatható csomag: server/build/install/cartinder-server/bin/cartinder-server
./gradlew -p server test           # tesztek
```

Az Android emulátor a gép `localhost`-ját a `http://10.0.2.2:8080` címen éri el.

## Beállítások (környezeti változók)

| Változó | Alapérték | Leírás |
|---|---|---|
| `PORT` | `8080` | HTTP port |
| `DATABASE_URL` | `jdbc:sqlite:data/cartinder.db` | JDBC URL, pl. `jdbc:postgresql://localhost/cartinder` |
| `DATABASE_USER`, `DATABASE_PASSWORD` | üres | PostgreSQL-hez |
| `JWT_SECRET` | fejlesztői kulcs | **Élesben kötelező beállítani!** |
| `UPLOAD_DIR` | `data/uploads` | Feltöltött hirdetésfotók helye |
| `SEED_DEMO_DATA` | `true` | 16 demó eladó és autó létrehozása első induláskor |

## Hogyan működik

- **Fiókok:** e-mail + jelszó (bcrypt), a válaszban 30 napig érvényes JWT. Minden más végpont `Authorization: Bearer <token>` fejlécet vár.
- **Swipe:** a vevő jobbra/felfelé húz → kedvelés. Valódi eladónál ez a `GET /api/likes` listába kerül, és az eladó dönt (`accept` / `decline`). Elfogadáskor jön létre a match.
- **Demó eladók:** a seedelt hirdetések gazdái botok. Azonnal döntenek (szuper like-ot mindig elfogadnak), és késleltetve, „gépelve” válaszolnak a chatben. Az elutasítást a vevő nem látja.
- **Chat:** a kliens lekérdezéssel frissít (`GET /api/matches/{id}/messages?after=<utolsó id>`), az olvasottságot a `POST /api/matches/{id}/read` jelzi.

## API

| Metódus | Útvonal | Leírás |
|---|---|---|
| POST | `/api/auth/register` | Regisztráció → token + profil |
| POST | `/api/auth/login` | Bejelentkezés → token + profil |
| GET / PUT | `/api/me` | Saját profil és statisztika / profil szerkesztése |
| GET | `/api/feed?maxPrice=&maxKm=&fuels=BENZIN,DIZEL` | Még nem látott autók |
| POST | `/api/listings/{id}/swipe` | `{"direction": "LEFT" \| "RIGHT" \| "UP"}` → azonnali match, ha van |
| POST | `/api/swipes/undo` | Utolsó swipe visszavonása |
| GET | `/api/likes` | A hirdetéseidre érkezett kedvelések |
| POST | `/api/likes/{id}/accept`, `/decline` | Kedvelés elfogadása / elutasítása |
| GET | `/api/matches` | Matchek utolsó üzenettel és olvasatlan számmal |
| GET / DELETE | `/api/matches/{id}` | Egy match / unmatch |
| GET / POST | `/api/matches/{id}/messages` | Üzenetek (`?after=`) / üzenet küldése |
| POST | `/api/matches/{id}/read` | Olvasottság jelzése |
| GET / POST | `/api/listings/mine`, `/api/listings` | Saját hirdetések / új hirdetés |
| PUT / DELETE | `/api/listings/{id}` | Hirdetés szerkesztése / törlése |
| POST | `/api/listings/{id}/photo` | Fotó feltöltése (multipart, JPEG/PNG/WebP, max. 8 MB) |
| GET | `/uploads/{fájl}` | Feltöltött fotók |

A hibák formátuma: `{"error": "magyar nyelvű üzenet"}`.

## Demó fotók

A demó autók fotói a Wikimedia Commonsról származnak. A szerző és a licenc minden hirdetésnél a `photoCredit` mezőben van, és az app megjeleníti a kártyán.
