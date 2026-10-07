#!/bin/zsh
# Runs the full pipeline from any terminal. Loads Java/Maven via sdkman (.sdkmanrc) and the keys from private/.env.
# private/.env is gitignored and holds plain KEY=value lines, no "export":
#   OLLAMA_API_KEY=...
#   OLLAMA_MODEL=gemma4:31b
cd "$(dirname "$0")/.." || exit 1
source ~/.sdkman/bin/sdkman-init.sh && sdk env >/dev/null
if [ -f private/.env ]; then set -a; source private/.env; set +a; fi
for v in OLLAMA_API_KEY OLLAMA_MODEL; do
  [ -z "${(P)v}" ] && { echo "Missing $v. Export it, or put '$v=...' in private/.env"; exit 1; }
done
cd app && mvn -q spring-boot:run -Dspring-boot.run.arguments=run
