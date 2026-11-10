#!/bin/zsh
# Usage: ./scripts/tailor.sh path/to/job.txt  -> data/cv/manual_<name>.pdf (grounded tailoring, no model call)
# private/.env is gitignored and holds plain KEY=value lines, no "export":
#   OLLAMA_API_KEY=...
#   OLLAMA_MODEL=gemma4:31b
cd "$(dirname "$0")/.." || exit 1
source ~/.sdkman/bin/sdkman-init.sh && sdk env >/dev/null
if [ -f private/.env ]; then set -a; source private/.env; set +a; fi
for v in OLLAMA_API_KEY OLLAMA_MODEL; do
  [ -z "${(P)v}" ] && { echo "Missing $v. Export it, or put '$v=...' in private/.env"; exit 1; }
done
cd app && F="$(cd .. && realpath "$1")" && set -- "$F" "${2:+$(cd .. && realpath "$2")}" && mvn -q spring-boot:run -Dspring-boot.run.arguments="--tailor=$1 ${2:+--profile=$2}"
