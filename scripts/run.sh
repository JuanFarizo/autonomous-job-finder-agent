#!/bin/zsh
# Runs the full pipeline from any terminal. Loads Java/Maven via sdkman (.sdkmanrc) and the keys from private/env.sh.
# private/env.sh is gitignored and holds lines like:  export OLLAMA_API_KEY=...   export OLLAMA_MODEL=gemma4:31b
cd "$(dirname "$0")/.." || exit 1
source ~/.sdkman/bin/sdkman-init.sh && sdk env >/dev/null
[ -f private/env.sh ] && source private/env.sh
for v in OLLAMA_API_KEY OLLAMA_MODEL; do
  [ -z "${(P)v}" ] && { echo "Missing $v. Export it, or put 'export $v=...' in private/env.sh"; exit 1; }
done
cd app && mvn -q spring-boot:run -Dspring-boot.run.arguments=run
