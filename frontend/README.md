# Bulón S.R.L. — Frontend

React 19 + TypeScript + Vite.

## Ejecutar

```bash
npm install
npm run dev        # http://localhost:5173
npm run build      # compila a dist/
```

El backend tiene que estar corriendo en `http://localhost:8080` (ver `../backend/README.md`).
En desarrollo, Vite redirige las llamadas a `/api` al backend (proxy en `vite.config.ts`),
por lo que no hace falta configurar CORS.

## Estructura

```
src/
├── api/          # Llamadas HTTP al backend
├── hooks/        # Hooks de React (p. ej. useHealth)
├── components/   # Componentes de la interfaz
├── App.tsx       # Página principal (panel general)
└── index.css     # Estilos globales y colores
```

## Panel general

Por ahora muestra el estado de la conexión con el backend consultando `GET /api/health`
cada 15 segundos:

| Estado            | Significado                                             |
|-------------------|---------------------------------------------------------|
| Conectado         | Backend y base de datos responden                       |
| Conexión parcial  | El backend responde pero la base de datos está caída     |
| Sin conexión      | El backend no responde                                   |
