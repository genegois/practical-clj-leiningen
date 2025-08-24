# PRACTICAL CLOJURE SUPER MINI DEMO
ya ini mah intinya standard backend webapp lah.
Stack with ring and reitit route, db postgresql and mongo.

## Terus cara ngetesnya? 
1. Pertama-tama, setting databasenya dulu.
   Kalok setting MongoDBnya simple ae, uri-nya mongodb://localhost:27017 (udah default), terus name-nya practical_clj
   
   kalo postgreSQLnya, lo masuk ke PostgreSQL, terus buat database dengan nama 'practical_clj'
```sql
CREATE DATABASE practical_clj;
\c practical_clj;
CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    email TEXT NOT NULL UNIQUE,
    password TEXT NOT NULL,
    name TEXT NOT NULL
);
CREATE TABLE tokens (
    id SERIAL PRIMARY KEY,
    token TEXT NOT NULL UNIQUE,
    user_id INT NOT NULL REFERENCES users(id) ON DELETE CASCADE
);

```
2. Download tuh kode , terus jalanin kodenya di dev/user.clj. Nah yowes tinggal jalanin repl, reload, terus jalanin (start)
   
   a. Kalo lo mau eksperimen di backend-nya tinggal eksperimen di situ
   
   b. Kalo lo mau ngetes frontend-nya, lo tinggal masuk ke folder itu terus jalanin pakek npm, tapi sebelum itu: 

   ```bash
   cd (ke tempat kode ni lo simpen)
   npm install shadow-cljs --save-dev
   npm install react react-dom
   npx shadow-cljs watch app
   ```
  
