// Copia el juego (index.html y fuentes) a www/, la carpeta que Capacitor mete en la app.
import { cpSync, mkdirSync, rmSync } from "node:fs";

rmSync("www", { recursive: true, force: true });
mkdirSync("www");
cpSync("index.html", "www/index.html");
cpSync("fonts", "www/fonts", { recursive: true });
console.log("www/ lista");
