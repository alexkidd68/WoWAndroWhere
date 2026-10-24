# CurseForge proxy

A small server you can run yourself so the app reaches CurseForge without the API key on the device. It adds your key, only forwards the World of Warcraft requests the app makes, rate-limits each device, and caches nothing. The app has no default proxy: enter this one's address in Settings → CurseForge → Your proxy.

## Run

On a server with Docker, where the proxy's hostname points at the server and ports 80 and 443 are open:

```bash
cp .env.example .env    # then put the key between the single quotes
docker compose up -d --build
curl https://your.host/health
```

Set `PROXY_HOST` in `.env` to the proxy's hostname. Caddy gets and renews the HTTPS certificate on its own. Then enter `https://your.host` in the app's Settings.
